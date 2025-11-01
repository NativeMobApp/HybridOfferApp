package com.example.OfferApp

import android.app.Application
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // <<< Firebase initialization added back
        val config = mapOf(
            "cloud_name" to "dyloasili",
            "api_key" to "272119243465887",
            "api_secret" to "V4ZMOhOrpI1augaCADHeOKrCXTQ"
        )
        MediaManager.init(this, config)
    }
}