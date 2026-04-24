# Anvil Automation Mod - Kurulum Rehberi

## Gereksinimler
- Java 21 (JDK)
- Gradle 8+

---

## Proje Yapısı

```
AnvilAutomationMod/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/main/
    ├── java/com/anvilautomation/
    │   ├── AnvilAutomationMod.java       ← Ana giriş noktası
    │   ├── AnvilAutomationController.java ← Bot mantığı
    │   ├── AnvilAutomationKeys.java       ← Tuş tanımları
    │   ├── gui/
    │   │   ├── BotScreen.java             ← Ana GUI
    │   │   └── ActivationScreen.java      ← Lisans ekranı
    │   └── key/
    │       └── KeyManager.java            ← Lisans sistemi
    └── resources/
        ├── fabric.mod.json
        └── assets/anvilautomation/lang/
            └── en_us.json
```

---

## Derleme Adımları

### 1. Gradle Wrapper İndir
```bash
gradle wrapper --gradle-version 8.8
```

### 2. Bağımlılıkları İndir
```bash
./gradlew dependencies
```

### 3. Modu Derle
```bash
./gradlew build
```

Çıktı: `build/libs/anvil-automation-3.0.0.jar`

---

## Kurulum (Oyuncu için)
1. [Fabric Loader 0.16.5](https://fabricmc.net/use/installer/) kur (Minecraft 1.21.1)
2. [Fabric API 0.102.0](https://modrinth.com/mod/fabric-api) indir → `.minecraft/mods/` klasörüne koy
3. Derlenmiş `.jar` dosyasını `.minecraft/mods/` klasörüne koy
4. Minecraft'ı başlat

---

## Tuşlar (Varsayılan)
| Tuş | Fonksiyon |
|-----|-----------|
| `B` | Menüyü Aç |
| `G` | Botu Başlat |
| `H` | Botu Durdur |
| `J` | Sandık / Örs Kaydet |
| `K` | Output Sandığı Kaydet |
| `N` | Paneli Aç/Kapat |

---

## Kullanım Sırası
1. `B` → Menüyü aç → Lisans anahtarını gir
2. 4 malzeme sandığına bak sırasıyla `J` ile kaydet:
   - 1. sandık: Temiz Diamond Kask
   - 2. sandık: Mending Kitabı
   - 3. sandık: Unbreaking III Kitabı
   - 4. sandık: Blast Protection IV Kitabı
3. Örse bak → `J` ile örsü kaydet
4. Output sandığına bak → `K` ile kaydet
5. `G` ile botu başlat

---

## Lisans Anahtarı Ekleme
`KeyManager.java` dosyasında `VALID_KEYS` setine ekle:
```java
VALID_KEYS.add("Kowalski-YeniAnahtar123");
```
