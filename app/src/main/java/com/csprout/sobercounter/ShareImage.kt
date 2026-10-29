package com.csprout.sobercounter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

private const val W = 1080
private const val H = 1350

/** Draws the four counters in the app's neon style. The sobriety date itself is deliberately left out. */
fun renderShareImage(e: Elapsed): Bitmap {
    val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.BLACK)

    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 54f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        letterSpacing = 0.18f
        textAlign = Paint.Align.CENTER
        setShadowLayer(18f, 0f, 0f, Color.argb(130, 255, 255, 255))
    }
    c.drawText("SOBRIETY TRACKER", W / 2f, 150f, title)

    val rows = listOf(
        Triple("YEARS", e.years to e.yearFrac, 0xFF00E5FF.toInt()),
        Triple("MONTHS", e.months to e.monthFrac, 0xFFFF2BD6.toInt()),
        Triple("DAYS", e.days to e.dayFrac, 0xFF39FF14.toInt()),
        Triple("HOURS", e.hours to e.hourFrac, 0xFFFFB300.toInt()),
    )
    val left = 70f
    val right = W - 70f
    val cardH = 240f
    val gap = 36f
    var top = 230f
    for ((label, value, color) in rows) {
        val card = RectF(left, top, right, top + cardH)
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; this.color = withAlpha(color, 12)
        })
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; this.color = withAlpha(color, 90)
        })

        val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 128f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setShadowLayer(34f, 0f, 0f, color)
        }
        c.drawText(value.first.toString(), left + 52f, top + 148f, number)

        val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = withAlpha(color, 230)
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.3f
            textAlign = Paint.Align.RIGHT
        }
        c.drawText(label, right - 52f, top + 138f, tag)

        // Progress rail with a soft glow.
        val railL = left + 52f
        val railR = right - 52f
        val railT = top + cardH - 56f
        val rail = RectF(railL, railT, railR, railT + 18f)
        c.drawRoundRect(rail, 9f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, 36) })
        val fillR = railL + (railR - railL) * value.second.coerceIn(0f, 1f)
        if (fillR > railL) {
            val fill = RectF(railL, railT, maxOf(fillR, railL + 18f), railT + 18f)
            c.drawRoundRect(fill, 9f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(railL, 0f, fill.right, 0f, withAlpha(color, 140), color, Shader.TileMode.CLAMP)
                setShadowLayer(16f, 0f, 0f, color)
            })
        }
        top += cardH + gap
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
