package com.example.kleine.fragments.shopping

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleine.R
import com.example.kleine.activities.ShoppingActivity
import com.example.kleine.adapters.recyclerview.MaterialAdapter
import com.example.kleine.database.SharedPreferencesHelper
import com.example.kleine.databinding.FragmentHomeBinding

import com.example.kleine.viewmodel.shopping.ShoppingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.example.kleine.resource.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {
    val TAG = "HomeFragment"
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var materialAdapter: MaterialAdapter
    private val sharedPreferencesHelper by lazy { SharedPreferencesHelper(requireContext()) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = (activity as ShoppingActivity).viewModel
    }
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferencesHelper.printSharedPreferences()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val status = document.getString("status")
                        Log.d(TAG, "User Status: $status")
                        if (status == "ADMINS") {
                            // Admin user, show the fragment
                            binding.frameAdd.visibility = View.GONE
                        }else if(status == "PARTNERS"){
                            binding.frameAdd.visibility = View.VISIBLE
                        }else{

                            binding.frameAdd.visibility = View.GONE
                        }
                    }
                }
            binding.frameAdd.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_addMaterialFragment)
            }
        }

        binding.fragmeMicrohpone.setOnClickListener {
            val snackBar = requireActivity().findViewById<CoordinatorLayout>(R.id.snackBar_coordinator)
            Snackbar.make(snackBar,resources.getText(R.string.g_coming_soon),Snackbar.LENGTH_SHORT).show()
        }

        // Initialize RecyclerView and Adapter
        materialAdapter = MaterialAdapter()  // No arguments here
        binding.productListRecycler.adapter = materialAdapter
        binding.productListRecycler.layoutManager = LinearLayoutManager(context)

        // Fetch materials from ViewModel and observe LiveData
        viewModel.getMaterials()  // This will update LiveData in ViewModel

        viewModel.materialsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Resource.Status.SUCCESS -> {
                    Log.d(TAG, "Fetched materials successfully. Item count: ${resource.data?.size}")
                    materialAdapter.differ.submitList(resource.data)
                }

                Resource.Status.ERROR -> {
                    Log.e(TAG, "Error fetching materials: ${resource.message}")
                }

                Resource.Status.LOADING -> {
                    Log.d(TAG, "Loading materials")
                }
            }
        }
        onItemClick()
    }

    private fun onItemClick() {
        materialAdapter.onItemClick = { material ->
            if (material != null) {
                val bundle = Bundle()
                bundle.putParcelable("material", material)
                findNavController().navigate(R.id.action_allOrdersFragment_to_materialDetailsFragment, bundle)
            } else {
                Log.e(TAG, "Material object is null")
            }
        }
    }



    override fun onResume() {
        super.onResume()
        val bottomNavigation =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}