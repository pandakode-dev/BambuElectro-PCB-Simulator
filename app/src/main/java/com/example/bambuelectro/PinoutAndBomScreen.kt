package com.example.bambuelectro

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun PinoutAndBomScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Visual Pinout & I2C, 1 = BOM Estimator CSV

    val bomList = remember {
        mutableStateListOf(
            BomComponentItem(1, "ESP32 DevKit V1 Wi-Fi/BT", "Mikrokontroler", "DIP-30 Dual Core", 1, 65000),
            BomComponentItem(2, "Sensor Energi PZEM-004T v3", "Sensor", "UART TTL + CT 100A", 1, 85000),
            BomComponentItem(3, "Layar OLED 0.96 Inch I2C", "Display", "SSD1306 (0x3C)", 1, 28000),
            BomComponentItem(4, "Resistor Metal Film 4.7kΩ", "Pasif", "1/4W ±1% Pull-Up", 10, 500),
            BomComponentItem(5, "Relay 2-Channel Optocoupler", "Aktuator", "5V Active Low", 1, 18000)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedSubTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 0) PcbDarkGreen else PureWhite
                ),
                border = BorderStroke(1.5.dp, if (selectedSubTab == 0) BrightGold else SoftTraceBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = if (selectedSubTab == 0) BrightGold else PcbDarkGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pinout & Alamat I2C", color = if (selectedSubTab == 0) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }

            Button(
                onClick = { selectedSubTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 1) CopperGold else PureWhite
                ),
                border = BorderStroke(1.5.dp, if (selectedSubTab == 1) PcbDarkGreen else SoftTraceBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = if (selectedSubTab == 1) PureWhite else CopperGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BOM Proyek (${bomList.size})", color = if (selectedSubTab == 1) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        if (selectedSubTab == 0) {
            VisualPinoutExplorerView()
        } else {
            BomProjectEstimatorView(bomList = bomList)
        }
    }
}

