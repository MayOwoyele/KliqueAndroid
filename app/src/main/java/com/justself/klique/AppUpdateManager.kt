package com.justself.klique

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random
import com.google.android.play.core.install.model.UpdateAvailability

object AppUpdateManager {
    private const val EXPIRATION_DATE_KEY = "EXPIRATION_DATE"
    private const val SHARED_PREFS_NAME = "APP_PREFS"
    val updateDismissedFlow = MutableStateFlow(false)

    private fun saveExpirationDate(context: Context, expirationDateMillis: Long) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong(EXPIRATION_DATE_KEY, expirationDateMillis)
            .apply()
    }

    fun initialize() {
        val randomNumber = Random.nextInt(1, 101)
        if (randomNumber < 25) {
            fetchRequiredVersionFromServer()
        }
    }

    fun getExpirationDate(context: Context): Long {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(EXPIRATION_DATE_KEY, 0L)
    }

    fun fetchRequiredVersionFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mapOf("platform" to "android")
                val response =
                    NetworkUtils.makeRequest("fetchRequiredVersion", KliqueHttpMethod.GET, params)
                if (response.first) {
                    Logger.d("Minimum version", response.second)
                    val jsonObject = JSONObject(response.second)
                    val requiredVersion = jsonObject.getInt("minimumVersion")
                    val expirationDate = jsonObject.getString("expirationDate").toLong()
                    val sharedPreferences =
                        MyKliqueApp.appContext.getSharedPreferences(
                            SHARED_PREFS_NAME,
                            Context.MODE_PRIVATE
                        )
                    sharedPreferences.edit().putInt("REQUIRED_VERSION", requiredVersion).apply()
                    saveExpirationDate(MyKliqueApp.appContext, expirationDate)
                }
            } catch (e: Exception) {
                Logger.d("Minumum version", e.toString())
                e.printStackTrace()
            }
        }
    }

    fun getRequiredVersion(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt("REQUIRED_VERSION", 1)
    }

    fun isUpdateRequired(context: Context): Boolean {
        val currentVersion = BuildConfig.VERSION_CODE
        val requiredVersion = getRequiredVersion(context)
        val expirationDate = getExpirationDate(context)
        val twoWeeksMillis = 14 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        return currentVersion < requiredVersion && (expirationDate - currentTime <= twoWeeksMillis)
    }

    fun dismissUpdateRequirement() {
        updateDismissedFlow.value = true
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
 *  Android‑side “App Update Manager” that behaves like the iOS one
 *  ▸  Pulls the latest version straight from Google Play (HTML scrape fallback)
 *  ▸  Still honours your existing ‘minimumVersion/expirationDate’ JSON endpoint
 *  ▸  Publishes the same Flow your AuthViewModel already consumes
 *  ▸  Grace‑period logic mirrors the Swift implementation (ask→force)
 *  ▸  Uses versionCode (BuildConfig.VERSION_CODE) for the comparison
 * ────────────────────────────────────────────────────────────────────────────── */

object DroidAppUpdateManager {
    private const val PREFS = "APP_PREFS"
    private const val KEY_FIRST_SEEN = "FIRST_SEEN_UPDATE_DATE"

    /** 2 weeks to nag, 4 weeks to force, same as iOS */
    const val ASK_AFTER = 14 * 24 * 60 * 60 * 1000L
    const val FORCE_AFTER = 28 * 24 * 60 * 60 * 1000L

    private lateinit var updateManagerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val prefs
        get() =
            MyKliqueApp.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** public state your ViewModel already consumes */
    val updateAvailableFlow = MutableStateFlow(false)
    val updateDismissedFlow = MutableStateFlow(false)

    /**
     * Call from Application.onCreate() —
     * this sets up the Play-Core client and watches for UPDATE_AVAILABLE
     */
    fun initCore(context: Context) {
        val mgr = AppUpdateManagerFactory.create(context)
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            val isAvailable = when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE,
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> true

                else -> false
            }

            updateAvailableFlow.value = isAvailable
            if (isAvailable && getFirstSeen() == 0L) {
                prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
            } else if (!isAvailable) {
                prefs.edit().remove(KEY_FIRST_SEEN).apply()
            }
        }
    }

    /**
     * Call from Activity.onResume() as well, to finish a flexible update
     * once the user has downloaded it.
     */
    fun resumeFlexibleUpdates() {
        val mgr = AppUpdateManagerFactory.create(MyKliqueApp.appContext)
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                mgr.completeUpdate()
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener { ex ->
                        if (ex is InstallException
                            && ex.errorCode == InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED
                        ) {
                            Logger.e("Flex", "Flexible update not allowed right now")
                        } else {
                            Logger.e("Flex", "Flexible update failed unexpectedly")
                        }
                    }
            }
        }
    }

    /** Used by combine() in your VM */
    fun isPastGracePeriod(): Boolean =
        (System.currentTimeMillis() - getFirstSeen()) >= ASK_AFTER

    fun getFirstSeen(): Long =
        prefs.getLong(KEY_FIRST_SEEN, 0L)
}