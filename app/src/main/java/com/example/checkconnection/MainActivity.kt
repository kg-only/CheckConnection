package com.example.checkconnection

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.checkconnection.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val binding by viewBinding(ActivityMainBinding::bind)
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    private lateinit var adapter: MyAdapter
    private var gson: Gson = Gson()
    private var mutableLiveData = MutableLiveData<List<Model>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //request android 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            }
            val uri: Uri = Uri.fromParts("package", this.packageName, null)
            intent.data = uri
            startActivity(intent)
            verifyStoragePermissions(this)
            getModel()

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            verifyStoragePermissions(this)
            getModel()
        }

        //
//        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                Environment.isExternalStorageManager()
//            } else {
//                TODO("VERSION.SDK_INT < R")
//            }
//        ) {
//            listModel = getModel()
//            verifyStoragePermissions(this)
//        } else {
//            val intent = Intent()
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//            }
//            val uri: Uri = Uri.fromParts("package", this.packageName, null)
//            intent.data = uri
//            startActivity(intent)
//        }

        binding.button.setOnClickListener { startAndStopService() }
        binding.buttonCheck.setOnClickListener { makeRequest() }

        adapter = MyAdapter()
        initRecycler()
    }

    private fun startAndStopService() {
        if (isMyServiceRunning(MyService::class.java)) {
            binding.button.text = "Фоновый режим отключен"
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
            stopService(Intent(this, MyService::class.java))
        } else {
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
            startService(Intent(this, MyService::class.java))
            binding.button.text = "Фоновый режим включен"
        }
    }

    private fun isMyServiceRunning(myClass: Class<MyService>): Boolean {
        val manager: ActivityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service: ActivityManager.RunningServiceInfo in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (myClass.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }

    private fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun initRecycler() {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.HORIZONTAL
            )
        )

    }

    private fun getModel(): LiveData<List<Model>> {
        val file = File(
            Environment.getExternalStorageDirectory(),
            "/Download/Telegram/checkConnection.txt"
        )
        val contents = file.readText() // Read file
        val listPersonType = object : TypeToken<List<Model>>() {}.type
        mutableLiveData.value = gson.fromJson(contents, listPersonType)
        return mutableLiveData
    }

    private fun makeRequest() {
        mutableLiveData.observe(this) {
            adapter.setItems(it)
            for (urls in it) {
                val request = Request.Builder().url(urls.url).build()
                val client = OkHttpClient()

                client.newCall(request).enqueue(object : Callback {
                    var mainHandler = Handler(this@MainActivity.mainLooper)
                    override fun onResponse(call: Call, response: Response) {
                        mainHandler.post {
                            if (response.code == 200) {
                                runOnUiThread {
                                    run {
                                        Toast.makeText(
                                            applicationContext,
                                            "Success ${urls.url}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                Log.e("###", "Request success ")
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            run {
                                Toast.makeText(
                                    applicationContext,
                                    "Error ${urls.url}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        Log.e("###", "Request error ")
                    }
                })
            }
        }
    }

}