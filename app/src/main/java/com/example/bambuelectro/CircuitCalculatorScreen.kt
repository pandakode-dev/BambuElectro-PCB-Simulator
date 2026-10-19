package com.example.bambuelectro

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ln

@Composable
fun CircuitCalculatorScreen() {
    var selectedCalcTab by remember { mutableIntStateOf(0) }

    val calcCategories = listOf(
        0 to "⚡ Ohm, LED & Divider",
        1 to "🔋 Baterai IoT & ADC",
        2 to "📐 Jalur PCB & IC 555"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Navigasi Kategori Kalkulator Sirkuit
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(calcCategories) { (idx, title) ->
                    val isSelected = selectedCalcTab == idx
                    Surface(
                        color = if (isSelected) PcbDarkGreen else PureWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isSelected) BrightGold else SoftTraceBorder),
                        modifier = Modifier.clickable { selectedCalcTab = idx }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) PureWhite else SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        when (selectedCalcTab) {
            0 -> {
                item { OhmAndPowerSafetyCard() }
                item { LedAndVoltageDividerCard() }
            }
            1 -> {
                item { IotBatteryLifeEstimatorCard() }
                item { McuAdcAndOpAmpCard() }
            }
            2 -> {
                item { PcbTraceIpc2221Card() }
                item { Ne555AndRcFilterCard() }
            }
        }
    }
}

