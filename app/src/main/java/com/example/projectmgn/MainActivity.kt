package com.example.projectmgn

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.min



class MainActivity : AppCompatActivity() {

    private val MAX_FONT_SIZE = 96F
    private lateinit var capturedimage:ImageView
    private lateinit var contentobject:TextView
    private lateinit var img:Bitmap
    private lateinit var  photofile: File
    private val filename="photo.jpg"
    private val rmg:Int=789
    private val pimg=9867

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            this.supportActionBar!!.hide()
        } catch (e: NullPointerException) {
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        capturedimage=findViewById(R.id.imageView)
        contentobject=findViewById(R.id.about)
        val takepicure=findViewById<Button>(R.id.button1)
        val scandetail=findViewById<Button>(R.id.button2)
        val fromgallery=findViewById<Button>(R.id.button4)
        val saveimage=findViewById<Button>(R.id.button5)
        takepicure.setOnClickListener{
            contentobject.text=""
            Log.d("inside button pressed","1st")
            val tpi=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photofile=getphotofile(filename)
            val fileP=FileProvider.getUriForFile(this,"com.example.projectmgn.fileprovider",photofile)
            tpi.putExtra(MediaStore.EXTRA_OUTPUT,fileP)
            startActivityForResult(tpi,rmg)
        }
        scandetail.setOnClickListener{
            GlobalScope.launch(Dispatchers.Default) {
                runObjectDetection(img)
            }

        }
        fromgallery.setOnClickListener{
            val imgal=Intent(ACTION_PICK)
            intent.type="image/*"
            startActivityForResult(imgal,pimg)
        }
        saveimage.setOnClickListener{

            if(img==null){
           MediaStore.Images.Media.insertImage(
               contentResolver,
               img,
               "detection",
               "Image of detection"

           )
            }

        }

    }
   

    



    private fun getphotofile(fileName:String):File{
        val dir=getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName,"jpg",dir)
    }



    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.3f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this,
            "lite-model_efficientdet_lite0_detection_metadata_1.tflite",
            options
        )

        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        // Step 4: Parse the detection result and show it

        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()


            val text = "${category.label}, ${category.score.times(100).toInt()}%"
            // adding for textview

            contentobject.append("* LABEL ->  ${category.label}  CONFIDENCE =  ${category.score.times(100).toInt().toString()} %")
            contentobject.append("\n")


            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
        runOnUiThread {
            capturedimage.setImageBitmap(imgWithResult)
        }
    }




    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.GREEN
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)


            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.GREEN
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==rmg && resultCode== Activity.RESULT_OK){
          val bitum:Bitmap=BitmapFactory.decodeFile(photofile.absolutePath)
            img=bitum
            capturedimage.setImageBitmap(bitum)
            val targetW: Int = capturedimage.width
            val targetH: Int = capturedimage.height

            val bmOptions = BitmapFactory.Options().apply {
                // Get the dimensions of the bitmap
                inJustDecodeBounds = true

                BitmapFactory.decodeFile(photofile.absolutePath, this)

                val photoW: Int = outWidth
                val photoH: Int = outHeight

                // Determine how much to scale down the image
                val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

                // Decode the image file into a Bitmap sized to fill the View
                inJustDecodeBounds = false
                inSampleSize = scaleFactor
                inMutable = true
            }
            val exifInterface = ExifInterface(photofile.absolutePath)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val bitmap = BitmapFactory.decodeFile(photofile.absolutePath, bmOptions)
            img= when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    rotateImage(bitmap, 90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    rotateImage(bitmap, 180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    rotateImage(bitmap, 270f)
                }
                else -> {
                    bitmap
                }
            }
        }

        if (resultCode == RESULT_OK && requestCode == pimg) {
           val imageUri = data?.data
            val bitum=MediaStore.Images.Media.getBitmap(this.contentResolver,imageUri)
            img=Bitmap.createScaledBitmap(
                bitum,
                capturedimage.width,
                capturedimage.height,
                false
            )
           capturedimage.setImageBitmap(img)
        }




    }



}
data class DetectionResult(val boundingBox: RectF, val text: String)