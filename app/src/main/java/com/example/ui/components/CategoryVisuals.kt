package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.TakeoutDining
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryVisual(
    val icon: ImageVector,
    val accentColor: Color,
    val tintBackground: Color
)

fun categoryVisualFor(category: String): CategoryVisual {
    return when (category.trim().lowercase()) {
        "pizza" -> CategoryVisual(
            icon = Icons.Rounded.LocalPizza,
            accentColor = Color(0xFFD34418),
            tintBackground = Color(0xFFFFECE6)
        )
        "shawarma" -> CategoryVisual(
            icon = Icons.Rounded.TakeoutDining,
            accentColor = Color(0xFFB36B00),
            tintBackground = Color(0xFFFFF2DC)
        )
        "finger chips" -> CategoryVisual(
            icon = Icons.Rounded.Fastfood,
            accentColor = Color(0xFFD98200),
            tintBackground = Color(0xFFFFF5DB)
        )
        "zinger shawarma" -> CategoryVisual(
            icon = Icons.Rounded.LocalDining,
            accentColor = Color(0xFF9C3A23),
            tintBackground = Color(0xFFFDE9E4)
        )
        "zinger burger" -> CategoryVisual(
            icon = Icons.Rounded.LunchDining,
            accentColor = Color(0xFF1B8A5A),
            tintBackground = Color(0xFFE3F7EC)
        )
        "all" -> CategoryVisual(
            icon = Icons.Rounded.RestaurantMenu,
            accentColor = Color(0xFFD34418),
            tintBackground = Color(0xFFFFECE6)
        )
        else -> CategoryVisual(
            icon = Icons.Rounded.Restaurant,
            accentColor = Color(0xFF6B4F43),
            tintBackground = Color(0xFFF3EAE4)
        )
    }
}
