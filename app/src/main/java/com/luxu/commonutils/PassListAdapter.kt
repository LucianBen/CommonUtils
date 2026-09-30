package com.luxu.commonutils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class PassListAdapter(dl: ArrayList<PasswordBean>, mContext: Context) :
    RecyclerView.Adapter<PassListAdapter.MyViewHolder>() {

    private var passList = ArrayList<PasswordBean>()
    private var allPassList: ArrayList<PasswordBean>? = null
    private var mContext: Context
    private lateinit var onItemClickListener: OnItemClickListener

    init {
        this.mContext = mContext
        allPassList = dl
        searchData("")
    }

    fun searchData(text: String) {
        passList.clear()
        allPassList?.forEach {
            if (it.webName.contains(text) || text.isEmpty()) {
                passList.add(it)
            }
        }

        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return passList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PassListAdapter.MyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.passlist_item, parent, false)
        val mViewHolder = MyViewHolder(view)

        view.findViewById<View>(R.id.copy_password).setOnClickListener {
            val position = mViewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION && position in passList.indices)
                onItemClickListener.onClick(it, passList[position], position)
        }
        onItemClickListener.let {
            mViewHolder.itemClick!!.apply {
                setOnClickListener {
                    val position: Int = mViewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION && position in passList.indices)
                        onItemClickListener.onClick(it, passList[position], position)
                }
                setOnLongClickListener {
                    val position: Int = mViewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION && position in passList.indices)
                        onItemClickListener.onLongClick(it, passList[position], position)
                    true
                }
            }
        }
        return mViewHolder

    }

    override fun onBindViewHolder(holder: PassListAdapter.MyViewHolder, position: Int) {
        holder.siteInitial.text = passList[position].webName.take(1).uppercase()
        holder.account!!.text = passList[position].account
        holder.webName!!.text = passList[position].webName
        holder.password!!.text = passList[position].password
        val remarkText = passList[position].remark

        if (remarkText.isNotEmpty()) {
            holder.remark1!!.visibility = View.VISIBLE
            holder.remark!!.visibility = View.VISIBLE
            holder.remark!!.text = remarkText
        } else {
            holder.remark!!.visibility = View.GONE
            holder.remark1!!.visibility = View.GONE
        }

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class MyViewHolder(view: View) : ViewHolder(view) {
        val siteInitial: TextView = view.findViewById(R.id.site_initial)
        var webName: TextView? = null
        var account: TextView? = null
        var password: TextView? = null
        var remark: TextView? = null
        var remark1: TextView? = null
        var itemClick: ConstraintLayout? = null

        init {
            webName = view.findViewById(R.id.webName)
            account = view.findViewById(R.id.account)
            password = view.findViewById(R.id.password)
            itemClick = view.findViewById(R.id.item_click)
            remark = view.findViewById(R.id.remark)
            remark1 = view.findViewById(R.id.remark_1)
        }

    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener : AdapterView.OnItemClickListener {
        fun onClick(view: View, value: PasswordBean, pos: Int)
        fun onLongClick(view: View, value: PasswordBean, pos: Int)
    }


}
