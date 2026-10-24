package com.talkbridge.app

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text translation using downloadable ML Kit language models.
 * The supported set is deliberately based on ML Kit's actual language codes;
 * languages such as Yoruba, Igbo and Hausa require a separate cloud provider.
 */
object TranslationService {
    private val translators = ConcurrentHashMap<String, Translator>()
    private val translationCache = ConcurrentHashMap<String, String>()
    private val readyPairs = ConcurrentHashMap.newKeySet<String>()
    private val pairLocks = ConcurrentHashMap<String, Mutex>()

    private val codeByName = mapOf(
        "English" to TranslateLanguage.ENGLISH,
        "Arabic" to TranslateLanguage.ARABIC,
        "Chinese" to TranslateLanguage.CHINESE,
        "French" to TranslateLanguage.FRENCH,
        "German" to TranslateLanguage.GERMAN,
        "Hindi" to TranslateLanguage.HINDI,
        "Italian" to TranslateLanguage.ITALIAN,
        "Japanese" to TranslateLanguage.JAPANESE,
        "Korean" to TranslateLanguage.KOREAN,
        "Dutch" to TranslateLanguage.DUTCH,
        "Portuguese" to TranslateLanguage.PORTUGUESE,
        "Russian" to TranslateLanguage.RUSSIAN,
        "Spanish" to TranslateLanguage.SPANISH,
        "Turkish" to TranslateLanguage.TURKISH
    )

    fun isSupported(language: String): Boolean = codeByName[language] != null

    suspend fun translate(text: String, sourceName: String, targetName: String): String {
        require(text.isNotBlank()) { "Enter text to translate." }
        val source = codeByName[sourceName]
            ?: throw UnsupportedOperationException("$sourceName is not available for on-device translation. A secure cloud translation service is needed.")
        val target = codeByName[targetName]
            ?: throw UnsupportedOperationException("$targetName is not available for on-device translation. A secure cloud translation service is needed.")
        if (source == target) return text

        val cacheKey = sourceName + "->" + targetName + ":" + text.trim()
        translationCache[cacheKey]?.let { return it }
        val pairKey = "$source->$target"
        val translator = translators.computeIfAbsent(pairKey) {
            Translation.getClient(
                TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
            )
        }

        // Keep the translator and downloaded model warm between keystrokes/messages.
        // A per-language-pair mutex prevents duplicate first-time downloads.
        if (!readyPairs.contains(pairKey)) {
            pairLocks.computeIfAbsent(pairKey) { Mutex() }.withLock {
                if (!readyPairs.contains(pairKey)) {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
                    readyPairs.add(pairKey)
                }
            }
        }
        val result = translator.translate(text).awaitTask()
        if (translationCache.size > 500) translationCache.clear()
        translationCache[cacheKey] = result
        return result
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
