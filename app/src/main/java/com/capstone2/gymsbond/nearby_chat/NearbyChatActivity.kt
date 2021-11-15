package com.capstone2.gymsbond.nearby_chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone2.gymsbond.R
import com.capstone2.gymsbond.interaction_chat.ui.RegisterActivity
import com.capstone2.gymsbond.nearby_chat.login.LoginActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.MessageListener
import kotlinx.android.synthetic.main.content_main2.*
import kotlinx.android.synthetic.main.ready_to_go.*
import com.google.android.gms.nearby.messages.Message
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.item_user.*
import java.util.*

class NearbyChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "NearbyChatActivity"
    }

    private var loginTime = System.currentTimeMillis()
    private lateinit var  userUUID: String
    private lateinit var messageListener: MessageListener
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var logoutDialog: AlertDialog.Builder
    private lateinit var activeMessage: Message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_chat)

        val name_edittext_register = intent.getStringExtra(RegisterActivity.KEY_USERNAME)
        userUUID = intent.getStringExtra(RegisterActivity.KEY_USER_UUID).toString()

        messageListAdapter = MessageListAdapter(this, userUUID)
        messageListAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateEmptyView()
            }
            override fun onChanged() {
                updateEmptyView()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateEmptyView()

                message_list_recycler.post {
                    message_list_recycler.smoothScrollToPosition(messageListAdapter.itemCount - 1)
                }
            }

            private fun updateEmptyView() {
                val showEmptyView = messageListAdapter.itemCount == 0
                empty_view.visibility = if (showEmptyView) View.VISIBLE else View.GONE
            }
        })

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        message_list_recycler.layoutManager = layoutManager
        message_list_recycler.adapter = messageListAdapter

        messageListener = object : MessageListener() {
            override fun onFound(message: Message) {
                Log.d(TAG, "Found message: ${message.content}")
                val deviceMessage = DeviceMessage.fromNearbyMessage(message)
                if (deviceMessage.creationTime < loginTime) {
                    Log.d(
                        TAG,
                        "Found message was sent before we logged in. Won't add it to chat history."
                    )
                } else {
                    messageListAdapter.add(deviceMessage)
                }
            }

            override fun onLost(message: Message) {
                Log.d(TAG, "Lost sight of message: ${message.content}")

            }
        }

        logoutDialog = AlertDialog.Builder(this)
        logoutDialog. setTitle("Are you sure you want to leave?")
            .setMessage("Your chat history will be deleted.")
            .setNegativeButton("No", null)

        message_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (message_input.text.toString().trim().isEmpty()) {
                    send_message_button.setImageResource(R.drawable.ic_send_disabled)
                    send_message_button.isEnabled = false
                } else {
                    send_message_button.setImageResource(R.drawable.ic_baseline_send)
                    send_message_button.isEnabled = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        send_message_button.isEnabled = false
        send_message_button.setOnClickListener {
            val timestamp = System.currentTimeMillis()
            val deviceMessage = DeviceMessage(userUUID,
                name_edittext_register.toString(), message_input.text.toString(), timestamp)

            activeMessage = deviceMessage.message
            Log.d(TAG, "Publishing message = ${activeMessage.content}")
            Nearby.getMessagesClient(this).publish(activeMessage)

            messageListAdapter.add(deviceMessage)
            message_input.setText("")
        }
    }

    public override fun onStart() {
        super.onStart()

        Nearby.getMessagesClient(this).subscribe(messageListener)
    }

    public override fun onStop() {
        if (::activeMessage.isInitialized)
            Nearby.getMessagesClient(this).unpublish(activeMessage)
        Nearby.getMessagesClient(this).unsubscribe(messageListener)
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logoutDialog.setPositiveButton("Yes") { _, _ ->
                    Util.clearSharedPreferences(this@NearbyChatActivity)
                    finish()
                }.show()
                true
            }
            R.id.action_clear_chat_history -> {
                messageListAdapter.clear()
                loginTime = System.currentTimeMillis()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        logoutDialog.setPositiveButton("Yes") { _, _ ->
            Util.clearSharedPreferences(this@NearbyChatActivity)
            super@NearbyChatActivity.onBackPressed()
        }.show()
    }
}