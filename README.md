# Professor Client Mod

## طريقة التثبيت والبناء

### المتطلبات
- Java 21 أو أحدث → https://adoptium.net
- Fabric Loader 0.16+ → https://fabricmc.net/use/installer
- Minecraft 1.21.1

---

### بناء الـ Mod (Windows)

1. افتح PowerShell في مجلد الـ Mod
2. شغّل:
```powershell
.\gradlew.bat build
```
3. الـ JAR سيكون في: `build\libs\professor-client-1.0.0.jar`

---

### بناء الـ Mod (Linux / Mac)

```bash
chmod +x build.sh
./build.sh
```

---

### التثبيت

1. انسخ ملف `.jar` إلى مجلد `.minecraft/mods/`
2. تأكد أن Fabric API مثبت أيضاً
3. شغّل Minecraft 1.21.1 مع Fabric

---

### كيفية الاستخدام

| الزر | الوظيفة |
|------|---------|
| **M** | فتح واجهة Professor Client |
| **ESC** | إغلاق الواجهة |
| زر "إرسال 300 باكت" | إرسال 300 باكت حركة للسيرفر |

---

### المميزات
- واجهة سوداء متحركة مع نجوم لامعة وشهب
- عنوان Professor Client مع توهج متغير الألوان
- زر إرسال 300 باكت للسيرفر
- لا يوقف اللعبة عند فتح الواجهة
