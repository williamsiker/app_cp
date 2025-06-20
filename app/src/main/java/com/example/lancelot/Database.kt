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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Entity(tableName = "themes")
data class CodeThemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorsJson: String
)

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

@Dao
interface ThemeDAO {
    @Insert
    suspend fun insertTheme(theme: CodeThemeEntity): Long

    @Query("DELETE FROM themes WHERE id = :themeId")
    suspend fun deleteTheme(themeId: Long)

    @Query("SELECT * FROM themes")
    suspend fun getAllThemes(): List<CodeThemeEntity>

    @Query("SELECT * FROM themes WHERE id = :themeId")
    suspend fun getThemeById(themeId: Long): CodeThemeEntity?
}

@Database(
    entities = [Styles::class, CodeThemeEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun styleDAO(): StyleDAO
    abstract fun themeDAO(): ThemeDAO
}







