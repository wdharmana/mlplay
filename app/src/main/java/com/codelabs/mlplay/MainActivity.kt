package com.codelabs.mlplay

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
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
