package com.justself.klique

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.justself.klique.AppUpdateManager.fetchRequiredVersionFromServer
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FetchRequiredVersionIntegrationTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        // Use the actual application context for SharedPreferences
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = context.getSharedPreferences(
            "MyKliquePrefs", // Match your actual SharedPreferences name
            Context.MODE_PRIVATE
        )

        // Clear SharedPreferences before each test
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `fetchRequiredVersionFromServer saves version and expiration date`() = runBlocking {
        // Ensure the server has a valid endpoint
        // Your `NetworkUtils.makeRequest` must point to the actual endpoint.

        // Call the function for real
        fetchRequiredVersionFromServer()

        // Verify the values saved to SharedPreferences
        val savedVersion = sharedPreferences.getInt("REQUIRED_VERSION", -1)
        val savedExpirationDate = sharedPreferences.getLong("EXPIRATION_DATE", -1L)

        // Assertions (Update the expected values based on the actual server response)
        assertEquals(2, savedVersion) // Replace `2` with the expected minimum version
        assertEquals(1704067200000L, savedExpirationDate) // Replace with the expected expiration date
    }
}