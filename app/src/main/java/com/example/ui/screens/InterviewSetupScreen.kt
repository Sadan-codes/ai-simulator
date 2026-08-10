package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSetupScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSessionCreated: (Long) -> Unit
) {
    val isGenerating by viewModel.isGeneratingQuestion.collectAsStateWithLifecycle()

    var selectedRole by remember { mutableStateOf("Backend") }
    var selectedLevel by remember { mutableStateOf("Junior / Intern") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var selectedCompany by remember { mutableStateOf("Google") }
    var selectedQuestionCount by remember { mutableStateOf(5) }

    val roles = listOf("Backend", "Frontend", "Android", "Data Science", "System Design", "Fullstack")
    val levels = listOf("Intern", "Junior / Intern", "Mid-Level", "Senior", "Staff / Lead")
    val difficulties = listOf("Easy", "Medium", "Hard", "Big-Tech FAANG")
    val companies = listOf("Google", "Amazon", "Meta", "Microsoft", "Apple", "General Tech", "Startup")
    val questionCounts = listOf(3, 5, 10)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Mock Interview", fontWeight = FontWeight.Bold) },
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
            // Role Selection
            item {
                Text(
                    text = "Target Job Role",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(roles) { role ->
                        SelectableChip(
                            text = role,
                            isSelected = role == selectedRole,
                            onClick = { selectedRole = role },
                            tag = "role_$role"
                        )
                    }
                }
            }

            // Experience Level Selection
            item {
                Text(
                    text = "Experience Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(levels) { level ->
                        SelectableChip(
                            text = level,
                            isSelected = level == selectedLevel,
                            onClick = { selectedLevel = level },
                            tag = "level_$level"
                        )
                    }
                }
            }

            // Company Format Preset
            item {
                Text(
                    text = "Company Interview Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(companies) { company ->
                        SelectableChip(
                            text = company,
                            isSelected = company == selectedCompany,
                            onClick = { selectedCompany = company },
                            tag = "company_$company"
                        )
                    }
                }
            }

            // Difficulty Selection
            item {
                Text(
                    text = "Interview Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(difficulties) { diff ->
                        SelectableChip(
                            text = diff,
                            isSelected = diff == selectedDifficulty,
                            onClick = { selectedDifficulty = diff },
                            tag = "diff_$diff"
                        )
                    }
                }
            }

            // Question Count
            item {
                Text(
                    text = "Number of Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    questionCounts.forEach { count ->
                        SelectableChip(
                            text = "$count Questions",
                            isSelected = count == selectedQuestionCount,
                            onClick = { selectedQuestionCount = count },
                            tag = "count_$count"
                        )
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Interview Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• $selectedCompany style $selectedRole Interview ($selectedLevel)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Difficulty: $selectedDifficulty • Total Questions: $selectedQuestionCount",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Live voice/text response, filler word analysis, and AI scorecard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Launch Button
            item {
                Button(
                    onClick = {
                        viewModel.startNewSession(
                            role = selectedRole,
                            difficulty = selectedDifficulty,
                            experienceLevel = selectedLevel,
                            companyPreset = selectedCompany,
                            questionCount = selectedQuestionCount,
                            onSessionCreated = onSessionCreated
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_interview_btn"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preparing AI Interviewer...")
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Begin Live Interview", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
