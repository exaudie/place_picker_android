package com.example.placepickerv2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.placepickerv2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBind: ActivityMainBinding


    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val inDat = it.data?.extras
            if (inDat != null) {
                var koordinat = inDat.getString(MapsActivity.KOORDINAT, "")
                var koordinatName = inDat.getString(MapsActivity.ADDRESS, "")
                mBind.tilLocation.editText?.setText(if (koordinatName.equals("")) "Sudah ditandai" else koordinatName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBind = ActivityMainBinding.inflate(layoutInflater)
        val view = mBind.root
        setContentView(view)

        mBind.btnPlacePicker.setOnClickListener {
            val intMaps = Intent(this, MapsActivity::class.java)
            resultLauncher.launch(intMaps)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resultLauncher.unregister()
    }
}