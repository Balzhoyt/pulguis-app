package com.balzhoyt.pulguis

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Tasks.call
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter


class ClasificadorABC (private val context: Context) {


        // Se agrega un interprete de tensorflow.
        private var interpreter: Interpreter? = null
        var isInitialized = false
            private set

        /** Executor se encarga de hacer la inferencia en segundo plano. */
        private val executorService: ExecutorService = Executors.newCachedThreadPool()

        private var inputImageWidth: Int = 0
        private var inputImageHeight: Int = 0
        private var modelInputSize: Int = 0

        fun initialize(): Task<Void> {
            return Tasks.call(
                executorService,
                Callable<Void> {
                    initializeInterpreter()
                    null
                }
            )
        }

        @Throws(IOException::class)
        private fun initializeInterpreter() {
            // Cargando el modelo desde la la carpeta de activos (assets).
            val assetManager = context.assets
            val model = loadModelFile(assetManager, "abc.tflite")

            // Inicializar TF Lite Interpreter con NNAPI habilitado.
            val options = Interpreter.Options()
            options.setUseNNAPI(true)
            val interpreter = Interpreter(model, options)

            // Leer la forma de entrada del archivo de modelo.
            val inputShape = interpreter.getInputTensor(0).shape()
            inputImageWidth = inputShape[1]
            inputImageHeight = inputShape[2]
            modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth *inputImageHeight * PIXEL_SIZE

            // Finalice la inicialización del intérprete.
            this.interpreter = interpreter

            isInitialized = true
            Log.d(TAG, "Intérprete TFLite inicializado..")
        }

        @Throws(IOException::class)
        private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
            val fileDescriptor = assetManager.openFd(filename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        private fun classify(bitmap: Bitmap): ResultadosClasificador {
            check(isInitialized) { "TF Lite Interpreter aún no se ha inicializado." }

            // Preprocesamiento: cambiando el tamaño de la imagen de entrada
            // para que coincida con la forma de entrada del modelo.

            val resizedImage = Bitmap.createScaledBitmap(
                bitmap,
                inputImageWidth,
                inputImageHeight,
                true
            )
            val byteBuffer = convertBitmapToByteBuffer(resizedImage)

            // Definiendo una matriz para almacenar la salida del modelo.
            val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

            // Run inference con la entrada de datos.
            interpreter?.run(byteBuffer, output)

            // Post-processing: find the digit that has highest probability
            // and return it a human-readable string.
            val result = output[0]
            val maxIndex = result.indices.maxBy { result[it] } ?: -1
            val resultadosClasificador =ResultadosClasificador(maxIndex,result[maxIndex])


            return resultadosClasificador
        }

        fun classifyAsync(bitmap: Bitmap): Task<ResultadosClasificador>? {
            return Tasks.call(executorService, Callable<ResultadosClasificador> { classify(bitmap) })
        }

        fun close() {
            Tasks.call(
                executorService,
                Callable<String> {
                    // TODO: close the TF Lite interpreter here
                    interpreter?.close()

                    Log.d(TAG, "Cerrado el TFLite interpreter.")
                    null
                }
            )
        }

        private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
            val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
            byteBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(inputImageWidth * inputImageHeight)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            for (pixelValue in pixels) {
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)

                // Convert RGB to grayscale and normalize pixel value to [0..1].
                val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
                byteBuffer.putFloat(normalizedPixelValue)
            }

            return byteBuffer
        }

        companion object {
            private const val TAG = "DigitClassifier"

            private const val FLOAT_TYPE_SIZE = 4
            private const val PIXEL_SIZE = 1

            private const val OUTPUT_CLASSES_COUNT = 3
        }
    }
