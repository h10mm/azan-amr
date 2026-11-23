package com.github.meypod.al_azan.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import java.util.Calendar

data class SpiritualContext(
    val wordTag: String,
    val title: String,
    val badge: String,
    val subtitle: String,
    val isSmartMoment: Boolean = false,
)

object SpiritualContextResolver {

    private fun parseTodayTimeMillis(timeStr: String): Long? {
        val clean = timeStr.trim().replace("ص", "").replace("م", "").replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
        val parts = clean.split(":")
        if (parts.size != 2) return null
        val h = parts[0].trim().toIntOrNull() ?: return null
        val m = parts[1].trim().toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        var hour24 = h
        if ((timeStr.contains("م") || timeStr.contains("PM", ignoreCase = true)) && hour24 < 12) {
            hour24 += 12
        } else if ((timeStr.contains("ص") || timeStr.contains("AM", ignoreCase = true)) && hour24 == 12) {
            hour24 = 0
        }
        cal.set(Calendar.HOUR_OF_DAY, hour24)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun elapsedMinutes(timeStr: String): Long? {
        val timeMillis = parseTodayTimeMillis(timeStr) ?: return null
        val diff = System.currentTimeMillis() - timeMillis
        return if (diff >= 0) diff / 60_000L else null
    }

    fun resolve(data: NateegaWidgetData): SpiritualContext {
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val isFriday = (dayOfWeek == Calendar.FRIDAY)
        val diffMinutes = ((data.countdownTargetMillis - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)

        // 1. فحص لحظات ما بعد الصلاة مباشرة: بين الأذان والإقامة (0-20د) وأذكار ما بعد الفريضة (21-45د)
        val lastPrayerInfo = when (data.nextPrayer) {
            Prayer.Sunrise -> "الفجر" to data.fajrTime
            Prayer.Dhuhr -> "الشروق" to data.sunriseTime
            Prayer.Asr -> "الظهر" to data.dhuhrTime
            Prayer.Sunset, Prayer.Maghrib -> "العصر" to data.asrTime
            Prayer.Isha -> "المغرب" to data.maghribTime
            Prayer.Midnight, Prayer.Tahajjud, Prayer.Fajr -> "العشاء" to data.ishaTime
        }
        val elapsed = elapsedMinutes(lastPrayerInfo.second)

        // أ) بين الأذان والإقامة (أول 20 دقيقة بعد أذان الصلوات الخمس)
        if (elapsed != null && elapsed in 0..20 && lastPrayerInfo.first != "الشروق") {
            return SpiritualContext(
                wordTag = "دُعَاء مُجَاب",
                title = "بين أذان وإقامة ${lastPrayerInfo.first}",
                badge = "مستجاب",
                subtitle = "«الدعاء لا يُرد بين الأذان والإقامة» • سنة الصلاة القبلية والاجتهاد بالدعاء",
                isSmartMoment = true,
            )
        }

        // ب) أذكار ما بعد الفريضة (21 إلى 45 دقيقة بعد الصلاة)
        if (elapsed != null && elapsed in 21..45 && lastPrayerInfo.first != "الشروق") {
            return SpiritualContext(
                wordTag = "أَذْكَار",
                title = "معقبات صلاة ${lastPrayerInfo.first}",
                badge = "الاستغفار والتسبيح",
                subtitle = "«معقبات لا يخيب قائلهن» • الاستغفار ثلاثاً وآية الكرسي والتسبيح 33",
                isSmartMoment = true,
            )
        }

        // 2. أوقات النهي والكراهة عن الصلاة
        // - الشروق: من الشروق حتى ارتفاع الشمس قيد رمح (أول 20د)
        if (data.nextPrayer == Prayer.Dhuhr && elapsed != null && elapsed in 0..20) {
            return SpiritualContext(
                wordTag = "إِمْسَاك",
                title = "وقت النهي عن الصلاة (الشروق)",
                badge = "نهي عن النوافل",
                subtitle = "توقف عن صلاة النوافل والزم الاستغفار والتسبيح حتى ترتفع الشمس",
                isSmartMoment = true,
            )
        }
        // - قبيل الظهر: استواء الشمس في كبد السماء (قبل الظهر بـ 15 دقيقة)
        if (data.nextPrayer == Prayer.Dhuhr && diffMinutes in 1..15) {
            return SpiritualContext(
                wordTag = "إِمْسَاك",
                title = "وقت النهي (استواء الشمس)",
                badge = "الظهر خلال $diffMinutes د",
                subtitle = "نهى النبي ﷺ عن الصلاة نصف النهار حتى تزول الشمس • استعد للوضوء",
                isSmartMoment = true,
            )
        }

        // 3. صلاة الضحى (صلاة الأوابين)
        if (data.nextPrayer == Prayer.Dhuhr && diffMinutes > 20) {
            return SpiritualContext(
                wordTag = "ضُحَى",
                title = "صلاة الضحى قائمة (صلاة الأوابين)",
                badge = "الظهر ${data.dhuhrTime}",
                subtitle = "ركعتان في الضحى تجزئان عن صدقة 360 مفصل في جسدك",
                isSmartMoment = true,
            )
        }

        // 4. يوم الجمعة وساعة الاستجابة
        if (isFriday) {
            // ساعة الاستجابة قبل الغروب (بين العصر والمغرب)
            if (data.nextPrayer in listOf(Prayer.Sunset, Prayer.Maghrib) && diffMinutes <= 60) {
                return SpiritualContext(
                    wordTag = "سَاعَة إِجَابَة",
                    title = "ساعة الاستجابة يوم الجمعة",
                    badge = "آخر ساعة قبل الغروب",
                    subtitle = "«فيها ساعة لا يوافقها عبد مسلم يدعو إلا استجيب له» • أكثر من الدعاء والصلاة على النبي ﷺ",
                    isSmartMoment = true,
                )
            }
            if (hour in 8..17 && data.nextPrayer in listOf(Prayer.Dhuhr, Prayer.Asr, Prayer.Sunset, Prayer.Maghrib)) {
                return SpiritualContext(
                    wordTag = "جُمُعَة",
                    title = "يوم الجمعة المبارك",
                    badge = "نور الأسبوع",
                    subtitle = "سورة الكهف وكثرة الصلاة على النبي ﷺ",
                    isSmartMoment = true,
                )
            }
        }

        // 5. صيام النوافل (الإثنين والخميس والأيام البيض 13 و 14 و 15) في المساء بعد 18:00
        if (hour >= 18) {
            val hijriDayNum = data.hijriDay.toIntOrNull()
            if (dayOfWeek == Calendar.SUNDAY) {
                return SpiritualContext(
                    wordTag = "صِيَام",
                    title = "غداً الإثنين سنة الصيام",
                    badge = "سنة نبوية",
                    subtitle = "«تُعرض الأعمال يوم الإثنين والخميس» • نوّر يومك بالصيام والنية",
                    isSmartMoment = true,
                )
            }
            if (dayOfWeek == Calendar.WEDNESDAY) {
                return SpiritualContext(
                    wordTag = "صِيَام",
                    title = "غداً الخميس سنة الصيام",
                    badge = "سنة نبوية",
                    subtitle = "«تُعرض الأعمال يوم الإثنين والخميس فأحب أن يُعرض عملي وأنا صائم»",
                    isSmartMoment = true,
                )
            }
            if (hijriDayNum in listOf(12, 13, 14)) {
                return SpiritualContext(
                    wordTag = "صِيَام",
                    title = "صيام الأيام البيض غداً (${hijriDayNum?.plus(1)} ${data.hijriMonth})",
                    badge = "الأيام البيض",
                    subtitle = "«صيام ثلاثة أيام من كل شهر صيام الدهر كله» • اغتنم الأجر",
                    isSmartMoment = true,
                )
            }
        }

        // 6. الثلث الأخير والأسحار والوتر
        if (data.nextPrayer in listOf(Prayer.Midnight, Prayer.Tahajjud, Prayer.Fajr)) {
            if (hour in 1..4) {
                return SpiritualContext(
                    wordTag = "قِيَام / سَحَر",
                    title = "وقت التنزل الإلهي والأسحار",
                    badge = "الفجر ${data.fajrTime}",
                    subtitle = "«هل من سائل فأعطيه؟» • والمستغفرين بالأسحار وركعة الوتر",
                    isSmartMoment = true,
                )
            }
            return SpiritualContext(
                wordTag = "وِتْر",
                title = "صلاة العشاء والوتر",
                badge = "أوتر قبل نومك",
                subtitle = "أوتروا يا أهل القرآن • ركعة وتر تضيء ليلتك",
                isSmartMoment = true,
            )
        }

        // 7. الحالة العامة للصلوات
        return when (data.nextPrayer) {
            Prayer.Sunrise -> SpiritualContext(
                wordTag = "فَجْر",
                title = "وقت صلاة الفجر",
                badge = "الشروق ${data.sunriseTime}",
                subtitle = "أذكار الصباح • تسبيح واستغفار حتى الشروق",
            )
            Prayer.Asr -> SpiritualContext(
                wordTag = "ظُهْر",
                title = "وقت صلاة الظهر",
                badge = "العصر ${data.asrTime}",
                subtitle = "أداء الفريضة وسننها الرواتب المباركة",
            )
            Prayer.Sunset, Prayer.Maghrib -> {
                if (diffMinutes <= 45) {
                    SpiritualContext(
                        wordTag = "ذِكْر",
                        title = "ساعة الدعاء وأذكار المساء",
                        badge = "الغروب خلال $diffMinutes د",
                        subtitle = "خير الدعاء قبل الغروب • أذكار المساء",
                    )
                } else {
                    SpiritualContext(
                        wordTag = "عَصْر",
                        title = "الصلاة الوسطى (العصر)",
                        badge = "المغرب ${data.maghribTime}",
                        subtitle = "حافظوا على الصلوات والصلاة الوسطى",
                    )
                }
            }
            Prayer.Isha -> SpiritualContext(
                wordTag = "مَغْرِب",
                title = "وقت صلاة المغرب",
                badge = "العشاء ${data.ishaTime}",
                subtitle = "أداء الفريضة وسنتها البعدية وأذكار المساء",
            )
            else -> SpiritualContext(
                wordTag = "ذِكْر",
                title = "طاعة وذكر لله",
                badge = "الصلوات الخمس",
                subtitle = "«ألا بذكر الله تطمئن القلوب»",
            )
        }
    }
}
