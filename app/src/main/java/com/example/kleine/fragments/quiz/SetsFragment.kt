package com.example.kleine.fragments.quiz

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.adapters.recyclerview.SetsAdapter
import com.example.kleine.databinding.FragmentSetsBinding
import com.example.kleine.viewmodel.quiz.SetsViewModel
import com.example.kleine.viewmodel.quiz.SetsViewModelFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SetsFragment : Fragment(), SetsAdapter.SetItemClickListener {
    private lateinit var binding: FragmentSetsBinding
    private lateinit var viewModel: SetsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sets, container,false)

        val materialDocId = arguments?.getString("materialDocId")
        if (materialDocId != null) {
            viewModel = ViewModelProvider(this, SetsViewModelFactory(materialDocId)).get(SetsViewModel::class.java)
        } else {
            // Handle the case when materialDocId is null, e.g., show an error message.
        }

        // Set the viewModel for data binding - this allows the bound layout access
        // to all the data in the VieWModel
        binding.setsViewModel = viewModel

        // Specify the fragment view as the lifecycle owner of the binding.
        // This is used so that the binding can observe LiveData updates
        binding.lifecycleOwner = viewLifecycleOwner

        // Initialize the adapter with an empty list and set it to the GridView.
        val adapter = SetsAdapter(requireContext(), emptyList(), this)
        binding.gridView.adapter = adapter

        // Observe the LiveData from the ViewModel.
        viewModel.sets.observe(viewLifecycleOwner, Observer { sets ->
            // Update the adapter data and refresh the GridView.
            adapter.updateData(sets)
        })

        binding.addQuestions.setOnClickListener {
            if (materialDocId != null) {
                viewModel.addNewSet(materialDocId)
            } else {
                // Handle the case when materialDocId is null, e.g., show an error message.
            }
        }

        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    // Implement the onItemClick method
    override fun onItemClick(setDocumentId: String) {
        val materialDocId = arguments?.getString("materialDocId") ?: return
        val bundle = bundleOf(
            "setDocumentId" to setDocumentId,
            "materialDocId" to materialDocId
        )

        findNavController().navigate(R.id.action_setsFragment_to_questionFragment, bundle)
    }

    override fun onItemLongClick(setDocumentId: String) {
        val materialDocId = arguments?.getString("materialDocId") ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Set")
            .setMessage("Are you sure you want to delete this set?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteSet(materialDocId, setDocumentId)
            }
            .setNegativeButton("No", null)
            .show()
    }

}