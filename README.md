# MbakAdmin 🤖📱

> **Admin WhatsApp Pintar, Toko Lo Gak Pernah Tidur.**  
> Solusi AI Sales Assistant untuk UMKM Indonesia langsung dari perangkat Android, 100% tanpa root dan tanpa ketergantungan server database produk eksternal.

---

## 🎯 Latar Belakang & Masalah

Sebagian besar UMKM di Indonesia masih mengandalkan WhatsApp pribadi/bisnis untuk memproses pesanan secara manual. Masalah utama:
1. **Slow Response:** Penjual kelelahan membalas ratusan chat pertanyaan yang berulang (cek stok, harga, varian, no rek) saat sedang sibuk produksi, packing, atau istirahat.
2. **Keterbatasan Solusi Eksisting:**
   * *WhatsApp Business Auto-reply:* Pesan kaku, tidak bisa memahami konteks pertanyaan pembeli.
   * *WhatsApp Gateway Cloud (Unofficial API):* Rentan banned nomor massal dan butuh biaya server bulanan yang mahal.
   * *Official WhatsApp Cloud API:* Sistem template berbayar per sesi percakapan, tidak fleksibel untuk UMKM mikro.

---

## 💡 Solusi: MbakAdmin

MbakAdmin adalah aplikasi Android native yang bertindak sebagai "asisten toko pribadi" dengan mengotomasi respon WhatsApp menggunakan kecerdasan buatan (AI):

- **Zero-Root Required:** Menggunakan Android OS native API (`NotificationListenerService` & `RemoteInput`).
- **Background Survival & Guardian Service:** Dilengkapi persistent foreground service (`MbakAdminGuardianService`) serta OEM Auto-Start & Battery Optimization helper untuk memastikan bot tidak dibunuh oleh OS saat layar HP mati (Xiaomi, Oppo, Vivo, Samsung, Infinix).
- **Katalog Produk 100% Lokal:** Data produk, varian, dan aturan toko disimpan di SQLite/Room database lokal di HP penjual. Privasi terjaga dan zero-server database maintenance.
* **Aman dari Banned:** Respon dikirimkan langsung oleh aplikasi resmi WhatsApp di perangkat pengguna sebagai aksi balasan notifikasi resmi.
* **Full Context Handling:** Mengurai pesan bertingkat menggunakan format `NotificationCompat.MessagingStyle` untuk menangkap obrolan panjang tanpa terpotong.
* **Model Bisnis Transparan:** Aplikasi gratis digunakan; biaya hanya dikenakan berbasis penggunaan kuota token AI.

---

## 🏗️ Arsitektur Sistem

```text
[ Pembeli WhatsApp ]
         │ (Kirim chat)
         ▼
[ Aplikasi Resmi WhatsApp di Android ]
         │ (Terbitkan notifikasi)
         ▼
[ MbakAdmin: NotificationListenerService ]
         │ (Ekstrak pesan via NotificationCompat.MessagingStyle)
         ▼
[ MbakAdmin Core Engine ]
         │ (Query katalog & aturan toko)
         ▼
[ Local Room DB (HP Seller) ]
         │ (Konteks produk + chat pembeli)
         ▼
[ AI Gateway / LLM Engine ]
         │ (Generate balasan persuasif gaya "Mbak Admin Olshop")
         ▼
[ MbakAdmin Reply Engine ]
         │ (Kirim via RemoteInput ActionIntent di background)
         ▼
[ WhatsApp Delivery ke Pembeli ]
```

---

## 🛠️ Tech Stack Rencana

* **Platform:** Android Native (Min SDK 26+, Android 8.0+)
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Local Storage:** Android Jetpack Room (SQLite)
* **Background Worker:** Android Service + Coroutines
* **AI Engine:** OpenAI-compatible API Gateway / LLM Proxy

---

## 📄 Lisensi

[MIT License](LICENSE)
