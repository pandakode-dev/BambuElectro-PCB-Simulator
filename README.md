




<div align="center">

# 🐼⚡ BambuElectro — Ultimate Electronics, PCB, PLC & Circuit Simulator Suite
### **Official Native Android Electrical & Microcontroller Engineering Workbench by PandaKode**

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-064E3B?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![IPC-2221](https://img.shields.io/badge/PCB%20Standard-IPC--2221%20%26%20EIA--96-D97706?style=for-the-badge&logo=circuitverse&logoColor=white)
![Offline First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-10B981?style=for-the-badge&logo=android&logoColor=white)

**BambuElectro** adalah aplikasi laboratorium elektronika, kalkulator desain PCB, referensi pinout mikrokontroler, serta simulator rangkaian interaktif (*Drag-and-Drop Breadboard & Digital Oscilloscope*) berbasis **Native Android (Kotlin & Jetpack Compose)**. Mengusung identitas visual *Solder Mask Emerald, ENIG Copper Gold, & Silicon Charcoal*, aplikasi ini dirancang khusus untuk insinyur elektronika, mahasiswa teknik, praktisi IoT, dan pengajar laboratorium.

</div>

---

## 📸 Tampilan Aplikasi (Screenshots)

| Visual Resistor & SMD EIA-96 | Drag & Drop Breadboard Sim | Digital Oscilloscope & FFT | Kalkulator PCB & Baterai IoT | Visual Pinout ESP32 & BOM |
| :---: | :---: | :---: | :---: | :---: |
| <img width="1080" height="2400" alt="Visual Resistor   SMD EIA-96" src="https://github.com/user-attachments/assets/98f16480-8fa1-443e-b96b-03d6bbba2ba0" /> | <img width="1080" height="2400" alt="Drag   Drop Breadboard Sim" src="https://github.com/user-attachments/assets/3fc55335-5624-4265-9922-a5e4ae9b3d7d" /> | <img width="1080" height="2400" alt="Digital Oscilloscope   FFT" src="https://github.com/user-attachments/assets/359ddeea-7457-4a6a-8850-1316d4a9c9a2" /> | <img width="1080" height="2400" alt="Kalkulator PCB   Baterai IoT" src="https://github.com/user-attachments/assets/fc0a2b21-779d-4c39-8128-8455d7e79a57" /> | <img width="1080" height="2400" alt="Visual Pinout ESP32   BOM" src="https://github.com/user-attachments/assets/10dcd3e6-d401-4aa4-b606-e9d45dad2bd4" /> |

---

## ✨ 12 Fitur Utama (Comprehensive Engineering Suite)

### 1. 🛠️ Interactive Drag-and-Drop Breadboard Simulator (Wokwi / Tinkercad Style)
* **Visualisasi Fisik & Simbol IEEE/ANSI Asli:** Merender bentuk fisik resistor dengan **4 gelang warna dinamis yang otomatis berubah mengikuti nilai Ohm-nya**, simbol gerbang logika standar IEEE (`AND`, `OR`, `NAND`, `XOR`), LED dengan pendaran cahaya, induktor spiral, kapasitor, dioda Zener, potensiometer putar, hingga transistor NPN & MOSFET.
* **Alat Ukur Interaktif di Atas Meja Kerja:** Tarik dan hubungkan **Multimeter Digital (DMM)**, **Osiloskop Probe (CH1/CH2)**, **Wattmeter**, dan **Logic Probe** langsung ke titik rangkaian. Ketuk alat ukur di kanvas untuk membuka popup layar instrumen *real-time*!
* **Animasi Aliran Elektron & Proteksi Burnout:** Titik-titik cahaya elektron mengalir di sepanjang kurva kabel *Cubic Bézier* dengan kecepatan proporsional terhadap besar arus (`mA`), dilengkapi animasi asap & percikan api (*burnout warning*) saat terjadi *overcurrent*.
* **Auto-Generate Sketch Arduino (`.ino`), Save/Load Project & Export PNG:** Otomatis menghasilkan kode C++ Arduino dari skematik yang dirakit, menyimpan proyek secara lokal, dan mengekspor gambar skematik ke WhatsApp/Laporan.

### 2. 📈 Dual-Channel Digital Storage Oscilloscope, Function Generator & FFT Spectrum
* Simulasi osiloskop dua kanal (**CH1 Input** & **CH2 Probe**) pada domain waktu (*Time-Domain*) dan domain frekuensi (**Harmonic FFT Spectrum dBV**) untuk gelombang *Sine, Square PWM, Triangle, Sawtooth*, dan *RC Filtered*.
* **Real Audio Sine Tone Generator (20 Hz – 8 kHz):** Memancarkan sinyal gelombang sinus murni melalui DAC audio ponsel (`AudioTrack` PCM 16-bit 44.1 kHz) untuk pengujian *amplifier* dan resonansi akustik, ditambah **Karnaugh Map (K-Map) Solver 2–4 Variabel**.

### 3. 🏭 Industrial PLC Instrumentation, Protocol Timing & PID Control Lab
* **Simulator Sensor PT100 RTD / NTC Thermistor & Jembatan Wheatstone:** Menghitung resistansi sensor terhadap perubahan suhu (°C), tegangan diferensial jembatan ($\Delta V$), dan penguatan Op-Amp Instrumentasi.
* **Industrial 4–20 mA Current Loop Scaler:** Konversi sinyal arus transmiter industri ke satuan proses fisik (`Bar`, `°C`, `Level %`), tegangan *shunt* $250\,\Omega$, serta nilai *Raw ADC* PLC Siemens (`0–27648`) dan ESP32 (`0–4095`) lengkap dengan deteksi kabel putus (*Loop Open Circuit < 3.8 mA*).
* **UART 8N1 Logic Timing Diagram & Modbus RTU CRC-16 Calculator:** Visualisasi gelombang bit *Start, D0–D7 (LSB First), Stop Bit* untuk karakter ASCII apa pun, disertai kalkulator checksum **CRC-16 Modbus** siap salin.
* **PID Controller Step Response Simulator & % Error Calculator:** Simulasi numerik kurva respon sistem Orde-2 terhadap perubahan parameter $K_p, K_i, K_d$ serta kalkulator persentase galat (`% Error`) laporan praktikum.

### 4. 📡 RF Antenna Smith Chart Lite & Two-Point Sensor Calibration (`✨ PRO LAB`)
* **RF Impedance Matching (L-Network) & Smith Chart:** Plot impedansi kompleks beban antena ($Z_L = R + jX$) pada kanvas Smith Chart interaktif, hitung **VSWR**, **Return Loss (dB)**, serta nilai Induktor ($\text{nH}$) dan Kapasitor ($\text{pF}$) penyesuai impedansi untuk frekuensi LoRa `433/915 MHz`, Wi-Fi `2.4 GHz`, dan FPV `5.8 GHz`.
* **Kalibrasi Linier Sensor 2-Titik ($y = mx + c$):** Menghasilkan persamaan regresi linier dari 2 titik pembacaan *Raw ADC* beserta fungsi C++ Arduino siap pakai.

### 5. ☀️ SMPS Buck/Boost Designer, Heatsink Thermal & Off-Grid Solar Sizing (`✨ PRO LAB`)
* **DC-DC Buck & Boost Converter Calculator:** Menghitung *Duty Cycle*, nilai minimum induktor toroid ($\mu\text{H}$), dan kapasitor filter *Low-ESR* ($\mu\text{F}$).
* **Kalkulator Suhu Inti IC & Heatsink ($\theta_{JA}$):** Mengestimasi suhu *Junction* ($T_j$) pada regulator linear/MOSFET untuk mencegah *thermal runaway*.
* **Battery Pack Builder (`xSyP`) & Solar Panel Calculator:** Merancang susunan seri-paralel baterai Li-Ion 18650 beserta kebutuhan daya panel surya ($\text{Wp}$) harian.

### 6. 🔌 Real-Time Serial Plotter & MCU Hardware Timer Register Generator (`✨ PRO LAB`)
* **Serial Monitor & Live Plotter:** Terminal komunikasi dengan tombol *Quick Macro* (`AT+RST`, `GET_ADC`, dll.), mode tampilan ASCII/HEX, dan grafik pemplotan data sensor secara *real-time*.
* **MCU Timer Interrupt Calculator (AVR / STM32 / ESP32):** Menentukan nilai *Prescaler* dan register pembanding (`OCR1A` / `ARR`) paling presisi untuk frekuensi *interrupt* target beserta generasi kode `ISR` C++ otomatis.

### 7. 🧲 Hardware EMF Gauss Meter, 3-Axis Vibrometer & CNC Stepper Lab (`✨ PRO LAB`)
* **Built-In Phone Sensor Lab:** Memanfaatkan sensor fisik *Magnetometer* ponsel untuk mengukur radiasi medan elektromagnetik (**EMF dalam $\mu\text{T}$ & MilliGauss**) di sekitar trafo/motor, serta sensor *Accelerometer* untuk mengukur getaran mekanis 3-sumbu ($\text{m/s}^2$).
* **CNC / 3D Printer Stepper Driver $V_{ref}$ & Servo PWM:** Menghitung tegangan putar *trim-pot* $V_{ref}$ pada driver **A4988, DRV8825, & TMC2208/2209**, nilai `Steps/mm` untuk firmware Marlin/GRBL, serta lebar pulsa PWM motor servo ($500-2500\,\mu\text{s}$).

### 8. 📊 Active Filter Bode Plot, Gamified Quiz & Official PDF Jobsheet Exporter (`✨ PRO LAB`)
* **Bode Plot Analyzer:** Menggambar kurva respon frekuensi logaritmik ($1\,\text{Hz} - 100\,\text{kHz}$) untuk filter aktif *Low-Pass / High-Pass* Orde-1 dan Orde-2 *Sallen-Key* lengkap dengan penanda titik patah $-3\,\text{dB}$ ($f_c$).
* **Kuis Kompetensi Elektronika Interaktif:** Latihan soal gelang resistor, kode SMD, pinout ESP32, loop 4–20mA, dan gerbang logika lengkap dengan skor dan pembahasan.
* **Native A4 PDF Lab Report Generator:** Mencetak tabel perbandingan nilai teori vs nilai ukur praktikum beserta analisis kesimpulan menjadi dokumen **`.PDF` A4 resmi** siap kirim ke dosen atau arsip laboratorium.

### 9. 🎨 Visual 4/5-Band Resistor, SMD EIA-96, IPC-2221 PCB & 10+ Pinout Database
* Mencakup dekoder gelang warna resistor, kode SMD presisi **EIA-96**, kalkulator lebar jalur tembaga PCB standar **IPC-2221**, kalkulator umur baterai IoT *Deep-Sleep*, **Bill of Materials (BOM) `.CSV` Exporter**, serta diagram kaki interaktif + tabel penjelasan lengkap setiap pin untuk **ESP32, Arduino Uno R3, PZEM-004T, DHT22, HC-SR04, L298N, NE555, LM358, dan LoRa Ra-02 SX1278**.

---

## 🏗️ Arsitektur Modular 10-File (Clean & Scalable Compose Architecture)

Proyek ini dibangun dengan arsitektur modular terpisah sehingga sangat mudah dipelajari dan dikembangkan:

```text
com.example.bambuelectro/
├── SplashActivity.kt             # Layar pembuka animasi logo Panda Teknisi PCB
├── MainActivity.kt               # Kerangka navigasi utama 5 Tab + akses cepat PRO LAB
├── ElectroModels.kt              # Skema warna PCB, model data, rumus IPC-2221 & EIA-96
├── PassiveComponentScreen.kt     # Visual gelang warna resistor, SMD decoder & seri/paralel
├── CircuitCalculatorScreen.kt    # Hukum Ohm + rekomendasi Watt, LED, Baterai IoT, ADC & PCB
├── VirtualSimLabScreen.kt        # Simulator Breadboard Drag-Drop, DMM, Scope CH1/2 & K-Map
├── PinoutAndBomScreen.kt         # Diagram visual 10 Board/IC/Sensor + tabel fungsi kaki & BOM CSV
├── SensorAndControlScreen.kt     # Jembatan Wheatstone, PLC 4-20mA, UART Timing, CRC-16 & PID
├── ProEngineeringHubScreen.kt    # Hub navigasi untuk 6 modul tingkat lanjut (PRO LAB)
├── RfAndCalibrationScreen.kt     # Smith Chart RF, L-Network Matching & Kalibrasi Sensor 2-Titik
├── PowerAndSolarScreen.kt        # Perancang SMPS Buck/Boost, Heatsink Thermal & Solar Sizing
├── EmbeddedAndSerialScreen.kt    # Real-Time Serial Plotter Terminal & MCU Timer Register Calc
├── MotionAndSensorLabScreen.kt   # Sensor fisik EMF/Vibrometer HP & Kalkulator Stepper/Servo CNC
├── BodeAndQuizScreen.kt          # Grafik Bode Plot Filter Aktif & Kuis Kompetensi Elektronika
└── LabReportGeneratorScreen.kt   # Engine pencetak Laporan Praktikum PDF A4 resmi

## 👨‍💻 Pengembang (Author)

Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** *(Software House — Android Apps, Web Systems, IoT Solutions, AI/ML & Indie Games)*  
🏛️ **Politeknik Caltex Riau** • Pekanbaru, Riau, Indonesia  
🌐 Website: [https://pandakode.com](https://pandakode.com)
