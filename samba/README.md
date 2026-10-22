# SAMBA — Dynamic Island for Android

Fase 1 (MVP) · Kotlin + Jetpack Compose · minSdk 26 · targetSdk 35

## Isi Fase 1
- Island overlay (pil #08090D dengan garis neon biru → ungu). Ketuk untuk expand, menyusut sendiri setelah 4 detik.
- Toggle aktif/nonaktif lewat foreground service. Notifikasi servis punya tombol "Matikan".
- Alur izin "Tampil di atas aplikasi lain".
- Slider posisi horizontal/vertikal, lebar, dan tinggi. Perubahan langsung terlihat di layar.
- Pengaturan tersimpan lokal (SharedPreferences). Tidak ada data yang dikirim ke mana pun.

## Cara build
1. Android Studio: Open folder ini, tunggu sync, lalu Run.
2. Tanpa PC: upload folder ini ke repositori GitHub, buka tab Actions, jalankan "Build APK", lalu unduh artefak `SAMBA-debug-apk`.
3. Terminal dengan Gradle 8.9 dan JDK 17: `gradle assembleDebug`.

## Cara mengaktifkan di HP
1. Buka SAMBA dan nyalakan toggle Island.
2. Izinkan "Tampil di atas aplikasi lain", lalu kembali ke SAMBA.
3. Geser slider posisi sampai island pas dengan punch-hole kamera.

Mematikan: matikan toggle, atau cabut izin di Pengaturan → Aplikasi → SAMBA.

## Struktur kode
- `SambaStore.kt` — konfigurasi island (StateFlow + penyimpanan lokal)
- `IslandOverlay.kt` — tampilan island (Compose) dan warna brand
- `IslandService.kt` — foreground service yang memasang overlay
- `MainActivity.kt` — layar utama: pratinjau, toggle, izin, slider

## Fase berikutnya
- Fase 2: NotificationListenerService (notifikasi), MediaSession (musik), baterai/charging.
  Di Android 13+, APK yang dipasang di luar Play Store perlu "Izinkan setelan terbatas" (Info aplikasi → menu ⋮) sebelum akses notifikasi bisa diaktifkan.
- Fase 3: Theme Studio, Gaming Mode, penyesuaian notch otomatis.
- Fase 4: uji lintas perangkat, baterai, aksesibilitas, rilis APK.
