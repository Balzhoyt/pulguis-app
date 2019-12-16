package com.balzhoyt.pulguis

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.divyanshu.draw.widget.DrawView
import kotlinx.android.synthetic.main.activity_aprender.*
import java.util.*


class AprenderNumerosActivity : AppCompatActivity() {

    var t1:TextToSpeech? =null

    var aprendeEsteNumero: Int? =null
    var puntuacion:Int = 0
    var intentos:Int=0
    var resultado:String =""

    private var drawView: DrawView? = null
    private var textView: TextView? = null
    private var clasificador = Clasificador(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aprender)
        getSupportActionBar()!!.hide()

        t1 = TextToSpeech(applicationContext,
            OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    t1!!.language = Locale.ITALIAN
                }
            })

        drawView = findViewById(R.id.draw_view)
        drawView?.setStrokeWidth(70.0f)
        drawView?.setColor(Color.WHITE)
        drawView?.setBackgroundColor(Color.BLACK)

        // boton para jugar y inicializar la clasificacion.
        btnJugar.setOnClickListener {
            txtPuntuacion.text=puntuacion.toString()
            intentos++
            txtNumIntento.text=intentos.toString()
            progressBar.progress=puntuacion

            aprendeEsteNumero=aleatorio(0,9)

            when(aprendeEsteNumero){
                0-> {ivNumero.setImageResource(R.drawable.n0)}
                1-> {ivNumero.setImageResource(R.drawable.n1)}
                2-> {ivNumero.setImageResource(R.drawable.n2)}
                3-> {ivNumero.setImageResource(R.drawable.n3)}
                4-> {ivNumero.setImageResource(R.drawable.n4)}
                5-> {ivNumero.setImageResource(R.drawable.n5)}
                6-> {ivNumero.setImageResource(R.drawable.n6)}
                7-> {ivNumero.setImageResource(R.drawable.n7)}
                8-> {ivNumero.setImageResource(R.drawable.n8)}
                9-> {ivNumero.setImageResource(R.drawable.n9)}
            }

            val toSpeak = "intenta dibujar un $aprendeEsteNumero"
            //Toast.makeText(applicationContext, toSpeak, Toast.LENGTH_SHORT).show()
            t1!!.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null)

            drawView?.clearCanvas()
            botonVisible(false)
        }
        // Activación de clasificación de configuración
        // para que se clasifique después de cada trazo dibujado.
        drawView?.setOnTouchListener { _, event ->
              drawView?.onTouchEvent(event)
             if (event.action == MotionEvent.ACTION_UP) {
                classifyDrawing()
             }
            true
        }
            clasificador
            .initialize()
            .addOnFailureListener { e -> Log.e(TAG, "Error al configurar el clasificador de dígitos.", e) }


    }


    private fun classifyDrawing() {
        val bitmap = drawView?.getBitmap()
        var confianza=""
        if ((bitmap != null) && (clasificador.isInitialized)) {
            clasificador
                .classifyAsync(bitmap)
                ?.addOnSuccessListener {
                    objResultados -> textView?.text = objResultados.resultados.toString()
                    val c =  objResultados.confianza
                    val numero=objResultados.resultados

                    val confianza = listOf(", eres un buen alumno, aprendes muy rápido",
                        ",buen trabajo, pero puedes hacerlo mejor", ",buen trabajo, pero puedes hacerlo mejor",
                        ",te falta practicar, pero puedes hacerlo mejor",
                        ", podemos hacerlo mejor la próxima vez",
                        ", tu trabajo está dando frutos")

                    if (numero == aprendeEsteNumero) {
                        puntuacion++
                        resultado = "Has dibujado un $numero, Excelénte muy bíen"
                    } else {
                        puntuacion--
                        resultado =", no coincide con lo que te dige, pero no hay problema, tambien se aprende de los errores"
                    }

                    val toSpeak = " $resultado  ${confianza[this!!.aleatorio(0,confianza.size)!!]}, pulsa de nuevo"
                    //Toast.makeText(applicationContext, toSpeak, Toast.LENGTH_SHORT).show()
                    t1!!.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null)

                    delayFunction({ botonVisible(true) }, 5000)
                }
                ?.addOnFailureListener { e ->
                    textView?.text = getString(
                        R.string.classification_error_message,
                        e.localizedMessage
                    )
                    Log.e(TAG, "Error clasificando el dibujo.", e)
                }
        }

    }

    private fun botonVisible(condicion:Boolean) {
        if(condicion==true){
            btnJugar!!.visibility=View.VISIBLE
            txtBoton.visibility=View.VISIBLE
            txtBoton.text="nuevo"
        }
        else{
            btnJugar!!.visibility=View.INVISIBLE
            txtBoton.visibility=View.INVISIBLE
        }

    }

    fun delayFunction(function: ()-> Unit, delay: Long) {
        Handler().postDelayed(function, delay)
    }
    private fun aleatorio(inicio: Int, fin: Int): Int? {
        return (inicio + Random().nextInt(fin + 1 ))
    }

    override fun onDestroy() {
        clasificador.close()
        super.onDestroy()
    }
    override fun onPause() {
        if (t1 != null) {
            t1!!.stop()
            t1!!.shutdown()
        }
        super.onPause()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
