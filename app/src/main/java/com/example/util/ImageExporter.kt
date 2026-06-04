package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.BacklogCard
import com.example.ui.Localization
import java.io.File
import java.io.FileOutputStream

object ImageExporter {

    // Generates a beautiful image summarizing a specific backlog card
    fun generateCardImage(
        context: Context,
        card: BacklogCard,
        columnName: String,
        lang: String
    ): File? {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw elegant gradient background
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#FEF7FF"), // Very soft lavender white
                Color.parseColor("#F3EDF7"), // Light dynamic M3 theme color
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw decorative subtle circles in background
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EADDFF")
            alpha = 90
            style = Paint.Style.FILL
        }
        canvas.drawCircle(700f, 100f, 250f, circlePaint)
        canvas.drawCircle(100f, 500f, 150f, circlePaint)

        // Draw top header banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4") // Royal M3 purple
            style = Paint.Style.FILL
        }
        val bannerRect = RectF(40f, 40f, width - 40f, 140f)
        canvas.drawRoundRect(bannerRect, 20f, 20f, bannerPaint)

        // Header Title text
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitle = Localization.get("app_title", lang).uppercase()
        canvas.drawText(headerTitle, 80f, 100f, headerTextPaint)

        // Card container
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 4f, Color.argb(40, 0, 0, 0))
        }
        val cardRect = RectF(40f, 160f, width - 40f, height - 70f)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        // Task name/title text
        val titleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(Localization.get("field_title", lang) + ":", 80f, 220f, titleLabelPaint)

        val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(card.title, 80f, 270f, titleTextPaint)

        // Description title
        canvas.drawText(Localization.get("field_description", lang) + ":", 80f, 330f, titleLabelPaint)

        // Description Body
        val descTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val desc = if (card.description.isBlank()) "-" else card.description
        // Simple paragraph wrapping
        var currentY = 370f
        val words = desc.split(" ")
        var sentence = ""
        for (word in words) {
            val testSentence = if (sentence.isEmpty()) word else "$sentence $word"
            val widthOfTest = descTextPaint.measureText(testSentence)
            if (widthOfTest > width - 180f) {
                canvas.drawText(sentence, 80f, currentY, descTextPaint)
                sentence = word
                currentY += 30f
                if (currentY > height - 160f) break // truncate if too long
            } else {
                sentence = testSentence
            }
        }
        if (sentence.isNotEmpty() && currentY <= height - 160f) {
            canvas.drawText(sentence, 80f, currentY, descTextPaint)
        }

        // Column / Section Info Pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EADDFF")
            style = Paint.Style.FILL
        }
        val columnPillRect = RectF(80f, height - 140f, 360f, height - 90f)
        canvas.drawRoundRect(columnPillRect, 25f, 25f, pillPaint)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#21005D")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(columnName, 100f, height - 110f, pillTextPaint)

        // Priority Badge pill
        val priorityColor = when (card.priority.lowercase()) {
            "high" -> Color.parseColor("#F9DEDC") // red-pink tint
            "low" -> Color.parseColor("#D0BCFF") // light violet tint
            else -> Color.parseColor("#E1E2EC") // default grey tint
        }
        val priorityTextColor = when (card.priority.lowercase()) {
            "high" -> Color.parseColor("#BA1A1A")
            "low" -> Color.parseColor("#6750A4")
            else -> Color.parseColor("#49454F")
        }

        val priorityPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = priorityColor
            style = Paint.Style.FILL
        }
        val priorityPillRect = RectF(380f, height - 140f, 540f, height - 90f)
        canvas.drawRoundRect(priorityPillRect, 25f, 25f, priorityPillPaint)

        val priorityTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = priorityTextColor
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pStr = "${Localization.get("home_priority", lang)}: ${card.priority}"
        canvas.drawText(pStr, 400f, height - 110f, priorityTextPaint)

        // Draw dynamic footer watermark
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            alpha = 150
        }
        canvas.drawText("WORKSPACE DIGEST • PNG SNAPSHOT OUTBOX• ID: ${card.id}", 80f, height - 42f, watermarkPaint)

        return saveBitmapToFile(context, bitmap, "backlog_card_${card.id}.png")
    }

    // Generates a overall high quality Sprint Report image with charts representation
    fun generateSprintReport(
        context: Context,
        cards: List<BacklogCard>,
        lang: String
    ): File? {
        val width = 900
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Beautiful deep gradient background representing modern project hub
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#FEF7FF"), // Lavender light top-left
                Color.parseColor("#EADDFF"), // Medium Lavender bottom-right
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Main white card body
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(15f, 0f, 5f, Color.argb(40, 0, 0, 0))
        }
        val bodyRect = RectF(40f, 40f, width - 40f, height - 40f)
        canvas.drawRoundRect(bodyRect, 32f, 32f, bodyPaint)

        // Draw header decoration block
        val decorationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, 180f), 32f, 32f, decorationPaint)
        // Overlap the lower round corners of the decoration block
        canvas.drawRect(RectF(40f, 100f, width - 40f, 180f), decorationPaint)

        // Header Title in Spanish/English/Portuguese
        val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tMain = Localization.get("btn_export_report", lang).uppercase()
        canvas.drawText(tMain, 80f, 110f, titleTextPaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EADDFF")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(Localization.get("home_subtitle", lang), 80f, 145f, subtitlePaint)

        // Summary metrics
        val totalCount = cards.size
        val highPriorityCount = cards.count { it.priority.lowercase() == "high" }
        val lowPriorityCount = cards.count { it.priority.lowercase() == "low" }
        val otherCount = totalCount - (highPriorityCount + lowPriorityCount)

        // Text paints
        val textLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw 3 statistics modules blocks
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F7F2FA")
            style = Paint.Style.FILL
        }

        // Module 1: Total
        val totalBox = RectF(80f, 210f, 310f, 340f)
        canvas.drawRoundRect(totalBox, 16f, 16f, boxPaint)
        canvas.drawText(Localization.get("metrics_total_count", lang).split(" ").take(2).joinToString(" "), 100f, 250f, Paint(textLabelPaint).apply { textSize=15f })
        canvas.drawText(totalCount.toString(), 100f, 310f, textValuePaint)

        // Module 2: High Priority
        val highBox = RectF(340f, 210f, 570f, 340f)
        canvas.drawRoundRect(highBox, 16f, 16f, boxPaint)
        canvas.drawText(Localization.get("metrics_high_priority_count", lang).split(" ").take(2).joinToString(" "), 360f, 250f, Paint(textLabelPaint).apply { textSize=15f })
        canvas.drawText(highPriorityCount.toString(), 360f, 310f, Paint(textValuePaint).apply { color = Color.parseColor("#BA1A1A") })

        // Module 3: Sprint Health Rate (Total minus High priority %)
        val healthBox = RectF(600f, 210f, 820f, 340f)
        canvas.drawRoundRect(healthBox, 16f, 16f, boxPaint)
        canvas.drawText(Localization.get("home_active_sprint_health", lang).split(" ").take(2).joinToString(" "), 620f, 250f, Paint(textLabelPaint).apply { textSize=15f })
        val healthPercent = if (totalCount > 0) ((totalCount - highPriorityCount) * 100) / totalCount else 100
        canvas.drawText("$healthPercent%", 620f, 310f, Paint(textValuePaint).apply { color = Color.parseColor("#4CAF50") }) // green-tint

        // Draw Visual Chart representation as user requested 'imajenes crealas y que se puedan exportar'
        val chartLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(Localization.get("metrics_distribution", lang), 80f, 390f, chartLabelPaint)

        // Let's draw a nice interactive horizontal stacked bar chart
        val barY = 420f
        val barHeight = 40f
        val maxBarWidth = 740f

        val highPercent = if (totalCount > 0) (highPriorityCount.toFloat() / totalCount) else 0f
        val lowPercent = if (totalCount > 0) (lowPriorityCount.toFloat() / totalCount) else 0f
        val nonePercent = if (totalCount > 0) (otherCount.toFloat() / totalCount) else 0f

        val highWidthVal = maxBarWidth * highPercent
        val lowWidthVal = maxBarWidth * lowPercent
        val noneWidthVal = maxBarWidth * nonePercent

        var currentStartX = 80f

        // Draw high priority segment (fuchsia/red)
        if (highWidthVal > 0) {
            val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F9DEDC") }
            canvas.drawRect(currentStartX, barY, currentStartX + highWidthVal, barY + barHeight, hPaint)
            currentStartX += highWidthVal
        }
        // Draw low priority segment (yellowish/orange)
        if (lowWidthVal > 0) {
            val lPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD8E4") }
            canvas.drawRect(currentStartX, barY, currentStartX + lowWidthVal, barY + barHeight, lPaint)
            currentStartX += lowWidthVal
        }
        // Draw none priority segment (pastel blue/lavender)
        if (noneWidthVal > 0) {
            val nPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EADDFF") }
            canvas.drawRect(currentStartX, barY, currentStartX + noneWidthVal, barY + barHeight, nPaint)
        }

        // Draw labels below the bar chart
        var legendY = 500f
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // Legend 1
        markerPaint.color = Color.parseColor("#F9DEDC")
        canvas.drawCircle(100f, legendY - 8f, 12f, markerPaint)
        canvas.drawText("High (" + (highPercent * 100).toInt() + "%)", 130f, legendY, Paint(textLabelPaint).apply { textSize=18f })

        // Legend 2
        markerPaint.color = Color.parseColor("#FFD8E4")
        canvas.drawCircle(350f, legendY - 8f, 12f, markerPaint)
        canvas.drawText("Low (" + (lowPercent * 100).toInt() + "%)", 380f, legendY, Paint(textLabelPaint).apply { textSize=18f })

        // Legend 3
        markerPaint.color = Color.parseColor("#EADDFF")
        canvas.drawCircle(600f, legendY - 8f, 12f, markerPaint)
        canvas.drawText("None (" + (nonePercent * 100).toInt() + "%)", 630f, legendY, Paint(textLabelPaint).apply { textSize=18f })

        // Draw a table with some cards listed
        canvas.drawText(Localization.get("tab_tasks", lang) + " (" + totalCount + ")", 80f, 570f, chartLabelPaint)

        val listTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#49454F")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val listTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1B20")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Table headers
        var itemY = 610f
        canvas.drawText(Localization.get("field_title", lang), 80f, itemY, listTitlePaint)
        canvas.drawText("Priority", 500f, itemY, listTitlePaint)
        canvas.drawText("Sprint Column ID", 700f, itemY, listTitlePaint)

        canvas.drawLine(80f, itemY + 8f, width - 80f, itemY + 8f, Paint().apply { color = Color.parseColor("#CAC4D0"); strokeWidth = 2f })

        itemY += 40f
        val cardsToDraw = cards.take(3)
        for (item in cardsToDraw) {
            canvas.drawText(if (item.title.length > 32) item.title.take(30) + "..." else item.title, 80f, itemY, listTextPaint)
            canvas.drawText(item.priority, 500f, itemY, listTextPaint)
            val colName = when (item.columnId) {
                0 -> "Core Features"
                1 -> "Collaboration"
                2 -> "User Management"
                else -> "Analytics"
            }
            canvas.drawText(colName, 700f, itemY, listTextPaint)
            itemY += 32f
        }

        // Draw lovely watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4")
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            alpha = 180
        }
        canvas.drawText("WORKSPACE DIGEST • GENERATED SPRINT REPORT PNG SNAPSHOT • OK", 80f, height - 60f, wmPaint)

        return saveBitmapToFile(context, bitmap, "backlog_sprint_report.png")
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareImage(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Backlog Image"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot share: " + e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }
}
