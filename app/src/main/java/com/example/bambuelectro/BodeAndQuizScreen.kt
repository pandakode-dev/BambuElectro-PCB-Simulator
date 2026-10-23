package com.example.bambuelectro

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.*

data class ElectroQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Composable
fun BodeAndQuizScreen() {
    // ========================================================================
    // 1. STATE ACTIVE FILTER BODE PLOT (LOW-PASS vs HIGH-PASS)
    // ========================================================================
    var isLowPass by remember { mutableStateOf(true) }
    var filterOrder by remember { mutableIntStateOf(1) } // Orde 1 (-20dB/dec) vs Orde 2 Sallen-Key (-40dB/dec)
    var rKohmSlider by remember { mutableFloatStateOf(10f) }  // 1 kOhm - 100 kOhm
    var cNfSlider by remember { mutableFloatStateOf(100f) }   // 10 nF - 1000 nF

    val rOhm = rKohmSlider * 1000.0
    val cFarad = cNfSlider * 1e-9
    val cutoffFreqHz = 1.0 / (2.0 * PI * rOhm * cFarad)

    // ========================================================================
    // 2. STATE GAMIFIED ELECTRONICS & PCB QUIZ
    // ========================================================================
    val quizBank = remember {
        listOf(
            ElectroQuizQuestion(
                "Resistor 4-gelang dengan warna: Kuning - Ungu - Merah - Emas memiliki nilai...",
                listOf("470 Ω ±5%", "4.7 kΩ ±5%", "47 kΩ ±5%", "4.7 MΩ ±1%"),
                1,
                "Kuning (4), Ungu (7), Merah (×100) = 4700 Ω = 4.7 kΩ dengan toleransi Emas (±5%)."
            ),
            ElectroQuizQuestion(
                "Resistor SMD dengan kode '4702' dan kapasitor keramik berkode '104' bernilai...",
                listOf("4.7 kΩ & 10 nF", "47 kΩ & 100 nF (0.1µF)", "470 Ω & 1 µF", "47 kΩ & 10 pF"),
                1,
                "SMD 4702 = 470 × 10² = 47,000 Ω (47 kΩ). Kapasitor 104 = 10 × 10⁴ pF = 100 nF = 0.1 µF."
            ),
            ElectroQuizQuestion(
                "Pada ESP32, manakah kelompok pin ADC yang tetap AMAN digunakan saat Wi-Fi aktif?",
                listOf("Pin ADC2 (GPIO4, GPIO2)", "Pin ADC1 (GPIO32 - GPIO39)", "Pin TX0 & RX0", "Pin GPIO12 Strapping"),
                1,
                "Driver Wi-Fi ESP32 menggunakan blok hardware ADC2, sehingga pembacaan sensor analog wajib memakai kanal ADC1 (GPIO32-39)."
            ),
            ElectroQuizQuestion(
                "Pada transmiter industri 4–20 mA dengan rentang ukur 0–100 Bar, berapakah tekanan saat arus terbaca 12 mA?",
                listOf("25 Bar", "50 Bar", "60 Bar", "75 Bar"),
                1,
                "12 mA berada tepat di tengah rentang 4–20 mA ((12-4)/16 = 50%), sehingga nilainya adalah 50 Bar."
            ),
            ElectroQuizQuestion(
                "Gerbang logika manakah yang menghasilkan output HIGH (1) HANYA JIKA kedua input-nya berbeda?",
                listOf("NAND (7400)", "NOR (7402)", "XOR (7486)", "AND (7408)"),
                2,
                "Gerbang Exclusive-OR (XOR) mengeluarkan logika 1 jika input A dan B berbeda (0-1 atau 1-0)."
            )
        )
    }

    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswerIdx by remember { mutableStateOf<Int?>(null) }

    val activeQuestion = quizBank[currentQuestionIdx]

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: BODE PLOT & ACTIVE OP-AMP FILTER DESIGNER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📊 Active Filter Bode Plot (Magnitude dB vs Log Freq)",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "Cut-Off (-3dB): fc = %.1f Hz  |  Roll-Off: -%d dB/decade",
                            cutoffFreqHz,
                            filterOrder * 20
                        ),
                        color = BrightGold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isLowPass,
                            onClick = { isLowPass = true },
                            label = { Text("Low-Pass (LPF)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = !isLowPass,
                            onClick = { isLowPass = false },
                            label = { Text("High-Pass (HPF)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = filterOrder == 2,
                            onClick = { filterOrder = if (filterOrder == 1) 2 else 1 },
                            label = { Text("Orde-$filterOrder (Sallen-Key)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Kanvas Bode Plot Skala Logaritmik (1 Hz s/d 100 kHz)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(12.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val topZeroDbY = h * 0.20f
                        val bottomMin40DbY = h * 0.88f

                        // Garis -3 dB Point
                        val minus3DbY = topZeroDbY + (3f / 40f) * (bottomMin40DbY - topZeroDbY)
                        drawLine(
                            color = Color(0xFFEF4444).copy(alpha = 0.6f),
                            start = Offset(0f, minus3DbY),
                            end = Offset(w, minus3DbY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Gambar Dekade Logaritmik (10^0 = 1Hz s/d 10^5 = 100kHz)
                        for (dec in 0..5) {
                            val xDec = (dec / 5f) * w
                            drawLine(Color.White.copy(alpha = 0.12f), Offset(xDec, 0f), Offset(xDec, h), 1f)
                        }

                        val bodePath = Path()
                        val steps = 140
                        for (s in 0..steps) {
                            val ratio = s.toFloat() / steps
                            val logFreq = ratio * 5.0 // 10^0 s/d 10^5 Hz
                            val f = 10.0.pow(logFreq)

                            val fRatio = if (isLowPass) (f / cutoffFreqHz) else (cutoffFreqHz / f)
                            val magLinear = 1.0 / sqrt(1.0 + fRatio.pow(2.0 * filterOrder))
                            val gainDb = (20.0 * log10(magLinear.coerceAtLeast(0.001))).coerceIn(-40.0, 0.0)

                            val x = ratio * w
                            val y = topZeroDbY + ((-gainDb.toFloat() / 40f) * (bottomMin40DbY - topZeroDbY))
                            if (s == 0) bodePath.moveTo(x, y) else bodePath.lineTo(x, y)
                        }

                        drawPath(
                            path = bodePath,
                            color = Color(0xFF34D399),
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )

                        // Tandai Posisi Frekuensi Cut-Off fc
                        val fcLogRatio = ((log10(cutoffFreqHz.coerceIn(1.0, 100000.0))) / 5.0).toFloat()
                        val fcX = fcLogRatio * w
                        drawCircle(color = BrightGold, radius = 8f, center = Offset(fcX, minus3DbY))
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "fc=%.0fHz (-3dB)", cutoffFreqHz),
                            (fcX - 40f).coerceIn(10f, w - 130f),
                            minus3DbY - 12f,
                            Paint().apply {
                                color = android.graphics.Color.YELLOW
                                textSize = 19f
                                isFakeBoldText = true
                            }
                        )
                    }

                    Text(String.format(Locale.US, "Resistor Filter (R): %.1f kΩ", rKohmSlider), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(value = rKohmSlider, onValueChange = { rKohmSlider = it }, valueRange = 1f..47f, colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = PcbEmerald))

                    Text(String.format(Locale.US, "Kapasitor Filter (C): %.0f nF", cNfSlider), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(value = cNfSlider, onValueChange = { cNfSlider = it }, valueRange = 10f..470f, colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = CopperGold))
                }
            }
        }

        // KARTU 2: KUIS TANTANGAN ELEKTRONIKA & TROUBLESHOOTING LAB
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Kuis Uji Kompetensi Elektronika (${currentQuestionIdx + 1}/${quizBank.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = PcbDarkGreen
                        )
                        Surface(color = BrightGold, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Skor: $score Poin",
                                color = SiliconDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = activeQuestion.question,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SiliconDark
                    )

                    activeQuestion.options.forEachIndexed { idx, optText ->
                        val isChosen = selectedAnswerIdx == idx
                        val isCorrect = idx == activeQuestion.correctIndex
                        val btnColor = when {
                            selectedAnswerIdx == null -> SchematicIvory
                            isCorrect -> Color(0xFFDCFCE7)
                            isChosen -> Color(0xFFFEE2E2)
                            else -> SchematicIvory
                        }

                        OutlinedButton(
                            onClick = {
                                if (selectedAnswerIdx == null) {
                                    selectedAnswerIdx = idx
                                    if (isCorrect) score += 20
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = btnColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${('A' + idx)}. $optText",
                                color = SiliconDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (selectedAnswerIdx != null) {
                        Surface(
                            color = SchematicIvory,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CopperGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "💡 Pembahasan: ${activeQuestion.explanation}",
                                    fontSize = 12.sp,
                                    color = PcbDarkGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = {
                                        selectedAnswerIdx = null
                                        currentQuestionIdx = (currentQuestionIdx + 1) % quizBank.size
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Soal Berikutnya ➔", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}