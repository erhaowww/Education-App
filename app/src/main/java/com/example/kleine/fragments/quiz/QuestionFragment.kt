package com.example.kleine.fragments.quiz

import android.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleine.R
import com.example.kleine.adapters.recyclerview.QuestionAdapter
import com.example.kleine.databinding.FragmentQuestionBinding
import com.example.kleine.viewmodel.quiz.QuestionViewModel
import com.example.kleine.viewmodel.quiz.QuestionViewModelFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [QuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuestionFragment : Fragment(), QuestionAdapter.QuestionItemClickListener {
    private lateinit var binding: FragmentQuestionBinding
    private lateinit var viewModel: QuestionViewModel
    private lateinit var setDocumentId: String
    private lateinit var materialDocId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_question, container,false)

        setDocumentId = arguments?.getString("setDocumentId") ?: ""
        materialDocId = arguments?.getString("materialDocId") ?: ""

        viewModel = ViewModelProvider(this, QuestionViewModelFactory(setDocumentId, materialDocId)).get(QuestionViewModel::class.java)
        binding.questionViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = QuestionAdapter(emptyList(), mutableMapOf(),this)
        binding.recyQuestion.adapter = adapter
        binding.recyQuestion.layoutManager = LinearLayoutManager(requireContext())

        viewModel.questionIdMap.observe(viewLifecycleOwner, Observer { idMap ->
            adapter.updateIdMap(idMap)  // Create this method in your Adapter to update the map
        })

        viewModel.questions.observe(viewLifecycleOwner) { questions ->
            adapter.updateQuestions(questions)
        }

        // Add this line to set up click listener for addQuestions button
        binding.addQuestions.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("setDocumentId", setDocumentId)
            bundle.putString("materialDocId", materialDocId)
            findNavController().navigate(R.id.action_questionFragment_to_addUpdateQuestionFragment, bundle)
        }

        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Observe operationStatus LiveData
        viewModel.operationStatus.observe(viewLifecycleOwner, Observer { status ->
            if (status == "Success") {
                Toast.makeText(context, "Question successfully added!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "Failure") {
                Toast.makeText(context, "Failed to add question!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "DeleteSuccess") {
                Toast.makeText(context, "Question successfully deleted!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            } else if (status == "DeleteFailure") {
                Toast.makeText(context, "Failed to delete question!", Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            }
        })

        return binding.root
    }

    override fun onQuestionClick(question: String, questionId: String) {
        val bundle = Bundle()
        bundle.putString("questionId", questionId)
        bundle.putString("setDocumentId", setDocumentId)
        bundle.putString("materialDocId", materialDocId)
        findNavController().navigate(R.id.action_questionFragment_to_addUpdateQuestionFragment, bundle)
    }

    // ... inside your QuestionFragment

    override fun onQuestionLongClick(question: String, questionId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Question")
        builder.setMessage("Are you sure you want to delete this question?")
        builder.setPositiveButton("Yes") { _, _ ->
            viewModel.deleteQuestion(materialDocId, setDocumentId, questionId)  // Implement this function in your ViewModel
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchQuestions(materialDocId, setDocumentId)
    }

}