package io.agora.demo1to1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import io.agora.demo1to1.VideoChatViewActivity.APP_ID_KEY
import io.agora.demo1to1.VideoChatViewActivity.CHANNEL_ID_KEY
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.activity_start.*
import java.util.*


class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        appid_et.setText(getString(R.string.agora_app_id))
        val sdkVersion = "SDK V " + RtcEngine.getSdkVersion()
        sdk_version_et.text = sdkVersion

    }

    fun joinChannelOnClick(view: View) {


        val appId = appid_et.text.toString().apply {
            if (isBlank()) {
                Toast.makeText(this@StartActivity, "App ID cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val channelName = channel_et.text.toString().apply {
            if (isBlank()) {
                Toast.makeText(this@StartActivity, "Channel ID cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }
        }

        startActivity(Intent(this, VideoChatViewActivity::class.java)
                .apply {
                    putExtra(CHANNEL_ID_KEY, channelName)
                    putExtra(APP_ID_KEY, appId)
                })

    }


    fun generateRandomOnClick(view: View) {
        val randomText = UUID.randomUUID().toString().replace("-", "").trim().substring(0, 6)
        channel_et.setText(randomText)
        channel_et.setSelection(channel_et.text.length)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Channel ID", randomText)
        clipboard.primaryClip = clip
        Toast.makeText(this, "Channel ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
