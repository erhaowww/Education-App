package com.example.kleine.adapters.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kleine.R
import com.example.kleine.database.Reward

class RewardAdapter(var rewards: MutableList<Reward>) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {
    var onEditButtonClick: ((String) -> Unit)? = null // Lambda function to handle edit button click
    var onDeleteButtonClick: ((String) -> Unit)? = null

    class RewardViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        // Initialize your views here
        val rewardImage: ImageView = view.findViewById(R.id.rewardImage)
        val rewardNameText: TextView = view.findViewById(R.id.rewardNameText)
        val rewardDescriptionText: TextView = view.findViewById(R.id.rewardDescriptionText)
        val rewardPointsText: TextView = view.findViewById(R.id.rewardPointsText)
        val redeemLimitText: TextView = view.findViewById(R.id.redeemLimitText)
        val redeemedCountText: TextView = view.findViewById(R.id.redeemedCountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_view_reward_item, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]
        // Bind the reward data to your views here
        holder.rewardNameText.text = reward.rewardName
        holder.rewardDescriptionText.text = reward.rewardDescription
        holder.rewardPointsText.text = "Reward Points: " + reward.rewardPoints.toString()
        holder.redeemLimitText.text = "Redeem Limit: " + reward.redeemLimit.toString()
        holder.redeemedCountText.text = "Redeemed Count: " + reward.redeemedCount.toString()

        when {
            reward.imageBytes != null -> {
                // If imageBytes is available, load it
                Glide.with(holder.itemView.context)
                    .load(reward.imageBytes)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.rewardImage)
            }
            reward.imageUrl != null -> {
                // If imageUrl is available (and imageBytes is not), load the imageUrl
                Glide.with(holder.itemView.context)
                    .load(reward.imageUrl)
                    .into(holder.rewardImage)
            }
            else -> {
                // Handle the case where both imageBytes and imageUrl are null
                // For example, you can set a placeholder or an error image
            }
        }

        holder.view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            onEditButtonClick?.invoke(reward.rewardName) // Invoke the lambda function when edit is clicked
        }

        holder.view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            onDeleteButtonClick?.invoke(reward.rewardName) // Invoke the lambda function when delete is clicked
        }
    }

    override fun getItemCount() = rewards.size

    fun updateData(newRewards: List<Reward>) {
        rewards.clear()
        rewards.addAll(newRewards)
        notifyDataSetChanged()
    }
}

