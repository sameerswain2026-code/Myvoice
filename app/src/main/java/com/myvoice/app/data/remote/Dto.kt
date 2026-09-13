package com.myvoice.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------- Gemini ----------------

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent> = emptyList(),
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiSystemInstruction(val parts: List<GeminiPart> = emptyList())

@Serializable
data class GeminiContent(val role: String? = null, val parts: List<GeminiPart> = emptyList())

@Serializable
data class GeminiPart(val text: String = "")

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("responseMimeType") val responseMimeType: String? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiErrorBody? = null
)

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null, val finishReason: String? = null)

@Serializable
data class GeminiErrorBody(val code: Int? = null, val message: String? = null, val status: String? = null)

// ---------------- Deepgram ----------------

@Serializable
data class DeepgramResponse(val results: DgResults? = null)

@Serializable
data class DgResults(val channels: List<DgChannel> = emptyList())

@Serializable
data class DgChannel(val alternatives: List<DgAlternative> = emptyList())

@Serializable
data class DgAlternative(val transcript: String = "")

// ---------------- Sarvam ----------------

@Serializable
data class SarvamResponse(val transcript: String? = null)

// ---------------- Tavily ----------------

@Serializable
data class TavilyRequest(
    val query: String,
    @SerialName("search_depth") val searchDepth: String = "basic",
    @SerialName("max_results") val maxResults: Int = 5,
    @SerialName("include_answer") val includeAnswer: Boolean = true
)

@Serializable
data class TavilyResponse(val answer: String? = null, val results: List<TavilyResult> = emptyList())

@Serializable
data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)

// ---------------- Appwrite ----------------

@Serializable
data class AppwriteDoc(
    @SerialName("\$id") val id: String = "",
    val title: String = "",
    val transcript: String = "",
    val reply: String = "",
    val tags: List<String> = emptyList(),
    val createdMs: String = "0",
    val updatedMs: String = "0",
    val favorite: Boolean = false,
    val source: String = "TEXT",
    val sttProvider: String = "",
    val llmModel: String = "",
    val durationMs: String = "0"
)

@Serializable
data class AppwriteDocList(val total: Int = 0, val documents: List<AppwriteDoc> = emptyList())
