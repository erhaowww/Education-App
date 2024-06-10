package com.example.kleine.fragments.reward

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.kleine.R
import com.example.kleine.databinding.FragmentRewardBinding
import com.example.kleine.model.Address
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RewardFragment : Fragment() {

    private lateinit var binding: FragmentRewardBinding
    private val selectedAddress: Address? by lazy {
        arguments?.getParcelable("address")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reward, container, false)

        // Setup ViewPager2 with TabLayout
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Redeem"
                else -> "Reward History"
            }
        }.attach()

        binding.closeButton.setOnClickListener {
            // Handle the back navigation to profile fragment.
            findNavController().navigate(R.id.action_rewardFragment_to_profileFragment) // or findNavController().popBackStack()
        }

        return binding.root
    }

    inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val redeemRewardFragment = RedeemRewardFragment()
                    selectedAddress?.let {
                        val bundle = Bundle()
                        bundle.putParcelable("address", it)
                        redeemRewardFragment.arguments = bundle
                    }
                    redeemRewardFragment
                }
                else -> RewardHistoryFragment() // This is the new Fragment for reward history
            }
        }
    }
}
