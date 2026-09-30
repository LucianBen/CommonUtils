package com.luxu.commonutils.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.luxu.commonutils.crypto.DeviceVaultKey
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.luxu.commonutils.*
import com.luxu.commonutils.base.BaseActivity
import com.luxu.commonutils.data.VaultRepository
import com.luxu.commonutils.utils.CommonUtils
import kotlinx.coroutines.*
import java.io.File

class MainActivity : BaseActivity(), SearchView.OnQueryTextListener, PassListAdapter.OnItemClickListener {
    private lateinit var list: RecyclerView
    private lateinit var search: SearchView
    private var adapter: PassListAdapter? = null
    private lateinit var status: TextView
    private var unlockJob: Job? = null
    private var started = false
    private var busy = false
    private var loadGeneration = 0
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && VaultSession.repository != null) loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Toolbar>(R.id.toolbar).also { it.title = "密码管理器"; setSupportActionBar(it) }
        status = findViewById(R.id.vault_status)
        list = findViewById(R.id.recyclerview)
        list.layoutManager = LinearLayoutManager(this)
        search = findViewById(R.id.searchView)
        search.setIconifiedByDefault(false)
        search.queryHint = "查找网站"
        search.setOnQueryTextListener(this)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            if (VaultSession.repository == null) openVault()
            else if (!busy) launcher.launch(Intent(this, AddPsdActivity::class.java))
        }
        findViewById<FloatingActionButton>(R.id.fab2).setOnClickListener {
            launcher.launch(Intent(this, ClockActivity::class.java))
        }
        clearList()
    }
    override fun onStart() { super.onStart(); started = true }
    override fun onResume() {
        super.onResume()
        if (VaultSession.repository == null) openVault() else loadData()
    }
    override fun onStop() {
        started = false
        unlockJob?.cancel()

        clearList()
        super.onStop()
    }
    private fun clearList() {
        loadGeneration++
        list.adapter = null; adapter = null
        list.visibility = View.INVISIBLE; search.visibility = View.INVISIBLE
    }
    private fun openVault() {
        if (!started || unlockJob?.isActive == true) return
        clearList()
        status.text = "正在读取密码库…"
        status.visibility = View.VISIBLE
        unlockJob = uiScope.launch {
            var opened: VaultRepository? = null
            try {
                withContext(Dispatchers.IO) {
                    opened = VaultRepository.openDevice(filesDir, DeviceVaultKey::get)
                }
                ensureActive()
                if (!started) return@launch
                VaultSession.unlock(checkNotNull(opened)); opened = null
                loadData()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                status.text = "密码库读取失败，原文件未清空。点击新增按钮可重试。若已有旧加密库，需要单独迁移。"
            } finally { opened?.close() }
        }
    }
    private fun loadData() {
        val repository = VaultSession.repository ?: return
        val generation = ++loadGeneration
        uiScope.launch {
            try {
                val (records, legacyRetained) = withContext(Dispatchers.IO) {
                    repository.load() to File(filesDir, "need.json").exists()
                }
                if (started && generation == loadGeneration && VaultSession.repository === repository) {
                    adapter = PassListAdapter(ArrayList(records), this@MainActivity).also {
                        it.setOnItemClickListener(this@MainActivity)
                        it.searchData(search.query.toString().trim())
                    }
                    status.text = if (records.isEmpty()) "密码库暂无记录" else ""
                    if (legacyRetained) {
                        status.text = "旧 need.json 按你的选择保留，仍包含明文密码。" +
                            if (records.isEmpty()) "密码库暂无记录" else ""
                    }
                    status.visibility = if (status.text.isEmpty()) View.GONE else View.VISIBLE
                    list.adapter = adapter
                    list.visibility = View.VISIBLE; search.visibility = View.VISIBLE
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (started && generation == loadGeneration) {
                    clearList(); status.visibility = View.VISIBLE; status.text = "密码库读取失败，原文件未清空，请检查文件。"
                }
            }
        }
    }
    override fun onClick(view: View, value: PasswordBean, pos: Int) {
        if (VaultSession.repository != null) CommonUtils.copyToClipboard(this, value.password)
    }
    override fun onLongClick(view: View, value: PasswordBean, pos: Int) {
        if (busy || VaultSession.repository == null) return
        AlertDialog.Builder(this).setTitle("请选择操作")
            .setPositiveButton("编辑") { _, _ ->
                launcher.launch(Intent(this, AddPsdActivity::class.java).putExtra("id", value.id))
            }.setNegativeButton("删除") { _, _ ->
                val repository = VaultSession.repository ?: return@setNegativeButton
                busy = true
                uiScope.launch {
                    try { withContext(Dispatchers.IO) { repository.delete(value.id) }; loadData() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { showError("删除失败，未更新列表，请重试。") }
                    finally { busy = false }
                }
            }.show()
    }
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = Unit
    override fun onQueryTextSubmit(query: String?) = false
    override fun onQueryTextChange(text: String?): Boolean { adapter?.searchData(text.orEmpty().trim()); return true }
}
