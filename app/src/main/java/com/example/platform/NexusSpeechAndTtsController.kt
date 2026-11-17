package com.example.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.PermissionManagerHelper
import com.example.ui.theme.containsDevanagari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechInputState {
    data object Idle : SpeechInputState()
    data object Listening : SpeechInputState()
    data class Partial(val text: String) : SpeechInputState()
    data class Result(val text: String) : SpeechInputState()
    data class Error(val message: String) : SpeechInputState()
}

class NexusSpeechAndTtsController(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingUtterance: String? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsAvailable = MutableStateFlow(true)
    val ttsAvailable: StateFlow<Boolean> = _ttsAvailable.asStateFlow()

    private val _speechState = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
    val speechState: StateFlow<SpeechInputState> = _speechState.asStateFlow()

    fun initTts() {
        if (tts != null) return
        runCatching {
            tts = TextToSpeech(context.applicationContext, this)
        }.onFailure {
            _ttsAvailable.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            _ttsAvailable.value = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            pendingUtterance?.let {
                pendingUtterance = null
                speak(it)
            }
        } else {
            _ttsAvailable.value = false
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (tts == null) {
            pendingUtterance = text
            initTts()
            return
        }
        if (!ttsReady) {
            pendingUtterance = text
            return
        }

        val targetLocale = if (containsDevanagari(text)) {
            Locale.forLanguageTag("hi-IN")
        } else {
            Locale.forLanguageTag("en-IN")
        }
        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        tts?.setSpeechRate(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
        _isSpeaking.value = false
    }

    fun startListening(languageTag: String = "en-IN") {
        if (!PermissionManagerHelper.hasMicrophonePermission(context)) {
            _speechState.value = SpeechInputState.Error("Microphone permission is required for voice input.")
            return
        }
        if (!PermissionManagerHelper.isSpeechRecognitionAvailable(context)) {
            _speechState.value = SpeechInputState.Error("On-device/system speech recognition is unavailable on this device. Please type your reminder.")
            return
        }

        stopListening()
        runCatching {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _speechState.value = SpeechInputState.Listening
                }

                override fun onBeginningOfSpeech() {
                    _speechState.value = SpeechInputState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap mic to retry or type below."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again or type."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Use typed input instead."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed."
                        else -> "Speech recognizer unavailable (code $error). Typed input works offline."
                    }
                    _speechState.value = SpeechInputState.Error(msg)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty()
                    if (best.isNotBlank()) {
                        _speechState.value = SpeechInputState.Result(best)
                    } else {
                        _speechState.value = SpeechInputState.Error("No speech recognized. Please try again.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        _speechState.value = SpeechInputState.Partial(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer = recognizer
            _speechState.value = SpeechInputState.Listening
            recognizer.startListening(intent)
        }.onFailure {
            _speechState.value = SpeechInputState.Error("Unable to start speech recognizer. Use typed input.")
        }
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun resetSpeechState() {
        _speechState.value = SpeechInputState.Idle
    }

    fun release() {
        stopListening()
        stopSpeaking()
        runCatching { tts?.shutdown() }
        tts = null
        ttsReady = false
    }
}
