# Preserve all Activities (modify if needed)
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Preserve all Fragments
-keep public class * extends androidx.fragment.app.Fragment

# Preserve classes used in layout XMLs (like Activities and Fragments)
-keepclassmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Firebase (if used)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep your main app package and models
-keep class com.example.simplesocialmediaapp.** { *; }
-dontwarn com.example.simplesocialmediaapp.**

# Preserve annotations
-keepattributes *Annotation*

# Preserve Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Optional: Obfuscate but keep functionality
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
