package com.whispermmepub.wownote.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AiProvider { GROQ, GEMINI }

data class AiConfig(
    val provider: AiProvider = AiProvider.GROQ,
    val groqModel: String = "openai/gpt-oss-20b",
    val geminiModel: String = "gemini-2.5-flash"
)

/** API keys are encrypted with an Android Keystore AES key before preferences storage. */
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("wow_note_ai_private", Context.MODE_PRIVATE)

    var provider: AiProvider
        get() = runCatching { AiProvider.valueOf(prefs.getString("provider", "GROQ")!!) }.getOrDefault(AiProvider.GROQ)
        set(value) { prefs.edit().putString("provider", value.name).apply() }

    var groqModel: String
        get() = prefs.getString("groq_model", "openai/gpt-oss-20b") ?: "openai/gpt-oss-20b"
        set(value) { prefs.edit().putString("groq_model", value.trim()).apply() }

    var geminiModel: String
        get() = prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) { prefs.edit().putString("gemini_model", value.trim()).apply() }

    fun saveGroqKey(value: String) = saveSecret("groq_key", value)
    fun saveGeminiKey(value: String) = saveSecret("gemini_key", value)
    fun groqKey(): String = loadSecret("groq_key")
    fun geminiKey(): String = loadSecret("gemini_key")

    fun config() = AiConfig(provider, groqModel, geminiModel)

    private fun saveSecret(name: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(name).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        prefs.edit().putString(name, payload).apply()
    }

    private fun loadSecret(name: String): String {
        val payload = prefs.getString(name, null) ?: return ""
        return runCatching {
            val parts = payload.split(':', limit = 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object { private const val KEY_ALIAS = "wow_note_ai_api_key_v1" }
}
