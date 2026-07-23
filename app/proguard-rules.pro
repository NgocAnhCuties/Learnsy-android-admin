# Kotlinx Serialization — giữ nguyên các @Serializable class và companion serializer,
# vì R8 mặc định sẽ xoá field/method nó nghĩ là "không dùng" nhưng thực ra được gọi
# qua reflection lúc encode/decode JSON.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.learnsy.admin.**$$serializer { *; }
-keepclassmembers class com.learnsy.admin.** {
    *** Companion;
}
-keepclasseswithmembers class com.learnsy.admin.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Giữ nguyên toàn bộ data class model dùng để (de)serialize JSON từ Supabase
# (Lesson, Question, Student, ListeningItem, QuizResult...) — field name phải khớp
# đúng key JSON, R8 đổi tên sẽ làm decode lỗi.
-keep class com.learnsy.admin.data.** { *; }

# Supabase-kt / Ktor — thư viện dùng nhiều reflection nội bộ cho request/response.
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Coil (async image loading cho avatar) — giữ nguyên để tránh crash lúc load ảnh.
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp/Ktor tham chiếu tới slf4j logging (optional, không có mặt thật trên Android
# runtime) — báo "missing class" khi R8 phân tích, không phải lỗi thật vì code này
# không bao giờ chạy trên Android. Bỏ qua cảnh báo thay vì keep class không tồn tại.
-dontwarn org.slf4j.**
