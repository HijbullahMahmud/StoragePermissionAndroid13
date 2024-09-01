package com.example.storagetest

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_CODE_PERMISSION = 100

    private val multiplePermissionList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayListOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        checkPermission();
    }

    private fun checkPermission() {
        //Android is 11 or above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
           if(Environment.isExternalStorageManager()){
               //permission granted
               loadMp4FilesFromDownloadFolder()
           }else{
               try {
                   val intent = Intent()
                   intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                   val uri = Uri.fromParts("package", packageName, null)
                   intent.data = uri
                   storageActivityResultLauncher.launch(intent)

               }catch (e: Exception){
                   val intent = Intent()
                   intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                   storageActivityResultLauncher.launch(intent)
               }
           }
        }else{
            //Android is below 11
            ActivityCompat.requestPermissions(this, multiplePermissionList.toTypedArray(), REQUEST_CODE_PERMISSION)
        }

    }

    private val storageActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(Environment.isExternalStorageManager()) {
                Log.d(TAG, "onActivityResult: Manage External Storage Permissions Granted");
                if(result.resultCode == RESULT_OK){
                    loadMp4FilesFromDownloadFolder()
                }
            }else{
                Log.d(TAG, "onActivityResult: Manage External Storage Permissions Denied");
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            // Check if the request was granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, load the MP4 files
                loadMp4FilesFromDownloadFolder()
            } else {
                // Permission was denied, show a message to the user
                Toast.makeText(this, "Permission denied. Cannot access files.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun loadMp4FilesFromDownloadFolder() {
        val downloadFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val videoFiles = downloadFolder.listFiles { file ->
            file.extension.equals("png", ignoreCase = true) ||
                    file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true)
        }?.map { file ->
            VideoFile(file.name, file.absolutePath)
        } ?: emptyList()
        for (videoFile in videoFiles) {
            Log.e(TAG, "loadMp4Files: ${videoFile.path}")
        }

//        adapter = VideoAdapter(videoFiles) { videoFile ->
//            // Handle click on video file, e.g., play the video
//            playVideo(videoFile)
//        }
//        recyclerView.adapter = adapter

        loadAllImages()
        loadAllVideos()
    }

    private fun loadAllImages(){
        val imageList = mutableListOf<VideoFile>()

        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA // or MediaStore.Images.Media.RELATIVE_PATH for newer devices
        )

        val selection = "${MediaStore.Images.Media.MIME_TYPE} IN (?, ?, ?)"
        val selectionArgs = arrayOf(
            "image/png",
            "image/jpeg",
            "image/jpg"
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn)
                imageList.add(VideoFile(name, path))
                Log.e(TAG, "loadAllImages: $path")
            }
        }

        // Uncomment and set up RecyclerView if needed
        // adapter = VideoAdapter(imageList) { videoFile ->
        //     playVideo(videoFile)
        // }
        // recyclerView.adapter = adapter
    }

    private fun loadAllVideos() {
        val videoList = mutableListOf<VideoFile>()

        val projection = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA // or MediaStore.Video.Media.RELATIVE_PATH for newer devices
        )

        val selection = "${MediaStore.Video.Media.MIME_TYPE} IN (?, ?, ?, ?)"
        val selectionArgs = arrayOf(
            "video/mp4",
            "video/avi",
            "video/mkv",
            "video/webm"
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn)
                videoList.add(VideoFile(name, path))
                Log.e(TAG, "loadAllVideos: $path")
            }
        }

        // Uncomment and set up RecyclerView if needed
        // adapter = VideoAdapter(videoList) { videoFile ->
        //     playVideo(videoFile)
        // }
        // recyclerView.adapter = adapter
    }

}