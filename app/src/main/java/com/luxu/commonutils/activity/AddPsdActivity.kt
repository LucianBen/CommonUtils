package com.luxu.commonutils.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.luxu.commonutils.R
import com.luxu.commonutils.VaultSession
import com.luxu.commonutils.base.BaseActivity
import com.luxu.commonutils.utils.CommonUtils
import kotlinx.coroutines.*
import java.util.UUID

/** Rotation-only draft: never serialize password inputs to saved-state bundles. */
class PasswordDraft : ViewModel() {
    val newRecordId = UUID.randomUUID().toString()
    var fields: List<String>? = null
    override fun onCleared() { fields = null }
}

class AddPsdActivity : BaseActivity(), View.OnClickListener {
    private lateinit var webName: EditText
    private lateinit var account: EditText
    private lateinit var password: EditText
    private lateinit var remark: EditText
    private lateinit var confirm: Button
    private var id: String? = null
    private var initialized = false
    private lateinit var draft: PasswordDraft
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VaultSession.repository == null) { finish(); return }
        setContentView(R.layout.activity_add_psd)
        id = intent.getStringExtra("id")
        webName = findViewById(R.id.webName); account = findViewById(R.id.account)
        password = findViewById(R.id.password); remark = findViewById(R.id.remark)
        confirm = findViewById(R.id.confirm)
        draft = ViewModelProvider(this)[PasswordDraft::class.java]
        listOf(webName, account, password, remark).forEach { it.isSaveEnabled = false }
        confirm.setOnClickListener(this)
        findViewById<TextView>(R.id.randomPsd).setOnClickListener(this)
        findViewById<TextView>(R.id.copy).setOnClickListener(this)
        findViewById<Toolbar>(R.id.toolbar).also {
            it.title = if (id == null) "添加密码" else "编辑密码"; setSupportActionBar(it)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val repository = VaultSession.repository ?: return
        confirm.isEnabled = false
        uiScope.launch {
            try {
                val retained = draft.fields
                if (retained != null) {
                    webName.setText(retained[0]); account.setText(retained[1])
                    password.setText(retained[2]); remark.setText(retained[3])
                } else if (id != null) {
                    val entry = withContext(Dispatchers.IO) {
                        repository.load().singleOrNull { it.id == id } ?: error("记录不存在")
                    }
                    webName.setText(entry.webName); account.setText(entry.account)
                    password.setText(entry.password); remark.setText(entry.remark)
                }
                initialized = true; confirm.isEnabled = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { showError("无法读取记录，请返回解锁密码库后重试。") }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (initialized) draft.fields = listOf(webName.text.toString(), account.text.toString(),
            password.text.toString(), remark.text.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); if (VaultSession.repository == null) finish() }
    override fun onClick(view: View?) {
        val repository = VaultSession.repository ?: run { finish(); return }
        when (view?.id) {
            R.id.confirm -> {
                if (!initialized || !confirm.isEnabled) return
                if (webName.text.isBlank() || account.text.isBlank() || password.text.isEmpty()) {
                    showError("请填写网站、账户和密码。"); return
                }
                val web = webName.text.toString(); val user = account.text.toString()
                val secret = password.text.toString(); val notes = remark.text.toString()
                confirm.isEnabled = false
                uiScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.save(id, web, user, secret, notes, draft.newRecordId)
                        }
                        setResult(RESULT_OK, Intent().putExtra("value", true)); finish()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        confirm.isEnabled = true
                        showError("保存失败，输入内容仍保留，请重试；若密码库已锁定，请返回解锁。")
                    }
                }
            }
            R.id.copy -> CommonUtils.copyToClipboard(this, password.text.toString())
            R.id.randomPsd -> password.setText(CommonUtils.getRandomPassword())
        }
    }
}
