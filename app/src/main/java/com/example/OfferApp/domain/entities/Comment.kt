package com.example.OfferApp.domain.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId var id: String = "",
    val postId: String = "", // Added to know which post it belongs to
    val user: User? = null,
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
