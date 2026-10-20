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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun PinoutAndBomScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Pinout & Penjelasan Kaki, 1 = BOM Estimator

    val bomList = remember {
        mutableStateListOf(
            BomComponentItem(1, "ESP32 DevKit V1 Wi-Fi/BT", "Mikrokontroler", "DIP-30 Dual Core", 1, 65000),
            BomComponentItem(2, "Arduino Uno R3 ATmega328P", "Mikrokontroler", "DIP-28 USB Type-B", 1, 85000),
            BomComponentItem(3, "Sensor Energi PZEM-004T v3", "Sensor Daya", "UART TTL + CT 100A", 1, 85000),
            BomComponentItem(4, "Sensor Suhu & Lembab DHT22", "Sensor Lingkungan", "Digital 1-Wire Akurat", 1, 45000),
            BomComponentItem(5, "Sensor Jarak Ultrasonik HC-SR04", "Sensor Jarak", "Trigger/Echo 5V", 2, 15000),
            BomComponentItem(6, "Driver Motor L298N Dual H-Bridge", "Aktuator", "5V-35V 2A Stepper/DC", 1, 25000),
            BomComponentItem(7, "Layar OLED 0.96 Inch I2C", "Display", "SSD1306 (0x3C)", 1, 28000),
            BomComponentItem(8, "Modul LoRa Ra-02 SX1278 433MHz", "Telekomunikasi", "SPI Transceiver", 2, 60000),
            BomComponentItem(9, "IC Timer NE555 + Soket DIP-8", "IC Analog", "Through-Hole DIP-8", 5, 3000),
            BomComponentItem(10, "Resistor Metal Film 1kΩ & 4.7kΩ", "Pasif", "1/4W ±1% Pack", 20, 500)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
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
                Text("Pinout & Fungsi Kaki", color = if (selectedSubTab == 0) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
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
                Text("BOM Proyek (${bomList.size})", color = if (selectedSubTab == 1) PureWhite else SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
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
// 1. VISUAL PINOUT DI ATAS + DAFTAR LENGKAP FUNGSI SETIAP KAKI DI BAWAH
// ============================================================================
@Composable
fun VisualPinoutExplorerView() {
    val boards = remember { getExpandedHardwareDatabase() }
    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = remember { listOf("Semua") + boards.map { it.category }.distinct() }

    val filteredBoards = remember(selectedCategory) {
        boards.filter { selectedCategory == "Semua" || it.category == selectedCategory }
    }

    var selectedBoard by remember { mutableStateOf(boards.first()) }
    var highlightedPin by remember { mutableStateOf<PinItem?>(boards.first().leftPins.first()) }

    val allPinsOrdered = remember(selectedBoard) {
        (selectedBoard.leftPins + selectedBoard.rightPins).sortedBy { it.pinNumber }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Filter Kategori Komponen (MCU, Sensor, Driver/Aktuator, IC Analog, RF)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            val firstInCat = boards.firstOrNull { cat == "Semua" || it.category == cat }
                            if (firstInCat != null) {
                                selectedBoard = firstInCat
                                highlightedPin = firstInCat.leftPins.firstOrNull()
                            }
                        },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CopperGold,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }
        }

        // Pilihan Komponen / Board / Sensor
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredBoards) { board ->
                    val isSelected = board.id == selectedBoard.id
                    Surface(
                        color = if (isSelected) PcbDarkGreen else PureWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isSelected) BrightGold else SoftTraceBorder),
                        modifier = Modifier.clickable {
                            selectedBoard = board
                            highlightedPin = board.leftPins.firstOrNull()
                        }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = board.title,
                                color = if (isSelected) PureWhite else SiliconDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${board.category} •${board.leftPins.size + board.rightPins.size} Kaki",
                                color = if (isSelected) BrightGold else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // BAGIAN ATAS: GAMBAR VISUAL PINOUT KIRI-KANAN BESERTA BODI CHIP/MODUL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, PcbDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔌 Diagram Kaki: ${selectedBoard.title}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = PcbDarkGreen
                            )
                            Text(
                                text = selectedBoard.specs,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                        Surface(color = PcbDarkGreen, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Total ${allPinsOrdered.size} Pin",
                                color = BrightGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legenda Warna Kaki Pin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PinLegendDot("VCC/5V/3V3", LogicHighRed)
                        PinLegendDot("GND (0V)", SiliconDark)
                        PinLegendDot("ADC/Analog", CopperGold)
                        PinLegendDot("UART/I2C/SPI", LogicLowBlue)
                        PinLegendDot("GPIO/PWM", PcbEmerald)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gambar Visual Chip / Modul Kiri-Kanan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedBoard.leftPins.forEach { pin ->
                                PinBadgeButton(
                                    pin = pin,
                                    isLeft = true,
                                    isSelected = highlightedPin?.pinNumber == pin.pinNumber,
                                    onClick = { highlightedPin = pin }
                                )
                            }
                        }

                        // Bodi Tengah IC / PCB Modul
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height((selectedBoard.leftPins.size.coerceAtLeast(selectedBoard.rightPins.size) * 34).dp)
                                .background(SiliconDark, RoundedCornerShape(12.dp))
                                .border(2.dp, BrightGold, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐼", fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedBoard.id.uppercase(),
                                    color = BrightGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedBoard.rightPins.forEach { pin ->
                                PinBadgeButton(
                                    pin = pin,
                                    isLeft = false,
                                    isSelected = highlightedPin?.pinNumber == pin.pinNumber,
                                    onClick = { highlightedPin = pin }
                                )
                            }
                        }
                    }
                }
            }
        }

        // KOTAK HIGHLIGHT PIN YANG SEDANG DIKETUK DI ATAS
        item {
            highlightedPin?.let { pin ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, BrightGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "📌 Detail Kaki Terpilih — Pin #${pin.pinNumber}: ${pin.pinName} [${pin.type}]",
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

        // BAGIAN BAWAH: TABEL PENJELASAN LENGKAP SELURUH KAKI & FUNGSINYA
        item {
            Text(
                text = "📋 Daftar Lengkap Urutan Kaki & Fungsinya (${selectedBoard.title}):",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = PcbDarkGreen
            )
        }

        items(allPinsOrdered, key = { "${selectedBoard.id}_${it.pinNumber}" }) { pin ->
            val isHighlighted = highlightedPin?.pinNumber == pin.pinNumber
            val typeBadgeColor = when (pin.type) {
                "POWER" -> LogicHighRed
                "GND" -> SiliconDark
                "ADC" -> CopperGold
                "COM" -> LogicLowBlue
                else -> PcbEmerald
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isHighlighted) Color(0xFFFEF3C7) else PureWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isHighlighted) 2.dp else 1.dp,
                    color = if (isHighlighted) CopperGold else SoftTraceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { highlightedPin = pin }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = typeBadgeColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(64.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "PIN #${pin.pinNumber}",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = pin.type,
                                color = SoftTraceBorder,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pin.pinName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = SiliconDark
                        )
                        Text(
                            text = pin.functionDesc,
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SiliconDark)
    }
}

