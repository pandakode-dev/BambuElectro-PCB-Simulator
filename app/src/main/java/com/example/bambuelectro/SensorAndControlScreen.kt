package com.example.bambuelectro

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp

@Composable
fun SensorAndControlScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val subTabs = listOf(
        Triple(0, "Sensor & PLC", Icons.Default.Sensors),
        Triple(1, "UART/I2C & CRC", Icons.Default.Cable),
        Triple(2, "PID & % Error", Icons.Default.AutoGraph)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Navigasi Sub-Tab Responsif (1 Baris Anti-Kepotong)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subTabs.forEach { (idx, title, icon) ->
                val isSelected = selectedSubTab == idx
                Surface(
                    color = if (isSelected) PcbDarkGreen else PureWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isSelected) BrightGold else SoftTraceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSubTab = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) BrightGold else PcbDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
            when (selectedSubTab) {
                0 -> WheatstoneAndPlcLoopView()
                1 -> ProtocolTimingAndModbusView()
                2 -> PidResponseAndLabErrorView()
            }
        }
    }
}

// ============================================================================
// SUB-TAB 1: JEMBATAN WHEATSTONE (NTC / PT100) & SKALA ARUS PLC 4-20mA
// ============================================================================
@Composable
fun WheatstoneAndPlcLoopView() {
    // 1. State Sensor Suhu PT100 / NTC & Jembatan Wheatstone
    var sensorType by remember { mutableStateOf("PT100 RTD") } // PT100 RTD vs NTC 10k
    var tempCelsius by remember { mutableFloatStateOf(25f) }    // -20°C s/d 200°C
    var vExcitation by remember { mutableStateOf("5.0") }       // Tegangan sumber jembatan
    var rBalanceOhm by remember { mutableStateOf("100.0") }     // R1 = R2 = R3
    var opAmpGain by remember { mutableStateOf("10.0") }        // Penguatan Op-Amp Instrumentasi

    val tC = tempCelsius.toDouble()
    // Rumus Standar Callendar-Van Dusen PT100: R(T) = R0 * (1 + 0.00385 * T)
    // Rumus Beta NTC 10k (B=3950, R25=10000): R(T) = 10000 * exp(3950 * (1/(T+273.15) - 1/298.15))
    val rSensorCalculated = remember(sensorType, tempCelsius) {
        if (sensorType == "PT100 RTD") {
            100.0 * (1.0 + 0.00385 * tC)
        } else {
            10000.0 * exp(3950.0 * ((1.0 / (tC + 273.15)) - (1.0 / 298.15)))
        }
    }

    val vExt = vExcitation.toDoubleOrNull() ?: 5.0
    val rRef = (rBalanceOhm.toDoubleOrNull() ?: 100.0).coerceAtLeast(1.0)
    val gain = (opAmpGain.toDoubleOrNull() ?: 10.0).coerceAtLeast(1.0)

    // Tegangan Jembatan Wheatstone: Vb = Vext * (Rx/(R3+Rx) - R2/(R1+R2)) dengan R1=R2=R3=rRef
    val vBridgeMv = vExt * ((rSensorCalculated / (rRef + rSensorCalculated)) - 0.5) * 1000.0
    val vAmpOutVolt = (vBridgeMv / 1000.0) * gain

    // 2. State Industrial 4-20 mA Current Loop Scaler
    var loopCurrentMa by remember { mutableFloatStateOf(12.0f) } // 4.0 - 20.0 mA
    var pvMin by remember { mutableStateOf("0.0") }              // Misal 0 Bar / 0°C
    var pvMax by remember { mutableStateOf("100.0") }            // Misal 100 Bar / 100°C
    var unitLabel by remember { mutableStateOf("Bar") }

    val pMin = pvMin.toDoubleOrNull() ?: 0.0
    val pMax = pvMax.toDoubleOrNull() ?: 100.0
    val spanPct = ((loopCurrentMa - 4.0f) / 16.0f).coerceIn(0f, 1f)
    val scaledProcessValue = pMin + spanPct * (pMax - pMin)
    val plcSiemensRaw = (spanPct * 27648).toInt() // Standar ADC PLC Siemens S7 (0 - 27648)
    val esp32AdcRaw = (spanPct * 4095).toInt()    // via Shunt 165 Ohm (0 - 3.3V)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: SIMULATOR SENSOR PT100 / NTC & JEMBATAN WHEATSTONE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🌡️ Simulator Sensor RTD PT100 / NTC & Jembatan Wheatstone",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PT100 RTD" to "100.0", "NTC 10kΩ (B=3950)" to "10000.0").forEach { (type, defR) ->
                            FilterChip(
                                selected = sensorType == type,
                                onClick = {
                                    sensorType = type
                                    rBalanceOhm = defR
                                },
                                label = { Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PcbDarkGreen,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "Suhu Lingkungan (T): %.1f °C ➔ Resistansi Sensor (Rx): %.2f Ω",
                            tempCelsius,
                            rSensorCalculated
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CopperGold
                    )

                    Slider(
                        value = tempCelsius,
                        onValueChange = { tempCelsius = it },
                        valueRange = -20f..180f,
                        colors = SliderDefaults.colors(thumbColor = CopperGold, activeTrackColor = PcbEmerald)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vExcitation,
                            onValueChange = { vExcitation = it },
                            label = { Text("V Eksitasi (V)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rBalanceOhm,
                            onValueChange = { rBalanceOhm = it },
                            label = { Text("R1=R2=R3 (Ω)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = opAmpGain,
                            onValueChange = { opAmpGain = it },
                            label = { Text("Gain Op-Amp") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Kotak Hasil Pengukuran Jembatan Wheatstone
                    Surface(
                        color = SiliconDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, BrightGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tegangan Diferensial Jembatan (ΔV):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%+.2f mV", vBridgeMv),
                                    color = BrightGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Output Penguat Instrumentasi (Vout):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%+.2f Volt", vAmpOutVolt),
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // KARTU 2: INDUSTRIAL 4-20 mA CURRENT LOOP & PLC ADC SCALER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🏭 Industrial 4–20 mA Current Loop & PLC Scaler",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )
                    Text(
                        text = "Konversi sinyal arus transmiter industri (4–20 mA) ke nilai fisik proses (PV) dan register ADC PLC / Mikrokontroler.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pvMin,
                            onValueChange = { pvMin = it },
                            label = { Text("Min PV (4mA)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pvMax,
                            onValueChange = { pvMax = it },
                            label = { Text("Max PV (20mA)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitLabel,
                            onValueChange = { unitLabel = it },
                            label = { Text("Satuan") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true
                        )
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "Arus Loop Saat Ini: %.2f mA (Span: %.1f%%)",
                            loopCurrentMa,
                            spanPct * 100f
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = PcbDarkGreen
                    )

                    Slider(
                        value = loopCurrentMa,
                        onValueChange = { loopCurrentMa = it },
                        valueRange = 3.5f..20.5f,
                        colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = CopperGold)
                    )

                    // Tombol Cepat Kalibrasi 4mA (0%), 8mA (25%), 12mA (50%), 16mA (75%), 20mA (100%)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(4f to "4mA (0%)", 8f to "8mA (25%)", 12f to "12mA (50%)", 16f to "16mA (75%)", 20f to "20mA (100%)")) { (ma, lbl) ->
                            SuggestionChip(
                                onClick = { loopCurrentMa = ma },
                                label = { Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Surface(
                        color = PcbDarkGreen,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (loopCurrentMa < 3.8f) {
                                Text(
                                    text = "🚨 ALARM: KABEL SENSOR PUTUS (LOOP OPEN CIRCUIT < 3.8mA)!",
                                    color = LogicHighRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nilai Proses Fisik (PV):", color = SoftTraceBorder, fontSize = 12.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f %s", scaledProcessValue, unitLabel),
                                    color = BrightGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tegangan Shunt 250Ω (1–5V):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f Volt", (loopCurrentMa / 1000f) * 250f),
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PLC Raw (0–27648) / ESP32 (12-Bit):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = "$plcSiemensRaw  |  $esp32AdcRaw",
                                    color = PureWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUB-TAB 2: VISUALISASI TIMING DIAGRAM (UART / I2C / PWM) & MODBUS CRC-16
// ============================================================================
@Composable
fun ProtocolTimingAndModbusView() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var charToTransmit by remember { mutableStateOf("A") }
    var baudRate by remember { mutableIntStateOf(9600) }
    var modbusHexInput by remember { mutableStateOf("01 04 00 00 00 02") } // Contoh Baca PZEM-004T / PLC

    val activeChar = charToTransmit.firstOrNull() ?: 'A'
    val asciiCode = activeChar.code
    // Bit Frame UART 8N1: Start Bit (0) + 8 Data Bits (LSB First) + Stop Bit (1)
    val uartBits = remember(asciiCode) {
        val list = mutableListOf<Int>()
        list.add(1) // Idle High
        list.add(0) // Start Bit (LOW)
        for (i in 0..7) {
            list.add((asciiCode shr i) and 1) // LSB First
        }
        list.add(1) // Stop Bit (HIGH)
        list.add(1) // Idle High
        list
    }
    val bitDurationUs = 1_000_000.0 / baudRate

    val crcResult = remember(modbusHexInput) {
        calculateModbusCrc16(modbusHexInput)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: LOGIC ANALYZER TIMING DIAGRAM GENERATOR (UART 8N1 & I2C)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⏱️ Digital Logic Analyzer — UART 8N1 Timing Diagram",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "Karakter: '%c' | ASCII: %d (0x%02X) | Lebar 1 Bit @%d bps = %.1f µs",
                            activeChar, asciiCode, asciiCode, baudRate, bitDurationUs
                        ),
                        color = BrightGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Kanvas Timing Diagram Gelombang Pulsa Kotak (Start, D0-D7, Stop)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(12.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val highY = h * 0.25f
                        val lowY = h * 0.72f
                        val stepW = w / uartBits.size

                        val pulsePath = Path()
                        uartBits.forEachIndexed { idx, bit ->
                            val xStart = idx * stepW
                            val xEnd = (idx + 1) * stepW
                            val y = if (bit == 1) highY else lowY

                            if (idx == 0) {
                                pulsePath.moveTo(xStart, y)
                            } else {
                                pulsePath.lineTo(xStart, y)
                            }
                            pulsePath.lineTo(xEnd, y)

                            // Garis bantu vertikal batas bit
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(xStart, 10f),
                                end = Offset(xStart, h - 10f),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Label nama bit (Idle, Start, D0..D7, Stop)
                            val bitName = when (idx) {
                                0 -> "IDLE"
                                1 -> "START"
                                in 2..9 -> "D${idx - 2}:$bit"
                                10 -> "STOP"
                                else -> "IDLE"
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                bitName,
                                xStart + (stepW / 2f),
                                h - 6f,
                                Paint().apply {
                                    color = when (idx) {
                                        1 -> android.graphics.Color.parseColor("#F87171")
                                        10 -> android.graphics.Color.parseColor("#4ADE80")
                                        else -> android.graphics.Color.parseColor("#FDE68A")
                                    }
                                    textSize = 17f
                                    isFakeBoldText = true
                                    textAlign = Paint.Align.CENTER
                                }
                            )
                        }

                        drawPath(
                            path = pulsePath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = charToTransmit,
                            onValueChange = { if (it.length <= 1) charToTransmit = it },
                            label = { Text("Ketik 1 Huruf/Angka ASCII", color = SoftTraceBorder) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            )
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1.2f)) {
                            items(listOf(9600, 19200, 115200)) { baud ->
                                FilterChip(
                                    selected = baudRate == baud,
                                    onClick = { baudRate = baud },
                                    label = { Text("$baud", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // KARTU 2: KALKULATOR CHECKSUM MODBUS RTU (CRC-16) UNTUK PLC & SENSOR PZEM
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🛡️ Kalkulator Checksum Modbus RTU (CRC-16)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )
                    Text(
                        text = "Hitung 2-Byte CRC-16 otomatis untuk perintah RS485 / Modbus RTU (Sensor Energi PZEM-004T, Inverter VFD, & PLC).",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "Baca PZEM-004T" to "01 04 00 00 00 0A",
                            "Read Holding Reg (03)" to "01 03 00 00 00 02",
                            "Write Single Coil (05)" to "01 05 00 00 FF 00"
                        )
                        items(presets) { (lbl, hex) ->
                            SuggestionChip(
                                onClick = { modbusHexInput = hex },
                                label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = modbusHexInput,
                        onValueChange = { modbusHexInput = it },
                        label = { Text("Payload Hex Modbus (Pisahkan dengan spasi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = SchematicIvory,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, CopperGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hasil Checksum CRC-16 (Low-Byte First): ${crcResult.first}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = CopperGold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Frame Lengkap Siap Kirim: ${crcResult.second}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PcbDarkGreen,
                                fontFamily = FontFamily.Monospace
                            )
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(crcResult.second))
                                    Toast.makeText(context, "Frame Modbus Hex disalin!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Frame Lengkap", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUB-TAB 3: SIMULATOR KURVA RESPON PID CONTROLLER & KALKULATOR % ERROR PRAKTIKUM
// ============================================================================
@Composable
fun PidResponseAndLabErrorView() {
    var kp by remember { mutableFloatStateOf(2.2f) }
    var ki by remember { mutableFloatStateOf(0.8f) }
    var kd by remember { mutableFloatStateOf(0.5f) }

    // Kalkulator % Error (Galat) Laporan Praktikum
    var theoryValStr by remember { mutableStateOf("8.00") }
    var measuredValStr by remember { mutableStateOf("7.84") }

    val vTheory = theoryValStr.toDoubleOrNull() ?: 8.0
    val vMeasured = measuredValStr.toDoubleOrNull() ?: 7.84
    val absError = abs(vTheory - vMeasured)
    val pctError = if (abs(vTheory) > 1e-9) (absError / abs(vTheory)) * 100.0 else 0.0
    val accuracyPct = (100.0 - pctError).coerceIn(0.0, 100.0)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: SIMULATOR GRAFIK STEP RESPONSE PID CONTROLLER (Kp, Ki, Kd)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🎛️ PID Controller Step Response Simulator",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = String.format(Locale.US, "Parameter Aktif: Kp = %.1f  |  Ki = %.2f  |  Kd = %.2f", kp, ki, kd),
                        color = BrightGold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Kanvas Simulasi Kurva Step Response Orde-2 dengan Kendali PID
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(12.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val setpointY = h * 0.35f
                        val bottomY = h - 20f

                        // Garis Setpoint (Target 100% - Merah Putus-putus)
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = Offset(0f, setpointY),
                            end = Offset(w, setpointY),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "SETPOINT TARGET (100%)",
                            12f,
                            setpointY - 8f,
                            Paint().apply {
                                color = android.graphics.Color.parseColor("#FCA5A5")
                                textSize = 18f
                                isFakeBoldText = true
                            }
                        )

                        // Simulasi Numerik Loop Tertutup Orde-2: Plant + PID
                        val path = Path()
                        var yPos = 0.0
                        var yVel = 0.0
                        var integral = 0.0
                        var prevErr = 1.0
                        val dt = 0.05
                        val steps = 160

                        for (s in 0..steps) {
                            val x = (s.toFloat() / steps) * w
                            val py = (bottomY - (yPos.toFloat() * (bottomY - setpointY))).coerceIn(8f, h - 8f)
                            if (s == 0) path.moveTo(x, py) else path.lineTo(x, py)

                            val err = 1.0 - yPos
                            integral = (integral + err * dt).coerceIn(-2.0, 2.0)
                            val deriv = (err - prevErr) / dt
                            val u = (kp * err) + (ki * integral) + (kd * deriv)
                            prevErr = err

                            val accel = u * 3.2 - (1.4 * yVel) - yPos
                            yVel += accel * dt
                            yPos += yVel * dt
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF34D399),
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )
                    }

                    // Slider Tuning Kp, Ki, Kd
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(String.format(Locale.US, "Proportional Gain (Kp = %.1f) — Kecepatan Naik (Rise Time)", kp), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(value = kp, onValueChange = { kp = it }, valueRange = 0.2f..8.0f, colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = PcbEmerald))

                        Text(String.format(Locale.US, "Integral Gain (Ki = %.2f) — Hilangkan Steady-State Error", ki), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(value = ki, onValueChange = { ki = it }, valueRange = 0.0f..3.0f, colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = CopperGold))

                        Text(String.format(Locale.US, "Derivative Gain (Kd = %.2f) — Redam Osilasi & Overshoot", kd), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(value = kd, onValueChange = { kd = it }, valueRange = 0.0f..3.0f, colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = Color(0xFF38BDF8)))
                    }
                }
            }
        }

        // KARTU 2: KALKULATOR PERSENTASE ERROR (GALAT) LAPORAN PRAKTIKUM LAB
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📊 Kalkulator % Error (Galat) Laporan Praktikum",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )
                    Text(
                        text = "Bandingkan nilai perhitungan rumus teori dengan hasil pengukuran Multimeter/Osiloskop di laboratorium.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = theoryValStr,
                            onValueChange = { theoryValStr = it },
                            label = { Text("Nilai Teori / Hitung") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = measuredValStr,
                            onValueChange = { measuredValStr = it },
                            label = { Text("Nilai Ukur Praktikum") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Surface(
                        color = PcbDarkGreen,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Selisih Mutlak (|Teori - Ukur|):", color = SoftTraceBorder, fontSize = 12.sp)
                                Text(String.format(Locale.US, "%.4f", absError), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Persentase Galat (% Error):", color = SoftTraceBorder, fontSize = 12.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f %%", pctError),
                                    color = if (pctError <= 5.0) BrightGold else LogicHighRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tingkat Akurasi Percobaan:", color = SoftTraceBorder, fontSize = 12.sp)
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.2f %% (%s)",
                                        accuracyPct,
                                        if (pctError <= 5.0) "Sangat Presisi ✅" else "Periksa Toleransi ⚠️"
                                    ),
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPER ALGORITMA STANDAR MODBUS RTU CRC-16 (POLYNOMIAL 0xA001)
// ============================================================================
fun calculateModbusCrc16(hexString: String): Pair<String, String> {
    val cleanTokens = hexString.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val bytes = mutableListOf<Int>()
    for (token in cleanTokens) {
        val parsed = token.removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: return Pair("INVALID HEX", "-")
        bytes.add(parsed and 0xFF)
    }
    if (bytes.isEmpty()) return Pair("-", "-")

    var crc = 0xFFFF
    for (b in bytes) {
        crc = crc xor b
        for (i in 0 until 8) {
            crc = if ((crc and 0x0001) != 0) {
                (crc ushr 1) xor 0xA001
            } else {
                crc ushr 1
            }
        }
    }
    val lowByte = crc and 0xFF
    val highByte = (crc ushr 8) and 0xFF
    val crcHex = String.format(Locale.US, "%02X %02X", lowByte, highByte)
    val fullFrame = cleanTokens.joinToString(" ") { it.uppercase() } + " " + crcHex
    return Pair(crcHex, fullFrame)
}