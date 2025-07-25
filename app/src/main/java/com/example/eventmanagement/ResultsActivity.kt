package com.example.eventmanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ResultsActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var nameSpinner: Spinner
    private lateinit var showEventsButton: Button
    private lateinit var backButton: Button

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
        showEventsButton = findViewById(R.id.showEventsButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupSpinner() {
        runBlocking {
            launch {
                val names = database.eventDao().getAllNames()
                runOnUiThread {
                    if (names.isEmpty()) {
                        Toast.makeText(this@ResultsActivity, "No names found. Add some events first.", Toast.LENGTH_LONG).show()
                        showEventsButton.isEnabled = false
                        showEventsButton.text = "No Events Available"
                    } else {
                        val adapter = ArrayAdapter(this@ResultsActivity, android.R.layout.simple_spinner_item, names)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        nameSpinner.adapter = adapter
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        showEventsButton.setOnClickListener {
            if (nameSpinner.selectedItem != null) {
                val selectedName = nameSpinner.selectedItem.toString()
                val intent = Intent(this, EventsListActivity::class.java)
                intent.putExtra("selected_name", selectedName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a name", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}