@Composable
fun PinBadgeButton(pin: PinItem, isLeft: Boolean, isSelected: Boolean, onClick: () -> Unit) {
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
        border = if (isSelected) BorderStroke(2.dp, BrightGold) else null,
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLeft) "${pin.pinNumber}.${pin.pinName}" else pin.pinName,
                color = PureWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isLeft) {
                Text("#${pin.pinNumber}", color = SoftTraceBorder, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// 2. BILL OF MATERIALS (BOM) DENGAN KATALOG CEPAT KOMPONEN LAB & EXPORT CSV
// ============================================================================
@Composable
fun BomProjectEstimatorView(bomList: MutableList<BomComponentItem>) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    val grandTotalIdr = bomList.sumOf { it.qty * it.unitPriceIdr }

    val quickCatalog = remember {
        listOf(
            BomComponentItem(101, "Raspberry Pi Pico W RP2040", "Mikrokontroler", "Dual-Core ARM Wi-Fi", 1, 95000),
            BomComponentItem(102, "STM32F103C8T6 BluePill", "Mikrokontroler", "ARM Cortex-M3 72MHz", 1, 38000),
            BomComponentItem(103, "Sensor Gas MQ-2 / MQ-135", "Sensor Gas", "Analog & TTL Output", 1, 18000),
            BomComponentItem(104, "Sensor Arus ACS712 20A/30A", "Sensor Daya", "Hall-Effect Analog", 1, 22000),
            BomComponentItem(105, "Sensor Suhu Air DS18B20 Waterproof", "Sensor Suhu", "1-Wire Digital Probe", 1, 20000),
            BomComponentItem(106, "Modul Step-Down Buck LM2596", "Power Supply", "Input 4-35V Output 1.2-30V", 1, 14000),
            BomComponentItem(107, "Motor Servo MG996R Metal Gear", "Aktuator", "PWM High Torque", 1, 55000),
            BomComponentItem(108, "Papan PCB Bolong FR4 9x15cm", "PCB & Mekanik", "Double Layer Tinned", 1, 25000)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbDarkGreen),
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
                    Column {
                        Text("Total Anggaran BOM Proyek:", color = SoftTraceBorder, fontSize = 11.sp)
                        Text(ElectroMath.formatRupiah(grandTotalIdr), color = BrightGold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Manual", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                Text("⚡ Ketuk untuk tambah cepat dari Katalog Komponen Lab:", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickCatalog) { preset ->
                        Surface(
                            color = SiliconDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BrightGold),
                            modifier = Modifier.clickable {
                                val existingIdx = bomList.indexOfFirst { it.partName == preset.partName }
                                if (existingIdx != -1) {
                                    val old = bomList[existingIdx]
                                    bomList[existingIdx] = old.copy(qty = old.qty + 1)
                                } else {
                                    bomList.add(0, preset.copy(id = System.currentTimeMillis()))
                                }
                                Toast.makeText(context, "+1 ${preset.partName} ditambahkan!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "+ ${preset.partName} (${ElectroMath.formatRupiah(preset.unitPriceIdr)})",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
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
                            Text("${item.category} •${item.footprintOrSpec}", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "${item.qty} pcs × ${ElectroMath.formatRupiah(item.unitPriceIdr)} =${ElectroMath.formatRupiah(item.qty * item.unitPriceIdr)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PcbEmerald
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val idx = bomList.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        if (item.qty > 1) bomList[idx] = item.copy(qty = item.qty - 1)
                                        else bomList.removeAt(idx)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = CopperGold)
                            }
                            Text("${item.qty}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            IconButton(
                                onClick = {
                                    val idx = bomList.indexOfFirst { it.id == item.id }
                                    if (idx != -1) bomList[idx] = item.copy(qty = item.qty + 1)
                                }
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = PcbEmerald)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Sensor / Modul") }
        var spec by remember { mutableStateOf("DIP / SMD 0805") }
        var qtyStr by remember { mutableStateOf("1") }
        var priceStr by remember { mutableStateOf("15000") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = PureWhite,
            title = { Text("Tambah Komponen ke BOM", fontWeight = FontWeight.ExtraBold, color = PcbDarkGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Komponen / IC / Sensor") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, singleLine = true)
                    OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Spesifikasi / Footprint") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it.filter { c -> c.isDigit() } }, label = { Text("Qty") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { c -> c.isDigit() } }, label = { Text("Harga (Rp)") }, modifier = Modifier.weight(1f), singleLine = true)
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
                                    category = category,
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

// ============================================================================
// 3. DATABASE PINOUT LENGKAP (MIKROKONTROLER, SENSOR, DRIVER, IC & RF)
// ============================================================================
fun getExpandedHardwareDatabase(): List<HardwareBoardPinout> {
    return listOf(
        HardwareBoardPinout(
            id = "ESP32",
            title = "ESP32 DevKit V1",
            category = "Mikrokontroler",
            specs = "Dual-Core Xtensa 240MHz • 3.3V Logic • Wi-Fi & BLE (Perhatikan Pin ADC1 vs ADC2!)",
            leftPins = listOf(
                PinItem(1, "EN (RST)", "Enable / Reset Mikrokontroler (Aktif LOW, dilengkapi Pull-Up internal)", "POWER"),
                PinItem(2, "GPIO36 (VP)", "Input Only • ADC1_CH0 (Sangat aman untuk sensor analog saat Wi-Fi aktif)", "ADC"),
                PinItem(3, "GPIO39 (VN)", "Input Only • ADC1_CH3 (Tidak memiliki resistor Pull-Up/Pull-Down internal)", "ADC"),
                PinItem(4, "GPIO34", "Input Only • ADC1_CH6 (Khusus pembacaan sensor analog / tombol eksternal)", "ADC"),
                PinItem(5, "GPIO35", "Input Only • ADC1_CH7 (Khusus pembacaan tegangan analog 0 - 3.3V)", "ADC"),
                PinItem(6, "GPIO32", "ADC1_CH4 • Touch9 • Pin I/O Digital & PWM Serbaguna", "GPIO"),
                PinItem(7, "GPIO33", "ADC1_CH5 • Touch8 • Pin I/O Digital & PWM Serbaguna", "GPIO"),
                PinItem(8, "GPIO25", "DAC_1 (Output Sinyal Analog Murni) • ADC2_CH8", "ADC"),
                PinItem(9, "GPIO26", "DAC_2 (Output Sinyal Analog Murni) • ADC2_CH9", "ADC"),
                PinItem(10, "GPIO27", "ADC2_CH7 • Touch7 • Digital I/O & PWM", "GPIO"),
                PinItem(11, "GPIO14", "HSPI CLK • Touch6 • Mengeluarkan sinyal PWM singkat saat Booting", "COM"),
                PinItem(12, "GPIO12", "⚠️ Strapping Pin (Jangan ditarik HIGH saat Boot karena gagal Flash!)", "GPIO"),
                PinItem(13, "GND", "Ground (0V Referensi Utama)", "GND"),
                PinItem(14, "VIN (5V)", "Input Catu Daya 5V DC dari USB atau Regulator Eksternal", "POWER")
            ),
            rightPins = listOf(
                PinItem(15, "GPIO23", "VSPI MOSI • Pin Digital I/O & PWM Paling Aman", "COM"),
                PinItem(16, "GPIO22", "I2C SCL Default (Clock untuk OLED, LCD PCF8574, BME280, RTC)", "COM"),
                PinItem(17, "GPIO1 (TX0)", "UART0 TX (Digunakan untuk Upload Program & Serial Monitor)", "COM"),
                PinItem(18, "GPIO3 (RX0)", "UART0 RX (Digunakan untuk Upload Program & Serial Monitor)", "COM"),
                PinItem(19, "GPIO21", "I2C SDA Default (Data untuk OLED, LCD PCF8574, BME280, RTC)", "COM"),
                PinItem(20, "GPIO19", "VSPI MISO • Digital I/O & PWM", "COM"),
                PinItem(21, "GPIO18", "VSPI SCK (Clock SPI untuk LoRa SX1278 / SD Card)", "COM"),
                PinItem(22, "GPIO5", "VSPI CS0 (Chip Select) • Strapping Pin", "COM"),
                PinItem(23, "GPIO17 (TX2)", "UART2 TX Hardware (Sangat cocok untuk Sensor PZEM-004T / GPS / SIM800L)", "COM"),
                PinItem(24, "GPIO16 (RX2)", "UART2 RX Hardware (Sangat cocok untuk Sensor PZEM-004T / GPS / SIM800L)", "COM"),
                PinItem(25, "GPIO4", "ADC2_CH0 • Touch0 • Sering dipakai untuk Sensor Suhu DHT22 / DS18B20", "GPIO"),
                PinItem(26, "GPIO2", "Terhubung ke LED Biru Internal Board • Wajib Mengambang/LOW saat Boot", "GPIO"),
                PinItem(27, "GPIO15", "HSPI CS0 • Strapping Pin (Mengeluarkan log saat Boot)", "GPIO"),
                PinItem(28, "3V3", "Output Tegangan Kerja 3.3V dari Regulator AMS1117 (Arus maks 600mA)", "POWER")
            )
        ),
        HardwareBoardPinout(
            id = "UNO_R3",
            title = "Arduino Uno R3 (ATmega328P)",
            category = "Mikrokontroler",
            specs = "8-Bit AVR 16MHz • 5V Logic • 6 Pin PWM • 6 Pin ADC 10-Bit (0-1023)",
            leftPins = listOf(
                PinItem(1, "IOREF / RST", "Referensi Tegangan I/O (5V) & Pin Reset Mikrokontroler", "POWER"),
                PinItem(2, "3.3V Out", "Output Tegangan 3.3V (Arus maksimum 50mA untuk modul kecil)", "POWER"),
                PinItem(3, "5V Out", "Output Tegangan Utama 5V DC dari Regulator / USB (Maks 500mA)", "POWER"),
                PinItem(4, "GND", "Ground (0V Referensi Utama)", "GND"),
                PinItem(5, "VIN (7-12V)", "Input Tegangan Adaptor DC Eksternal (Rekomendasi 7V - 12V DC)", "POWER"),
                PinItem(6, "A0", "Analog Input Channel 0 (ADC 10-Bit: 0 - 1023) / Digital Pin 14", "ADC"),
                PinItem(7, "A1", "Analog Input Channel 1 (ADC 10-Bit: 0 - 1023) / Digital Pin 15", "ADC"),
                PinItem(8, "A2", "Analog Input Channel 2 (ADC 10-Bit: 0 - 1023) / Digital Pin 16", "ADC"),
                PinItem(9, "A3", "Analog Input Channel 3 (ADC 10-Bit: 0 - 1023) / Digital Pin 17", "ADC"),
                PinItem(10, "A4 (SDA)", "I2C Data Line (SDA) + Analog Input Channel 4", "COM"),
                PinItem(11, "A5 (SCL)", "I2C Clock Line (SCL) + Analog Input Channel 5", "COM")
            ),
            rightPins = listOf(
                PinItem(12, "D13 (SCK)", "SPI Clock + Terhubung ke LED Built-in Oranye pada Board Uno", "COM"),
                PinItem(13, "D12 (MISO)", "SPI Master In Slave Out + Digital I/O", "COM"),
                PinItem(14, "D11 (~PWM)", "SPI MOSI + Output PWM 8-Bit (Timer 2)", "COM"),
                PinItem(15, "D10 (~PWM)", "SPI SS (Chip Select) + Output PWM 8-Bit (Timer 1)", "GPIO"),
                PinItem(16, "D9 (~PWM)", "Output PWM 16-Bit (Cocok untuk kendali Motor Servo)", "GPIO"),
                PinItem(17, "D8", "Digital Input / Output Standar", "GPIO"),
                PinItem(18, "D7 & D6 (~)", "Pin D7 Digital I/O & Pin D6 Output PWM (Timer 0)", "GPIO"),
                PinItem(19, "D5 (~) & D4", "Pin D5 Output PWM & Pin D4 Digital I/O", "GPIO"),
                PinItem(20, "D3 (~INT1)", "External Interrupt 1 (`attachInterrupt`) + Output PWM", "GPIO"),
                PinItem(21, "D2 (INT0)", "External Interrupt 0 (Cocok untuk Sensor Rotary Encoder / Flow Meter)", "GPIO"),
                PinItem(22, "D1 (TX) & D0 (RX)", "UART Serial Hardware (Jangan pasang komponen saat upload kode!)", "COM")
            )
        ),
        HardwareBoardPinout(
            id = "PZEM004T",
            title = "Sensor Energi PZEM-004T v3",
            category = "Sensor Lab",
            specs = "Ukur Tegangan 80-260VAC, Arus 100A, Daya Aktif (W), Energi (kWh), Frekuensi & Power Factor",
            leftPins = listOf(
                PinItem(1, "5V (VCC DC)", "Catu Daya 5V DC untuk menyuplai sisi Optocoupler TTL", "POWER"),
                PinItem(2, "RX (TTL)", "Menerima perintah Modbus dari TX Mikrokontroler (Misal GPIO17 ESP32)", "COM"),
                PinItem(3, "TX (TTL)", "Mengirim balasan data ke RX Mikrokontroler (Misal GPIO16 ESP32)", "COM"),
                PinItem(4, "GND (DC)", "Ground sisi Mikrokontroler (Terisolasi penuh dari tegangan AC 220V)", "GND")
            ),
            rightPins = listOf(
                PinItem(5, "AC-L (Line)", "⚡ Input Listrik Jala-Jala PLN 220V Fasa (Wajib hati-hati tegangan tinggi!)", "POWER"),
                PinItem(6, "AC-N (Neutral)", "Input Listrik Jala-Jala PLN 220V Netral (Menyuplai chip ukur internal)", "GND"),
                PinItem(7, "CT+ (Merah)", "Kabel Positif Kumparan Toroid Current Transformer (Kalungkan pada 1 kabel beban)", "ADC"),
                PinItem(8, "CT- (Hitam)", "Kabel Negatif Kumparan Toroid Current Transformer (100A:100mA)", "ADC")
            )
        ),
        HardwareBoardPinout(
            id = "DHT_HCSR",
            title = "Sensor DHT22 & Ultrasonik HC-SR04",
            category = "Sensor Lab",
            specs = "Kombinasi 2 Sensor Praktikum Paling Populer: Suhu/Kelembaban & Pengukur Jarak",
            leftPins = listOf(
                PinItem(1, "DHT22 Pin 1 (VCC)", "Catu Daya Positif Sensor DHT22 (Mendukung 3.3V ESP32 maupun 5V Arduino)", "POWER"),
                PinItem(2, "DHT22 Pin 2 (DATA)", "Sinyal Data Digital 1-Wire (Pasang resistor Pull-Up 4.7kΩ atau 10kΩ ke VCC)", "COM"),
                PinItem(3, "DHT22 Pin 3 (NC)", "Not Connected (Kaki ke-3 kosong / tidak dihubungkan ke mana pun)", "GPIO"),
                PinItem(4, "DHT22 Pin 4 (GND)", "Ground (0V Referensi Sensor DHT22)", "GND")
            ),
            rightPins = listOf(
                PinItem(5, "HC-SR04 VCC", "Catu Daya 5V DC untuk Pemancar Ultrasonik 40 kHz", "POWER"),
                PinItem(6, "HC-SR04 TRIG", "Trigger Input: Beri pulsa HIGH minimal 10 µs untuk memancarkan gelombang suara", "GPIO"),
                PinItem(7, "HC-SR04 ECHO", "Echo Output: Mengeluarkan pulsa HIGH sepanjang waktu pantulan (Rumus: durasi × 0.034 / 2)", "ADC"),
                PinItem(8, "HC-SR04 GND", "Ground (0V Referensi Sensor HC-SR04)", "GND")
            )
        ),
        HardwareBoardPinout(
            id = "L298N",
            title = "Driver Motor L298N Dual H-Bridge",
            category = "Aktuator & Driver",
            specs = "Kendali 2 Motor DC (Maju/Mundur/Kecepatan PWM) atau 1 Motor Stepper Bipolar (5V - 35V, 2A)",
            leftPins = listOf(
                PinItem(1, "OUT1 & OUT2", "Terminal Output ke Kabel Motor DC Kiri (Motor A)", "POWER"),
                PinItem(2, "12V Power In", "Input Tegangan Baterai / Adaptor Motor (6V s/d 35V DC)", "POWER"),
                PinItem(3, "GND Common", "⚠️ Wajib disatukan antara Ground Baterai Motor dan Ground Arduino/ESP32!", "GND"),
                PinItem(4, "5V Logic", "Output 5V dari regulator 78M05 internal (Jika Jumper 5V_EN terpasang)", "POWER"),
                PinItem(5, "OUT3 & OUT4", "Terminal Output ke Kabel Motor DC Kanan (Motor B)", "POWER")
            ),
            rightPins = listOf(
                PinItem(6, "ENA (PWM A)", "Pin pengatur kecepatan PWM Motor A (0 - 255). Lepas jumper untuk kontrol PWM", "GPIO"),
                PinItem(7, "IN1 (Arah A1)", "Logika Arah Putar Motor A (HIGH-LOW = Maju, LOW-HIGH = Mundur)", "GPIO"),
                PinItem(8, "IN2 (Arah A2)", "Logika Arah Putar Motor A pasangan IN1", "GPIO"),
                PinItem(9, "IN3 (Arah B1)", "Logika Arah Putar Motor B (HIGH-LOW = Maju, LOW-HIGH = Mundur)", "GPIO"),
                PinItem(10, "IN4 & ENB", "IN4 Logika Arah Motor B & ENB Pin Kecepatan PWM Motor B", "GPIO")
            )
        ),
        HardwareBoardPinout(
            id = "NE555",
            title = "IC NE555 Timer & LM358 Op-Amp",
            category = "IC Analog",
            specs = "Susunan Kaki DIP-8 Standar untuk IC NE555 (Osilator PWM) & LM358 (Dual Op-Amp)",
            leftPins = listOf(
                PinItem(1, "Pin 1 (GND / OUT-A)", "Pada NE555: Ground (0V) | Pada LM358: Output Penguat Op-Amp A", "GND"),
                PinItem(2, "Pin 2 (TRIG / IN-A)", "Pada NE555: Trigger (< 1/3 VCC) | Pada LM358: Inverting Input (-) Op-Amp A", "ADC"),
                PinItem(3, "Pin 3 (OUT / IN+A)", "Pada NE555: Output Gelombang Kotak | Pada LM358: Non-Inverting Input (+) Op-Amp A", "GPIO"),
                PinItem(4, "Pin 4 (RST / GND)", "Pada NE555: Reset (Hubungkan ke VCC) | Pada LM358: Ground / VEE (-)", "POWER")
            ),
            rightPins = listOf(
                PinItem(8, "Pin 8 (VCC+)", "Pada NE555 & LM358: Catu Daya Positif VCC (+5V s/d +15V DC)", "POWER"),
                PinItem(7, "Pin 7 (DISCH / OUT-B)", "Pada NE555: Discharge Kapasitor | Pada LM358: Output Penguat Op-Amp B", "ADC"),
                PinItem(6, "Pin 6 (THRES / IN-B)", "Pada NE555: Threshold (> 2/3 VCC) | Pada LM358: Inverting Input (-) Op-Amp B", "ADC"),
                PinItem(5, "Pin 5 (CTRL / IN+B)", "Pada NE555: Control Voltage (10nF ke GND) | Pada LM358: Non-Inverting (+) B", "COM")
            )
        ),
        HardwareBoardPinout(
            id = "LORA_SX",
            title = "Modul LoRa Ra-02 (SX1278 433MHz)",
            category = "Telekomunikasi RF",
            specs = "Long Range Spread Spectrum Transceiver • Jangkauan 3-10 km • Antarmuka SPI 3.3V Murni!",
            leftPins = listOf(
                PinItem(1, "ANT (Antena)", "⚠️ Wajib pasang antena pegas/SMA 433MHz sebelum dinyalakan agar PA tidak rusak!", "ADC"),
                PinItem(2, "GND", "Ground (0V Referensi RF Shield)", "GND"),
                PinItem(3, "3.3V VCC", "⚠️ Tegangan Kerja Murni 3.3V DC (JANGAN dihubungkan ke 5V karena chip akan putus!)", "POWER"),
                PinItem(4, "RST (Reset)", "Pin Reset Modul LoRa (Hubungkan ke GPIO14 ESP32 / D9 Arduino via Level Shifter)", "GPIO"),
                PinItem(5, "DIO0 (IRQ)", "Interrupt Pin: Memberi sinyal ke MCU saat paket LoRa selesai diterima (RxDone)", "GPIO")
            ),
            rightPins = listOf(
                PinItem(6, "NSS (CS)", "SPI Chip Select / Slave Select (Hubungkan ke GPIO5 ESP32 / D10 Arduino)", "COM"),
                PinItem(7, "MOSI", "SPI Master Out Slave In (Hubungkan ke GPIO23 ESP32 / D11 Arduino)", "COM"),
                PinItem(8, "MISO", "SPI Master In Slave Out (Hubungkan ke GPIO19 ESP32 / D12 Arduino)", "COM"),
                PinItem(9, "SCK", "SPI Clock (Hubungkan ke GPIO18 ESP32 / D13 Arduino)", "COM"),
                PinItem(10, "GND", "Ground Tambahan di Sisi Kanan", "GND")
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