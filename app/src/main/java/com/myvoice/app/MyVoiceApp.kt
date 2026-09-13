package com.myvoice.app

import android.app.Application
import android.content.Context
import com.myvoice.app.audio.AudioRecorder
import com.myvoice.app.data.db.AppDatabase
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.data.remote.AppwriteSync
import com.myvoice.app.data.remote.DeepgramClient
import com.myvoice.app.data.remote.GeminiClient
import com.myvoice.app.data.remote.SarvamClient
import com.myvoice.app.data.remote.TavilyClient
import com.myvoice.app.domain.ThinkingEngine
import com.myvoice.app.domain.ThoughtMetadataGenerator
import com.myvoice.app.speech.DeviceSpeechRecognizer
import com.myvoice.app.speech.TtsManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container. Everything is created once and shared by the app.
 */
class AppContainer(context: Context) {

    val settingsRepository = SettingsRepository(context)
    val database = AppDatabase.build(context)
    val thoughtRepository = ThoughtRepository(database.thoughtDao())

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val geminiClient = GeminiClient(http, settingsRepository)
    val deepgramClient = DeepgramClient(http)
    val sarvamClient = SarvamClient(http)
    val tavilyClient = TavilyClient(http)
    val appwriteSync = AppwriteSync(http, settingsRepository, thoughtRepository)

    val thinkingEngine = ThinkingEngine(geminiClient, tavilyClient, settingsRepository)
    val metadataGenerator = ThoughtMetadataGenerator(geminiClient)

    val audioRecorder = AudioRecorder(context)
    val deviceRecognizer = DeviceSpeechRecognizer(context)
    val ttsManager = TtsManager(context)
}

class MyVoiceApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
