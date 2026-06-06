package com.malla.mvp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

object BubbleShapes {
    val ModernOwn = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp, topEnd = 16.dp)
    val ModernOther = RoundedCornerShape(topStart = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp, topEnd = 16.dp)
    val ModernDefault = RoundedCornerShape(16.dp)
    val Rounded = RoundedCornerShape(24.dp)

    val Pixel = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
    }

    val ComicOwn = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                val d = density.density * 4f
                moveTo(0f, h * 0.5f)
                lineTo(0f, 16f * d)
                cubicTo(0f, 4f * d, 16f * d, 0f, 28f * d, 4f * d)
                lineTo(w - 12f * d, 0f)
                cubicTo(w - 4f * d, 0f, w, 8f * d, w, 20f * d)
                lineTo(w, h - 12f * d)
                cubicTo(w, h - 4f * d, w - 8f * d, h, w - 16f * d, h)
                lineTo(12f * d, h)
                cubicTo(4f * d, h, 0f, h - 4f * d, 0f, h - 8f * d)
                close()
            }
            return Outline.Generic(path)
        }
    }

    val ComicOther = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                val d = density.density * 4f
                moveTo(w, h * 0.5f)
                lineTo(w, 16f * d)
                cubicTo(w, 4f * d, w - 16f * d, 0f, w - 28f * d, 4f * d)
                lineTo(12f * d, 0f)
                cubicTo(4f * d, 0f, 0f, 8f * d, 0f, 20f * d)
                lineTo(0f, h - 12f * d)
                cubicTo(0f, h - 4f * d, 8f * d, h, 16f * d, h)
                lineTo(w - 12f * d, h)
                cubicTo(w - 4f * d, h, w, h - 4f * d, w, h - 8f * d)
                close()
            }
            return Outline.Generic(path)
        }
    }

    val ColaOwn = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                val d = density.density * 4f
                val tailW = 12f * d
                val tailH = 10f * d
                moveTo(0f, 0f)
                lineTo(w - 16f * d, 0f)
                cubicTo(w - 4f * d, 0f, w, 4f * d, w, 16f * d)
                lineTo(w, h - tailH - 8f * d)
                lineTo(w, h - tailH)
                lineTo(w - tailW, h)
                lineTo(w - tailW - 4f * d, h - tailH)
                cubicTo(w - 8f * d, h, 8f * d, h, 0f, h - 8f * d)
                lineTo(0f, 8f * d)
                cubicTo(0f, 4f * d, 4f * d, 0f, 8f * d, 0f)
                close()
            }
            return Outline.Generic(path)
        }
    }

    val ColaOther = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                val d = density.density * 4f
                val tailW = 12f * d
                val tailH = 10f * d
                moveTo(w, 0f)
                lineTo(16f * d, 0f)
                cubicTo(4f * d, 0f, 0f, 4f * d, 0f, 16f * d)
                lineTo(0f, h - tailH - 8f * d)
                lineTo(0f, h - tailH)
                lineTo(tailW, h)
                lineTo(tailW + 4f * d, h - tailH)
                cubicTo(8f * d, h, w - 8f * d, h, w, h - 8f * d)
                lineTo(w, 8f * d)
                cubicTo(w, 4f * d, w - 4f * d, 0f, w - 8f * d, 0f)
                close()
            }
            return Outline.Generic(path)
        }
    }
}
