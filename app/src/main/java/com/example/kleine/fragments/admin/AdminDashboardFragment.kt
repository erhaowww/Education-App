package com.example.kleine.fragments.admin

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kleine.BuildConfig
import com.example.kleine.R
import com.example.kleine.activities.LunchActivity
import com.example.kleine.activities.ShoppingActivity
import com.example.kleine.databinding.FragmentAdminDashboardBinding
import com.example.kleine.databinding.FragmentJoinPartnerBinding
import com.example.kleine.databinding.FragmentProfileBinding
import com.example.kleine.databinding.FragmentReplyCommentBinding
import com.example.kleine.databinding.FragmentViewPartnershipBinding
import com.example.kleine.model.User
import com.example.kleine.resource.Resource
import com.example.kleine.util.Constants.Companion.UPDATE_ADDRESS_FLAG
import com.example.kleine.viewmodel.shopping.ShoppingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth


class AdminDashboardFragment() : Fragment(), Parcelable {
    val TAG = "AdminDashboardFragment"
    private lateinit var binding: FragmentAdminDashboardBinding
    private lateinit var viewModel: ShoppingViewModel

    constructor(parcel: Parcel) : this() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = (activity as ShoppingActivity).viewModel
        viewModel.getUser()
    }

    override fun onCreateView (
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onViewPartnershipDataClick()
        onViewRewardDataClick()

    }

    private fun onViewPartnershipDataClick() {
        binding.viewPartnershipData.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminViewPartnershipFragment)
        }
    }

    private fun onViewRewardDataClick() {
        binding.viewRewardData.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_adminViewRewardFragment)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AdminDashboardFragment> {
        override fun createFromParcel(parcel: Parcel): AdminDashboardFragment {
            return AdminDashboardFragment(parcel)
        }

        override fun newArray(size: Int): Array<AdminDashboardFragment?> {
            return arrayOfNulls(size)
        }
    }


}