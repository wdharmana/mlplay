package com.codelabs.mlplay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var pictureImagePath = ""
    private var bitmap: Bitmap? = null

    val REQUEST_CAMERA = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkPermissions()) {
            startPermissionRequests()
        }

        btn_camera.setOnClickListener { openCamera(REQUEST_CAMERA) }

        btn_text.setOnClickListener {
            bitmap?.let {
                analyzeText()
            }
        }

        btn_face.setOnClickListener {
            bitmap?.let {
                analyzeFace()
            }
        }

        btn_image.setOnClickListener {
            bitmap?.let {
                analyzeImageLabel()
            }
        }

    }

    private fun openCamera(requestCode: Int) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = timeStamp + ".jpg"
        val storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName
        val file = File(pictureImagePath)
        val outputFileUri = Uri.fromFile(file)
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        startActivityForResult(cameraIntent, requestCode)
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager
                .PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager
                        .PERMISSION_GRANTED
    }

    private fun startPermissionRequests() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, permissions,
                201)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val imgFile = File(pictureImagePath)
            if (imgFile.exists()) {
                val bitmapFile = BitmapFactory.decodeFile(imgFile.absolutePath)
                bitmap = imageOrientationValidator(bitmapFile, imgFile.absolutePath)
                img_result.setImageBitmap(bitmap)
            }
        }

    }

    private fun analyzeText() {

        val image = FirebaseVisionImage.fromBitmap(bitmap!!)
        val detector = FirebaseVision.getInstance()
                .onDeviceTextRecognizer

        detector.processImage(image)
                .addOnSuccessListener { result ->
                    val resultText = result.text
                    tv_result.text = resultText
                }
                .addOnFailureListener {
                    tv_result.text = it.message
                }
    }

    private fun analyzeFace() {

        progress.visibility = View.VISIBLE

        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()


        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(highAccuracyOpts)

        val image = FirebaseVisionImage.fromBitmap(bitmap!!)

        detector.detectInImage(image)
                .addOnSuccessListener { faces ->

                    for (face in faces) {
                        val smileProb = if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            face.smilingProbability
                        } else 0F

                        val rightEyeOpenProb = if (face.rightEyeOpenProbability != FirebaseVisionFace
                                        .UNCOMPUTED_PROBABILITY) {
                            face.rightEyeOpenProbability
                        } else 0F

                        val leftEyeOpenProb = if (face.leftEyeOpenProbability != FirebaseVisionFace
                                        .UNCOMPUTED_PROBABILITY) {
                            face.leftEyeOpenProbability
                        } else 0F

                        setFace(smileProb > 0.4, rightEyeOpenProb > 0.3, leftEyeOpenProb > 0.3)
                    }

                    progress.visibility = View.GONE

                }
                .addOnFailureListener {
                    tv_result.text = it.message

                    progress.visibility = View.GONE

                }

    }


    private fun setFace(smile: Boolean, rightEyeOpened: Boolean, leftEyeOpened: Boolean) {
        var result = "Today, You look "
        if (smile) {
            result += "great "
            if (rightEyeOpened && leftEyeOpened) {
                result += "with awesome eyes "
            } else if (rightEyeOpened || leftEyeOpened) {
                result += "when winkling"
            }
        } else if (rightEyeOpened && leftEyeOpened) {
            result += "scary "
        } else {
            result += "sad "
        }
        tv_result.text = result
    }

    private fun analyzeImageLabel() {
        val image = FirebaseVisionImage.fromBitmap(bitmap!!)
        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler()

        labeler.processImage(image)
                .addOnSuccessListener { labels ->

                    var items = ""

                    for (label in labels) {
                        val text = label.text
                        val confidence = label.confidence

                        items += "${text} (${confidence * 100}%)\n"

                    }

                    tv_result.text = items

                }
                .addOnFailureListener { e ->
                    tv_result.text = e.message
                }
    }

    private fun imageOrientationValidator(bmap: Bitmap, path: String): Bitmap? {
        var bitmap: Bitmap?
        bitmap = bmap

        val ei: ExifInterface
        try {
            ei = ExifInterface(path)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> bitmap = rotateImage(bitmap, 90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> bitmap = rotateImage(bitmap, 180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> bitmap = rotateImage(bitmap, 270F)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {

        var bitmap: Bitmap? = null
        val matrix = Matrix()
        matrix.postRotate(angle)
        try {
            bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                    matrix, true)
        } catch (err: OutOfMemoryError) {
            err.printStackTrace()
        }

        return bitmap
    }

}
