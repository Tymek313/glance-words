package com.example.glancewords.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.glancewords.R
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(device = Devices.PHONE)
fun TestScreen() {
    val swipeState = rememberSwipeableState(initialValue = 0)
    var sideBarWidth by remember { mutableStateOf(0f) }
    val cornerRadius by remember {
        derivedStateOf {
            if (swipeState.offset.value.isNaN() || sideBarWidth == 0f) {
                30f
            } else {
                30 + abs(swipeState.offset.value) / sideBarWidth * 40
            }

//            swipeState.progress.run {
//                val negateFraction = if(from == 1) {
//
//                } else {
//
//                }
//            }
//            if(from) {
//
//            }
//            swipeState.progress.run { to + (1 - from) * fraction } * 40
        }
    }

    /*
    * 0 + 0 * 0.0 * 20 = 0          0
    * 0 + 1 * 0.25 * 20 = 5         0 + 0.25 * 20
    * -1 + 1 * 1 * 20 = 20          1 * 1 * 20
    * (-1 + 0 + 0.25) * 20 = 15     1 - 0.25 * 20
    *
    * -1 + 0.25 * 20 = 15
    * -from + to + fraction * offset
    * */

    // from 0 to 0 fraction 1 | radius 0
    // from 0 to 1 fraction 0 - 1 | radius 5
    // from 1 to 1 fraction 1 | radius 20
    // from 1 to 0 fraction 0 - 1 | radius 15

    Log.d("TestScreen", "progress: ${swipeState.progress}")
    Log.d("TestScreen", "corder radius: $cornerRadius")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B))
    ) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .onGloballyPositioned { sideBarWidth = it.size.width.toFloat() }
                .padding(8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(androidx.glance.appwidget.R.drawable.glance_btn_checkbox_checked_mtrl),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
        println(cornerRadius)

        Column(
            Modifier
                .swipeable(
                    state = swipeState,
                    anchors = mapOf(-2f to 0, -sideBarWidth to 1),
                    orientation = Orientation.Horizontal,
                    resistance = SwipeableDefaults.resistanceConfig(setOf(0f, -sideBarWidth), factorAtMax = 0f)
                )
                .offset { IntOffset(x = swipeState.offset.value.toInt(), y = 0) }
                .fillMaxSize()
                .clip(RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius))
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
        ) {
            (1..100).forEach {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    text = it.toString()
                )
            }
        }
    }
}