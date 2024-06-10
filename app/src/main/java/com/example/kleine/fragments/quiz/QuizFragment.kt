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
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.FragmentQuizBinding
import com.example.kleine.viewmodel.quiz.QuizViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [QuizFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuizFragment : Fragment() {
    private lateinit var binding: FragmentQuizBinding
    private lateinit var viewModel: QuizViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_quiz,container,false)
        viewModel = ViewModelProvider(this).get(QuizViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner

        val materialDocId: String? = arguments?.getString("materialDocId")

        materialDocId?.let {
            viewModel.fetchAndDisplayMaterialName(it)
        }

        // Observe changes in ViewModel
        viewModel.materialNameLiveData.observe(viewLifecycleOwner, Observer{ materialName ->
            binding.materialName.text = materialName
        })

//        viewModel.showToastLiveData.observe(viewLifecycleOwner, Observer{ message ->
//            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//        })

        viewModel.navigateToPlayFragmentLiveData.observe(viewLifecycleOwner, Observer { setId ->
            val materialId = materialDocId ?: return@Observer
            if (setId != null) {
                val action = QuizFragmentDirections.actionQuizFragmentToPlayFragment(materialId, setId)
                findNavController().navigate(action)
                // Resetting the LiveData value to null after navigation
                viewModel.navigateToPlayFragmentLiveData.value = null
            }
        })

        viewModel.showToastLiveData.observe(this, Observer { message ->
            if (message == "No available sets with sufficient questions") {
                // Display a toast message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.resetToastLiveData()
            }
        })


        binding.btnPlay.setOnClickListener { view ->
            materialDocId?.let {
                viewModel.checkAvailableSetsAndNavigate(it, view)
            }
        }

        binding.btnExit.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("materialDocId", materialDocId)
            findNavController().navigate(R.id.action_quizFragment_to_orderDetails, bundle)
        }

        return binding.root
    }
}
