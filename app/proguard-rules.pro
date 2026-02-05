# --- REGLAS BÁSICAS DE ANDROID ---
# Mantener las clases R (Recursos) para evitar errores de "Resource not found"
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Mantener todos los componentes que declaraste en el Manifest
# (Activities, Services, Receivers, Tiles)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.service.quicksettings.TileService

# --- REGLAS PARA COROUTINES (KOTLIN) ---
# Vital para que no crashee al usar lifecycleScope o Dispatchers.IO
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# --- REGLAS ESPECÍFICAS DE TU APP ---
# Protegemos RootHandler para que R8 no le cambie el nombre a los métodos
# que usas para ejecutar comandos 'su'.
-keep class com.tavataan.bypass.RootHandler { *; }

# Ignorar advertencias de librerías comunes
-dontwarn android.content.**
-dontwarn androidx.**
-dontwarn org.jetbrains.kotlin.**