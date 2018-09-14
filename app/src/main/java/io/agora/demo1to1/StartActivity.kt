package io.agora.demo1to1

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import hideKeyboard
import io.agora.demo1to1.VideoChatViewActivity.*
import io.agora.rtc.Constants
import kotlinx.android.synthetic.main.activity_start.*
import java.util.*

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun joinChannelOnClick(view: View) {
        val channelName = channel_et.text.toString()
        val userName = name_et.text.toString()
        if (channelName.isNotBlank() && userName.isNotBlank()) {
            showBroadcastOrViewerSelection()
        } else {
            Toast.makeText(this, "Channel ID or Username cannot be empty",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBroadcastOrViewerSelection() {
        val channelName = channel_et.text.toString()
        val userName = name_et.text.toString()

        val builder = AlertDialog.Builder(this)
        val choice = arrayOf("Broadcast", "View")
        val checkedItem = 0
        var clientType = Constants.CLIENT_ROLE_BROADCASTER

        builder.setSingleChoiceItems(choice, checkedItem) { text, which ->
            clientType = if (which == 0) Constants.CLIENT_ROLE_BROADCASTER
            else Constants.CLIENT_ROLE_AUDIENCE
        }

        builder.setPositiveButton("JOIN") { _, which ->
            startActivity(Intent(this, VideoChatViewActivity::class.java)
                    .apply {
                        putExtra(USER_NAME, userName)
                        putExtra(CLIENT_TYPE, clientType)
                        putExtra(CHANNEL_ID_KEY, channelName)
                    })
        }

        val dialog = builder.create()
        dialog.show()
        channel_et.hideKeyboard()
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
