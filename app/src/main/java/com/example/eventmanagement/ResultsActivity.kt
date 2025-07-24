package com.example.eventmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ResultsActivity : AppCompatActivity() {
    private lateinit var database: EventDatabase
    private lateinit var nameSpinner: Spinner
    private lateinit var eventsListView: ListView
    private lateinit var showEventsButton: Button
    private lateinit var backButton: Button

    private var eventsList = mutableListOf<Event>()
    private lateinit var eventsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        database = EventDatabase.getDatabase(this)

        initViews()
        setupSpinner()
        setupListeners()
    }

    private fun initViews() {
        nameSpinner = findViewById(R.id.resultsNameSpinner)
        eventsListView = findViewById(R.id.eventsListView)
        showEventsButton = findViewById(R.id.showEventsButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupSpinner() {
        runBlocking {
            launch {
                val names = database.eventDao().getAllNames()
                runOnUiThread {
                    val adapter = ArrayAdapter(this@ResultsActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    nameSpinner.adapter = adapter
                }
            }
        }
    }

    private fun setupListeners() {
        showEventsButton.setOnClickListener {
            if (nameSpinner.selectedItem != null) {
                loadEventsForName(nameSpinner.selectedItem.toString())
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        eventsListView.setOnItemClickListener { _, _, position, _ ->
            if (position < eventsList.size) {
                showEditDeleteDialog(eventsList[position])
            }
        }
    }

    private fun loadEventsForName(name: String) {
        runBlocking {
            launch {
                eventsList.clear()
                eventsList.addAll(database.eventDao().getEventsByName(name))

                runOnUiThread {
                    val displayList = eventsList.map { event ->
                        "Date: ${event.date} | Time: ${event.time}"
                    }

                    eventsAdapter = ArrayAdapter(this@ResultsActivity, android.R.layout.simple_list_item_1, displayList)
                    eventsListView.adapter = eventsAdapter

                    if (eventsList.isEmpty()) {
                        Toast.makeText(this@ResultsActivity, "No events found for $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showEditDeleteDialog(event: Event) {
        val options = arrayOf("Edit", "Delete")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Choose Action")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> editEvent(event)
                1 -> deleteEvent(event)
            }
        }
        builder.show()
    }

    private fun editEvent(event: Event) {
        val intent = Intent(this, EditEventActivity::class.java)
        intent.putExtra("event_id", event.id)
        intent.putExtra("event_name", event.name)
        intent.putExtra("event_date", event.date)
        intent.putExtra("event_time", event.time)
        startActivityForResult(intent, 1)
    }

    private fun deleteEvent(event: Event) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Event")
        builder.setMessage("Are you sure you want to delete this event?")
        builder.setPositiveButton("Yes") { _, _ ->
            runBlocking {
                launch {
                    database.eventDao().deleteEvent(event)
                    runOnUiThread {
                        Toast.makeText(this@ResultsActivity, "Event deleted", Toast.LENGTH_SHORT).show()
                        loadEventsForName(event.name)
                    }
                }
            }
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Refresh the events list
            if (nameSpinner.selectedItem != null) {
                loadEventsForName(nameSpinner.selectedItem.toString())
            }
        }
    }
}
