# kotlinx.serialization keeps its generated serializers via the plugin; DTOs are plain @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.agrilink.app.data.api.dto.** { *** Companion; }
-keepclassmembers class com.agrilink.app.data.api.dto.** { *** Companion; }
# Retrofit service interfaces are used through reflection proxies.
-keep,allowobfuscation interface com.agrilink.app.data.api.ApiService
