package com.henry.chatroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.json.JSONObject
import org.json.JSONException
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import okhttp3.*

class MainActivity : AppCompatActivity() {

    private var webSocket: WebSocket? = null
    private var adapter: MessageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val messageList = findViewById<ListView>(R.id.messageList)
        val messageBox = findViewById<EditText>(R.id.messageBox)
        val send = findViewById<TextView>(R.id.send)

        instantiateWebSocket()

        adapter = MessageAdapter()
        messageList.adapter = adapter

        send.setOnClickListener {
            val message = messageBox.text.toString()

            if (message.isNotEmpty()) {
                webSocket?.send(message)
                messageBox.setText("")

                val jsonObject = JSONObject()

                try {
                    jsonObject.put("message", message)
                    jsonObject.put("byServer", false)

                    adapter?.addItem(jsonObject)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun instantiateWebSocket() {
        val client = OkHttpClient()

        //replace x.x.x.x with your machine's IP Address
        val request = Request.Builder().url("ws://192.168.31.228:9999").build()

        val socketListener = SocketListener(this)

        webSocket = client.newWebSocket(request, socketListener)
    }

    inner class SocketListener(private var activity: MainActivity) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            activity.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Connection Established!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            activity.runOnUiThread {
                val jsonObject = JSONObject()

                try {
                    jsonObject.put("message", text)
                    jsonObject.put("byServer", true)

                    adapter?.addItem(jsonObject)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class MessageAdapter : BaseAdapter() {

        private var messagesList: MutableList<JSONObject> = ArrayList()


        override fun getCount(): Int {
            return messagesList.size
        }

        override fun getItem(i: Int): Any {
            return messagesList[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view

            if (view == null)
                view = layoutInflater.inflate(R.layout.message_list_item, viewGroup, false)

            val sentMessage = view!!.findViewById<TextView>(R.id.sentMessage)
            val receivedMessage = view.findViewById<TextView>(R.id.receivedMessage)

            val item = messagesList[i]

            try {
                if (item.getBoolean("byServer")) {
                    receivedMessage.visibility = View.VISIBLE
                    receivedMessage.text = item.getString("message")

                    sentMessage.visibility = View.INVISIBLE
                } else {
                    sentMessage.visibility = View.VISIBLE
                    sentMessage.text = item.getString("message")

                    receivedMessage.visibility = View.INVISIBLE
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return view
        }

        internal fun addItem(item: JSONObject) {
            messagesList.add(item)
            notifyDataSetChanged()
        }
    }
}
