package com.example.kleine.fragments.quiz

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.FragmentAddUpdateQuestionBinding
import com.example.kleine.viewmodel.quiz.QuestionViewModel
import com.example.kleine.viewmodel.quiz.QuestionViewModelFactory

class AddUpdateQuestionFragment : Fragment() {
    private lateinit var binding: FragmentAddUpdateQuestionBinding
    private lateinit var viewModel: QuestionViewModel
    private lateinit var setDocumentId: String
    private lateinit var materialDocId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_update_question, container, false)
        setDocumentId = arguments?.getString("setDocumentId") ?: ""
        materialDocId = arguments?.getString("materialDocId") ?: ""
        viewModel = ViewModelProvider(this, QuestionViewModelFactory(setDocumentId, materialDocId)).get(QuestionViewModel::class.java)

        val questionId = arguments?.getString("questionId") // Replace with the correct key
        if (questionId != null) {
            viewModel.fetchSingleQuestion(materialDocId, setDocumentId, questionId).observe(viewLifecycleOwner, Observer { questionData ->
                binding.inputQuestion.setText(questionData["questionText"] as String)
                binding.editTextText2.setText(questionData["optionA"] as String)
                binding.editTextText3.setText(questionData["optionB"] as String)
                binding.editTextText4.setText(questionData["optionC"] as String)
                binding.editTextText5.setText(questionData["optionD"] as String)

                val correctAnswer = questionData["correctAnswer"] as String
                // Set the correct radio button based on correctAnswer
                when(correctAnswer) {
                    "optionA" -> binding.radioButton.isChecked = true
                    "optionB" -> binding.radioButton2.isChecked = true
                    "optionC" -> binding.radioButton3.isChecked = true
                    "optionD" -> binding.radioButton4.isChecked = true
                }
            })
        }

        binding.btnUploadQuestion.setOnClickListener {
            val questionText = binding.inputQuestion.text.toString()
            val answerA = binding.editTextText2.text.toString()
            val answerB = binding.editTextText3.text.toString()
            val answerC = binding.editTextText4.text.toString()
            val answerD = binding.editTextText5.text.toString()

            // Validate inputs
            if (questionText.isBlank()) {
                binding.inputQuestion.requestFocus()
                return@setOnClickListener
            }

            if (answerA.isBlank()) {
                binding.editTextText2.requestFocus()
                return@setOnClickListener
            }

            if (answerB.isBlank()) {
                binding.editTextText3.requestFocus()
                return@setOnClickListener
            }

            if (answerC.isBlank()) {
                binding.editTextText4.requestFocus()
                return@setOnClickListener
            }

            if (answerD.isBlank()) {
                binding.editTextText5.requestFocus()
                return@setOnClickListener
            }

            // Get selected RadioButton
            val selectedRadioButtonId = binding.optionContainer.checkedRadioButtonId
            val correctOption = when (selectedRadioButtonId) {
                R.id.radioButton -> "optionA"
                R.id.radioButton2 -> "optionB"
                R.id.radioButton3 -> "optionC"
                R.id.radioButton4 -> "optionD"
                else -> ""
            }

            val newQuestion = mapOf(
                "questionText" to questionText,
                "optionA" to answerA,
                "optionB" to answerB,
                "optionC" to answerC,
                "optionD" to answerD,
                "correctAnswer" to correctOption
            )

            if (questionId != null) {
                // Update the question
                viewModel.updateQuestion(materialDocId, setDocumentId, questionId, newQuestion)
            } else {
                // Add a new question
                viewModel.addNewQuestion(materialDocId, setDocumentId, newQuestion)
            }
        }

        viewModel.operationStatus.observe(viewLifecycleOwner, Observer { status ->
            if (status == "Success") {
                Toast.makeText(context, "Question successfully added!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "Failure") {
                Toast.makeText(context, "Failed to add question!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "UpdateSuccess") {
                Toast.makeText(context, "Question successfully updated!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "UpdateFailure") {
                Toast.makeText(context, "Failed to update question!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "DuplicateQuestion") {
                Toast.makeText(context, "The question already exists in this set!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            }
        })

        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

}
