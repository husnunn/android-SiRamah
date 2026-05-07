# Task: Sesuaikan UI Android Rohmah App agar Mendukung Tema Dark Mode

Kamu bekerja pada project Android Kotlin `com.gridy.rohmahapp`.

## Tujuan

Sesuaikan tampilan aplikasi agar mendukung **tema dark mode** dengan rapi, konsisten, dan tidak merusak flow existing.

Aplikasi harus bisa tampil baik pada:

- Light mode
- Dark mode
- System default mode

Jangan membuat ulang arsitektur aplikasi.
Jangan membuat ulang screen dari nol.
Jangan mengubah logic bisnis.
Fokus hanya pada penyesuaian UI, warna, theme, style, drawable, dan komponen visual.

---

## Prinsip Utama

Saya ingin semua warna UI mengikuti theme, bukan hardcoded langsung di layout.

Hindari warna seperti ini di XML:

```xml
android:textColor="#000000"
android:background="#FFFFFF"
app:cardBackgroundColor="#FFFFFF"
Ganti dengan warna berbasis resource/theme seperti:
android:textColor="?attr/colorOnSurface"
android:background="?attr/colorSurface"
app:cardBackgroundColor="?attr/colorSurface"
Atau gunakan color resource semantic seperti:
@color/text_primary
@color/text_secondary
@color/background
@color/surface
@color/border
@color/primary

Scope Pekerjaan

Sesuaikan dark theme untuk seluruh UI yang relevan, terutama:
- Activity
- Fragment
- BottomSheet
- Dialog
- RecyclerView item layout
- CardView / MaterialCardView
- TextView
- Button
- EditText / TextInputLayout
- Toolbar / AppBar
- Navigation / menu
- Background screen
- Drawable shape
- Divider / border
- Icon tint
- Empty state
- Loading state

File yang Perlu Dicek

Cek dan sesuaikan file berikut jika ada:
/app/src/main/res/values/colors.xml
/app/src/main/res/values-night/colors.xml
/app/src/main/res/values/themes.xml
/app/src/main/res/values-night/themes.xml
/app/src/main/res/values/styles.xml
/app/src/main/res/drawable/
/app/src/main/res/layout/
/app/src/main/res/menu/
/app/src/main/res/navigation/

Jika project memakai Material Components, pastikan theme menggunakan parent Material yang sesuai.
Requirement Theme

Pastikan tersedia konfigurasi warna untuk light dan dark mode.

Light Mode

Contoh semantic color:
<!-- File: /app/src/main/res/values/colors.xml -->

<color name="color_primary">#2563EB</color>
<color name="color_on_primary">#FFFFFF</color>

<color name="color_background">#F8FAFC</color>
<color name="color_on_background">#0F172A</color>

<color name="color_surface">#FFFFFF</color>
<color name="color_on_surface">#0F172A</color>

<color name="color_text_primary">#0F172A</color>
<color name="color_text_secondary">#64748B</color>
<color name="color_border">#E2E8F0</color>
<color name="color_error">#DC2626</color>

Dark Mode

Contoh semantic color:
<!-- File: /app/src/main/res/values-night/colors.xml -->

<color name="color_primary">#60A5FA</color>
<color name="color_on_primary">#0F172A</color>

<color name="color_background">#0F172A</color>
<color name="color_on_background">#F8FAFC</color>

<color name="color_surface">#1E293B</color>
<color name="color_on_surface">#F8FAFC</color>

<color name="color_text_primary">#F8FAFC</color>
<color name="color_text_secondary">#CBD5E1</color>
<color name="color_border">#334155</color>
<color name="color_error">#F87171</color>

Warna boleh disesuaikan dengan branding existing project, tapi harus tetap readable di dark mode.
Requirement themes.xml

Pastikan theme memakai warna semantic.

Contoh:
<!-- File: /app/src/main/res/values/themes.xml -->

<style name="Theme.RohmahApp" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="colorPrimary">@color/color_primary</item>
    <item name="colorOnPrimary">@color/color_on_primary</item>

    <item name="android:colorBackground">@color/color_background</item>
    <item name="colorSurface">@color/color_surface</item>
    <item name="colorOnSurface">@color/color_on_surface</item>

    <item name="android:navigationBarColor">@color/color_background</item>
    <item name="android:statusBarColor">@color/color_background</item>
    <item name="android:windowLightStatusBar">true</item>
</style>

Untuk values-night/themes.xml, pastikan status bar icon tidak salah warna.

Contoh:
<!-- File: /app/src/main/res/values-night/themes.xml -->

<style name="Theme.RohmahApp" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="colorPrimary">@color/color_primary</item>
    <item name="colorOnPrimary">@color/color_on_primary</item>

    <item name="android:colorBackground">@color/color_background</item>
    <item name="colorSurface">@color/color_surface</item>
    <item name="colorOnSurface">@color/color_on_surface</item>

    <item name="android:navigationBarColor">@color/color_background</item>
    <item name="android:statusBarColor">@color/color_background</item>
    <item name="android:windowLightStatusBar">false</item>
</style>

Sesuaikan parent theme dengan project existing jika berbeda.
Requirement Layout XML

Cari semua layout XML yang masih memakai hardcoded color.

Contoh yang perlu diganti:
android:background="#FFFFFF"
android:textColor="#000000"
android:textColor="#888888"
app:cardBackgroundColor="#FFFFFF"
app:strokeColor="#DDDDDD"
android:tint="#000000"

Ganti menjadi:
android:background="@color/color_background"
android:textColor="@color/color_text_primary"
app:cardBackgroundColor="@color/color_surface"
app:strokeColor="@color/color_border"
app:tint="@color/color_text_primary"
Atau gunakan theme attribute jika cocok:
android:background="?attr/colorSurface"
android:textColor="?attr/colorOnSurface"
Requirement Drawable

Cek drawable shape yang menggunakan warna hardcoded.

Contoh buruk:
<solid android:color="#FFFFFF" />
<stroke android:width="1dp" android:color="#E0E0E0" />
Ganti menjadi:
<solid android:color="@color/color_surface" />
<stroke android:width="1dp" android:color="@color/color_border" />
Pastikan drawable untuk button, card, input, dan background tetap terlihat jelas di dark mode.
Requirement Text

Pastikan semua teks readable.

Gunakan kategori:
Text utama      → color_text_primary
Text secondary  → color_text_secondary
Text disabled   → warna disabled / alpha
Text error      → color_error
Jangan gunakan warna abu-abu terlalu gelap di dark mode karena sulit dibaca.

Requirement Card dan RecyclerView

Untuk item card
Background card       → color_surface
Text utama            → color_text_primary
Text pendukung        → color_text_secondary
Border / divider      → color_border
Icon                  → tint sesuai context
Pastikan RecyclerView item tidak memakai background putih hardcoded.

Requirement Dialog dan BottomSheet

Pastikan dialog dan bottom sheet mengikuti dark mode.

Yang perlu dicek:
- background dialog
- title text
- body text
- button text
- input field
- divider
- icon
Background dialog/bottomsheet sebaiknya memakai:
@color/color_surface
Text memakai:
@color/color_text_primary
@color/color_text_secondary

Requirement Input

Untuk EditText/TextInputLayout:
Background input   → color_surface atau transparent sesuai style
Text color         → color_text_primary
Hint color         → color_text_secondary
Stroke/border      → color_border
Error color        → color_error
Pastikan input tetap terlihat di dark mode.

Requirement Button

Untuk button primary:
Background  → color_primary
Text        → color_on_primary
Untuk button secondary/outline:
Border      → color_border
Text        → color_text_primary atau color_primary
Background  → transparent atau color_surface
Jangan gunakan background putih hardcoded untuk button.

Requirement Icon

Cek icon ImageView / Material Icon / Vector Drawable.

Jika icon adalah icon UI, tint harus menyesuaikan theme:
app:tint="@color/color_text_primary"
Untuk icon secondary:
app:tint="@color/color_text_secondary"
Untuk icon primary:
app:tint="@color/color_primary"
Jangan biarkan icon hitam hardcoded di dark mode.

Requirement Status Bar dan Navigation Bar

Pastikan status bar dan navigation bar sesuai tema.

Light mode:

background terang
icon gelap

Dark mode:

background gelap
icon terang

Jika ada kode Kotlin yang mengatur status bar secara manual, sesuaikan agar support dark mode.

Jangan Dilakukan

Jangan lakukan hal berikut:
- Jangan membuat ulang UI dari nol
- Jangan mengubah logic ViewModel
- Jangan mengubah Repository
- Jangan mengubah API
- Jangan menghapus layout existing
- Jangan hardcode warna baru langsung di layout
- Jangan hanya memperbaiki satu screen saja jika ada warna global yang bermasalah
- Jangan memakai warna putih sebagai default background semua komponen
- Jangan memakai warna hitam sebagai default text semua komponen

Expected Result

Setelah task selesai:
- Aplikasi support light dan dark mode
- Tidak ada text gelap di background gelap
- Tidak ada background putih mencolok di dark mode
- Card, dialog, bottomsheet, input, button, icon tetap readable
- Warna menggunakan resource/theme, bukan hardcoded
- Logic aplikasi tidak berubah
- Build project sukses

Output yang Harus Dilaporkan

Setelah implementasi selesai, jelaskan:
1. File apa saja yang diubah
2. Warna semantic apa saja yang ditambahkan
3. Layout mana saja yang diperbaiki
4. Drawable mana saja yang diperbaiki
5. Apakah ada hardcoded color yang masih tersisa
6. Cara test light mode dan dark mode
7. TODO jika masih ada screen yang perlu dicek manual
