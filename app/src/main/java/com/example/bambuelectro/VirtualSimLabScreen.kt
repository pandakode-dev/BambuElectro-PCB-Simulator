package com.example.bambuelectro

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.Toast
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// 1. KATALOG 19 KOMPONEN ELEKTRONIKA & GERBANG LOGIKA STANDAR IEEE
// ============================================================================
enum class SimCompType(val label: String, val shortCode: String, val badgeColor: Color, val category: String) {
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
    val valueNum: Double,
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
// 2. KONVERTER NILAI OHM KE WARNA GELANG RESISTOR OTOMATIS (REAL-TIME BANDS)
// ============================================================================
fun ohmValueToBandColors(ohms: Double): List<Color> {
    val digitColors = listOf(
        Color(0xFF18181B), // 0: Hitam
        Color(0xFF78350F), // 1: Coklat
        Color(0xFFDC2626), // 2: Merah
        Color(0xFFF97316), // 3: Oranye
        Color(0xFFEAB308), // 4: Kuning
        Color(0xFF16A34A), // 5: Hijau
        Color(0xFF2563EB), // 6: Biru
        Color(0xFF9333EA), // 7: Ungu
        Color(0xFF64748B), // 8: Abu-abu
        Color(0xFFF8FAFC)  // 9: Putih
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

// ============================================================================
// 3. ENGINE PENGGAMBAR BENTUK ASLI KOMPONEN ELEKTRONIKA & GERBANG LOGIKA
// ============================================================================
fun DrawScope.drawRealisticComponent(
    comp: SimNodeComponent,
    cx: Float,
    cy: Float,
    isSelected: Boolean,
    isWireSource: Boolean
) {
    val halfW = 58f
    val halfH = 28f
    val borderCol = when {
        isWireSource -> Color.Red
        isSelected -> BrightGold
        else -> comp.type.badgeColor
    }

    // Kawat Kaki Tembaga/Perak Kiri & Kanan
    drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(cx - halfW - 14f, cy),
        end = Offset(cx - halfW + 8f, cy),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(cx + halfW - 8f, cy),
        end = Offset(cx + halfW + 14f, cy),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Pin Titik Solder Emas Kiri & Kanan
    drawCircle(color = BrightGold, radius = 6.5f, center = Offset(cx - halfW - 12f, cy))
    drawCircle(color = BrightGold, radius = 6.5f, center = Offset(cx + halfW + 12f, cy))

    when (comp.type) {
        // A. GAMBAR FISIK RESISTOR + 4 GELANG WARNA OTOMATIS MENGIKUTI NILAI OHM!
        SimCompType.RESISTOR -> {
            val rBodyW = 96f
            val rBodyH = 38f
            val left = cx - rBodyW / 2
            val top = cy - rBodyH / 2

            // Bodi Kapsul Krem Resistor Karbon
            drawRoundRect(
                color = Color(0xFFFDE68A),
                topLeft = Offset(left, top),
                size = Size(rBodyW, rBodyH),
                cornerRadius = CornerRadius(18f, 18f)
            )
            // Empat Gelang Warna Dinamis Sesuai Nilai Ohm
            val bands = ohmValueToBandColors(comp.valueNum)
            val bandStep = rBodyW / 5.2f
            bands.forEachIndexed { idx, bCol ->
                val bx = left + 14f + (idx * bandStep)
                drawRect(
                    color = bCol,
                    topLeft = Offset(bx, top),
                    size = Size(10f, rBodyH)
                )
            }
            // Garis Tepi Seleksi
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(left, top),
                size = Size(rBodyW, rBodyH),
                cornerRadius = CornerRadius(18f, 18f),
                style = Stroke(width = if (isSelected) 4.5f else 2.5f)
            )
        }

        // B. POTENSIOMETER PUTAR (KNOB & PANAH WIPER)
        SimCompType.POTENTIO -> {
            drawCircle(color = Color(0xFF1E293B), radius = 30f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 30f, center = Offset(cx, cy), style = Stroke(width = 4f))
            val angleRad = ((comp.valueNum / 100.0) * 1.5 * PI - (0.75 * PI)).toFloat()
            drawLine(
                color = BrightGold,
                start = Offset(cx, cy),
                end = Offset(cx + 22f * kotlin.math.cos(angleRad), cy + 22f * sin(angleRad)),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // C. GERBANG LOGIKA STANDAR IEEE/ANSI (AND, NAND, OR, XOR)
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

        // D. KUBAH LAMPU LED ASLI + PENDAR CAHAYA MENYALA
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

        // E. KAPASITOR (SIMBOL PELAT SEJAJAR + TABung ELIPS)
        SimCompType.CAPACITOR -> {
            drawCircle(color = Color(0xFF1E3A8A), radius = 28f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 28f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawLine(PureWhite, Offset(cx - 7f, cy - 18f), Offset(cx - 7f, cy + 18f), strokeWidth = 5f)
            drawLine(BrightGold, Offset(cx + 7f, cy - 18f), Offset(cx + 7f, cy + 18f), strokeWidth = 5f)
        }

        // F. INDUKTOR / KUMPARAN LILITAN TEMBAGA (4 LENGKUNGAN SPIRAL)
        SimCompType.INDUCTOR -> {
            drawRoundRect(
                color = Color(0xFF134E4A),
                topLeft = Offset(cx - 48f, cy - 22f),
                size = Size(96f, 44f),
                cornerRadius = CornerRadius(12f, 12f)
            )
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
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(cx - 48f, cy - 22f),
                size = Size(96f, 44f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = if (isSelected) 4f else 2f)
            )
        }

        // G. DIODA ZENER (SIMBOL SEGITIGA PN-JUNCTION + GARIS KATODA Z)
        SimCompType.ZENER -> {
            drawRoundRect(
                color = Color(0xFF4C0519),
                topLeft = Offset(cx - 42f, cy - 22f),
                size = Size(84f, 44f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            val tri = Path().apply {
                moveTo(cx - 16f, cy - 14f)
                lineTo(cx - 16f, cy + 14f)
                lineTo(cx + 12f, cy)
                close()
            }
            drawPath(tri, color = BrightGold)
            drawLine(PureWhite, Offset(cx + 14f, cy - 15f), Offset(cx + 14f, cy + 15f), strokeWidth = 4f)
            drawLine(PureWhite, Offset(cx + 8f, cy - 15f), Offset(cx + 14f, cy - 15f), strokeWidth = 3f)
            drawLine(PureWhite, Offset(cx + 14f, cy + 15f), Offset(cx + 20f, cy + 15f), strokeWidth = 3f)
        }

        // H. TRANSISTOR NPN & MOSFET (SIMBOL SKEMATIK LINGKARAN TO-92 / TO-220)
        SimCompType.NPN_BJT, SimCompType.MOSFET_N -> {
            drawCircle(color = Color(0xFF1E1B4B), radius = 30f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 30f, center = Offset(cx, cy), style = Stroke(width = if (isSelected) 4.5f else 3f))
            // Batang Basis / Gate
            drawLine(BrightGold, Offset(cx - 8f, cy - 16f), Offset(cx - 8f, cy + 16f), strokeWidth = 5f)
            // Jalur Collector & Emitter
            drawLine(PureWhite, Offset(cx - 8f, cy - 8f), Offset(cx + 14f, cy - 18f), strokeWidth = 3.5f)
            drawLine(PureWhite, Offset(cx - 8f, cy + 8f), Offset(cx + 14f, cy + 18f), strokeWidth = 3.5f)
        }

        // I. SIMBOL GROUND SKEMATIK ASLI (3 GARIS HORIZONTAL MERUNCING)
        SimCompType.GROUND -> {
            drawCircle(color = Color(0xFF0F172A), radius = 28f, center = Offset(cx, cy))
            drawCircle(color = borderCol, radius = 28f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawLine( Color(0xFF38BDF8), Offset(cx, cy - 16f), Offset(cx, cy - 2f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 18f, cy - 2f), Offset(cx + 18f, cy - 2f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 12f, cy + 6f), Offset(cx + 12f, cy + 6f), strokeWidth = 4f)
            drawLine(Color(0xFF38BDF8), Offset(cx - 5f, cy + 14f), Offset(cx + 5f, cy + 14f), strokeWidth = 4f)
        }

        // J. IC DIP-8 NE555, 7-SEGMENT DISPLAY, PSU DC & KOMPONEN LAINNYA
        else -> {
            drawRoundRect(
                color = SiliconDark,
                topLeft = Offset(cx - halfW, cy - halfH),
                size = Size(halfW * 2, halfH * 2),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Jika IC 555, gambar kaki-kaki IC di atas & bawah
            if (comp.type == SimCompType.NE555_IC) {
                for (pin in -1..1) {
                    drawRect(Color.LightGray, Offset(cx + (pin * 26f) - 4f, cy - halfH - 6f), Size(8f, 6f))
                    drawRect(Color.LightGray, Offset(cx + (pin * 26f) - 4f, cy + halfH), Size(8f, 6f))
                }
            }
            drawRoundRect(
                color = borderCol,
                topLeft = Offset(cx - halfW, cy - halfH),
                size = Size(halfW * 2, halfH * 2),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = if (isSelected) 4.5f else 2.5f)
            )
        }
    }

    // Label Nama Komponen di Atas Bodi & Nilai V/mA di Bawah Bodi
    drawContext.canvas.nativeCanvas.drawText(
        comp.title.take(14),
        cx,
        cy - 36f,
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
    )

    val telemetryLabel = when (comp.type) {
        SimCompType.LED_LAMP -> if (comp.isOvercurrent) "⚠️ OVERCURRENT" else if (comp.isActiveOutput) "💡 ON (${String.format(Locale.US, "%.1fmA", comp.currentMa)})" else "OFF"
        SimCompType.GROUND -> "0.0V (GND)"
        else -> String.format(Locale.US, "%.1fV | %.1fmA", comp.nodeVoltage, comp.currentMa)
    }

    drawContext.canvas.nativeCanvas.drawText(
        telemetryLabel,
        cx,
        cy + 46f,
        Paint().apply {
            color = android.graphics.Color.parseColor("#FDE68A")
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
    )

    // Gambar Titik Probe Merah Multimeter jika Komponen Sedang Diukur
    if (isSelected) {
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 9f,
            center = Offset(cx + halfW - 6f, cy - 38f)
        )
    }
}

// ============================================================================
// 4. LAYAR UTAMA LABORATORIUM SIMULASI (BREADBOARD, OSILOSKOP, AUDIO & LOGIKA)
// ============================================================================
@Composable
fun VirtualSimLabScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    val subTabs = listOf(
        Triple(0, "Breadboard Sim", Icons.Default.DeveloperBoard),
        Triple(1, "Osiloskop & FFT", Icons.Default.Timeline),
        Triple(2, "Audio & Logika", Icons.Default.GraphicEq)
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
    val components = remember { mutableStateListOf<SimNodeComponent>() }
    val wires = remember { mutableStateListOf<SimWireConnection>() }
    val savedProjects = remember {
        mutableStateListOf<SavedCircuitProject>().apply {
            addAll(SimProjectStorage.loadAllProjects(context))
        }
    }

    var isWiringMode by remember { mutableStateOf(false) }
    var wireStartNodeId by remember { mutableStateOf<Int?>(null) }
    var selectedNodeId by remember { mutableStateOf<Int?>(3) }
    var psuVoltageSlider by remember { mutableFloatStateOf(12f) }
    var selectedPaletteCategory by remember { mutableStateOf("Semua") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

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

        for (i in components.indices) {
            val comp = components[i]
            when (comp.type) {
                SimCompType.DC_SOURCE -> {
                    val pMw = vIn * loopCurrentMa
                    components[i] = comp.copy(nodeVoltage = vIn, currentMa = loopCurrentMa, powerMw = pMw)
                }
                SimCompType.RESISTOR -> {
                    val vDrop = (comp.valueNum / totalResistance) * vIn
                    val displayV = if (passiveLoadList.size >= 2 && comp.id == passiveLoadList.last().id) {
                        vIn * (comp.valueNum / totalResistance)
                    } else {
                        runningVoltage
                    }
                    runningVoltage = (runningVoltage - vDrop).coerceAtLeast(0.0)
                    val pMw = vDrop * loopCurrentMa
                    components[i] = comp.copy(nodeVoltage = displayV, currentMa = loopCurrentMa, powerMw = pMw)
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
                SimCompType.ZENER -> {
                    val vZener = comp.valueNum.coerceIn(3.3, 12.0)
                    val clampedV = vIn.coerceAtMost(vZener)
                    components[i] = comp.copy(
                        nodeVoltage = clampedV,
                        currentMa = loopCurrentMa,
                        powerMw = clampedV * loopCurrentMa,
                        isActiveOutput = vIn >= vZener
                    )
                }
                SimCompType.NPN_BJT, SimCompType.MOSFET_N -> {
                    val isSwitchedOn = vIn >= 3.0 && logicInputs.all { it.valueNum >= 0.5 }
                    components[i] = comp.copy(
                        nodeVoltage = if (isSwitchedOn) vIn - 0.2 else 0.0,
                        currentMa = if (isSwitchedOn) loopCurrentMa else 0.0,
                        powerMw = if (isSwitchedOn) 0.2 * loopCurrentMa else 0.0,
                        isActiveOutput = isSwitchedOn
                    )
                }
                SimCompType.NE555_IC -> {
                    components[i] = comp.copy(
                        nodeVoltage = vIn * 0.67,
                        currentMa = 10.0,
                        powerMw = vIn * 10.0,
                        isActiveOutput = true
                    )
                }
                SimCompType.LED_LAMP, SimCompType.BUZZER, SimCompType.SEVEN_SEG -> {
                    val gateComp = components.find {
                        it.type in listOf(SimCompType.GATE_AND, SimCompType.GATE_OR, SimCompType.GATE_NAND, SimCompType.GATE_XOR)
                    }
                    if (gateComp != null) {
                        val isHigh = gateComp.nodeVoltage >= 3.0
                        components[i] = comp.copy(
                            nodeVoltage = if (isHigh) 5.0 else 0.0,
                            currentMa = if (isHigh) 15.0 else 0.0,
                            powerMw = if (isHigh) 75.0 else 0.0,
                            isActiveOutput = isHigh,
                            isOvercurrent = false
                        )
                    } else {
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
                }
                SimCompType.LOGIC_SW -> {
                    val isHigh = comp.valueNum >= 0.5
                    components[i] = comp.copy(
                        title = if (isHigh) "SW (HIGH 1)" else "SW (LOW 0)",
                        nodeVoltage = if (isHigh) 5.0 else 0.0,
                        currentMa = 0.0,
                        powerMw = 0.0,
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
                SimCompType.CAPACITOR, SimCompType.INDUCTOR -> {
                    components[i] = comp.copy(nodeVoltage = vIn * 0.632, currentMa = loopCurrentMa * 0.5, powerMw = 5.0)
                }
                else -> {
                    components[i] = comp.copy(nodeVoltage = 0.0, currentMa = 0.0, powerMw = 0.0)
                }
            }
        }
    }

    fun loadVoltageDividerPreset() {
        components.clear()
        wires.clear()
        psuVoltageSlider = 12f
        components.addAll(
            listOf(
                SimNodeComponent(1, SimCompType.DC_SOURCE, "PSU 12.0V", 0.20f, 0.25f, 12.0),
                SimNodeComponent(2, SimCompType.RESISTOR, "R1 (1kΩ)", 0.50f, 0.25f, 1000.0),
                SimNodeComponent(3, SimCompType.RESISTOR, "R2 (2kΩ)", 0.50f, 0.70f, 2000.0),
                SimNodeComponent(4, SimCompType.LED_LAMP, "LED Beban", 0.80f, 0.48f, 2.0),
                SimNodeComponent(5, SimCompType.GROUND, "GND (0V)", 0.20f, 0.70f, 0.0)
            )
        )
        wires.addAll(
            listOf(
                SimWireConnection(1, 2, 0xFFEF4444),
                SimWireConnection(2, 3, 0xFFF59E0B),
                SimWireConnection(3, 5, 0xFF38BDF8),
                SimWireConnection(2, 4, 0xFF10B981),
                SimWireConnection(4, 5, 0xFF38BDF8)
            )
        )
        selectedNodeId = 3
        solveCircuitPhysics()
    }

    fun loadDigitalPreset() {
        components.clear()
        wires.clear()
        psuVoltageSlider = 5f
        components.addAll(
            listOf(
                SimNodeComponent(1, SimCompType.LOGIC_SW, "SW (HIGH 1)", 0.20f, 0.26f, 1.0),
                SimNodeComponent(2, SimCompType.LOGIC_SW, "SW (HIGH 1)", 0.20f, 0.72f, 1.0),
                SimNodeComponent(3, SimCompType.GATE_AND, "Gate AND", 0.52f, 0.48f, 5.0),
                SimNodeComponent(4, SimCompType.LED_LAMP, "LED Out", 0.82f, 0.48f, 5.0)
            )
        )
        wires.addAll(
            listOf(
                SimWireConnection(1, 3, 0xFFF59E0B),
                SimWireConnection(2, 3, 0xFF38BDF8),
                SimWireConnection(3, 4, 0xFF10B981)
            )
        )
        selectedNodeId = 3
        solveCircuitPhysics()
    }

    fun loadZenerPotentioPreset() {
        components.clear()
        wires.clear()
        psuVoltageSlider = 12f
        components.addAll(
            listOf(
                SimNodeComponent(1, SimCompType.DC_SOURCE, "PSU 12.0V", 0.18f, 0.28f, 12.0),
                SimNodeComponent(2, SimCompType.POTENTIO, "VR (65%)", 0.50f, 0.28f, 65.0),
                SimNodeComponent(3, SimCompType.ZENER, "Zener 5.1V", 0.50f, 0.70f, 5.1),
                SimNodeComponent(4, SimCompType.NPN_BJT, "Q-NPN", 0.82f, 0.50f, 5.0),
                SimNodeComponent(5, SimCompType.GROUND, "GND (0V)", 0.18f, 0.70f, 0.0)
            )
        )
        wires.addAll(
            listOf(
                SimWireConnection(1, 2, 0xFFEF4444),
                SimWireConnection(2, 3, 0xFFF59E0B),
                SimWireConnection(2, 4, 0xFF10B981),
                SimWireConnection(3, 5, 0xFF38BDF8)
            )
        )
        selectedNodeId = 2
        solveCircuitPhysics()
    }

    LaunchedEffect(Unit) {
        if (components.isEmpty()) {
            loadVoltageDividerPreset()
        }
    }

    val probedComp = components.find { it.id == selectedNodeId } ?: components.firstOrNull()
    val categories = listOf("Semua", "Sumber", "Pasif", "Semikonduktor", "IC & Aktuator", "Digital")
    val filteredCatalog = remember(selectedPaletteCategory) {
        SimCompType.entries.filter { selectedPaletteCategory == "Semua" || it.category == selectedPaletteCategory }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. PANEL MULTIMETER DIGITAL & TOMBOL SIMPAN PROYEK
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📟 Multimeter & Bench PSU",
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Probe Merah: ${probedComp?.title ?: "-"}",
                                color = SoftTraceBorder,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = PcbEmerald,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BrightGold),
                                modifier = Modifier.clickable { showSaveDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Simpan", tint = PureWhite, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simpan", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Surface(
                                color = CopperGold,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showLoadDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Buka", tint = PureWhite, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buka (${savedProjects.size})", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    exportCircuitSnapshotPng(context, components, wires, psuVoltageSlider)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Export PNG",
                                    tint = PureWhite,
                                    modifier = Modifier.padding(6.dp).size(15.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LcdMetricBox(
                            label = "TEGANGAN (Vout)",
                            value = String.format(Locale.US, "%.2f V", probedComp?.nodeVoltage ?: 0.0),
                            valueColor = BrightGold,
                            modifier = Modifier.weight(1f)
                        )
                        LcdMetricBox(
                            label = "ARUS (I)",
                            value = String.format(Locale.US, "%.2f mA", probedComp?.currentMa ?: 0.0),
                            valueColor = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        LcdMetricBox(
                            label = "DAYA (P)",
                            value = String.format(Locale.US, "%.1f mW", probedComp?.powerMw ?: 0.0),
                            valueColor = Color(0xFF4ADE80),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "⚡ Tegangan Sumber PSU (Vin): %.1f Volt", psuVoltageSlider),
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Rentang: 1.5V – 24V", color = SoftTraceBorder, fontSize = 10.sp)
                        }
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

        // 2. PALET KOMPONEN & PILIHAN MODE INTERAKSI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isWiringMode = false
                                wireStartNodeId = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isWiringMode) PcbDarkGreen else SchematicIvory
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = "🖐️ Geser & Ukur Probe",
                                color = if (!isWiringMode) PureWhite else SiliconDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                isWiringMode = true
                                wireStartNodeId = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWiringMode) LogicHighRed else SchematicIvory
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isWiringMode) "🔌 Klik 2 Komponen!" else "🔌 Pasang Kabel Jumper",
                                color = if (isWiringMode) PureWhite else SiliconDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedPaletteCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaletteCategory = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CopperGold,
                                    selectedLabelColor = PureWhite
                                )
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
                                        SimCompType.DC_SOURCE -> psuVoltageSlider.toDouble()
                                        SimCompType.RESISTOR -> 1000.0
                                        SimCompType.POTENTIO -> 50.0
                                        SimCompType.ZENER -> 5.1
                                        SimCompType.CAPACITOR -> 100.0
                                        SimCompType.LOGIC_SW -> 1.0
                                        else -> 5.0
                                    }
                                    val randX = (22..78).random() / 100f
                                    val randY = (26..72).random() / 100f
                                    val defaultTitle = if (type == SimCompType.RESISTOR) "R$newId (1kΩ)" else "${type.shortCode}$newId"
                                    components.add(
                                        SimNodeComponent(
                                            id = newId,
                                            type = type,
                                            title = defaultTitle,
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
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(type.badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ ${type.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SiliconDark,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. KANVAS BREADBOARD DENGAN VISUALISASI KOMPONEN REALISTIS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
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
                                        hypot((cx - tapOffset.x).toDouble(), (cy - tapOffset.y).toDouble()) < 75.0
                                    }
                                    if (hitComp != null) {
                                        if (isWiringMode) {
                                            if (wireStartNodeId == null) {
                                                wireStartNodeId = hitComp.id
                                            } else if (wireStartNodeId != hitComp.id) {
                                                val palette = listOf(0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF38BDF8, 0xFFA855F7)
                                                wires.add(
                                                    SimWireConnection(
                                                        fromId = wireStartNodeId!!,
                                                        toId = hitComp.id,
                                                        colorHex = palette[wires.size % palette.size]
                                                    )
                                                )
                                                wireStartNodeId = null
                                                solveCircuitPhysics()
                                            }
                                        } else {
                                            selectedNodeId = hitComp.id
                                            if (hitComp.type == SimCompType.LOGIC_SW) {
                                                val idx = components.indexOfFirst { it.id == hitComp.id }
                                                if (idx != -1) {
                                                    val nextVal = if (hitComp.valueNum >= 0.5) 0.0 else 1.0
                                                    components[idx] = hitComp.copy(valueNum = nextVal)
                                                    solveCircuitPhysics()
                                                }
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
                                                    val newRelY = (old.relY + (dragAmount.y / h)).coerceIn(0.16f, 0.80f)
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

                        // 1. Gambar Titik Lubang Breadboard
                        val step = 42f
                        var gx = 28f
                        while (gx < canvasW) {
                            var gy = 28f
                            while (gy < canvasH) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.12f),
                                    radius = 2.2f,
                                    center = Offset(gx, gy)
                                )
                                gy += step
                            }
                            gx += step
                        }

                        // 2. Gambar Kabel Jumper Melengkung (Cubic Bezier)
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
                                drawPath(
                                    path = path,
                                    color = Color(w.colorHex),
                                    style = Stroke(width = 6.5f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // 3. Panggil Engine Penggambar Bentuk Asli Komponen Elektronika!
                        components.forEach { comp ->
                            val cx = comp.relX * canvasW
                            val cy = comp.relY * canvasH
                            drawRealisticComponent(
                                comp = comp,
                                cx = cx,
                                cy = cy,
                                isSelected = comp.id == selectedNodeId,
                                isWireSource = comp.id == wireStartNodeId
                            )
                        }
                    }

                    // Baris Preset Rangkaian Cepat di Bagian Bawah Kanvas
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { PresetChipButton("Preset Pembagi Tegangan", PcbEmerald) { loadVoltageDividerPreset() } }
                        item { PresetChipButton("Preset Gerbang Logika IEEE", Color(0xFF7C3AED)) { loadDigitalPreset() } }
                        item { PresetChipButton("Preset Zener, VR & NPN", CopperGold) { loadZenerPotentioPreset() } }
                        item {
                            PresetChipButton("Reset Kabel", LogicHighRed) {
                                wires.clear()
                                solveCircuitPhysics()
                            }
                        }
                    }
                }
            }
        }

        // 4. INSPEKTOR PARAMETER KOMPONEN + PRATINJAU GELANG WARNA RESISTOR
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
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = {
                                    wires.removeAll { it.fromId == sel.id || it.toId == sel.id }
                                    components.remove(sel)
                                    solveCircuitPhysics()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Hapus", color = LogicHighRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        when (sel.type) {
                            SimCompType.RESISTOR -> {
                                Text(
                                    text = "Pilih nilai Ohm (Warna gelang resistor di kanvas atas akan otomatis berubah!):",
                                    fontSize = 11.sp,
                                    color = SiliconDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val rList = listOf(100.0, 220.0, 330.0, 470.0, 1000.0, 2000.0, 4700.0, 10000.0, 47000.0)
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
                            }
                            SimCompType.POTENTIO -> {
                                Text("Putar Knob Potensiometer (${sel.valueNum.toInt()}%):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = sel.valueNum.toFloat(),
                                    onValueChange = { pct ->
                                        val idx = components.indexOfFirst { it.id == sel.id }
                                        if (idx != -1) {
                                            components[idx] = sel.copy(valueNum = pct.toDouble())
                                            solveCircuitPhysics()
                                        }
                                    },
                                    valueRange = 1f..100f,
                                    colors = SliderDefaults.colors(thumbColor = CopperGold, activeTrackColor = PcbEmerald)
                                )
                            }
                            SimCompType.ZENER -> {
                                Text("Pilih Tegangan Breakdown Dioda Zener:", fontSize = 11.sp, color = Color.Gray)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf(3.3, 5.1, 6.2, 9.1, 12.0)) { vz ->
                                        FilterChip(
                                            selected = sel.valueNum == vz,
                                            onClick = {
                                                val idx = components.indexOfFirst { it.id == sel.id }
                                                if (idx != -1) {
                                                    components[idx] = sel.copy(title = "Zener ${vz}V", valueNum = vz)
                                                    solveCircuitPhysics()
                                                }
                                            },
                                            label = { Text("${vz}V", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "Tip: Ketuk komponen Switch (SW) untuk mengubah logika 1/0, atau pilih Resistor untuk mengganti nilai Ohm & gelang warnanya.",
                                    fontSize = 11.sp,
                                    color = SiliconDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var projectName by remember { mutableStateOf("Rangkaian Praktikum ${savedProjects.size + 1}") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = PureWhite,
            title = { Text("💾 Simpan Proyek Rangkaian", fontWeight = FontWeight.ExtraBold, color = PcbDarkGreen, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seluruh posisi komponen, nilai Ohm, dan kabel jumper akan disimpan.", fontSize = 12.sp, color = Color.DarkGray)
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Nama Proyek Rangkaian") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
                        items(savedProjects, key = { it.id }) { proj ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SchematicIvory),
                                border = BorderStroke(1.dp, SoftTraceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PcbDarkGreen)
                                        Text("${proj.components.size} Komponen • PSU ${proj.psuVoltage}V • ${proj.timestamp}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Row {
                                        Button(
                                            onClick = {
                                                psuVoltageSlider = proj.psuVoltage
                                                components.clear()
                                                components.addAll(proj.components)
                                                wires.clear()
                                                wires.addAll(proj.wires)
                                                solveCircuitPhysics()
                                                showLoadDialog = false
                                                Toast.makeText(context, "Proyek '${proj.name}' dimuat!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PcbDarkGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Buka", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = {
                                                savedProjects.remove(proj)
                                                SimProjectStorage.saveAllProjects(context, savedProjects)
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = LogicHighRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) { Text("Tutup", fontWeight = FontWeight.Bold) }
            }
        )
    }
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
            Text(
                text = label,
                fontSize = 9.sp,
                color = SoftTraceBorder,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PresetChipButton(text: String, bgColor: Color, onClick: () -> Unit) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = PureWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun OscilloscopeAndSpectrumAnalyzer() {
    var selectedWave by remember { mutableStateOf(WaveShape.SINE) }
    var freqHz by remember { mutableFloatStateOf(50f) }
    var amplitudeVpk by remember { mutableFloatStateOf(5.0f) }
    var dutyCyclePct by remember { mutableFloatStateOf(50f) }
    var isRunning by remember { mutableStateOf(true) }
    var phaseAnim by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            phaseAnim = (phaseAnim + 0.14f) % (2f * PI.toFloat())
            delay(32L)
        }
    }

    val vPeakToPeak = amplitudeVpk * 2f
    val vRms = when (selectedWave) {
        WaveShape.SINE -> amplitudeVpk / sqrt(2.0).toFloat()
        WaveShape.SQUARE -> amplitudeVpk * sqrt((dutyCyclePct / 100.0)).toFloat()
        else -> amplitudeVpk / sqrt(3.0).toFloat()
    }
    val periodMs = (1000f / freqHz.coerceAtLeast(1f))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(20.dp),
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
                                text = "📈 Digital Storage Oscilloscope & FFT",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(Locale.US, "%.0fHz | Vpp:%.1fV | Vrms:%.2fV | T:%.1fms", freqHz, vPeakToPeak, vRms, periodMs),
                                color = BrightGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { isRunning = !isRunning },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) LogicHighRed else PcbEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isRunning) "HOLD" else "RUN", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color(0xFF04130F), RoundedCornerShape(14.dp))
                            .border(1.dp, PcbEmerald, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val scopeMidY = h * 0.36f
                        val fftBottomY = h - 18f

                        for (i in 0..8) {
                            val x = (w / 8f) * i
                            drawLine(Color.White.copy(alpha = 0.1f), Offset(x, 0f), Offset(x, h), 1f)
                        }
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(0f, scopeMidY),
                            end = Offset(w, scopeMidY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        val wavePath = Path()
                        val cycles = (freqHz / 25f).coerceIn(1f, 10f)
                        val maxAmpPx = (h * 0.28f) * (amplitudeVpk / 12f)

                        val samples = 180
                        for (s in 0..samples) {
                            val ratio = s.toFloat() / samples
                            val x = ratio * w
                            val angle = (ratio * cycles * 2.0 * PI) + phaseAnim
                            val rawY = when (selectedWave) {
                                WaveShape.SINE -> sin(angle).toFloat()
                                WaveShape.SQUARE -> {
                                    val modAngle = (angle % (2.0 * PI)) / (2.0 * PI)
                                    if (modAngle < (dutyCyclePct / 100.0)) 1f else -1f
                                }
                                WaveShape.TRIANGLE -> {
                                    val p = (angle / (2.0 * PI)) % 1.0
                                    (if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)).toFloat()
                                }
                                WaveShape.SAWTOOTH -> {
                                    val p = (angle / (2.0 * PI)) % 1.0
                                    (2.0 * p - 1.0).toFloat()
                                }
                                WaveShape.RC_FILTERED -> (sin(angle) * 0.72 + sin(3 * angle) * 0.08).toFloat()
                            }
                            val y = scopeMidY - (rawY * maxAmpPx)
                            if (s == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                        }

                        drawPath(wavePath, Color(0xFF34D399), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

                        drawLine(Color(0xFFD97706), Offset(0f, h * 0.68f), Offset(w, h * 0.68f), 2f)
                        drawContext.canvas.nativeCanvas.drawText(
                            "FFT HARMONIC SPECTRUM (dBV)",
                            10f,
                            h * 0.75f,
                            Paint().apply {
                                color = android.graphics.Color.parseColor("#FDE68A")
                                textSize = 19f
                                isFakeBoldText = true
                            }
                        )

                        val harmonics = when (selectedWave) {
                            WaveShape.SINE -> listOf(1.0f, 0.04f, 0.02f, 0.01f, 0.01f, 0.01f)
                            WaveShape.SQUARE -> listOf(1.0f, 0.05f, 0.35f, 0.04f, 0.20f, 0.03f)
                            WaveShape.TRIANGLE -> listOf(1.0f, 0.02f, 0.12f, 0.01f, 0.04f, 0.01f)
                            WaveShape.SAWTOOTH -> listOf(1.0f, 0.50f, 0.33f, 0.25f, 0.20f, 0.16f)
                            WaveShape.RC_FILTERED -> listOf(0.75f, 0.03f, 0.08f, 0.01f, 0.02f, 0.01f)
                        }

                        val barSpacing = w / (harmonics.size + 1)
                        harmonics.forEachIndexed { idx, mag ->
                            val bx = barSpacing * (idx + 1)
                            val barH = (h * 0.21f) * mag * (amplitudeVpk / 12f).coerceAtLeast(0.3f)
                            drawRoundRect(
                                color = if (idx == 0) BrightGold else Color(0xFF38BDF8),
                                topLeft = Offset(bx - 14f, fftBottomY - barH),
                                size = Size(28f, barH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎛️ Function Generator Control", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WaveShape.entries) { shape ->
                            FilterChip(
                                selected = selectedWave == shape,
                                onClick = { selectedWave = shape },
                                label = { Text(shape.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Text(String.format(Locale.US, "Frekuensi: %.0f Hz", freqHz), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(value = freqHz, onValueChange = { freqHz = it }, valueRange = 10f..250f)

                    Text(String.format(Locale.US, "Amplitudo Peak: %.1f V (Vpp = %.1f V)", amplitudeVpk, vPeakToPeak), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    var selectedLogicGate by remember { mutableStateOf("AND") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🔊 Real Audio Sine Tone Generator (20 Hz – 8 kHz)",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Keluarkan sinyal frekuensi audio murni dari speaker HP untuk menguji amplifier atau resonansi akustik.",
                        color = SoftTraceBorder,
                        fontSize = 11.sp
                    )

                    Text(
                        text = String.format(Locale.US, "Frekuensi Output Speaker: %.0f Hz", audioFreqHz),
                        color = BrightGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )

                    Slider(
                        value = audioFreqHz,
                        onValueChange = { audioFreqHz = it },
                        valueRange = 60f..4000f,
                        colors = SliderDefaults.colors(thumbColor = BrightGold, activeTrackColor = CopperGold)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(100f to "100Hz Bass", 440f to "440Hz (A4)", 1000f to "1kHz Kalibrasi", 3000f to "3kHz Treble")) { (f, lbl) ->
                            SuggestionChip(
                                onClick = { audioFreqHz = f },
                                label = { Text(lbl, fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPlayingTone) LogicHighRed else CopperGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlayingTone) "SEDANG MEMANCARKAN NADA..." else "BUNYIKAN SINYAL AUDIO (1.8 DETIK)",
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🧮 Tabel Kebenaran Gerbang Logika (TTL 74xx Series)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("AND (7408)", "OR (7432)", "NAND (7400)", "NOR (7402)", "XOR (7486)")) { gate ->
                            val shortName = gate.substringBefore(" ")
                            FilterChip(
                                selected = selectedLogicGate == shortName,
                                onClick = { selectedLogicGate = shortName },
                                label = { Text(gate, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    val inputs = listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
                    Surface(color = SchematicIvory, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SoftTraceBorder)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("INPUT A", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = SiliconDark)
                                Text("INPUT B", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = SiliconDark)
                                Text("OUTPUT Y ($selectedLogicGate)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = PcbDarkGreen)
                            }
                            HorizontalDivider(color = SoftTraceBorder)
                            inputs.forEach { (a, b) ->
                                val out = when (selectedLogicGate) {
                                    "AND" -> a and b
                                    "OR" -> a or b
                                    "NAND" -> if ((a and b) == 1) 0 else 1
                                    "NOR" -> if ((a or b) == 1) 0 else 1
                                    else -> a xor b
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$a (${if (a == 1) "5V" else "0V"})", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text("$b (${if (b == 1) "5V" else "0V"})", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    Text(
                                        text = "$out (${if (out == 1) "HIGH 5V" else "LOW 0V"})",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (out == 1) PcbEmerald else LogicHighRed
                                    )
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
                    put("id", c.id)
                    put("type", c.type.name)
                    put("title", c.title)
                    put("relX", c.relX.toDouble())
                    put("relY", c.relY.toDouble())
                    put("valueNum", c.valueNum)
                })
            }
            val wireArr = JSONArray()
            proj.wires.forEach { w ->
                wireArr.put(JSONObject().apply {
                    put("fromId", w.fromId)
                    put("toId", w.toId)
                    put("colorHex", w.colorHex)
                })
            }
            arr.put(JSONObject().apply {
                put("id", proj.id)
                put("name", proj.name)
                put("timestamp", proj.timestamp)
                put("psuVoltage", proj.psuVoltage.toDouble())
                put("components", compArr)
                put("wires", wireArr)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString("projects_json", arr.toString()).apply()
    }

    fun loadAllProjects(context: Context): List<SavedCircuitProject> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("projects_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val compArr = o.getJSONArray("components")
                val comps = List(compArr.length()) { j ->
                    val co = compArr.getJSONObject(j)
                    val typeName = co.optString("type", SimCompType.RESISTOR.name)
                    val cType = runCatching { SimCompType.valueOf(typeName) }.getOrDefault(SimCompType.RESISTOR)
                    SimNodeComponent(
                        id = co.optInt("id"),
                        type = cType,
                        title = co.optString("title"),
                        relX = co.optDouble("relX", 0.5).toFloat(),
                        relY = co.optDouble("relY", 0.5).toFloat(),
                        valueNum = co.optDouble("valueNum", 1000.0)
                    )
                }
                val wireArr = o.getJSONArray("wires")
                val wires = List(wireArr.length()) { k ->
                    val wo = wireArr.getJSONObject(k)
                    SimWireConnection(
                        fromId = wo.optInt("fromId"),
                        toId = wo.optInt("toId"),
                        colorHex = wo.optLong("colorHex", 0xFFF59E0B)
                    )
                }
                SavedCircuitProject(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    timestamp = o.optString("timestamp"),
                    psuVoltage = o.optDouble("psuVoltage", 12.0).toFloat(),
                    components = comps,
                    wires = wires
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun exportCircuitSnapshotPng(
    context: Context,
    components: List<SimNodeComponent>,
    wires: List<SimWireConnection>,
    psuVolt: Float
) {
    try {
        val width = 850
        val height = 580
        val btm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(btm)
        canvas.drawColor(android.graphics.Color.parseColor("#061A14"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.parseColor("#F59E0B")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("BambuElectro Schematic Snapshot (PSU Vin: ${psuVolt}V)", 32f, 44f, paint)

        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        wires.forEach { w ->
            val f = components.find { it.id == w.fromId }
            val t = components.find { it.id == w.toId }
            if (f != null && t != null) {
                paint.color = w.colorHex.toInt()
                canvas.drawLine(f.relX * width, f.relY * height, t.relX * width, t.relY * height, paint)
            }
        }

        components.forEach { c ->
            val cx = c.relX * width
            val cy = c.relY * height
            paint.style = Paint.Style.FILL
            paint.color = if (c.type == SimCompType.RESISTOR) android.graphics.Color.parseColor("#FDE68A") else android.graphics.Color.parseColor("#1E293B")
            canvas.drawRoundRect(cx - 65f, cy - 26f, cx + 65f, cy + 26f, 16f, 16f, paint)

            if (c.type == SimCompType.RESISTOR) {
                val bands = ohmValueToBandColors(c.valueNum)
                bands.forEachIndexed { idx, col ->
                    paint.color = col.toArgb()
                    val bx = (cx - 45f) + (idx * 24f)
                    canvas.drawRect(bx, cy - 26f, bx + 10f, cy + 26f, paint)
                }
            }

            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.parseColor("#F59E0B")
            paint.strokeWidth = 3f
            canvas.drawRoundRect(cx - 65f, cy - 26f, cx + 65f, cy + 26f, 16f, 16f, paint)

            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 18f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(c.title, cx, cy - 34f, paint)

            paint.color = android.graphics.Color.parseColor("#FDE68A")
            paint.textSize = 15f
            canvas.drawText(String.format(Locale.US, "%.2fV | %.1fmA", c.nodeVoltage, c.currentMa), cx, cy + 44f, paint)
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Skematik_BambuElectro_${System.currentTimeMillis() % 10000}.png")
        FileOutputStream(file).use { btm.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Hasil Simulasi Rangkaian BambuElectro by PandaKode 🐼⚡")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Gambar Skematik ke..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor gambar: ${e.message}", Toast.LENGTH_SHORT).show()
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
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
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