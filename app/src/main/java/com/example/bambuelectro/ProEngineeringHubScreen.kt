package com.example.bambuelectro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProEngineeringHubScreen() {
    var activeModule by remember { mutableIntStateOf(0) }

    val modules = listOf(
        Triple(0, "📡 RF & Kalibrasi", Icons.Default.SettingsInputAntenna),
        Triple(1, "☀️ SMPS & Solar", Icons.Default.SolarPower),
        Triple(2, "🔌 Serial & Timer MCU", Icons.Default.Terminal),
        Triple(3, "🧲 EMF & Stepper CNC", Icons.Default.PrecisionManufacturing),
        Triple(4, "📊 Bode Plot & Kuis", Icons.Default.Quiz),
        Triple(5, "📝 Jobsheet PDF", Icons.Default.PictureAsPdf)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Baris Navigasi Geser (LazyRow) untuk 6 Modul Pro Engineering
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(modules) { (idx, title, icon) ->
                val isSelected = activeModule == idx
                Surface(
                    color = if (isSelected) PcbDarkGreen else PureWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isSelected) BrightGold else SoftTraceBorder),
                    modifier = Modifier.clickable { activeModule = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) BrightGold else PcbDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) PureWhite else SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeModule) {
                0 -> RfAndCalibrationScreen()
                1 -> PowerAndSolarScreen()
                2 -> EmbeddedAndSerialScreen()
                3 -> MotionAndSensorLabScreen()
                4 -> BodeAndQuizScreen()
                5 -> LabReportGeneratorScreen()
            }
        }
    }
}