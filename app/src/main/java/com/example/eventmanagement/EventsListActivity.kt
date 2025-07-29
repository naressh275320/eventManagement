package com.example.eventmanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat

class EventsListActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var nameTextView: TextView
    private lateinit var eventsListView: ListView
    private lateinit var backButton: Button
    private lateinit var shareButton: Button

    private var eventsList = mutableListOf<Event>()
    private lateinit var eventsAdapter: ArrayAdapter<String>
    private var selectedName: String = "" // Will be empty for "View All Events"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events_list)

        database = EventDatabase.getDatabase(this)
        // Check if a specific name was passed (from the old workflow, or if you plan to filter)
        selectedName = intent.getStringExtra("selected_name") ?: ""

        initViews()
        setupListeners()
        // Data loading is moved to onResume()
    }

    override fun onResume() {
        super.onResume()
        // Reload events every time the activity comes to the foreground
        if (selectedName.isEmpty()) {
            loadAllFutureEvents() // New method to load all events for "View All Events" page
        } else {
            loadEventsForName(selectedName) // Keep original logic if filtering by specific name
        }
    }

    private fun initViews() {
        nameTextView = findViewById(R.id.selectedNameTextView)
        eventsListView = findViewById(R.id.eventsListView)
        backButton = findViewById(R.id.eventsBackButton)
        shareButton = findViewById(R.id.shareButton)

        if (selectedName.isEmpty()) {
            nameTextView.text = "All Upcoming Events" // Title for the new "View All Events" page
        } else {
            nameTextView.text = "EVENTS FOR: ${selectedName.uppercase()}" // Keep for single-person view if still used
        }
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

        shareButton.setOnClickListener {
            shareEventsToWhatsApp()
        }
    }

    // Existing method for specific name filtering (if still desired)
    private fun loadEventsForName(name: String) {
        runBlocking {
            launch {
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                database.eventDao().deleteExpiredEvents(currentDate) // Clean up expired events first

                eventsList.clear()
                eventsList.addAll(database.eventDao().getFutureEventsByName(name, currentDate))

                runOnUiThread {
                    val displayList = eventsList.map { event ->
                        // 1. Convert AM/PM to காலை/மாலை first
                        val convertedTimeWithIndicator = event.time
                            .replace("AM", "காலை", ignoreCase = true)
                            .replace("PM", "மாலை", ignoreCase = true)

                        // 2. Now, reformat the `convertedTimeWithIndicator` for display
                        val formattedTimeForDisplay: String
                        val timeParts = convertedTimeWithIndicator.split(" ") // Splits into ["HH:MM", "காலை/மாலை"]

                        if (timeParts.size == 2) {
                            val timeValue = timeParts[0] // This will be "03:00"
                            val amPmIndicator = timeParts[1] // This will be "காலை" or "மாலை"
                            formattedTimeForDisplay = "$amPmIndicator $timeValue" // Reordered: "காலை 03:00"
                        } else {
                            // Fallback case if the format isn't as expected
                            formattedTimeForDisplay = convertedTimeWithIndicator
                        }

                        // Combine into the final display string for this event
                        "Date: ${event.date} \nTime: $formattedTimeForDisplay"
                    }

                    eventsAdapter = object : ArrayAdapter<String>(
                        this@EventsListActivity,
                        android.R.layout.simple_list_item_1,
                        displayList
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            view.setTextColor(ContextCompat.getColor(context, R.color.black))
                            return view
                        }
                    }
                    eventsListView.adapter = eventsAdapter

                    if (eventsList.isEmpty()) {
                        Toast.makeText(this@EventsListActivity, "No upcoming events found for $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // New method to load all future events for the "View All" page
    private fun loadAllFutureEvents() {
        runBlocking {
            launch {
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                database.eventDao().deleteExpiredEvents(currentDate) // Clean up expired events first

                eventsList.clear()
                val allEvents = database.eventDao().getAllEvents() // Get ALL events from DB

                // Filter for future events and sort them
                val futureEvents = allEvents.filter { event ->
                    try {
                        val eventDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(event.date)
                        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(currentDate)
                        eventDate != null && (eventDate.after(today) || eventDate.equals(today))
                    } catch (e: Exception) {
                        // Handle parsing errors if date format is inconsistent
                        false
                    }
                }.sortedWith(compareBy(
                    { it.name }, // Sort by name first
                    { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.date) }, // Then by date
                    { it.time } // Then by time
                ))

                eventsList.addAll(futureEvents)

                runOnUiThread {
                    val displayList = eventsList.map { event ->
                        // 1. Convert AM/PM to காலை/மாலை first
                        val convertedTimeWithIndicator = event.time
                            .replace("AM", "காலை", ignoreCase = true)
                            .replace("PM", "மாலை", ignoreCase = true)

                        // 2. Now, reformat the `convertedTimeWithIndicator` for display
                        val formattedTimeForDisplay: String
                        val timeParts = convertedTimeWithIndicator.split(" ") // Splits into ["HH:MM", "காலை/மாலை"]

                        if (timeParts.size == 2) {
                            val timeValue = timeParts[0] // This will be "03:00"
                            val amPmIndicator = timeParts[1] // This will be "காலை" or "மாலை"
                            formattedTimeForDisplay = "$amPmIndicator $timeValue" // Reordered: "காலை 03:00"
                        } else {
                            // Fallback case if the format isn't as expected
                            formattedTimeForDisplay = convertedTimeWithIndicator
                        }

                        // Combine into the final display string for this event
                        "Date: ${event.date} \nTime: $formattedTimeForDisplay"
                    }

                    eventsAdapter = object : ArrayAdapter<String>(
                        this@EventsListActivity,
                        android.R.layout.simple_list_item_1,
                        displayList
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent) as TextView
                            view.setTextColor(ContextCompat.getColor(context, R.color.black))
                            return view
                        }
                    }
                    eventsListView.adapter = eventsAdapter

                    if (eventsList.isEmpty()) {
                        Toast.makeText(this@EventsListActivity, "No upcoming events found.", Toast.LENGTH_SHORT).show()
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
                        // After deleting, reload based on current context
                        if (selectedName.isEmpty()) {
                            loadAllFutureEvents()
                        } else {
                            loadEventsForName(selectedName)
                        }
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
            // If an event was edited, reload the list
            if (selectedName.isEmpty()) {
                loadAllFutureEvents()
            } else {
                loadEventsForName(selectedName)
            }
        }
    }

    private fun shareEventsToWhatsApp() {
        val stringBuilder = StringBuilder()
        stringBuilder.append(nameTextView.text.toString()).append("\n\n")

        if (eventsList.isNotEmpty()) {
            for (event in eventsList) {
                // 1. Convert AM/PM to காலை/மாலை
                val customTimeWithIndicator = event.time
                    .replace("AM", "காலை", ignoreCase = true)
                    .replace("PM", "மாலை", ignoreCase = true)

                // 2. Reformat the `customTimeWithIndicator` for display
                val formattedTimeForDisplay: String
                val timeParts = customTimeWithIndicator.split(" ") // Splits into ["HH:MM", "காலை/மாலை"]

                if (timeParts.size == 2) {
                    val timeValue = timeParts[0] // This will be "03:00"
                    val amPmIndicator = timeParts[1] // This will be "காலை" or "மாலை"
                    formattedTimeForDisplay = "$amPmIndicator $timeValue" // Reordered: "காலை 03:00"
                } else {
                    // Fallback case if the format isn't as expected
                    formattedTimeForDisplay = customTimeWithIndicator
                }

                // 3. Append the reformatted time to the StringBuilder
                stringBuilder.append("Date: ${event.date} \nTime: $formattedTimeForDisplay\n")
            }
        } else {
            stringBuilder.append("No upcoming events to share!")
        }

        val shareText = stringBuilder.toString()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        val whatsappPackage = "com.whatsapp"
        try {
            packageManager.getPackageInfo(whatsappPackage, PackageManager.GET_ACTIVITIES)
            shareIntent.setPackage(whatsappPackage)
            startActivity(shareIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "WhatsApp is not installed. Sharing via general options.", Toast.LENGTH_LONG).show()
            startActivity(Intent.createChooser(shareIntent, "Share events via..."))
        }
    }
}