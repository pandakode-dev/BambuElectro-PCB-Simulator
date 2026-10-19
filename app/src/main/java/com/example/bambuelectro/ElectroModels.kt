package com.example.bambuelectro

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow

// ============================================================================
// 1. PALET WARNA TEMA PCB: SOLDER MASK EMERALD, COPPER GOLD & SILICON SLATE
// ============================================================================
val PcbDarkGreen = Color(0xFF064E3B)      // Hijau Gelap Papan PCB FR-4
val PcbEmerald = Color(0xFF047857)        // Hijau Solder Mask Aktif
val CopperGold = Color(0xFFD97706)        // Kuning Tembaga Pad/Via PCB
val BrightGold = Color(0xFFF59E0B)        // Emas Pin Konektor ENIG
val SiliconDark = Color(0xFF1E293B)       // Hitam Abu Bodi IC Mikrokontroler
val SchematicIvory = Color(0xFFFAF9F5)    // Kertas Skematik Putih Hangat
val SoftTraceBorder = Color(0xFFA7F3D0)   // Garis Tepi Kartu Hijau Mint
val PureWhite = Color(0xFFFFFFFF)
val LogicHighRed = Color(0xFFEF4444)      // Indikator VCC / High (5V/3.3V)
val LogicLowBlue = Color(0xFF0284C7)      // Indikator GND / Sinyal

val BambuElectroColorScheme = lightColorScheme(
    primary = PcbEmerald,
    onPrimary = PureWhite,
    primaryContainer = SoftTraceBorder,
    onPrimaryContainer = PcbDarkGreen,
    secondary = CopperGold,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = SiliconDark,
    background = SchematicIvory,
    onBackground = SiliconDark,
    surface = PureWhite,
    onSurface = SiliconDark
)

// ============================================================================
// 2. DATA MODEL GELANG WARNA RESISTOR, PINOUT IC & BOM PROYEK
// ============================================================================
data class ResistorColorBand(
    val name: String,
    val colorHex: Color,
    val digit: Int?,          // 0 - 9
    val multiplier: Double,   // Pengali Ohm
    val tolerance: String?    // ±1%, ±5%, dll.
)

val StandardResistorBands = listOf(
    ResistorColorBand("Hitam (0)", Color(0xFF18181B), 0, 1.0, null),
    ResistorColorBand("Coklat (1)", Color(0xFF78350F), 1, 10.0, "±1% (F)"),
    ResistorColorBand("Merah (2)", Color(0xFFDC2626), 2, 100.0, "±2% (G)"),
    ResistorColorBand("Oranye (3)", Color(0xFFF97316), 3, 1_000.0, null),
    ResistorColorBand("Kuning (4)", Color(0xFFEAB308), 4, 10_000.0, null),
    ResistorColorBand("Hijau (5)", Color(0xFF16A34A), 5, 100_000.0, "±0.5% (D)"),
    ResistorColorBand("Biru (6)", Color(0xFF2563EB), 6, 1_000_000.0, "±0.25% (C)"),
    ResistorColorBand("Ungu (7)", Color(0xFF9333EA), 7, 10_000_000.0, "±0.1% (B)"),
    ResistorColorBand("Abu-abu (8)", Color(0xFF64748B), 8, 100_000_000.0, null),
    ResistorColorBand("Putih (9)", Color(0xFFF8FAFC), 9, 1_000_000_000.0, null),
    ResistorColorBand("Emas (×0.1)", Color(0xFFD97706), null, 0.1, "±5% (J)"),
    ResistorColorBand("Perak (×0.01)", Color(0xFF94A3B8), null, 0.01, "±10% (K)")
)

data class PinItem(
    val pinNumber: Int,
    val pinName: String,
    val functionDesc: String,
    val type: String // "POWER", "GND", "GPIO", "ADC", "COM"
)

data class HardwareBoardPinout(
    val id: String,
    val title: String,
    val category: String,
    val specs: String,
    val leftPins: List<PinItem>,
    val rightPins: List<PinItem>
)

data class BomComponentItem(
    val id: Long = System.currentTimeMillis(),
    val partName: String,
    val category: String, // Resistor, IC, Sensor, Modul, PCB
    val footprintOrSpec: String,
    val qty: Int,
    val unitPriceIdr: Long
)

// ============================================================================
// 3. ENGINE RUMUS FISIKA ELEKTRONIKA, SMD EIA-96 & IPC-2221 PCB TRACE
// ============================================================================
object ElectroMath {

