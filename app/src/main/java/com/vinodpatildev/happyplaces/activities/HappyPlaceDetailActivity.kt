package com.vinodpatildev.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vinodpatildev.happyplaces.R
import com.vinodpatildev.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.vinodpatildev.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private var binding : ActivityHappyPlaceDetailBinding? = null
    private var happyPlaceDetailModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            happyPlaceDetailModel = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS)!! as HappyPlaceModel
        }
        if(happyPlaceDetailModel != null){
            setSupportActionBar(binding?.toolbarHappyPlaceDetail)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = happyPlaceDetailModel!!.title

            binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener {
                onBackPressed()
            }
        }

        binding?.ivPlaceImage?.setImageURI(Uri.parse(happyPlaceDetailModel?.image))
        binding?.tvDescription?.text = happyPlaceDetailModel?.description
        binding?.tvLocation?.text = happyPlaceDetailModel?.location

        binding?.btnViewOnMap?.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,happyPlaceDetailModel)
            startActivity(intent)
        }


    }
}