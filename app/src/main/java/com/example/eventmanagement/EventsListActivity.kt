package com.example.eventmanagement

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventsListActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var nameTextView: TextView
    private lateinit var eventsListView: ListView
    private lateinit var backButton: Button
    private lateinit var shareButton: Button

    private var eventsList = mutableListOf<Event>()
    private lateinit var eventsAdapter: ArrayAdapter<String>
    private var selectedName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_list)

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
        shareButton = findViewById(R.id.shareButton)

        nameTextView.text = "Events for: $selectedName"
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        shareButton.setOnClickListener {
            shareEventsListToWhatsApp()
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
                val allEvents = database.eventDao().getEventsByName(name)

                // Filter out past events - only show future events
                val futureEvents = allEvents.filter { event ->
                    isEventInFuture(event.date)
                }

                eventsList.addAll(futureEvents)

                runOnUiThread {
                    val displayList = eventsList.map { event ->
                        val customTime = event.time.replace("AM", "காலை").replace("PM", "மாலை")
                        "Date: ${event.date} | Time: $customTime"
                    }

                    eventsAdapter = ArrayAdapter(this@EventsListActivity, android.R.layout.simple_list_item_1, displayList)
                    eventsListView.adapter = eventsAdapter

                    if (eventsList.isEmpty()) {
                        if (allEvents.isNotEmpty()) {
                            Toast.makeText(this@EventsListActivity, "All events for $name have passed", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@EventsListActivity, "No events found for $name", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun isEventInFuture(eventDateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val eventDate = dateFormat.parse(eventDateString)
            val currentDate = Calendar.getInstance()

            // Set current date to start of day (00:00:00) for accurate comparison
            currentDate.set(Calendar.HOUR_OF_DAY, 0)
            currentDate.set(Calendar.MINUTE, 0)
            currentDate.set(Calendar.SECOND, 0)
            currentDate.set(Calendar.MILLISECOND, 0)

            val eventCalendar = Calendar.getInstance()
            eventCalendar.time = eventDate ?: return false
            eventCalendar.set(Calendar.HOUR_OF_DAY, 0)
            eventCalendar.set(Calendar.MINUTE, 0)
            eventCalendar.set(Calendar.SECOND, 0)
            eventCalendar.set(Calendar.MILLISECOND, 0)

            // Return true if event date is today or in the future
            !eventCalendar.before(currentDate)
        } catch (e: Exception) {
            // If date parsing fails, show the event to be safe
            true
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

    private fun shareEventsListToWhatsApp() {
        if (eventsList.isEmpty()) {
            Toast.makeText(this, "No events to share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareText = buildShareText()

        try {
            val whatsappIntent = Intent(Intent.ACTION_SEND)
            whatsappIntent.type = "text/plain"
            whatsappIntent.setPackage("com.whatsapp")
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            startActivity(whatsappIntent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, show generic share dialog
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Events for $selectedName")
            startActivity(Intent.createChooser(shareIntent, "Share Events via"))
        }
    }

    private fun buildShareText(): String {
        val shareText = StringBuilder()
        shareText.append("Event Schedule for $selectedName\n\n")

        if (eventsList.isNotEmpty()) {
            // Sort events by date and time (only future events are in the list)
            val sortedEvents = eventsList.sortedWith(compareBy({ it.date }, { it.time }))

            for (event in sortedEvents) {
                val customTime = event.time.replace("AM", "காலை").replace("PM", "மாலை")
                shareText.append("Date: ${event.date}\n")
                shareText.append("Time: $customTime\n\n")
            }
        }

        return shareText.toString().trim()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadEventsForName(selectedName)
        }
    }
}

