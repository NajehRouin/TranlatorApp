package com.example.translatorapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var translator: Translator
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var outputLanguageSpinner: Spinner

    private val inputLanguages = listOf(
        "English", "French (Canada)", "French (France)", "Arabic", "Italian", "Japanese", "Turkish"
    )
    private val inputLanguageCodes = listOf(
        TranslateLanguage.ENGLISH, TranslateLanguage.FRENCH, TranslateLanguage.FRENCH,
        TranslateLanguage.ARABIC, TranslateLanguage.ITALIAN, TranslateLanguage.JAPANESE, TranslateLanguage.TURKISH
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val translateButton = findViewById<Button>(R.id.translateButton)
        val outputText = findViewById<TextView>(R.id.outputText)
        val inputLanguageSpinner = findViewById<Spinner>(R.id.inputLanguageSpinner)
        outputLanguageSpinner = findViewById<Spinner>(R.id.outputLanguageSpinner)
        val speakButton = findViewById<Button>(R.id.speakButton)
        val voiceInputButton = findViewById<ImageButton>(R.id.voiceInputButton)

        // Initialiser TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale("ar")) // Par défaut en arabe
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Langue non supportée")
                }
            } else {
                Log.e("TTS", "Échec de l'initialisation de TTS")
            }
        }


        // Adapter pour les Spinners
        val inputAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, inputLanguages)
        inputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputLanguageSpinner.adapter = inputAdapter

        val outputAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, inputLanguages)
        outputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        outputLanguageSpinner.adapter = outputAdapter

        // Listener pour la langue d'entrée
        inputLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = inputLanguages[position]
                inputEditText.hint = "Enter text in $selectedLanguage"

                val filteredOutputLanguages = inputLanguages.filter { it != selectedLanguage }
                val newOutputAdapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    filteredOutputLanguages
                )
                newOutputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                outputLanguageSpinner.adapter = newOutputAdapter
                outputLanguageSpinner.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener pour le bouton de traduction
        translateButton.setOnClickListener {
            val selectedInputLanguagePosition = inputLanguageSpinner.selectedItemPosition
            val sourceLanguage = inputLanguageCodes[selectedInputLanguagePosition]

            val selectedOutputLanguagePosition = outputLanguageSpinner.selectedItemPosition
            val targetLanguage = inputLanguageCodes.filterIndexed { index, _ ->
                inputLanguages[index] != inputLanguages[selectedInputLanguagePosition]
            }[selectedOutputLanguagePosition]

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    val textToTranslate = inputEditText.text.toString()
                    translateText(textToTranslate, outputText)
                }
                .addOnFailureListener {
                    outputText.text = "Model Download Failed"
                }
        }

        // Listener pour le bouton speak
        speakButton.setOnClickListener {
            val outputText = findViewById<TextView>(R.id.outputText).text.toString()
            speak(outputText)
        }

        // Listener pour le bouton de reconnaissance vocale
        voiceInputButton.setOnClickListener {
            startVoiceInput()
        }
    }

    private fun translateText(inputText: String, outputTextView: TextView) {
        translator.translate(inputText)
            .addOnSuccessListener { translatedText ->
                outputTextView.text = translatedText
            }
            .addOnFailureListener {
                outputTextView.text = "Translation Failed"
            }
    }

    private fun speak(text: String) {
        val selectedOutputLanguagePosition = outputLanguageSpinner.selectedItemPosition
        val selectedLanguageCode = inputLanguageCodes[selectedOutputLanguagePosition] // Utilisez le code correct
        val locale = when (selectedLanguageCode) {
            TranslateLanguage.ARABIC -> Locale("ar")
            TranslateLanguage.ENGLISH -> Locale("en")
            TranslateLanguage.FRENCH -> Locale("fr")
            TranslateLanguage.ITALIAN -> Locale("it")
            TranslateLanguage.JAPANESE -> Locale("ja")
            TranslateLanguage.TURKISH -> Locale("tr")
            else -> Locale("ar") // langue par défaut
        }

        textToSpeech.language = locale
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val inputEditText = findViewById<EditText>(R.id.inputEditText)
            inputEditText.setText(result?.get(0))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
    }
}
