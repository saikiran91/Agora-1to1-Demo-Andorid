package io.agora.demo1to1

import android.databinding.ObservableArrayList
import com.github.nitrico.lastadapter.LastAdapter
import getSimpleTime
import io.agora.demo1to1.databinding.ItemMessageBinding
import kotlinx.android.synthetic.main.item_message.view.*
import java.util.*

class ChatAdapter {
    val listOfChat: ObservableArrayList<Chat> = ObservableArrayList()
    val adapter: LastAdapter by lazy { initAdapter() }
    private fun initAdapter() = LastAdapter(listOfChat, BR.chat)
            .map<Chat, ItemMessageBinding>(R.layout.item_message) {
                onBind { item ->
                    item.itemView.time_tv.text = Date(item.binding.chat!!.sendDate).getSimpleTime()
                }
            }
}