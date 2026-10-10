package com.zyleai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 12, 18)
    private val panel = Color.rgb(13, 22, 30)
    private val neon = Color.rgb(0, 255, 170)
    private val cyan = Color.rgb(0, 220, 255)
    private val white = Color.rgb(225, 245, 240)
    private val prefs by lazy { getSharedPreferences("zyle_history", MODE_PRIVATE) }
    private lateinit var chat: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView

    // Change this to your backend URL after deploying it.
    // Never put a Gemini API key in this Android app.
    private val backendUrl = "https://YOUR-BACKEND-DOMAIN.example/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        addMessage("ZYLE AI", "Sistem online. Tanyakan apa saja atau minta bantuan coding.", false)
        loadHistory()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, neon, 14)
            setPadding(14, 12, 14, 12)
        }
        header.addView(TextView(this).apply {
            text = "▰ ZYLE AI // TERMINAL"
            textSize = 21f
            typeface = Typeface.MONOSPACE
            setTextColor(neon)
            setTypeface(null, Typeface.BOLD)
        })
        status = TextView(this).apply {
            text = "● READY  |  CHAT + CODE + WEB"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
        }
        header.addView(status)
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        tools.addView(smallButton("RIWAYAT") { showHistory() }, LinearLayout.LayoutParams(0, 42, 1f))
        tools.addView(smallButton("CARI WEB") { searchWeb() }, LinearLayout.LayoutParams(0, 42, 1f))
        tools.addView(smallButton("CHAT BARU") { newChat() }, LinearLayout.LayoutParams(0, 42, 1f))
        root.addView(tools)

        scroll = ScrollView(this).apply { isFillViewport = true }
        chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 4, 2, 12)
        }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, cyan, 12)
            setPadding(8, 4, 8, 4)
        }
        input = EditText(this).apply {
            hint = "Ketik pesan atau pertanyaan coding..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setSingleLine(false)
            maxLines = 4
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8, 8, 8, 8)
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(smallButton("KIRIM") { sendMessage() }, LinearLayout.LayoutParams(-2, 44))
        root.addView(composer)
        setContentView(root)
    }

    private fun rounded(color: Int, stroke: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(1, stroke)
        }

    private fun smallButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(neon)
            background = rounded(panel, neon, 10)
            setOnClickListener { action() }
            isAllCaps = false
        }

    private fun addMessage(who: String, message: String, user: Boolean) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(if (user) Color.rgb(12, 31, 34) else panel, if (user) cyan else neon, 10)
            setPadding(12, 9, 12, 9)
        }
        card.addView(TextView(this).apply {
            text = if (user) "YOU >" else "$who >"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(if (user) cyan else neon)
            setTypeface(null, Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = message
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(white)
            setTextIsSelectable(true)
            setPadding(0, 4, 0, 0)
        })
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 4, 0, 8)
        chat.addView(card, lp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        addMessage("YOU", prompt, true)
        saveTurn("YOU", prompt)
        status.text = "● THINKING..."
        addMessage("ZYLE AI", "Sedang memproses...", false)
        val placeholderIndex = chat.childCount - 1

        thread {
            val answer = try { callBackend(prompt) } catch (e: Exception) {
                "Belum dapat terhubung ke server AI. Periksa URL backend dan koneksi internet.\nDetail: ${e.message ?: "koneksi gagal"}"
            }
            runOnUiThread {
                if (placeholderIndex < chat.childCount) chat.removeViewAt(placeholderIndex)
                addMessage("ZYLE AI", answer, false)
                saveTurn("ZYLE AI", answer)
                status.text = "● READY  |  CHAT + CODE + WEB"
            }
        }
    }

    private fun callBackend(prompt: String): String {
        val url = URL(backendUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            val payload = JSONObject().put("message", prompt).toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (conn.responseCode !in 200..299) return "Server error ${conn.responseCode}: $response"
            return JSONObject(response).optString("reply", "Server tidak mengirim jawaban.")
        } finally {
            conn.disconnect()
        }
    }

    private fun saveTurn(who: String, text: String) {
        val arr = try { JSONArray(prefs.getString("turns", "[]")) } catch (_: Exception) { JSONArray() }
        arr.put(JSONObject().put("who", who).put("text", text).put("time", System.currentTimeMillis()))
        // Keep only the most recent 200 messages.
        val trimmed = JSONArray()
        for (i in maxOf(0, arr.length() - 200) until arr.length()) trimmed.put(arr.getJSONObject(i))
        prefs.edit().putString("turns", trimmed.toString()).apply()
    }

    private fun loadHistory() {
        // History is local to this device; avoid duplicate display on initial screen.
        status.text = "● READY  |  HISTORY SAVED LOCALLY"
    }

    private fun showHistory() {
        val arr = try { JSONArray(prefs.getString("turns", "[]")) } catch (_: Exception) { JSONArray() }
        chat.removeAllViews()
        if (arr.length() == 0) {
            addMessage("ZYLE AI", "Belum ada riwayat chat.", false)
            return
        }
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            addMessage(item.optString("who", "CHAT"), item.optString("text", ""), item.optString("who") == "YOU")
        }
    }

    private fun newChat() {
        chat.removeAllViews()
        addMessage("ZYLE AI", "Chat baru dimulai. Riwayat lama tetap tersimpan.", false)
    }

    private fun searchWeb() {
        val q = input.text.toString().trim()
        if (q.isEmpty()) {
            input.hint = "Ketik kata kunci, lalu tekan CARI WEB"
            return
        }
        try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")))
        } catch (_: Exception) {
            Toast.makeText(this, "Tidak bisa membuka browser.", Toast.LENGTH_SHORT).show()
        }
    }
}
