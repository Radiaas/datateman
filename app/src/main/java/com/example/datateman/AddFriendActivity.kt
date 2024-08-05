package com.example.datateman

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.datateman.databinding.ActivityAddFriendBinding
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import android.util.Base64
import androidx.core.content.FileProvider

class AddFriendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddFriendBinding

    private lateinit var viewModel: FriendViewModel

    private lateinit var photoFile: File

    private var photoStr: String = ""

    private var cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.ivPictures.setImageBitmap(takenImage)
            }
        }

    private var galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val parcelFileDescriptor = contentResolver.openFileDescriptor(
                    it.data?.data ?: return@registerForActivityResult, "r"
                )
                val fileDescriptor = parcelFileDescriptor?.fileDescriptor
                val inputStream = FileInputStream(fileDescriptor)

                val outputStream = FileOutputStream(photoFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                parcelFileDescriptor?.close()

                val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.ivPictures.setImageBitmap(takenImage)
                photoStr = bitmapToString(takenImage)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_friend)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Cannot Create Image File", Toast.LENGTH_SHORT).show()
            return
        }

        val viewModelFactory = FriendVMFactory(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[FriendViewModel::class.java]

        binding.btnSave.setOnClickListener {
            addData()
        }

        binding.btnCamera.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener{
            openGallery()
        }

        binding.ivPictures.setOnClickListener{
            openGallery()
            takePhoto()
        }
    }

    private fun addData() {
        val name = binding.etName.text.toString().trim()
        val school = binding.etSchool.text.toString().trim()
        val hobby = binding.etHobby.text.toString().trim()

        if (name.isEmpty() || school.isEmpty() || hobby.isEmpty()) {
            Toast.makeText(this, "Please fill the blank form", Toast.LENGTH_SHORT).show()
            return
        }

        val data = Friend(name, school, hobby, photoStr)
        lifecycleScope.launch {
            viewModel.insertFriend(data)
        }
        finish()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_", ".jpg", storageDir)
    }

    private fun bitmapToString(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun stringToBitmap(encodedString: String): Bitmap? {
        return try {
            val byteArray = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoURI = FileProvider.getUriForFile(this, "com.example.datateman.fileprovider", photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        cameraLauncher.launch(takePictureIntent)
    }
}
