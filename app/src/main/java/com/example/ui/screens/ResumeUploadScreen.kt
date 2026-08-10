package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ResumeProfileEntity
import com.example.ui.MainViewModel
import org.json.JSONArray
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeUploadScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resumes by viewModel.allResumes.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsingResume.collectAsStateWithLifecycle()

    var resumeText by remember {
        mutableStateOf(
            """
            Alex Candidate
            Senior Software Engineer
            Email: alex@example.com | Phone: (555) 019-2834
            
            SUMMARY:
            Full-stack engineer with 3+ years of experience building scalable web applications & REST microservices. Skilled in React, Node.js, Kotlin, Python, and PostgreSQL.
            
            SKILLS:
            Languages: Kotlin, Python, JavaScript, TypeScript, SQL
            Frameworks: React, Jetpack Compose, FastAPI, Node.js, Spring Boot
            Databases & Tools: PostgreSQL, MongoDB, Docker, Git, Redis, System Design
            
            EXPERIENCE:
            Full-Stack Developer @ Tech Corp (2022 - Present)
            - Engineered high-throughput REST APIs using FastAPI and PostgreSQL handling 500k daily requests.
            - Built responsive dashboard web UI in React & TypeScript.
            
            EDUCATION:
            B.S. in Computer Science, State University
            """.trimIndent()
        )
    }

    // File Picker for Phone Documents (PDF, TXT, DOC)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (text, base64Img) = readUriContent(context, uri)
            if (!text.isNullOrBlank()) {
                resumeText = text
                Toast.makeText(context, "Loaded resume text from document!", Toast.LENGTH_SHORT).show()
            } else if (!base64Img.isNullOrBlank()) {
                viewModel.parseAndSaveResumeImage(base64Img, "Selected Resume File") {
                    Toast.makeText(context, "Parsed resume image from phone!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Could not extract text from selected file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Launcher for Taking Photo of Resume
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val base64Jpeg = bitmapToBase64(bitmap)
            viewModel.parseAndSaveResumeImage(base64Jpeg, "Photo Resume Scan") {
                Toast.makeText(context, "Resume photo scanned and parsed with Gemini AI!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gallery Image Picker for Resume Photo
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (_, base64Img) = readUriContent(context, uri)
            if (!base64Img.isNullOrBlank()) {
                viewModel.parseAndSaveResumeImage(base64Img, "Resume Photo from Gallery") {
                    Toast.makeText(context, "Resume image parsed with Gemini AI!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to read image from gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "AI Skill Extractor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select a document from your phone folder, take a photo of your resume, pick an image from gallery, or paste text below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Import Buttons (Phone Folder, Camera Photo, Image Gallery)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Import Options:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { documentPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone File", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { galleryPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = resumeText,
                    onValueChange = { resumeText = it },
                    label = { Text("Resume Content / Extracted Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("resume_input_text"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        if (resumeText.isNotBlank()) {
                            viewModel.parseAndSaveResume(resumeText) {
                                Toast.makeText(context, "Resume analyzed & saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("parse_resume_btn"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isParsing && resumeText.isNotBlank()
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extracting Skills with Gemini AI...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze & Save Resume", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (resumes.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Resume Profiles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                items(resumes) { resume ->
                    ResumeProfileCard(
                        resume = resume,
                        onDelete = { viewModel.deleteResume(resume.id) }
                    )
                }
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

private fun readUriContent(context: Context, uri: Uri): Pair<String?, String?> {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: ""
    return if (mimeType.startsWith("image/")) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                Pair(null, bitmapToBase64(bitmap))
            } else Pair(null, null)
        } catch (e: Exception) {
            Pair(null, null)
        }
    } else {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val text = inputStream?.bufferedReader()?.use { it.readText() }
            Pair(text, null)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
}

@Composable
fun ResumeProfileCard(
    resume: ResumeProfileEntity,
    onDelete: () -> Unit
) {
    val skills = parseJsonList(resume.skillsJson)
    val languages = parseJsonList(resume.languagesJson)
    val frameworks = parseJsonList(resume.frameworksJson)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (resume.candidateName.isNotEmpty()) resume.candidateName else resume.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (resume.education.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = resume.education, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = "Extracted Skills:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(skills) { skill ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun parseJsonList(jsonStr: String): List<String> {
    return try {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        list
    } catch (e: Exception) {
        emptyList()
    }
}
