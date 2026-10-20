




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
| <img width="1080" height="2400" alt="Visual Resistor   SMD EIA-96" src="https://github.com/user-attachments/assets/98f16480-8fa1-443e-b96b-03d6bbba2ba0" /> | <img width="1080" height="2400" alt="Drag   Drop Breadboard Sim" src="https://github.com/user-attachments/assets/3fc55335-5624-4265-9922-a5e4ae9b3d7d" /> | <img width="1080" height="2400" alt="Digital Oscilloscope   FFT" src="https://github.com/user-attachments/assets/359ddeea-7457-4a6a-8850-1316d4a9c9a2" /> | <img width="1080" height="2400" alt="Kalkulator PCB   Baterai IoT" src="https://github.com/user-attachments/assets/fc0a2b21-779d-4c39-8128-8455d7e79a57" /> | <img width="1080" height="2400" alt="Visual Pinout ESP32   BOM" src="https://github.com/user-attachments/assets/10dcd3e6-d401-4aa4-b606-e9d45dad2bd4" /> |

---

## ✨ Fitur Utama (Killer Engineering Features)

### 1. 🛠️ Interactive Drag-and-Drop Breadboard Simulator (Wokwi & Tinkercad Style)
* **Visualisasi 19 Komponen Realistis:** Merender bentuk fisik resistor dengan **gelang warna dinamis yang otomatis berubah mengikuti nilai Ohm-nya**, simbol gerbang logika standar IEEE/ANSI (`AND`, `OR`, `NAND`, `XOR`), kubah lampu LED menyala, lilitan induktor, kapasitor, dioda Zener, potensiometer putar, hingga transistor NPN & MOSFET.
* **Alat Ukur Interaktif di Kanvas:** Tarik dan pasang **Multimeter Digital (DMM)** atau **Osiloskop Probe (CH1/CH2)** langsung di atas meja kerja, lalu ketuk instrumen tersebut untuk membuka layar ukur tegangan ($V$), arus ($mA$), hambatan ($\Omega$), dan bentuk gelombang sinyal secara *real-time*.
* **Animasi Aliran Elektron & Proteksi Burnout:** Titik-titik cahaya emas mengalir dinamis menyusuri kabel jumper sesuai besar arus (`mA`), disertai efek visual asap dan percikan api (*burnout alert*) jika komponen mengalami kelebihan beban (*overcurrent*).
* **Auto-Generate Sketch Arduino (.INO):** Otomatis menyusun draf kode C++/Arduino berdasarkan komponen dan kabel yang terpasang di kanvas simulasi.
* **Project Save/Load & PNG Exporter:** Simpan rangkaian ke memori perangkat atau ekspor skematik menjadi gambar `.PNG` siap lampirkan ke laporan praktikum.

### 2. 📈 Digital Storage Oscilloscope, Function Generator & FFT Spectrum
* Simulasi osiloskop dual-channel (*Time-Domain Waveform* & *Harmonic Frequency FFT Spectrum dBV*) untuk gelombang **Sinus, Kotak (PWM), Segitiga, Gigi Gergaji, dan Output Filter RC**.
* Dilengkapi **Real Audio Sine Tone Generator (20 Hz – 8 kHz)** menggunakan `AudioTrack` PCM 16-bit untuk menguji *amplifier* atau akustik ruang langsung dari speaker ponsel.

### 3. 🏭 Tab Baru: PLC, PID Controller & Industrial Sensor Lab
* **Simulator Sensor RTD PT100 / NTC & Jembatan Wheatstone:** Hitung perubahan resistansi ($\Omega$) berdasarkan suhu (°C) dan tegangan diferensial Op-Amp Instrumentasi.
* **Industrial 4–20 mA Current Loop Scaler:** Konversi sinyal arus transmiter industri ke nilai proses fisik (PV) dan nilai register ADC PLC Siemens (`0–27648`) atau ESP32 (`0–4095`).
* **Digital Logic Analyzer & Modbus CRC-16:** Visualisasi timing diagram pulsa UART 8N1 serta kalkulator checksum CRC-16 untuk komunikasi serial RS485 / Modbus RTU.
* **PID Controller Step Response Simulator & % Error Calculator:** Simulasi kurva respon sistem kendali Orde-2 berdasarkan tuning $K_p, K_i, K_d$ serta kalkulator galat praktikum teoretis vs pengukuran riil.

