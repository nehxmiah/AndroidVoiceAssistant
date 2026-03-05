package com.example.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voiceassistant.databinding.ActivityMainBinding
import com.example.voiceassistant.models.Calculator
import com.example.voiceassistant.models.Clock
import com.example.voiceassistant.models.CommandProcessor
import com.example.voiceassistant.models.ConversationMessage
import com.example.voiceassistant.adapters.ConversationAdapter
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var calculator: Calculator
    private lateinit var clock: Clock
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var conversationAdapter: ConversationAdapter

    private var isListening = false
    private var ttsInitialized = false
    private val conversationList = mutableListOf<ConversationMessage>()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        calculator = Calculator()
        clock = Clock()
        commandProcessor = CommandProcessor(calculator, clock)
        textToSpeech = TextToSpeech(this, this)

        // Setup UI
        setupRecyclerView()
        setupClickListeners()
        updateClock()
        startClockUpdates()

        // Request microphone permission
        requestAudioPermission()

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            initializeSpeechRecognizer()
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show()
        }

        // Welcome message
        addMessage("Welcome! I can help with time, date, and calculations.", false)
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(conversationList)
        binding.conversationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.calculateButton.setOnClickListener {
            handleCalculation()
        }

        binding.voiceButton.setOnClickListener {
            toggleListening()
        }

        binding.calculatorInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleCalculation()
                true
            } else {
                false
            }
        }

        binding.clearButton.setOnClickListener {
            binding.calculatorInput.setText("")
            binding.resultText.text = "Result: "
        }
    }

    private fun updateClock() {
        val timeStr = clock.getCurrentTime(false)
        val dateStr = clock.getCurrentDate(true)

        binding.timeText.text = timeStr
        binding.dateText.text = dateStr
    }

    private fun startClockUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateClock()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun handleCalculation() {
        val expression = binding.calculatorInput.text.toString().trim()

        if (expression.isEmpty()) {
            binding.resultText.text = "Result: Enter an expression"
            return
        }

        try {
            val result = calculator.evaluate(expression)
            val resultText = if (result % 1.0 == 0.0) {
                result.toInt().toString()
            } else {
                String.format("%.6f", result).trimEnd('0').trimEnd('.')
            }
            binding.resultText.text = "Result: $resultText"
            addMessage("Calculated: $expression = $resultText", false)
        } catch (e: Exception) {
            binding.resultText.text = "Error: ${e.message}"
            addMessage("Calculation error: ${e.message}", false)
        }
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!checkAudioPermission()) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            requestAudioPermission()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        isListening = true
        binding.voiceButton.text = "Stop Listening"
        binding.statusText.text = "Status: Listening..."
        binding.statusText.setTextColor(getColor(android.R.color.holo_red_dark))

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting speech recognition: ${e.message}", Toast.LENGTH_SHORT).show()
            stopListening()
        }
    }

    private fun stopListening() {
        isListening = false
        binding.voiceButton.text = "Start Listening"
        binding.statusText.text = "Status: Ready"
        binding.statusText.setTextColor(getColor(android.R.color.holo_green_dark))

        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            // Ignore errors when stopping
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    binding.statusText.text = "Status: Speak now..."
                }
            }

            override fun onBeginningOfSpeech() {
                runOnUiThread {
                    binding.statusText.text = "Status: Listening..."
                }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                runOnUiThread {
                    binding.statusText.text = "Status: Processing..."
                }
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }

                runOnUiThread {
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }

                    if (isListening) {
                        handler.postDelayed({
                            if (isListening) startListening()
                        }, 500)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    runOnUiThread {
                        handleVoiceInput(matches[0])
                    }
                }

                if (isListening) {
                    handler.postDelayed({
                        if (isListening) startListening()
                    }, 500)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleVoiceInput(text: String) {
        addMessage("You: $text", true)

        // Process command
        val response = commandProcessor.process(text)

        // Update calculator display if it was a calculation
        val textLower = text.lowercase()
        if (textLower.contains("calculate") ||
            textLower.contains("what is") ||
            textLower.contains("compute")) {
            val expr = calculator.parseSpokenExpression(text)
            if (expr.isNotEmpty()) {
                binding.calculatorInput.setText(expr)
                try {
                    val result = calculator.evaluate(expr)
                    val resultText = if (result % 1.0 == 0.0) {
                        result.toInt().toString()
                    } else {
                        String.format("%.6f", result).trimEnd('0').trimEnd('.')
                    }
                    binding.resultText.text = "Result: $resultText"
                } catch (e: Exception) {
                    binding.resultText.text = "Error: ${e.message}"
                }
            }
        }

        addMessage("Assistant: $response", false)
        speak(response)
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val timestamp = clock.getTimestamp()
        conversationList.add(ConversationMessage(message, timestamp, isUser))
        conversationAdapter.notifyItemInserted(conversationList.size - 1)
        binding.conversationRecyclerView.scrollToPosition(conversationList.size - 1)
    }

    private fun speak(text: String) {
        if (ttsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            ttsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (ttsInitialized) {
                textToSpeech.setSpeechRate(1.0f)
            } else {
                Toast.makeText(this, "Text-to-speech not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAudioPermission() {
        if (!checkAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission denied. Voice features disabled.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}