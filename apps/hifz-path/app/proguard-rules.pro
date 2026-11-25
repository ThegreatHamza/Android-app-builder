# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class com.hamzaapps.** {
    *** Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.hamzaapps.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Play Billing
-keep class com.android.vending.billing.** { *; }
