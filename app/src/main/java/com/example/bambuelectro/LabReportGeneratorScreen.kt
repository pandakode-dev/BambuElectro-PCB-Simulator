package com.example.bambuelectro

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class LabMeasurementRow(
    val testPointName: String,
    val unit: String,
    val theoryValue: Double,
    val measuredValue: Double
) {
    val errorPercent: Double
        get() = if (abs(theoryValue) > 1e-6) (abs(theoryValue - measuredValue) / abs(theoryValue)) * 100.0 else 0.0
}

@Composable
fun LabReportGeneratorScreen() {
    val context = LocalContext.current
    var studentName by remember { mutableStateOf("Muhammad Saputra") }
    var studentIdNim by remember { mutableStateOf("PCR-MTTK-2026") }
    var labCourseTitle by remember { mutableStateOf("Praktikum Rangkaian Elektronika & Instrumentasi") }
    var conclusionNotes by remember { mutableStateOf("Hasil pengukuran praktikum memiliki rata-rata galat di bawah 3% dan sesuai dengan hukum pembagi tegangan.") }

    var newPointName by remember { mutableStateOf("Titik Vout R2") }
    var newUnit by remember { mutableStateOf("Volt") }
    var newTheoryStr by remember { mutableStateOf("8.00") }
    var newMeasuredStr by remember { mutableStateOf("7.88") }

    val rows = remember {
        mutableStateListOf(
            LabMeasurementRow("Tegangan Sumber (Vin)", "Volt", 12.00, 11.96),
            LabMeasurementRow("Tegangan R1 (VR1)", "Volt", 4.00, 3.95),
            LabMeasurementRow("Tegangan R2 (Vout)", "Volt", 8.00, 7.92),
            LabMeasurementRow("Arus Loop Rangkaian (I)", "mA", 4.00, 3.91)
        )
    }

    val avgErrorPct = if (rows.isNotEmpty()) rows.map { it.errorPercent }.average() else 0.0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KARTU 1: IDENTITAS PRAKTIKAN & TOMBOL CETAK PDF RESMI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, BrightGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📝 Generator Jobsheet & Laporan Praktikum (.PDF)",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = String.format(Locale.US, "Rata-rata Galat Percobaan: %.2f %%  |  Akurasi: %.2f %%", avgErrorPct, (100.0 - avgErrorPct).coerceIn(0.0, 100.0)),
                        color = BrightGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )

                    Button(
                        onClick = {
                            generateAndShareLabPdf(
                                context = context,
                                studentName = studentName,
                                nim = studentIdNim,
                                title = labCourseTitle,
                                conclusion = conclusionNotes,
                                rows = rows
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CETAK & BAGIKAN LAPORAN PRAKTIKUM (.PDF)", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // KARTU 2: FORM IDENTITAS MAHASISWA / PENELITI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👤 Identitas Laporan Laboratorium", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Nama Praktikan / Peneliti") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = studentIdNim, onValueChange = { studentIdNim = it }, label = { Text("NIM / ID Lab") }, modifier = Modifier.weight(0.8f), singleLine = true)
                    }
                    OutlinedTextField(value = labCourseTitle, onValueChange = { labCourseTitle = it }, label = { Text("Judul Modul Praktikum") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = conclusionNotes, onValueChange = { conclusionNotes = it }, label = { Text("Kesimpulan Analisis Percobaan") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // KARTU 3: TAMBAH BARIS PENGUKURAN TEORI vs PRAKTEK
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, CopperGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("➕ Tambah Data Tabel Pengukuran", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newPointName, onValueChange = { newPointName = it }, label = { Text("Parameter Ukur") }, modifier = Modifier.weight(1.2f), singleLine = true)
                        OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Satuan") }, modifier = Modifier.weight(0.7f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newTheoryStr, onValueChange = { newTheoryStr = it }, label = { Text("Nilai Teori") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = newMeasuredStr, onValueChange = { newMeasuredStr = it }, label = { Text("Nilai Ukur") }, modifier = Modifier.weight(1f), singleLine = true)
                        Button(
                            onClick = {
                                val tVal = newTheoryStr.toDoubleOrNull() ?: 0.0
                                val mVal = newMeasuredStr.toDoubleOrNull() ?: 0.0
                                rows.add(LabMeasurementRow(newPointName.ifBlank { "Titik Ukur" }, newUnit, tVal, mVal))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Tabel", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // DAFTAR TABEL PENGUKURAN BESERTA PERSENTASE ERROR OTOMATIS
        itemsIndexed(rows) { idx, item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${idx + 1}. ${item.testPointName}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SiliconDark)
                        Text(
                            text = String.format(Locale.US, "Teori: %.2f %s  |  Ukur: %.2f %s", item.theoryValue, item.unit, item.measuredValue, item.unit),
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                    Surface(
                        color = if (item.errorPercent <= 5.0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "Error: %.2f%%", item.errorPercent),
                            color = if (item.errorPercent <= 5.0) PcbDarkGreen else LogicHighRed,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = { rows.removeAt(idx) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LogicHighRed)
                    }
                }
            }
        }
    }
}

// ============================================================================
// ENGINE GENERATOR DOKUMEN PDF A4 RESMI (MENGGUNAKAN NATIVE PDFDOCUMENT ANDROID)
// ============================================================================
fun generateAndShareLabPdf(
    context: Context,
    studentName: String,
    nim: String,
    title: String,
    conclusion: String,
    rows: List<LabMeasurementRow>
) {
    try {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4 Standar
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Kop Laporan Hijau PCB & Emas
        paint.color = AndroidColor.parseColor("#064E3B")
        canvas.drawRect(0f, 0f, 595f, 92f, paint)
        paint.color = AndroidColor.parseColor("#F59E0B")
        canvas.drawRect(0f, 88f, 595f, 94f, paint)

        paint.color = AndroidColor.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("BAMBUELECTRO — LAPORAN PRAKTIKUM LABORATORIUM", 32f, 42f, paint)

        paint.color = AndroidColor.parseColor("#A7F3D0")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        canvas.drawText("Dicetak otomatis melalui BambuElectro Engineering Suite • Tanggal: $dateStr", 32f, 68f, paint)

        // Informasi Mahasiswa
        var y = 130f
        paint.color = AndroidColor.parseColor("#1E293B")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Judul Percobaan : $title", 32f, y, paint)
        y += 22f
        canvas.drawText("Nama Praktikan  : $studentName", 32f, y, paint)
        canvas.drawText("NIM / ID : $nim", 350f, y, paint)

        // Header Tabel Pengukuran
        y += 30f
        paint.color = AndroidColor.parseColor("#047857")
        canvas.drawRect(32f, y, 563f, y + 28f, paint)
        paint.color = AndroidColor.WHITE
        paint.textSize = 11f
        canvas.drawText("No", 42f, y + 18f, paint)
        canvas.drawText("Parameter Titik Ukur", 80f, y + 18f, paint)
        canvas.drawText("Nilai Teori", 280f, y + 18f, paint)
        canvas.drawText("Nilai Ukur", 380f, y + 18f, paint)
        canvas.drawText("% Galat (Error)", 475f, y + 18f, paint)

        y += 28f
        paint.isFakeBoldText = false
        rows.forEachIndexed { i, r ->
            paint.color = if (i % 2 == 0) AndroidColor.parseColor("#F8FAFC") else AndroidColor.parseColor("#ECFDF5")
            canvas.drawRect(32f, y, 563f, y + 26f, paint)

            paint.color = AndroidColor.parseColor("#1E293B")
            canvas.drawText("${i + 1}", 42f, y + 17f, paint)
            canvas.drawText(r.testPointName.take(28), 80f, y + 17f, paint)
            canvas.drawText(String.format(Locale.US, "%.2f %s", r.theoryValue, r.unit), 280f, y + 17f, paint)
            canvas.drawText(String.format(Locale.US, "%.2f %s", r.measuredValue, r.unit), 380f, y + 17f, paint)

            paint.isFakeBoldText = true
            canvas.drawText(String.format(Locale.US, "%.2f %%", r.errorPercent), 475f, y + 17f, paint)
            paint.isFakeBoldText = false
            y += 26f
        }

        // Kesimpulan & Rata-rata Error
        y += 32f
        val avgErr = if (rows.isNotEmpty()) rows.map { it.errorPercent }.average() else 0.0
        paint.isFakeBoldText = true
        paint.textSize = 13f
        paint.color = AndroidColor.parseColor("#064E3B")
        canvas.drawText(String.format(Locale.US, "Rata-Rata Galat Pengukuran: %.2f %% (Akurasi: %.2f %%)", avgErr, (100.0 - avgErr).coerceIn(0.0, 100.0)), 32f, y, paint)

        y += 28f
        canvas.drawText("Kesimpulan Analisis:", 32f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 11.5f
        paint.color = AndroidColor.DKGRAY
        canvas.drawText(conclusion.take(95), 32f, y, paint)
        if (conclusion.length > 95) {
            canvas.drawText(conclusion.substring(95).take(95), 32f, y + 18f, paint)
        }

        // Footer Validasi PandaKode
        paint.color = AndroidColor.GRAY
        paint.textSize = 10f
        canvas.drawText("Dokumen ini diverifikasi secara digital oleh BambuElectro Engineering Suite • PandaKode", 32f, 810f, paint)

        pdfDoc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(dir, "Laporan_Praktikum_BambuElectro_${System.currentTimeMillis() % 10000}.pdf")
        FileOutputStream(pdfFile).use { pdfDoc.writeTo(it) }
        pdfDoc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Praktikum - $studentName ($nim)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF ke..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}