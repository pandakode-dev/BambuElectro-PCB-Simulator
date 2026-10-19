<div align="center">

# 🐼⚡ BambuElectro — Smart Electronics, PCB & Circuit Simulator Suite
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
| *(Tarik SS 1)* | *(Tarik SS 2)* | *(Tarik SS 3)* | *(Tarik SS 4)* | *(Tarik SS 5)* |

---

## ✨ 6 Fitur Utama (Killer Engineering Features)

### 1. 🛠️ Interactive Drag & Drop Breadboard Simulator (Wokwi / Tinkercad Style)
* **Visualisasi Komponen Realistis:** Merender bentuk fisik resistor dengan **gelang warna dinamis yang otomatis berubah mengikuti nilai Ohm-nya**, simbol gerbang logika standar IEEE/ANSI (`AND`, `OR`, `NAND`, `XOR`), kubah lampu LED menyala, lilitan induktor, kapasitor, dioda Zener, potensiometer putar, hingga transistor NPN & MOSFET.
* **Live Digital Multimeter & Bench PSU (1.5V – 24V):** Mengukur pembagi tegangan ($V_{out}$), arus cabang (mA), dan disipasi daya (mW) secara *real-time* saat komponen digeser atau diubah nilainya.
* **Project Save/Load & PNG Schematic Exporter:** Simpan proyek rangkaian ke memori perangkat atau bagikan gambar skematik ke WhatsApp/Laporan Praktikum.

### 2. 📈 Digital Storage Oscilloscope, Function Generator & FFT Spectrum
* Simulasi osiloskop dua domain (*Time-Domain Waveform* & *Harmonic Frequency FFT Spectrum dBV*) untuk gelombang **Sinus, Kotak (PWM), Segitiga, Gigi Gergaji, dan Output Filter RC**.
* Dilengkapi **Real Audio Sine Tone Generator (20 Hz – 8 kHz)** menggunakan `AudioTrack` PCM 16-bit untuk menguji *amplifier* atau akustik ruang langsung dari speaker ponsel.

### 3. 🎨 Visual 4/5-Band Resistor & SMD EIA-96 Decoder
* Kalkulator kode gelang warna resistor 4-band dan 5-band presisi tinggi.
* **Dekoder Resistor SMD (3-Digit, 4-Digit, R-Decimal Shunt, & Standar Presisi EIA-96)** serta dekoder kapasitor keramik (`104` ➔ `pF / nF / µF`).

### 4. 📐 PCB Trace Width (IPC-2221) & IoT Deep-Sleep Battery Estimator
* **IPC-2221 PCB Calculator:** Menghitung lebar minimum jalur tembaga PCB (`mm` & `mil`) berdasarkan arus beban (Ampere), ketebalan tembaga ($1\text{ oz} / 2\text{ oz}$), dan batas kenaikan suhu ($\Delta T$).
* **IoT Battery Life Calculator:** Mengestimasi umur baterai (Hari/Bulan) untuk node ESP32/LoRa berdasarkan profil arus *Active Mode (mA)* dan *Deep-Sleep Mode (µA)*.

### 5. ⚡ Hukum Ohm + Rekomendasi Rating Watt Fisik, NE555 & ADC MCU
* Menghitung disipasi panas Watt beserta rekomendasi ukuran fisik resistor ($\frac{1}{4}\text{W}, \frac{1}{2}\text{W}, 1\text{W}, 5\text{W}$ *Wirewound*).
* Kalkulator resolusi digital **ADC Mikrokontroler (10-bit Arduino, 12-bit ESP32, 16-bit ADS1115)**, pembagi tegangan level-shifter `5V ➔ 3.3V`, dan osilator PWM **IC NE555**.

### 6. 🔌 Interactive Hardware Pinout Explorer, I2C Lookup & BOM Exporter (.CSV)
* Inspeksi kaki pin interaktif untuk **ESP32 DevKit V1 (30-Pin), IC NE555, dan Sensor Energi PZEM-004T v3**, lengkap dengan direktori alamat Hexadesimal sensor **I2C**.
* **Bill of Materials (BOM) Cost Estimator:** Rekap anggaran belanja komponen proyek elektronika dan ekspor langsung ke format **Excel (`.CSV`)**.

---

## 🛠️ Tech Stack & Arsitektur Modular

* **Bahasa & UI:** 100% Kotlin + Jetpack Compose (Material Design 3 — Adaptive & Responsive Layout).
* **Custom Vector & Canvas Engine:** 2D `Canvas`, Cubic Bézier Jumper Wires, IEEE Logic Gate Paths, & Dynamic Resistor Color Band Renderer.
* **Audio DSP Engine:** Native Android `AudioTrack` 44.1 kHz PCM Sine Wave Synthesizer.
* **Arsitektur 5-File Modular:**
  1. `ElectroModels.kt` — Skema warna PCB, model data, rumus IPC-2221 & dekoder SMD EIA-96.
  2. `PassiveComponentScreen.kt` — Studio gelang warna resistor, SMD, kapasitor, dan seri/paralel.
  3. `CircuitCalculatorScreen.kt` — Kalkulator Hukum Ohm, LED, Baterai IoT, ADC, PCB Trace, & NE555.
  4. `VirtualSimLabScreen.kt` — Simulator Breadboard Drag-and-Drop, Osiloskop FFT, & Audio Generator.
  5. `PinoutAndBomScreen.kt` & `MainActivity.kt` — Pinout interaktif, tabel I2C, BOM CSV Exporter, & navigasi utama.

---

## 👨‍💻 Pengembang (Author)

Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** *(Software House — Android Apps, Web Systems, IoT Solutions, AI/ML & Indie Games)*  
🏛️ **Politeknik Caltex Riau** • Pekanbaru, Riau, Indonesia  
🌐 Website: [https://pandakode.com](https://pandakode.com)
