package com.agrilink.app.ui.common

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.agrilink.app.R
import java.util.Locale

/** "Read aloud" support for low-literacy users (design: every onboarding option can be read out). */
class Speaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            pending?.let { (text, tag) -> pending = null; if (ready) speak(text, tag) }
        }
    }

    fun speak(text: String, languageTag: String) {
        val engine = tts
        if (engine == null || !ready) {
            pending = text to languageTag
            return
        }
        val result = engine.setLanguage(Locale.forLanguageTag(languageTag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, R.string.voice_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agrilink")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
