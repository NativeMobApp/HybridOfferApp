package com.example.OfferApp.domain.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Post(
    @DocumentId var id: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var location: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var category: String = "",
    var price: Double = 0.0, // Price field added
    var user: User? = null,
    var scores: List<Score> = emptyList(),
    @ServerTimestamp val timestamp: Date? = null,
    var status: String = "activa",
    var store: String = ""
)
