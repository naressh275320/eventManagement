package com.example.eventmanagement

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsListActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var nameTextView: TextView
    private lateinit var eventsListView: ListView
    private lateinit var backButton: Button
    private lateinit var shareButton: Button // Added share button reference

    private var eventsList = mutableListOf<Event>()
    private lateinit var eventsAdapter: ArrayAdapter<String>
    private var selectedName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_list) // Make sure this matches your XML file name

        database = EventDatabase.getDatabase(this)
        selectedName = intent.getStringExtra("selected_name") ?: ""

        initViews()
        setupListeners()
        loadEventsForName(selectedName)
    }

    private fun initViews() {
        nameTextView = findViewById(R.id.selectedNameTextView)
        eventsListView = findViewById(R.id.eventsListView)
        backButton = findViewById(R.id.eventsBackButton)
        shareButton = findViewById(R.id.shareEventsButton) // Initialize the share button

        nameTextView.text = "Events for: $selectedName"
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        eventsListView.setOnItemClickListener { _, _, position, _ ->
            if (position < eventsList.size) {
                showEditDeleteDialog(eventsList[position])
            }
        }

        // Set OnClickListener for the Share button
        shareButton.setOnClickListener {
            shareEventsToWhatsApp()
        }
    }

    private fun loadEventsForName(name: String) {
        runBlocking {
            launch {
                // Get current date in dd/MM/yyyy format
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                // Delete expired events for all users
                database.eventDao().deleteExpiredEvents(currentDate)

                eventsList.clear()
                // Load only future events
                eventsList.addAll(database.eventDao().getFutureEventsByName(name, currentDate))

                runOnUiThread {
                    val displayList = eventsList.map { event ->
                        val customTime = event.time.replace("AM", "kalai").replace("PM", "malai")
                        "Date: ${event.date} | Time: $customTime"
                    }

                    eventsAdapter = ArrayAdapter(this@EventsListActivity, android.R.layout.simple_list_item_1, displayList)
                    eventsListView.adapter = eventsAdapter

                    if (eventsList.isEmpty()) {
                        Toast.makeText(this@EventsListActivity, "No upcoming events found for $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showEditDeleteDialog(event: Event) {
        val options = arrayOf("Edit", "Delete")
        val builder = android.app.AlertDialog.Builder(this)
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
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Event")
        builder.setMessage("Are you sure you want to delete this event?")
        builder.setPositiveButton("Yes") { _, _ ->
            runBlocking {
                launch {
                    database.eventDao().deleteEvent(event)
                    runOnUiThread {
                        Toast.makeText(this@EventsListActivity, "Event deleted", Toast.LENGTH_SHORT).show()
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
            loadEventsForName(selectedName)
        }
    }

    // --- New method for sharing events ---
    private fun shareEventsToWhatsApp() {
        val stringBuilder = StringBuilder()
        stringBuilder.append(nameTextView.text.toString()).append("\n\n") // Add the "Events for:" line

        // Build the list of events from the 'eventsList' (your original data source)
        if (eventsList.isNotEmpty()) {
            stringBuilder.append("My Events:\n")
            for (event in eventsList) {
                val customTime = event.time.replace("AM", "kalai").replace("PM", "malai")
                stringBuilder.append("- Date: ${event.date} | Time: $customTime\n")
            }
        } else {
            stringBuilder.append("No upcoming events to share!")
        }

        val shareText = stringBuilder.toString()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        // Try to directly target WhatsApp
        val whatsappPackage = "com.whatsapp"
        try {
            packageManager.getPackageInfo(whatsappPackage, PackageManager.GET_ACTIVITIES)
            shareIntent.setPackage(whatsappPackage)
            startActivity(shareIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            // WhatsApp is not installed, offer a general share chooser
            Toast.makeText(this, "WhatsApp is not installed. Sharing via general options.", Toast.LENGTH_LONG).show()
            startActivity(Intent.createChooser(shareIntent, "Share events via..."))
        }
        // Alternatively, always use the general share chooser for broader options:
        // startActivity(Intent.createChooser(shareIntent, "Share events via..."))
    }
    // --- End of new method ---
}