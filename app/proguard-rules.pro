# Proguard/R8 rules for True IDLE.

# Preserve Kotlin reflection/serialization attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep all models and their member fields/properties completely unobfuscated for robust JSON serialization and save restoration
-keep class com.aistudio.trueidle.model.** { *; }
-keepclassmembers class com.aistudio.trueidle.model.** { *; }
-keepnames class com.aistudio.trueidle.model.** { *; }
-keepclassmembernames class com.aistudio.trueidle.model.** { *; }

# Keep data layer and save store classes
-keep class com.aistudio.trueidle.data.** { *; }
-keepclassmembers class com.aistudio.trueidle.data.** { *; }

# kotlinx.serialization rules
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes and their synthetic generated Companion serializers
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.aistudio.trueidle.**
-keepclassmembers class com.aistudio.trueidle.** {
    static **$* *;
    static <1>$Companion *;
    *** Companion;
}

# Enum rules: keep names and values for enums used across screens and categories
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
