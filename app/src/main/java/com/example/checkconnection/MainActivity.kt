package com.example.checkconnection

import android.app.AlarmManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private val UPDATE_INTERVAL = 5000L
    private val updateWidgetHandler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setWork()
    }

    private fun setWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)

        val myWorkRequest =
            PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(myWorkRequest)

    }

    private var updateWidgetRunnable: Runnable = Runnable {
        run {
            //Update UI
            setWork()
            // Re-run it after the update interval

            updateWidgetHandler.postDelayed(updateWidgetRunnable, UPDATE_INTERVAL)
        }

    }


    override fun onResume() {
        super.onResume()
        updateWidgetHandler.postDelayed(updateWidgetRunnable, UPDATE_INTERVAL)
    }

    override fun onPause() {
        super.onPause()
        updateWidgetHandler.removeCallbacks(updateWidgetRunnable);
    }

}