// ============================================================================
// 1. VISUAL CHIP/BOARD PINOUT EXPLORER + PERINGATAN PIN ESP32 & DIREKTORI I2C
// ============================================================================
@Composable
fun VisualPinoutExplorerView() {
    val boards = remember { getHardwareBoardsDatabase() }
    var selectedBoard by remember { mutableStateOf(boards.first()) }
    var selectedPinDetail by remember { mutableStateOf<PinItem?>(boards.first().leftPins.first()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(boards) { board ->
                    val isSelected = board.id == selectedBoard.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedBoard = board
                            selectedPinDetail = board.leftPins.firstOrNull()
                        },
                        label = { Text(board.title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PcbEmerald,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }
        }

        // Kotak Detail Pin yang Sedang Diklik
        item {
            selectedPinDetail?.let { pin ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BrightGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🔍 Inspeksi Kaki Pin #${pin.pinNumber}: ${pin.pinName} (${pin.type})",
                            color = BrightGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = pin.functionDesc,
                            color = PureWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Gambar Visual Bodi IC / Modul Kiri-Kanan Interaktif
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(selectedBoard.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbDarkGreen)
                    Text(selectedBoard.specs, fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bodi Silicon IC Hitam dengan Kaki Tembaga di Kiri & Kanan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Deretan Pin Kiri
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedBoard.leftPins.forEach { pin ->
                                PinBadgeButton(pin = pin, isLeft = true, onClick = { selectedPinDetail = pin })
                            }
                        }

                        // Bodi Tengah Chip IC
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height((selectedBoard.leftPins.size * 36).dp)
                                .background(SiliconDark, RoundedCornerShape(10.dp))
                                .border(2.dp, BrightGold, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedBoard.id.uppercase(),
                                color = SoftTraceBorder,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Deretan Pin Kanan
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedBoard.rightPins.forEach { pin ->
                                PinBadgeButton(pin = pin, isLeft = false, onClick = { selectedPinDetail = pin })
                            }
                        }
                    }
                }
            }
        }

        // Tabel Referensi Cepat Alamat Hex I2C Sensor Populer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SoftTraceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🧭 Referensi Cepat Alamat Hex I2C Sensor & Modul", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PcbDarkGreen)
                    val i2cList = listOf(
                        "0x3C / 0x3D" to "Layar OLED SSD1306 (0.96\" / 1.3\")",
                        "0x27 / 0x3F" to "LCD Karakter 16x2 / 20x4 Backpack PCF8574",
                        "0x68" to "Sensor IMU Gyro MPU6050 & Modul RTC DS3231",
                        "0x76 / 0x77" to "Sensor Suhu/Tekanan BME280 / BMP280",
                        "0x48" to "Modul Eksternal ADC 16-Bit ADS1115",
                        "0x23" to "Sensor Intensitas Cahaya Digital BH1750 (Lux)"
                    )
                    i2cList.forEach { (addr, desc) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(addr, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = CopperGold)
                            Text(desc, fontSize = 12.sp, color = SiliconDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinBadgeButton(pin: PinItem, isLeft: Boolean, onClick: () -> Unit) {
    val badgeColor = when (pin.type) {
        "POWER" -> LogicHighRed
        "GND" -> SiliconDark
        "ADC" -> CopperGold
        "COM" -> LogicLowBlue
        else -> PcbEmerald
    }
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = if (isLeft) Arrangement.SpaceBetween else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLeft) "${pin.pinNumber}. ${pin.pinName}" else pin.pinName,
                color = PureWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            if (!isLeft) {
                Text("#${pin.pinNumber}", color = SoftTraceBorder, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// 2. BILL OF MATERIALS (BOM) PROJECT ESTIMATOR & EXPORT EXCEL (.CSV)
// ============================================================================
@Composable
fun BomProjectEstimatorView(bomList: MutableList<BomComponentItem>) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    val grandTotalIdr = bomList.sumOf { it.qty * it.unitPriceIdr }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.dp, BrightGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Estimasi Anggaran BOM:", color = SoftTraceBorder, fontSize = 11.sp)
                    Text(ElectroMath.formatRupiah(grandTotalIdr), color = BrightGold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Komponen", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { exportBomToCsv(context, bomList) },
                        colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Export CSV", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(bomList, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SoftTraceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.partName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = SiliconDark)
                            Text("${item.category} • ${item.footprintOrSpec}", fontSize = 11.sp, color = Color.Gray)
                            Text("${item.qty} pcs × ${ElectroMath.formatRupiah(item.unitPriceIdr)} = ${ElectroMath.formatRupiah(item.qty * item.unitPriceIdr)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PcbEmerald)
                        }
                        IconButton(onClick = { bomList.remove(item) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LogicHighRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var spec by remember { mutableStateOf("DIP / SMD 0805") }
        var qtyStr by remember { mutableStateOf("1") }
        var priceStr by remember { mutableStateOf("15000") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = PureWhite,
            title = { Text("Tambah Komponen ke Daftar BOM", fontWeight = FontWeight.ExtraBold, color = PcbDarkGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Komponen / IC / Sensor") }, singleLine = true)
                    OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Spesifikasi / Footprint Package") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it.filter { c -> c.isDigit() } }, label = { Text("Jumlah (Qty)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { c -> c.isDigit() } }, label = { Text("Harga Satuan (Rp)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            bomList.add(
                                0,
                                BomComponentItem(
                                    partName = name,
                                    category = "Komponen",
                                    footprintOrSpec = spec,
                                    qty = (qtyStr.toIntOrNull() ?: 1).coerceAtLeast(1),
                                    unitPriceIdr = priceStr.toLongOrNull() ?: 0L
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmerald)
                ) {
                    Text("Simpan", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }
}

fun getHardwareBoardsDatabase(): List<HardwareBoardPinout> {
    return listOf(
        HardwareBoardPinout(
            id = "ESP32",
            title = "ESP32 DevKit V1 (30-Pin)",
            category = "IoT MCU",
            specs = "Dual-Core 240MHz • 3.3V Logic • Wi-Fi & BLE (Klik Pin untuk Detail!)",
            leftPins = listOf(
                PinItem(1, "EN (RST)", "Enable / Reset Chip (Pull-Up internal)", "POWER"),
                PinItem(2, "GPIO36 (VP)", "Input Only • ADC1_CH0 (Aman dipakai bersama Wi-Fi)", "ADC"),
                PinItem(3, "GPIO39 (VN)", "Input Only • ADC1_CH3 (Aman dipakai bersama Wi-Fi)", "ADC"),
                PinItem(4, "GPIO34", "Input Only • ADC1_CH6 (Tidak ada internal Pull-Up)", "ADC"),
                PinItem(5, "GPIO35", "Input Only • ADC1_CH7 (Cocok untuk sensor analog)", "ADC"),
                PinItem(6, "GPIO32", "ADC1_CH4 • Touch9 • PWM & I/O Serbaguna", "GPIO"),
                PinItem(7, "GPIO33", "ADC1_CH5 • Touch8 • PWM & I/O Serbaguna", "GPIO"),
                PinItem(8, "GPIO25", "DAC_1 (Output Analog Murni) • ADC2_CH8", "ADC"),
                PinItem(9, "GND", "Ground (0V Reference)", "GND"),
                PinItem(10, "VIN (5V)", "Input Catu Daya 5V dari USB / Eksternal Regulator", "POWER")
            ),
            rightPins = listOf(
                PinItem(11, "GPIO23", "VSPI MOSI • Pin Digital PWM Aman", "COM"),
                PinItem(12, "GPIO22", "I2C SCL Default (Layar OLED / LCD / RTC)", "COM"),
                PinItem(13, "GPIO1 (TX0)", "UART0 TX (Serial Monitor Programming)", "COM"),
                PinItem(14, "GPIO3 (RX0)", "UART0 RX (Serial Monitor Programming)", "COM"),
                PinItem(15, "GPIO21", "I2C SDA Default (Layar OLED / LCD / RTC)", "COM"),
                PinItem(16, "GPIO19", "VSPI MISO • Digital I/O", "COM"),
                PinItem(17, "GPIO18", "VSPI SCK • Digital I/O", "COM"),
                PinItem(18, "GPIO17 (TX2)", "UART2 TX (Cocok untuk Sensor PZEM-004T / GPS)", "COM"),
                PinItem(19, "GPIO16 (RX2)", "UART2 RX (Cocok untuk Sensor PZEM-004T / GPS)", "COM"),
                PinItem(20, "3V3", "Output Tegangan Kerja 3.3V (Maks 600mA)", "POWER")
            )
        ),
        HardwareBoardPinout(
            id = "NE555",
            title = "IC NE555 Timer (DIP-8)",
            category = "Analog IC",
            specs = "Tegangan Kerja: 4.5V s/d 16V • Arus Output: 200mA",
            leftPins = listOf(
                PinItem(1, "GND", "Ground (0V Reference)", "GND"),
                PinItem(2, "TRIG", "Trigger Input (< 1/3 VCC memicu Output HIGH)", "ADC"),
                PinItem(3, "OUT", "Output Sinyal Gelombang Kotak / PWM", "GPIO"),
                PinItem(4, "RESET", "Active-Low Reset (Hubungkan ke VCC jika tidak dipakai)", "POWER")
            ),
            rightPins = listOf(
                PinItem(8, "VCC", "Catu Daya Positif (+5V s/d +15V)", "POWER"),
                PinItem(7, "DISCH", "Discharge Kolektor Terbuka untuk Kapasitor Timing", "ADC"),
                PinItem(6, "THRES", "Threshold (> 2/3 VCC mengakhiri siklus waktu)", "ADC"),
                PinItem(5, "CTRL", "Control Voltage (Pasang kapasitor 10nF ke GND)", "COM")
            )
        ),
        HardwareBoardPinout(
            id = "PZEM004T",
            title = "Sensor Energi PZEM-004T v3",
            category = "Smart Energy",
            specs = "Ukur Tegangan 80-260VAC, Arus 100A, Watt, kWh, Pf & Hz (Modbus UART)",
            leftPins = listOf(
                PinItem(1, "5V (VCC)", "Catu Daya 5V DC untuk Optocoupler Internal", "POWER"),
                PinItem(2, "RX", "Terima Data TTL (Hubungkan ke TX2 GPIO17 ESP32)", "COM"),
                PinItem(3, "TX", "Kirim Data TTL (Hubungkan ke RX2 GPIO16 ESP32)", "COM"),
                PinItem(4, "GND", "Ground DC Mikrokontroler", "GND")
            ),
            rightPins = listOf(
                PinItem(5, "AC-L (Fasa)", "Input Jala-jala Listrik AC 220V Fasa (Hati-hati Tegangan Tinggi!)", "POWER"),
                PinItem(6, "AC-N (Netral)", "Input Jala-jala Listrik AC 220V Netral", "GND"),
                PinItem(7, "CT+ (Merah)", "Kabel Kumparan Current Transformer (CT) Arus Beban", "ADC"),
                PinItem(8, "CT- (Hitam)", "Kabel Kumparan Current Transformer (CT) Arus Beban", "ADC")
            )
        )
    )
}

fun exportBomToCsv(context: Context, list: List<BomComponentItem>) {
    try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "BOM_Proyek_BambuElectro_${System.currentTimeMillis() % 10000}.csv")
        val sb = StringBuilder()
        sb.append("No,Nama Komponen,Kategori,Spesifikasi_Footprint,Qty,Harga_Satuan_Rp,Subtotal_Rp\n")
        list.forEachIndexed { idx, item ->
            val sub = item.qty * item.unitPriceIdr
            sb.append("${idx + 1},\"${item.partName}\",\"${item.category}\",\"${item.footprintOrSpec}\",${item.qty},${item.unitPriceIdr},$sub\n")
        }
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Daftar Belanja Komponen (BOM) - BambuElectro")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Daftar BOM (.CSV) ke..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}