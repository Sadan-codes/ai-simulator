package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedResumeResult(
    val candidateName: String,
    val skills: List<String>,
    val languages: List<String>,
    val frameworks: List<String>,
    val projects: List<String>,
    val education: String,
    val yearsExperience: Double
)

data class EvaluatedAnswerResult(
    val overallScore: Int, // 1-10
    val technicalAccuracy: Int, // 1-10
    val communication: Int, // 1-10
    val problemSolving: Int, // 1-10
    val confidence: Int, // 1-10
    val feedback: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val idealAnswer: String
)

data class GeneratedReportResult(
    val overallScore: Int, // 0-100
    val technicalScore: Int,
    val communicationScore: Int,
    val problemSolvingScore: Int,
    val confidenceScore: Int,
    val verdict: String, // "Strong Hire", "Hire", "Needs Improvement"
    val summaryFeedback: String,
    val learningRoadmap: List<String>
)

class GeminiService {

    private val evaluationCache = java.util.concurrent.ConcurrentHashMap<String, EvaluatedAnswerResult>()
    private val questionCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun callGeminiApi(prompt: String, systemInstruction: String? = null, maxTokens: Int = 350): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.w("GeminiService", "API Key is empty. Falling back to local smart AI simulation.")
            return@withContext ""
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)

        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        if (!systemInstruction.isNullOrEmpty()) {
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            systemObj.put("parts", sysParts)
            requestJson.put("systemInstruction", systemObj)
        }

        // Fast & crisp generation config with JSON output
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.4)
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("maxOutputTokens", maxTokens)
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiService", "API call failed with code ${response.code}: $responseString")
                return@withContext ""
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val candidateContent = firstCandidate.optJSONObject("content")
                val parts = candidateContent?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "")
                }
            }
            ""
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in callGeminiApi", e)
            ""
        }
    }

    private suspend fun callGeminiStreamApi(
        prompt: String,
        systemInstruction: String? = null,
        maxTokens: Int = 350,
        onChunk: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext callGeminiApi(prompt, systemInstruction, maxTokens)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=$apiKey"

        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)

        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        if (!systemInstruction.isNullOrEmpty()) {
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            systemObj.put("parts", sysParts)
            requestJson.put("systemInstruction", systemObj)
        }

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.3)
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("maxOutputTokens", maxTokens)
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        val fullAccumulatedText = StringBuilder()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("GeminiService", "Stream API failed with code ${response.code}")
                return@withContext callGeminiApi(prompt, systemInstruction, maxTokens)
            }

            val source = response.body?.source()
            if (source != null) {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val dataJson = line.substring(6).trim()
                        if (dataJson != "[DONE]" && dataJson.isNotEmpty()) {
                            try {
                                val obj = JSONObject(dataJson)
                                val candidates = obj.optJSONArray("candidates")
                                if (candidates != null && candidates.length() > 0) {
                                    val candidate = candidates.getJSONObject(0)
                                    val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        val chunkText = parts.getJSONObject(0).optString("text", "")
                                        if (chunkText.isNotEmpty()) {
                                            fullAccumulatedText.append(chunkText)
                                            onChunk(chunkText)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip unparseable SSE chunk
                            }
                        }
                    }
                }
            }
            val accumulated = fullAccumulatedText.toString()
            if (accumulated.isNotEmpty()) accumulated else callGeminiApi(prompt, systemInstruction, maxTokens)
        } catch (e: Exception) {
            Log.e("GeminiService", "Stream error, falling back to standard call", e)
            callGeminiApi(prompt, systemInstruction, maxTokens)
        }
    }

    private suspend fun callGeminiVisionApi(base64Image: String, prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.w("GeminiService", "API Key is empty. Falling back to local smart AI simulation.")
            return@withContext ""
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        // Text Part
        val textPart = JSONObject()
        textPart.put("text", prompt)
        partsArray.put(textPart)

        // Inline Image Part
        val imagePart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Image)
        imagePart.put("inlineData", inlineData)
        partsArray.put(imagePart)

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)

        val requestJson = JSONObject()
        requestJson.put("contents", contentsArray)

        if (!systemInstruction.isNullOrEmpty()) {
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", systemInstruction)
            sysParts.put(sysPart)
            systemObj.put("parts", sysParts)
            requestJson.put("systemInstruction", systemObj)
        }

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.4)
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiService", "Vision API call failed with code ${response.code}: $responseString")
                return@withContext ""
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val candidateContent = firstCandidate.optJSONObject("content")
                val parts = candidateContent?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "")
                }
            }
            ""
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in callGeminiVisionApi", e)
            ""
        }
    }

    suspend fun parseResumeImage(base64Jpeg: String): ParsedResumeResult {
        val systemPrompt = "You are an expert HR AI Resume Parser. Read and extract key details from this image of a resume into strict JSON format with keys: candidateName (string), skills (array of strings), languages (array of strings), frameworks (array of strings), projects (array of strings), education (string), yearsExperience (number)."
        val userPrompt = "Analyze this resume image and extract candidate name, skills, frameworks, languages, education, and projects into strict JSON."

        val aiText = callGeminiVisionApi(base64Jpeg, userPrompt, systemPrompt)
        if (aiText.isNotEmpty()) {
            try {
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)
                val candidateName = json.optString("candidateName", "Candidate")
                val skills = json.optJSONArray("skills")?.toList() ?: listOf("Software Development", "Problem Solving")
                val languages = json.optJSONArray("languages")?.toList() ?: listOf("Kotlin", "Java", "SQL")
                val frameworks = json.optJSONArray("frameworks")?.toList() ?: listOf("Android Jetpack", "Spring Boot")
                val projects = json.optJSONArray("projects")?.toList() ?: listOf("Mobile Application", "REST API")
                val education = json.optString("education", "B.S. Computer Science")
                val yearsExperience = json.optDouble("yearsExperience", 2.0)

                return ParsedResumeResult(candidateName, skills, languages, frameworks, projects, education, yearsExperience)
            } catch (e: Exception) {
                Log.e("GeminiService", "Failed to parse JSON response for resume image", e)
            }
        }

        return fallbackParseResume("Uploaded Resume Photo")
    }

    suspend fun parseResumeText(rawResumeText: String): ParsedResumeResult {
        val systemPrompt = "You are an expert HR AI Resume Parser. Extract key details from the resume into strict JSON format with keys: candidateName (string), skills (array of strings), languages (array of strings), frameworks (array of strings), projects (array of strings), education (string), yearsExperience (number)."
        val userPrompt = "Parse this resume:\n\n$rawResumeText\n\nReturn ONLY raw JSON with no markdown block ticks if possible."

        val aiText = callGeminiApi(userPrompt, systemPrompt)
        if (aiText.isNotEmpty()) {
            try {
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)
                val candidateName = json.optString("candidateName", "Candidate")
                val skills = json.optJSONArray("skills")?.toList() ?: listOf("Software Development", "Problem Solving")
                val languages = json.optJSONArray("languages")?.toList() ?: listOf("Kotlin", "Java", "SQL")
                val frameworks = json.optJSONArray("frameworks")?.toList() ?: listOf("Android Jetpack", "Spring Boot")
                val projects = json.optJSONArray("projects")?.toList() ?: listOf("Mobile Application", "REST API")
                val education = json.optString("education", "B.S. Computer Science")
                val yearsExperience = json.optDouble("yearsExperience", 2.0)

                return ParsedResumeResult(candidateName, skills, languages, frameworks, projects, education, yearsExperience)
            } catch (e: Exception) {
                Log.e("GeminiService", "Failed to parse JSON response for resume", e)
            }
        }

        // Fallback fallback intelligent extraction if API unavailable or key empty
        return fallbackParseResume(rawResumeText)
    }

    suspend fun generateQuestion(
        role: String,
        difficulty: String,
        experienceLevel: String,
        companyPreset: String,
        questionIndex: Int,
        totalQuestions: Int,
        resumeSkills: List<String>,
        previousQuestion: String? = null,
        previousAnswer: String? = null
    ): Pair<String, String> { // Pair(QuestionText, Category)
        val qCacheKey = "${role.lowercase().trim()}_${difficulty.lowercase().trim()}_${companyPreset.lowercase().trim()}_${questionIndex}_${previousAnswer?.hashCode() ?: 0}"
        questionCache[qCacheKey]?.let { cached ->
            Log.d("GeminiService", "⚡ Question Cache HIT for key: $qCacheKey")
            return cached
        }

        val systemPrompt = "You are a witty, humorously engaging senior tech interviewer conducting a $companyPreset interview for a $experienceLevel $role position ($difficulty difficulty). You love lighthearted developer jokes, witty observations (about coffee, 2 AM debugging, or StackOverflow), while asking sharp, realistic technical questions. Ask ONLY ONE focused question at a time. Infuse subtle humor into the question!"

        val contextBuilder = StringBuilder()
        contextBuilder.append("Candidate Resume Skills: ").append(resumeSkills.joinToString(", ")).append("\n")
        contextBuilder.append("Current Question Number: ").append(questionIndex).append(" of ").append(totalQuestions).append("\n")

        if (!previousQuestion.isNullOrEmpty() && !previousAnswer.isNullOrEmpty()) {
            contextBuilder.append("Previous Question: ").append(previousQuestion).append("\n")
            contextBuilder.append("Candidate's Previous Answer: ").append(previousAnswer).append("\n")
            contextBuilder.append("Ask a logical, witty follow-up or delve deeper into a technical detail or trade-off from their answer or resume.\n")
        } else {
            contextBuilder.append("Ask a witty, impactful opening technical question tailored to their resume and $role role.\n")
        }

        contextBuilder.append("Return JSON with keys: question (string with humorous tone) and category (string: 'Technical', 'System Design', 'Behavioral', 'Live Coding').")

        val aiText = callGeminiStreamApi(contextBuilder.toString(), systemPrompt, maxTokens = 250)
        if (aiText.isNotEmpty()) {
            try {
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)
                val question = json.optString("question", "")
                val category = json.optString("category", "Technical")
                if (question.isNotEmpty()) {
                    val result = Pair(question, category)
                    questionCache[qCacheKey] = result
                    return result
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Failed to parse question JSON", e)
            }
        }

        // Smart fallback questions per role
        val fallback = fallbackGenerateQuestion(role, difficulty, questionIndex, resumeSkills, previousAnswer)
        questionCache[qCacheKey] = fallback
        return fallback
    }

    suspend fun evaluateAnswer(
        role: String,
        question: String,
        userAnswer: String,
        resumeSkills: List<String>,
        fillerWordCount: Int
    ): EvaluatedAnswerResult {
        val cacheKey = "${role.lowercase().trim()}_${question.lowercase().trim()}_${userAnswer.lowercase().trim()}_$fillerWordCount"
        evaluationCache[cacheKey]?.let { cachedResult ->
            Log.d("GeminiService", "⚡ Evaluation Cache HIT! Returning instant result in <1ms.")
            return cachedResult
        }

        val systemPrompt = "You are a witty, sharp tech lead evaluating a candidate's answer for a $role interview. Provide a fast, constructive, and humorous evaluation in JSON format. Add funny dev analogies or lighthearted jokes in the feedback!"
        val userPrompt = """
            Question asked: "$question"
            Candidate's Answer: "$userAnswer"
            Filler Words Detected ("um", "like", etc.): $fillerWordCount
            Candidate Skills: ${resumeSkills.joinToString(", ")}

            Evaluate quickly and return strict JSON with keys:
            - overallScore (integer 1-10)
            - technicalAccuracy (integer 1-10)
            - communication (integer 1-10)
            - problemSolving (integer 1-10)
            - confidence (integer 1-10)
            - feedback (string: concise 2-sentence feedback with a touch of humor)
            - strengths (array of strings)
            - weaknesses (array of strings)
            - idealAnswer (string: 2 sentence ideal response with a witty summary)
        """.trimIndent()

        val aiText = callGeminiStreamApi(userPrompt, systemPrompt, maxTokens = 300)
        if (aiText.isNotEmpty()) {
            try {
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)

                val evaluated = EvaluatedAnswerResult(
                    overallScore = json.optInt("overallScore", 7),
                    technicalAccuracy = json.optInt("technicalAccuracy", 7),
                    communication = json.optInt("communication", 7),
                    problemSolving = json.optInt("problemSolving", 7),
                    confidence = json.optInt("confidence", 7),
                    feedback = json.optString("feedback", "Good response explaining the core concepts."),
                    strengths = json.optJSONArray("strengths")?.toList() ?: listOf("Clear technical terminology", "Direct response"),
                    weaknesses = json.optJSONArray("weaknesses")?.toList() ?: listOf("Could mention edge cases or trade-offs"),
                    idealAnswer = json.optString("idealAnswer", "A top answer clearly defines key mechanisms, performance trade-offs, and practical edge cases.")
                )

                // Store in response cache map for ultra-fast subsequent lookups
                evaluationCache[cacheKey] = evaluated
                return evaluated
            } catch (e: Exception) {
                Log.e("GeminiService", "Failed to parse evaluation JSON", e)
            }
        }

        val fallback = fallbackEvaluateAnswer(question, userAnswer, fillerWordCount)
        evaluationCache[cacheKey] = fallback
        return fallback
    }

    suspend fun generateSessionReport(
        role: String,
        companyPreset: String,
        questionsAndAnswers: List<Pair<String, String>>, // (Question, Answer)
        evaluations: List<EvaluatedAnswerResult>,
        totalFillerWords: Int
    ): GeneratedReportResult {
        val avgTech = evaluations.map { it.technicalAccuracy }.average().toInt().coerceIn(1, 10)
        val avgComm = evaluations.map { it.communication }.average().toInt().coerceIn(1, 10)
        val avgProb = evaluations.map { it.problemSolving }.average().toInt().coerceIn(1, 10)
        val avgConf = evaluations.map { it.confidence }.average().toInt().coerceIn(1, 10)

        val overallPercentage = ((avgTech * 35 + avgProb * 30 + avgComm * 20 + avgConf * 15) / 10).coerceIn(10, 100)

        val verdict = when {
            overallPercentage >= 85 -> "Strong Hire"
            overallPercentage >= 70 -> "Hire"
            overallPercentage >= 55 -> "Leaning Hire"
            else -> "Needs Improvement"
        }

        val prompt = "Generate a summary performance scorecard report and 4-step learning roadmap for a candidate who just finished a $role interview for $companyPreset with an overall score of $overallPercentage%. Return JSON with keys: summaryFeedback (string), learningRoadmap (array of 4 specific actionable study topics)."

        val aiText = callGeminiApi(prompt, "You are a CTO giving post-interview candidate feedback.")
        var summaryText = "Solid performance overall demonstrating solid fundamental knowledge in $role. Focus on deepening system trade-off explanations and reducing verbal fillers."
        var roadmap = listOf(
            "Master Core $role Data Structures & System Patterns",
            "Practice Edge-Case Analysis and Complexity Trade-offs",
            "Refine Structured Answer Framework (STAR / CAR Method)",
            "Conduct Mock Time-pressured Code / Architecture Walks"
        )

        if (aiText.isNotEmpty()) {
            try {
                val cleanedJson = aiText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleanedJson)
                summaryText = json.optString("summaryFeedback", summaryText)
                val aiRoadmap = json.optJSONArray("learningRoadmap")?.toList()
                if (!aiRoadmap.isNullOrEmpty()) {
                    roadmap = aiRoadmap
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Failed to parse report summary JSON", e)
            }
        }

        return GeneratedReportResult(
            overallScore = overallPercentage,
            technicalScore = avgTech * 10,
            communicationScore = avgComm * 10,
            problemSolvingScore = avgProb * 10,
            confidenceScore = avgConf * 10,
            verdict = verdict,
            summaryFeedback = summaryText,
            learningRoadmap = roadmap
        )
    }

    // Helper JSONArray extension
    private fun JSONArray.toList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until this.length()) {
            list.add(this.optString(i))
        }
        return list
    }

    // Fallbacks when API key is not present or offline
    private fun fallbackParseResume(text: String): ParsedResumeResult {
        val name = if (text.contains("Name:", ignoreCase = true)) {
            text.substringAfter("Name:").substringBefore("\n").trim()
        } else "Candidate"

        val extractedSkills = mutableListOf<String>()
        val knownSkills = listOf("Kotlin", "Java", "Python", "React", "TypeScript", "Node.js", "PostgreSQL", "MongoDB", "SQL", "Docker", "Git", "System Design", "Android", "REST API", "GraphQL")
        for (skill in knownSkills) {
            if (text.contains(skill, ignoreCase = true)) {
                extractedSkills.add(skill)
            }
        }
        if (extractedSkills.isEmpty()) {
            extractedSkills.addAll(listOf("Software Engineering", "Algorithms", "Problem Solving", "REST APIs"))
        }

        return ParsedResumeResult(
            candidateName = if (name.isEmpty()) "Alex Candidate" else name,
            skills = extractedSkills,
            languages = extractedSkills.filter { it in listOf("Kotlin", "Java", "Python", "TypeScript", "SQL") },
            frameworks = extractedSkills.filter { it in listOf("React", "Node.js", "Android", "FastAPI") },
            projects = listOf("AI Interview Simulator App", "Cloud Microservices Engine"),
            education = "B.S. Computer Science / Software Engineering",
            yearsExperience = 2.5
        )
    }

    private fun fallbackGenerateQuestion(
        role: String,
        difficulty: String,
        questionIndex: Int,
        resumeSkills: List<String>,
        previousAnswer: String?
    ): Pair<String, String> {
        val mainSkill = resumeSkills.firstOrNull() ?: role

        if (!previousAnswer.isNullOrEmpty()) {
            if (previousAnswer.contains("JWT", ignoreCase = true) || previousAnswer.contains("session", ignoreCase = true) || previousAnswer.contains("token", ignoreCase = true)) {
                return Pair("You mentioned token authentication. How would you handle JWT token revocation or refresh strategies securely in a high-concurrency API?", "System Design")
            }
            if (previousAnswer.contains("cache", ignoreCase = true) || previousAnswer.contains("database", ignoreCase = true) || previousAnswer.contains("state", ignoreCase = true)) {
                return Pair("When introducing a caching layer like Redis in front of PostgreSQL, how do you manage cache invalidation and prevent cache stampede?", "System Design")
            }
        }

        val questions = mapOf(
            "Frontend" to listOf(
                "Explain React's Virtual DOM reconciliation mechanism and how key props optimize rendering performance." to "Technical",
                "How do you implement client-side caching and state synchronization using tools like React Query or Redux?" to "Technical",
                "What strategies do you use for frontend performance optimization (e.g., code splitting, lazy loading, asset compression)?" to "Technical"
            ),
            "Backend" to listOf(
                "How would you design a secure, rate-limited RESTful authentication service using JWT vs stateful sessions?" to "Technical",
                "Explain database indexing in PostgreSQL. How do B-Tree indexes improve query performance and what are the trade-offs on write operations?" to "Technical",
                "How do you prevent SQL injection and handle database transactions safely across multiple microservices?" to "System Design"
            ),
            "Data Science" to listOf(
                "Explain the trade-off between Bias and Variance in machine learning models and how regularization mitigates overfitting." to "Technical",
                "How do you handle missing data and feature scaling in large tabular datasets before model training?" to "Technical",
                "Describe how transformer-based language models use self-attention mechanisms to process contextual sequences." to "Technical"
            ),
            "Android" to listOf(
                "Explain how Jetpack Compose handles recomposition and state hoisting compared to traditional XML Layout views." to "Technical",
                "How does Kotlin Coroutines manage thread dispatchers (Dispatchers.IO, Main, Default) and structured concurrency?" to "Technical",
                "How do you integrate Room database with Flow to achieve reactive UI updates in Clean Architecture?" to "System Design"
            )
        )

        val roleQuestions = questions[role] ?: questions["Backend"]!!
        val index = (questionIndex - 1) % roleQuestions.size
        return roleQuestions[index]
    }

    private fun fallbackEvaluateAnswer(question: String, answer: String, fillerWords: Int): EvaluatedAnswerResult {
        val length = answer.trim().length
        val score = when {
            length > 150 -> 8
            length > 60 -> 7
            length > 20 -> 5
            else -> 3
        }

        val feedback = if (length > 60) {
            "Good technical coverage of the concept. Your explanation was direct and well-structured."
        } else {
            "Your answer was brief. Try providing concrete architectural examples or trade-offs to demonstrate depth."
        }

        val fillerPenalty = (fillerWords * 0.5).toInt().coerceAtMost(3)
        val commScore = (score + 1 - fillerPenalty).coerceIn(1, 10)

        return EvaluatedAnswerResult(
            overallScore = score,
            technicalAccuracy = score,
            communication = commScore,
            problemSolving = score,
            confidence = (8 - fillerPenalty).coerceIn(1, 10),
            feedback = feedback,
            strengths = listOf("Direct technical terminology", "Clear structural logic"),
            weaknesses = if (fillerWords > 2) listOf("Contains $fillerWords verbal filler words ('um', 'like')", "Could elaborate on edge cases") else listOf("Could elaborate on real-world edge cases"),
            idealAnswer = "A strong response clearly states the primary mechanism, provides a quick real-world example, and compares trade-offs such as latency, memory, or scalability."
        )
    }
}
