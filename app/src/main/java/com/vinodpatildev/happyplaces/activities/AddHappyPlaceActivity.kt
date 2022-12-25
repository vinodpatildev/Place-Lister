package com.vinodpatildev.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.happyplaces.database.DatabaseHandler
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.vinodpatildev.happyplaces.R
import com.vinodpatildev.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.vinodpatildev.happyplaces.models.HappyPlaceModel
import com.vinodpatildev.happyplaces.utils.GetAddressFromLatLng
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var binding: ActivityAddHappyPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mHappyPlaceDetails : HappyPlaceModel? = null
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


    private val cameraLauncher = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ){
        result ->
        if(result.resultCode == RESULT_OK){
            val selectedImage = result.data?.extras?.get("data") as Bitmap
            binding?.ivPlaceImage?.setImageBitmap(selectedImage)

            saveImageToInternalStorage = saveImageToInternalStorage(selectedImage)
        }

    }
    private val gallaryLauncher = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ){
        result ->
        if(result.resultCode == RESULT_OK){
            val uri: Uri? = result.data?.data
            binding?.ivPlaceImage?.setImageURI( uri )
            val selectedImage = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
//            val selectedImage = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver, uri!! ))
            saveImageToInternalStorage = saveImageToInternalStorage(selectedImage)
        }
    }

    private val mapLauncher = registerForActivityResult( ActivityResultContracts.StartActivityForResult() ){
            result ->
        if(result.resultCode == RESULT_OK){
            Log.d("MAP","map launched")
            val place: Place? = result.data?.let { Autocomplete.getPlaceFromIntent(it) }
            binding?.etLocation?.setText(place?.address)
            mLatitude = place?.latLng!!.latitude
            mLongitude = place?.latLng!!.longitude
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlaces)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding?.toolbarAddPlaces?.setNavigationOnClickListener {
            onBackPressed()
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel

        }
        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        updateDateInView()

        if(mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit happy place"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)

            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            binding?.ivPlaceImage?.setImageURI((saveImageToInternalStorage))

            binding?.btnSave?.text = "SUBMIT"
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)


        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity, resources.getString((R.string.google_maps_api_key)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(binding != null){
            binding = null
        }
    }

    override fun onClick(view: View?) {
        when(view!!.id){
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.et_location->{
                try {
                    val fields = listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@AddHappyPlaceActivity)
                    mapLauncher.launch(intent)

                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location -> {
                if(!isLocationEnabled()){

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)

                }else{
                    Dexter.withContext(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        .withListener(object: MultiplePermissionsListener{
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if(report!!.areAllPermissionsGranted()){
                                    requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: MutableList<PermissionRequest>?,
                                report: PermissionToken?
                            ) {
                                val message = "You need to enable location services in order to access map. You can do this in settings."
                                showRationalDialogForPermissions(message)
                            }

                        })
                        .onSameThread()
                        .check()
                }
            }
            R.id.tv_add_image ->{
                AlertDialog.Builder(this)
                    .setTitle("Select Action")
                    .setItems( arrayOf("Select photo from Gallery","Capture photo from camera") ){
                            dialog, which ->
                        when(which){
                            0 -> {
                                choosePhotoFromGallery()
                            }
                            1 -> {
                                choosePhotoFromCamera()
                            }
                        }
                    }
                    .create()
                    .show()
            }
            R.id.btn_save ->{
                when{
                    binding?.etTitle?.text.isNullOrEmpty() ->{
                        Snackbar.make(view,"Please enter title.",Snackbar.LENGTH_LONG).show()
                    }
                    binding?.etDescription?.text.isNullOrEmpty() ->{
                        Snackbar.make(view,"Please enter description.",Snackbar.LENGTH_LONG).show()
                    }
                    binding?.etLocation?.text.isNullOrEmpty() ->{
                        Snackbar.make(view,"Please enter location.",Snackbar.LENGTH_LONG).show()
                    }
                    saveImageToInternalStorage == null ->{
                        Snackbar.make(view,"Please select and image.",Snackbar.LENGTH_LONG).show()
                    }
                    else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            mHappyPlaceDetails?.id ?: 0,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        val addHappyPlace : Int
                        if(mHappyPlaceDetails != null){
                            addHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                        }else{
                            addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                        }
                        if(addHappyPlace > 0){
                            Snackbar.make(view,"Data inserted successfully.",Snackbar.LENGTH_LONG).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }

                }

            }
        }
    }


    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())

    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            val mLastLocation: Location = result.lastLocation!!
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude
            Log.d("TESLA-LOG","lattitude: $mLatitude")
            Log.d("TESLA-LOG","longitude: $mLongitude")

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)
            addressTask.setAddressListener(object: GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address:String?){
                    binding?.etLocation?.setText(address)
                    Log.e("TESTA-LOG","Address found $address")
                }
                override fun onError(){
                    Log.e("TESTA-LOG","Get Address - Something went wrong.")
                }
            })
            addressTask.getAddress()
        }
    }

    private fun choosePhotoFromCamera() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }else{
            Dexter.withContext(this)
                .withPermission(
                    Manifest.permission.CAMERA
                )
                .withListener(object: PermissionListener{
                    override fun onPermissionGranted(report: PermissionGrantedResponse?) {
                        Toast.makeText(this@AddHappyPlaceActivity,"Permission granted for CAMERA.", Toast.LENGTH_LONG).show()
                    }

                    override fun onPermissionDenied(report: PermissionDeniedResponse?) {
                        Toast.makeText(this@AddHappyPlaceActivity,"Permission denied for CAMERA.", Toast.LENGTH_LONG).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        val message = "It looks like you have turned off CAMERA permission required for this feature. It can be enabled under the Application Settings."
                        showRationalDialogForPermissions(message)
                    }
                })
                .onSameThread()
                .check()
        }
    }
    private fun choosePhotoFromGallery() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            val pickIntent = Intent( Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI )
            gallaryLauncher.launch(pickIntent)
        }else{
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object: MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted()){
                            Toast.makeText(this@AddHappyPlaceActivity,"Storage READ/WRITE permissions are granted.",Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        val message = "It looks like you have turned off permission required for this feature. It can be enabled under the Application Settings."
                        showRationalDialogForPermissions(message)
                    }
                })
                .onSameThread()
                .check()
        }
    }
    private fun showRationalDialogForPermissions(message: String) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("GO TO SETTINGS"){_,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){dialog, which ->
                dialog.dismiss()
            }
            .show()
    }
    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_APPEND)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress( Bitmap.CompressFormat.JPEG, 90, stream )
            stream.flush()
            stream.close()

        }catch (e: IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }
    private fun updateDateInView(){
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())

    }
    companion object{
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"

    }
}











