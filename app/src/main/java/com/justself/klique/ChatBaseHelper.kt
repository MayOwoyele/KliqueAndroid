package com.justself.klique

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// This is for the ChatScreen composable
class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "chats.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_CHATS = "chats"

        private const val COLUMN_ID = "id"
        private const val COLUMN_CONTACT_NAME = "contact_name"
        private const val COLUMN_CUSTOMER_ID = "customer_id"
        private const val COLUMN_LAST_MSG = "last_msg"
        private const val COLUMN_LAST_MSG_ADDTIME = "last_msg_addtime"
        private const val COLUMN_PROFILE_PHOTO = "profile_photo"
        private const val COLUMN_RECIPIENT_ID = "recipient_id"
        private const val COLUMN_UNREAD_MSG_COUNTER = "unread_msg_counter"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_CHATS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CONTACT_NAME TEXT, " +
                "$COLUMN_CUSTOMER_ID TEXT, " +
                "$COLUMN_LAST_MSG TEXT, " +
                "$COLUMN_LAST_MSG_ADDTIME TEXT, " +
                "$COLUMN_PROFILE_PHOTO TEXT, " +
                "$COLUMN_RECIPIENT_ID TEXT, " +
                "$COLUMN_UNREAD_MSG_COUNTER INTEGER)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHATS")
        onCreate(db)
    }

    fun addChat(chat: ChatList) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CONTACT_NAME, chat.contactName)
            put(COLUMN_CUSTOMER_ID, chat.customerId)
            put(COLUMN_LAST_MSG, chat.lastMsg)
            put(COLUMN_LAST_MSG_ADDTIME, chat.lastMsgAddtime)
            put(COLUMN_PROFILE_PHOTO, chat.profilePhoto)
            put(COLUMN_RECIPIENT_ID, chat.recipientId)
            put(COLUMN_UNREAD_MSG_COUNTER, chat.unreadMsgCounter)
        }

        db.insert(TABLE_CHATS, null, values)
        db.close()
    }

    fun updateChat(chat: ChatList) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LAST_MSG, chat.lastMsg)
            put(COLUMN_LAST_MSG_ADDTIME, chat.lastMsgAddtime)
            put(COLUMN_UNREAD_MSG_COUNTER, chat.unreadMsgCounter)
        }

        db.update(TABLE_CHATS, values, "$COLUMN_RECIPIENT_ID = ?", arrayOf(chat.recipientId))
        db.close()
    }

    fun deleteChat(recipientId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_CHATS, "$COLUMN_RECIPIENT_ID = ?", arrayOf(recipientId))
        db.close()
    }

    fun getAllChats(): List<ChatList> {
        val chatList = mutableListOf<ChatList>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_CHATS", null)

        if (cursor.moveToFirst()) {
            do {
                val chat = ChatList(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUSTOMER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MSG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MSG_ADDTIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_PHOTO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECIPIENT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UNREAD_MSG_COUNTER))
                )
                chatList.add(chat)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return chatList
    }
}