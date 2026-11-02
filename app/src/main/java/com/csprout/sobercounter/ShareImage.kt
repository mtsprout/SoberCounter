package com.csprout.sobercounter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

private const val W = 1080
private const val MIN_H = 450
private const val PAD = 64f
private const val CYAN = 0xFF00E5FF.toInt()
private const val MAGENTA = 0xFFFF2BD6.toInt()
private const val GREEN = 0xFF39FF14.toInt()
private const val AMBER = 0xFFFFB300.toInt()

/** One coloured piece of the share line, e.g. "3 months". */
data class SharePart(val value: Long, val unit: String, val color: Int) {
    val text get() = "$value $unit"
}

private fun plural(v: Long, one: String) = if (v == 1L) one else one + "s"

/**
 * The pieces of the share line: years, months and days with zero units skipped.
 * Hours only appear when there is not yet a full day, and an empty list means "just started".
 */
fun shareParts(e: Elapsed): List<SharePart> {
    val parts = listOf(
        SharePart(e.years, plural(e.years, "year"), CYAN),
        SharePart(e.months, plural(e.months, "month"), MAGENTA),
        SharePart(e.days, plural(e.days, "day"), GREEN),
    ).filter { it.value > 0 }
    if (parts.isNotEmpty()) return parts
    return if (e.hours > 0) listOf(SharePart(e.hours, plural(e.hours, "hour"), AMBER)) else emptyList()
}

fun shareSentence(e: Elapsed): String =
    shareParts(e).takeIf { it.isNotEmpty() }?.joinToString(", ") { it.text } ?: "Just getting started"

/**
 * Draws one compact banner: "1 year, 3 months, 21 days" in the app's neon colours.
 * The sobriety date itself is deliberately left out. A long line shrinks first, then wraps between units.
 */
fun renderShareImage(e: Elapsed): Bitmap {
    val parts = shareParts(e)
    val face = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    val maxWidth = W - 2 * PAD

    fun paintFor(size: Float, color: Int, glow: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = face
        if (glow) setShadowLayer(size * 0.28f, 0f, 0f, color)
    }

    // Tokens: each part plus its trailing comma, which is drawn dim so the colours carry the look.
    val tokens = if (parts.isEmpty()) listOf(SharePart(0, "Just getting started", GREEN)) else parts
    fun widthOf(size: Float): Float {
        val p = paintFor(size, Color.WHITE, glow = false)
        return tokens.indices.sumOf { i ->
            val t = if (parts.isEmpty()) tokens[i].unit else tokens[i].text
            (p.measureText(t) + if (i < tokens.size - 1) p.measureText(", ") else 0f).toDouble()
        }.toFloat()
    }

    var size = 96f
    while (size > 60f && widthOf(size) > maxWidth) size -= 2f

    // Greedy wrap between units, only when the shrunken line is still too wide.
    val lines = mutableListOf<MutableList<Int>>(mutableListOf())
    val measure = paintFor(size, Color.WHITE, glow = false)
    fun tokenText(i: Int) = if (parts.isEmpty()) tokens[i].unit else tokens[i].text
    var used = 0f
    for (i in tokens.indices) {
        val w = measure.measureText(tokenText(i)) + if (i < tokens.size - 1) measure.measureText(",") else 0f
        val gap = if (lines.last().isEmpty()) 0f else measure.measureText(" ")
        if (lines.last().isNotEmpty() && used + gap + w > maxWidth) {
            lines.add(mutableListOf()); used = 0f
        }
        used += (if (lines.last().isEmpty()) 0f else gap) + w
        lines.last().add(i)
    }

    val lineH = size * 1.3f
    val firstBaseline = 300f
    val h = maxOf(MIN_H, (firstBaseline + lineH * (lines.size - 1) + 135f).toInt())
    val bmp = Bitmap.createBitmap(W, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.BLACK)

    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = face
        letterSpacing = 0.18f
        textAlign = Paint.Align.CENTER
        setShadowLayer(14f, 0f, 0f, Color.argb(130, 255, 255, 255))
    }
    c.drawText("SOBRIETY TRACKER", W / 2f, 150f, title)

    val dim = paintFor(size, 0xFF777777.toInt(), glow = false)
    val space = measure.measureText(" ")
    lines.forEachIndexed { row, idxs ->
        var lineW = 0f
        idxs.forEachIndexed { k, i ->
            lineW += measure.measureText(tokenText(i)) + if (i < tokens.size - 1) measure.measureText(",") else 0f
            if (k > 0) lineW += space
        }
        var x = (W - lineW) / 2f
        val y = firstBaseline + row * lineH
        idxs.forEachIndexed { k, i ->
            if (k > 0) x += space
            val tp = paintFor(size, tokens[i].color)
            c.drawText(tokenText(i), x, y, tp)
            x += tp.measureText(tokenText(i))
            if (i < tokens.size - 1) {
                c.drawText(",", x, y, dim)
                x += dim.measureText(",")
            }
        }
    }
    return bmp
}

private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

/** Writes the image to the app cache and opens Android's share sheet. */
fun shareElapsed(context: Context, e: Elapsed) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "sobriety-tracker.png")
    file.outputStream().use { renderShareImage(e).compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share progress"))
}
