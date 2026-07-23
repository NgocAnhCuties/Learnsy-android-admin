package com.learnsy.admin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tương đương KpiCard trong dashboard.jsx
@Composable
fun KpiCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    sub: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.05f))))
            .border(1.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(color.copy(alpha = 0.08f))
                    .border(1.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA07090))
        }
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        if (sub != null) {
            Text(sub, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA07090))
        }
    }
}

// Tương đương ProgressBar trong dashboard.jsx — shimmer animation lặp vô hạn
@Composable
fun ShimmerProgressBar(pct: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.Black.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.73f), color)))
        )
    }
}
