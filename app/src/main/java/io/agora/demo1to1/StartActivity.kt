package io.agora.demo1to1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import io.agora.demo1to1.VideoChatViewActivity.CHANNEL_ID_KEY
import kotlinx.android.synthetic.main.activity_start.*
import java.util.*


class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun joinChannelOnClick(view: View) {
        val channelName = channel_et.text.toString()
        if (channelName.isNotBlank()) {
            startActivity(Intent(this,
                    VideoChatViewActivity::class.java).apply { putExtra(CHANNEL_ID_KEY, channelName) })
        } else {
            Toast.makeText(this, "Channel ID cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }


    fun generateRandomOnClick(view: View) {
        val randomText = UUID.randomUUID().toString()
        channel_et.setText(randomText)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Channel ID", randomText)
        clipboard.primaryClip = clip
        Toast.makeText(this, "Channel ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
