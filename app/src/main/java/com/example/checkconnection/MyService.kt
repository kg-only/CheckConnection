package com.example.checkconnection

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.checkconnection.Constants.CHANNEL_ID
import com.example.checkconnection.Constants.MUSIC_NOTIFICATION_ID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate


class MyService : Service() {

    private lateinit var musicPlayer: MediaPlayer
    private var gson: Gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)
    private lateinit var listModel: List<Model>
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initMusic()
        notificationChannel()
        listModel = getModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()

        Timer().scheduleAtFixedRate(0, 300000L) {
            if (isOnline(this@MyService)) {
                coroutineScope.launch {
                    makeRequest()
                }
            } else {
                sendNotification("Отсутствует интернет покдлючение")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun makeRequest() = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .build()

        for (urls in listModel) {
            if (urls.requestTime()) {
                urls.updateLastTime()

                val request = Request.Builder().url(urls.url).build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 200) {
                            Log.e("###", "Request success ")
                        }
                        if (response.body != null)
                            response.close()
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        sendNotification(urls.url + " ошибка соединения")
                        vibratePhone()
                        musicPlayer.start()
                        Timer().schedule(2000) {
                            if (musicPlayer.isPlaying) {
                                musicPlayer.pause()
                            }
                        }
                        Log.e("###", "Request error ")
                    }
                })
            }
        }
    }

    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_wifi_24)
                .setContentText("Работа в фоне")
                .setContentIntent(pendingIntent)
                .build()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        startForeground(MUSIC_NOTIFICATION_ID, notification)
    }

    private fun initMusic() {
        musicPlayer = MediaPlayer.create(this, R.raw.ultrazvuk)
        musicPlayer.isLooping = true
        musicPlayer.setVolume(100F, 100F)
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        } else {
            TODO("VERSION.SDK_INT < M")
        }
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun notificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "title"
            val descriptionText = "desc"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(text: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_wifi_24)
            .setContentTitle("Check Connection")
            .setContentText(text)
            .setVibrate(longArrayOf(300, 300))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this)) {
            notify(12345, builder.build())
        }
        //turn on the screen
        val pm = this.getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) pm.isInteractive else pm.isScreenOn // check if screen is on

        if (!isScreenOn) {
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "myApp:notificationLock"
            )
            wl.acquire(3000) //set your time in milliseconds
        }
    }

    private fun getModel(): List<Model> {
        val file = File(
            Environment.getExternalStorageDirectory(),
            "/Download/Telegram/checkConnection.txt"
        )
        val contents = file.readText() // Read file
        val listPersonType = object : TypeToken<List<Model>>() {}.type
        return gson.fromJson(contents, listPersonType)
    }
}