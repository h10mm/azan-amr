package com.github.meypod.al_azan

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.github.meypod.al_azan.core.domain.model.system.SystemChange
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.di.AdhanSyncInitializer
import com.github.meypod.al_azan.di.DiyanetFixInitializer
import com.github.meypod.al_azan.di.DndSyncInitializer
import com.github.meypod.al_azan.di.MigrationEntryPoint
import com.github.meypod.al_azan.di.NotificationChannelInitializer
import com.github.meypod.al_azan.di.RamadanNoticeInitializer
import com.github.meypod.al_azan.di.ReminderSyncInitializer
import com.github.meypod.al_azan.di.WidgetSyncInitializer
import com.tencent.mmkv.MMKV
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.TimeZone
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    companion object {
        private const val TAG = "App"
        private const val SETTINGS_STORAGE = "SETTINGS_STORAGE"
        private const val MIGRATED_TO_V2 = "MIGRATED_TO_V2"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationChannelInitializer: dagger.Lazy<NotificationChannelInitializer>

    @Inject
    lateinit var widgetSyncInitializer: dagger.Lazy<WidgetSyncInitializer>

    @Inject
    lateinit var adhanSyncInitializer: dagger.Lazy<AdhanSyncInitializer>

    @Inject
    lateinit var reminderSyncInitializer: dagger.Lazy<ReminderSyncInitializer>

    @Inject
    lateinit var ramadanNoticeInitializer: dagger.Lazy<RamadanNoticeInitializer>

    @Inject
    lateinit var dndSyncInitializer: dagger.Lazy<DndSyncInitializer>

    @Inject
    lateinit var diyanetFixInitializer: dagger.Lazy<DiyanetFixInitializer>

    @Inject
    lateinit var schedulerReconciler: dagger.Lazy<SchedulerReconciler>

    @Inject
    lateinit var systemChangeRepository: dagger.Lazy<SystemChangeRepository>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        val mmkv = MMKV.defaultMMKV()
        if (mmkv.contains(SETTINGS_STORAGE) && !mmkv.contains(MIGRATED_TO_V2)) {
            Log.i(TAG, "Legacy settings detected. Starting v2 migration.")
            val migrationResult = runCatching {
                // Using runBlocking is necessary here because if we migrate asynchronously,
                // the app might start with a seemingly fresh install state and the user might
                // configure it before the migration finishes. We want the user to wait until
                // their old data is fully transferred to the new database/storage.
                runBlocking {
                    val migrationEntryPoint =
                        EntryPointAccessors.fromApplication(this@App, MigrationEntryPoint::class.java)
                    migrationEntryPoint.repositoryMigrationRunner().run()
                }
            }
            migrationResult.onSuccess {
                Log.i(TAG, "v2 migration completed successfully.")
            }
            migrationResult.onFailure { error ->
                Log.e(TAG, "v2 migration failed. Will start with fresh data.", error)
            }
            mmkv.encode(MIGRATED_TO_V2, true)
        }
        notificationChannelInitializer.get().start() // we use lazy because MMKV needs to be initialized first
        widgetSyncInitializer.get().start()
        adhanSyncInitializer.get().start()
        reminderSyncInitializer.get().start()
        ramadanNoticeInitializer.get().start()
        dndSyncInitializer.get().start()
        diyanetFixInitializer.get().start()
        registerRuntimeTimeChangeReceiver()
    }

    private fun registerRuntimeTimeChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val action = intent?.action ?: return
                    schedulerReconciler.get().markManualTimeChange()
                    when (action) {
                        Intent.ACTION_TIME_CHANGED ->
                            systemChangeRepository.get().tryEmit(SystemChange.TimeChanged)

                        Intent.ACTION_TIMEZONE_CHANGED ->
                            systemChangeRepository.get().tryEmit(
                                SystemChange.TimeZoneChanged(TimeZone.getDefault().id),
                            )
                    }
                    appScope.launch {
                        schedulerReconciler.get().onSystemTimeChanged()
                    }
                }
            },
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }
}
