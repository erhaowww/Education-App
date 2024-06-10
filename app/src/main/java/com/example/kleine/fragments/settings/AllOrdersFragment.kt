package com.example.kleine.fragments.settings

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleine.R
import com.example.kleine.activities.ShoppingActivity
import com.example.kleine.adapters.recyclerview.AllOrdersAdapter
import com.example.kleine.adapters.recyclerview.MaterialAdapter
import com.example.kleine.databinding.FragmentAllOrdersBinding
import com.example.kleine.model.Enrollment
import com.example.kleine.model.Material
import com.example.kleine.resource.Resource
import com.example.kleine.viewmodel.shopping.ShoppingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore


class AllOrdersFragment : Fragment() {

    val TAG = "AllOrdersFragment"
    private lateinit var viewModel: ShoppingViewModel
    private lateinit var binding: FragmentAllOrdersBinding
    private lateinit var allOrdersAdapter: AllOrdersAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = (activity as ShoppingActivity).viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAllOrdersBinding.inflate(inflater)
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchEnrolledMaterials()
        setupRecyclerView()
        onCloseClick()
        onItemClick()
        onItemLongClick()
        setupEnrollmentListener()
    }


    private fun fetchEnrolledMaterials() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("enrollments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val enrollments = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Enrollment::class.java)
                    }

                    if (enrollments.isEmpty()) {
                        // If no enrollments, directly show the message and update UI
                        Toast.makeText(context, "You have not yet enrolled in any course", Toast.LENGTH_SHORT).show()
                        displayMaterials(emptyList())
                    } else {
                        fetchMaterialsForEnrollments(enrollments)
                        Log.d(TAG, "Number of enrollments fetched: ${enrollments.size}")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching user enrollments", exception)
                    // Handle the error appropriately
                }
        } else {
            // Handle the case where the user is not logged in
        }
    }

    private fun fetchMaterialsForEnrollments(enrollments: List<Enrollment>) {
        val firestore = FirebaseFirestore.getInstance()

        // Filter out empty strings and remove duplicate IDs
        val materialIds = enrollments.map { it.materialId }.filter { it.isNotEmpty() }.distinct()

        // Log the IDs being used in the query for debugging purposes
        Log.d(TAG, "Attempting to fetch materials with IDs: $materialIds")

        // Split the materialIds list into chunks of size 10 or fewer
        val chunks = materialIds.chunked(10)

        // Initialize an empty list to hold the fetched materials
        val materials = mutableListOf<Material>()

        // Define a counter to keep track of completed queries
        var completedQueries = 0

        // Iterate over each chunk and perform a query
        for (chunk in chunks) {
            firestore.collection("Materials")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val chunkMaterials = querySnapshot.documents.mapNotNull { document ->
                        val material = document.toObject(Material::class.java)
                        material?.id = document.id // Set the id of the Material object
                        material
                    }
                    Log.d(TAG, "Materials fetched successfully: $chunkMaterials")
                    materials.addAll(chunkMaterials)

                    // Check if all queries are completed
                    completedQueries++
                    if (completedQueries == chunks.size) {
                        // All queries are completed, display the materials
                        displayMaterials(materials)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching materials", exception)
                    // Handle the error appropriately
                }
        }
    }


    private fun onItemLongClick() {
        allOrdersAdapter.onItemLongClick = { material ->
            // Create an AlertDialog.Builder to build the confirmation dialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Unenroll Course")
            builder.setMessage("Are you sure you want to unenroll from this course?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                // User confirmed, proceed with unenrollment/archiving
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("enrollments")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("materialId", material.id)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                document.reference.update("archived", true)
                            }
                            Toast.makeText(context, "Material archived successfully", Toast.LENGTH_SHORT).show()
                            // No need to explicitly fetch enrolled materials here,
                            // the snapshot listener will handle it
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Error archiving material", exception)
                            Toast.makeText(context, "Error archiving material", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                // User canceled, dismiss the dialog
                dialog.dismiss()
            }
            // Show the dialog
            builder.show()
        }
    }




    private fun displayMaterials(materials: List<Material>) {
        Log.d(TAG, "Displaying materials: ${materials.size}")
        allOrdersAdapter.differ.submitList(materials)
        binding.rvAllOrders.invalidate()
    }


    private fun onItemClick() {
        allOrdersAdapter.onItemClick = { material ->
            val bundle = Bundle()
            bundle.putString("materialDocId", material.id)
            findNavController().navigate(R.id.action_allOrdersFragment_to_orderDetails, bundle)
        }

    }

    private fun setupEnrollmentListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("enrollments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }

                    Log.d(TAG, "SnapshotListener triggered")

                    val enrollments = snapshots?.documents?.mapNotNull { document ->
                        document.toObject(Enrollment::class.java)
                    } ?: emptyList()

                    Log.d(TAG, "Updated enrollments: $enrollments")

                    if (enrollments.isEmpty()) {
                        Toast.makeText(context, "You have not yet enrolled in any course", Toast.LENGTH_SHORT).show()
                        displayMaterials(emptyList())
                    } else {
                        fetchMaterialsForEnrollments(enrollments)
                    }
                }
        }
    }





    private fun onCloseClick() {
        binding.imgCloseOrders.setOnClickListener {
            findNavController().navigateUp()
        }
    }



    private fun hideLoading() {
        binding.progressbarAllOrders.visibility = View.GONE

    }

    private fun showLoading() {
        binding.progressbarAllOrders.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        allOrdersAdapter = AllOrdersAdapter()
        binding.rvAllOrders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allOrdersAdapter
        }
    }
}