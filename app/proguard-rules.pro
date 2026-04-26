# Add project specific ProGuard rules here.

# LiteRT LM JNI callback layer: the native library uses hardcoded method names to call
# back into Java via GetMethodID/CallIntMethodV. If R8 renames these, GetMethodID returns
# null and ART aborts with SIGABRT. Keep the entire package to prevent this.
-keep class com.google.ai.edge.litertlm.** { *; }
