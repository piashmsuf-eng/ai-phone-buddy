package com.myra.assistant.ui.settings

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.service.AccessibilityHelperService
import org.json.JSONArray
import org.json.JSONObject
import com.myra.assistant.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var modelSpinner: Spinner
    private lateinit var voiceSpinner: Spinner
    private lateinit var personalityGroup: RadioGroup
    private lateinit var primeRecycler: RecyclerView
    private lateinit var accessibilityText: TextView
    private lateinit var saveBtn: Button
    private val primeContacts = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        nameInput = findViewById(R.id.nameInput)
        modelSpinner = findViewById(R.id.modelSpinner)
        voiceSpinner = findViewById(R.id.voiceSpinner)
        personalityGroup = findViewById(R.id.personalityGroup)
        primeRecycler = findViewById(R.id.primeRecycler)
        accessibilityText = findViewById(R.id.accessibilityText)
        saveBtn = findViewById(R.id.saveBtn)

        val models = arrayOf(
            "models/gemini-2.5-flash-native-audio-preview-12-2025",
            "models/gemini-2.0-flash-live-001",
            "models/gemini-2.5-flash-preview-native-audio-dialog"
        )
        val modelLabels = arrayOf("Native Audio (Human Voice)", "Flash Live (Fast)", "Pro Audio Dialog")
        modelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modelLabels)

        val voices = arrayOf("Aoede", "Charon", "Kore", "Fenrir", "Puck", "Leda", "Orus", "Zephyr")
        voiceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, voices)

        loadPrefs()
        loadPrimeContacts()

        primeRecycler.layoutManager = LinearLayoutManager(this)
        primeRecycler.adapter = PrimeContactAdapter()

        findViewById<Button>(R.id.addPrimeBtn).setOnClickListener { showAddPrimeContactDialog() }

        accessibilityText.setOnClickListener {
            startActivity(android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        saveBtn.setOnClickListener { savePrefs() }
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences("myra_prefs", MODE_PRIVATE)
        apiKeyInput.setText(prefs.getString("api_key", "") ?: "")
        nameInput.setText(prefs.getString("user_name", "Boss") ?: "Boss")
        val modelIdx = modelSpinner.adapter.let { adapter ->
            val models = arrayOf(
                "models/gemini-2.5-flash-native-audio-preview-12-2025",
                "models/gemini-2.0-flash-live-001",
                "models/gemini-2.5-flash-preview-native-audio-dialog"
            )
            val saved = prefs.getString("gemini_model", models[0])
            models.indexOf(saved).coerceAtLeast(0)
        }
        modelSpinner.setSelection(modelIdx)
        val voiceIdx = arrayOf("Aoede", "Charon", "Kore", "Fenrir", "Puck", "Leda", "Orus", "Zephyr")
            .indexOf(prefs.getString("gemini_voice", "Aoede")).coerceAtLeast(0)
        voiceSpinner.setSelection(voiceIdx)
        when (prefs.getString("personality_mode", "gf")) {
            "gf" -> personalityGroup.check(R.id.personalityGf)
            "professional" -> personalityGroup.check(R.id.personalityPro)
            "assistant" -> personalityGroup.check(R.id.personalityAsst)
        }
        accessibilityText.text = if (AccessibilityHelperService.isEnabled(this)) "Accessibility: ON ✅" else "Accessibility: OFF ❌ (Tap to open settings)"
    }

    private fun loadPrimeContacts() {
        val prefs = getSharedPreferences("myra_prefs", MODE_PRIVATE)
        primeContacts.clear()
        val json = prefs.getString("prime_contacts_json", null)
        if (json != null) {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) primeContacts.add(arr.getJSONObject(i))
        }
    }

    private fun savePrefs() {
        val prefs = getSharedPreferences("myra_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("api_key", apiKeyInput.text.toString().trim())
            putString("user_name", nameInput.text.toString().trim().ifBlank { "Boss" })
            val models = arrayOf(
                "models/gemini-2.5-flash-native-audio-preview-12-2025",
                "models/gemini-2.0-flash-live-001",
                "models/gemini-2.5-flash-preview-native-audio-dialog"
            )
            putString("gemini_model", models[modelSpinner.selectedItemPosition.coerceIn(0, 2)])
            putString("gemini_voice", arrayOf("Aoede", "Charon", "Kore", "Fenrir", "Puck", "Leda", "Orus", "Zephyr")[voiceSpinner.selectedItemPosition.coerceIn(0, 7)])
            putString("personality_mode", when (personalityGroup.checkedRadioButtonId) {
                R.id.personalityPro -> "professional"
                R.id.personalityAsst -> "assistant"
                else -> "gf"
            })
            val arr = JSONArray()
            for (c in primeContacts) arr.put(c)
            putString("prime_contacts_json", arr.toString())
        }.apply()
        Toast.makeText(this, "Saved! Restart app to apply.", Toast.LENGTH_SHORT).show()
    }

    private fun showAddPrimeContactDialog() {
        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_prime_contact, null)
        val nameInput = view.findViewById<EditText>(R.id.dialogNameInput)
        val numberInput = view.findViewById<EditText>(R.id.dialogNumberInput)
        dialog.setTitle("Add Prime Contact")
        dialog.setView(view)
        dialog.setPositiveButton("Add") { _, _ ->
            val name = nameInput.text.toString().trim()
            val number = numberInput.text.toString().trim()
            if (name.isNotBlank() && number.isNotBlank()) {
                primeContacts.add(JSONObject().apply { put("name", name); put("number", number) })
                primeRecycler.adapter?.notifyItemInserted(primeContacts.size - 1)
            }
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    inner class PrimeContactAdapter : RecyclerView.Adapter<PrimeContactAdapter.ViewHolder>() {
        override fun getItemCount() = primeContacts.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_prime_contact, parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val c = primeContacts[pos]
            holder.nameText.text = c.optString("name")
            holder.numberText.text = c.optString("number")
            holder.deleteBtn.setOnClickListener {
                primeContacts.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.primeItemName)
            val numberText: TextView = view.findViewById(R.id.primeItemNumber)
            val deleteBtn: ImageButton = view.findViewById(R.id.primeItemDelete)
        }
    }
}
