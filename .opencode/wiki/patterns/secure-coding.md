---
tags:
  - wiki/pattern
  - security
---

# Secure Coding

> **Last verified:** 2026-05-29 | **Verified by:** [analysis]

## Contexto

Este documento recopila los patrones de secure coding encontrados en los repositorios de Ualá (app principal + core library) analizados para una entrevista técnica. Cubre exactamente los temas de la JD: AES/CBC, SHA-256, TLS, obfuscación, almacenamiento encriptado, root detection y binding biométrico-criptográfico.

Cada sección explica **qué es la herramienta, cómo la usa Ualá en producción, y por qué se tomó esa decisión técnica** (incluyendo tradeoffs).

---

## AES/CBC/PKCS7 — Encriptación del PIN con AndroidKeyStore

### Qué es

AES (Advanced Encryption Standard) es un cifrado simétrico por bloques. CBC (Cipher Block Chaining) es un modo de operación que encadena cada bloque con el anterior usando un XOR, requiriendo un IV (Initialization Vector) aleatorio por cada operación de encriptación. PKCS7 es el esquema de padding que completa bloques incompletos.

CBC por sí solo da **confidencialidad** pero no **integridad** (un atacante puede modificar el ciphertext y no lo vas a detectar). Para integridad se necesita authenticated encryption como GCM.

### Cómo lo usa Ualá

**Archivo fuente:** `uala-android-core-main/core/src/main/java/ar/com/bancar/uala/core/utils/KeyStoreManager.java`

```java
// Line 51-54 — Creación del cipher AES/CBC/PKCS7
private void createCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
    mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
}

// Lines 86-93 — Key generation con autenticación biométrica obligatoria
KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setUserAuthenticationRequired(true)    // ← requiere huella/face ID
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

// Lines 116-133 — initCipher con manejo de IV
public boolean initCipher(int mode, IvParameterSpec params) {
    SecretKey key = (SecretKey) mKeyStore.getKey(UALA_PIN_KEY_ALIAS, null);
    if (mode == Cipher.ENCRYPT_MODE) {
        mCipher.init(mode, key);           // IV auto-generado
    } else {
        mCipher.init(mode, key, params);   // decrypt requiere el IV original
    }
}
```

**Flujo completo en `FingerprintHelper.kt`** (orquestador):
1. `initEncryptCipher()` — crea el cipher en modo ENCRYPT, el IV se genera aleatoriamente
2. `savePin(pin)` — encripta con `doFinal()`, extrae el IV del cipher con `getParameters().getParameterSpec(IvParameterSpec::class.java)`, persiste ciphertext + IV en Base64 dentro de SharedPreferences
3. `initDecryptCipher()` — recupera el IV de SharedPreferences, reconstruye `IvParameterSpec`, lo pasa al cipher en modo DECRYPT
4. El cipher ya inicializado se envuelve en `BiometricPrompt.CryptoObject` para que la huella lo autorice

### Por qué CBC y no GCM

**Tradeoff documentado:** Android Keystore en dispositivos pre-Android 10 **no soporta GCM con `setUserAuthenticationRequired(true)`**. La autenticación biométrica requiere que el TEE (Trusted Execution Environment) autorice cada uso de la clave, y en versiones viejas del keystore, GCM no exponía esta capacidad. CBC era el único modo compatible con el requisito de autenticación de usuario.

En Android 10+ se podría migrar a GCM para obtener authenticated encryption, pero requeriría un feature flag por versión de OS.

### Entrevista — qué decir

> "AES/CBC se usa para encriptar el PIN del usuario dentro del AndroidKeyStore. La clave se genera con `setUserAuthenticationRequired(true)`, lo que fuerza que cada uso del cipher pase por el TEE con validación biométrica. El IV se genera aleatorio al encriptar y se persiste en Base64 — necesario para desencriptar en modo CBC. Se eligió CBC sobre GCM por retrocompatibilidad: GCM + autenticación biométrica no funcionaba en Android < 10."

---

## SHA-256 — Hashing de dispositivo y RSA-OAEP

### Qué es

