package com.example.bambuelectro

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.*

@Composable
fun RfAndCalibrationScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // 1. State RF Impedance Matching & Smith Chart Lite
    var freqMhzStr by remember { mutableStateOf("433.0") } // LoRa 433MHz, 868MHz, Wi-Fi 2400MHz
    var zSourceOhm by remember { mutableStateOf("50.0") }
    var rLoadOhm by remember { mutableStateOf("18.0") }
    var xLoadOhm by remember { mutableStateOf("-12.0") }

    val fMhz = (freqMhzStr.toDoubleOrNull() ?: 433.0).coerceAtLeast(1.0)
    val z0 = (zSourceOhm.toDoubleOrNull() ?: 50.0).coerceAtLeast(1.0)
    val rl = (rLoadOhm.toDoubleOrNull() ?: 18.0).coerceAtLeast(0.5)
    val xl = xLoadOhm.toDoubleOrNull() ?: 0.0

    // Perhitungan Koefisien Refleksi (Gamma), VSWR & Return Loss
    val denom = (rl + z0).pow(2) + xl.pow(2)
    val gammaReal = ((rl.pow(2) + xl.pow(2) - z0.pow(2)) / denom).coerceIn(-0.98, 0.98)
    val gammaImag = ((2.0 * z0 * xl) / denom).coerceIn(-0.98, 0.98)
    val gammaMag = hypot(gammaReal, gammaImag).coerceIn(0.0, 0.98)
    val vswr = (1.0 + gammaMag) / (1.0 - gammaMag)
    val returnLossDb = if (gammaMag > 0.0001) -20.0 * log10(gammaMag) else 45.0

    // Perhitungan L-Network Impedance Matching (Komponen L dalam nH & C dalam pF)
    val qFactor = sqrt((max(z0, rl) / min(z0, rl)) - 1.0)
    val omega = 2.0 * PI * fMhz * 1e6
    val xsReactance = if (z0 > rl) qFactor * rl else z0 / qFactor
    val xpReactance = if (z0 > rl) z0 / qFactor else rl / qFactor
    val matchInductorNh = (xsReactance / omega) * 1e9
    val matchCapPf = (1.0 / (omega * xpReactance)) * 1e12

    // 2. State Kalibrasi Sensor 2-Titik (Linearization y = mx + c)
    var sensorName by remember { mutableStateOf("Sensor Tekanan / pH") }
    var adcPoint1 by remember { mutableStateOf("410") }
    var realVal1 by remember { mutableStateOf("0.0") }
    var adcPoint2 by remember { mutableStateOf("3680") }
    var realVal2 by remember { mutableStateOf("10.0") }
    var testAdcInput by remember { mutableStateOf("2048") }

    val x1 = adcPoint1.toDoubleOrNull() ?: 410.0
    val y1 = realVal1.toDoubleOrNull() ?: 0.0
    val x2 = adcPoint2.toDoubleOrNull() ?: 3680.0
    val y2 = realVal2.toDoubleOrNull() ?: 10.0
    val xTest = testAdcInput.toDoubleOrNull() ?: 2048.0

    val slopeM = if (abs(x2 - x1) > 1e-6) (y2 - y1) / (x2 - x1) else 0.0
    val interceptC = y1 - (slopeM * x1)
    val calibratedOutput = (slopeM * xTest) + interceptC

    val arduinoCalibrationCode = remember(sensorName, slopeM, interceptC) {
        String.format(
            Locale.US,
            "// Kalibrasi Linier 2-Titik: %s\nconst float SLOPE_M = %.6ff;\nconst float OFFSET_C = %.4ff;\n\nfloat readCalibratedSensor(int rawAdc) {\n  return (SLOPE_M * rawAdc) + OFFSET_C;\n}",
            sensorName, slopeM, interceptC
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: RF ANTENNA IMPEDANCE MATCHING & SMITH CHART LITE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📡 RF Antenna Matching & Smith Chart Lite",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Hitung VSWR, Return Loss, dan jaringan penyesuai impedansi LC (L-Network) menuju 50 Ω.",
                        color = SoftTraceBorder,
                        fontSize = 11.sp
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val rfPresets = listOf(
                            "LoRa 433MHz" to "433.0",
                            "LoRa 915MHz" to "915.0",
                            "Wi-Fi/BLE 2.4GHz" to "2450.0",
                            "FPV 5.8GHz" to "5800.0"
                        )
                        items(rfPresets) { (lbl, fVal) ->
                            SuggestionChip(
                                onClick = { freqMhzStr = fVal },
                                label = { Text(lbl, fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = freqMhzStr,
                            onValueChange = { freqMhzStr = it },
                            label = { Text("Freq (MHz)", color = SoftTraceBorder, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                        OutlinedTextField(
                            value = rLoadOhm,
                            onValueChange = { rLoadOhm = it },
                            label = { Text("R Antena (Ω)", color = SoftTraceBorder, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                        OutlinedTextField(
                            value = xLoadOhm,
                            onValueChange = { xLoadOhm = it },
                            label = { Text("jX Reaktansi (Ω)", color = SoftTraceBorder, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                    }

                    // Kanvas Visualisasi Smith Chart Lite
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(14.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = min(size.width, size.height) * 0.42f

                        // Lingkaran Luar Smith Chart (|Gamma| = 1.0)
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.5f),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3f)
                        )

                        // Sumbu Resistansi Murni (Garis Horizontal Tengah)
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(cx - radius, cy),
                            end = Offset(cx + radius, cy),
                            strokeWidth = 2f
                        )

                        // Lingkaran Resistansi Konstan (r = 0.5, r = 1.0, r = 2.0)
                        listOf(0.25f, 0.5f, 0.7f).forEach { rScale ->
                            val subR = radius * rScale
                            drawCircle(
                                color = Color.Cyan.copy(alpha = 0.18f),
                                radius = subR,
                                center = Offset(cx + (radius - subR), cy),
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Titik Pusat 50 Ohm Match (Hijau)
                        drawCircle(color = Color(0xFF4ADE80), radius = 7f, center = Offset(cx, cy))

                        // Titik Impedansi Beban Antena Saat Ini (Gamma Plot)
                        val ptX = cx + (gammaReal.toFloat() * radius)
                        val ptY = cy - (gammaImag.toFloat() * radius)
                        drawLine(
                            color = BrightGold,
                            start = Offset(cx, cy),
                            end = Offset(ptX, ptY),
                            strokeWidth = 3f
                        )
                        drawCircle(color = LogicHighRed, radius = 9f, center = Offset(ptX, ptY))

                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "ZL = %.1f %+.1fj Ω", rl, xl),
                            ptX + 12f,
                            ptY - 10f,
                            Paint().apply {
                                color = android.graphics.Color.YELLOW
                                textSize = 20f
                                isFakeBoldText = true
                            }
                        )
                    }

                    // Kotak Hasil Perhitungan VSWR, Return Loss & L-Network
                    Surface(
                        color = Color(0xFF052E22),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SoftTraceBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VSWR & Return Loss:", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "1 : %.2f  |  %.1f dB", vswr, returnLossDb),
                                    color = if (vswr <= 2.0) Color(0xFF4ADE80) else BrightGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("L-Network Induktor Seri (L):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f nH (Q = %.2f)", matchInductorNh, qFactor),
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("L-Network Kapasitor Paralel (C):", color = SoftTraceBorder, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f pF", matchCapPf),
                                    color = BrightGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // KARTU 2: KALIBRASI SENSOR 2-TITIK (LINEAR REGRESSION y = mx + c)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🧲 Kalibrasi Linier Sensor 2-Titik (y = mx + c)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )
                    Text(
                        text = "Masukkan 2 titik pembacaan Raw ADC dan nilai alat ukur standar untuk menghasilkan persamaan kalibrasi otomatis.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = sensorName,
                        onValueChange = { sensorName = it },
                        label = { Text("Nama Sensor yang Dikalibrasi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = adcPoint1,
                            onValueChange = { adcPoint1 = it },
                            label = { Text("Raw ADC 1 (x1)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = realVal1,
                            onValueChange = { realVal1 = it },
                            label = { Text("Nilai Asli 1 (y1)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = adcPoint2,
                            onValueChange = { adcPoint2 = it },
                            label = { Text("Raw ADC 2 (x2)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = realVal2,
                            onValueChange = { realVal2 = it },
                            label = { Text("Nilai Asli 2 (y2)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = testAdcInput,
                        onValueChange = { testAdcInput = it },
                        label = { Text("Uji Nilai Raw ADC Saat Ini") },
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
                                text = String.format(Locale.US, "Persamaan: y = %.6fx %+.4f", slopeM, interceptC),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = PcbDarkGreen,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "Hasil Uji Kalibrasi (ADC %.0f) = %.3f", xTest, calibratedOutput),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = CopperGold
                            )
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(arduinoCalibrationCode))
                                    Toast.makeText(context, "Kode C++ Kalibrasi Sensor disalin!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Fungsi C++ Arduino", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}