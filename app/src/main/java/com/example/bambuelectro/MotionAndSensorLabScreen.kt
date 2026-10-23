package com.example.bambuelectro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun MotionAndSensorLabScreen() {
    val context = LocalContext.current

    // ========================================================================
    // 1. STATE SENSOR FISIK HP: EMF GAUSS METER (µT) & VIBROMETER (m/s²)
    // ========================================================================
    var magX by remember { mutableFloatStateOf(18.5f) }
    var magY by remember { mutableFloatStateOf(-12.2f) }
    var magZ by remember { mutableFloatStateOf(34.0f) }

    var accX by remember { mutableFloatStateOf(0.12f) }
    var accY by remember { mutableFloatStateOf(0.08f) }
    var accZ by remember { mutableFloatStateOf(9.81f) }

    // Membaca Sensor Fisik Magnetometer & Accelerometer Asli di HP Pengguna
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magX = event.values[0]
                        magY = event.values[1]
                        magZ = event.values[2]
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        accX = event.values[0]
                        accY = event.values[1]
                        accZ = event.values[2]
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        accSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val totalEmfMicroTesla = sqrt(magX * magX + magY * magY + magZ * magZ)
    val totalEmfMilliGauss = totalEmfMicroTesla * 10f
    val netVibrationG = kotlin.math.abs(sqrt(accX * accX + accY * accY + accZ * accZ) - 9.81f)

    // ========================================================================
    // 2. STATE CNC / 3D PRINTER STEPPER DRIVER VREF & SERVO PWM
    // ========================================================================
    var selectedDriver by remember { mutableStateOf("A4988 (Rs=0.10Ω)") }
    var motorMaxCurrentAmp by remember { mutableStateOf("1.5") }
    var microsteps by remember { mutableIntStateOf(16) }
    var leadScrewPitchMm by remember { mutableStateOf("8.0") } // T8 Leadscrew standard 8mm/rev
    var servoAngleDeg by remember { mutableFloatStateOf(90f) }

    val iMax = (motorMaxCurrentAmp.toDoubleOrNull() ?: 1.5).coerceAtLeast(0.1)
    val iSafe = iMax * 0.80 // Standar keamanan 80% arus puncak motor stepper

    val vRefStepperVolt = remember(selectedDriver, iSafe) {
        when {
            selectedDriver.startsWith("A4988") -> iSafe * 8.0 * 0.10
            selectedDriver.startsWith("DRV8825") -> iSafe / 2.0
            else -> (iSafe * 2.5) / 1.77 // TMC2208 / TMC2209 RMS
        }
    }

    val pitch = (leadScrewPitchMm.toDoubleOrNull() ?: 8.0).coerceAtLeast(0.5)
    val stepsPerMm = (200.0 * microsteps) / pitch // Motor standar 1.8 deg = 200 full steps/rev
    val servoPulseUs = 500 + ((servoAngleDeg / 180f) * 2000f).toInt()
    val servoDutyPct = (servoPulseUs / 20000f) * 100f // Periode 50Hz = 20,000 us

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: HARDWARE EMF GAUSS METER & 3-AXIS MECHANICAL VIBROMETER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🧲 Hardware EMF Gauss Meter & Vibrometer (Sensor HP)",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Dekatkan HP ke trafo, motor listrik, induktor, atau speaker untuk mendeteksi radiasi elektromagnetik & getaran mekanis.",
                        color = SoftTraceBorder,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF052E22),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, if (totalEmfMicroTesla > 85f) LogicHighRed else BrightGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("MEDAN MAGNET (EMF)", fontSize = 10.sp, color = SoftTraceBorder, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.US, "%.1f µT", totalEmfMicroTesla),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalEmfMicroTesla > 85f) LogicHighRed else BrightGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f mG (MilliGauss)", totalEmfMilliGauss),
                                    fontSize = 10.sp,
                                    color = PureWhite
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF052E22),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, SoftTraceBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("GETARAN MEKANIS (IMU)", fontSize = 10.sp, color = SoftTraceBorder, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.US, "%.2f m/s²", netVibrationG),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = String.format(Locale.US, "X:%.1f Y:%.1f Z:%.1f", accX, accY, accZ),
                                    fontSize = 10.sp,
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    // Bar Indikator Intensitas Medan Elektromagnetik
                    LinearProgressIndicator(
                        progress = { (totalEmfMicroTesla / 200f).coerceIn(0.05f, 1f) },
                        color = if (totalEmfMicroTesla > 85f) LogicHighRed else PcbEmerald,
                        trackColor = Color(0xFF04130F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }

        // KARTU 2: KALKULATOR VREF DRIVER MOTOR STEPPER & STEPS/MM CNC
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚙️ CNC & 3D Printer Stepper Driver Vref Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val drivers = listOf("A4988 (Rs=0.10Ω)", "DRV8825 (825)", "TMC2208 / TMC2209")
                        items(drivers) { drv ->
                            FilterChip(
                                selected = selectedDriver == drv,
                                onClick = { selectedDriver = drv },
                                label = { Text(drv, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = motorMaxCurrentAmp,
                            onValueChange = { motorMaxCurrentAmp = it },
                            label = { Text("Arus Motor (Ampere)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = leadScrewPitchMm,
                            onValueChange = { leadScrewPitchMm = it },
                            label = { Text("Pitch Ulir/Belt (mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(8, 16, 32).forEach { ms ->
                            FilterChip(
                                selected = microsteps == ms,
                                onClick = { microsteps = ms },
                                label = { Text("1/$ms Microstep", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Surface(color = PcbDarkGreen, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = String.format(Locale.US, "🎯 Setel Tegangan Trim-Pot Vref = %.2f Volt", vRefStepperVolt),
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "Arus Kerja Aman (80%%): %.2f A  |  GRBL/Marlin: %.1f steps/mm", iSafe, stepsPerMm),
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // KARTU 3: SIMULATOR PULSA PWM MOTOR SERVO (50Hz: 500µs - 2500µs)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🦾 Simulator Sudut & Lebar Pulsa PWM Motor Servo (50 Hz)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = PcbDarkGreen
                    )
                    Text(
                        text = String.format(Locale.US, "Sudut: %.0f°  ➔  Lebar Pulsa HIGH: %d µs (Duty: %.2f%%)", servoAngleDeg, servoPulseUs, servoDutyPct),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = CopperGold
                    )
                    Slider(
                        value = servoAngleDeg,
                        onValueChange = { servoAngleDeg = it },
                        valueRange = 0f..180f,
                        colors = SliderDefaults.colors(thumbColor = CopperGold, activeTrackColor = PcbEmerald)
                    )
                }
            }
        }
    }
}