package com.luxu.commonutils.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.luxu.commonutils.PassListAdapter
import com.luxu.commonutils.PasswordBean
import com.luxu.commonutils.R
import com.luxu.commonutils.ReadWriteFile
import com.luxu.commonutils.base.BaseActivity
import com.luxu.commonutils.utils.CommonUtils
import java.util.concurrent.Executor


class MainActivity : BaseActivity(), OnQueryTextListener, View.OnClickListener,
    PassListAdapter.OnItemClickListener {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var mAdapter: PassListAdapter? = null
    private lateinit var recyclerview: RecyclerView
    private lateinit var search: SearchView
    private lateinit var actButton: FloatingActionButton
    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val value = it.data?.getBooleanExtra("value", false)
                if (value == true) {
                    loadData()
                    mAdapter?.notifyDataSetChanged()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        initBiometric()

        initView()
        loadData()
    }

    private fun loadData() {
        val value = ReadWriteFile.readJsonData(filesDir.absolutePath + "/need.json")

        mAdapter = PassListAdapter(value, this)
        recyclerview.adapter = mAdapter
        mAdapter!!.setOnItemClickListener(this)
    }

    private fun initView() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "密码管理器"
        setSupportActionBar(toolbar)

        findViewById<FloatingActionButton>(R.id.fab2).setOnClickListener(this)

        actButton = findViewById(R.id.fab)
        actButton.setOnClickListener(this)
        recyclerview = findViewById(R.id.recyclerview)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerview.setLayoutManager(linearLayoutManager)

        search = findViewById(R.id.searchView)
        search.setIconifiedByDefault(false)
        search.queryHint = "查找网站"
        search.setOnQueryTextListener(this)

    }

    /*
    *      方法
    * */

    fun moreAlertDialog(value: PasswordBean, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("更多")
        builder.setMessage("请选择操作")
        builder.setPositiveButton("编辑") { _, _ ->
            val intent = Intent(this@MainActivity, AddPsdActivity::class.java)
            intent.putExtra("webName", value.webName)
            intent.putExtra("account", value.account)
            intent.putExtra("password", value.password)
            intent.putExtra("remark", value.remark)
            intent.putExtra("id", value.id)
            intent.putExtra("pos", position)
            launcher.launch(intent)
        }
        builder.setNegativeButton("删除") { _, _ ->
            ReadWriteFile.deleteJsonData(position)
            mAdapter!!.changeData(position)
        }
        val alert = builder.create()
        alert.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab -> {
                val intent = Intent(this, AddPsdActivity::class.java)
                intent.putExtra("id", -1)
                launcher.launch(intent)
            }
            R.id.fab2 ->{
                val intent = Intent(this, ClockActivity::class.java)
                intent.putExtra("id", -1)
                launcher.launch(intent)
            }
        }
    }

    override fun onClick(view: View, value: PasswordBean, pos: Int) {
        CommonUtils.copyToClipboard(this@MainActivity, value.password)
    }

    override fun onLongClick(view: View, value: PasswordBean, pos: Int) {
        moreAlertDialog(value, pos)
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
    }

    /*
    *  指纹识别
    * */
    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, authenticationCallback)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("指纹验证")
            .setSubtitle("请验证指纹")
            .setNegativeButtonText("取消")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // 指纹识别设备可用，可以开始指纹识别
    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            // 指纹识别过程中出现错误
            Toast.makeText(this@MainActivity, "取消识别", Toast.LENGTH_SHORT).show()
            biometricPrompt.authenticate(promptInfo)
            actButton.visibility = View.GONE
            search.visibility = View.GONE
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // 指纹识别成功
            actButton.visibility = View.VISIBLE
            search.visibility = View.VISIBLE
            loadData()
        }

        override fun onAuthenticationFailed() {
            // 指纹识别失败
            Toast.makeText(this@MainActivity, "识别失败", Toast.LENGTH_SHORT).show()

        }
    }

    /*
    *   搜索框
    * */
    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        println(newText)
        newText?.trim()?.let { mAdapter!!.searchData(it) }
        return false
    }


}