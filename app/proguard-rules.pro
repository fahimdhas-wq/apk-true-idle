# Proguard rules for True IDLE
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
