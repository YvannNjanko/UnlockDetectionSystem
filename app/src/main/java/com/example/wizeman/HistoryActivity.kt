package com.example.wizeman

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val dataList = mutableListOf<Violation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(dataList)
        recyclerView.adapter = adapter

        fetchHistory()
    }

    private fun fetchHistory() {
        val db = FirebaseFirestore.getInstance()
        db.collection("violations")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val violation = document.toObject(Violation::class.java)
                    dataList.add(violation)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    companion object {
        private const val TAG = "HistoryActivity"
    }
}
