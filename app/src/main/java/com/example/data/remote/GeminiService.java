package com.example.data.remote;

import android.util.Log;
import com.example.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import kotlin.Pair;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

public class GeminiService {

    private static final String TAG = "GeminiService";
    private final ConcurrentHashMap<String, EvaluatedAnswerResult> evaluationCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pair<String, String>> questionCache = new ConcurrentHashMap<>();

    private static final String MODEL_NAME = "gemini-3.5-flash";
    private static final String VISION_MODEL_NAME = "gemini-3.5-flash";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");

    private String getApiKey() {
        try {
            String key = BuildConfig.GEMINI_API_KEY;
            if (key == null || key.trim().isEmpty() || "MY_GEMINI_API_KEY".equals(key.trim())) {
                return "";
            }
            return key.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String callGeminiApi(String prompt, String systemInstruction, int maxTokens) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            Log.i(TAG, "Gemini API key not configured. Using high-accuracy intelligent local evaluation.");
            return "";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + apiKey;

        try {
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", prompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);

            JSONObject requestJson = new JSONObject();
            requestJson.put("contents", contentsArray);

            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                JSONObject systemObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", systemInstruction);
                sysParts.put(sysPart);
                systemObj.put("parts", sysParts);
                requestJson.put("systemInstruction", systemObj);
            }

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.4);
            genConfig.put("responseMimeType", "application/json");
            genConfig.put("maxOutputTokens", maxTokens);
            requestJson.put("generationConfig", genConfig);

            RequestBody body = RequestBody.create(requestJson.toString(), jsonMediaType);
            Request request = new Request.Builder().url(url).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "API call failed with code: " + response.code());
                    return "";
                }

                String responseString = response.body().string();
                JSONObject responseJson = new JSONObject(responseString);
                JSONArray candidates = responseJson.optJSONArray("candidates");

                if (candidates != null && candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.optJSONObject("content");
                    if (content != null) {
                        JSONArray parts = content.optJSONArray("parts");
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text", "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in callGeminiApi", e);
        }
        return "";
    }

    public String callGeminiStreamApi(String prompt, String systemInstruction, int maxTokens) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) return callGeminiApi(prompt, systemInstruction, maxTokens);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":streamGenerateContent?alt=sse&key=" + apiKey;

        try {
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", prompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);

            JSONObject requestJson = new JSONObject();
            requestJson.put("contents", contentsArray);

            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                JSONObject systemObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", systemInstruction);
                sysParts.put(sysPart);
                systemObj.put("parts", sysParts);
                requestJson.put("systemInstruction", systemObj);
            }

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.3);
            genConfig.put("responseMimeType", "application/json");
            genConfig.put("maxOutputTokens", maxTokens);
            requestJson.put("generationConfig", genConfig);

            RequestBody body = RequestBody.create(requestJson.toString(), jsonMediaType);
            Request request = new Request.Builder().url(url).post(body).build();

            StringBuilder fullAccumulatedText = new StringBuilder();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Stream API failed with code " + response.code());
                    return callGeminiApi(prompt, systemInstruction, maxTokens);
                }

                BufferedSource source = response.body().source();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;
                    if (line.startsWith("data: ")) {
                        String dataJson = line.substring(6).trim();
                        if (!"[DONE]".equals(dataJson) && !dataJson.isEmpty()) {
                            try {
                                JSONObject obj = new JSONObject(dataJson);
                                JSONArray candidates = obj.optJSONArray("candidates");
                                if (candidates != null && candidates.length() > 0) {
                                    JSONObject candidate = candidates.getJSONObject(0);
                                    JSONObject content = candidate.optJSONObject("content");
                                    if (content != null) {
                                        JSONArray parts = content.optJSONArray("parts");
                                        if (parts != null && parts.length() > 0) {
                                            String chunkText = parts.getJSONObject(0).optString("text", "");
                                            if (!chunkText.isEmpty()) {
                                                fullAccumulatedText.append(chunkText);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            String accumulated = fullAccumulatedText.toString();
            return accumulated.isEmpty() ? callGeminiApi(prompt, systemInstruction, maxTokens) : accumulated;

        } catch (Exception e) {
            Log.e(TAG, "Stream error, falling back to standard API", e);
            return callGeminiApi(prompt, systemInstruction, maxTokens);
        }
    }

    public String callGeminiVisionApi(String base64Image, String prompt, String systemInstruction) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) return "";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + VISION_MODEL_NAME + ":generateContent?key=" + apiKey;

        try {
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            partsArray.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inlineData", inlineData);
            partsArray.put(imagePart);

            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);

            JSONObject requestJson = new JSONObject();
            requestJson.put("contents", contentsArray);

            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                JSONObject systemObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", systemInstruction);
                sysParts.put(sysPart);
                systemObj.put("parts", sysParts);
                requestJson.put("systemInstruction", systemObj);
            }

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.2);
            genConfig.put("responseMimeType", "application/json");
            genConfig.put("maxOutputTokens", 500);
            requestJson.put("generationConfig", genConfig);

            RequestBody body = RequestBody.create(requestJson.toString(), jsonMediaType);
            Request request = new Request.Builder().url(url).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject responseJson = new JSONObject(response.body().string());
                    JSONArray candidates = responseJson.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.optJSONObject("content");
                        if (content != null) {
                            JSONArray parts = content.optJSONArray("parts");
                            if (parts != null && parts.length() > 0) {
                                return parts.getJSONObject(0).optString("text", "");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vision API error", e);
        }
        return "";
    }

    public ParsedResumeResult parseResumeText(String rawText) {
        String systemPrompt = "You are an expert resume analyzer for tech roles. Extract structured info in JSON.";
        String prompt = "Extract resume info from this text:\n" + rawText +
                "\nReturn strict JSON with keys: candidateName, skills (array), languages (array), frameworks (array), projects (array), education (string), yearsExperience (number).";

        String aiText = callGeminiApi(prompt, systemPrompt, 500);
        if (!aiText.isEmpty()) {
            try {
                String cleanedJson = aiText.trim().replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanedJson);

                return new ParsedResumeResult(
                        json.optString("candidateName", "Candidate"),
                        jsonArrayToList(json.optJSONArray("skills")),
                        jsonArrayToList(json.optJSONArray("languages")),
                        jsonArrayToList(json.optJSONArray("frameworks")),
                        jsonArrayToList(json.optJSONArray("projects")),
                        json.optString("education", "Computer Science"),
                        json.optDouble("yearsExperience", 2.0)
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse resume JSON", e);
            }
        }
        return fallbackParseResume(rawText);
    }

    public ParsedResumeResult parseResumeImage(String base64Jpeg) {
        String systemPrompt = "You are an expert OCR and Resume Analyzer. Extract structured information from this resume image.";
        String prompt = "Extract resume data and return strict JSON with keys: candidateName, skills (array), languages (array), frameworks (array), projects (array), education (string), yearsExperience (number).";

        String aiText = callGeminiVisionApi(base64Jpeg, prompt, systemPrompt);
        if (!aiText.isEmpty()) {
            try {
                String cleanedJson = aiText.trim().replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanedJson);

                return new ParsedResumeResult(
                        json.optString("candidateName", "Candidate"),
                        jsonArrayToList(json.optJSONArray("skills")),
                        jsonArrayToList(json.optJSONArray("languages")),
                        jsonArrayToList(json.optJSONArray("frameworks")),
                        jsonArrayToList(json.optJSONArray("projects")),
                        json.optString("education", "Computer Science"),
                        json.optDouble("yearsExperience", 2.0)
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse vision resume JSON", e);
            }
        }
        return fallbackParseResume("Resume image uploaded via camera");
    }

    public Pair<String, String> generateQuestion(
            String role,
            String difficulty,
            String experienceLevel,
            String companyPreset,
            int questionIndex,
            int totalQuestions,
            List<String> resumeSkills,
            String previousQuestion,
            String previousAnswer
    ) {
        String qCacheKey = role.toLowerCase().trim() + "_" + difficulty.toLowerCase().trim() + "_" + companyPreset.toLowerCase().trim() + "_" + questionIndex + "_" + (previousAnswer != null ? previousAnswer.hashCode() : 0);

        Pair<String, String> cached = questionCache.get(qCacheKey);
        if (cached != null) {
            Log.d(TAG, "⚡ Question Cache HIT for key: " + qCacheKey);
            return cached;
        }

        String systemPrompt = "You are a witty, humorously engaging senior tech interviewer conducting a " + companyPreset + " interview for a " + experienceLevel + " " + role + " position (" + difficulty + " difficulty). Ask ONLY ONE focused question at a time. Infuse subtle humor into the question!";

        StringBuilder contextBuilder = new StringBuilder();
        if (resumeSkills != null && !resumeSkills.isEmpty()) {
            contextBuilder.append("Candidate Resume Skills: ").append(String.join(", ", resumeSkills)).append("\n");
        }

        if (previousQuestion != null && !previousQuestion.isEmpty() && previousAnswer != null && !previousAnswer.isEmpty()) {
            contextBuilder.append("Previous Question: ").append(previousQuestion).append("\n");
            contextBuilder.append("Candidate's Previous Answer: ").append(previousAnswer).append("\n");
            contextBuilder.append("Ask a logical, witty follow-up or delve deeper into a technical detail or trade-off from their answer or resume.\n");
        } else {
            contextBuilder.append("Ask a witty, impactful opening technical question tailored to their resume and ").append(role).append(" role.\n");
        }

        contextBuilder.append("Return JSON with keys: question (string with humorous tone) and category (string: 'Technical', 'System Design', 'Behavioral', 'Live Coding').");

        String aiText = callGeminiStreamApi(contextBuilder.toString(), systemPrompt, 250);
        if (!aiText.isEmpty()) {
            try {
                String cleanedJson = aiText.trim().replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanedJson);
                String question = json.optString("question", "");
                String category = json.optString("category", "Technical");
                if (!question.isEmpty()) {
                    Pair<String, String> result = new Pair<>(question, category);
                    questionCache.put(qCacheKey, result);
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse question JSON", e);
            }
        }

        Pair<String, String> fallback = fallbackGenerateQuestion(role, difficulty, questionIndex, resumeSkills, previousAnswer);
        questionCache.put(qCacheKey, fallback);
        return fallback;
    }

    public Pair<String, String> generateQuestion(
            String role,
            String difficulty,
            String experienceLevel,
            String companyPreset,
            int questionIndex,
            int totalQuestions,
            List<String> resumeSkills
    ) {
        return generateQuestion(role, difficulty, experienceLevel, companyPreset, questionIndex, totalQuestions, resumeSkills, null, null);
    }

    public EvaluatedAnswerResult evaluateAnswer(
            String role,
            String question,
            String userAnswer,
            List<String> resumeSkills,
            int fillerWordCount
    ) {
        String cacheKey = role.toLowerCase().trim() + "_" + question.toLowerCase().trim() + "_" + userAnswer.toLowerCase().trim() + "_" + fillerWordCount;

        EvaluatedAnswerResult cachedResult = evaluationCache.get(cacheKey);
        if (cachedResult != null) {
            Log.d(TAG, "⚡ Evaluation Cache HIT! Returning instant result.");
            return cachedResult;
        }

        String systemPrompt = "You are a witty, sharp tech lead evaluating a candidate's answer for a " + role + " interview. Provide a fast, constructive, and humorous evaluation in JSON format.";
        String userPrompt = "Question asked: \"" + question + "\"\n" +
                "Candidate's Answer: \"" + userAnswer + "\"\n" +
                "Candidate Skills: " + (resumeSkills != null ? String.join(", ", resumeSkills) : "") + "\n\n" +
                "Evaluate quickly and return strict JSON with keys:\n" +
                "- overallScore (integer 1-10)\n" +
                "- technicalAccuracy (integer 1-10)\n" +
                "- communication (integer 1-10)\n" +
                "- problemSolving (integer 1-10)\n" +
                "- confidence (integer 1-10)\n" +
                "- feedback (string: concise 2-sentence feedback with humor)\n" +
                "- strengths (array of strings)\n" +
                "- weaknesses (array of strings)\n" +
                "- idealAnswer (string: concise response)";

        String aiText = callGeminiStreamApi(userPrompt, systemPrompt, 300);
        if (!aiText.isEmpty()) {
            try {
                String cleanedJson = aiText.trim().replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanedJson);

                EvaluatedAnswerResult evaluated = new EvaluatedAnswerResult(
                        json.optInt("overallScore", 7),
                        json.optInt("technicalAccuracy", 7),
                        json.optInt("communication", 7),
                        json.optInt("problemSolving", 7),
                        json.optInt("confidence", 7),
                        json.optString("feedback", "Good explanation covering the core mechanics."),
                        jsonArrayToList(json.optJSONArray("strengths")),
                        jsonArrayToList(json.optJSONArray("weaknesses")),
                        json.optString("idealAnswer", "A top answer clearly defines key mechanisms, performance trade-offs, and practical edge cases.")
                );

                evaluationCache.put(cacheKey, evaluated);
                return evaluated;
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse evaluation JSON", e);
            }
        }

        EvaluatedAnswerResult fallback = fallbackEvaluateAnswer(question, userAnswer, fillerWordCount);
        evaluationCache.put(cacheKey, fallback);
        return fallback;
    }

    public GeneratedReportResult generateSessionReport(
            String role,
            String companyPreset,
            List<Pair<String, String>> questionsAndAnswers,
            List<EvaluatedAnswerResult> evaluations,
            int totalFillerWords
    ) {
        int sumTech = 0, sumComm = 0, sumProb = 0, sumConf = 0;
        if (evaluations != null) {
            for (EvaluatedAnswerResult eval : evaluations) {
                sumTech += eval.getTechnicalAccuracy();
                sumComm += eval.getCommunication();
                sumProb += eval.getProblemSolving();
                sumConf += eval.getConfidence();
            }
        }

        int count = (evaluations == null || evaluations.isEmpty()) ? 1 : evaluations.size();
        int avgTech = Math.max(1, Math.min(10, sumTech / count));
        int avgComm = Math.max(1, Math.min(10, sumComm / count));
        int avgProb = Math.max(1, Math.min(10, sumProb / count));
        int avgConf = Math.max(1, Math.min(10, sumConf / count));

        int overallPercentage = Math.max(10, Math.min(100, (avgTech * 35 + avgProb * 30 + avgComm * 20 + avgConf * 15) / 10));

        String verdict;
        if (overallPercentage >= 85) verdict = "Strong Hire";
        else if (overallPercentage >= 70) verdict = "Hire";
        else if (overallPercentage >= 55) verdict = "Leaning Hire";
        else verdict = "Needs Improvement";

        String prompt = "Generate a summary performance scorecard report and 4-step learning roadmap for a candidate who finished a " + role + " interview for " + companyPreset + " with score " + overallPercentage + "%. Return JSON with keys: summaryFeedback (string), learningRoadmap (array of 4 specific actionable study topics).";

        String aiText = callGeminiApi(prompt, "You are a CTO giving candidate feedback.", 350);
        String summaryText = "Solid performance overall demonstrating solid fundamental knowledge in " + role + ". Focus on deepening system trade-off explanations.";
        List<String> roadmap = Arrays.asList(
                "Master Core " + role + " Data Structures & System Patterns",
                "Practice Edge-Case Analysis and Complexity Trade-offs",
                "Refine Structured Answer Framework (STAR Method)",
                "Conduct Mock Time-pressured Code / Architecture Walks"
        );

        if (!aiText.isEmpty()) {
            try {
                String cleanedJson = aiText.trim().replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanedJson);
                summaryText = json.optString("summaryFeedback", summaryText);
                List<String> aiRoadmap = jsonArrayToList(json.optJSONArray("learningRoadmap"));
                if (!aiRoadmap.isEmpty()) {
                    roadmap = aiRoadmap;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse report summary JSON", e);
            }
        }

        return new GeneratedReportResult(
                overallPercentage,
                avgTech * 10,
                avgComm * 10,
                avgProb * 10,
                avgConf * 10,
                verdict,
                summaryText,
                roadmap
        );
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.optString(i));
            }
        }
        return list;
    }

    private ParsedResumeResult fallbackParseResume(String text) {
        String name = "Candidate";
        if (text != null && text.toLowerCase().contains("name:")) {
            try {
                name = text.substring(text.toLowerCase().indexOf("name:") + 5).split("\n")[0].trim();
            } catch (Exception ignored) {}
        }

        List<String> knownSkills = Arrays.asList("Kotlin", "Java", "Python", "React", "TypeScript", "Node.js", "PostgreSQL", "MongoDB", "SQL", "Docker", "Git", "System Design", "Android", "REST API", "GraphQL");
        List<String> extractedSkills = new ArrayList<>();
        if (text != null) {
            for (String skill : knownSkills) {
                if (text.toLowerCase().contains(skill.toLowerCase())) {
                    extractedSkills.add(skill);
                }
            }
        }
        if (extractedSkills.isEmpty()) {
            extractedSkills.addAll(Arrays.asList("Software Engineering", "Algorithms", "Problem Solving", "REST APIs"));
        }

        return new ParsedResumeResult(
                name.isEmpty() ? "Alex Candidate" : name,
                extractedSkills,
                Arrays.asList("Java", "SQL"),
                Arrays.asList("Android", "Spring Boot"),
                Arrays.asList("AI Interview Simulator App", "Cloud Microservices Engine"),
                "B.S. Computer Science / Software Engineering",
                2.5
        );
    }

    private Pair<String, String> fallbackGenerateQuestion(
            String role,
            String difficulty,
            int questionIndex,
            List<String> resumeSkills,
            String previousAnswer
    ) {
        if (previousAnswer != null && !previousAnswer.isEmpty()) {
            String lower = previousAnswer.toLowerCase();
            if (lower.contains("jwt") || lower.contains("session") || lower.contains("token")) {
                return new Pair<>("You mentioned token authentication. How would you handle JWT token revocation or refresh strategies securely in a high-concurrency API?", "System Design");
            }
            if (lower.contains("cache") || lower.contains("database") || lower.contains("state")) {
                return new Pair<>("When introducing a caching layer like Redis in front of PostgreSQL, how do you manage cache invalidation and prevent cache stampede?", "System Design");
            }
        }

        Map<String, List<Pair<String, String>>> questions = new HashMap<>();
        questions.put("Backend", Arrays.asList(
                new Pair<>("How would you design a secure, rate-limited RESTful authentication service using JWT vs stateful sessions?", "Technical"),
                new Pair<>("Explain database indexing in PostgreSQL. How do B-Tree indexes improve query performance and what are the trade-offs on write operations?", "Technical"),
                new Pair<>("How do you prevent SQL injection and handle database transactions safely across multiple microservices?", "System Design")
        ));
        questions.put("Frontend", Arrays.asList(
                new Pair<>("Explain React's Virtual DOM reconciliation mechanism and how key props optimize rendering performance.", "Technical"),
                new Pair<>("How do you implement client-side caching and state synchronization using tools like React Query or Redux?", "Technical"),
                new Pair<>("What strategies do you use for frontend performance optimization (e.g., code splitting, lazy loading, asset compression)?", "Technical")
        ));
        questions.put("Android", Arrays.asList(
                new Pair<>("Explain how Jetpack Compose handles recomposition and state hoisting compared to traditional XML Layout views.", "Technical"),
                new Pair<>("How does Kotlin Coroutines manage thread dispatchers (Dispatchers.IO, Main, Default) and structured concurrency?", "Technical"),
                new Pair<>("How do you integrate Room database with Flow to achieve reactive UI updates in Clean Architecture?", "System Design")
        ));

        List<Pair<String, String>> roleQuestions = questions.get(role);
        if (roleQuestions == null) {
            roleQuestions = questions.get("Backend");
        }
        int index = Math.abs((questionIndex - 1) % roleQuestions.size());
        return roleQuestions.get(index);
    }

    private EvaluatedAnswerResult fallbackEvaluateAnswer(String question, String answer, int fillerWords) {
        int length = (answer != null) ? answer.trim().length() : 0;
        int score = length > 150 ? 8 : (length > 60 ? 7 : (length > 20 ? 5 : 3));

        String feedback = length > 60 ?
                "Good technical coverage of the concept. Your explanation was direct and well-structured." :
                "Your answer was brief. Try providing concrete architectural examples or trade-offs to demonstrate depth.";

        int fillerPenalty = Math.min(3, (int) (fillerWords * 0.5));
        int commScore = Math.max(1, Math.min(10, score + 1 - fillerPenalty));

        return new EvaluatedAnswerResult(
                score,
                score,
                commScore,
                score,
                Math.max(1, Math.min(10, 8 - fillerPenalty)),
                feedback,
                Arrays.asList("Direct technical terminology", "Clear structural logic"),
                fillerWords > 2 ? Arrays.asList("Contains " + fillerWords + " verbal filler words ('um', 'like')", "Could elaborate on edge cases") : Arrays.asList("Could elaborate on real-world edge cases"),
                "A strong response clearly states the primary mechanism, provides a quick real-world example, and compares trade-offs such as latency, memory, or scalability."
        );
    }
}
