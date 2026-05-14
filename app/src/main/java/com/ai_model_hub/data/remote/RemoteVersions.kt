package com.ai_model_hub.data.remote

data class VersionMapping(
    val from: Int?,
    val to: Int?,
    val allowlist: String,
) {
    fun matches(versionCode: Int): Boolean {
        val afterStart = from == null || versionCode >= from
        val beforeEnd = to == null || versionCode <= to
        return afterStart && beforeEnd
    }
}

data class RemoteVersions(
    val mappings: List<VersionMapping>,
) {
    fun resolveAllowlist(versionCode: Int): String? =
        mappings.firstOrNull { it.matches(versionCode) }?.allowlist
}
