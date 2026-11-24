package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val pricePkr: Int,
    val description: String = "",
    val isAvailable: Boolean = true,
    val sortOrder: Int = 100,
    val createdAt: Long = System.currentTimeMillis()
)
