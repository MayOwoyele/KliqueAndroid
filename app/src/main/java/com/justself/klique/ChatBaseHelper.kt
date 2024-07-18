package com.justself.klique

import android.content.Context
import androidx.room.*

// Define Entity Class
@Entity(tableName = "chats")
data class ChatList(
    @PrimaryKey val enemyId: Int,  // Primary key and integer
    val contactName: String,
    val lastMsg: String,
    val lastMsgAddtime: String,
    val profilePhoto: String,
    val myId: Int,  // Renamed from recipientId to myId
    val unreadMsgCounter: Int
)

// Define DAO (Data Access Object) Interface
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChat(chat: ChatList)

    @Update
    suspend fun updateChat(chat: ChatList)

    @Query("DELETE FROM chats WHERE enemyId = :enemyId")
    suspend fun deleteChat(enemyId: Int)

    // Update getAllChats to accept myId as a parameter
    @Query("SELECT * FROM chats WHERE myId = :myId")
    fun getAllChats(myId: Int): List<ChatList>
}

// Define Database Class
@Database(entities = [ChatList::class], version = 1)  // Keeping version 1
abstract class ChatListDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

// Initialize Database
object DatabaseProvider {
    private var CHATLIST_DATABASE_INSTANCE: ChatListDatabase? = null

    fun getChatListDatabase(context: Context): ChatListDatabase {
        if (CHATLIST_DATABASE_INSTANCE == null) {
            synchronized(ChatListDatabase::class) {
                CHATLIST_DATABASE_INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    ChatListDatabase::class.java,
                    "chats.db"
                )
                    .fallbackToDestructiveMigration()  // Recreate the database
                    .build()
            }
        }
        return CHATLIST_DATABASE_INSTANCE!!
    }
    private var CONTACTS_INSTANCE: ContactsDatabase? = null

    fun getContactsDatabase(context: Context): ContactsDatabase {
        if (CONTACTS_INSTANCE == null) {
            synchronized(ContactsDatabase::class) {
                CONTACTS_INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    ContactsDatabase::class.java,
                    "contacts_database"
                )
                    .fallbackToDestructiveMigration()  // Optional: Recreate the database if schema changes
                    .build()
            }
        }
        return CONTACTS_INSTANCE!!
    }
}
