package com.example.voiceassistant.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.voiceassistant.databinding.ItemConversationBinding
import com.example.voiceassistant.models.ConversationMessage

class ConversationAdapter(
    private val messages: List<ConversationMessage>
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ConversationMessage) {
            binding.messageText.text = message.message
            binding.timestampText.text = message.timestamp

            // Style differently for user vs assistant messages
            if (message.isUser) {
                binding.messageText.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_blue_dark)
                )
                binding.messageText.setTypeface(null, Typeface.BOLD)
            } else {
                binding.messageText.setTextColor(
                    binding.root.context.getColor(android.R.color.black)
                )
                binding.messageText.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}