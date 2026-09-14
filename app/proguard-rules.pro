# BookMyTurf keeps no payment credentials in the client. Add gateway-specific rules here.
# Persisted local models must retain their serialization names across optimized builds.
-keep class com.bookmyturf.app.data.** implements java.io.Serializable { *; }
