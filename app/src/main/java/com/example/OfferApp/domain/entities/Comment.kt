package com.example.OfferApp.domain.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId var id: String = "",
    val postId: String = "",
    val userId: String = "", // For easier querying
    val user: User? = null,
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
