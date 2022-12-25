package com.vinodpatildev.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vinodpatildev.happyplaces.R
import com.vinodpatildev.happyplaces.databinding.ActivityMapBinding
import com.vinodpatildev.happyplaces.models.HappyPlaceModel

class MapActivity : AppCompatActivity(),OnMapReadyCallback {
    private var binding: ActivityMapBinding? = null
    private var mHappyPlaceDetailModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetailModel = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if(mHappyPlaceDetailModel != null){
            setSupportActionBar(binding?.toolbarMap)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = mHappyPlaceDetailModel?.title

            binding?.toolbarMap?.setNavigationOnClickListener {
                onBackPressed()
            }
            val supportMapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)

        }
    }

    override fun onMapReady(map: GoogleMap) {
        val position = LatLng(mHappyPlaceDetailModel!!.latitude, mHappyPlaceDetailModel!!.longitude)
        map.addMarker(MarkerOptions().position(position).title(mHappyPlaceDetailModel!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 8f)
        map.animateCamera(newLatLngZoom)
    }
}