SHA-256 (Secure Hash Algorithm, 256 bits) es una función hash criptográfica unidireccional y determinística. Propiedades clave:
- **Resistencia a preimagen:** dado un hash, es computacionalmente inviable encontrar la entrada
- **Resistencia a colisiones:** es inviable encontrar dos entradas distintas con el mismo hash
- **Determinístico:** misma entrada → mismo hash siempre

SHA-1, su predecesor, se considera roto desde 2017 con el ataque SHAttered (primera colisión práctica encontrada). Cualquier sistema que use SHA-1 para seguridad debe migrar a SHA-256.

### Cómo lo usa Ualá

**Archivo fuente 1:** `uala-android-core-main/core/src/main/java/ar/com/bancar/uala/core/utils/CipherUtils.kt:26-27`

```kotlin
const val RSA = "RSA"
const val TRANSFORMATION_ECB_OAEP_SHA256 = "RSA/ECB/OAEPwithSHA-256andMGF1Padding"
```

**Uso:** RSA-OAEP-SHA256 para encriptación asimétrica. OAEP (Optimal Asymmetric Encryption Padding) usa SHA-256 como función hash interna y MGF1 como generador de máscara. Esto da:
- **Confidencialidad:** los datos se encriptan con la clave pública del backend
- **Integridad:** el padding OAEP incluye verificación de que el ciphertext no fue alterado
- **No deterministicidad:** OAEP introduce aleatoriedad; misma entrada + misma clave = distinto ciphertext cada vez

**Flujo:** el cliente encripta datos sensibles con `encryptRsa(publicKey, TRANSFORMATION_ECB_OAEP_SHA256)` → el backend desencripta con su clave privada.

**Archivo fuente 2:** `uala-android-app-develop/CHANGELOG.md:4158`

```
[TAER-967] - Change algorithm SHA-1 by SHA-256 of deviceId [PR-5508]
```

**Uso 2:** Fingerprinting de dispositivo. Se hashea el deviceId con SHA-256 para generar un identificador unidireccional que:
- No revela el deviceId original
- Es determinístico (mismo dispositivo → mismo hash)
- Resiste colisiones (no se puede fabricar otro deviceId con el mismo hash)
- Reemplazó SHA-1 porque SHA-1 está criptográficamente roto

### Por qué SHA-256 y no SHA-1 o MD5

- **MD5:** roto desde 2004, colisiones triviales, nunca usarlo
- **SHA-1:** roto desde 2017 (SHAttered), Google y CWI encontraron la primera colisión. Riesgo real.
- **SHA-256:** sin ataques prácticos conocidos, estándar de la industria, requerido por compliance (PCI-DSS, FIPS)

### Entrevista — qué decir

> "SHA-256 se usa en dos lugares: RSA-OAEP-SHA256 para encriptar datos sensibles con clave pública antes de enviarlos al backend, y hashing del deviceId como fingerprint unidireccional. SHA-1 se reemplazó por SHA-256 en el deviceId porque SHA-1 está roto desde 2017. SHA-256 es determinístico, resistente a colisiones y estándar en la industria para integridad de datos."

---

## TLS / Network Security Config — Defensa en profundidad del transporte

### Qué es

TLS (Transport Layer Security) es el protocolo que encripta la comunicación entre el cliente y el servidor. Android provee `NetworkSecurityConfig` para definir políticas de red a nivel aplicación:
- Qué autoridades certificadoras (CAs) se confía
- Si se permite tráfico cleartext (HTTP)
- Si se permite traffic de debugging con CAs de usuario (Charles Proxy, mitmproxy)

### Cómo lo usa Ualá

**Archivo fuente 1:** `uala-android-app-develop/app/src/main/res/xml/network_security_config.xml`

