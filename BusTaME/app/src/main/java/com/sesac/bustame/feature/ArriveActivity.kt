package com.sesac.bustame.feature

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.sesac.bustame.R
import com.sesac.bustame.databinding.ActivityArriveBinding
class ArriveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArriveBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arrive)

        Handler().postDelayed({
            startStartActivity()
        }, DURATION)
    }

    private fun startStartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val DURATION: Long = 3000
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
