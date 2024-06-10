package com.example.kleine.fragments.quiz

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.databinding.FragmentPlayBinding
import com.example.kleine.viewmodel.quiz.PlayViewModel
import com.example.kleine.viewmodel.quiz.PlayViewModelFactory
import androidx.activity.OnBackPressedCallback


class PlayFragment : Fragment() {

    private lateinit var binding: FragmentPlayBinding
    private lateinit var viewModel: PlayViewModel

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Do nothing, thus disabling the back button
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_play, container, false)

        val materialID = arguments?.getString("materialDocId")
        val setID = arguments?.getString("randomSetId")
        viewModel = ViewModelProvider(this, PlayViewModelFactory(materialID, setID)).get(PlayViewModel::class.java)

        // Set the viewModel for data binding - this allows the bound layout access
        // to all the data in the VieWModel
        binding.playViewModel = viewModel

        // Specify the fragment view as the lifecycle owner of the binding.
        // This is used so that the binding can observe LiveData updates
        binding.lifecycleOwner = viewLifecycleOwner

//        binding.imageBack.setOnClickListener {
//            // handle back click
//            findNavController().navigateUp()
//        }

        binding.imageBack.visibility = View.GONE

        // Observe LiveData and set button backgrounds
        viewModel.btnBackground.observe(viewLifecycleOwner, Observer { resId ->
            binding.btnChoose1.setBackgroundResource(resId)
            binding.btnChoose2.setBackgroundResource(resId)
            binding.btnChoose3.setBackgroundResource(resId)
            binding.btnChoose4.setBackgroundResource(resId)
        })

        viewModel.toastMessage.observe(viewLifecycleOwner, Observer { message ->
            if (viewModel.showToastMsg.value!!) {
                Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
                viewModel.resetShowToastMsg()
            }
        })

        viewModel.showDialogEvent.observe(viewLifecycleOwner, Observer { shouldShow  ->
            if (shouldShow) {
                val dialog = Dialog(requireActivity())
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialog.setCancelable(false)
                dialog.setContentView(R.layout.timeout_dialog)

                // Assuming that tryAgain is the ID of the button in your timeout_dialog layout
                val tryAgainButton: Button = dialog.findViewById(R.id.tryAgain)
                tryAgainButton.setOnClickListener {
                    dialog.dismiss()
                    // Assuming you're using Navigation component, use findNavController to navigate
                    findNavController().popBackStack(R.id.quizFragment, false)
                }

                dialog.window?.setBackgroundDrawableResource(R.drawable.background_btn)
                dialog.show()
            }
        })

        viewModel.navigateToResult.observe(viewLifecycleOwner, Observer<Boolean> { shouldNavigate ->
            if (shouldNavigate) {
                // Create a Bundle and put the score inside
                val bundle = Bundle().apply {
                    putInt("Result", viewModel.scorePlayer.value!!)
                    putInt("TotalQuestions", viewModel.questionList.size)
                    putString("materialDocId", materialID)
                    putString("setID", setID)
                }
                findNavController().navigate(R.id.action_playFragment_to_resultFragment, bundle)
            }
        })

        // Add OnBackPressedCallback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        return binding.root
    }

    override fun onDestroyView() {
        // Disable the OnBackPressedCallback
        backPressCallback.isEnabled = false
        super.onDestroyView()
    }


}
