package dev.gmillz.camera.ui

import android.util.Log
import android.view.MotionEvent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowDropDown
import androidx.compose.material.icons.sharp.FlashAuto
import androidx.compose.material.icons.sharp.FlashOff
import androidx.compose.material.icons.sharp.FlashOn
import androidx.compose.material.icons.sharp.FlipCameraAndroid
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material.icons.sharp.StopCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import dev.gmillz.camera.CameraMode
import dev.gmillz.camera.R
import dev.gmillz.camera.ui.components.FocusRing
import dev.gmillz.camera.ui.components.TabRow
import dev.gmillz.camera.ui.components.TabTitle

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CameraView(
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    val settingsOpen by remember { cameraViewModel.settingsOpen }
    val maximizeQuality by remember { cameraViewModel.maximizeQuality }
    val flashMode by remember { cameraViewModel.flashMode }
    val cameraMode by remember { cameraViewModel.currentMode }
    val isRecording by remember { cameraViewModel.isRecording }
    val elapsedTime by remember { cameraViewModel.elapsedTime }
    val aspectRatio by remember { cameraViewModel.aspectRatio }

    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }

    val settingButtonAlpha: Float by animateFloatAsState(
        targetValue = if (!settingsOpen) 1f else 0f,
        label = "settingsButtonAlpha"
    )

    val settingsBoxAlpha: Float by animateFloatAsState(
        targetValue = if (settingsOpen) 1f else 0f,
        label = "settingsBoxAlpha"
    )

    LaunchedEffect(lensFacing) {
        cameraViewModel.initializeCamera(lifecycleOwner, previewView.surfaceProvider)
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter {
                Log.d("TEST", "pointerInteropFilter")
                if (settingsOpen) {
                    Log.d("TEST", "here")
                    return@pointerInteropFilter false
                }
                if (it.action == MotionEvent.ACTION_UP) {
                    y = it.y
                    x = it.x
                    cameraViewModel.startFocusAndMetering(
                        it.x,
                        it.y,
                        previewView.width.toFloat(),
                        previewView.height.toFloat()
                    )
                    return@pointerInteropFilter true
                } else if (it.action == MotionEvent.ACTION_DOWN) {
                    return@pointerInteropFilter true
                }
                false
            }
            .clickable {
                if (settingsOpen) {
                    cameraViewModel.toggleSettingsOpen()
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        FocusRing(x = x, y = y)

        Box(
            modifier = Modifier
                .graphicsLayer(alpha = settingButtonAlpha)
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = {
                    cameraViewModel.toggleSettingsOpen()
                },
                modifier = Modifier
                    .width(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Sharp.ArrowDropDown,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center),
                onClick = {
                    if (cameraMode == CameraMode.VIDEO) {
                        cameraViewModel.toggleRecord()
                    } else {
                        cameraViewModel.captureImage()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isRecording) {
                        Icons.Sharp.StopCircle
                    } else {
                        Icons.Sharp.Lens
                    },
                    contentDescription = "Take picture",
                    tint = if (cameraMode == CameraMode.VIDEO) {
                        Color.Red
                    } else {
                        Color.White
                    },
                    modifier = Modifier
                        .size(100.dp)
                        .padding(1.dp)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            if (!isRecording) {
                IconButton(
                    modifier = Modifier
                        .padding(start = 30.dp, bottom = 5.dp)
                        .size(60.dp)
                        .align(Alignment.CenterStart),
                    onClick = {
                        cameraViewModel.toggleCamera()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Sharp.FlipCameraAndroid,
                        tint = Color.White,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                    )
                }
            }

            IconButton(
                modifier = Modifier
                    .padding(end = 30.dp, bottom = 5.dp)
                    .size(60.dp)
                    .align(Alignment.CenterEnd),
                onClick = {
                    if (!isRecording) {
                        cameraViewModel.captureImage()
                    }
                }
            ) {
                if (isRecording) {
                    Icon(
                        imageVector = Icons.Sharp.Lens,
                        contentDescription = "Take picture",
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(1.dp)
                            .border(2.dp, Color.White, CircleShape)
                    )
                } else {
                    AsyncImage(
                        model = cameraViewModel.lastCapturedItemState.value?.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(60.dp)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            var selectedTabPosition by remember { mutableIntStateOf(0) }
            if (elapsedTime.isNotEmpty()) {
                Text(
                    text = elapsedTime,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .padding(bottom = 5.dp)
                        .background(color = Color.Red, shape = CircleShape)
                )
            } else {
                TabRow(
                    selectedTabPosition = selectedTabPosition,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp),
                    containerShape = RectangleShape,
                ) {
                    cameraViewModel.getAvailableCameraModes().forEachIndexed { index, cameraMode ->
                        TabTitle(
                            title = stringResource(id = cameraMode.title),
                            position = index,
                            onClick = {
                                selectedTabPosition = index
                                cameraViewModel.setCameraMode(cameraMode)
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .graphicsLayer(alpha = settingsBoxAlpha)
                .clip(RoundedCornerShape(20.dp))
                .align(Alignment.Center)
                .size(width = 375.dp, height = 250.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .clickable { /* TODO */ }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*IconButton(
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Sharp.LocationOn,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null
                    )
                }*/

                IconButton(
                    onClick = {
                        cameraViewModel.toggleAspectRatio()
                     },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = if (aspectRatio == AspectRatio.RATIO_16_9) {
                            stringResource(id = R.string.aspect_ratio_16_9)
                        } else {
                            stringResource(id = R.string.aspect_ratio_4_3)
                        }
                    )
                }

                /*IconButton(
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Sharp.FlashlightOn,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null
                    )
                }*/

                IconButton(
                    onClick = { cameraViewModel.toggleFlashMode() }
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Sharp.FlashAuto
                            ImageCapture.FLASH_MODE_ON -> Icons.Sharp.FlashOn
                            else -> Icons.Sharp.FlashOff
                        },
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null
                    )
                }

                /*IconButton(
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Sharp.GridOn,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null
                    )
                }*/
            }

            Divider(modifier = Modifier.height(1.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 10.dp, top = 10.dp)
            ) {
                Text(
                    text = "Optimize for",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )

                RadioButton(
                    selected = maximizeQuality,
                    onClick = {
                        cameraViewModel.toggleMaximizeQuality(true)
                    }
                )

                Text(
                    text = "Quality",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )

                RadioButton(
                    selected = !maximizeQuality,
                    onClick = {
                        cameraViewModel.toggleMaximizeQuality(false)
                    }
                )

                Text(
                    text = "Latency",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )
            }

            /*Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 10.dp, top = 10.dp)
            ) {
                Text(
                    text = "Focus Timeout",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )

                val items = listOf(
                    "Off",
                    "3s",
                    "5s",
                    "8s",
                    "10s"
                )

                var text by remember { mutableStateOf("") }
                var isOpen by remember {
                    mutableStateOf(false)
                }

                DropdownList(
                    expandedO = isOpen,
                    list = items,
                    onDismissRequest = {
                        isOpen = false
                    },
                    onSelect = {
                        Log.d("TEST", "value - $it")
                    }
                )
            }*/
        }
    }
}