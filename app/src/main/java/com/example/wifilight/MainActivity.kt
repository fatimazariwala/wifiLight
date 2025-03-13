package com.example.wifilight

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifilight.databinding.ActivityMainBinding
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Locale


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Main Activity"
    }

    lateinit var  binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer

    val audioRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            Log.i(TAG,"Background Location Permissions Granted!")
        }
    }

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            audioRequest.launch (
                android.Manifest.permission.RECORD_AUDIO
            )
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.i(TAG,"Results: ${matches}")
                if (matches != null && !matches.isEmpty()) {
                    val command = matches[0].lowercase(Locale.getDefault())
                    if (command == "on") {
                        Toast.makeText(this@MainActivity, "Received Signal ON!", Toast.LENGTH_SHORT).show()
                    } else if (command == "off" || command == "of") {
                        Toast.makeText(this@MainActivity,"Received Signal OFF",Toast.LENGTH_LONG).show()
                    }
                }
                //restartListening()
            }

            override fun onReadyForSpeech(params: Bundle) {
                Log.i(TAG,"Model Ready For Speech...")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onPartialResults(partialResults: Bundle) {}
            override fun onEvent(eventType: Int, params: Bundle) {}
        })

        startListening()


        binding.btnOn.setOnClickListener() {
            startListening()
//            sendDataToESP("on")
        }

        binding.btnOff.setOnClickListener() {
            sendDataToESP("off")
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'On' or 'Off'")
        speechRecognizer.startListening(intent)
    }

    private fun restartListening() {
        speechRecognizer.stopListening() // Stop any ongoing listening
        speechRecognizer.cancel() // Cancel current recognition
        startListening() // Restart listening
    }

    private fun sendDataToESP(msg:String) {
        val url = "http://192.168.4.1/get?data=$msg"
        val body: RequestBody = "".toRequestBody(null)

        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("HTTP", "Failed to fetch data", e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData: String = response.body.toString()
                    runOnUiThread { binding.replayStat.setText("Response Status: ${response.code}") }
                }
            }
        })
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }
}