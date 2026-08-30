package com.whispermmepub.wownote.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class AiTask(val label: String) {
    WRITE("Write"),
    REWRITE("Rewrite"),
    EXPAND("Expand"),
    SHORTEN("Shorten"),
    PROOFREAD("Proofread"),
    SUMMARIZE("Summarize"),
    REVIEW_STYLE("Review style")
}

class AiClient(private val context: Context) {
    private val settings = AiSettings(context)
    private val styleProvider = ReviewStyleProvider(context)

    suspend fun run(task: AiTask, userInstruction: String, noteText: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val style = if (task == AiTask.REVIEW_STYLE) styleProvider.styleContext() else ""
            val prompt = buildPrompt(task, userInstruction, noteText, style)
            when (settings.provider) {
                AiProvider.GROQ -> callGroq(prompt)
                AiProvider.GEMINI -> callGemini(prompt)
            }
        }
    }

    private fun buildPrompt(task: AiTask, instruction: String, text: String, style: String): String {
        val action = when (task) {
            AiTask.WRITE -> "အသုံးပြုသူ၏ညွှန်ကြားချက်အတိုင်း မြန်မာစာကို အသစ်ရေးပါ။"
            AiTask.REWRITE -> "ပေးထားသောစာကို အဓိပ္ပါယ်မလွဲဘဲ သဘာဝကျပြီး ဖတ်ကောင်းအောင် ပြန်ရေးပါ။"
            AiTask.EXPAND -> "ပေးထားသောစာကို မူရင်းအဓိပ္ပါယ်နှင့်အသံမပျက်စေဘဲ အသေးစိတ်ချဲ့ရေးပါ။"
            AiTask.SHORTEN -> "ပေးထားသောစာ၏အဓိကအချက်မပျောက်ဘဲ ပိုတို၊ ပိုတိကျအောင်ရေးပါ။"
            AiTask.PROOFREAD -> "မြန်မာစာလုံးပေါင်း၊ သဒ္ဒါ၊ ပုဒ်ဖြတ်နှင့် စာကြောင်းစီးဆင်းမှုကို ပြင်ပေးပါ။ မလိုအပ်ဘဲအသံမပြောင်းပါနှင့်။"
            AiTask.SUMMARIZE -> "ပေးထားသောစာကို အချက်အလက်မမှားစေဘဲ ဖတ်လွယ်သော မြန်မာစာအနှစ်ချုပ်ရေးပါ။"
            AiTask.REVIEW_STYLE -> "ပေးထားသောအကြောင်းအရာကို အောက်ပါ style samples များ၏ လေသံ၊ စာကြောင်းလှုပ်ရှားမှုနှင့် မြန်မာစာရေးဟန်ကို လေ့လာအသုံးချပြီး လူရေးသလို သဘာဝကျသော စာအုပ်အညွှန်း/သုံးသပ်ချက်ရေးပါ။ Sample ထဲက စာကြောင်းတွေကို တိုက်ရိုက်မကူးပါနှင့်။"
        }
        return """
You are the writing assistant inside WoW Note.
Primary output language: Myanmar Unicode unless the user explicitly asks for another language.
Write naturally and clearly. Avoid repetitive AI-sounding introductions, canned praise, exaggerated conclusions, and unnecessary headings.
Do not explain your process. Return only the finished text that can be inserted into the note.

TASK:
$action

USER INSTRUCTION:
${instruction.ifBlank { "လိုအပ်သလို အကောင်းဆုံးလုပ်ပါ။" }}

CURRENT NOTE / SELECTED TEXT:
${text.ifBlank { "(empty)" }}

${if (style.isNotBlank()) "REFERENCE WRITING SAMPLES (imitate style, never copy wording):\n$style" else ""}
""".trimIndent()
    }

    private fun callGroq(prompt: String): String {
        val key = settings.groqKey()
        require(key.isNotBlank()) { "Groq API key မထည့်ရသေးပါ။" }
        val body = JSONObject()
            .put("model", settings.groqModel)
            .put("temperature", 0.72)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "You are WoW Note's Burmese writing assistant."))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        val response = postJson(
            url = "https://api.groq.com/openai/v1/chat/completions",
            body = body,
            headers = mapOf("Authorization" to "Bearer $key")
        )
        return response.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    private fun callGemini(prompt: String): String {
        val key = settings.geminiKey()
        require(key.isNotBlank()) { "Gemini API key မထည့်ရသေးပါ။" }
        val model = settings.geminiModel.ifBlank { "gemini-2.5-flash" }
        val body = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", prompt))
                )
            )
        )
        val response = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key",
            body = body,
            headers = emptyMap()
        )
        val parts = response.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        return buildString {
            for (i in 0 until parts.length()) {
                val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (text.isNotBlank()) append(text)
            }
        }.trim()
    }

    private fun postJson(url: String, body: JSONObject, headers: Map<String, String>): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 90_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText) }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "AI API error $code")
            }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
