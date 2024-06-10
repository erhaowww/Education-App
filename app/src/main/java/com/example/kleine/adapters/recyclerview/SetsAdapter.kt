package com.example.kleine.adapters.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.kleine.R

class SetsAdapter(
    private val context: Context,
    private var data: List<Pair<String, Int>>,
    private val itemClickListener: SetItemClickListener
    ) : BaseAdapter() {

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.partner_item_sets, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val item = getItem(position) as Pair<String, Int>
        viewHolder.bind(item)

        // Add OnClickListener to navigate to the QuestionFragment
        view.setOnClickListener {
            itemClickListener.onItemClick(item.first)
        }

        view.setOnLongClickListener {
            itemClickListener.onItemLongClick(item.first)
            true
        }

        return view
    }

    private class ViewHolder(view: View) {
        val setName: TextView = view.findViewById(R.id.setName)
        val setNumber: TextView = view.findViewById(R.id.setNumber)

        fun bind(item: Pair<String, Int>) {
            setName.text = "SET - "
            setNumber.text = item.second.toString()
        }
    }

    // If you want to refresh the data, you can use this function
    fun updateData(newData: List<Pair<String, Int>>) {
        data = newData
        notifyDataSetChanged()
    }

    interface SetItemClickListener {
        fun onItemClick(setDocumentId: String)
        fun onItemLongClick(setDocumentId: String)
    }

}
