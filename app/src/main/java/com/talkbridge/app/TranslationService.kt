package com.talkbridge.app

import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text translation using downloadable ML Kit language models.
 * The supported set is deliberately based on ML Kit's actual language codes;
 * languages such as Yoruba, Igbo and Hausa require a separate cloud provider.
 */
object TranslationService {
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

        val translator: Translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
        )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            return translator.translate(text).awaitTask()
        } finally {
            translator.close()
        }
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
