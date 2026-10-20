package com.example.bambuelectro

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

// ============================================================================
// 1. KATALOG KOMPONEN + 4 ALAT UKUR YANG BISA DITARIK (DRAG-AND-DROP INSTRUMENTS)
// ============================================================================
enum class SimCompType(val label: String, val shortCode: String, val badgeColor: Color, val category: String) {
    // Kategori Baru: ALAT UKUR INTERAKTIF (Bisa Ditarik & Diklik!)
    METER_DMM("Multimeter (DMM)", "DMM", Color(0xFFF59E0B), "Alat Ukur"),
    SCOPE_PROBE("Osiloskop CH1/2", "OSC", Color(0xFF06B6D4), "Alat Ukur"),
    WATTMETER("Wattmeter Digital", "WATT", Color(0xFF10B981), "Alat Ukur"),
    LOGIC_PROBE("Logic Probe H/L", "L-PRB", Color(0xFFEC4899), "Alat Ukur"),

    // Komponen Rangkaian
    DC_SOURCE("PSU DC (V)", "VCC", Color(0xFFDC2626), "Sumber"),
    FUNC_GEN("Func Gen AC", "ACG", Color(0xFF0284C7), "Sumber"),
    GROUND("Ground 0V", "GND", Color(0xFF334155), "Sumber"),
    RESISTOR("Resistor", "R", Color(0xFFD97706), "Pasif"),
    POTENTIO("Potensiometer", "VR", Color(0xFFB45309), "Pasif"),
    CAPACITOR("Kapasitor", "C", Color(0xFF2563EB), "Pasif"),
    INDUCTOR("Induktor", "L", Color(0xFF0D9488), "Pasif"),
    ZENER("Dioda Zener", "DZ", Color(0xFFE11D48), "Semikonduktor"),
    NPN_BJT("Transistor NPN", "Q-NPN", Color(0xFF4F46E5), "Semikonduktor"),
    MOSFET_N("MOSFET N-Ch", "MOS", Color(0xFF6366F1), "Semikonduktor"),
    NE555_IC("IC Timer 555", "555", Color(0xFF475569), "IC & Aktuator"),
    LED_LAMP("Lampu LED", "LED", Color(0xFF16A34A), "IC & Aktuator"),
    BUZZER("Buzzer Piezo", "BUZ", Color(0xFFCA8A04), "IC & Aktuator"),
    SEVEN_SEG("7-Segment", "7SEG", Color(0xFFBE123C), "IC & Aktuator"),
    LOGIC_SW("Switch 1/0", "SW", Color(0xFF0891B2), "Digital"),
    GATE_AND("Gate AND", "AND", Color(0xFF7C3AED), "Digital"),
    GATE_OR("Gate OR", "OR", Color(0xFF9333EA), "Digital"),
    GATE_NAND("Gate NAND", "NAND", Color(0xFFC026D3), "Digital"),
    GATE_XOR("Gate XOR", "XOR", Color(0xFFDB2777), "Digital")
}

data class SimNodeComponent(
    val id: Int,
    val type: SimCompType,
    val title: String,
    val relX: Float,
    val relY: Float,
    val valueNum: Double, // Untuk DMM: 0.0=Volt, 1.0=Ampere(mA), 2.0=Ohm
    val nodeVoltage: Double = 0.0,
    val currentMa: Double = 0.0,
    val powerMw: Double = 0.0,
    val isActiveOutput: Boolean = false,
    val isOvercurrent: Boolean = false
)

data class SimWireConnection(
    val fromId: Int,
    val toId: Int,
    val colorHex: Long
)

data class SavedCircuitProject(
    val id: Long,
    val name: String,
    val timestamp: String,
    val psuVoltage: Float,
    val components: List<SimNodeComponent>,
    val wires: List<SimWireConnection>
)

enum class WaveShape(val label: String) {
    SINE("Sinus (AC)"),
    SQUARE("Kotak (PWM)"),
    TRIANGLE("Segitiga"),
    SAWTOOTH("Gigi Gergaji"),
    RC_FILTERED("Filter RC")
}

// ============================================================================
// 2. KONVERTER GELANG WARNA & PENGGAMBAR KOMPONEN + ALAT UKUR REALISTIS
// ============================================================================
fun ohmValueToBandColors(ohms: Double): List<Color> {
    val digitColors = listOf(
        Color(0xFF18181B), Color(0xFF78350F), Color(0xFFDC2626), Color(0xFFF97316),
        Color(0xFFEAB308), Color(0xFF16A34A), Color(0xFF2563EB), Color(0xFF9333EA),
        Color(0xFF64748B), Color(0xFFF8FAFC)
    )
    val goldTol = Color(0xFFD97706)
    val silverMult = Color(0xFF94A3B8)

    val safeOhm = ohms.coerceAtLeast(0.1)
    val exp = floor(log10(safeOhm)).toInt() - 1
    val normalized = (safeOhm / 10.0.pow(exp.toDouble())).roundToInt().coerceIn(10, 99)

    val d1 = (normalized / 10).coerceIn(0, 9)
    val d2 = (normalized % 10).coerceIn(0, 9)
    val multColor = when (exp) {
        -2 -> silverMult
        -1 -> goldTol
        in 0..9 -> digitColors[exp]
        else -> digitColors[3]
    }
    return listOf(digitColors[d1], digitColors[d2], multColor, goldTol)
}