```xml
<network-security-config>
    <debug-overrides>
        <trust-anchors>
            <!-- Trust user added CAs while debuggable only -->
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

**Archivo fuente 2:** `uala-android-app-develop/app/src/main/AndroidManifest.xml:55`

```xml
android:networkSecurityConfig="@xml/network_security_config"
```

**Archivo fuente 3:** `uala-android-app-develop/CHANGELOG.md:1405`

```
MEX: Requests to Auth0 use TLS 1.2 or later
```

**Análisis de la configuración:**

1. **Release builds:** heredan el comportamiento default de Android — solo se confía en CAs del sistema (preinstaladas en el dispositivo). Las CAs instaladas por el usuario (que podrían ser maliciosas) no se confían. Tráfico cleartext bloqueado (Android 9+ lo bloquea por defecto).

2. **Debug builds:** el `<debug-overrides>` permite CAs de usuario (Charles Proxy para debugging). Esto no afecta release porque `debug-overrides` solo aplica cuando `android:debuggable="true"`.

3. **Sin excepción global de cleartext:** a diferencia de `uala-android-home-main` que tiene `cleartextTrafficPermitted="true"` incluso en release (⚠️), el app principal no lo permite — toda comunicación es HTTPS.

4. **TLS 1.2+ forzado para Auth0:** las requests de autenticación requieren TLS 1.2 como mínimo, bloqueando TLS 1.0 y 1.1 que tienen vulnerabilidades conocidas (POODLE, BEAST).

### Por qué esto importa

Sin Network Security Config, un atacante con acceso físico al dispositivo puede instalar un certificado raíz malicioso y realizar MITM sobre todo el tráfico HTTPS de la app. Con esta configuración, eso solo es posible en debug builds (que no se distribuyen al público).

### Entrevista — qué decir

> "Network Security Config fuerza solo CAs del sistema en release, bloqueando certificados instalados por el usuario que podrían usarse para MITM. En debug permite CAs de usuario para testing con Charles Proxy. Se exige TLS 1.2+ para Auth0, bloqueando TLS 1.0/1.1 que son vulnerables a POODLE y BEAST. No hay cleartext traffic permitido en release."

---

## ProGuard / R8 — Ofuscación y shrinking

### Qué es

R8 es el ofuscador de Android que reemplazó a ProGuard. Hace tres cosas:

1. **Shrinking (code shrinking):** elimina clases, métodos y campos no usados del APK
2. **Obfuscation:** renombra clases, métodos y campos a nombres cortos e ininteligibles (ej. `a.b.c()`)
3. **Optimization:** aplica optimizaciones de bytecode (inline, dead code elimination, peephole)

Desde el punto de vista de seguridad, la ofuscación hace la ingeniería inversa más difícil y costosa. No es imposible de reverse-engineer, pero eleva la barrera significativamente.

### Cómo lo usa Ualá

**Archivo fuente 1:** `uala-android-app-develop/app/build.gradle:92-93`

```groovy
release {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
}
```

**Archivo fuente 2:** `uala-android-app-develop/app/proguard-rules.pro` (188 líneas)

Reglas clave:

```proguard
# Encrypt
-keep class net.sqlcipher.** {*;}

# Core libraries protegidas de ofuscación excesiva
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**

