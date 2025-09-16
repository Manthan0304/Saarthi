package com.example.saarthi.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saarthi.R

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Saarthi",
            description = "Track your routes and share your location with family members safely and securely.",
            icon = "🗺️"
        ),
        OnboardingPage(
            title = "Record Your Routes",
            description = "Start recording your journeys with just one tap. Track distance, time, and see your path on the map.",
            icon = "📍"
        ),
        OnboardingPage(
            title = "Share with Family",
            description = "Share your live location with trusted family members. Control who can see your location.",
            icon = "👥"
        ),
        OnboardingPage(
            title = "Background Tracking",
            description = "Continue tracking even when the app is in the background. Your routes are always being recorded.",
            icon = "📱"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onGetStarted,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Skip")
            }
        }

        // Page content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Text(
                text = pages[currentPage].icon,
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Title
            Text(
                text = pages[currentPage].title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Description
            Text(
                text = pages[currentPage].description,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    Surface(
                        modifier = Modifier
                            .size(if (index == currentPage) 12.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (index == currentPage) Color(0xFF4CAF50) else Color(0xFF666666)
                    ) {
                        // Empty content
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (currentPage > 0) {
                    Button(
                        onClick = { currentPage-- },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF666666)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Next/Get Started button
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onGetStarted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (currentPage < pages.size - 1) "Next" else "Get Started")
                }
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)
