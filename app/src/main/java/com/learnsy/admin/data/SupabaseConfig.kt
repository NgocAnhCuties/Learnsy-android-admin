package com.learnsy.admin.data

import com.learnsy.admin.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage

// Key nạp qua BuildConfig (local.properties -> build.gradle.kts),
// không cần AES-GCM decrypt như bản web (đó là để giấu key khỏi client JS,
// native app không cần vì key đã nằm trong binary, không lộ qua network tab).
object SupabaseConfig {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPA_URL,
        supabaseKey = BuildConfig.SUPA_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Functions)
        install(Storage)
    }

    // Tương đương ADMIN_API_KEY web — dùng cho header x-admin-secret
    // khi gọi Edge Function student-set-password.
    const val ADMIN_API_KEY: String = BuildConfig.ADMIN_API_KEY
}