# Retrofit — las interfaces deben mantener nombres de métodos (reflection)
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# kotlinx.serialization — el serializador usa reflection
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
```

### Por qué ciertas clases se excluyen de la ofuscación

| Librería | Razón para keep rule |
|----------|---------------------|
| SQLCipher | Usa JNI (native code) — la ofuscación de nombres rompe el binding nativo |
| Retrofit | Usa reflection para leer anotaciones HTTP y construir requests dinámicamente |
| kotlinx.serialization | El plugin del compilador genera serializers basados en nombres de clase — si se renombran, no matchean |
| OkHttp + Conscrypt | `-dontwarn` evita warnings por clases opcionales de TLS (Conscrypt/BouncyCastle) |
| Dagger/Hilt | Usa code generation — los nombres de clase generados deben matchear con las interfaces originales |
| Room | Genera implementaciones en compile-time basadas en los nombres de DAOs y entidades |

### Opciones de ofuscación en el proyecto propio vs Ualá

| Config | Ualá app-develop | Este proyecto (`proyecto-android`) |
|--------|-----------------|-------------------------------------|
| `minifyEnabled` (release) | `true` | `true` |
| `shrinkResources` | No habilitado | `true` |
| Reglas ProGuard | 188 líneas (específicas por librería) | 73 líneas |
| `allowBackup` | `false` | ⚠️ `true` |

### Entrevista — qué decir

> "ProGuard/R8 se habilita en release con 188 reglas de keep rules. Hace shrinking, obfuscation y optimization del bytecode. Las keep rules son críticas: Retrofit usa reflection para leer anotaciones HTTP, kotlinx.serialization genera serializers por nombre de clase, SQLCipher usa JNI — si R8 los ofusca, se rompen en runtime. Cada librería que usa reflection o code generation necesita su keep rule."

---

## EncryptedSharedPreferences + SQLCipher — Defensa en profundidad del almacenamiento

### Qué es

En Android el almacenamiento local es un vector de ataque: SharedPreferences se guardan en XML plano en `/data/data/<app>/shared_prefs/` y la base de datos SQLite en `/data/data/<app>/databases/`. En dispositivos rooteados, un atacante puede leer estos archivos directamente.

Ualá implementa **tres capas de encriptación en profundidad**:

```
Capa 1: EncryptedSharedPreferences (AES-256-GCM)  ← guarda la clave de la DB
Capa 2: SQLCipher (AES-256 a nivel SQLite)        ← encripta toda la base de datos
Capa 3: AndroidKeyStore (hardware-backed)          ← protege la master key
```

### Cómo lo usa Ualá

**Archivo fuente:** `uala-android-app-develop/app/src/main/java/ar/com/bancar/uala/data/local/UalaDataBase.kt`

#### Capa 1: EncryptedSharedPreferences con MasterKey del Keystore

```kotlin
// Lines 155-161 — SharedPreferences encriptadas con doble esquema
return EncryptedSharedPreferences.create(
    context,
    BuildConfig.SHARED_PREFS_NAME,  // "ar.com.uala.encrypted-shared_prefs"
    masterKey,                        // MasterKey del AndroidKeyStore
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,    // claves
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM   // valores
)
```

**AES-256-SIV para keys, AES-256-GCM para values:**
- **SIV (Synthetic IV):** determinístico — mismo plaintext key name → mismo ciphertext. Necesario para poder hacer lookup de la key por nombre.
- **GCM (Galois/Counter Mode):** authenticated encryption — da confidencialidad + integridad. Si alguien modifica el ciphertext del value, el authentication tag no matchea y la desencriptación falla.

#### Capa 2: SQLCipher — SQLite encriptado

```groovy
// build.gradle:699 — Dependencia de SQLCipher
implementation "net.zetetic:android-database-sqlcipher:$sqlcipherVersion"
```

SQLCipher reemplaza el SQLite estándar por uno que aplica AES-256 a nivel de página de base de datos. Cada página de 4KB se encripta individualmente con un IV derivado del número de página — esto permite acceso aleatorio sin desencriptar toda la DB.

#### Capa 3: Clave generada en runtime, guardada en capa 1

```kotlin
// Lines 167-171 — Clave AES-256 generada en runtime
private fun generatePassphrase(): ByteArray {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)  // KEY_SIZE = 256
    return keyGenerator.generateKey().encoded
}
```

La passphrase de SQLCipher es una clave AES-256 generada aleatoriamente en runtime. Esta clave se persiste dentro de `EncryptedSharedPreferences` (capa 1), que a su vez está protegida por `MasterKey` en el AndroidKeyStore (capa 3).

**Flujo completo al abrir la app:**
1. AndroidKeyStore entrega `MasterKey` (protegida por hardware si hay TEE)
2. `MasterKey` desencripta `EncryptedSharedPreferences`
3. De ahí se lee la passphrase de SQLCipher (generada en el primer launch)
4. Esa passphrase abre la base de datos SQLCipher

### Por qué tres capas

- **Una sola capa es frágil:** si encontrás la clave del Keystore, tenés todo
- **EncryptedSharedPreferences + SQLCipher:** aunque rompas una capa, la otra protege datos distintos
- **Clave en runtime + persistencia encriptada:** la passphrase de SQLCipher nunca está hardcodeada, se genera fresh en cada instalación, y se persiste ya encriptada

### Entrevista — qué decir

> "El almacenamiento usa defensa en profundidad con tres capas: EncryptedSharedPreferences con AES-256-GCM para valores y SIV para keys, SQLCipher que encripta toda la base de datos SQLite con AES-256 a nivel página, y AndroidKeyStore como raíz de confianza. La passphrase de SQLCipher se genera en runtime con KeyGenerator y se guarda ya encriptada dentro de EncryptedSharedPreferences. AES-256-GCM da authenticated encryption — si alguien modifica el ciphertext, el authentication tag no matchea y la operación falla."

---

## Root Detection — Bloqueo de dispositivos comprometidos

### Qué es

Un dispositivo rooteado permite que cualquier app ejecute código como superusuario (`su`), lo que habilita:
- Leer `/data/data/<app>/` de cualquier aplicación
- Interceptar llamadas al Keystore
- Modificar la app en runtime (Xposed, Frida)
- Bypassear certificate pinning

Root detection intenta identificar si el dispositivo fue modificado para negar el acceso a la app.

### Cómo lo usa Ualá

**Archivo fuente:** `uala-android-app-develop/app/src/main/java/ar/com/bancar/uala/ui/onboarding/OnBoardingActivity.kt:141`

```kotlin
if (BuildConfig.DENY_ROOTED && RootBeer(this).isRooted) {
    // Redirigir a RootedDeviceActivity — acceso bloqueado
}
```

**RootBeer** es una librería open-source que chequea múltiples indicadores:
- Presencia del binario `su` en paths comunes (`/system/bin/su`, `/system/xbin/su`)
- Build tags que contengan "test-keys" (firmware de desarrollo, no producción)
- Presencia de apps de root management (SuperSU, Magisk Manager)
- Directorios y archivos típicos de root (`/data/local/`, `/system/app/Superuser.apk`)
- `ro.debuggable` y `ro.secure` flags del sistema

También se integra en `FingerprintHelper` (core) — **deshabilita la autenticación biométrica si detecta root**, porque en un dispositivo rooteado el Keystore puede estar comprometido.

### Por qué root detection es necesario en una app financiera

Un atacante con root puede:
1. Extraer la base de datos SQLCipher → necesita la passphrase
2. La passphrase está en EncryptedSharedPreferences → necesita la MasterKey
3. La MasterKey está en el AndroidKeyStore → en un dispositivo rooteado, puede extraer las keys del TEE con herramientas como Frida

Root detection no es infalible (Magisk puede ocultarse), pero es una capa más de defensa. Combinado con:
- SafetyNet / Play Integrity
- server-side fraud detection
- No almacenar datos críticos permanentemente en el dispositivo

### Entrevista — qué decir

> "Root detection con RootBeer bloquea el acceso si detecta `su`, build tags de desarrollo, o apps de root management. También deshabilita la biometría en dispositivos rooteados porque el Keystore pierde su garantía de hardware. No es infalible pero es una capa necesaria — combinado con Play Integrity y detección de fraude server-side."

---

## BiometricPrompt + CryptoObject — Vinculación criptográfica de la biometría

### Qué es

`BiometricPrompt` es la API de Android (AndroidX Biometric) para autenticación biométrica (huella, rostro, iris). `BiometricPrompt.CryptoObject` vincula la operación criptográfica a la autenticación: el cipher se inicializa, se envuelve en un `CryptoObject`, y solo después de que el SO valide la biometría se puede ejecutar `doFinal()`.

Esto impide bypass a nivel aplicación: aunque un atacante modifique el código para saltarse la UI de huella, el TEE no libera la clave sin la señal biométrica del sensor.

### Cómo lo usa Ualá

**Archivo fuente 1:** `uala-android-visual-main/visual/src/main/java/ar/com/bancar/uala/visual/ui/confirmation/NewFingerprintConfirmView.kt`

```kotlin
// Line 75-81 — BiometricPrompt con CryptoObject
private fun startBiometricPrompt(cipher: Cipher) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirmá tu identidad")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG  // Clase 3 (TEE/Secure Element)
        )
        .build()

    BiometricPrompt(
        (context as FragmentActivity),
        ContextCompat.getMainExecutor(context),
        getBiometricListener()
    ).authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
}
```

**Archivo fuente 2:** `uala-android-app-develop/app/src/main/java/ar/com/bancar/uala/ui/security/SecurityCodeConfirmFragment.kt`

```kotlin
// Lines 105-114 — Flujo completo
val success = mFingerPrintHelper.initDecryptCipher()  // inicia cipher CBC modo DECRYPT
val decryptCipher = mFingerPrintHelper.currentCipher
fingerprintView?.setCryptoObject(FingerprintManager.CryptoObject(decryptCipher!!))
```

### Flujo completo PIN + huella

```
1. Usuario crea PIN
   → KeyStoreManager genera key AES-256 en AndroidKeyStore (setUserAuthenticationRequired)
   → initEncryptCipher() crea cipher AES/CBC/PKCS7 modo ENCRYPT
   → doFinal(pin) encripta el PIN, IV se persiste en Base64
   → ciphertext + IV guardados en SharedPreferences

