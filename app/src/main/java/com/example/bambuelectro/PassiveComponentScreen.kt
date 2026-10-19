package com.example.bambuelectro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PassiveComponentScreen() {
    var isFiveBandMode by remember { mutableStateOf(false) }

    // Default: Coklat (1) - Hitam (0) - Merah (x100) - Emas (±5%) -> 1 kΩ
    var band1 by remember { mutableStateOf(StandardResistorBands[1]) }
    var band2 by remember { mutableStateOf(StandardResistorBands[0]) }
    var band3 by remember { mutableStateOf(StandardResistorBands[0]) }
    var multiplierBand by remember { mutableStateOf(StandardResistorBands[2]) }
    var toleranceBand by remember { mutableStateOf(StandardResistorBands[10]) }

    // State SMD & Kapasitor Decoder
    var smdCodeInput by remember { mutableStateOf("472") }
    var capCodeInput by remember { mutableStateOf("104") }
    var seriesParR1 by remember { mutableStateOf("1000") }
    var seriesParR2 by remember { mutableStateOf("2200") }

    val calculatedOhms = remember(isFiveBandMode, band1, band2, band3, multiplierBand) {
        val d1 = band1.digit ?: 1
        val d2 = band2.digit ?: 0
        val d3 = band3.digit ?: 0
        val baseVal = if (isFiveBandMode) (d1 * 100 + d2 * 10 + d3) else (d1 * 10 + d2)
        baseVal * multiplierBand.multiplier
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. KARTU VISUALISASI FISIK RESISTOR & NILAI OHM REAL-TIME
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎨 Visual Resistor Color Code",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${ElectroMath.formatResistance(calculatedOhms)} ${toleranceBand.tolerance ?: "±5%"}",
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        }

                        // Switch 4-Band vs 5-Band Presisi
                        FilterChip(
                            selected = isFiveBandMode,
                            onClick = { isFiveBandMode = !isFiveBandMode },
                            label = {
                                Text(
                                    text = if (isFiveBandMode) "Mode 5-Gelang" else "Mode 4-Gelang",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CopperGold,
                                containerColor = PcbEmerald
                            )
                        )
                    }

                    // Kanvas Gambar Komponen Fisik Resistor + Gelang Warna
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                            .background(Color(0xFF032E22), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp)
                    ) {
                        val cy = size.height / 2f
                        val bodyW = size.width * 0.62f
                        val bodyH = 44.dp.toPx()
                        val bodyLeft = (size.width - bodyW) / 2f
                        val bodyTop = cy - (bodyH / 2f)

                        // Kawat Kaki Tembaga/Timah Kiri & Kanan
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(10f, cy),
                            end = Offset(size.width - 10f, cy),
                            strokeWidth = 10f
                        )

                        // Bodi Keramik Resistor (Krem/Biru Metal Film)
                        drawRoundRect(
                            color = if (isFiveBandMode) Color(0xFFbae6fd) else Color(0xFFFDE68A),
                            topLeft = Offset(bodyLeft, bodyTop),
                            size = Size(bodyW, bodyH),
                            cornerRadius = CornerRadius(22f, 22f)
                        )

                        // Posisi Gelang Warna
                        val activeBands = if (isFiveBandMode) {
                            listOf(band1, band2, band3, multiplierBand, toleranceBand)
                        } else {
                            listOf(band1, band2, multiplierBand, toleranceBand)
                        }

                        val step = bodyW / (activeBands.size + 1)
                        activeBands.forEachIndexed { idx, b ->
                            val bx = bodyLeft + step * (idx + 1) - 10f
                            drawRect(
                                color = b.colorHex,
                                topLeft = Offset(bx, bodyTop),
                                size = Size(20f, bodyH)
                            )
                        }
                    }

                    // Tombol Cepat Nilai Resistor Populer Lab (Quick Presets)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            Triple("220 Ω (LED)", StandardResistorBands[2], StandardResistorBands[2]) to StandardResistorBands[1],
                            Triple("330 Ω", StandardResistorBands[3], StandardResistorBands[3]) to StandardResistorBands[1],
                            Triple("1 kΩ", StandardResistorBands[1], StandardResistorBands[0]) to StandardResistorBands[2],
                            Triple("4.7 kΩ (Pull-Up)", StandardResistorBands[4], StandardResistorBands[7]) to StandardResistorBands[2],
                            Triple("10 kΩ", StandardResistorBands[1], StandardResistorBands[0]) to StandardResistorBands[3],
                            Triple("100 kΩ", StandardResistorBands[1], StandardResistorBands[0]) to StandardResistorBands[4]
                        )
                        items(presets) { (info, mult) ->
                            val (label, b1, b2) = info
                            Surface(
                                color = PcbEmerald,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BrightGold),
                                modifier = Modifier.clickable {
                                    isFiveBandMode = false
                                    band1 = b1
                                    band2 = b2
                                    multiplierBand = mult
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. PEMILIH WARNA PER GELANG (INTERAKTIF & MUDAH DIGESER)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BandSelectorRow(
                        label = "Gelang 1 (Angka Pertama):",
                        options = StandardResistorBands.filter { it.digit != null && it.digit > 0 },
                        selected = band1,
                        onSelect = { band1 = it }
                    )
                    BandSelectorRow(
                        label = "Gelang 2 (Angka Kedua):",
                        options = StandardResistorBands.filter { it.digit != null },
                        selected = band2,
                        onSelect = { band2 = it }
                    )
                    if (isFiveBandMode) {
                        BandSelectorRow(
                            label = "Gelang 3 (Angka Ketiga - Presisi):",
                            options = StandardResistorBands.filter { it.digit != null },
                            selected = band3,
                            onSelect = { band3 = it }
                        )
                    }
                    BandSelectorRow(
                        label = "Gelang Pengali (Multiplier):",
                        options = StandardResistorBands,
                        selected = multiplierBand,
                        onSelect = { multiplierBand = it }
                    )
                    BandSelectorRow(
                        label = "Gelang Toleransi (%):",
                        options = StandardResistorBands.filter { it.tolerance != null },
                        selected = toleranceBand,
                        onSelect = { toleranceBand = it }
                    )
                }
            }
        }

        // 3. DEKODER RESISTOR SMD (3-DIGIT, 4-DIGIT, R-DECIMAL, EIA-96) & KAPASITOR KERAMIK
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🔬 SMD Resistor (EIA-96) & Ceramic Capacitor Decoder",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = smdCodeInput,
                            onValueChange = { smdCodeInput = it },
                            label = { Text("Kode SMD (103, 4R7, 01C)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = capCodeInput,
                            onValueChange = { capCodeInput = it },
                            label = { Text("Kapasitor (104, 223)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Surface(color = SchematicIvory, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SoftTraceBorder)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "• Hasil SMD [${smdCodeInput.uppercase()}]: ${ElectroMath.decodeSmdResistor(smdCodeInput)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PcbDarkGreen
                            )
                            Text(
                                text = "• Kapasitor [$capCodeInput]: ${ElectroMath.decodeCeramicCapacitor(capCodeInput)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CopperGold
                            )
                        }
                    }
                }
            }
        }

        // 4. KALKULATOR RANGKAIAN SERI & PARALEL RESISTOR CEPAT
        item {
            val r1 = seriesParR1.toDoubleOrNull() ?: 1000.0
            val r2 = seriesParR2.toDoubleOrNull() ?: 1000.0
            val rSeries = r1 + r2
            val rParallel = if (r1 + r2 > 0) (r1 * r2) / (r1 + r2) else 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔗 Kalkulator Pengganti Resistor Seri & Paralel", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = seriesParR1,
                            onValueChange = { seriesParR1 = it },
                            label = { Text("Resistor R1 (Ω)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = seriesParR2,
                            onValueChange = { seriesParR2 = it },
                            label = { Text("Resistor R2 (Ω)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("R Seri (R1 + R2): ${ElectroMath.formatResistance(rSeries)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SiliconDark)
                        Text("R Paralel (R1 || R2): ${ElectroMath.formatResistance(rParallel)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PcbEmerald)
                    }
                }
            }
        }
    }
}

@Composable
fun BandSelectorRow(
    label: String,
    options: List<ResistorColorBand>,
    selected: ResistorColorBand,
    onSelect: (ResistorColorBand) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SiliconDark)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { item ->
                val isSelected = item.name == selected.name
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) PcbDarkGreen else SchematicIvory,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrightGold else SoftTraceBorder),
                    modifier = Modifier.clickable { onSelect(item) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(item.colorHex)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PureWhite else SiliconDark
                        )
                    }
                }
            }
        }
    }
}