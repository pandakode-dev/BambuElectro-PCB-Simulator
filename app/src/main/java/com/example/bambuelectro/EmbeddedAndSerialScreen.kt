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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EmbeddedAndSerialScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // ========================================================================
    // 1. STATE SERIAL MONITOR & REAL-TIME SERIAL PLOTTER
    // ========================================================================
    var isStreamingSerial by remember { mutableStateOf(true) }
    var selectedBaud by remember { mutableIntStateOf(115200) }
    var customCmdInput by remember { mutableStateOf("AT+READ_SENSOR=1") }
    var showHexMode by remember { mutableStateOf(false) }

    val plotterPoints = remember { mutableStateListOf<Float>() }
    val serialLogs = remember {
        mutableStateListOf(
            "[12:00:01] SYS: BambuElectro USB-OTG / UART Bridge Ready @115200 bps",
            "[12:00:02] RX <- {\"temp\":28.4, \"vbus\":12.1, \"adc\":2048}"
        )
    }

    // Generator paket telemetri berkala untuk menguji grafik Serial Plotter
    LaunchedEffect(isStreamingSerial) {
        var tick = 0f
        while (isStreamingSerial) {
            val simulatedSensorVal = (50f + 32f * sin(tick) + (-5..5).random()).coerceIn(5f, 95f)
            plotterPoints.add(simulatedSensorVal)
            if (plotterPoints.size > 50) plotterPoints.removeAt(0)

            if (tick.toInt() % 3 == 0 && serialLogs.size < 35) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val rawLine = String.format(Locale.US, "ADC_CH1:%.1f, V_IN:12.05", simulatedSensorVal)
                val formatted = if (showHexMode) {
                    rawLine.map { String.format("%02X", it.code) }.joinToString(" ")
                } else {
                    rawLine
                }
                serialLogs.add(0, "[$timeStr] RX <- $formatted")
            }
            tick += 0.25f
            delay(180L)
        }
    }

    // ========================================================================
    // 2. STATE KALKULATOR REGISTER TIMER INTERRUPT MCU (AVR / STM32 / ESP32)
    // ========================================================================
    var selectedMcuArch by remember { mutableStateOf("Arduino AVR (16MHz)") }
    var mcuClockMhzStr by remember { mutableStateOf("16.0") }
    var targetFreqHzStr by remember { mutableStateOf("1000.0") } // Default 1 kHz (1 ms tick)

    val fCpuHz = ((mcuClockMhzStr.toDoubleOrNull() ?: 16.0).coerceAtLeast(1.0)) * 1_000_000.0
    val fTarget = (targetFreqHzStr.toDoubleOrNull() ?: 1000.0).coerceAtLeast(0.1)

    // Cari Prescaler terbaik untuk Timer 16-bit (Maks 65535)
    val avrPrescalers = listOf(1, 8, 64, 256, 1024)
    val chosenPrescaler = remember(fCpuHz, fTarget, selectedMcuArch) {
        if (selectedMcuArch.contains("AVR")) {
            avrPrescalers.firstOrNull { p ->
                val ocr = (fCpuHz / (p * fTarget)) - 1.0
                ocr in 1.0..65535.0
            } ?: 1024
        } else {
            // Untuk STM32 / ESP32 bebas prescaler 16-bit
            ((fCpuHz / (fTarget * 10000.0)).roundToInt()).coerceIn(1, 65535)
        }
    }

    val compareRegVal = remember(fCpuHz, fTarget, chosenPrescaler) {
        ((fCpuHz / (chosenPrescaler * fTarget)) - 1.0).roundToInt().coerceIn(1, 65535)
    }

    val actualFreqHz = fCpuHz / (chosenPrescaler * (compareRegVal + 1.0))
    val freqErrorPct = (abs(actualFreqHz - fTarget) / fTarget) * 100.0

    val timerCCode = remember(selectedMcuArch, chosenPrescaler, compareRegVal, actualFreqHz) {
        if (selectedMcuArch.contains("AVR")) {
            """
            // Arduino Uno/Nano/Mega - Timer1 CTC Interrupt (${String.format(Locale.US, "%.2f", actualFreqHz)} Hz)
            void setupTimer1() {
              cli();
              TCCR1A = 0;
              TCCR1B = 0;
              TCNT1  = 0;
              OCR1A  = $compareRegVal; // Compare Match Register
              TCCR1B |= (1 << WGM12);  // Mode CTC
              // Prescaler = $chosenPrescaler
              TCCR1B |= (1 << CS11) | (1 << CS10);
              TIMSK1 |= (1 << OCIE1A); // Enable Timer1 Interrupt
              sei();
            }
            ISR(TIMER1_COMPA_vect) {
              // Kode dieksekusi presisi setiap ${String.format(Locale.US, "%.3f", 1000.0 / actualFreqHz)} ms
            }
            """.trimIndent()
        } else {
            """
            // STM32 HAL / ESP32 Timer Config (${String.format(Locale.US, "%.2f", actualFreqHz)} Hz)
            htim2.Instance = TIM2;
            htim2.Init.Prescaler = ${chosenPrescaler - 1};
            htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
            htim2.Init.Period = $compareRegVal; // Auto-Reload Register (ARR)
            HAL_TIM_Base_Init(&htim2);
            HAL_TIM_Base_Start_IT(&htim2);
            """.trimIndent()
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: USB-OTG / BLUETOOTH SERIAL MONITOR & REAL-TIME PLOTTER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔌 Real-Time Serial Plotter & Terminal",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Baudrate: $selectedBaud bps • Mode: ${if (showHexMode) "HEX" else "ASCII"}",
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = showHexMode,
                                onClick = { showHexMode = !showHexMode },
                                label = { Text("HEX", fontSize = 10.sp, color = PureWhite, fontWeight = FontWeight.Bold) }
                            )
                            Button(
                                onClick = { isStreamingSerial = !isStreamingSerial },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isStreamingSerial) LogicHighRed else PcbEmerald
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isStreamingSerial) "PAUSE" else "STREAM",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Kanvas Grafik Serial Plotter
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(12.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        for (r in 1..3) {
                            val gy = (h / 4f) * r
                            drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, gy), Offset(w, gy), 1f)
                        }

                        if (plotterPoints.size > 1) {
                            val path = Path()
                            val stepX = w / 50f
                            plotterPoints.forEachIndexed { idx, value ->
                                val px = idx * stepX
                                val py = h - ((value / 100f) * (h - 16f)) - 8f
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF38BDF8),
                                style = Stroke(width = 4f, cap = StrokeCap.Round)
                            )
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            "LIVE SERIAL PLOTTER: ${String.format(Locale.US, "%.1f", plotterPoints.lastOrNull() ?: 0f)}",
                            12f,
                            22f,
                            Paint().apply {
                                color = android.graphics.Color.parseColor("#FDE047")
                                textSize = 19f
                                isFakeBoldText = true
                            }
                        )
                    }

                    // Tombol Cepat Macro Perintah Mikrokontroler
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val macros = listOf("AT+RST", "RELAY_1=ON", "RELAY_1=OFF", "GET_ADC", "STATUS?")
                        items(macros) { cmd ->
                            Surface(
                                color = PcbDarkGreen,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BrightGold),
                                modifier = Modifier.clickable {
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    serialLogs.add(0, "[$timeStr] TX -> $cmd")
                                }
                            ) {
                                Text(
                                    text = "⚡ $cmd",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Baris Kirim Perintah Custom
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customCmdInput,
                            onValueChange = { customCmdInput = it },
                            label = { Text("Kirim Perintah Serial TX", color = SoftTraceBorder) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                        )
                        Button(
                            onClick = {
                                if (customCmdInput.isNotBlank()) {
                                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    serialLogs.add(0, "[$timeStr] TX -> $customCmdInput")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SEND", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }

                    // Kotak Log Konsol Serial
                    Surface(
                        color = Color(0xFF020806),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SoftTraceBorder.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            serialLogs.take(5).forEach { logLine ->
                                Text(
                                    text = logLine,
                                    color = if (logLine.contains("TX ->")) BrightGold else Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // KARTU 2: KALKULATOR REGISTER TIMER INTERRUPT MCU (ARDUINO / STM32 / ESP32)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "💻 MCU Hardware Timer Interrupt & Prescaler Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PcbDarkGreen
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val archList = listOf(
                            "Arduino AVR (16MHz)" to "16.0",
                            "STM32 BluePill (72MHz)" to "72.0",
                            "ESP32 Xtensa (80MHz APB)" to "80.0"
                        )
                        items(archList) { (arch, clk) ->
                            FilterChip(
                                selected = selectedMcuArch == arch,
                                onClick = {
                                    selectedMcuArch = arch
                                    mcuClockMhzStr = clk
                                },
                                label = { Text(arch, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mcuClockMhzStr,
                            onValueChange = { mcuClockMhzStr = it },
                            label = { Text("Clock CPU (MHz)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetFreqHzStr,
                            onValueChange = { targetFreqHzStr = it },
                            label = { Text("Target Interrupt (Hz)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Surface(
                        color = SchematicIvory,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, CopperGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Prescaler Terbaik: $chosenPrescaler  |  Nilai Register (OCR1A / ARR): $compareRegVal",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PcbDarkGreen
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Frekuensi Aktual: %.2f Hz (Error: %.3f%%)",
                                    actualFreqHz,
                                    freqErrorPct
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CopperGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(timerCCode))
                                    Toast.makeText(context, "Kode Register Timer C++ disalin!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Kode Interrupt Timer C++", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}