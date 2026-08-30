package com.whispermmepub.wownote.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.whispermmepub.wownote.ui.components.FloatingPressable
import com.whispermmepub.wownote.ui.components.FloatingSurface
import kotlinx.coroutines.launch

@Composable
fun AiAssistantDialog(
    noteText: String,
    selectedText: String,
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit,
    onReplace: (String) -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AiSettings(context) }
    val client = remember { AiClient(context) }
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(settings.provider) }
    var groqModel by remember { mutableStateOf(settings.groqModel) }
    var geminiModel by remember { mutableStateOf(settings.geminiModel) }
    var groqKey by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var task by remember { mutableStateOf(AiTask.WRITE) }
    var instruction by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    fun saveSettings() {
        settings.provider = provider
        settings.groqModel = groqModel.ifBlank { "openai/gpt-oss-20b" }
        settings.geminiModel = geminiModel.ifBlank { "gemini-2.5-flash" }
        if (groqKey.isNotBlank()) settings.saveGroqKey(groqKey.trim())
        if (geminiKey.isNotBlank()) settings.saveGeminiKey(geminiKey.trim())
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 24.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("WoW AI", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Groq / Gemini writing assistant",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
                Spacer(Modifier.height(14.dp))

                Text("Provider", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderButton("Groq", provider == AiProvider.GROQ, Modifier.weight(1f)) { provider = AiProvider.GROQ }
                    ProviderButton("Gemini", provider == AiProvider.GEMINI, Modifier.weight(1f)) { provider = AiProvider.GEMINI }
                }
                Spacer(Modifier.height(12.dp))

                if (provider == AiProvider.GROQ) {
                    Label("Groq API key")
                    Field(groqKey, { groqKey = it }, "Paste new key (saved key stays encrypted)")
                    Spacer(Modifier.height(8.dp))
                    Label("Groq model")
                    Field(groqModel, { groqModel = it }, "openai/gpt-oss-20b")
                } else {
                    Label("Gemini API key")
                    Field(geminiKey, { geminiKey = it }, "Paste new key (saved key stays encrypted)")
                    Spacer(Modifier.height(8.dp))
                    Label("Gemini model")
                    Field(geminiModel, { geminiModel = it }, "gemini-2.5-flash")
                }

                Spacer(Modifier.height(14.dp))
                Label("AI task")
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AiTask.entries.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { option ->
                                TaskButton(option, task == option, Modifier.weight(1f)) { task = option }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Label("Instruction")
                Field(
                    value = instruction,
                    onValueChange = { instruction = it },
                    hint = if (task == AiTask.WRITE) "ဥပမာ — မိုးရွာတဲ့ညအကြောင်း စာပိုဒ်တစ်ပုဒ်ရေး" else "လိုချင်တဲ့ tone / length / rule ကိုရေး"
                )

                if (selectedText.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Selected text ကို AI input အဖြစ်သုံးမယ်", fontSize = 12.sp, color = Color(0xFF007AFF))
                }

                Spacer(Modifier.height(14.dp))
                FloatingPressable(
                    onClick = {
                        saveSettings()
                        running = true
                        error = null
                        result = ""
                        scope.launch {
                            val source = selectedText.ifBlank { noteText }
                            client.run(task, instruction, source)
                                .onSuccess { result = it }
                                .onFailure { error = it.message ?: "AI request failed" }
                            running = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shapeRadius = 18.dp,
                    elevation = 10.dp,
                    backgroundColor = Color(0xFF007AFF)
                ) {
                    if (running) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).height(24.dp).width(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Generate", modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Color(0xFFD33A2C), fontSize = 13.sp)
                }

                if (result.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    FloatingSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        elevation = 7.dp,
                        backgroundColor = Color(0xFFF2F2F7),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(result, fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF171719))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingPressable(
                            onClick = { onInsert(result); onDismiss() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shapeRadius = 17.dp,
                            elevation = 7.dp,
                            backgroundColor = Color(0xFFE9F3FF)
                        ) { Text("Insert", modifier = Modifier.align(Alignment.Center), color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) }
                        FloatingPressable(
                            onClick = { onReplace(result); onDismiss() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shapeRadius = 17.dp,
                            elevation = 7.dp,
                            backgroundColor = Color(0xFF007AFF)
                        ) { Text("Replace", modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                FloatingPressable(
                    onClick = { saveSettings(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shapeRadius = 16.dp,
                    elevation = 4.dp,
                    backgroundColor = Color(0xFFF2F2F7)
                ) { Text("Done", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6E6E73))
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, hint: String) {
    FloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 5.dp,
        backgroundColor = Color(0xFFF2F2F7),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF171719)),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text(hint, fontSize = 13.sp, color = Color(0xFF8E8E93))
                    inner()
                }
            }
        )
    }
}

@Composable
private fun ProviderButton(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shapeRadius = 15.dp,
        elevation = if (active) 7.dp else 3.dp,
        backgroundColor = if (active) Color(0xFF007AFF) else Color(0xFFF2F2F7)
    ) {
        Text(label, modifier = Modifier.align(Alignment.Center), color = if (active) Color.White else Color(0xFF3A3A3C), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TaskButton(task: AiTask, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FloatingPressable(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shapeRadius = 14.dp,
        elevation = if (active) 6.dp else 2.dp,
        backgroundColor = if (active) Color(0xFFE5F1FF) else Color(0xFFF2F2F7)
    ) {
        Text(task.label, modifier = Modifier.align(Alignment.Center), fontSize = 11.sp, color = if (active) Color(0xFF007AFF) else Color(0xFF525257), fontWeight = FontWeight.SemiBold)
    }
}
