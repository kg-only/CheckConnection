package com.example.checkconnection

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.checkconnection.databinding.ItemViewBinding

class MyAdapter :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    private val data = mutableListOf<Model>()

    inner class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        private val binding by viewBinding(ItemViewBinding::bind)

        fun bind(item: Model) {
            binding.url.text = item.url
            binding.timeout.text = item.timeoutRequest.toString()
            if (item.responseCode == 200) {
                binding.status.text = "200 success"
            }else{
                binding.status.text = "error"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item =
            LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return ViewHolder(item)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount() = data.size

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(list: List<Model>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()//read about this
    }
}