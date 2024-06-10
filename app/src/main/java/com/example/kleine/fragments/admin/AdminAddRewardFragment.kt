package com.example.kleine.fragments.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kleine.R
import com.example.kleine.database.HelpDatabase
import com.example.kleine.databinding.FragmentAdminAddRewardBinding
import com.example.kleine.viewmodel.admin.AdminAddRewardViewModel
import com.example.kleine.viewmodel.admin.AdminAddRewardViewModelFactory


class AdminAddRewardFragment : Fragment() {

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGE = 1234
    }

    private lateinit var binding: FragmentAdminAddRewardBinding
    private lateinit var viewModel: AdminAddRewardViewModel
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_admin_add_reward, container, false)
        val appContext = requireContext().applicationContext
        val rewardDao = HelpDatabase.getDatabase(requireContext()).rewardDao()
        val factory = AdminAddRewardViewModelFactory(appContext, rewardDao)
        viewModel = ViewModelProvider(this, factory).get(AdminAddRewardViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.imgRewardPreview.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
        }

        // Handle the Save button click
        binding.btnAdd.setOnClickListener {
            val rewardName = binding.txtRewardName.text.toString().trim()
            val rewardDescription = binding.txtRewardDescription.text.toString().trim()
            val rewardPoints = binding.txtRewardPoints.text.toString().toIntOrNull()
            val redeemLimit = binding.txtRedeemLimit.text.toString().toIntOrNull()

            when {
                selectedImageUri == null -> {
                    Toast.makeText(context, "Please select an image!", Toast.LENGTH_SHORT).show()
                }

                rewardName.isEmpty() -> {
                    Toast.makeText(context, "Reward name cannot be empty!", Toast.LENGTH_SHORT).show()
                    binding.txtRewardName.requestFocus()
                }

                rewardDescription.isEmpty() -> {
                    Toast.makeText(context, "Reward description cannot be empty!", Toast.LENGTH_SHORT).show()
                    binding.txtRewardDescription.requestFocus()
                }

                rewardPoints == null || rewardPoints <= 0 -> {
                    Toast.makeText(context, "Reward points must be greater than zero!", Toast.LENGTH_SHORT).show()
                    binding.txtRewardPoints.requestFocus()
                }

                redeemLimit == null || redeemLimit <= 0 -> {
                    Toast.makeText(context, "Redeem limit must be greater than zero!", Toast.LENGTH_SHORT).show()
                    binding.txtRedeemLimit.requestFocus()
                }

                else -> {
                    viewModel.checkRewardNameExists(rewardName) { exists ->
                        if (exists) {
                            Toast.makeText(context, "Reward name already exists!", Toast.LENGTH_SHORT).show()
                            binding.txtRewardName.requestFocus()
                        } else {
                            //save into database
                            viewModel.saveReward(selectedImageUri, rewardName, rewardDescription, rewardPoints, redeemLimit)
                        }
                    }
                }
            }
        }

        // Observe upload success and show a message or navigate
        viewModel.uploadSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(context, "Reward added successfully!", Toast.LENGTH_SHORT).show()
                // Navigate back to AdminViewRewardFragment
                findNavController().navigateUp()
            }
        })

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        })

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }


        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let {
                binding.imgRewardPreview.setImageURI(it)
            }
        }
    }

}
