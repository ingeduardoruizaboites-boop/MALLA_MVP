package com.malla.mvp.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

@Composable
fun QrScanScreen(
    onQrScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    BackHandler { onBack() }
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se necesita permiso de cámara")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Text("Cancelar")
        }
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        private var camera: Camera? = null
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                camera = Camera.open()
                                camera?.setPreviewDisplay(holder)
                                camera?.parameters?.let {
                                    it.previewFormat = ImageFormat.NV21
                                    camera?.parameters = it
                                }
                                camera?.setPreviewCallback { data, _ ->
                                    decodeQr(data, onQrScanned)
                                }
                                camera?.startPreview()
                            } catch (e: Exception) {
                                Log.e("QrScan", "Error al abrir cámara", e)
                            }
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            camera?.stopPreview()
                            camera?.release()
                            camera = null
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun decodeQr(data: ByteArray, callback: (String) -> Unit) {
    try {
        val source = PlanarYUVLuminanceSource(data, 640, 480, 0, 0, 640, 480, false)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeReader()
        val result = reader.decode(binaryBitmap)
        if (result != null) {
            callback(result.text)
        }
    } catch (e: NotFoundException) {
        // QR no encontrado
    } catch (e: Exception) {
        Log.e("QrScan", "Error decodificando QR", e)
    }
}
