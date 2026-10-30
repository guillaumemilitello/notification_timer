# Keep Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Hilt
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.app.Activity

# Keep kotlinx.serialization models
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
