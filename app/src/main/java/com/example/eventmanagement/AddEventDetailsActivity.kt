package com.example.eventmanagement

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class AddEventDetailsActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var dateButton: Button
    private lateinit var saveButton: Button
    private lateinit var multiSelectRecyclerView: RecyclerView
    private lateinit var selectAllButton: Button
    private lateinit var clearAllButton: Button
    private lateinit var backToHomeButton: Button

    private var selectedDate = ""
    private val nameTimeSelections = mutableListOf<NameTimeSelection>()
    private lateinit var multiSelectAdapter: MultiSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event_details)

        database = EventDatabase.getDatabase(this)

        initViews()
        setupMultiSelectList()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Reload all names whenever this activity comes to foreground
        loadAllNamesForSelection()
    }

    private fun initViews() {
        dateButton = findViewById(R.id.dateButton)
        saveButton = findViewById(R.id.saveButton)
        multiSelectRecyclerView = findViewById(R.id.multiSelectRecyclerView)
        selectAllButton = findViewById(R.id.selectAllButton)
        clearAllButton = findViewById(R.id.clearAllButton)
        backToHomeButton = findViewById(R.id.backToHomeButton)
    }

    private fun setupMultiSelectList() {
        multiSelectAdapter = MultiSelectAdapter(nameTimeSelections) { position ->
            showTimePickerForPosition(position)
        }
        multiSelectRecyclerView.layoutManager = LinearLayoutManager(this)
        multiSelectRecyclerView.adapter = multiSelectAdapter
    }

    private fun loadAllNamesForSelection() {
        runBlocking {
            launch {
                // Get names from the new NameDao
                val existingNames = database.nameDao().getAllNames()
                val newSelections = mutableListOf<NameTimeSelection>()
                for (name in existingNames) {
                    val existingSelection = nameTimeSelections.find { it.name == name }
                    newSelections.add(existingSelection ?: NameTimeSelection(name))
                }
                runOnUiThread {
                    nameTimeSelections.clear()
                    nameTimeSelections.addAll(newSelections)
                    multiSelectAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupListeners() {
        selectAllButton.setOnClickListener {
            for ((index, selection) in nameTimeSelections.withIndex()) {
                if (!selection.isSelected) {
                    selection.isSelected = true
                    multiSelectAdapter.notifyItemChanged(index)
                }
            }
        }

        clearAllButton.setOnClickListener {
            for ((index, selection) in nameTimeSelections.withIndex()) {
                if (selection.isSelected || selection.time.isNotEmpty()) {
                    selection.isSelected = false
                    selection.time = ""
                    multiSelectAdapter.notifyItemChanged(index)
                }
            }
        }

        dateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            saveEvents()
        }

        backToHomeButton.setOnClickListener {
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

    private fun showTimePickerForPosition(position: Int) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = formatTimeWithAmPm(hourOfDay, minute)
                nameTimeSelections[position].time = time
                multiSelectAdapter.notifyItemChanged(position)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
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
                    Toast.makeText(this@AddEventDetailsActivity, message, Toast.LENGTH_LONG).show()
                    resetFields()
                }
            }
        }
    }

    private fun resetFields() {
        selectedDate = ""
        dateButton.text = "Select Common Date"

        for ((index, selection) in nameTimeSelections.withIndex()) {
            if (selection.isSelected || selection.time.isNotEmpty()) {
                selection.isSelected = false
                selection.time = ""
                multiSelectAdapter.notifyItemChanged(index)
            }
        }
    }
}