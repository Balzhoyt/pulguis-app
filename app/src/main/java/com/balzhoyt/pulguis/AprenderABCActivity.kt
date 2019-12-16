package com.balzhoyt.pulguis

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.divyanshu.draw.widget.DrawView
import kotlinx.android.synthetic.main.activity_aprender.*
import java.util.*

class AprenderABCActivity : AppCompatActivity() {

    var t1: TextToSpeech? =null

    var aprendeEstaLetra: String =""
    var puntuacion:Int = 0
    var intentos:Int=0
    var resultado:String =""

    private var drawView: DrawView? = null
    private var textView: TextView? = null
    private var clasificador = ClasificadorABC(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aprender_abc)
        getSupportActionBar()!!.hide()

        t1 = TextToSpeech(applicationContext,
            TextToSpeech.OnInitListener { status ->
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

            var LetraAleatoria=aleatorio(0,2)

            when(LetraAleatoria){
                0-> {ivNumero.setImageResource(R.drawable.a);aprendeEstaLetra="a"}
                1-> {ivNumero.setImageResource(R.drawable.b);aprendeEstaLetra="b"}
                2-> {ivNumero.setImageResource(R.drawable.c);aprendeEstaLetra="c"}
            }

            val toSpeak = "intenta dibujar una $aprendeEstaLetra"
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
                    val letra= when(objResultados.resultados.toInt()){
                        0->{"a"}
                        1->{"b"}
                        2->{"c"}

                        else -> {"no es letra"}
                    }

                    val confianza = listOf(", eres un buen alumno, aprendes muy rápido",
                        ",buen trabajo, pero puedes hacerlo mejor", ",buen trabajo, pero puedes hacerlo mejor",
                        ",te falta practicar, pero puedes hacerlo mejor",
                        ", podemos hacerlo mejor la próxima vez",
                        ", tu trabajo está dando frutos")

                    if (letra == aprendeEstaLetra) {
                        puntuacion++
                        resultado = "Has dibujado una $letra, Excelénte muy bíen"
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
            btnJugar!!.visibility= View.VISIBLE
            txtBoton.visibility= View.VISIBLE
            txtBoton.text="nuevo"
        }
        else{
            btnJugar!!.visibility= View.INVISIBLE
            txtBoton.visibility= View.INVISIBLE
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
