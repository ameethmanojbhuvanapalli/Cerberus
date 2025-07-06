package com.example.cerberus.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import com.example.cerberus.R
import com.example.cerberus.model.AuthenticatorTypeItem

class AuthTypeSelectAdapter(
    private val context: Context,
    private val items: List<AuthenticatorTypeItem>,
    private var selectedType: AuthenticatorTypeItem,
    private val onSelected: (AuthenticatorTypeItem) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = items[position]
        val isSelected = item == selectedType

        // Set icon and name (customize icons as you wish)
        holder.icon.setImageResource(item.iconRes)
        holder.appName.text = item.displayName

        // Set lock icon and card background (use checkmark/tint for selection)
        holder.lockIcon.setImageResource(R.drawable.ic_check_circle)
        holder.lockIcon.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.card.setBackgroundResource(
            if (isSelected) R.drawable.bg_gradient_dark_red else R.drawable.bg_gradient_black
        )

        holder.card.setOnClickListener {
            if (!isSelected) {
                selectedType = item
                notifyDataSetChanged()
                onSelected(item)
            }
        }

        return view
    }

    private class ViewHolder(view: View) {
        val card: CardView = view.findViewById(R.id.app_item_card)
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val lockIcon: ImageView = view.findViewById(R.id.lock_icon)
    }
}