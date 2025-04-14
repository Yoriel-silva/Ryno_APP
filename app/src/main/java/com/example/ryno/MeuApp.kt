package com.example.ryno

import android.app.Application
import com.cloudinary.android.MediaManager

class MeuApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config: HashMap<String, String> = hashMapOf(
            "cloud_name" to "dkdklks7s",
            "api_key" to "511314524176174",
            "api_secret" to "_1ZfonAKT_jHTEndBWJytJ0GHtY"
        )

        MediaManager.init(this, config)
    }
}