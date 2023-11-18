package dev.gmillz.camera.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FocusRing(
    x: Float,
    y: Float
) {

    var visible by remember { mutableStateOf(false) }

    if (x != 0f) {
        LaunchedEffect(x) {
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Layout(
            content = {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        ) { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(x.toInt() - placeable.width / 2, y.toInt() - placeable.height / 2)
            }
        }
    }

    LaunchedEffect(y) {
        Log.d("TEST", "here")
        delay(500)
        visible = false
    }
}