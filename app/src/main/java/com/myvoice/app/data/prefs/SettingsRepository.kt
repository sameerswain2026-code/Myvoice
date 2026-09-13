package com.myvoice.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "myvoice_settings")

enum class SttProvider { DEVICE, DEEPGRAM, SARVAM }

data class AppSettings(
    val sttProvider: SttProvider = SttProvider.DEVICE,
    // Deepgram (cloud STT)
    val deepgramKey: String = "",
    val deepgramModel: String = "nova-2",
    // Sarvam (Indic cloud STT)
    val sarvamKey: String = "",
    val sarvamModel: String = "saaras:v3",
    val sarvamMode: String = "transcribe",
    // Gemini (LLM)
    val geminiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash",
    // Tavily (web search tool)
    val tavilyKey: String = "",
    val webSearchEnabled: Boolean = true,
    // Voice output
    val autoSpeak: Boolean = false,
    // Thinking persona
    val persona: String = "",
    // Appwrite cloud sync
    val appwriteEndpoint: String = "https://cloud.appwrite.io/v1",
    val appwriteProject: String = "",
    val appwriteDatabase: String = "",
    val appwriteCollection: String = "",
    val appwriteKey: String = ""
) {
    val hasGemini: Boolean get() = geminiKey.isNotBlank()
    val hasCloudStt: Boolean get() = (sttProvider == SttProvider.DEEPGRAM && deepgramKey.isNotBlank()) ||
        (sttProvider == SttProvider.SARVAM && sarvamKey.isNotBlank())
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val STT_PROVIDER = stringPreferencesKey("stt_provider")
        val DEEPGRAM_KEY = stringPreferencesKey("deepgram_key")
        val DEEPGRAM_MODEL = stringPreferencesKey("deepgram_model")
        val SARVAM_KEY = stringPreferencesKey("sarvam_key")
        val SARVAM_MODEL = stringPreferencesKey("sarvam_model")
        val SARVAM_MODE = stringPreferencesKey("sarvam_mode")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val TAVILY_KEY = stringPreferencesKey("tavily_key")
        val WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val PERSONA = stringPreferencesKey("persona")
        val APPWRITE_ENDPOINT = stringPreferencesKey("appwrite_endpoint")
        val APPWRITE_PROJECT = stringPreferencesKey("appwrite_project")
        val APPWRITE_DATABASE = stringPreferencesKey("appwrite_database")
        val APPWRITE_COLLECTION = stringPreferencesKey("appwrite_collection")
        val APPWRITE_KEY = stringPreferencesKey("appwrite_key")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            sttProvider = enumOrDefault(p[Keys.STT_PROVIDER], SttProvider.DEVICE),
            deepgramKey = p[Keys.DEEPGRAM_KEY].orEmpty(),
            deepgramModel = p[Keys.DEEPGRAM_MODEL] ?: "nova-2",
            sarvamKey = p[Keys.SARVAM_KEY].orEmpty(),
            sarvamModel = p[Keys.SARVAM_MODEL] ?: "saaras:v3",
            sarvamMode = p[Keys.SARVAM_MODE] ?: "transcribe",
            geminiKey = p[Keys.GEMINI_KEY].orEmpty(),
            geminiModel = p[Keys.GEMINI_MODEL] ?: "gemini-2.0-flash",
            tavilyKey = p[Keys.TAVILY_KEY].orEmpty(),
            webSearchEnabled = p[Keys.WEB_SEARCH_ENABLED] ?: true,
            autoSpeak = p[Keys.AUTO_SPEAK] ?: false,
            persona = p[Keys.PERSONA].orEmpty(),
            appwriteEndpoint = p[Keys.APPWRITE_ENDPOINT] ?: "https://cloud.appwrite.io/v1",
            appwriteProject = p[Keys.APPWRITE_PROJECT].orEmpty(),
            appwriteDatabase = p[Keys.APPWRITE_DATABASE].orEmpty(),
            appwriteCollection = p[Keys.APPWRITE_COLLECTION].orEmpty(),
            appwriteKey = p[Keys.APPWRITE_KEY].orEmpty()
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun save(s: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.STT_PROVIDER] = s.sttProvider.name
            p[Keys.DEEPGRAM_KEY] = s.deepgramKey.trim()
            p[Keys.DEEPGRAM_MODEL] = s.deepgramModel.trim().ifBlank { "nova-2" }
            p[Keys.SARVAM_KEY] = s.sarvamKey.trim()
            p[Keys.SARVAM_MODEL] = s.sarvamModel.trim().ifBlank { "saaras:v3" }
            p[Keys.SARVAM_MODE] = s.sarvamMode
            p[Keys.GEMINI_KEY] = s.geminiKey.trim()
            p[Keys.GEMINI_MODEL] = s.geminiModel.trim().ifBlank { "gemini-2.0-flash" }
            p[Keys.TAVILY_KEY] = s.tavilyKey.trim()
            p[Keys.WEB_SEARCH_ENABLED] = s.webSearchEnabled
            p[Keys.AUTO_SPEAK] = s.autoSpeak
            p[Keys.PERSONA] = s.persona
            p[Keys.APPWRITE_ENDPOINT] = s.appwriteEndpoint.trim().ifBlank { "https://cloud.appwrite.io/v1" }
            p[Keys.APPWRITE_PROJECT] = s.appwriteProject.trim()
            p[Keys.APPWRITE_DATABASE] = s.appwriteDatabase.trim()
            p[Keys.APPWRITE_COLLECTION] = s.appwriteCollection.trim()
            p[Keys.APPWRITE_KEY] = s.appwriteKey.trim()
        }
    }

    suspend fun setAutoSpeak(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = value }
    }
}

private fun enumOrDefault(raw: String?, default: SttProvider): SttProvider =
    raw?.let { r -> SttProvider.entries.firstOrNull { it.name == r } } ?: default
