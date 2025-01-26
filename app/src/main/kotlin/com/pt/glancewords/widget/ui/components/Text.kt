package com.pt.glancewords.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.text.TextStyle

@Composable
fun WidgetText(text: String, modifier: GlanceModifier = GlanceModifier, style: TextStyle = defaultTextStyle, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, style, maxLines)
}

val defaultTextStyle
    @Composable
    get() = TextDefaults.defaultTextStyle.copy(fontSize = TextUnit(18f, TextUnitType.Sp), color = GlanceTheme.colors.onBackground)

val smallTextStyle
    @Composable
    get() = defaultTextStyle.copy(fontSize = TextUnit(13f, TextUnitType.Sp))

val smallBoldTextStyle
    @Composable
    get() = smallTextStyle.copy(fontWeight = FontWeight.Bold)
