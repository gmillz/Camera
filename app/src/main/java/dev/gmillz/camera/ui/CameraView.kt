package dev.gmillz.camera.ui

import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlipCameraAndroid
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CameraView(
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing) {
        cameraViewModel.initializeCamera(lifecycleOwner, previewView.surfaceProvider)
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            IconButton(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center),
                onClick = {
                    cameraViewModel.captureImage()
                }
            ) {
                Icon(
                    imageVector = Icons.Sharp.Lens,
                    contentDescription = "Take picture",
                    tint = Color.White,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(1.dp)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            IconButton(
                modifier = Modifier
                    .padding(start = 30.dp)
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
    }
}