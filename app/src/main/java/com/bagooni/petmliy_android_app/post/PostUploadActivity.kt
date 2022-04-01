package com.bagooni.petmliy_android_app.post

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import com.bagooni.petmliy_android_app.R
import com.bagooni.petmliy_android_app.databinding.ActivityAlbumBinding
import com.bagooni.petmliy_android_app.databinding.ActivityPostUploadBinding
import com.bumptech.glide.Glide

class PostUploadActivity : AppCompatActivity() {
    var imageUri : Uri? = null
    private lateinit var binding: ActivityPostUploadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPostUploadBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.closeButton.setOnClickListener{ finish() }

        val postImg = binding.postImg
        val glide = Glide.with(this)

        val imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                imageUri = it.data!!.data
                glide.load(imageUri).centerCrop().into(postImg)
            }
        imagePickerLauncher.launch(
            Intent(Intent.ACTION_PICK).apply {
                this.type = MediaStore.Images.Media.CONTENT_TYPE
            }
        )

    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }


}