package com.example.datateman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datateman.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: FriendViewModel
    private lateinit var adapter: AdapterRVFriend
    private var friendList = ArrayList<Friend>()

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

    private fun showEditFriendDialog(friend: Friend) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_friend, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_edit_name)
        val etSchool = dialogView.findViewById<EditText>(R.id.et_edit_school)
        val etHobby = dialogView.findViewById<EditText>(R.id.et_edit_hobby)

        etName.setText(friend.name)
        etSchool.setText(friend.school)
        etHobby.setText(friend.hobby)

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
}
