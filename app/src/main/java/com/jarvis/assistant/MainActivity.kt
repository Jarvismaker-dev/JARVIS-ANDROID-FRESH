package com.jarvis.assistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { JarvisScreen(::askGroq, ::speak) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.UK
    }

    private fun speak(text: String) { runOnUiThread { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply") } }

    private fun askGroq(apiKey: String, prompt: String, callback: (String) -> Unit) {
        executor.execute {
            val answer = try {
                val payload = JSONObject()
                    .put("model", "llama-3.3-70b-versatile")
                    .put("messages", org.json.JSONArray().put(
                        JSONObject().put("role", "system").put("content", "You are JARVIS, a concise and helpful personal assistant. Use clear British English.")
                    ).put(JSONObject().put("role", "user").put("content", prompt)))
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) "Groq error ${response.code}: ${body.take(300)}"
                    else JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                }
            } catch (e: Exception) { "Connection error: ${e.localizedMessage ?: "Please try again."}" }
            runOnUiThread { callback(answer) }
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); executor.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisScreen(ask: (String, String, (String) -> Unit) -> Unit, speak: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Hello. JARVIS is ready. Add your Groq API key to begin.") }
    var busy by remember { mutableStateOf(false) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) prompt = spoken
    }
    MaterialTheme {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("J.A.R.V.I.S", style = MaterialTheme.typography.headlineLarge)
                Text("Your personal Android assistant", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("Groq API key") }, singleLine = true)
                Text("The key stays in this screen's memory and is not saved. Do not share it or commit it to GitHub.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Ask JARVIS") }, minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
                        try { speechLauncher.launch(intent) } catch (_: Exception) { answer = "Speech recognition is not available on this phone." }
                    }) { Text("Speak") }
                    Button(enabled = !busy && apiKey.isNotBlank() && prompt.isNotBlank(), onClick = {
                        busy = true
                        ask(apiKey.trim(), prompt.trim()) { answer = it; busy = false }
                    }) { Text(if (busy) "Thinking…" else "Ask") }
                }
                HorizontalDivider()
                Text("JARVIS", style = MaterialTheme.typography.titleMedium)
                Text(answer)
                OutlinedButton(onClick = { speak(answer) }) { Text("Read aloud") }
                Text("Voice output uses your phone's UK English text-to-speech voice if installed. A male voice is not guaranteed.", style = MaterialTheme.typography.bodySmall)
                Text("Safety: this starter does not control phone settings or apps. Any future phone actions will require your confirmation.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
