package com.example.kleine.adapters.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kleine.R

class QuestionAdapter(
    private var questions: List<String>,
    private var questionIdMap: Map<String, String>,
    private val itemClickListener: QuestionItemClickListener
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.question)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val question = questions[position]
                    val questionId = questionIdMap[question] ?: ""
                    itemClickListener.onQuestionClick(question, questionId)
                }
            }

            itemView.setOnLongClickListener {  // Add this block for long-click
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val question = questions[position]
                    val questionId = questionIdMap[question] ?: ""
                    itemClickListener.onQuestionLongClick(question, questionId)
                    true  // Indicating that the long-click event is handled
                } else {
                    false
                }
            }
        }

        fun bind(question: String) {
            questionText.text = question
        }
    }

    interface QuestionItemClickListener {
        fun onQuestionClick(question: String, questionId: String)
        fun onQuestionLongClick(question: String, questionId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.partner_item_question, parent, false)
        return QuestionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount() = questions.size

    fun updateQuestions(newQuestions: List<String>) {
        questions = newQuestions
        notifyDataSetChanged()
    }

    fun updateIdMap(newMap: Map<String, String>) {
        this.questionIdMap = newMap
        notifyDataSetChanged()
    }
}