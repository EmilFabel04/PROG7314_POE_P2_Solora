package dev.solora.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotificationCard(
                title = "New quote ready",
                message = "System sizing complete for K. Naidoo",
                time = "5 min ago"
            )
            NotificationCard(
                title = "Lead follow-up",
                message = "Schedule visit for P. Jacobs",
                time = "Today, 15:00"
            )
            NotificationCard(
                title = "Reminder",
                message = "Upload site photos for Mandela St.",
                time = "Yesterday"
            )
            NotificationCard(
                title = "System check",
                message = "Battery health report available",
                time = "Yesterday"
            )
        }
    }
}

@Composable
private fun NotificationCard(title: String, message: String, time: String) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(time, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
