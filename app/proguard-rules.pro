# On-device ML Kit text recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Defensive enum keeps (only relevant when R8/minify is enabled)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