fun DrawScope.drawRealisticComponent(
    comp: SimNodeComponent,
    cx: Float,
    cy: Float,
    isSelected: Boolean,
    isWireSource: Boolean,
    burnPhase: Float
) {
    val halfW = 58f
    val halfH = 28f
    val borderCol = when {
        isWireSource -> Color.Red
        isSelected -> BrightGold
        else -> comp.type.badgeColor
    }

    // Kawat Kaki & Terminal Probe Kiri-Kanan
    drawLine(Color(0xFFCBD5E1), Offset(cx - halfW - 14f, cy), Offset(cx - halfW + 8f, cy), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(cx + halfW - 8f, cy), Offset(cx + halfW + 14f, cy), strokeWidth = 5f, cap = StrokeCap.Round)
    drawCircle(color = Color(0xFFEF4444), radius = 6.5f, center = Offset(cx - halfW - 12f, cy)) // Terminal + Merah
    drawCircle(color = BrightGold, radius = 6.5f, center = Offset(cx + halfW + 12f, cy))        // Terminal -/Out Emas

    if (comp.isOvercurrent) {
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.5f + 0.3f * sin(burnPhase * 15f)),
            radius = 64f,
            center = Offset(cx, cy)
        )
        for (i in 0..4) {
            val offset = (burnPhase * 80f + i * 20) % 60f
            drawCircle(
                color = Color.LightGray.copy(alpha = (1f - offset / 60f) * 0.45f),
                radius = 8f + offset / 4f,
                center = Offset(cx + sin(burnPhase * 5f + i) * 15f, cy - 25f - offset)
            )
        }
    }

    when (comp.type) {
        // 1. ALAT UKUR MULTIMETER DIGITAL (DMM) DI ATAS KANVAS
        SimCompType.METER_DMM -> {
            val w = 114f
            val h = 64f
            drawRoundRect(
                color = Color(0xFFB45309), // Casing Kuning/Oranye Fluke Style
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Layar LCD Hijau Gelap di dalam Multimeter
            drawRoundRect(
                color = Color(0xFF064E3B),
                topLeft = Offset(cx - w / 2 + 8f, cy - h / 2 + 8f),
                size = Size(w - 16f, 28f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            val dmmMode = comp.valueNum.toInt()
            val lcdText = when (dmmMode) {
                1 -> String.format(Locale.US, "%.2f mA", comp.currentMa)
                2 -> String.format(Locale.US, "%.0f Ω", if (comp.currentMa > 0) (comp.nodeVoltage / (comp.currentMa / 1000.0)) else 1000.0)
                else -> String.format(Locale.US, "%.2f V", comp.nodeVoltage)
            }
            drawContext.canvas.nativeCanvas.drawText(
                lcdText,
                cx,
                cy - 2f,
                Paint().apply {
                    color = android.graphics.Color.parseColor("#FDE047")
                    textSize = 21f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
            )
            drawRoundRect(
                color = if (isSelected) PureWhite else BrightGold,
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = if (isSelected) 4.5f else 2.5f)
            )
        }

        // 2. ALAT UKUR OSILOSKOP MINI (KLIK UNTUK BUKA LAYAR CH1 & CH2!)
        SimCompType.SCOPE_PROBE -> {
            val w = 118f
            val h = 64f
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Gambar Gelombang Mini CH1 (Kuning) & CH2 (Cyan) di Bodi Osiloskop
            val wave1 = Path()
            val wave2 = Path()
            for (xStep in -40..40 step 4) {
                val px = cx + xStep
                val angle = (xStep * 0.12f) + (burnPhase * 8f)
                val py1 = cy - 6f - sin(angle) * 10f
                val py2 = cy + 10f - cos(angle) * 8f
                if (xStep == -40) {
                    wave1.moveTo(px, py1)
                    wave2.moveTo(px, py2)
                } else {
                    wave1.lineTo(px, py1)
                    wave2.lineTo(px, py2)
                }
            }
            drawPath(wave1, Color(0xFFFACC15), style = Stroke(width = 3f))
            drawPath(wave2, Color(0xFF38BDF8), style = Stroke(width = 3f))
            drawRoundRect(
                color = Color(0xFF06B6D4),
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = if (isSelected) 4.5f else 3f)
            )
        }

        // 3. WATTMETER DIGITAL & LOGIC PROBE
        SimCompType.WATTMETER, SimCompType.LOGIC_PROBE -> {
            val w = 110f
            val h = 58f
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            val txt = if (comp.type == SimCompType.WATTMETER) {
                String.format(Locale.US, "%.1f mW", comp.powerMw)
            } else {
                if (comp.nodeVoltage >= 2.5) "HIGH [1]" else "LOW [0]"
            }
            drawContext.canvas.nativeCanvas.drawText(
                txt,
                cx,
                cy + 7f,
                Paint().apply {
                    color = if (comp.nodeVoltage >= 2.5) android.graphics.Color.parseColor("#4ADE80") else android.graphics.Color.parseColor("#F87171")
                    textSize = 21f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
            )
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = if (isSelected) 4.5f else 2.5f)
            )
        }

        // 4. RESISTOR FISIK + 4 GELANG WARNA DINAMIS
        SimCompType.RESISTOR -> {
            val rBodyW = 96f
            val rBodyH = 38f
            val left = cx - rBodyW / 2
            val top = cy - rBodyH / 2

            drawRoundRect(
                color = if (comp.isOvercurrent) Color(0xFF78350F) else Color(0xFFFDE68A),
                topLeft = Offset(left, top),
                size = Size(rBodyW, rBodyH),
                cornerRadius = CornerRadius(18f, 18f)
            )
            val bands = ohmValueToBandColors(comp.valueNum)
            val bandStep = rBodyW / 5.2f
            bands.forEachIndexed { idx, bCol ->
                val bx = left + 14f + (idx * bandStep)
                drawRect(
                    color = if (comp.isOvercurrent) Color.DarkGray else bCol,
                    topLeft = Offset(bx, top),
                    size = Size(10f, rBodyH)
                )
            }
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(left, top),
                size = Size(rBodyW, rBodyH),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = if (isSelected) 4.5f else 2.5f)
            )
        }

        SimCompType.POTENTIO -> {
            drawCircle(color = Color(0xFF1E293B), radius = 30f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 30f, center = Offset(cx, cy), style = Stroke(width = 4f))
            val angleRad = ((comp.valueNum / 100.0) * 1.5 * PI - (0.75 * PI)).toFloat()
            drawLine(
                color = BrightGold,
                start = Offset(cx, cy),
                end = Offset(cx + 22f * cos(angleRad), cy + 22f * sin(angleRad)),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        SimCompType.GATE_AND, SimCompType.GATE_NAND -> {
            val path = Path().apply {
                moveTo(cx - 42f, cy - 28f)
                lineTo(cx + 4f, cy - 28f)
                cubicTo(cx + 48f, cy - 28f, cx + 48f, cy + 28f, cx + 4f, cy + 28f)
                lineTo(cx - 42f, cy + 28f)
                close()
            }
            drawPath(path = path, color = Color(0xFF31104F))
            drawPath(path = path, color = borderCol, style = Stroke(width = if (isSelected) 4.5f else 3f))
            if (comp.type == SimCompType.GATE_NAND) {
                drawCircle(color = Color(0xFF31104F), radius = 7f, center = Offset(cx + 44f, cy))
                drawCircle(color = borderCol, radius = 7f, center = Offset(cx + 44f, cy), style = Stroke(width = 3f))
            }
        }

        SimCompType.GATE_OR, SimCompType.GATE_XOR -> {
            val path = Path().apply {
                moveTo(cx - 42f, cy - 28f)
                quadraticBezierTo(cx + 10f, cy - 28f, cx + 44f, cy)
                quadraticBezierTo(cx + 10f, cy + 28f, cx - 42f, cy + 28f)
                quadraticBezierTo(cx - 22f, cy, cx - 42f, cy - 28f)
                close()
            }
            drawPath(path = path, color = Color(0xFF3B0764))
            drawPath(path = path, color = borderCol, style = Stroke(width = if (isSelected) 4.5f else 3f))
            if (comp.type == SimCompType.GATE_XOR) {
                val xorArc = Path().apply {
                    moveTo(cx - 50f, cy - 28f)
                    quadraticBezierTo(cx - 30f, cy, cx - 50f, cy + 28f)
                }
                drawPath(path = xorArc, color = borderCol, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            }
        }

        SimCompType.LED_LAMP -> {
            if (comp.isActiveOutput) {
                drawCircle(
                    color = if (comp.isOvercurrent) Color.Red.copy(alpha = 0.5f) else Color(0xFF4ADE80).copy(alpha = 0.42f),
                    radius = 56f,
                    center = Offset(cx, cy)
                )
            }
            val ledColor = when {
                comp.isOvercurrent -> Color(0xFFEF4444)
                comp.isActiveOutput -> Color(0xFF22C55E)
                else -> Color(0xFF14532D)
            }
            drawCircle(color = ledColor, radius = 26f, center = Offset(cx, cy))
            drawCircle(color = PureWhite.copy(alpha = 0.5f), radius = 9f, center = Offset(cx - 7f, cy - 7f))
            drawCircle(color = borderCol, radius = 26f, center = Offset(cx, cy), style = Stroke(width = if (isSelected) 4.5f else 2.5f))
        }

        SimCompType.CAPACITOR -> {
            drawCircle(color = Color(0xFF1E3A8A), radius = 28f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 28f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawLine(PureWhite, Offset(cx - 7f, cy - 18f), Offset(cx - 7f, cy + 18f), strokeWidth = 5f)
            drawLine(BrightGold, Offset(cx + 7f, cy - 18f), Offset(cx + 7f, cy + 18f), strokeWidth = 5f)
        }

        SimCompType.INDUCTOR -> {
            drawRoundRect(Color(0xFF134E4A), Offset(cx - 48f, cy - 22f), Size(96f, 44f), CornerRadius(12f, 12f))
            for (i in -1..1) {
                drawArc(
                    color = BrightGold,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx + (i * 24f) - 12f, cy - 14f),
                    size = Size(24f, 28f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }
            drawRoundRect(borderCol, Offset(cx - 48f, cy - 22f), Size(96f, 44f), CornerRadius(12f, 12f), style = Stroke(width = if (isSelected) 4f else 2f))
        }

        SimCompType.ZENER -> {
            drawRoundRect(Color(0xFF4C0519), Offset(cx - 42f, cy - 22f), Size(84f, 44f), CornerRadius(10f, 10f))
            val tri = Path().apply {
                moveTo(cx - 16f, cy - 14f)
                lineTo(cx - 16f, cy + 14f)
                lineTo(cx + 12f, cy)
                close()
            }
            drawPath(tri, color = BrightGold)
            drawLine(PureWhite, Offset(cx + 14f, cy - 15f), Offset(cx + 14f, cy + 15f), strokeWidth = 4f)
        }

        SimCompType.NPN_BJT, SimCompType.MOSFET_N -> {
            drawCircle(color = Color(0xFF1E1B4B), radius = 30f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 30f, center = Offset(cx, cy), style = Stroke(width = if (isSelected) 4.5f else 3f))
            drawLine(BrightGold, Offset(cx - 8f, cy - 16f), Offset(cx - 8f, cy + 16f), strokeWidth = 5f)
            drawLine(PureWhite, Offset(cx - 8f, cy - 8f), Offset(cx + 14f, cy - 18f), strokeWidth = 3.5f)
            drawLine(PureWhite, Offset(cx - 8f, cy + 8f), Offset(cx + 14f, cy + 18f), strokeWidth = 3.5f)
        }

        SimCompType.GROUND -> {
            drawCircle(color = Color(0xFF0F172A), radius = 28f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 28f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawLine(Color(0xFF38BDF8), Offset(cx, cy - 16f), Offset(cx, cy - 2f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 18f, cy - 2f), Offset(cx + 18f, cy - 2f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 12f, cy + 6f), Offset(cx + 12f, cy + 6f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 5f, cy + 14f), Offset(cx + 5f, cy + 14f), strokeWidth = 4f)
        }

        else -> {
            drawRoundRect(SiliconDark, Offset(cx - halfW, cy - halfH), Size(halfW * 2, halfH * 2), CornerRadius(12f, 12f))
            drawRoundRect(borderCol, Offset(cx - halfW, cy - halfH), Size(halfW * 2, halfH * 2), CornerRadius(12f, 12f), style = Stroke(width = if (isSelected) 4.5f else 2.5f))
        }
    }

    drawContext.canvas.nativeCanvas.drawText(
        comp.title.take(15),
        cx,
        cy - 38f,
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 19f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
    )

    val telemetryLabel = when {
        comp.isOvercurrent -> "💥 BURNOUT!"
        comp.type == SimCompType.SCOPE_PROBE -> "🔍 KLIK LIHAT CH1/CH2"
        comp.type == SimCompType.METER_DMM -> "🔍 KLIK MODE V / mA / Ω"
        comp.type == SimCompType.LED_LAMP -> if (comp.isActiveOutput) "💡 ON (${String.format(Locale.US, "%.1fmA", comp.currentMa)})" else "OFF"
        comp.type == SimCompType.GROUND -> "0.0V (GND)"
        else -> String.format(Locale.US, "%.1fV | %.1fmA", comp.nodeVoltage, comp.currentMa)
    }

    drawContext.canvas.nativeCanvas.drawText(
        telemetryLabel,
        cx,
        cy + 48f,
        Paint().apply {
            color = if (comp.isOvercurrent) android.graphics.Color.RED else android.graphics.Color.parseColor("#FDE68A")
            textSize = 17f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
    )
}

// ============================================================================
// 3. LAYAR UTAMA SIMULATOR DENGAN POPUP INSTRUMEN SAAT ALAT UKUR DIKLIK
// ============================================================================
@Composable
fun VirtualSimLabScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val subTabs = listOf(
        Triple(0, "Breadboard Sim", Icons.Default.DeveloperBoard),
        Triple(1, "Osiloskop & FFT", Icons.Default.Timeline),
        Triple(2, "Audio & K-Map", Icons.Default.GraphicEq)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                        Icon(icon, contentDescription = null, tint = if (isSelected) BrightGold else PcbDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(title, color = if (isSelected) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> ResponsiveBreadboardSimulator()
                1 -> OscilloscopeAndSpectrumAnalyzer()
                2 -> AudioToneAndLogicGateLab()
            }
        }
    }
}

@Composable
fun ResponsiveBreadboardSimulator() {
    val context = LocalContext.current
    val clip = LocalClipboardManager.current
    val components = remember { mutableStateListOf<SimNodeComponent>() }
    val wires = remember { mutableStateListOf<SimWireConnection>() }
    val savedProjects = remember {
        mutableStateListOf<SavedCircuitProject>().apply {
            addAll(SimProjectStorage.loadAllProjects(context))
        }
    }

    var isWiringMode by remember { mutableStateOf(false) }
    var wireStartNodeId by remember { mutableStateOf<Int?>(null) }
    var selectedNodeId by remember { mutableStateOf<Int?>(6) }
    var psuVoltageSlider by remember { mutableFloatStateOf(12f) }
    var selectedPaletteCategory by remember { mutableStateOf("Alat Ukur") } // Langsung tampilkan Alat Ukur di awal!

    // Popup Modal ketika Alat Ukur di atas Breadboard Diklik!
    var activeDmmPopupComp by remember { mutableStateOf<SimNodeComponent?>(null) }
    var showDualChannelScopePopup by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "ElectronBurnAnim")
    val animFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animFactor"
    )

    fun solveCircuitPhysics() {
        val psuNode = components.find { it.type == SimCompType.DC_SOURCE }
        val vIn = psuNode?.valueNum ?: psuVoltageSlider.toDouble()

        val passiveLoadList = components.filter {
            it.type == SimCompType.RESISTOR || it.type == SimCompType.POTENTIO
        }
        val totalResistance = passiveLoadList.sumOf {
            if (it.type == SimCompType.POTENTIO) (it.valueNum.coerceIn(1.0, 100.0) / 100.0) * 10000.0
            else it.valueNum
        }.coerceAtLeast(1.0)

        val loopCurrentMa = (vIn / totalResistance) * 1000.0
        var runningVoltage = vIn
        val logicInputs = components.filter { it.type == SimCompType.LOGIC_SW }

        // Pertama hitung seluruh komponen utama
        for (i in components.indices) {
            val comp = components[i]
            when (comp.type) {
                SimCompType.DC_SOURCE, SimCompType.FUNC_GEN -> {
                    components[i] = comp.copy(nodeVoltage = vIn, currentMa = loopCurrentMa, powerMw = vIn * loopCurrentMa)
                }
                SimCompType.RESISTOR -> {
                    val vDrop = (comp.valueNum / totalResistance) * vIn
                    val displayV = if (passiveLoadList.size >= 2 && comp.id == passiveLoadList.last().id) {
                        vIn * (comp.valueNum / totalResistance)
                    } else {
                        runningVoltage
                    }
                    runningVoltage = (runningVoltage - vDrop).coerceAtLeast(0.0)
                    components[i] = comp.copy(nodeVoltage = displayV, currentMa = loopCurrentMa, powerMw = vDrop * loopCurrentMa)
                }
                SimCompType.POTENTIO -> {
                    val ratio = (comp.valueNum.coerceIn(0.0, 100.0) / 100.0)
                    val vWiper = vIn * ratio
                    components[i] = comp.copy(
                        title = "VR (${comp.valueNum.toInt()}%)",
                        nodeVoltage = vWiper,
                        currentMa = loopCurrentMa,
                        powerMw = vWiper * loopCurrentMa
                    )
                }
                SimCompType.LED_LAMP, SimCompType.BUZZER, SimCompType.SEVEN_SEG -> {
                    val connected = wires.any { it.toId == comp.id || it.fromId == comp.id }
                    val isOn = vIn >= 1.8 && connected
                    val iMa = if (passiveLoadList.isEmpty()) 110.0 else ((vIn - 2.0).coerceAtLeast(0.0) / totalResistance) * 1000.0
                    components[i] = comp.copy(
                        nodeVoltage = if (isOn) 2.1 else 0.0,
                        currentMa = if (isOn) iMa else 0.0,
                        powerMw = if (isOn) 2.1 * iMa else 0.0,
                        isActiveOutput = isOn,
                        isOvercurrent = isOn && iMa > 35.0
                    )
                }
                SimCompType.LOGIC_SW -> {
                    val isHigh = comp.valueNum >= 0.5
                    components[i] = comp.copy(
                        title = if (isHigh) "SW (HIGH 1)" else "SW (LOW 0)",
                        nodeVoltage = if (isHigh) 5.0 else 0.0,
                        currentMa = 0.0,
                        isActiveOutput = isHigh
                    )
                }
                SimCompType.GATE_AND -> {
                    val outHigh = logicInputs.isNotEmpty() && logicInputs.all { it.valueNum >= 0.5 }
                    components[i] = comp.copy(nodeVoltage = if (outHigh) 5.0 else 0.0, currentMa = 8.0, isActiveOutput = outHigh)
                }
                SimCompType.GATE_OR -> {
                    val outHigh = logicInputs.any { it.valueNum >= 0.5 }
                    components[i] = comp.copy(nodeVoltage = if (outHigh) 5.0 else 0.0, currentMa = 8.0, isActiveOutput = outHigh)
                }
                SimCompType.GATE_NAND -> {
                    val outHigh = !(logicInputs.isNotEmpty() && logicInputs.all { it.valueNum >= 0.5 })
                    components[i] = comp.copy(nodeVoltage = if (outHigh) 5.0 else 0.0, currentMa = 8.0, isActiveOutput = outHigh)
                }
                SimCompType.GATE_XOR -> {
                    val outHigh = (logicInputs.count { it.valueNum >= 0.5 } % 2) == 1
                    components[i] = comp.copy(nodeVoltage = if (outHigh) 5.0 else 0.0, currentMa = 8.0, isActiveOutput = outHigh)
                }
                else -> {}
            }
        }

        // Kedua: Perbarui pembacaan semua ALAT UKUR (DMM, Osiloskop, Wattmeter, Logic Probe) berdasarkan komponen yang terhubung kabel dengannya!
        for (i in components.indices) {
            val comp = components[i]
            if (comp.type in listOf(SimCompType.METER_DMM, SimCompType.SCOPE_PROBE, SimCompType.WATTMETER, SimCompType.LOGIC_PROBE)) {
                val connectedNodeId = wires.firstOrNull { it.fromId == comp.id }?.toId
                    ?: wires.firstOrNull { it.toId == comp.id }?.fromId
                val targetComp = components.find { it.id == connectedNodeId }
                    ?: components.firstOrNull { it.type == SimCompType.RESISTOR }

                val measuredV = targetComp?.nodeVoltage ?: (vIn * 0.66)
                val measuredI = targetComp?.currentMa ?: loopCurrentMa
                components[i] = comp.copy(
                    nodeVoltage = measuredV,
                    currentMa = measuredI,
                    powerMw = measuredV * measuredI,
                    isActiveOutput = true
                )
            }
        }
    }

    // Preset Default: Sudah terpasang Multimeter DMM & Osiloskop CH1/CH2 di Meja Kerja!
    fun loadVoltageDividerWithInstrumentsPreset() {
        components.clear()
        wires.clear()
        psuVoltageSlider = 12f
        components.addAll(
            listOf(
                SimNodeComponent(1, SimCompType.DC_SOURCE, "PSU 12.0V", 0.18f, 0.24f, 12.0),
                SimNodeComponent(2, SimCompType.RESISTOR, "R1 (1kΩ)", 0.48f, 0.24f, 1000.0),
                SimNodeComponent(3, SimCompType.RESISTOR, "R2 (2kΩ)", 0.48f, 0.72f, 2000.0),
                SimNodeComponent(4, SimCompType.LED_LAMP, "LED Beban", 0.80f, 0.24f, 2.0),
                SimNodeComponent(5, SimCompType.GROUND, "GND (0V)", 0.18f, 0.72f, 0.0),
                SimNodeComponent(6, SimCompType.METER_DMM, "DMM Vout", 0.80f, 0.52f, 0.0),
                SimNodeComponent(7, SimCompType.SCOPE_PROBE, "Osiloskop CH1/2", 0.80f, 0.78f, 50.0)
            )
        )
        wires.addAll(
            listOf(
                SimWireConnection(1, 2, 0xFFEF4444),
                SimWireConnection(2, 3, 0xFFF59E0B),
                SimWireConnection(3, 5, 0xFF38BDF8),
                SimWireConnection(2, 4, 0xFF10B981),
                SimWireConnection(3, 6, 0xFFFACC15), // Kabel Probe Multimeter ke R2
                SimWireConnection(3, 7, 0xFF06B6D4)  // Kabel Probe Osiloskop ke R2
            )
        )
        selectedNodeId = 6
        solveCircuitPhysics()
    }

    LaunchedEffect(Unit) {
        if (components.isEmpty()) {
            loadVoltageDividerWithInstrumentsPreset()
        }
    }

    val probedComp = components.find { it.id == selectedNodeId } ?: components.firstOrNull()
    val categories = listOf("Alat Ukur", "Semua", "Sumber", "Pasif", "Semikonduktor", "IC & Aktuator", "Digital")
    val filteredCatalog = remember(selectedPaletteCategory) {
        SimCompType.entries.filter { selectedPaletteCategory == "Semua" || it.category == selectedPaletteCategory }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. PANEL ATAS MULTIMETER & TOMBOL PROYEK
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
                                text = "📟 Multimeter & Instrument Bench",
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Target Ukur: ${probedComp?.title ?: "-"} (Ketuk DMM/OSC di kanvas!)",
                                color = SoftTraceBorder,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = PcbEmerald,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BrightGold),
                                modifier = Modifier.clickable { showSaveDialog = true }
                            ) {
                                Text("💾 Simpan", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                            Surface(
                                color = CopperGold,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showLoadDialog = true }
                            ) {
                                Text("📂 Buka (${savedProjects.size})", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showCodeDialog = true }
                            ) {
                                Text("🤖 .INO", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LcdMetricBox("TEGANGAN (V)", String.format(Locale.US, "%.2f V", probedComp?.nodeVoltage ?: 0.0), BrightGold, Modifier.weight(1f))
                        LcdMetricBox("ARUS (I)", String.format(Locale.US, "%.2f mA", probedComp?.currentMa ?: 0.0), PureWhite, Modifier.weight(1f))
                        LcdMetricBox("DAYA (P)", String.format(Locale.US, "%.1f mW", probedComp?.powerMw ?: 0.0), Color(0xFF4ADE80), Modifier.weight(1f))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(String.format(Locale.US, "⚡ Tegangan Sumber PSU (Vin): %.1f Volt", psuVoltageSlider), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = psuVoltageSlider,
                            onValueChange = { newV ->
                                psuVoltageSlider = newV
                                val idx = components.indexOfFirst { it.type == SimCompType.DC_SOURCE }
                                if (idx != -1) {
                                    components[idx] = components[idx].copy(
                                        title = String.format(Locale.US, "PSU %.1fV", newV),
                                        valueNum = newV.toDouble()
                                    )
                                }
                                solveCircuitPhysics()
                            },
                            valueRange = 1.5f..24f,
                            colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = PcbEmerald)
                        )
                    }
                }
            }
        }

        // 2. PALET ALAT UKUR & KOMPONEN YANG BISA DITARIK KE KANVAS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isWiringMode = false; wireStartNodeId = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (!isWiringMode) PcbDarkGreen else SchematicIvory),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🖐️ Geser & Klik Alat Ukur", color = if (!isWiringMode) PureWhite else SiliconDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }

                        Button(
                            onClick = { isWiringMode = true; wireStartNodeId = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isWiringMode) LogicHighRed else SchematicIvory),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(if (isWiringMode) "🔌 Klik 2 Titik!" else "🔌 Pasang Kabel Probe", color = if (isWiringMode) PureWhite else SiliconDark, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedPaletteCategory == cat,
                                onClick = { selectedPaletteCategory = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CopperGold, selectedLabelColor = PureWhite)
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredCatalog) { type ->
                            Surface(
                                color = SchematicIvory,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, type.badgeColor),
                                modifier = Modifier.clickable {
                                    val newId = (components.maxOfOrNull { it.id } ?: 0) + 1
                                    val defVal = when (type) {
                                        SimCompType.METER_DMM -> 0.0 // Mode Volt
                                        SimCompType.DC_SOURCE -> psuVoltageSlider.toDouble()
                                        SimCompType.RESISTOR -> 1000.0
                                        SimCompType.POTENTIO -> 50.0
                                        SimCompType.ZENER -> 5.1
                                        else -> 5.0
                                    }
                                    val randX = (25..75).random() / 100f
                                    val randY = (26..74).random() / 100f
                                    components.add(
                                        SimNodeComponent(
                                            id = newId,
                                            type = type,
                                            title = "${type.shortCode}$newId",
                                            relX = randX,
                                            relY = randY,
                                            valueNum = defVal
                                        )
                                    )
                                    selectedNodeId = newId
                                    solveCircuitPhysics()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(type.badgeColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ ${type.label}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = SiliconDark, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. KANVAS BREADBOARD (KLIK MULTIMETER / OSILOSKOP UNTUK MEMBUKA INSTRUMEN!)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF061A14))
                            .pointerInput(isWiringMode) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val hitComp = components.find {
                                        val cx = it.relX * w
                                        val cy = it.relY * h
                                        hypot((cx - tapOffset.x).toDouble(), (cy - tapOffset.y).toDouble()) < 78.0
                                    }
                                    if (hitComp != null) {
                                        if (isWiringMode) {
                                            if (wireStartNodeId == null) {
                                                wireStartNodeId = hitComp.id
                                            } else if (wireStartNodeId != hitComp.id) {
                                                val palette = listOf(0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF38BDF8, 0xFFA855F7)
                                                wires.add(SimWireConnection(wireStartNodeId!!, hitComp.id, palette[wires.size % palette.size]))
                                                wireStartNodeId = null
                                                solveCircuitPhysics()
                                            }
                                        } else {
                                            selectedNodeId = hitComp.id
                                            when (hitComp.type) {
                                                // JIKA MULTIMETER DIKLIK -> BUKA DIALOG PILIH TEGANGAN / ARUS / OHM
                                                SimCompType.METER_DMM -> {
                                                    activeDmmPopupComp = hitComp
                                                }
                                                // JIKA OSILOSKOP DIKLIK -> BUKA LAYAR POPUP SINYAL CH1 & CH2!
                                                SimCompType.SCOPE_PROBE -> {
                                                    showDualChannelScopePopup = true
                                                }
                                                SimCompType.LOGIC_SW -> {
                                                    val idx = components.indexOfFirst { it.id == hitComp.id }
                                                    if (idx != -1) {
                                                        val nextVal = if (hitComp.valueNum >= 0.5) 0.0 else 1.0
                                                        components[idx] = hitComp.copy(valueNum = nextVal)
                                                        solveCircuitPhysics()
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(isWiringMode) {
                                if (!isWiringMode) {
                                    var draggedId: Int? = null
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            val hit = components.find {
                                                val cx = it.relX * w
                                                val cy = it.relY * h
                                                hypot((cx - startOffset.x).toDouble(), (cy - startOffset.y).toDouble()) < 80.0
                                            }
                                            draggedId = hit?.id
                                            if (hit != null) selectedNodeId = hit.id
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            val h = size.height.toFloat().coerceAtLeast(1f)
                                            draggedId?.let { id ->
                                                val idx = components.indexOfFirst { it.id == id }
                                                if (idx != -1) {
                                                    val old = components[idx]
                                                    val newRelX = (old.relX + (dragAmount.x / w)).coerceIn(0.14f, 0.86f)
                                                    val newRelY = (old.relY + (dragAmount.y / h)).coerceIn(0.15f, 0.82f)
                                                    components[idx] = old.copy(relX = newRelX, relY = newRelY)
                                                }
                                            }
                                        },
                                        onDragEnd = { draggedId = null }
                                    )
                                }
                            }
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height

                        val step = 42f
                        var gx = 28f
                        while (gx < canvasW) {
                            var gy = 28f
                            while (gy < canvasH) {
                                drawCircle(Color.White.copy(alpha = 0.12f), radius = 2.2f, center = Offset(gx, gy))
                                gy += step
                            }
                            gx += step
                        }

                        wires.forEach { w ->
                            val fromNode = components.find { it.id == w.fromId }
                            val toNode = components.find { it.id == w.toId }
                            if (fromNode != null && toNode != null) {
                                val fx = fromNode.relX * canvasW
                                val fy = fromNode.relY * canvasH
                                val tx = toNode.relX * canvasW
                                val ty = toNode.relY * canvasH

                                val path = Path().apply {
                                    moveTo(fx, fy)
                                    val midX = (fx + tx) / 2f
                                    cubicTo(midX, fy - 45f, midX, ty + 45f, tx, ty)
                                }
                                drawPath(path, Color(w.colorHex), style = Stroke(width = 6.5f, cap = StrokeCap.Round))

                                val deltaMa = fromNode.currentMa.coerceAtLeast(1.0)
                                val speedFactor = (deltaMa / 15.0).coerceIn(0.2, 5.0)
                                for (i in 0 until 4) {
                                    val t = (((animFactor * speedFactor + i * 25) % 100) / 100f).toFloat()
                                    val midY1 = fy - 45f
                                    val midY2 = ty + 45f
                                    val midX = (fx + tx) / 2f
                                    val dx = (1 - t).pow(3) * fx + 3 * (1 - t).pow(2) * t * midX + 3 * (1 - t) * t.pow(2) * midX + t.pow(3) * tx
                                    val dy = (1 - t).pow(3) * fy + 3 * (1 - t).pow(2) * t * midY1 + 3 * (1 - t) * t.pow(2) * midY2 + t.pow(3) * ty
                                    drawCircle(BrightGold, radius = 4f, center = Offset(dx, dy))
                                }
                            }
                        }

                        components.forEach { comp ->
                            drawRealisticComponent(
                                comp = comp,
                                cx = comp.relX * canvasW,
                                cy = comp.relY * canvasH,
                                isSelected = comp.id == selectedNodeId,
                                isWireSource = comp.id == wireStartNodeId,
                                burnPhase = animFactor / 50f
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { PresetChipButton("Preset Lengkap + DMM & Osiloskop", PcbEmerald) { loadVoltageDividerWithInstrumentsPreset() } }
                        item { PresetChipButton("Buka Layar Osiloskop CH1/CH2", Color(0xFF0284C7)) { showDualChannelScopePopup = true } }
                        item { PresetChipButton("Reset Kabel", LogicHighRed) { wires.clear(); solveCircuitPhysics() } }
                    }
                }
            }
        }

        // 4. INSPEKTOR PARAMETER KOMPONEN BAWAH
        item {
            probedComp?.let { sel ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, CopperGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔧 Parameter: ${sel.title} (${sel.type.label})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PcbDarkGreen,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    wires.removeAll { it.fromId == sel.id || it.toId == sel.id }
                                    components.remove(sel)
                                    solveCircuitPhysics()
                                }
                            ) {
                                Text("Hapus", color = LogicHighRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        if (sel.type == SimCompType.RESISTOR) {
                            Text("Pilih nilai Ohm (Warna gelang resistor & angka Multimeter DMM otomatis berubah!):", fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val rList = listOf(100.0, 220.0, 330.0, 470.0, 1000.0, 2000.0, 4700.0, 10000.0)
                                items(rList) { ohm ->
                                    FilterChip(
                                        selected = sel.valueNum == ohm,
                                        onClick = {
                                            val idx = components.indexOfFirst { it.id == sel.id }
                                            if (idx != -1) {
                                                components[idx] = sel.copy(
                                                    title = "R${sel.id} (${ElectroMath.formatResistance(ohm)})",
                                                    valueNum = ohm
                                                )
                                                solveCircuitPhysics()
                                            }
                                        },
                                        label = { Text(ElectroMath.formatResistance(ohm), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        } else if (sel.type == SimCompType.METER_DMM) {
                            Button(
                                onClick = { activeDmmPopupComp = sel },
                                colors = ButtonDefaults.buttonColors(containerColor = CopperGold)
                            ) {
                                Text("📟 Buka Pengaturan Mode Multimeter (V / mA / Ω)", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (sel.type == SimCompType.SCOPE_PROBE) {
                            Button(
                                onClick = { showDualChannelScopePopup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("📈 Buka Layar Sinyal Osiloskop CH1 & CH2", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // POPUP 1: SAAT ALAT UKUR MULTIMETER (DMM) DI KANVAS DIKLIK!
    // ========================================================================
    activeDmmPopupComp?.let { dmm ->
        val latestDmm = components.find { it.id == dmm.id } ?: dmm
        val currentMode = latestDmm.valueNum.toInt()
        val calcR = if (latestDmm.currentMa > 0) (latestDmm.nodeVoltage / (latestDmm.currentMa / 1000.0)) else 1000.0

        AlertDialog(
            onDismissRequest = { activeDmmPopupComp = null },
            containerColor = SiliconDark,
            title = {
                Text("📟 Digital Multimeter — ${latestDmm.title}", color = BrightGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, BrightGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (currentMode) {
                                    1 -> "MODE: DC CURRENT (AMPEREMETER)"
                                    2 -> "MODE: RESISTANCE (OHMMETER)"
                                    else -> "MODE: DC VOLTAGE (VOLTMETER)"
                                },
                                color = SoftTraceBorder,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (currentMode) {
                                    1 -> String.format(Locale.US, "%.2f mA", latestDmm.currentMa)
                                    2 -> ElectroMath.formatResistance(calcR)
                                    else -> String.format(Locale.US, "%.2f Volt", latestDmm.nodeVoltage)
                                },
                                color = BrightGold,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "Daya Titik Ukur: %.1f mW", latestDmm.powerMw),
                                color = PureWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text("Pilih Mode Pengukuran Multimeter:", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(0 to "Tegangan (V)", 1 to "Arus (mA)", 2 to "Hambatan (Ω)").forEach { (mIdx, mLabel) ->
                            Button(
                                onClick = {
                                    val idx = components.indexOfFirst { it.id == latestDmm.id }
                                    if (idx != -1) {
                                        components[idx] = components[idx].copy(valueNum = mIdx.toDouble())
                                        activeDmmPopupComp = components[idx]
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentMode == mIdx) CopperGold else Color(0xFF334155)
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(mLabel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeDmmPopupComp = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald)
                ) {
                    Text("Tutup Instrumen", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ========================================================================
    // POPUP 2: SAAT OSILOSKOP DI KANVAS DIKLIK -> TAMPILKAN CH1 & CH2 LIVE!
    // ========================================================================
    if (showDualChannelScopePopup) {
        var showCh1 by remember { mutableStateOf(true) }
        var showCh2 by remember { mutableStateOf(true) }
        var ch1Mode by remember { mutableStateOf(WaveShape.SINE) }
        var ch2Mode by remember { mutableStateOf(WaveShape.SQUARE) }

        val scopeNode = components.find { it.type == SimCompType.SCOPE_PROBE }
        val ch1Vpk = psuVoltageSlider
        val ch2Vpk = (scopeNode?.nodeVoltage?.toFloat() ?: (psuVoltageSlider * 0.66f)).coerceAtLeast(1f)

        AlertDialog(
            onDismissRequest = { showDualChannelScopePopup = false },
            containerColor = SiliconDark,
            title = {
                Text("📈 Dual-Channel Oscilloscope (CH1 & CH2)", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(String.format(Locale.US, "🟡 CH1 (Input): %.1f Vpp", ch1Vpk * 2), color = Color(0xFFFACC15), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(String.format(Locale.US, "🔵 CH2 (Probe): %.1f Vpp", ch2Vpk * 2), color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    // Layar Osiloskop Dual-Channel
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        for (i in 0..8) {
                            val gx = (w / 8f) * i
                            drawLine(Color.White.copy(alpha = 0.1f), Offset(gx, 0f), Offset(gx, h), 1f)
                        }
                        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, midY), Offset(w, midY), 1.5f)

                        val phase = (animFactor / 100f) * (2f * PI.toFloat()) * 4f

                        if (showCh1) {
                            val path1 = Path()
                            for (s in 0..140) {
                                val r = s / 140f
                                val x = r * w
                                val ang = (r * 3.0 * 2.0 * PI) + phase
                                val raw = if (ch1Mode == WaveShape.SINE) sin(ang).toFloat() else if (sin(ang) >= 0) 1f else -1f
                                val y = midY - raw * (h * 0.36f)
                                if (s == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                            }
                            drawPath(path1, Color(0xFFFACC15), style = Stroke(width = 4f, cap = StrokeCap.Round))
                        }

                        if (showCh2) {
                            val path2 = Path()
                            val scaleRatio = (ch2Vpk / ch1Vpk.coerceAtLeast(1f)).coerceIn(0.25f, 1.0f)
                            for (s in 0..140) {
                                val r = s / 140f
                                val x = r * w
                                val ang = (r * 3.0 * 2.0 * PI) + phase - 0.5
                                val raw = if (ch2Mode == WaveShape.SQUARE) (if (sin(ang) >= 0) 1f else -1f) else sin(ang).toFloat()
                                val y = midY - raw * (h * 0.36f * scaleRatio)
                                if (s == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                            }
                            drawPath(path2, Color(0xFF38BDF8), style = Stroke(width = 4f, cap = StrokeCap.Round))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = showCh1,
                            onClick = { showCh1 = !showCh1 },
                            label = { Text("CH1 Aktif", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = showCh2,
                            onClick = { showCh2 = !showCh2 },
                            label = { Text("CH2 Aktif", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = ch2Mode == WaveShape.SINE,
                            onClick = { ch2Mode = if (ch2Mode == WaveShape.SINE) WaveShape.SQUARE else WaveShape.SINE },
                            label = { Text("Ubah CH2 Sine/PWM", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDualChannelScopePopup = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Tutup Osiloskop", color = SiliconDark, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    // Dialog Simpan / Buka / Kode Arduino tetap tersedia
    if (showSaveDialog) {
        var projectName by remember { mutableStateOf("Rangkaian Praktikum ${savedProjects.size + 1}") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = PureWhite,
            title = { Text("💾 Simpan Proyek Rangkaian", fontWeight = FontWeight.ExtraBold, color = PcbDarkGreen, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nama Proyek Rangkaian") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val time = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                        val proj = SavedCircuitProject(
                            id = System.currentTimeMillis(),
                            name = projectName.ifBlank { "Proyek Elektronika" },
                            timestamp = time,
                            psuVoltage = psuVoltageSlider,
                            components = components.toList(),
                            wires = wires.toList()
                        )
                        savedProjects.add(0, proj)
                        SimProjectStorage.saveAllProjects(context, savedProjects)
                        showSaveDialog = false
                        Toast.makeText(context, "Proyek '${proj.name}' tersimpan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald)
                ) {
                    Text("Simpan Permanen", color = PureWhite, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            containerColor = PureWhite,
            title = { Text("📂 Daftar Proyek Tersimpan (${savedProjects.size})", fontWeight = FontWeight.ExtraBold, color = PcbDarkGreen, fontSize = 16.sp) },
            text = {
                if (savedProjects.isEmpty()) {
                    Text("Belum ada proyek yang disimpan.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(240.dp)) {
                        items(savedProjects, key = { it.id }) { proj ->
                            Card(colors = CardDefaults.cardColors(containerColor = SchematicIvory), border = BorderStroke(1.dp, SoftTraceBorder)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PcbDarkGreen)
                                        Text("${proj.components.size} Item • ${proj.timestamp}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            psuVoltageSlider = proj.psuVoltage
                                            components.clear(); components.addAll(proj.components)
                                            wires.clear(); wires.addAll(proj.wires)
                                            solveCircuitPhysics()
                                            showLoadDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen)
                                    ) {
                                        Text("Buka", color = PureWhite, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showCodeDialog) {
        val inoCode = generateArduinoCode(components)
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            containerColor = PureWhite,
            title = { Text("🤖 Auto-Gen Sketch Arduino (.ino)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PcbDarkGreen) },
            text = {
                Surface(color = SiliconDark, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(230.dp)) {
                    Text(
                        text = inoCode,
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clip.setText(AnnotatedString(inoCode))
                        Toast.makeText(context, "Kode C++ disalin!", Toast.LENGTH_SHORT).show()
                        showCodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald)
                ) {
                    Text("Copy Code", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) { Text("Tutup") }
            }
        )
    }
}

fun generateArduinoCode(components: List<SimNodeComponent>): String {
    val sb = StringBuilder()
    sb.append("// ⚡ Auto-Generated by BambuElectro Suite\n")
    sb.append("const int PIN_SENSOR_ADC = A0;\nconst int PIN_LED_OUT = 2;\n\n")
    sb.append("void setup() {\n  Serial.begin(115200);\n  pinMode(PIN_LED_OUT, OUTPUT);\n}\n\n")
    sb.append("void loop() {\n  int rawAdc = analogRead(PIN_SENSOR_ADC);\n")
    sb.append("  float voltage = rawAdc * (5.0 / 1023.0);\n")
    sb.append("  Serial.print(\"Measured Vout: \"); Serial.println(voltage);\n")
    sb.append("  digitalWrite(PIN_LED_OUT, voltage > 2.0 ? HIGH : LOW);\n")
    sb.append("  delay(250);\n}\n")
    return sb.toString()
}

@Composable
fun LcdMetricBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF052E22),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SoftTraceBorder.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = SoftTraceBorder, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

@Composable
fun PresetChipButton(text: String, bgColor: Color, onClick: () -> Unit) {
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onClick() }) {
        Text(text, color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
fun OscilloscopeAndSpectrumAnalyzer() {
    var selectedWave by remember { mutableStateOf(WaveShape.SINE) }
    var freqHz by remember { mutableFloatStateOf(50f) }
    var amplitudeVpk by remember { mutableFloatStateOf(5.0f) }
    var isRunning by remember { mutableStateOf(true) }
    var phaseAnim by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            phaseAnim = (phaseAnim + 0.14f) % (2f * PI.toFloat())
            delay(32L)
        }
    }

    val vPeakToPeak = amplitudeVpk * 2f
    val vRms = amplitudeVpk / sqrt(2.0).toFloat()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SiliconDark), shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp, BrightGold)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("📈 Digital Storage Oscilloscope & FFT", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(String.format(Locale.US, "%.0fHz | Vpp:%.1fV | Vrms:%.2fV", freqHz, vPeakToPeak, vRms), color = BrightGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(onClick = { isRunning = !isRunning }, colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) LogicHighRed else PcbEmerald)) {
                            Text(if (isRunning) "HOLD" else "RUN", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Canvas(
                        modifier = Modifier.fillMaxWidth().height(230.dp).background(Color(0xFF04130F), RoundedCornerShape(14.dp)).border(1.dp, PcbEmerald, RoundedCornerShape(14.dp)).padding(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val scopeMidY = h * 0.36f
                        val fftBottomY = h - 18f

                        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, scopeMidY), Offset(w, scopeMidY), 1.5f)
                        val wavePath = Path()
                        val cycles = (freqHz / 25f).coerceIn(1f, 10f)
                        val maxAmpPx = (h * 0.28f) * (amplitudeVpk / 12f)

                        for (s in 0..160) {
                            val ratio = s / 160f
                            val x = ratio * w
                            val angle = (ratio * cycles * 2.0 * PI) + phaseAnim
                            val rawY = when (selectedWave) {
                                WaveShape.SINE -> sin(angle).toFloat()
                                WaveShape.SQUARE -> if (sin(angle) >= 0.0) 1f else -1f
                                WaveShape.TRIANGLE -> asin(sin(angle)).toFloat() / (PI.toFloat() / 2f)
                                else -> sin(angle).toFloat() * 0.8f
                            }
                            val y = scopeMidY - (rawY * maxAmpPx)
                            if (s == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                        }
                        drawPath(wavePath, Color(0xFF34D399), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

                        drawLine(Color(0xFFD97706), Offset(0f, h * 0.68f), Offset(w, h * 0.68f), 2f)
                        val harmonics = if (selectedWave == WaveShape.SQUARE) listOf(1.0f, 0.08f, 0.35f, 0.05f, 0.22f, 0.14f) else listOf(1.0f, 0.05f, 0.03f, 0.02f, 0.01f, 0.01f)
                        val barSpacing = w / (harmonics.size + 1)
                        harmonics.forEachIndexed { idx, mag ->
                            val bx = barSpacing * (idx + 1)
                            val barH = (h * 0.21f) * mag
                            drawRoundRect(if (idx == 0) BrightGold else Color(0xFF38BDF8), Offset(bx - 14f, fftBottomY - barH), Size(28f, barH), CornerRadius(6f, 6f))
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.5.dp, SoftTraceBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WaveShape.entries) { shape ->
                            FilterChip(selected = selectedWave == shape, onClick = { selectedWave = shape }, label = { Text(shape.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                        }
                    }
                    Text(String.format(Locale.US, "Frekuensi: %.0f Hz", freqHz), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(value = freqHz, onValueChange = { freqHz = it }, valueRange = 10f..250f)
                    Text(String.format(Locale.US, "Amplitudo Peak: %.1f V", amplitudeVpk), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(value = amplitudeVpk, onValueChange = { amplitudeVpk = it }, valueRange = 1f..12f)
                }
            }
        }
    }
}

@Composable
fun AudioToneAndLogicGateLab() {
    val scope = rememberCoroutineScope()
    var audioFreqHz by remember { mutableFloatStateOf(440f) }
    var isPlayingTone by remember { mutableStateOf(false) }
    var variableCount by remember { mutableIntStateOf(3) }
    val kmapCells = remember { mutableStateMapOf<Int, Int>() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PcbDarkGreen), shape = RoundedCornerShape(18.dp), border = BorderStroke(2.dp, BrightGold)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔊 Real Audio Sine Tone Generator", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(String.format(Locale.US, "Frekuensi Output Speaker: %.0f Hz", audioFreqHz), color = BrightGold, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Slider(value = audioFreqHz, onValueChange = { audioFreqHz = it }, valueRange = 60f..4000f)
                    Button(
                        onClick = {
                            if (!isPlayingTone) {
                                isPlayingTone = true
                                scope.launch {
                                    playPureSineTone(audioFreqHz.toDouble(), 1800)
                                    isPlayingTone = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPlayingTone) "MEMANCARKAN NADA..." else "BUNYIKAN SINYAL AUDIO", color = PureWhite, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.5.dp, SoftTraceBorder)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🧮 Karnaugh Map (K-Map) Solver", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2 to "2 Var", 3 to "3 Var", 4 to "4 Var").forEach { (cnt, lbl) ->
                            FilterChip(selected = variableCount == cnt, onClick = { variableCount = cnt; kmapCells.clear() }, label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                        }
                    }
                    val rows = if (variableCount == 4) 4 else 2
                    val cols = if (variableCount >= 3) 4 else 2
                    for (r in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                val v = kmapCells[idx] ?: 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(if (v == 1) PcbEmerald else SchematicIvory, RoundedCornerShape(8.dp))
                                        .border(1.dp, SoftTraceBorder, RoundedCornerShape(8.dp))
                                        .clickable { kmapCells[idx] = if (v == 0) 1 else 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$v", color = if (v == 1) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object SimProjectStorage {
    private const val PREF_NAME = "bambuelectro_sim_projects"

    fun saveAllProjects(context: Context, list: List<SavedCircuitProject>) {
        val arr = JSONArray()
        list.forEach { proj ->
            val compArr = JSONArray()
            proj.components.forEach { c ->
                compArr.put(JSONObject().apply {
                    put("id", c.id); put("type", c.type.name); put("title", c.title)
                    put("relX", c.relX.toDouble()); put("relY", c.relY.toDouble()); put("valueNum", c.valueNum)
                })
            }
            val wireArr = JSONArray()
            proj.wires.forEach { w ->
                wireArr.put(JSONObject().apply {
                    put("fromId", w.fromId); put("toId", w.toId); put("colorHex", w.colorHex)
                })
            }
            arr.put(JSONObject().apply {
                put("id", proj.id); put("name", proj.name); put("timestamp", proj.timestamp)
                put("psuVoltage", proj.psuVoltage.toDouble()); put("components", compArr); put("wires", wireArr)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("projects_json", arr.toString()).apply()
    }

    fun loadAllProjects(context: Context): List<SavedCircuitProject> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("projects_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val compArr = o.getJSONArray("components")
                val comps = List(compArr.length()) { j ->
                    val co = compArr.getJSONObject(j)
                    val cType = runCatching { SimCompType.valueOf(co.optString("type")) }.getOrDefault(SimCompType.RESISTOR)
                    SimNodeComponent(co.optInt("id"), cType, co.optString("title"), co.optDouble("relX", 0.5).toFloat(), co.optDouble("relY", 0.5).toFloat(), co.optDouble("valueNum", 1000.0))
                }
                val wireArr = o.getJSONArray("wires")
                val wires = List(wireArr.length()) { k ->
                    val wo = wireArr.getJSONObject(k)
                    SimWireConnection(wo.optInt("fromId"), wo.optInt("toId"), wo.optLong("colorHex", 0xFFF59E0B))
                }
                SavedCircuitProject(o.optLong("id"), o.optString("name"), o.optString("timestamp"), o.optDouble("psuVoltage", 12.0).toFloat(), comps, wires)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

suspend fun playPureSineTone(freqHz: Double, durationMs: Int) = withContext(Dispatchers.Default) {
    try {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate) / 1000
        val generatedSnd = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val sample = sin(2.0 * PI * i / (sampleRate / freqHz))
            generatedSnd[i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(generatedSnd.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
        delay(durationMs.toLong())
        audioTrack.stop()
        audioTrack.release()
    } catch (_: Exception) {}
}