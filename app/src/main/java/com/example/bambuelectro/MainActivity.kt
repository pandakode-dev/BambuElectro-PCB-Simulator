package com.example.bambuelectro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BambuElectroColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BambuElectroMainApp()
                }
            }
        }
    }
}

@Composable
fun BambuElectroMainApp() {
    var selectedTab by remember { mutableIntStateOf(0) } // 0..4 = BottomBar, 5 = Pro Engineering Hub

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PcbDarkGreen, PcbEmerald, SiliconDark)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SchematicIvory,
                            border = BorderStroke(2.dp, BrightGold),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_bambuelectro),
                                contentDescription = "Logo BambuElectro",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BambuElectro",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = "PCB • Breadboard Sim • RF • PLC • Jobsheet PDF",
                                color = SoftTraceBorder,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Akses Modul Pro Suite (RF Smith Chart, SMPS/Solar, & PDF Jobsheet)
                    Surface(
                        color = if (selectedTab == 5) BrightGold else SiliconDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, BrightGold),
                        modifier = Modifier.clickable { selectedTab = 5 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Pro Suite",
                                tint = if (selectedTab == 5) SiliconDark else BrightGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PRO LAB",
                                color = if (selectedTab == 5) SiliconDark else PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(BrightGold)
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = PureWhite, tonalElevation = 10.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                    label = { Text("Resistor", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = PcbDarkGreen,
                        indicatorColor = PcbEmerald
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ElectricalServices, contentDescription = null) },
                    label = { Text("Sirkuit", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = CopperGold,
                        indicatorColor = CopperGold
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = null) },
                    label = { Text("Simulasi", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = PcbDarkGreen,
                        indicatorColor = PcbEmerald
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Memory, contentDescription = null) },
                    label = { Text("Pinout", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = CopperGold,
                        indicatorColor = CopperGold
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                    label = { Text("PLC & PID", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = PcbDarkGreen,
                        indicatorColor = PcbEmerald
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PassiveComponentScreen()
                1 -> CircuitCalculatorScreen()
                2 -> VirtualSimLabScreen()
                3 -> PinoutAndBomScreen()
                4 -> SensorAndControlScreen()
                5 -> ProEngineeringHubScreen()
            }
        }
    }
}