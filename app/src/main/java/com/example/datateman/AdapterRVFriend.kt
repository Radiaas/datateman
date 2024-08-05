package com.example.datateman

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterRVFriend(
    private val context: Context,
    private var listItem: List<Friend>,
    private val onItemClick: (data: Friend) -> Unit,
    private val onItemEdit: (data: Friend) -> Unit,
    private val onItemDelete: (data: Friend) -> Unit
) : RecyclerView.Adapter<AdapterRVFriend.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.activity_item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val currentItem = listItem[position]

        holder.tvName.text = currentItem.name
        holder.tvSchool.text = currentItem.school
        holder.tvHobby.text = currentItem.hobby

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        holder.btnEdit.setOnClickListener {
            onItemEdit(currentItem)
        }

        holder.btnDelete.setOnClickListener {
            onItemDelete(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return listItem.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(listItem: List<Friend>) {
        this.listItem = listItem
        notifyDataSetChanged()
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvSchool: TextView = itemView.findViewById(R.id.tv_school)
        val tvHobby: TextView = itemView.findViewById(R.id.tv_hobby)
        val btnEdit: TextView = itemView.findViewById(R.id.btn_edit)
        val btnDelete: TextView = itemView.findViewById(R.id.btn_delete)
    }
}