// ============================================================================
// 1. KALKULATOR HUKUM OHM, DAYA WATT & REKOMENDASI RATING FISIK RESISTOR
// ============================================================================
@Composable
fun OhmAndPowerSafetyCard() {
    var voltageStr by remember { mutableStateOf("12.0") }
    var resistanceStr by remember { mutableStateOf("470") }

    val v = voltageStr.toDoubleOrNull() ?: 0.0
    val r = (resistanceStr.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.01)
    val currentAmp = v / r
    val currentMa = currentAmp * 1000.0
    val powerWatt = v * currentAmp

    // Rekomendasi aman minimal 2x disipasi daya aktual (Derating Safety Factor)
    val recommendedWattPackage = when {
        powerWatt <= 0.125 -> "1/4 Watt (0.25W Standar)"
        powerWatt <= 0.25 -> "1/2 Watt (0.5W)"
        powerWatt <= 0.5 -> "1 Watt (Metal Oxide)"
        powerWatt <= 1.0 -> "2 Watt (Power Resistor)"
        powerWatt <= 2.5 -> "5 Watt (Kapur / Wirewound)"
        else -> "10W+ Aluminium Heatsink Resistor!"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, PcbEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "⚡ Hukum Ohm, Disipasi Daya & Rekomendasi Watt Resistor",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = PcbDarkGreen
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = voltageStr,
                    onValueChange = { voltageStr = it },
                    label = { Text("Tegangan Sumber V (Volt)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = resistanceStr,
                    onValueChange = { resistanceStr = it },
                    label = { Text("Beban Resistor R (Ω)") },
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
                        Text("Arus Mengalir (I = V/R):", color = SoftTraceBorder, fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.US, "%.2f mA (%.4f A)", currentMa, currentAmp),
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daya Panas (P = V×I):", color = SoftTraceBorder, fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.US, "%.3f Watt (%.1f mW)", powerWatt, powerWatt * 1000),
                            color = BrightGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    HorizontalDivider(color = PcbEmerald)
                    Text(
                        text = "🛡️ Rekomendasi Fisik: Gunakan minimal $recommendedWattPackage",
                        color = if (powerWatt > 0.5) LogicHighRed else SoftTraceBorder,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. KALKULATOR RESISTOR LED & PEMBAGI TEGANGAN (VOLTAGE DIVIDER / LEVEL SHIFTER)
// ============================================================================
@Composable
fun LedAndVoltageDividerCard() {
    var vSourceLed by remember { mutableStateOf("5.0") }
    var vForwardLed by remember { mutableStateOf("2.0") }
    var iLedMa by remember { mutableStateOf("15.0") }

    val vs = vSourceLed.toDoubleOrNull() ?: 5.0
    val vf = vForwardLed.toDoubleOrNull() ?: 2.0
    val ima = (iLedMa.toDoubleOrNull() ?: 15.0).coerceAtLeast(1.0)
    val rLedCalculated = ((vs - vf).coerceAtLeast(0.0) / (ima / 1000.0))

    // Voltage Divider State (Misal menurunkan sensor 5V ke pin 3.3V ESP32)
    var vinDiv by remember { mutableStateOf("5.0") }
    var r1Div by remember { mutableStateOf("1000") }
    var r2Div by remember { mutableStateOf("2000") }

    val vIn = vinDiv.toDoubleOrNull() ?: 5.0
    val r1 = (r1Div.toDoubleOrNull() ?: 1000.0).coerceAtLeast(1.0)
    val r2 = (r2Div.toDoubleOrNull() ?: 2000.0).coerceAtLeast(1.0)
    val vOut = vIn * (r2 / (r1 + r2))

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, SoftTraceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("💡 Kalkulator Resistor Pengaman LED", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)

            // Pilihan Cepat Warna LED
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val ledPresets = listOf(
                    "Merah/Kuning (2.0V)" to "2.0",
                    "Hijau/Biru/Putih (3.2V)" to "3.2",
                    "Inframerah IR (1.4V)" to "1.4"
                )
                items(ledPresets) { (lbl, vfVal) ->
                    SuggestionChip(
                        onClick = { vForwardLed = vfVal },
                        label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vSourceLed, onValueChange = { vSourceLed = it }, label = { Text("Vs (Volt)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = vForwardLed, onValueChange = { vForwardLed = it }, label = { Text("Vf LED (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = iLedMa, onValueChange = { iLedMa = it }, label = { Text("Arus (mA)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Text(
                text = "✅ Pasang Resistor Seri: ${ElectroMath.formatResistance(rLedCalculated)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = CopperGold
            )

            HorizontalDivider(color = SoftTraceBorder)

            Text("🔀 Pembagi Tegangan (Voltage Divider 5V ➔ 3.3V ESP32)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vinDiv, onValueChange = { vinDiv = it }, label = { Text("Vin (V)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = r1Div, onValueChange = { r1Div = it }, label = { Text("R1 Atas (Ω)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = r2Div, onValueChange = { r2Div = it }, label = { Text("R2 Bawah (Ω)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Surface(color = SchematicIvory, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CopperGold)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tegangan Keluaran (Vout):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f Volt %s", vOut, if (vOut <= 3.35) "(Aman untuk ESP32)" else "(⚠️ >3.3V!)"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (vOut <= 3.35) PcbEmerald else LogicHighRed
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. FITUR EKSKLUSIF: IOT DEEP-SLEEP BATTERY LIFE ESTIMATOR (ESP32 / LORA)
// ============================================================================
@Composable
fun IotBatteryLifeEstimatorCard() {
    var batteryCapMah by remember { mutableStateOf("3000") }      // 1x 18650 Li-Ion
    var activeCurrentMa by remember { mutableStateOf("120") }     // ESP32 Wi-Fi Tx
    var sleepCurrentUa by remember { mutableStateOf("150") }      // Deep Sleep 150 µA
    var wakeDurationSec by remember { mutableStateOf("4") }       // Bangun 4 detik
    var sleepIntervalMin by remember { mutableStateOf("15") }     // Kirim data tiap 15 menit

    val cap = (batteryCapMah.toDoubleOrNull() ?: 3000.0) * 0.85 // Efisiensi riil baterai 85%
    val iActiveMa = activeCurrentMa.toDoubleOrNull() ?: 120.0
    val iSleepMa = (sleepCurrentUa.toDoubleOrNull() ?: 150.0) / 1000.0
    val tWakeSec = (wakeDurationSec.toDoubleOrNull() ?: 4.0).coerceAtLeast(0.1)
    val tCycleSec = ((sleepIntervalMin.toDoubleOrNull() ?: 15.0) * 60.0).coerceAtLeast(tWakeSec)

    val avgCurrentMa = ((iActiveMa * tWakeSec) + (iSleepMa * (tCycleSec - tWakeSec))) / tCycleSec
    val totalHours = if (avgCurrentMa > 0) cap / avgCurrentMa else 0.0
    val totalDays = totalHours / 24.0

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, CopperGold),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🔋 IoT Node Deep-Sleep Battery Life Estimator (ESP32/LoRa)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = PcbDarkGreen
            )
            Text(
                text = "Simulasikan ketahanan baterai proyek sensor nirkabel Anda berdasarkan siklus bangun dan tidur.",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = batteryCapMah, onValueChange = { batteryCapMah = it }, label = { Text("Kapasitas (mAh)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = activeCurrentMa, onValueChange = { activeCurrentMa = it }, label = { Text("Arus Aktif (mA)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = sleepCurrentUa, onValueChange = { sleepCurrentUa = it }, label = { Text("Deep Sleep (µA)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = wakeDurationSec, onValueChange = { wakeDurationSec = it }, label = { Text("Waktu Aktif (dtk)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = sleepIntervalMin, onValueChange = { sleepIntervalMin = it }, label = { Text("Interval (Mnt)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Surface(color = PcbDarkGreen, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = String.format(Locale.US, "Konsumsi Rata-Rata: %.3f mA", avgCurrentMa),
                        color = SoftTraceBorder,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.US, "⏱️ Estimasi Hidup: %.1f Hari (%.1f Bulan)", totalDays, totalDays / 30.0),
                        color = BrightGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// 4. KALKULATOR RESOLUSI ADC MIKROKONTROLER (ESP32/ARDUINO) & GAIN OP-AMP
// ============================================================================
@Composable
fun McuAdcAndOpAmpCard() {
    var vRefAdc by remember { mutableStateOf("3.3") }
    var vSensorIn by remember { mutableStateOf("1.65") }
    var adcBits by remember { mutableIntStateOf(12) } // 10-bit Arduino, 12-bit ESP32, 16-bit ADS1115

    val vRef = (vRefAdc.toDoubleOrNull() ?: 3.3).coerceAtLeast(0.1)
    val vIn = (vSensorIn.toDoubleOrNull() ?: 1.65).coerceIn(0.0, vRef)
    val maxSteps = (1 shl adcBits) - 1
    val digitalValue = ((vIn / vRef) * maxSteps).toInt()
    val resolutionMv = (vRef * 1000.0) / maxSteps

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, SoftTraceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔢 Konverter ADC Mikrokontroler (Arduino / ESP32 / ADS1115)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10 to "10-Bit (Uno 1023)", 12 to "12-Bit (ESP32 4095)", 16 to "16-Bit (65535)").forEach { (bits, lbl) ->
                    FilterChip(
                        selected = adcBits == bits,
                        onClick = { adcBits = bits },
                        label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vRefAdc, onValueChange = { vRefAdc = it }, label = { Text("Vref MCU (Volt)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = vSensorIn, onValueChange = { vSensorIn = it }, label = { Text("Vin Sensor (Volt)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Surface(color = SchematicIvory, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SoftTraceBorder)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nilai Digital Raw ADC: $digitalValue (dari 0 s/d $maxSteps)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    Text(String.format(Locale.US, "Resolusi Step terkecil (LSB): %.3f mV / step", resolutionMv), fontSize = 12.sp, color = CopperGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 5. KALKULATOR LEBAR JALUR TEMBAGA PCB (STANDAR INDUSTRI IPC-2221)
// ============================================================================
@Composable
fun PcbTraceIpc2221Card() {
    var traceCurrentAmp by remember { mutableStateOf("3.0") }
    var tempRiseC by remember { mutableStateOf("10.0") }
    var copperWeightOz by remember { mutableDoubleStateOf(1.0) }

    val amp = traceCurrentAmp.toDoubleOrNull() ?: 3.0
    val dt = tempRiseC.toDoubleOrNull() ?: 10.0
    val (widthMm, widthMils) = ElectroMath.calculatePcbTraceWidthMm(amp, dt, copperWeightOz)

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, PcbEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📐 Kalkulator Lebar Jalur PCB (Standar IPC-2221)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)
            Text("Cegah jalur PCB terbakar saat dilewati arus besar (Power Supply / Motor Driver).", fontSize = 11.sp, color = Color.Gray)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = traceCurrentAmp, onValueChange = { traceCurrentAmp = it }, label = { Text("Arus Jalur (Ampere)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = tempRiseC, onValueChange = { tempRiseC = it }, label = { Text("Batas Naik Suhu (°C)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0 to "Ketebalan 1 oz (35 µm - Standar)", 2.0 to "Ketebalan 2 oz (70 µm - Tebal)").forEach { (oz, lbl) ->
                    FilterChip(
                        selected = copperWeightOz == oz,
                        onClick = { copperWeightOz = oz },
                        label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Surface(color = PcbDarkGreen, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Lebar Jalur Minimum (External Layer):", color = SoftTraceBorder, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f mm  (%.1f mil)", widthMm, widthMils),
                        color = BrightGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. KALKULATOR IC NE555 ASTABLE PWM & FILTER PASIF RC CUT-OFF
// ============================================================================
@Composable
fun Ne555AndRcFilterCard() {
    var r1Kohm by remember { mutableStateOf("1.0") }
    var r2Kohm by remember { mutableStateOf("10.0") }
    var capUf by remember { mutableStateOf("10.0") }

    val r1 = (r1Kohm.toDoubleOrNull() ?: 1.0) * 1000.0
    val r2 = (r2Kohm.toDoubleOrNull() ?: 10.0) * 1000.0
    val cFarad = ((capUf.toDoubleOrNull() ?: 10.0) * 1e-6).coerceAtLeast(1e-12)

    val freq555 = 1.44 / ((r1 + 2.0 * r2) * cFarad)
    val dutyCycle555 = ((r1 + r2) / (r1 + 2.0 * r2)) * 100.0
    val rcCutoff = ElectroMath.calculateRcCutoffHz(r2, capUf.toDoubleOrNull() ?: 10.0)

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, SoftTraceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎛️ IC NE555 Timer (Astable Oscillator) & Filter RC", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = r1Kohm, onValueChange = { r1Kohm = it }, label = { Text("R1 (kΩ)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = r2Kohm, onValueChange = { r2Kohm = it }, label = { Text("R2 (kΩ)") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = capUf, onValueChange = { capUf = it }, label = { Text("C (µF)") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Surface(color = SchematicIvory, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CopperGold)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(String.format(Locale.US, "• Frekuensi Output IC 555: %.2f Hz", freq555), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PcbDarkGreen)
                    Text(String.format(Locale.US, "• Duty Cycle PWM: %.1f %%", dutyCycle555), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CopperGold)
                    Text(String.format(Locale.US, "• Cut-Off Filter RC (R2 & C): %.2f Hz", rcCutoff), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SiliconDark)
                }
            }
        }
    }
}