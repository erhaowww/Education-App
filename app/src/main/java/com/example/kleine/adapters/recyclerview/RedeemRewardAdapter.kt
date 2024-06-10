package com.example.kleine.adapters.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kleine.databinding.RedeemRewardItemBinding
import com.example.kleine.model.Reward

class RedeemRewardAdapter(
    private val rewards: List<Reward>,
    private val onRedeemClickListener: (Reward) -> Unit
) : RecyclerView.Adapter<RedeemRewardAdapter.RewardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RedeemRewardItemBinding.inflate(layoutInflater, parent, false)
        return RewardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(rewards[position])
    }

    override fun getItemCount() = rewards.size

    inner class RewardViewHolder(private val binding: RedeemRewardItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reward: Reward) {
            binding.textViewVoucherName.text = reward.rewardName
            binding.textViewRewardDescription.text = reward.rewardDescription
            binding.buttonRedeem.text = reward.rewardPoints.toString()

            // Load image using Glide
            Glide.with(binding.root)
                .load(reward.imageUrl)
                .override(256, 256)
                .centerCrop()
                .into(binding.imageViewVoucher)

            binding.buttonRedeem.setOnClickListener {
                onRedeemClickListener(reward)
            }
        }
    }
}
