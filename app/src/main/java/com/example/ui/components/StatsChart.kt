package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DriveFile

data class MonthlyData(
  val monthName: String,
  val sizeBytes: Long
)

@Composable
fun UsageStatsCard(
  files: List<DriveFile>,
  modifier: Modifier = Modifier
) {
  val totalBytes = files.sumOf { it.size }
  val imageBytes = files.filter { it.isImage }.sumOf { it.size }
  val videoBytes = files.filter { it.isVideo }.sumOf { it.size }
  val docBytes = files.filter { it.isPdf || it.extension in listOf("doc", "docx", "txt", "pdf") }.sumOf { it.size }
  val archiveBytes = files.filter { it.isArchive }.sumOf { it.size }
  val otherBytes = (totalBytes - (imageBytes + videoBytes + docBytes + archiveBytes)).coerceAtLeast(0L)

  val monthsData = listOf(
    MonthlyData("May", 140_000_000L),
    MonthlyData("Jun", 320_000_000L),
    MonthlyData("Jul", 650_000_000L),
    MonthlyData("Aug", 480_000_000L),
    MonthlyData("Sep", 890_000_000L),
    MonthlyData("Oct", totalBytes.coerceAtLeast(250_000_000L))
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("usage_stats_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Upload Volume (Last 6 Months)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Monthly Bar Chart
      val maxMonthBytes = (monthsData.maxOfOrNull { it.sizeBytes } ?: 1L).coerceAtLeast(1L)
      val barColor = MaterialTheme.colorScheme.primary

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        monthsData.forEach { data ->
          val heightFraction = (data.sizeBytes.toFloat() / maxMonthBytes.toFloat()).coerceIn(0.1f, 1f)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
          ) {
            Canvas(
              modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(heightFraction)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            ) {
              drawRect(color = barColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = data.monthName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Storage by File Type",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Multi-segment progress bar
      val safeTotal = totalBytes.coerceAtLeast(1L).toFloat()
      val imgFrac = (imageBytes / safeTotal)
      val vidFrac = (videoBytes / safeTotal)
      val docFrac = (docBytes / safeTotal)
      val arcFrac = (archiveBytes / safeTotal)

      val imgColor = Color(0xFF10B981) // Green
      val vidColor = Color(0xFF3B82F6) // Blue
      val docColor = Color(0xFFF59E0B) // Amber
      val arcColor = Color(0xFF8B5CF6) // Purple
      val othColor = Color(0xFF9CA3AF) // Gray

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp))
      ) {
        val w = size.width
        var startX = 0f

        val imgW = w * imgFrac
        drawRect(imgColor, Offset(startX, 0f), Size(imgW, size.height))
        startX += imgW

        val vidW = w * vidFrac
        drawRect(vidColor, Offset(startX, 0f), Size(vidW, size.height))
        startX += vidW

        val docW = w * docFrac
        drawRect(docColor, Offset(startX, 0f), Size(docW, size.height))
        startX += docW

        val arcW = w * arcFrac
        drawRect(arcColor, Offset(startX, 0f), Size(arcW, size.height))
        startX += arcW

        val remW = (w - startX).coerceAtLeast(0f)
        if (remW > 0) {
          drawRect(othColor, Offset(startX, 0f), Size(remW, size.height))
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Legend
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        LegendItem(imgColor, "Images", formatFileSize(imageBytes))
        LegendItem(vidColor, "Videos", formatFileSize(videoBytes))
        LegendItem(docColor, "Docs", formatFileSize(docBytes))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        LegendItem(arcColor, "Archives", formatFileSize(archiveBytes))
        LegendItem(othColor, "Other", formatFileSize(otherBytes))
        LegendItem(MaterialTheme.colorScheme.primary, "Total", formatFileSize(totalBytes))
      }
    }
  }
}

@Composable
private fun LegendItem(color: Color, label: String, sizeStr: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
      Text(text = sizeStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
  }
}
