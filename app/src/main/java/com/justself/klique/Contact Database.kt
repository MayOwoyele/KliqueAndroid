package com.justself.klique

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.justself.klique.Bookshelf.Contacts.data.Contact
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts ORDER BY isAppUser DESC, name ASC")
    suspend fun getSortedContacts(): List<ContactEntity>
}
@Database(entities = [ContactEntity::class], version = 1)
abstract class ContactsDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val phoneNumber: String,
    val name: String,
    val isAppUser: Boolean,
    val customerId: Int?,
    val thumbnailUrl: String?
)
// the = symbol is a shorthand for return type. this is a normal function extension,
// fun toContact(): Contact{}
fun ContactEntity.toContact() = Contact(
    name = this.name,
    phoneNumber = this.phoneNumber,
    isAppUser = this.isAppUser,
    customerId = this.customerId,
    thumbnailUrl = this.thumbnailUrl
)

fun Contact.toContactEntity() = ContactEntity(
    name = this.name,
    phoneNumber = this.phoneNumber,
    isAppUser = this.isAppUser,
    customerId = this.customerId,
    thumbnailUrl = this.thumbnailUrl
)