2. Usuario quiere autenticarse
   → initDecryptCipher() recupera el IV, crea cipher AES/CBC/PKCS7 modo DECRYPT
   → cipher se envuelve en BiometricPrompt.CryptoObject
   → BiometricPrompt muestra el diálogo de huella
   → TEE valida la huella → libera la key → doFinal() desencripta el PIN
   → PIN desencriptado se envía al backend para autenticación

3. Si el dispositivo tiene root
   → FingerprintHelper deshabilita la biometría directamente
   → Se fuerza autenticación por PIN manual
```

### BIOMETRIC_STRONG vs BIOMETRIC_WEAK

| Clase | Significado | Ejemplos |
|-------|------------|----------|
| `BIOMETRIC_STRONG` (Clase 3) | Autenticación respaldada por hardware (TEE, Secure Element) con tasa de falsa aceptación ≤ 0.002% | Huella capacitiva, Face ID, iris en TEE |
| `BIOMETRIC_WEAK` (Clase 2) | Autenticación sin garantía de hardware, puede correr en el SO principal | Reconocimiento facial con cámara frontal básica |
| `DEVICE_CREDENTIAL` (Clase 1) | PIN/patrón/contraseña del dispositivo | Lock screen |

Ualá usa `BIOMETRIC_STRONG` — solo acepta biometría respaldada por TEE/Secure Element, que es lo requerido para apps financieras.

### Entrevista — qué decir

> "BiometricPrompt se usa con CryptoObject para vincular la operación criptográfica a la autenticación biométrica. La clave AES/CBC está en el AndroidKeyStore con `setUserAuthenticationRequired(true)` — el TEE no la libera sin validación biométrica. Se usa `BIOMETRIC_STRONG` que solo acepta biometría Clase 3 respaldada por hardware. Si se detecta root, la biometría se deshabilita completamente y se fuerza autenticación manual."

---

## Tabla resumen

| Tema de la JD | Dónde está en Ualá | Archivo(s) clave |
|---------------|-------------------|------------------|
| **AES/CBC (Cipher Block Chaining)** | Encriptación del PIN en AndroidKeyStore | `core/.../KeyStoreManager.java` + `core/.../FingerprintHelper.kt` |
| **SHA-256** | RSA-OAEP-SHA256 + hashing de deviceId | `core/.../CipherUtils.kt` + CHANGELOG PR-5508 |
| **TLS / Network Security** | Solo system CAs en release, TLS 1.2+ para Auth0 | `app/.../network_security_config.xml` + `AndroidManifest.xml` |
| **Obfuscation (ProGuard/R8)** | `minifyEnabled true` con 188 keep rules | `app/build.gradle` + `app/proguard-rules.pro` |
| **Encrypted Storage** | EncryptedSharedPrefs + SQLCipher + KeyStore | `app/.../UalaDataBase.kt` |
| **Root Detection** | RootBeer en onboarding + deshabilita biometría | `app/.../OnBoardingActivity.kt` + `core/.../FingerprintHelper.kt` |
| **Biometric + Crypto binding** | `BiometricPrompt.CryptoObject` con `BIOMETRIC_STRONG` | `visual/.../NewFingerprintConfirmView.kt` |

---

## Ver también

- [[patterns/error-handling]] — Manejo de excepciones de red (CertificateException, SSLException)
- [[patterns/room-paging]] — Room + SQLCipher (database encryption layer)
- [[tool/retrofit-setup]] — OkHttp + TLS configuration
- [[processes/tech-defense-guide]] — Guía de defensa técnica para entrevistas
