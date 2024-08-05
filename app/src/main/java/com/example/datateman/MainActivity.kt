package com.example.datateman

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datateman.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: FriendViewModel
    private lateinit var adapter: AdapterRVFriend
    private var friendList = ArrayList<Friend>()
    private lateinit var photoFile: File
    private lateinit var dialogView: android.view.View

    private var galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val bitmap = uriToBitmap(uri)
                    updateImageViewAndFriend(bitmap)
                }
            }
        }

    private var cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
                updateImageViewAndFriend(takenImage)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewModelFactory = FriendVMFactory(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[FriendViewModel::class.java]

        adapter = AdapterRVFriend(this, friendList, { data ->
            showFriendDetails(data)
        }, { data ->
            showEditFriendDialog(data)
        }, { data ->
            deleteFriend(data)
        })
        binding.rvShowData.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getFriend().collect { friends ->
                        Log.d("DATABASE", "Friends: $friends")
                        friendList.clear()
                        friendList.addAll(friends)
                        adapter.setData(friendList)
                    }
                }
            }
        }

        binding.ftbnAdd.setOnClickListener {
            val destination = Intent(this, AddFriendActivity::class.java)
            startActivity(destination)
        }
    }

    private fun showFriendDetails(friend: Friend) {
        AlertDialog.Builder(this)
            .setTitle(friend.name)
            .setMessage("School: ${friend.school}\nHobby: ${friend.hobby}")
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressLint("InflateParams")
    private fun showEditFriendDialog(friend: Friend) {
        dialogView = layoutInflater.inflate(R.layout.dialog_edit_friend, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_edit_name)
        val etSchool = dialogView.findViewById<EditText>(R.id.et_edit_school)
        val etHobby = dialogView.findViewById<EditText>(R.id.et_edit_hobby)
        val ivPicture = dialogView.findViewById<ImageView>(R.id.iv_pictures)
        val btnSelectFromGallery = dialogView.findViewById<Button>(R.id.btn_Gallery)
        val btnTakePhoto = dialogView.findViewById<Button>(R.id.btn_camera)
        val btnDeleteImage = dialogView.findViewById<Button>(R.id.delete)

        etName.setText(friend.name)
        etSchool.setText(friend.school)
        etHobby.setText(friend.hobby)

        // Set the existing image if available
        val photoBitmap = stringToBitmap(friend.photo)
        photoBitmap?.let {
            ivPicture.setImageBitmap(it)
        }

        btnSelectFromGallery.setOnClickListener {
            selectImageFromGallery()
        }

        btnTakePhoto.setOnClickListener {
            takePhotoWithCamera()
        }

        // Add delete image functionality
        btnDeleteImage.setOnClickListener {
            ivPicture.setImageResource(android.R.color.transparent) // Clear the ImageView
            friend.photo = "" // Clear the photo field in the Friend object
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Friend")
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val updatedName = etName.text.toString().trim()
                val updatedSchool = etSchool.text.toString().trim()
                val updatedHobby = etHobby.text.toString().trim()

                if (updatedName.isNotEmpty() && updatedSchool.isNotEmpty() && updatedHobby.isNotEmpty()) {
                    friend.name = updatedName
                    friend.school = updatedSchool
                    friend.hobby = updatedHobby

                    // Update the photo if changed
                    val bitmap = (ivPicture.drawable as? BitmapDrawable)?.bitmap
                    friend.photo = if (bitmap != null) bitmapToString(bitmap) else ""

                    lifecycleScope.launch {
                        viewModel.updateFriend(friend)
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFriend(friend: Friend) {
        AlertDialog.Builder(this)
            .setTitle("Delete Friend")
            .setMessage("Are you sure you want to delete ${friend.name}?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteFriend(friend)
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun selectImageFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    private fun takePhotoWithCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Cannot Create Image File", Toast.LENGTH_SHORT).show()
            return
        }
        val photoURI = FileProvider.getUriForFile(this, "com.example.datateman.fileprovider", photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        cameraLauncher.launch(takePictureIntent)
    }

    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_", ".jpg", storageDir)
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IOException("Failed to convert URI to Bitmap")
    }

    private fun bitmapToString(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun stringToBitmap(encodedString: String): Bitmap? {
        return try {
            val byteArray = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun updateImageViewAndFriend(bitmap: Bitmap) {
        val ivPicture = dialogView.findViewById<ImageView>(R.id.iv_pictures)
        ivPicture.setImageBitmap(bitmap)
    }
}
