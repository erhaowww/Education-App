package com.example.kleine.adapters.recyclerview


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kleine.R
import com.example.kleine.model.PassedQuiz
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PassedQuizzesAdapter(private var quizzes: MutableList<PassedQuiz> = mutableListOf()) :
    RecyclerView.Adapter<PassedQuizzesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val materialNameTextView: TextView = itemView.findViewById(R.id.tv_material_name)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        val setNameTextView: TextView = itemView.findViewById(R.id.tv_set_name)
        val scoreTextView: TextView = itemView.findViewById(R.id.tv_score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.passed_quiz_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quiz = quizzes[position]
        holder.materialNameTextView.text = quiz.materialName
        holder.dateTextView.text = formatDate(quiz.date)
        holder.setNameTextView.text = quiz.setName
        holder.scoreTextView.text = quiz.score
    }

    private fun formatDate(dateStr: String): String? {
        val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        // If the date string matches the pattern "01-10-2023" (i.e., dd-MM-yyyy)
        if (dateStr.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
            return dateStr
        }

        // Pattern to match "Sun Sep 24 18:06:24 GMT 2023"
        val firestoreSDF = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy", Locale.ENGLISH)
        val firestoreDate: Date? = firestoreSDF.parse(dateStr)

        return firestoreDate?.let { outputFormat.format(it) }
    }



    override fun getItemCount() = quizzes.size

    fun updateQuizzes(newQuizzes: List<PassedQuiz>) {
        quizzes.clear()
        quizzes.addAll(newQuizzes)
        notifyDataSetChanged()
    }
}



