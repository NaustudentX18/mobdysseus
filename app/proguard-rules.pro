# Mobdysseus release rules
# Keep Compose runtime annotations and generated code.
-keep class androidx.compose.runtime.** { *; }
-keep class com.jakemalby.odysseusmobile.** { *; }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
