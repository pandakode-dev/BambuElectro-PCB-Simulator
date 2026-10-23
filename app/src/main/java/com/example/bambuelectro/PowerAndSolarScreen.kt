package com.example.bambuelectro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil

@Composable
fun PowerAndSolarScreen() {
    // 1. State SMPS Buck / Boost Converter Designer
    var isBuckMode by remember { mutableStateOf(true) }
    var vInSmps by remember { mutableStateOf("12.0") }
    var vOutSmps by remember { mutableStateOf("5.0") }
    var iLoadAmp by remember { mutableStateOf("2.0") }
    var freqKhzSmps by remember { mutableStateOf("150.0") } // Misal LM2596 150kHz

    val vin = (vInSmps.toDoubleOrNull() ?: 12.0).coerceAtLeast(1.0)
    val vout = (vOutSmps.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.5)
    val iout = (iLoadAmp.toDoubleOrNull() ?: 2.0).coerceAtLeast(0.05)
    val fHz = ((freqKhzSmps.toDoubleOrNull() ?: 150.0).coerceAtLeast(10.0)) * 1000.0

    val dutyCyclePct = if (isBuckMode) {
        ((vout / vin.coerceAtLeast(vout)) * 100.0).coerceIn(5.0, 95.0)
    } else {
        ((1.0 - (vin / vout.coerceAtLeast(vin + 0.5))) * 100.0).coerceIn(5.0, 95.0)
    }

    val rippleCurrent = 0.3 * iout // Asumsi 30% ripple arus induktor standar desain SMPS
    val inductorUh = if (isBuckMode) {
        ((vin - vout).coerceAtLeast(0.5) * (dutyCyclePct / 100.0)) / (fHz * rippleCurrent) * 1e6
    } else {
        (vin * (dutyCyclePct / 100.0)) / (fHz * rippleCurrent) * 1e6
    }
    val outputCapUf = (rippleCurrent / (8.0 * fHz * 0.05)) * 1e6 // Asumsi Vripple 50mV

    // 2. State Kalkulator Heatsink Thermal (Regulator Linear 7805 / LM317 / MOSFET)
    var pDissWatt by remember { mutableStateOf("3.5") }
    var tempAmbientC by remember { mutableStateOf("30.0") }
    var thetaJc by remember { mutableStateOf("5.0") }  // Junction-to-Case (°C/W)
    var thetaHs by remember { mutableStateOf("12.0") } // Heatsink-to-Ambient (°C/W)

    val pd = pDissWatt.toDoubleOrNull() ?: 3.5
    val ta = tempAmbientC.toDoubleOrNull() ?: 30.0
    val rJc = thetaJc.toDoubleOrNull() ?: 5.0
    val rHs = thetaHs.toDoubleOrNull() ?: 12.0
    val tJunction = ta + pd * (rJc + rHs)

    // 3. State Perancang Baterai Pack (xSyP) & Panel Surya Off-Grid IoT
    var dailyLoadWattHour by remember { mutableStateOf("120.0") } // Wh per hari
    var sunHoursPerDay by remember { mutableStateOf("4.5") }      // PSH Indonesia ~4.5 jam
    var seriesCountS by remember { mutableIntStateOf(3) }         // 3S = 11.1V (12.6V Full)
    var parallelCountP by remember { mutableIntStateOf(2) }       // 2P

    val whDay = dailyLoadWattHour.toDoubleOrNull() ?: 120.0
    val psh = (sunHoursPerDay.toDoubleOrNull() ?: 4.5).coerceAtLeast(1.0)
    val recommendedSolarWp = ceil((whDay * 1.3) / psh) // Faktor rugi 1.3x
    val packNominalVolt = seriesCountS * 3.7
    val packFullVolt = seriesCountS * 4.2
    val packCapAh = parallelCountP * 3.0 // Asumsi 1 sel 18650 = 3.0 Ah (3000mAh)
    val packEnergyWh = packNominalVolt * packCapAh

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: PERANCANG SMPS DC-DC BUCK & BOOST CONVERTER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚡ SMPS DC-DC Buck / Boost Inductance Designer",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isBuckMode,
                            onClick = { isBuckMode = true; vInSmps = "12.0"; vOutSmps = "5.0" },
                            label = { Text("Step-Down (Buck)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = !isBuckMode,
                            onClick = { isBuckMode = false; vInSmps = "5.0"; vOutSmps = "12.0" },
                            label = { Text("Step-Up (Boost)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = vInSmps, onValueChange = { vInSmps = it }, label = { Text("Vin (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = vOutSmps, onValueChange = { vOutSmps = it }, label = { Text("Vout (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = iLoadAmp, onValueChange = { iLoadAmp = it }, label = { Text("I Beban (A)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = freqKhzSmps, onValueChange = { freqKhzSmps = it }, label = { Text("Freq (kHz)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Surface(color = PcbDarkGreen, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(String.format(Locale.US, "• PWM Duty Cycle: %.1f %%", dutyCyclePct), color = SoftTraceBorder, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "• Induktor Toroid Minimum (L): %.1f µH", inductorUh), color = BrightGold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "• Kapasitor Filter Low-ESR (Cout): ≥ %.0f µF", outputCapUf), color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // KARTU 2: KALKULATOR SUHU HEATSINK THERMAL (7805 / LM317 / MOSFET)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "♨️ Kalkulator Suhu Sambungan IC & Heatsink Thermal",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pDissWatt, onValueChange = { pDissWatt = it }, label = { Text("Daya Panas (W)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = tempAmbientC, onValueChange = { tempAmbientC = it }, label = { Text("Suhu Ruang (°C)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = thetaJc, onValueChange = { thetaJc = it }, label = { Text("Rθ JC (°C/W)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = thetaHs, onValueChange = { thetaHs = it }, label = { Text("Rθ Heatsink (°C/W)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    Surface(color = SchematicIvory, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CopperGold)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = String.format(Locale.US, "Estimasi Suhu Inti Silicon (Tj): %.1f °C", tJunction),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (tJunction < 100.0) PcbEmerald else LogicHighRed
                            )
                            Text(
                                text = if (tJunction < 100.0) "✅ Aman (Di bawah batas kritis 125°C)" else "🚨 BAHAYA OVERHEAT! Gunakan Heatsink lebih besar / Kipas!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tJunction < 100.0) SiliconDark else LogicHighRed
                            )
                        }
                    }
                }
            }
        }

        // KARTU 3: BATTERY PACK BUILDER (18650 xSyP) & OFF-GRID SOLAR SIZING
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "☀️ Battery Pack (xSyP) & Solar Off-Grid Calculator",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dailyLoadWattHour,
                            onValueChange = { dailyLoadWattHour = it },
                            label = { Text("Beban Harian (Wh/hari)", color = SoftTraceBorder) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                        OutlinedTextField(
                            value = sunHoursPerDay,
                            onValueChange = { sunHoursPerDay = it },
                            label = { Text("Jam Matahari (PSH)", color = SoftTraceBorder) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                    }

                    Text("Konfigurasi Rakitan Sel Li-Ion 18650 (${seriesCountS}S ${parallelCountP}P):", color = BrightGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4, 6).forEach { s ->
                            FilterChip(
                                selected = seriesCountS == s,
                                onClick = { seriesCountS = s },
                                label = { Text("${s}S", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4, 6).forEach { p ->
                            FilterChip(
                                selected = parallelCountP == p,
                                onClick = { parallelCountP = p },
                                label = { Text("${p}P (${p * 3}Ah)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Surface(color = Color(0xFF052E22), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = String.format(Locale.US, "🔋 Pack %dS%dP (%d Sel): Nominal %.1fV (Penuh %.1fV)", seriesCountS, parallelCountP, seriesCountS * parallelCountP, packNominalVolt, packFullVolt),
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "⚡ Kapasitas Total: %.1f Ah  |  Energi: %.1f Wh", packCapAh, packEnergyWh),
                                color = BrightGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = String.format(Locale.US, "☀️ Rekomendasi Panel Surya Minimum: %.0f Wp", recommendedSolarWp),
                                color = Color(0xFF4ADE80),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}