package com.justself.klique

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    var profilePictureUrl: String = "https://fastly.picsum.photos/id/433/450/300.jpg?hmac=FIWPaQ-to3njMOAmnwI8dg-5TyOzFGi1nVVfEoEERf4"
        private set
    var bio: String = "This is the user's bio."
        private set

    fun updateProfile(newProfilePictureUri: Uri?, newBio: String) {
        if (newProfilePictureUri != null) {
            Log.d("Update Profile", "Uri is $newProfilePictureUri")
        }
        if (newBio != bio) {
            Log.d("Update Profile", "Bio is called?")
        }
    }
}