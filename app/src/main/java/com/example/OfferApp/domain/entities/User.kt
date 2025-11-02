package com.example.OfferApp.domain.entities

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var uid: String = "",
    var username: String = "",
    var email: String = "",
    var profileImageUrl: String = "", // Field for profile picture URL
    var followers: List<String> = listOf(),
    var following: List<String> = listOf()
)
