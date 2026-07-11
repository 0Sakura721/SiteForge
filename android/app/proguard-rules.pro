# SiteForge ProGuard Rules

# Keep NanoHTTPD
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# Keep Room entities
-keep class com.siteforge.app.data.model.** { *; }

# Keep data classes
-keepattributes *Annotation*
-keepattributes Signature
