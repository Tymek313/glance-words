-keep class com.google.api.client.json.GenericJson
-keepclasseswithmembers class * extends com.google.api.client.json.GenericJson { *; }
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}