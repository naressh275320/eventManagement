package com.example.eventmanagement

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ManageNamesActivity : Activity() {
    private lateinit var database: EventDatabase
    private lateinit var customNameEditText: EditText
    private lateinit var addCustomNameButton: Button
    private lateinit var existingNamesListView: ListView
    private lateinit var backToHomeButton: Button

    private val currentNames = mutableListOf<String>()
    private lateinit var namesAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_names)

        database = EventDatabase.getDatabase(this)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayNames() // Reload names whenever this activity comes to foreground
    }

    private fun initViews() {
        customNameEditText = findViewById(R.id.customNameEditText)
        addCustomNameButton = findViewById(R.id.addCustomNameButton)
        existingNamesListView = findViewById(R.id.existingNamesListView)
        backToHomeButton = findViewById(R.id.backToHomeButton)

        namesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, currentNames)
        existingNamesListView.adapter = namesAdapter
    }

    private fun setupListeners() {
        addCustomNameButton.setOnClickListener {
            val customName = customNameEditText.text.toString().trim()
            if (customName.isNotEmpty()) {
                runBlocking {
                    launch {
                        // Use the new NameDao to check and insert
                        val nameExists = database.nameDao().nameExists(customName)
                        runOnUiThread {
                            if (!nameExists) {
                                // Insert into the names_table, not events table!
                                runBlocking {
                                    launch {
                                        database.nameDao().insertName(Name(personName = customName))
                                        runOnUiThread {
                                            loadAndDisplayNames() // Refresh the displayed list
                                            customNameEditText.setText("")
                                            Toast.makeText(this@ManageNamesActivity, "$customName added to list!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this@ManageNamesActivity, "Name already exists!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }

        backToHomeButton.setOnClickListener {
            finish()
        }
    }

    private fun loadAndDisplayNames() {
        runBlocking {
            launch {
                // Load names from the new names_table
                val allUniqueNames = database.nameDao().getAllNames()
                runOnUiThread {
                    currentNames.clear()
                    currentNames.addAll(allUniqueNames)
                    namesAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}