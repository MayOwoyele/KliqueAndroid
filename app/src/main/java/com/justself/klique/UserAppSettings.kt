package com.justself.klique

import android.content.Context
import android.util.Log

object UserAppSettings {
    private const val PREF_NAME = "klique_prefs"
    private const val KEY_SUPPRESS_ADMIN_TIP = "suppress_admin_tip"
    private const val KEY_SHOW_INVITE_MODAL = "show_invite_modal"
    private const val EXCEEDED_CONTACT_LIST = "exceeded_contact_list"
    private const val SHOULD_RESTORE_DIARY = "should_restore_diary"
    private const val KEY_LAST_INVITE_TIME = "last_invite_time"

    fun suppressAdminTip(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SUPPRESS_ADMIN_TIP, false)
    }

    fun setSuppressAdminTip(context: Context, suppress: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SUPPRESS_ADMIN_TIP, suppress)
            .apply()
    }
    fun fetchShowInviteModal(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_INVITE_MODAL, true)
    }
    fun setShowInviteModal(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_INVITE_MODAL, show)
            .apply()
    }
    fun fetchExceededContactList(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(EXCEEDED_CONTACT_LIST, false)
    }
    fun setExceededContactList(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(EXCEEDED_CONTACT_LIST, show)
            .apply()
    }
    fun shouldRestoreDiary(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(SHOULD_RESTORE_DIARY, true)
    }
    fun setShouldRestoreDiary(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOULD_RESTORE_DIARY, show)
            .apply()
    }
    fun fetchLastInviteTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_INVITE_TIME, 0L)
    }

    fun setLastInviteTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_INVITE_TIME, time).apply()
    }
}
inline fun loggerD(tag: String, lazyMessage: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, lazyMessage())
    }
}