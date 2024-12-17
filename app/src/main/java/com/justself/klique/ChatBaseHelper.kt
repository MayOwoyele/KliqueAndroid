package com.justself.klique

import android.content.Context
import androidx.room.*

// ChatList Database Management
@Entity(tableName = "chats")
data class ChatList(
    @PrimaryKey val enemyId: Int,
    val contactName: String,
    val lastMsg: String,
    val lastMsgAddTime: String,
    val profilePhoto: String,
    val myId: Int,
    val unreadMsgCounter: Int,
    val isVerified: Boolean = true
)

// Define DAO (Data Access Object) Interface
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChat(chat: ChatList)

    @Query("DELETE FROM chats WHERE enemyId = :enemyId")
    suspend fun deleteChat(enemyId: Int)

    @Query("SELECT * FROM chats WHERE myId = :myId")
    fun getAllChats(myId: Int): List<ChatList>

    @Query("UPDATE chats SET contactname = :contactName, profilePhoto = :profilePhoto, isVerified = :isVerified WHERE enemyId = :enemyId")
    suspend fun updateProfile(
        enemyId: Int,
        contactName: String,
        profilePhoto: String,
        isVerified: Boolean
    )

    @Query("UPDATE chats SET unreadMsgCounter = unreadMsgCounter + 1 WHERE enemyId = :enemyId")
    suspend fun incrementUnreadMsgCounter(enemyId: Int)

    @Query("UPDATE chats SET unreadMsgCounter = 0 WHERE enemyId = :enemyId")
    suspend fun resetUnreadMsgCounter(enemyId: Int)

    @Query("UPDATE chats SET unreadMsgCounter = unreadMsgCounter + :count WHERE enemyId = :enemyId")
    suspend fun incrementUnreadMsgCounterBy(enemyId: Int, count: Int)

    @Query("UPDATE chats SET lastMsg = :lastMsg, lastMsgAddTime = :timeStamp WHERE enemyId = :enemyId")
    suspend fun updateLastMessage(enemyId: Int, lastMsg: String, timeStamp: String)

    @Query("SELECT EXISTS(SELECT 1 FROM chats WHERE (myId = :myId AND enemyId = :enemyId) OR (myId = :enemyId AND enemyId = :myId))")
    suspend fun chatExists(myId: Int, enemyId: Int): Boolean

    @Query("SELECT * FROM chats WHERE (myId = :myId AND contactName LIKE :query) OR (enemyId = :myId AND contactName LIKE :query)")
    fun searchChats(myId: Int, query: String): List<ChatList>
}

// Define Database Class
@Database(entities = [ChatList::class], version = 1)  // Keeping version 1
abstract class ChatListDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
@Entity(tableName = "personalChats")
data class PersonalChat(
    @PrimaryKey val messageId: String,
    val enemyId: Int,
    val myId: Int,
    val content: String,
    val status: PersonalMessageStatus,
    val messageType: PersonalMessageType,
    val timeStamp: String,
    val mediaUri: String? = null,
    val gistId: String? = null,
    val topic: String? = null
)

@Dao
interface PersonalChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPersonalChat(personalChat: PersonalChat)

    @Query("DELETE FROM personalChats WHERE messageId = :messageId")
    suspend fun deletePersonalChat(messageId: String)

    @Query("""
    SELECT * FROM personalChats 
    WHERE ((myId = :myId AND enemyId = :enemyId) 
       OR (myId = :enemyId AND enemyId = :myId))
      AND timeStamp < (
        SELECT timeStamp FROM personalChats
        WHERE messageId = :lastMessageId
        LIMIT 1
      )
    ORDER BY timeStamp DESC 
    LIMIT :pageSize
""")
    suspend fun getPersonalChatsBefore(
        myId: Int,
        enemyId: Int,
        lastMessageId: String,
        pageSize: Int
    ): List<PersonalChat>

    @Query("DELETE FROM personalChats WHERE (myId = :myId AND enemyId = :enemyId) OR (myId = :enemyId AND enemyId = :myId)")
    suspend fun deletePersonalChatsForEnemy(myId: Int, enemyId: Int)

    @Query("SELECT * FROM personalChats WHERE messageId = :messageId LIMIT 1")
    suspend fun getPersonalChatById(messageId: String): PersonalChat?

    @Query("SELECT * from personalChats WHERE myId = :myId AND enemyId = :enemyId")
    suspend fun getPersonalChatsByEnemyId(myId: Int, enemyId: Int): List<PersonalChat>

    @Query("SELECT * FROM personalChats WHERE status = :status")
    suspend fun getMessagesByStatus(status: PersonalMessageStatus): List<PersonalChat>

    @Query("UPDATE personalChats SET status = :newStatus WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, newStatus: PersonalMessageStatus)
    @Query("SELECT * FROM personalChats WHERE (myId = :myId AND enemyId = :enemyId) OR (myId = :enemyId AND enemyId = :myId) ORDER BY timeStamp DESC LIMIT :pageSize")
    suspend fun getInitialPersonalChats(
        myId: Int,
        enemyId: Int,
        pageSize: Int
    ): List<PersonalChat>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(personalChat: PersonalChat)
}

@Database(entities = [PersonalChat::class], version = 1)
abstract class PersonalChatDatabase : RoomDatabase() {
    abstract fun personalChatDao(): PersonalChatDao
}

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

    private var PERSONALCHAT_DATABASE_INSTANCE: PersonalChatDatabase? = null
    fun getPersonalChatDatabase(context: Context): PersonalChatDatabase {
        if (PERSONALCHAT_DATABASE_INSTANCE == null) {
            synchronized(PersonalChatDatabase::class) {
                PERSONALCHAT_DATABASE_INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    PersonalChatDatabase::class.java,
                    "personal_chats.db"
                ).fallbackToDestructiveMigration()
                    .build()
            }
        }
        return PERSONALCHAT_DATABASE_INSTANCE!!
    }

    private var GIST_STATE_DATABASE: GistRoomCreatedBase? = null
    fun getGistRoomCreatedBase(context: Context): GistRoomCreatedBase {
        if (GIST_STATE_DATABASE == null) {
            synchronized(GistRoomCreatedBase::class) {
                GIST_STATE_DATABASE = Room.databaseBuilder(
                    context.applicationContext, GistRoomCreatedBase::class.java, "gist_state.db"
                ).fallbackToDestructiveMigration()
                    .build()
            }
        }
        return GIST_STATE_DATABASE!!
    }
}
enum class PersonalMessageType(val typeString: String){
    P_IMAGE("PImage"),
    P_TEXT("PText"),
    P_VIDEO("PVideo"),
    P_AUDIO("PAudio"),
    P_GIST_INVITE("PGistInvite")
}
enum class PersonalMessageStatus {
    PENDING,
    SENT,
    DELIVERED
}
