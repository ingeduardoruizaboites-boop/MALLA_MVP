package com.malla.mvp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.malla.mvp.util.QrCodeGenerator

@Composable
fun QrCodeDisplay(content: String, size: Int = 200) {
    val bitmap: Bitmap = remember(content) {
        QrCodeGenerator.generateQrCode(content, size, size)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR efímero",
        modifier = Modifier.size(size.dp)
    )
}
