package com.example.googlemediapipe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class speech_to_text : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textView: TextView
    private lateinit var textview2:TextView
    private lateinit var startButton: Button


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_speech_to_text)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textView = findViewById(R.id.speechTextView)
        textview2 = findViewById(R.id.result_textview)
        startButton = findViewById(R.id.startSpeechButton)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        startButton.setOnClickListener {
            startSpeechRecognition()
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener{
            override fun onReadyForSpeech(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onBeginningOfSpeech() {
                TODO("Not yet implemented")
            }

            override fun onRmsChanged(p0: Float) {
                TODO("Not yet implemented")
            }

            override fun onBufferReceived(p0: ByteArray?) {
                TODO("Not yet implemented")
            }

            override fun onEndOfSpeech() {
                restartSpeechRecognition()
            }

            override fun onError(p0: Int) {
                Toast.makeText(applicationContext,"Error $p0",Toast.LENGTH_LONG).show()

            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if(matches != null){
                    textview2.text = matches[0]
                }
                restartSpeechRecognition()
            }

            override fun onPartialResults(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }
        })






    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
        speechRecognizer.startListening(intent)


    }

    private fun restartSpeechRecognition() {
        speechRecognizer.stopListening()
        startSpeechRecognition() // Restart listening automatically
    }

    public fun ondestry(){
        super.onDestroy()
        speechRecognizer.destroy()
   }

}