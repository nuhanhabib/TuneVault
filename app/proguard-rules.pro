# yt-dlp bridge: runs an embedded Python and parses its output with Jackson via reflection.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.**
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
