package com.myra.assistant.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.R

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        val last = messages.lastOrNull()
        if (last != null && !last.isUser && !msg.isUser && last.text == msg.text) return
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun lastMyraText(): String? = messages.lastOrNull { !it.isUser }?.text

    override fun getItemCount() = messages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_myra
        return ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemViewType(pos: Int) = if (messages[pos].isUser) 1 else 0

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.textView.text = messages[pos].text
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}
