package com.github.meypod.al_azan.main.tasbih

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Continuous Arabic SpeechRecognizer controller for hands-free voice Tasbih counting.
 * Accurately matches the currently active dhikr, normalizes Arabic text (harakat, alefs, taa marbouta),
 * counts multiple occurrences per utterance, and prevents duplicate counts between partial and final results.
 */
class TasbihSpeechController(
    private val context: Context,
    private val getCurrentDhikrText: () -> String,
    private val onCountDetected: (times: Int) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorMessage: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var lastCountedOccurrencesInUtterance = 0

    private val restartRunnable = Runnable {
        if (shouldKeepListening) {
            beginListeningSession()
        }
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorMessage("خدمة التعرف على الصوت غير متوفرة على هذا الجهاز")
            onListeningStateChanged(false)
            return
        }
        shouldKeepListening = true
        onListeningStateChanged(true)
        beginListeningSession()
    }

    fun stop() {
        shouldKeepListening = false
        mainHandler.removeCallbacks(restartRunnable)
        destroyRecognizer()
        onListeningStateChanged(false)
    }

    fun release() {
        stop()
    }

    private fun beginListeningSession() {
        mainHandler.post {
            if (!shouldKeepListening) return@post
            destroyRecognizer()
            lastCountedOccurrencesInUtterance = 0
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer
                recognizer.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) = Unit
                        override fun onBeginningOfSpeech() {
                            lastCountedOccurrencesInUtterance = 0
                        }
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit

                        override fun onError(error: Int) {
                            lastCountedOccurrencesInUtterance = 0
                            if (!shouldKeepListening) return
                            when (error) {
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    shouldKeepListening = false
                                    onListeningStateChanged(false)
                                    onErrorMessage("يرجى منح إذن الميكروفون لتشغيل العدّ الصوتي")
                                }
                                SpeechRecognizer.ERROR_AUDIO -> {
                                    shouldKeepListening = false
                                    onListeningStateChanged(false)
                                    onErrorMessage("تعذر الوصول إلى الميكروفون حالياً")
                                }
                                else -> {
                                    // Silence timeout / no match / client busy -> quietly restart session
                                    scheduleRestart(350L)
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spoken = matches?.firstOrNull().orEmpty().trim()
                            if (spoken.isNotEmpty()) {
                                val currentDhikr = getCurrentDhikrText()
                                val totalDetected = matchDhikrOccurrences(spoken, currentDhikr)
                                if (totalDetected > lastCountedOccurrencesInUtterance) {
                                    val delta = totalDetected - lastCountedOccurrencesInUtterance
                                    onCountDetected(delta)
                                }
                            }
                            lastCountedOccurrencesInUtterance = 0
                            if (shouldKeepListening) {
                                scheduleRestart(200L)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                                .trim()
                            if (partial.isNotEmpty()) {
                                val currentDhikr = getCurrentDhikrText()
                                val totalDetected = matchDhikrOccurrences(partial, currentDhikr)
                                if (totalDetected > lastCountedOccurrencesInUtterance) {
                                    val delta = totalDetected - lastCountedOccurrencesInUtterance
                                    lastCountedOccurrencesInUtterance = totalDetected
                                    onCountDetected(delta)
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    },
                )

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(intent)
            } catch (_: Exception) {
                shouldKeepListening = false
                onListeningStateChanged(false)
                onErrorMessage("تعذر تشغيل العدّ الصوتي على هذا الجهاز")
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, delayMs)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
            // Safe cleanup
        } finally {
            speechRecognizer = null
        }
    }

    companion object {
        /**
         * Normalizes Arabic text by removing tashkeel, normalizing alefs, taa marbouta,
         * alef maqsura, tatweel, punctuation, and extra whitespace.
         */
        fun normalizeArabic(text: String): String {
            if (text.isBlank()) return ""
            return text
                // 1. Remove Arabic harakat / diacritics / tashkeel / Quranic signs
                .replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
                // 2. Normalize Alef forms (أ, إ, آ, ٱ -> ا)
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ٱ', 'ا')
                // 3. Normalize Taa Marbouta (ة -> ه)
                .replace('ة', 'ه')
                // 4. Normalize Alef Maqsura (ى -> ي)
                .replace('ى', 'ي')
                // 5. Remove Tatweel / Kashida (ـ)
                .replace("ـ", "")
                // 6. Replace punctuation with space
                .replace(Regex("[.,!؟;:،()\"\\-—_/\\[\\]{}<>]"), " ")
                // 7. Collapse multiple spaces into single space and trim
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Matches how many times the active [dhikrText] occurred in [spokenText].
         * Returns 0 if no match (unrelated speech or noise is ignored completely).
         */
        fun matchDhikrOccurrences(spokenText: String, dhikrText: String): Int {
            val normSpoken = normalizeArabic(spokenText)
            val normDhikr = normalizeArabic(dhikrText)
            if (normSpoken.isBlank() || normDhikr.isBlank()) return 0

            val dhikrWords = normDhikr.split(" ").filter { it.isNotBlank() }
            if (dhikrWords.isEmpty()) return 0

            // Case A: Short Dhikrs (<= 4 words, e.g. "سبحان الله", "الحمد لله", "الله اكبر", "لا اله الا الله", "استغفر الله")
            if (dhikrWords.size <= 4) {
                return countPhraseOccurrences(normSpoken, normDhikr)
            }

            // Case B: Long Dhikrs (> 4 words, e.g. "لا إله إلا الله وحده لا شريك له...")
            // 1. Direct whole-phrase occurrences if present
            val directOccurrences = countPhraseOccurrences(normSpoken, normDhikr)
            if (directOccurrences > 0) return directOccurrences

            // 2. Keyword prefix matching (first 3 words) or 70% word similarity
            val first3Words = dhikrWords.take(3).joinToString(" ")
            val spokenWordsSet = normSpoken.split(" ").filter { it.isNotBlank() }.toSet()
            val matchingWordsCount = dhikrWords.count { it in spokenWordsSet }
            val similarity = matchingWordsCount.toFloat() / dhikrWords.size.toFloat()

            val hasPrefixMatch = normSpoken.contains(first3Words)
            val hasHighSimilarity = similarity >= 0.70f

            return if (hasPrefixMatch || hasHighSimilarity) 1 else 0
        }

        /**
         * Counts exact occurrences of [phrase] inside [text] respecting word boundaries.
         */
        private fun countPhraseOccurrences(text: String, phrase: String): Int {
            if (text.isBlank() || phrase.isBlank()) return 0
            var count = 0
            var startIndex = 0
            while (startIndex < text.length) {
                val index = text.indexOf(phrase, startIndex)
                if (index == -1) break
                val isStartBoundary = index == 0 || text[index - 1] == ' '
                val endIndex = index + phrase.length
                val isEndBoundary = endIndex == text.length || text[endIndex] == ' '
                if (isStartBoundary && isEndBoundary) {
                    count++
                    startIndex = endIndex
                } else {
                    startIndex = index + 1
                }
            }
            return count
        }
    }
}
