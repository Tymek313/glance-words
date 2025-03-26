package com.pt.glancewords.app.widget.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
fun WidgetText(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    style: TextStyle = bodyLargeTextStyle,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(text, modifier, style, maxLines)
}

val bodyLargeTextStyle
    @Composable
    get() = TextStyle(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.SansSerif
    )

val labelSmallTextStyle
    @Composable
    get() = TextStyle(
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif
    )

val labelSmallBoldTextStyle
    @Composable
    get() = TextStyle(
        fontSize = MaterialTheme.typography.labelSmall.fontSize,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
