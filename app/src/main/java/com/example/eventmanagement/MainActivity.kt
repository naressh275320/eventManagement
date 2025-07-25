package com.example.eventmanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var database: EventDatabase
    private lateinit var nameSpinner: Spinner
    private lateinit var customNameEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var saveButton: Button
    private lateinit var viewResultsButton: Button
    private lateinit var addCustomNameButton: Button

    private var selectedDate = ""
    private var selectedTime = ""
    private val predefinedNames = mutableListOf("Ruben", "Vishnu", "Mahendra", "Shankar", "Manimaran", "Padmanabhan", "Goapl")
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = EventDatabase.getDatabase(this)

        initViews()
        setupSpinner()
        setupListeners()
        loadExistingNames()
    }

    private fun initViews() {
        nameSpinner = findViewById(R.id.nameSpinner)
        customNameEditText = findViewById(R.id.customNameEditText)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        saveButton = findViewById(R.id.saveButton)
        viewResultsButton = findViewById(R.id.viewResultsButton)
        addCustomNameButton = findViewById(R.id.addCustomNameButton)
    }

    private fun setupSpinner() {
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, predefinedNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        nameSpinner.adapter = adapter
    }

    private fun loadExistingNames() {
        runBlocking {
            launch {
                val existingNames = database.eventDao().getAllNames()
                for (name in existingNames) {
                    if (!predefinedNames.contains(name)) {
                        predefinedNames.add(name)
                    }
                }
                runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupListeners() {
        addCustomNameButton.setOnClickListener {
            val customName = customNameEditText.text.toString().trim()
            if (customName.isNotEmpty() && !predefinedNames.contains(customName)) {
                predefinedNames.add(customName)
                adapter.notifyDataSetChanged()
                customNameEditText.setText("")
                Toast.makeText(this, "Name added to list", Toast.LENGTH_SHORT).show()
            } else if (customName.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Name already exists", Toast.LENGTH_SHORT).show()
            }
        }

        dateButton.setOnClickListener {
            showDatePicker()
        }

        timeButton.setOnClickListener {
            showTimePicker()
        }

        saveButton.setOnClickListener {
            saveEvent()
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

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Convert 24-hour format to 12-hour format with AM/PM
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val time = Calendar.getInstance()
                time.set(Calendar.HOUR_OF_DAY, hourOfDay)
                time.set(Calendar.MINUTE, minute)
                selectedTime = timeFormat.format(time.time)
                timeButton.text = "Time: $selectedTime"
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false  // Changed from true to false to show 12-hour format
        )
        timePickerDialog.show()
    }

    private fun saveEvent() {
        val selectedName = nameSpinner.selectedItem.toString()

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val event = Event(
            name = selectedName,
            date = selectedDate,
            time = selectedTime
        )

        runBlocking {
            launch {
                database.eventDao().insertEvent(event)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Event saved successfully", Toast.LENGTH_SHORT).show()
                    resetFields()
                }
            }
        }
    }

    private fun resetFields() {
        selectedDate = ""
        selectedTime = ""
        dateButton.text = "Select Date"
        timeButton.text = "Select Time"
        nameSpinner.setSelection(0)
    }
}
