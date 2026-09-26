# Server classes the host mode (server-core on Netty) references only on a desktop JVM: tcnative (OpenSSL), JFR,
# log4j, JMX. Android has none of them and the code paths that use them are not reached on the phone.
-dontwarn com.sun.management.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.ldap.**
-dontwarn jdk.jfr.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**

# Netty finds its own classes, fields and methods by name at runtime (Unsafe field offsets, method handles such as
# `acquireFenceFallback`, class lookups): whatever R8 renames or drops fails only when the host starts. Kept whole.
-keep class io.netty.** { *; }
# Netty refuses a handler added to several pipelines unless it carries @Sharable, which it reads at runtime; Ktor's
# own handlers need the annotation kept too.
-keepattributes RuntimeVisibleAnnotations
-keep @io.netty.channel.ChannelHandler$Sharable class *

# Ktor's server side finds its own members by name too: with it shrunk, a WebSocket upgrades (101) and then its
# incoming channel closes at once, so every client reconnects in a loop.
-keep class io.ktor.server.** { *; }

# The host verifies its JWTs with auth0 java-jwt, which parses them through Jackson databind: both work by reflection
# on their own classes.
-keep class com.auth0.jwt.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.ext.**

# Optional dependencies of the Netty codecs kept above (compression, marshalling, protobuf, BlockHound, GraalVM):
# the host uses none of them.
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn io.netty.pkitesting.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.**
-dontwarn org.jboss.marshalling.**
-dontwarn org.osgi.annotation.bundle.**
-dontwarn reactor.blockhound.**
