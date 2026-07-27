package com.luxu.commonutils.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.luxu.commonutils.R
import com.luxu.commonutils.ReadWriteFile
import com.luxu.commonutils.base.BaseActivity
import com.luxu.commonutils.utils.CommonUtils
import kotlin.properties.Delegates

class AddPsdActivity : BaseActivity(), View.OnClickListener {
    private lateinit var webName: EditText
    private lateinit var account: EditText
    private lateinit var password: EditText
    private lateinit var remark: EditText
    private lateinit var randomPsd: TextView
    private var id by Delegates.notNull<Int>()
    private var pos by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_psd)

        initView()
        initData()
    }

    private fun initData() {
        id = intent.getIntExtra("id", -1)
        pos = intent.getIntExtra("pos", -1)
        webName.setText(intent.getStringExtra("webName"))
        account.setText(intent.getStringExtra("account"))
        password.setText(intent.getStringExtra("password"))
        remark.setText(intent.getStringExtra("remark"))

    }

    private fun initView() {
        webName = findViewById(R.id.webName)
        account = findViewById(R.id.account)
        password = findViewById(R.id.password)
        randomPsd = findViewById(R.id.randomPsd)
        remark = findViewById(R.id.remark)
        randomPsd.setOnClickListener(this)
        findViewById<Button>(R.id.confirm).setOnClickListener(this)
        findViewById<TextView>(R.id.copy).setOnClickListener(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle("添加密码")
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.confirm -> {
                if (id == -1) {
                    ReadWriteFile.writeNewJsonData(
                        webName.text.toString(), account.text.toString(),
                        password.text.toString(), remark.text.toString()
                    )
                } else {
                    ReadWriteFile.changeJsonData(
                        pos,
                        webName.text.toString(), account.text.toString(),
                        password.text.toString(), remark.text.toString()
                    )
                }
                val intent = Intent().putExtra("value", true)
                setResult(RESULT_OK, intent)
                finish()
            }

            R.id.copy -> CommonUtils.copyToClipboard(this, password.text.toString())

            R.id.randomPsd -> password.setText(CommonUtils.getRandomPassword())

        }
    }


}