### 4. 🎨 Visual Resistor & SMD EIA-96 Decoder
* Kalkulator kode gelang warna resistor 4-band dan 5-band presisi tinggi.
* **Dekoder Resistor SMD (3-Digit, 4-Digit, R-Decimal Shunt, & Standar Presisi EIA-96)** serta dekoder kapasitor keramik (`104` ➔ `pF / nF / µF`).

### 5. 📐 PCB Trace Width (IPC-2221) & IoT Deep-Sleep Battery Estimator
* **IPC-2221 PCB Calculator:** Menghitung lebar minimum jalur tembaga PCB (`mm` & `mil`) berdasarkan arus beban (Ampere), ketebalan tembaga ($1\text{ oz} / 2\text{ oz}$), dan batas kenaikan suhu ($\Delta T$).
* **IoT Battery Life Calculator:** Mengestimasi umur baterai (Hari/Bulan) untuk node ESP32/LoRa berdasarkan profil arus *Active Mode (mA)* dan *Deep-Sleep Mode (µA)*.

### 6. 🔌 Interactive Hardware Pinout Explorer & BOM Exporter (.CSV)
* Inspeksi kaki pin interaktif untuk **10 Board/Sensor/IC Populer**: *ESP32 DevKit V1, Arduino Uno R3, Raspberry Pi Pico, PZEM-004T, DHT22, HC-SR04, L298N, NE555/LM358, dan LoRa SX1278*, lengkap dengan gambar visual di atas dan tabel fungsi setiap kaki secara detail di bawahnya.
* **Bill of Materials (BOM) Cost Estimator:** Rekap anggaran belanja komponen proyek elektronika dan ekspor langsung ke format **Excel (`.CSV`)**.

---

## 🛠️ Stack Teknologi & Arsitektur Modular

* **Bahasa & UI:** 100% Kotlin + Jetpack Compose (Material Design 3 — Adaptive & Responsive Layout).
* **Custom Vector & Canvas Engine:** 2D `Canvas`, Cubic Bézier Jumper Wires, IEEE Logic Gate Paths, & Dynamic Resistor Color Band Renderer.
* **Audio DSP Engine:** Native Android `AudioTrack` 44.1 kHz PCM Sine Wave Synthesizer.
* **Arsitektur 5-File Modular:**
  1. `ElectroModels.kt` — Skema warna PCB, model data, rumus IPC-2221 & dekoder SMD EIA-96.
  2. `PassiveComponentScreen.kt` — Studio gelang warna resistor, SMD, kapasitor, dan seri/paralel.
  3. `CircuitCalculatorScreen.kt` — Kalkulator Hukum Ohm, LED, Baterai IoT, ADC, PCB Trace, & NE555.
  4. `VirtualSimLabScreen.kt` — Simulator Breadboard, Multimeter Interaktif, Osiloskop Dual-Channel CH1/CH2, & Audio Generator.
  5. `PinoutAndBomScreen.kt`, `SensorAndControlScreen.kt`, & `MainActivity.kt` — Pinout lengkap, PLC 4-20mA, K-Map Solver, dan navigasi utama.

---

## 👨‍💻 Pengembang (Author)

Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** *(Software House — Android Apps, Web Systems, IoT Solutions, AI/ML & Indie Games)*  
🏛️ **Politeknik Caltex Riau** • Pekanbaru, Riau, Indonesia  
🌐 Website: [https://pandakode.com](https://pandakode.com)
