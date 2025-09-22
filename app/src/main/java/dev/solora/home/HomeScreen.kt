package dev.solora.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.solora.leads.LeadsViewModel
import dev.solora.quotes.QuotesViewModel

@Composable
fun HomeScreen(
    onOpenQuotes: () -> Unit,
    onOpenLeads: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val quotesVm: QuotesViewModel = viewModel()
    val leadsVm: LeadsViewModel = viewModel()
    val quotes by quotesVm.quotes.collectAsState()
    val leads by leadsVm.leads.collectAsState()

    val monthlySavings = quotes.sumOf { it.savingsRands }
    val totalQuotes = quotes.size
    val totalLeads = leads.size

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroBanner(
                monthlySavings = monthlySavings,
                totalQuotes = totalQuotes,
                totalLeads = totalLeads,
                onSettings = onOpenSettings
            )
            QuickActions(
                onOpenQuotes = onOpenQuotes,
                onOpenLeads = onOpenLeads,
                onOpenNotifications = onOpenNotifications
            )
            DashboardSummary(totalLeads = totalLeads, totalQuotes = totalQuotes)
            NotificationsPreview(onOpenNotifications = onOpenNotifications)
        }
    }
}

@Composable
private fun HeroBanner(monthlySavings: Double, totalQuotes: Int, totalLeads: Int, onSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome back", color = Color.White.copy(alpha = 0.85f))
                        Text("Solora Consultant", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
                Text("Projected monthly savings", color = Color.White.copy(alpha = 0.7f))
                Text("R ${"%.0f".format(monthlySavings)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryChip(title = "Quotes", value = totalQuotes.toString(), icon = Icons.Filled.RequestQuote)
                    SummaryChip(title = "Leads", value = totalLeads.toString(), icon = Icons.Filled.People)
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(title: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Column {
            Text(title, color = Color.White.copy(alpha = 0.7f))
            Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickActions(onOpenQuotes: () -> Unit, onOpenLeads: () -> Unit, onOpenNotifications: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quick actions", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard(
                modifier = Modifier.weight(1f),
                title = "Calculate quote",
                subtitle = "Capture site info",
                icon = Icons.Filled.RequestQuote,
                onClick = onOpenQuotes
            )
            ActionCard(
                modifier = Modifier.weight(1f),
                title = "View leads",
                subtitle = "Manage pipeline",
                icon = Icons.Filled.People,
                onClick = onOpenLeads
            )
        }
        ActionCard(
            title = "Notifications",
            subtitle = "Recent updates",
            icon = Icons.Filled.Notifications,
            onClick = onOpenNotifications
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DashboardSummary(totalLeads: Int, totalQuotes: Int) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Dashboard", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Active leads",
                    value = totalLeads.toString(),
                    accent = Color(0xFFFFA726)
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    label = "Quotes this month",
                    value = totalQuotes.toString(),
                    accent = Color(0xFF29B6F6)
                )
            }
            StatTile(
                label = "Performance score",
                value = "82%",
                accent = Color(0xFF66BB6A)
            )
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = accent, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun NotificationsPreview(onOpenNotifications: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onOpenNotifications) { Icon(Icons.Filled.Notifications, contentDescription = null) }
            }
            NotificationRow(
                title = "New quote ready",
                message = "System sizing complete for K. Naidoo",
                time = "5 min ago"
            )
            NotificationRow(
                title = "Lead follow-up",
                message = "Schedule visit for P. Jacobs",
                time = "Today, 15:00"
            )
            NotificationRow(
                title = "Reminder",
                message = "Upload site photos for Mandela St.",
                time = "Yesterday"
            )
        }
    }
}

@Composable
private fun NotificationRow(title: String, message: String, time: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(time, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
