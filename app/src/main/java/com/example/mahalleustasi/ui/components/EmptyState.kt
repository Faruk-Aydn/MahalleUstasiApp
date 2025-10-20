package com.example.mahalleustasi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        if (!description.isNullOrBlank()) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
