package com.example.kleine.fragments.quiz

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.FragmentResultBinding
import com.example.kleine.viewmodel.quiz.ResultViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : Fragment() {
    private lateinit var binding: FragmentResultBinding
    private lateinit var viewModel: ResultViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_result, container,false)
        viewModel = ViewModelProvider(this).get(ResultViewModel::class.java)
        // This is used so that the binding can observe LiveData updates
        binding.lifecycleOwner = viewLifecycleOwner

        val materialDocId = arguments?.getString("materialDocId") ?: ""
        val setID = arguments?.getString("setID") ?: ""

        val score = arguments?.getInt("Result", 0) ?: 0
        val totalQuestions = arguments?.getInt("TotalQuestions", 1) ?: 1 // replace with actual total questions
        val percentageScored = (score.toFloat() / totalQuestions) * 100
        binding.textView.text = "Score: $score"

        if (percentageScored >= 80) {
            viewModel.updateUserPoints(5)
            viewModel.storeQuizHistory(score, totalQuestions, materialDocId, setID)
            showCongratsDialog()
            binding.btnRestart.text = "Go to Profile"
            binding.btnRestart.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("materialDocId", materialDocId)
                }
                findNavController().navigate(R.id.action_resultFragment_to_orderDetails, bundle)
            }
        } else {
            binding.btnRestart.text = "Restart"
            binding.btnRestart.setOnClickListener {
                val action = ResultFragmentDirections.actionResultFragmentToQuizFragment(materialDocId)
                findNavController().navigate(action)
            }
        }
        return binding.root
    }

    private fun showCongratsDialog() {
        AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.baseline_stars_24) // Set the image or icon
            .setTitle("Congratulations!") // Set the title
            .setMessage("You have earn 5 points! You can use the points to claim your rewards!") // Set the description
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}