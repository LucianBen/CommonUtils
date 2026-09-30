package com.luxu.commonutils.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.luxu.commonutils.R
import com.luxu.commonutils.base.BaseActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.password_entry).setOnClickListener {
            startActivity(Intent(this, PasswordActivity::class.java))
        }
        findViewById<View>(R.id.water_entry).setOnClickListener {
            startActivity(Intent(this, ClockActivity::class.java))
        }
        findViewById<View>(R.id.accounting_entry).setOnClickListener {
            Toast.makeText(this, R.string.feature_in_development, Toast.LENGTH_SHORT).show()
        }
    }
}
