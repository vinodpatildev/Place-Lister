package com.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.happyplaces.database.DatabaseHandler
import com.vinodpatildev.happyplaces.R
import com.vinodpatildev.happyplaces.activities.AddHappyPlaceActivity
import com.vinodpatildev.happyplaces.activities.MainActivity
import com.vinodpatildev.happyplaces.databinding.ItemHappyPlaceBinding
import com.vinodpatildev.happyplaces.models.HappyPlaceModel

open class HappyPlacesAdapter( private val context: Context, private var list: ArrayList<HappyPlaceModel> ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder( LayoutInflater.from(context).inflate( R.layout.item_happy_place, parent, false ) )
//        return MyViewHolder( LayoutInflater.from(context).inflate( R.layout.item_happy_place, parent, false ) )
//        return MyViewHolder( ItemHappyPlaceBinding.inflate( LayoutInflater.from(parent.context), parent, false ))
    }
override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {
            holder.itemView.findViewById<ImageView>(R.id.ivPlaceImage).setImageURI(Uri.parse(model.image))
            holder.itemView.findViewById<TextView>(R.id.tvTitle).text = model.title
            holder.itemView.findViewById<TextView>(R.id.tvDescription).text = model.description
            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }
    override fun getItemCount(): Int {
        return list.size
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int, launcher : ActivityResultLauncher<Intent> ) {
        Log.d("SWAP","notifyEditItem()")
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
//        activity.startActivityForResult(
//                intent,
//                requestCode
//        )
        launcher.launch(intent)
        // Activity is started with requestCode
        notifyItemChanged(position) // Notify any registered observers that the item at position has changed.
    }
    fun removeAt(position: Int) {
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteHappyPlace(list[position])
        if (isDeleted > 0) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }
    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}