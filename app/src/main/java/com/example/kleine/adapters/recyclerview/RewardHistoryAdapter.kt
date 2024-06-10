package com.example.kleine.adapters.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleine.databinding.RewardHistoryItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RewardHistoryAdapter(
    private val rewardHistory: List<Map<String, Any>>
) : RecyclerView.Adapter<RewardHistoryAdapter.RewardHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RewardHistoryItemBinding.inflate(inflater, parent, false)
        return RewardHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RewardHistoryViewHolder, position: Int) {
        holder.bind(rewardHistory[position])
    }

    override fun getItemCount() = rewardHistory.size

    class RewardHistoryViewHolder(private val binding: RewardHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(history: Map<String, Any>) {
            binding.textViewRewardName.text = history["rewardName"] as? String
            binding.textViewRewardDetails.text = history["rewardDetails"] as? String

            // Format and display the redeemed date appropriately
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            // Check for Firestore Timestamp
            val timestamp = history["redeemedDate"] as? com.google.firebase.Timestamp
            val date = timestamp?.toDate()
            if (date != null) {
                binding.textViewRewardDate.text = sdf.format(date)
            } else {
                // Check for RoomDB Timestamp (in milliseconds)
                val millis = history["redeemedDate"] as? Long
                if (millis != null) {
                    val roomDate = Date(millis)
                    binding.textViewRewardDate.text = sdf.format(roomDate)
                }
            }
        }
    }
}
