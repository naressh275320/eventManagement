package com.example.eventmanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainLandingActivity : Activity() { // Or AppCompatActivity if you're using AppCompat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_landing)

        findViewById<Button>(R.id.buttonAddNames).setOnClickListener {
            // Start the activity for adding/managing names
            val intent = Intent(this, ManageNamesActivity::class.java) // Create this activity next
            startActivity(intent)
        }

        findViewById<Button>(R.id.buttonAddEventDetails).setOnClickListener {
            // Start the activity for adding event details (selecting people, date, time)
            val intent = Intent(this, AddEventDetailsActivity::class.java) // Create this activity next
            startActivity(intent)
        }

        findViewById<Button>(R.id.buttonViewResults).setOnClickListener {
            // Start the activity to view results (your existing EventsListActivity/ResultsActivity)
            val intent = Intent(this, ResultsActivity::class.java) // Assuming ResultsActivity is your results page
            startActivity(intent)
        }
    }
}