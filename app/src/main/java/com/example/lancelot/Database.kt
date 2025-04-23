package com.example.lancelot

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "languages")
data class Languages(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null
)

@Entity(tableName = "styles")
data class Styles(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
    val sizeFont: Int,
    @ColumnInfo(defaultValue = "0") val isBold: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isItalic: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isUnderline: Boolean = false
)

@Entity(
    tableName = "keyword_groups",
    foreignKeys = [
        ForeignKey(
            entity = Languages::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Styles::class,
            parentColumns = ["id"],
            childColumns = ["styleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["languageId"]),
        Index(value = ["styleId"])
    ]
)
data class KeywordGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val languageId: Long,
    val styleId: Long?
)

@Entity(
    tableName = "keywords",
    foreignKeys = [
        ForeignKey(
            entity = Languages::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KeywordGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Styles::class,
            parentColumns = ["id"],
            childColumns = ["styleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["languageId"]),
        Index(value = ["groupId"]),
        Index(value = ["styleId"])
    ]
)
data class Keywords(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val languageId: Long,
    val groupId: Long?,
    val styleId: Long?
)

@Dao
interface LanguageDAO {
    @Insert
    suspend fun insertLanguages(language: Languages): Long

    @Query("DELETE FROM languages WHERE id = :languageId")
    suspend fun deleteLanguage(languageId: Long)

    @Query("SELECT * FROM languages")
    suspend fun getAllLanguages(): List<Languages>
    
    @Query("SELECT * FROM languages WHERE id = :languageId")
    suspend fun getLanguageById(languageId: Long): Languages?
}

@Dao
interface KeywordGroupDAO {
    @Insert
    suspend fun insertGroup(group: KeywordGroup): Long

    @Query("DELETE FROM keyword_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query("SELECT * FROM keyword_groups WHERE languageId = :languageId")
    suspend fun getAllGroupsForLanguage(languageId: Long): List<KeywordGroup>

    @Query("SELECT * FROM keyword_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): KeywordGroup?

    @Query("""
        UPDATE keyword_groups 
        SET styleId = :styleId 
        WHERE id = :groupId
    """)
    suspend fun updateGroupStyle(groupId: Long, styleId: Long?)
}

@Dao
interface KeywordDAO {
    @Insert
    suspend fun insertKeyword(keyword: Keywords): Long

    @Query("DELETE FROM keywords WHERE id = :keywordId")
    suspend fun deleteKeyword(keywordId: Long)

    @Query("SELECT * FROM keywords WHERE languageId = :languageId")
    suspend fun getAllKeywordsForLanguage(languageId: Long): List<Keywords>

    @Query("SELECT * FROM keywords WHERE groupId = :groupId")
    suspend fun getKeywordsByGroup(groupId: Long): List<Keywords>
    
    @Query("""
        SELECT k.*, s.* 
        FROM keywords k 
        LEFT JOIN styles s ON k.styleId = s.id 
        WHERE k.languageId = :languageId
    """)
    suspend fun getKeywordsWithStylesForLanguage(languageId: Long): Map<Keywords, Styles?>

    @Query("""
        UPDATE keywords 
        SET styleId = :styleId 
        WHERE groupId = :groupId
    """)
    suspend fun updateGroupKeywordsStyle(groupId: Long, styleId: Long?)
}

@Dao
interface StyleDAO {
    @Insert
    suspend fun insertStyle(style: Styles): Long

    @Query("DELETE FROM styles WHERE id = :styleId")
    suspend fun deleteStyle(styleId: Long)

    @Query("SELECT * FROM styles")
    suspend fun getAllStyles(): List<Styles>
    
    @Query("SELECT * FROM styles WHERE id = :styleId")
    suspend fun getStyleById(styleId: Long): Styles?
}

@Database(
    entities = [Languages::class, Keywords::class, Styles::class, KeywordGroup::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun languageDAO(): LanguageDAO
    abstract fun keywordDAO(): KeywordDAO
    abstract fun styleDAO(): StyleDAO
    abstract fun keywordGroupDAO(): KeywordGroupDAO
}







