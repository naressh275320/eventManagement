package com.example.eventmanagement

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

// Keep your existing NameTimeSelection data class if it's in a separate file,
// otherwise define it here or in the MultiSelectAdapter file.
// data class NameTimeSelection(val name: String, var isSelected: Boolean = false, var time: String = "")

class MainActivity : Activity() { // If you need AppCompat features like toolbar/themes, change this to AppCompatActivity
    private lateinit var database: EventDatabase
    private lateinit var customNameEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var saveButton: Button
    private lateinit var viewResultsButton: Button
    private lateinit var addCustomNameButton: Button
    private lateinit var multiSelectRecyclerView: RecyclerView // Changed from ListView
    private lateinit var selectAllButton: Button
    private lateinit var clearAllButton: Button

    private var selectedDate = ""
    private val predefinedNames = mutableListOf("Ruben", "Mahendra", "Vishnu", "Gopal", "Padmanabhan", "Manimaran")
    private val nameTimeSelections = mutableListOf<NameTimeSelection>()
    private lateinit var multiSelectAdapter: MultiSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Make sure this matches your XML file name

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
        multiSelectRecyclerView = findViewById(R.id.multiSelectRecyclerView) // Changed ID to match XML
        selectAllButton = findViewById(R.id.selectAllButton)
        clearAllButton = findViewById(R.id.clearAllButton)
    }

    private fun setupMultiSelectList() {
        // Initialize the name-time selections
        // This should only happen once unless you want to reset the list
        if (nameTimeSelections.isEmpty()) { // Ensure names are added only once
            for (name in predefinedNames) {
                nameTimeSelections.add(NameTimeSelection(name))
            }
        }


        multiSelectAdapter = MultiSelectAdapter(nameTimeSelections) { position ->
            showTimePickerForPosition(position)
        }
        multiSelectRecyclerView.layoutManager = LinearLayoutManager(this) // Set LayoutManager
        multiSelectRecyclerView.adapter = multiSelectAdapter
    }

    private fun loadExistingNames() {
        runBlocking {
            launch {
                val existingNames = database.eventDao().getAllNames()
                val newNamesToAdd = mutableListOf<NameTimeSelection>()
                for (name in existingNames) {
                    // Check if name is already in predefined or already loaded
                    if (!predefinedNames.contains(name) && !nameTimeSelections.any { it.name == name } && name.isNotEmpty()) {
                        predefinedNames.add(name) // Add to predefined for future launches
                        newNamesToAdd.add(NameTimeSelection(name))
                    }
                }
                // Add new names to the existing list
                if (newNamesToAdd.isNotEmpty()) {
                    nameTimeSelections.addAll(newNamesToAdd)
                }

                runOnUiThread {
                    multiSelectAdapter.notifyDataSetChanged() // Notify adapter of data changes
                }
            }
        }
    }

    private fun setupListeners() {
        addCustomNameButton.setOnClickListener {
            val customName = customNameEditText.text.toString().trim()
            // Check against current list including dynamically added names
            if (customName.isNotEmpty() && !nameTimeSelections.any { it.name.equals(customName, ignoreCase = true) }) {
                predefinedNames.add(customName) // Keep track of all names
                nameTimeSelections.add(NameTimeSelection(customName))
                multiSelectAdapter.notifyItemInserted(nameTimeSelections.size - 1) // More efficient update
                customNameEditText.setText("")
                Toast.makeText(this, "Name added to list", Toast.LENGTH_SHORT).show()
            } else if (customName.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Name already exists", Toast.LENGTH_SHORT).show()
            }
        }

        selectAllButton.setOnClickListener {
            for ((index, selection) in nameTimeSelections.withIndex()) {
                if (!selection.isSelected) { // Only update if not already selected
                    selection.isSelected = true
                    multiSelectAdapter.notifyItemChanged(index) // Update only changed item
                }
            }
            // If you want to use notifyDataSetChanged(), that's also fine, just less efficient
            // multiSelectAdapter.notifyDataSetChanged()
        }

        clearAllButton.setOnClickListener {
            for ((index, selection) in nameTimeSelections.withIndex()) {
                if (selection.isSelected || selection.time.isNotEmpty()) { // Only update if state needs changing
                    selection.isSelected = false
                    selection.time = ""
                    multiSelectAdapter.notifyItemChanged(index) // Update only changed item
                }
            }
            // multiSelectAdapter.notifyDataSetChanged()
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
                multiSelectAdapter.notifyItemChanged(position) // Notify only this item changed
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
        dateButton.text = "Select Common Date" // Changed text to match XML

        for ((index, selection) in nameTimeSelections.withIndex()) {
            if (selection.isSelected || selection.time.isNotEmpty()) {
                selection.isSelected = false
                selection.time = ""
                multiSelectAdapter.notifyItemChanged(index) // Update only changed item
            }
        }
    }
}