package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.prefs.AppSettings
import com.myvoice.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Optional two-way sync of thoughts with an Appwrite database over its REST API.
 *
 * Pushes locally-unsynced thoughts, then pulls remote documents that are newer
 * than the local copies. Configuration lives in Settings.
 */
class AppwriteSync(
    private val http: OkHttpClient,
    private val settings: SettingsRepository,
    private val repo: ThoughtRepository
) {

    data class SyncResult(val pushed: Int, val updated: Int, val pulled: Int, val failed: Int) {
        fun summary(): String {
            val base = "Pushed $pushed · Updated $updated · Pulled $pulled"
            return if (failed > 0) "$base · Failed $failed (check config)" else base
        }
    }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val s = settings.current()
        requireAppwriteConfig(s)
        var pushed = 0
        var updated = 0
        var pulled = 0
        var failed = 0

        // ---- Push unsynced thoughts ----
        for (t in repo.unsynced()) {
            try {
                val url = "${s.appwriteEndpoint.trimEnd('/')}/databases/${s.appwriteDatabase}/collections/${s.appwriteCollection}/documents"
                val call = Request.Builder()
                    .url(url)
                    .headers(headers(s))
                    .post(createBody(t.id, docData(t)))
                    .build()
                http.newCall(call).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    when {
                        resp.isSuccessful -> {
                            repo.markSynced(listOf(t.id))
                            pushed++
                        }
                        resp.code == 409 -> {
                            // Document already exists remotely → update it.
                            val patch = Request.Builder()
                                .url("$url/${t.id}")
                                .headers(headers(s))
                                .patch(updateBody(docData(t)))
                                .build()
                            http.newCall(patch).execute().use { r2 ->
                                if (r2.isSuccessful) {
                                    repo.markSynced(listOf(t.id))
                                    updated++
                                } else {
                                    failed++
                                }
                            }
                        }
                        else -> throw httpError(resp.code, raw, "Appwrite")
                    }
                }
            } catch (_: Exception) {
                failed++
            }
        }

        // ---- Pull newer remote documents ----
        try {
            val base = "${s.appwriteEndpoint.trimEnd('/')}/databases/${s.appwriteDatabase}/collections/${s.appwriteCollection}/documents"
            val url = base.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("queries", "[\"limit(100)\"]")
                ?.build()
                ?: throw AssistantException("Invalid Appwrite endpoint: ${s.appwriteEndpoint}")
            val call = Request.Builder().url(url).headers(headers(s)).get().build()
            http.newCall(call).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw httpError(resp.code, raw, "Appwrite")
                val list = appJson.decodeFromString<AppwriteDocList>(raw)
                for (doc in list.documents) {
                    if (doc.id.isBlank()) continue
                    val remote = doc.toThought()
                    val local = repo.byId(doc.id)
                    if (local == null || local.updatedAt < remote.updatedAt) {
                        repo.save(remote)
                        pulled++
                    }
                }
            }
        } catch (_: Exception) {
            // Pull failures are non-fatal; push result is already recorded.
        }

        SyncResult(pushed, updated, pulled, failed)
    }

    private fun requireAppwriteConfig(s: AppSettings) {
        if (s.appwriteProject.isBlank() || s.appwriteDatabase.isBlank() ||
            s.appwriteCollection.isBlank() || s.appwriteKey.isBlank()
        ) {
            throw AssistantException(
                "Cloud sync is not configured — fill in the Appwrite details in Settings → Cloud sync."
            )
        }
    }

    private fun headers(s: AppSettings): okhttp3.Headers = okhttp3.Headers.Builder()
        .add("X-Appwrite-Project", s.appwriteProject.trim())
        .add("X-Appwrite-Key", s.appwriteKey.trim())
        .add("Content-Type", "application/json")
        .build()

    private fun docData(t: Thought): JsonObject = buildJsonObject {
        put("title", JsonPrimitive(t.title))
        put("transcript", JsonPrimitive(t.transcript))
        put("reply", JsonPrimitive(t.reply))
        put("tags", JsonArray(t.tags.map { JsonPrimitive(it) }))
        put("createdMs", JsonPrimitive(t.createdAt.toString()))
        put("updatedMs", JsonPrimitive(t.updatedAt.toString()))
        put("favorite", JsonPrimitive(t.isFavorite))
        put("source", JsonPrimitive(t.source))
        put("sttProvider", JsonPrimitive(t.sttProvider))
        put("llmModel", JsonPrimitive(t.llmModel))
        put("durationMs", JsonPrimitive(t.durationMs.toString()))
    }

    private fun createBody(documentId: String, data: JsonObject) = rawJsonBody(
        buildJsonObject {
            put("documentId", JsonPrimitive(documentId))
            put("data", data)
        }.toString()
    )

    private fun updateBody(data: JsonObject) = rawJsonBody(
        buildJsonObject { put("data", data) }.toString()
    )

    private fun AppwriteDoc.toThought(): Thought = Thought(
        id = id,
        title = title,
        transcript = transcript,
        reply = reply,
        tags = tags,
        createdAt = createdMs.toLongOrNull() ?: 0L,
        updatedAt = updatedMs.toLongOrNull() ?: 0L,
        isFavorite = favorite,
        source = source.ifBlank { "TEXT" },
        sttProvider = sttProvider,
        llmModel = llmModel,
        durationMs = durationMs.toLongOrNull() ?: 0L,
        synced = true
    )
}
