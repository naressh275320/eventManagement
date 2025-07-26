package com.example.eventmanagement

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var customNameEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var saveButton: Button
    private lateinit var viewResultsButton: Button
    private lateinit var addCustomNameButton: Button
    private lateinit var multiSelectListView: ListView
    private lateinit var selectAllButton: Button
    private lateinit var clearAllButton: Button

    private var selectedDate = ""
    private val predefinedNames = mutableListOf("John Doe", "Jane Smith", "Mike Johnson", "Sarah Wilson")
    private val nameTimeSelections = mutableListOf<NameTimeSelection>()
    private lateinit var multiSelectAdapter: MultiSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = EventDatabase.getDatabase(this)

        initViews()
        setupMultiSelectList()
        setupListeners()
        loadExistingNames()
    }

    private fun initViews() {
        customNameEditText = findViewById(R.id.customNameEditText)
        dateButton = findViewById(R.id.dateButton)
        saveButton = findViewById(R.id.saveButton)
        viewResultsButton = findViewById(R.id.viewResultsButton)
        addCustomNameButton = findViewById(R.id.addCustomNameButton)
        multiSelectListView = findViewById(R.id.multiSelectListView)
        selectAllButton = findViewById(R.id.selectAllButton)
        clearAllButton = findViewById(R.id.clearAllButton)
    }

    private fun setupMultiSelectList() {
        // Initialize the name-time selections
        for (name in predefinedNames) {
            nameTimeSelections.add(NameTimeSelection(name))
        }

        multiSelectAdapter = MultiSelectAdapter(this, nameTimeSelections) { position ->
            showTimePickerForPosition(position)
        }
        multiSelectListView.adapter = multiSelectAdapter
    }

    private fun loadExistingNames() {
        runBlocking {
            launch {
                val existingNames = database.eventDao().getAllNames()
                for (name in existingNames) {
                    if (!predefinedNames.contains(name) && name.isNotEmpty()) {
                        predefinedNames.add(name)
                        nameTimeSelections.add(NameTimeSelection(name))
                    }
                }
                runOnUiThread {
                    multiSelectAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupListeners() {
        addCustomNameButton.setOnClickListener {
            val customName = customNameEditText.text.toString().trim()
            if (customName.isNotEmpty() && !predefinedNames.contains(customName)) {
                predefinedNames.add(customName)
                nameTimeSelections.add(NameTimeSelection(customName))
                multiSelectAdapter.notifyDataSetChanged()
                customNameEditText.setText("")
                Toast.makeText(this, "Name added to list", Toast.LENGTH_SHORT).show()
            } else if (customName.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Name already exists", Toast.LENGTH_SHORT).show()
            }
        }

        selectAllButton.setOnClickListener {
            for (selection in nameTimeSelections) {
                selection.isSelected = true
            }
            multiSelectAdapter.notifyDataSetChanged()
        }

        clearAllButton.setOnClickListener {
            for (selection in nameTimeSelections) {
                selection.isSelected = false
                selection.time = ""
            }
            multiSelectAdapter.notifyDataSetChanged()
        }

        dateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            saveEvents()
        }

        viewResultsButton.setOnClickListener {
            val intent = Intent(this, ResultsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = "$dayOfMonth/${month + 1}/$year"
                dateButton.text = "Date: $selectedDate"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerForPosition(position: Int) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = formatTimeWithAmPm(hourOfDay, minute)
                nameTimeSelections[position].time = time
                multiSelectAdapter.notifyDataSetChanged()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false  // Changed to false to show AM/PM
        )
        timePickerDialog.show()
    }

    private fun formatTimeWithAmPm(hourOfDay: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    private fun saveEvents() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPeople = nameTimeSelections.filter { it.isSelected && it.time.isNotEmpty() }

        if (selectedPeople.isEmpty()) {
            Toast.makeText(this, "Please select at least one person and assign time", Toast.LENGTH_SHORT).show()
            return
        }

        val eventsToSave = selectedPeople.map { selection ->
            Event(
                name = selection.name,
                date = selectedDate,
                time = selection.time
            )
        }

        runBlocking {
            launch {
                database.eventDao().insertEvents(eventsToSave)
                runOnUiThread {
                    val message = "Saved ${eventsToSave.size} events successfully!"
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    resetFields()
                }
            }
        }
    }

    private fun resetFields() {
        selectedDate = ""
        dateButton.text = "Select Date"

        for (selection in nameTimeSelections) {
            selection.isSelected = false
            selection.time = ""
        }
        multiSelectAdapter.notifyDataSetChanged()
    }
}

class MultiSelectAdapter(
    private val context: Activity,
    private val nameTimeSelections: MutableList<NameTimeSelection>,
    private val onTimeClick: (Int) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = nameTimeSelections.size

    override fun getItem(position: Int): Any = nameTimeSelections[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View {
        val view = convertView ?: context.layoutInflater.inflate(R.layout.multi_select_item, parent, false)

        val checkBox = view.findViewById<CheckBox>(R.id.nameCheckBox)
        val nameText = view.findViewById<TextView>(R.id.nameTextView)
        val timeButton = view.findViewById<Button>(R.id.timeSelectButton)

        val selection = nameTimeSelections[position]

        checkBox.text = selection.name
        checkBox.isChecked = selection.isSelected

        timeButton.text = if (selection.time.isEmpty()) "Select Time" else "Time: ${selection.time}"
        timeButton.isEnabled = selection.isSelected

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            selection.isSelected = isChecked
            timeButton.isEnabled = isChecked
            if (!isChecked) {
                selection.time = ""
                timeButton.text = "Select Time"
            }
        }

        timeButton.setOnClickListener {
            if (selection.isSelected) {
                onTimeClick(position)
            }
        }

        return view
    }
}
