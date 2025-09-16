package com.example.saarthi.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saarthi.R

@Composable
fun OnBoardingScreen(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)), // black background
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon (replace with your map icon asset)
        Image(
            painter = painterResource(id = R.drawable.baseline_map_24), // add your icon in drawable
            contentDescription = "Map Icon",
            modifier = Modifier.size(90.dp) ,
                    colorFilter = ColorFilter.tint(Color(0xFF2E7D32))        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Breadcrumb\nNavigation",
            color = Color(0xFFBFBFBF), // grey text
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Retrace your journey",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}