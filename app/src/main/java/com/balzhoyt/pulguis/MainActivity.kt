package com.balzhoyt.pulguis

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    var t1:TextToSpeech? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()!!.hide()

        t1 = TextToSpeech(applicationContext,
            TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    t1!!.language = Locale.ITALIAN
                }
            })

        cvAprenderNumeros.setOnClickListener{
            val i = Intent(this,AprenderNumerosActivity::class.java)
            startActivity(i)

            voz("Vamos! aprenderemos los Números!")
        }

        cvAprenderABC.setOnClickListener{
            val i = Intent(this,AprenderABCActivity::class.java)
            startActivity(i)
            voz("Muy bien, vamos a aprender el abecedario")
        }

        cvAprenderFiguras.setOnClickListener{
            voz("Figuras, Ok..Muy bien")
        }
        cvAprenderColores.setOnClickListener{
            voz("los colores, me gusta")
        }




    }

    private fun voz(s: String) {
        val toSpeak = s
        //Toast.makeText(applicationContext, toSpeak, Toast.LENGTH_SHORT).show()
        t1!!.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null)
    }
}
