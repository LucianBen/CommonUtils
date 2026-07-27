package com.luxu.commonutils.activity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.luxu.commonutils.R
import com.luxu.commonutils.WaterWorker
import java.util.concurrent.TimeUnit


class ClockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock)


        findViewById<Button>(R.id.click).setOnClickListener {
//            5e702c08-1027-4a75-8a5f-98d013e133fa
            val waterRequest: WorkRequest =
                PeriodicWorkRequestBuilder<WaterWorker>(15, TimeUnit.MINUTES)
                    .addTag("WaterMinder")
                    .build()
            WorkManager
                .getInstance(this)
                .enqueue(waterRequest)

        }


    }
}