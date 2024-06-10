package com.example.kleine.fragments.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleine.R
import com.example.kleine.adapters.recyclerview.RewardAdapter
import com.example.kleine.database.HelpDatabase
import com.example.kleine.database.Reward
import com.example.kleine.databinding.FragmentAdminViewRewardBinding
import com.example.kleine.viewmodel.admin.AdminViewRewardViewModel
import com.example.kleine.viewmodel.admin.AdminViewRewardViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AdminViewRewardFragment : Fragment() {

    private lateinit var binding: FragmentAdminViewRewardBinding
    private lateinit var viewModel: AdminViewRewardViewModel
    private lateinit var adapter: RewardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_admin_view_reward, container, false)
        val toolbar: Toolbar = binding.toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        setHasOptionsMenu(true)
        val appContext = requireContext().applicationContext
        val rewardDao = HelpDatabase.getDatabase(requireContext()).rewardDao()
        val factory = AdminViewRewardViewModelFactory(appContext, rewardDao)
        viewModel = ViewModelProvider(this, factory).get(AdminViewRewardViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        // Set LayoutManager for the RecyclerView
        binding.rv.layoutManager = LinearLayoutManager(context)
        adapter = RewardAdapter(mutableListOf()).apply {
            onEditButtonClick = { rewardName ->
                val action = AdminViewRewardFragmentDirections.actionAdminViewRewardFragmentToAdminUpdateRewardFragment(rewardName)
                findNavController().navigate(action)
            }
            onDeleteButtonClick = { rewardName ->
                showDeleteConfirmationDialog(rewardName)
            }
        }
        binding.rv.adapter = adapter

        viewModel.rewards.observe(viewLifecycleOwner) { rewards ->
            adapter.updateData(rewards)
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Reward successfully deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete reward! Pls check your connection", Toast.LENGTH_SHORT).show()
            }
        }

        binding.floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminViewRewardFragment_to_adminAddRewardFragment)
        }

        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        Log.d("Menu", "onCreateOptionsMenu is called")
        inflater.inflate(R.menu.search, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle query submission
                query?.let {
                    searchInFirestore(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle query text change
                newText?.let {
                    if (it.isEmpty()) {
                        fetchAllRecords() // fetch and display all records
                    } else {
                        searchInFirestore(it) // perform the search based on input
                    }
                }
                return true
            }
        })

    }

    private fun fetchAllRecords() {
        val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")
        rewardsCollection.get().addOnSuccessListener { snapshot ->
            val rewardList = mutableListOf<Reward>()
            for (document in snapshot.documents) {
                val reward = document.toObject(Reward::class.java)
                if (reward != null) {
                    rewardList.add(reward)
                }
            }
            viewModel.rewards.value = rewardList
        }
    }


    private fun searchInFirestore(query: String) {
        val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")
        val lowerBound = query.toLowerCase(Locale.getDefault())
        val upperBound = lowerBound + '\uf8ff'
        rewardsCollection.whereGreaterThanOrEqualTo("rewardName", lowerBound)
            .whereLessThanOrEqualTo("rewardName", upperBound)
            .get()
            .addOnSuccessListener { snapshot ->
                val rewardList = mutableListOf<Reward>()
                for (document in snapshot.documents) {
                    val reward = document.toObject(Reward::class.java)
                    if (reward != null) {
                        rewardList.add(reward)
                    }
                }
                viewModel.rewards.value = rewardList
            }
    }


    private fun showDeleteConfirmationDialog(documentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reward")
            .setMessage("Are you sure you want to delete this reward?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteReward(documentId)
            }
            .setNegativeButton("No", null)
            .show()
    }

//    override fun onResume() {
//        super.onResume()
//        viewModel.loadDataBasedOnConnection()
//    }

}