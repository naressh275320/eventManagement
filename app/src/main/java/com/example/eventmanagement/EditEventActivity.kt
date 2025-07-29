package com.example.eventmanagement

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var nameTextView: TextView

    private var eventId: Long = 0
    private var eventName: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_event)

        database = EventDatabase.getDatabase(this)

        initViews()
        loadEventData()
        setupListeners()
    }

    private fun initViews() {
        nameTextView = findViewById(R.id.editEventNameTextView)
        dateButton = findViewById(R.id.editDateButton)
        timeButton = findViewById(R.id.editTimeButton)
        saveButton = findViewById(R.id.editSaveButton)
        cancelButton = findViewById(R.id.editCancelButton)
    }

    private fun loadEventData() {
        eventId = intent.getLongExtra("event_id", 0)
        eventName = intent.getStringExtra("event_name") ?: ""
        selectedDate = intent.getStringExtra("event_date") ?: ""
        selectedTime = intent.getStringExtra("event_time") ?: ""

        nameTextView.text = "Editing event for: $eventName"
        dateButton.text = "Date: $selectedDate"

        // Convert AM/PM to காலை/மாலை for display
        val convertedTimeWithIndicator = selectedTime
            .replace("AM", "காலை", ignoreCase = true)
            .replace("PM", "மாலை", ignoreCase = true)

        // 2. Now, reformat the `convertedTimeWithIndicator` for the button display
        //    Example: "03:00 காலை" will be split into ["03:00", "காலை"]
        val timeParts = convertedTimeWithIndicator.split(" ")

        if (timeParts.size == 2) {
            val timeValue = timeParts[0] // This will be "03:00"
            val amPmIndicator = timeParts[1] // This will be "காலை" or "மாலை"

            // 3. Construct the display string with the desired order
            timeButton.text = "Time: $amPmIndicator $timeValue"
        } else {
            // Fallback case if the format isn't as expected (e.g., no space or unexpected characters)
            timeButton.text = "Time: $convertedTimeWithIndicator"
        }
    }

    private fun setupListeners() {
        dateButton.setOnClickListener {
            showDatePicker()
        }

        timeButton.setOnClickListener {
            showTimePicker()
        }

        saveButton.setOnClickListener {
            saveChanges()
        }

        cancelButton.setOnClickListener {
            finish()
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
                selectedTime = formatTimeWithAmPm(hourOfDay, minute)
                timeButton.text = "Time: $selectedTime"
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

    private fun saveChanges() {
        val updatedEvent = Event(
            id = eventId,
            name = eventName,
            date = selectedDate,
            time = selectedTime
        )

        runBlocking {
            launch {
                database.eventDao().updateEvent(updatedEvent)
                runOnUiThread {
                    Toast.makeText(this@EditEventActivity, "Event updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}
