package com.malla.mvp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StickerPickerDialog() {
    val showPicker by StickerState.showPicker.collectAsState()
    if (!showPicker) return

    val stickerUrls = listOf(
        "https://media.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy.gif",
        "https://media.giphy.com/media/l0HlNQ03J5JxX6lva/giphy.gif",
        "https://media.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif"
    )

    AlertDialog(
        onDismissRequest = { StickerState.closePicker() },
        title = { Text("Stickers") },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(300.dp)) {
                items(stickerUrls.size) { index ->
                    AsyncImage(
                        model = stickerUrls[index],
                        contentDescription = "Sticker",
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { StickerState.showFullScreen(stickerUrls[index]) },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { StickerState.closePicker() }) { Text("Cerrar") } }
    )
}
