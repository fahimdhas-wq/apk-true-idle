# Proguard/R8 rules for True IDLE.

# kotlinx.serialization generates a KSerializer for every @Serializable class and looks it up
# through the companion object. Keep the generated serializers plus the classes that hold them.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.aistudio.trueidle.**
-keepclassmembers class com.aistudio.trueidle.** {
    static **$* *;
    static <1>$Companion *;
    *** Companion;
}
-keepclasseswithmembers class com.aistudio.trueidle.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The save model is persisted by name, so never strip its members.
-keep,includedescriptorclasses class com.aistudio.trueidle.model.** { *; }
