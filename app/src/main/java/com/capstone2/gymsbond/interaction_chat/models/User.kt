package com.capstone2.gymsbond.interaction_chat.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class User {
    @Parcelize
    data class User(
        val uid: String,
        val name: String,
        val profileImageUrl: String?
    ) : Parcelable {
        constructor() : this("", "", "")
    }
}