    fun formatResistance(ohms: Double): String {
        return when {
            ohms >= 1_000_000.0 -> String.format(Locale.US, "%.2f MΩ", ohms / 1_000_000.0).replace(".00", "")
            ohms >= 1_000.0 -> String.format(Locale.US, "%.2f kΩ", ohms / 1_000.0).replace(".00", "")
            ohms < 1.0 -> String.format(Locale.US, "%.2f Ω", ohms)
            else -> String.format(Locale.US, "%.1f Ω", ohms).replace(".0", "")
        }
    }

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp ${formatter.format(amount)}"
    }

    // Dekoder Kode Resistor SMD (Mendukung 3-Digit, 4-Digit, R-Desimal, dan EIA-96)
    fun decodeSmdResistor(rawCode: String): String {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) return "Masukkan kode SMD (Contoh: 103, 4R7, 4702, 01C)"

        // 1. Cek format R desimal (Misal: 4R7 = 4.7 Ω, R22 = 0.22 Ω)
        if (code.contains("R")) {
            val replaced = code.replace("R", ".")
            val valOhm = replaced.toDoubleOrNull()
            if (valOhm != null) return "${formatResistance(valOhm)} (Presisi / Shunt)"
        }

        // 2. Cek standar EIA-96 (2 Angka + 1 Huruf, misal 01C = 10kΩ ±1%)
        val eia96Values = listOf(
            100,102,105,107,110,113,115,118,121,124,127,130,133,137,140,143,
            147,150,154,158,162,165,169,174,178,182,187,191,196,200,205,210,
            215,221,226,232,237,243,249,255,261,267,274,280,287,294,301,309,
            316,324,332,340,348,357,365,374,383,392,402,412,422,432,442,453,
            464,475,487,499,511,523,536,549,562,576,590,604,619,634,649,665,
            681,698,715,732,750,768,787,806,825,845,866,887,909,931,953,976
        )
        if (code.length == 3 && code.substring(0, 2).all { it.isDigit() } && code[2].isLetter()) {
            val idx = code.substring(0, 2).toInt()
            if (idx in 1..96) {
                val baseVal = eia96Values[idx - 1].toDouble()
                val mult = when (code[2]) {
                    'Z' -> 0.001
                    'Y', 'R' -> 0.01
                    'X', 'S' -> 0.1
                    'A' -> 1.0
                    'B', 'H' -> 10.0
                    'C' -> 100.0
                    'D' -> 1_000.0
                    'E' -> 10_000.0
                    'F' -> 100_000.0
                    else -> 1.0
                }
                return "${formatResistance(baseVal * mult)} (Standar EIA-96 ±1%)"
            }
        }

        // 3. Cek 3-Digit atau 4-Digit Standar (Misal: 103 = 10kΩ, 4702 = 47kΩ)
        if (code.all { it.isDigit() } && code.length in 3..4) {
            val sigDigits = code.substring(0, code.length - 1).toDouble()
            val exp = code.last().digitToInt()
            val ohms = sigDigits * 10.0.pow(exp)
            val tol = if (code.length == 3) "±5% (3-Digit)" else "±1% (4-Digit)"
            return "${formatResistance(ohms)} $tol"
        }

        return "Format kode SMD tidak dikenali"
    }

    // Dekoder Kode Kapasitor Keramik (Misal: 104 = 100 nF = 0.1 µF = 100,000 pF)
    fun decodeCeramicCapacitor(rawCode: String): String {
        val code = rawCode.trim()
        if (code.length == 3 && code.all { it.isDigit() }) {
            val base = code.substring(0, 2).toDouble()
            val exp = code[2].digitToInt()
            val pf = base * 10.0.pow(exp)
            val nf = pf / 1_000.0
            val uf = pf / 1_000_000.0
            return String.format(Locale.US, "%.0f pF  |  %.2f nF  |  %.4f µF", pf, nf, uf)
        }
        return "Masukkan 3 angka kode kapasitor (Misal: 104, 223, 472)"
    }

    // Rumus Standar Industri IPC-2221: Lebar Jalur Tembaga PCB (External Layer)
    // Area (mils^2) = (I / (k * (dT^b))) ^ (1/c), dengan k=0.048, b=0.44, c=0.725
    fun calculatePcbTraceWidthMm(currentAmp: Double, tempRiseC: Double, copperOz: Double): Pair<Double, Double> {
        val k = 0.048
        val b = 0.44
        val c = 0.725
        val safeI = currentAmp.coerceAtLeast(0.1)
        val safeDt = tempRiseC.coerceAtLeast(5.0)
        val thicknessMils = copperOz.coerceAtLeast(0.5) * 1.378

        val areaMils2 = (safeI / (k * safeDt.pow(b))).pow(1.0 / c)
        val widthMils = areaMils2 / thicknessMils
        val widthMm = widthMils * 0.0254
        return Pair(widthMm, widthMils)
    }

    // Rumus Frekuensi Cut-Off Filter RC (f_c = 1 / (2 * pi * R * C))
    fun calculateRcCutoffHz(resistorOhm: Double, capUf: Double): Double {
        if (resistorOhm <= 0.0 || capUf <= 0.0) return 0.0
        val capFarad = capUf * 1e-6
        return 1.0 / (2.0 * PI * resistorOhm * capFarad)
    }
}