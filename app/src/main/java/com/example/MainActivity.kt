package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BacklogCard
import com.example.ui.BacklogViewModel
import com.example.ui.ColumnInfo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.AuthScreen
import com.example.ui.Localization
import com.example.util.ImageExporter
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.launch

// Local simulated thread class for high quality collaborative notes
data class TeamMessage(
    val sender: String,
    val initial: String,
    val text: String,
    val role: String,
    val timestamp: String,
    val color: Color
)

class MainActivity : ComponentActivity() {
    private val viewModel: BacklogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()

                if (!isAdminLoggedIn) {
                    // Block all app content until admin credentials are verified
                    com.example.ui.AdminLoginScreen(
                        onLoginSuccess = { viewModel.isAdminLoggedIn.value = true }
                    )
                } else if (currentUser == null) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        BacklogDashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BacklogDashboardScreen(
    viewModel: BacklogViewModel,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cardsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguageState.collectAsStateWithLifecycle()
    
    // UI states
    var selectedCategoryFilter by remember { mutableStateOf(-1) } // -1 means "All"
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEditCard by remember { mutableStateOf<BacklogCard?>(null) }
    var exportedImageFile by remember { mutableStateOf<File?>(null) }
    
    // Bottom navigation selection (0: Home/Dashboard, 1: Backlog/Tasks, 2: Collaborate, 3: Settings)
    var selectedTab by remember { mutableStateOf(0) }
    
    // Details statistics dialog state
    var showStatsDetails by remember { mutableStateOf(false) }
    
    // Collaborative roster dialog state
    var showTeamRoster by remember { mutableStateOf(false) }

    // Live collaborative chat forum comments
    val chatMessages = remember {
        mutableStateListOf(
            TeamMessage("MC", "MC", "Hey guys, let's make sure the timeline stays updated with core customer milestones.", "Product Manager", "10:15 AM", Color(0xFF6750A4)),
            TeamMessage("JD", "JD", "Sure, I'm working on Calendar Integration right now. Moving it to High priority because we need automated scheduling.", "Lead Architect", "11:32 AM", Color(0xFF1D1B20)),
            TeamMessage("AB", "AB", "Awesome. I'll test student onboarding flow. Let's make sure touch targets are at least 48dp.", "Senior QA", "1:24 PM", Color(0xFF49454F))
        )
    }

    var selectedChatSender by remember { mutableStateOf("JD") }
    var chatInputText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFEF7FF), // Theme Background
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                lang = currentLang
            )
        }
    ) { contentPadding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            
            // Switch screen modes based on bottom tabs
            when (selectedTab) {
                0 -> {
                    // TAB 0: HOME / PROJECT DASHBOARD
                    DashboardTabContent(
                        currentUser = currentUser,
                        cards = cards,
                        columns = viewModel.columns,
                        lang = currentLang,
                        onCardEdit = { selectedEditCard = it },
                        onShowStats = { showStatsDetails = true },
                        onShowRoster = { showTeamRoster = true },
                        onExportReport = { exportedImageFile = it }
                    )
                }
                1 -> {
                    // TAB 1: BACKLOG / KANBAN BOARD & COLUMN FEED
                    BacklogTabContent(
                        viewModel = viewModel,
                        cards = cards,
                        searchQuery = searchQuery,
                        selectedCategoryFilter = selectedCategoryFilter,
                        lang = currentLang,
                        onSelectCategory = { selectedCategoryFilter = it },
                        onCardEdit = { selectedEditCard = it }
                    )
                }
                2 -> {
                    // TAB 2: TEAM COLLABORATION / CHAT
                    val chatMsgs by viewModel.chatMessagesState.collectAsStateWithLifecycle()
                    val regUsers by viewModel.registeredUsersState.collectAsStateWithLifecycle()
                    val activeRecipient by viewModel.activeRecipientState.collectAsStateWithLifecycle()

                    ChatTabContent(
                        registeredUsers = regUsers,
                        chatMessages = chatMsgs,
                        chatInputText = chatInputText,
                        onChatInputChanged = { chatInputText = it },
                        activeRecipient = activeRecipient,
                        onRecipientSelected = { viewModel.selectRecipient(it) },
                        onSendMessage = { text, fileUrl ->
                            viewModel.sendMessage(text, fileUrl)
                            chatInputText = ""
                        },
                        currentUser = currentUser,
                        lang = currentLang
                    )
                }
                3 -> {
                    // TAB 3: ADMIN SETTINGS
                    SettingsTabContent(
                        viewModel = viewModel,
                        cards = cards,
                        lang = currentLang
                    )
                }
            }
        }
    }
    
    // Floating Action Button shown on Dashboard & Board to keep adding functional access easy
    if (selectedTab == 0 || selectedTab == 1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp, end = 24.dp), // Floating above bottom bar safety margin
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD0BCFF), // M3 theme FAB color
                contentColor = Color(0xFF381E72),
                modifier = Modifier.testTag("add_card_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Feature"
                )
            }
        }
    }
    
    // Overlay Dialogs
    if (showAddDialog) {
        AddCardDialog(
            columns = viewModel.columns,
            lang = currentLang,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, columnId, priority, desc ->
                viewModel.addCard(title, columnId, priority, desc)
                showAddDialog = false
            }
        )
    }
    
    selectedEditCard?.let { card ->
        EditCardDialog(
            card = card,
            columns = viewModel.columns,
            lang = currentLang,
            onDismiss = { selectedEditCard = null },
            onSave = { updatedCard ->
                viewModel.updateCard(updatedCard)
                selectedEditCard = null
            },
            onDelete = {
                viewModel.deleteCard(card.id)
                selectedEditCard = null
            },
            onExport = { file ->
                exportedImageFile = file
                selectedEditCard = null
            }
        )
    }

    exportedImageFile?.let { file ->
        ExportSuccessDialog(
            file = file,
            lang = currentLang,
            onDismiss = { exportedImageFile = null }
        )
    }

    // Interactive details statistic dialog
    if (showStatsDetails) {
        StatsBreakdownDialog(
            cards = cards,
            columns = viewModel.columns,
            lang = currentLang,
            onDismiss = { showStatsDetails = false }
        )
    }

    // Active roster team members list dialogue
    if (showTeamRoster) {
        TeamMembersDialog(
            onDismiss = { showTeamRoster = false }
        )
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = Color(0xFFF3EDF7), // M3 Navigation bar color
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val navItems = listOf(
            Triple(0, Localization.get("tab_home", lang), Icons.Default.Home),
            Triple(1, Localization.get("tab_tasks", lang), Icons.Default.List),
            Triple(2, Localization.get("tab_chat", lang), Icons.Default.Face),
            Triple(3, Localization.get("tab_settings", lang), Icons.Default.Settings)
        )
        
        navItems.forEach { (tabId, label, icon) ->
            val isSelected = selectedTab == tabId
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tabId) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFE8DEF8) // active pill colour
                )
            )
        }
    }
}

// ==========================================
// TAB 0: HOME / PROJECT DASHBOARD VIEW
// ==========================================
@Composable
fun DashboardTabContent(
    currentUser: com.example.data.User?,
    cards: List<BacklogCard>,
    columns: List<ColumnInfo>,
    lang: String,
    onCardEdit: (BacklogCard) -> Unit,
    onShowStats: () -> Unit,
    onShowRoster: () -> Unit,
    onExportReport: (File?) -> Unit
) {
    val totalCount = cards.size
    val highPriorityCount = cards.count { it.priority.equals("High", ignoreCase = true) }
    
    val userInitial = remember(currentUser) {
        val name = currentUser?.displayName ?: "U"
        if (name.isNotBlank()) {
            name.trim().split(" ").map { it.take(1) }.joinToString("").uppercase().take(2)
        } else {
            "U"
        }
    }

    // Interactive progress ratio computation: total cards minus high priority backlog
    val progressPercent = if (totalCount > 0) {
        ((totalCount - highPriorityCount) * 100) / totalCount
    } else {
        100
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header row styled beautifully matching Project Hub HTML template
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D1B20).copy(alpha = 0.08f))
                            .clickable { /* action */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Icon",
                            tint = Color(0xFF1D1B20)
                        )
                    }
                    Text(
                        text = Localization.get("app_title", lang),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1D1B20),
                        modifier = Modifier.testTag("app_title")
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6750A4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active Backlog Sprint Core Card Widget bg-[#EADDFF]
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val progressLabel = if (highPriorityCount > 0) Localization.get("active_backlog", lang) else Localization.get("backlog_clear", lang)
                    val statusText = if (highPriorityCount > 0) Localization.get("on_track", lang) else Localization.get("accomplished", lang)
                    val statusBg = if (highPriorityCount > 0) Color(0xFF21005D) else Color(0xFF2E7D32)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = progressLabel,
                                color = Color(0xFF21005D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$progressPercent%",
                                color = Color(0xFF21005D),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(statusBg)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress track white/30 filling to primary purple
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        val factor = if (progressPercent in 0..100) progressPercent / 100f else 1f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(factor)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFF6750A4))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (highPriorityCount > 0) {
                                Localization.get("high_priority_pending", lang).replace("{count}", highPriorityCount.toString())
                            } else {
                                Localization.get("ready_for_work", lang)
                            },
                            color = Color(0xFF21005D).copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        
                        Button(
                            onClick = onShowStats,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(Localization.get("btn_details", lang), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Priority Tasks section label bg-[#F3EDF7]
        item {
            Text(
                text = Localization.get("priority_backlog_tasks", lang),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Priority Tasks scroll cards representation
        val highPriorityCards = cards.filter { it.priority.lowercase() == "high" }
        if (highPriorityCards.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF6750A4)
                        )
                        Column {
                            Text(
                                text = Localization.get("zero_high_priority", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = Localization.get("board_synced", lang),
                                fontSize = 12.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        } else {
            items(highPriorityCards.take(4)) { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3EDF7))
                        .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onCardEdit(card) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "High Check",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D1B20)
                        )
                        val catName = columns.firstOrNull { it.id == card.columnId }?.getLocalizedName(lang) ?: "Feature"
                        val priorityLabel = if (lang == "es") "Prioridad" else if (lang == "pt") "Prioridade" else "Priority"
                        Text(
                            text = "$catName • ${card.priority} $priorityLabel",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Edit Card Action",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Two-column widgets section: Timeline & Team
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Card 1: Timeline
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFE6E1E5), RoundedCornerShape(20.dp))
                        .clickable { onShowStats() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD1E4FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Timeline Icon",
                                tint = Color(0xFF001D36)
                            )
                        }
                        Column {
                            Text(
                                text = Localization.get("timeline_tracker", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Localization.get("roadmap_active", lang),
                                fontSize = 12.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }

                // Secondary Card 2: Team Active Members Hub
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFE6E1E5), RoundedCornerShape(20.dp))
                        .clickable { onShowRoster() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFD8E4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Team Icon",
                                tint = Color(0xFF31111D)
                            )
                        }
                        Column {
                            Text(
                                text = Localization.get("collaboration_team", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Localization.get("members_assigned", lang),
                                fontSize = 12.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        }

        // Export report illustration item
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val resId = context.resources.getIdentifier(
                        "img_workspace_illustration",
                        "drawable",
                        context.packageName
                    )
                    if (resId != 0) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = resId),
                            contentDescription = "Workspace Digest Snapshot Graphic",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Localization.get("export_banner_title", lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = Localization.get("export_banner_desc", lang),
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Button(
                            onClick = {
                                val file = ImageExporter.generateSprintReport(context, cards, lang)
                                onExportReport(file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            modifier = Modifier.fillMaxWidth().testTag("export_sprint_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Report PNG",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("btn_export_report", lang), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: BACKLOG / KANBAN BOARD & COLUMN FEED
// ==========================================
@Composable
fun BacklogTabContent(
    viewModel: BacklogViewModel,
    cards: List<BacklogCard>,
    searchQuery: String,
    selectedCategoryFilter: Int,
    lang: String,
    onSelectCategory: (Int) -> Unit,
    onCardEdit: (BacklogCard) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow(
            searchQuery = searchQuery,
            lang = lang,
            onSearchChange = { viewModel.searchQuery.value = it }
        )
        
        FilterChipsRow(
            columns = viewModel.columns,
            selectedId = selectedCategoryFilter,
            lang = lang,
            onSelect = onSelectCategory
        )
        
        HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f), thickness = 1.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
            val isWideScreen = maxWidth > 650.dp
            
            if (isWideScreen && selectedCategoryFilter == -1) {
                KanbanBoardView(
                    columns = viewModel.columns,
                    cards = cards,
                    lang = lang,
                    onCardClick = onCardEdit
                )
            } else {
                val targetColumns = if (selectedCategoryFilter == -1) {
                    viewModel.columns
                } else {
                    viewModel.columns.filter { it.id == selectedCategoryFilter }
                }
                
                VerticalFeedView(
                    columns = targetColumns,
                    cards = cards,
                    lang = lang,
                    onCardClick = onCardEdit
                )
            }
        }
    }
}

// ==========================================
// TAB 2: FEEDBACK WORKSPACE CHAT FORUM
// ==========================================
@Composable
fun ChatTabContent(
    registeredUsers: List<com.example.data.User>,
    chatMessages: List<com.example.data.ChatMessage>,
    chatInputText: String,
    onChatInputChanged: (String) -> Unit,
    activeRecipient: com.example.data.User?,
    onRecipientSelected: (com.example.data.User?) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    currentUser: com.example.data.User?,
    lang: String
) {
    val context = LocalContext.current
    var showZipDialog by remember { mutableStateOf(false) }

    if (showZipDialog) {
        var zipFileName by remember { mutableStateOf("sprint_assets_v3.zip") }
        var selectedPortal by remember { mutableStateOf("https://gofile.io/") }
        
        AlertDialog(
            onDismissRequest = { showZipDialog = false },
            title = {
                Text(
                    text = if (lang == "es") "Adjuntar Recurso ZIP" else if (lang == "pt") "Anexar Recurso ZIP" else "Attach ZIP Resource",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (lang == "es") "Sube tu archivo ZIP y asocia la redirección para que los destinatarios lo descarguen desde su navegador:" 
                               else if (lang == "pt") "Envie seu arquivo ZIP e associe o link para redirecionamento para download no navegador:" 
                               else "Upload your ZIP file and associate the redirection webpage for downloads:",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                    
                    OutlinedTextField(
                        value = zipFileName,
                        onValueChange = { zipFileName = it },
                        label = { Text("ZIP File Name", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            focusedLabelColor = Color(0xFF6750A4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = selectedPortal,
                        onValueChange = { selectedPortal = it },
                        label = { Text("Download Web Redirect URL", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            focusedLabelColor = Color(0xFF6750A4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val presetPortals = listOf(
                        "https://gofile.io/" to "GoFile Portal",
                        "https://mediafire.com/" to "MediaFire Host",
                        "https://drive.google.com/" to "Google Drive",
                        "https://dropbox.com/" to "Dropbox Hub"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presetPortals.forEach { (url, label) ->
                            val isSel = selectedPortal == url
                            Button(
                                onClick = { selectedPortal = url },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFF6750A4) else Color(0xFFF3EDF7),
                                    contentColor = if (isSel) Color.White else Color(0xFF49454F)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = if (selectedPortal.endsWith("/")) "$selectedPortal$zipFileName" else "$selectedPortal/$zipFileName"
                        onSendMessage(
                            if (lang == "es") "Recurso compartido: $zipFileName" else if (lang == "pt") "Recurso compartilhado: $zipFileName" else "Shared resource: $zipFileName",
                            finalUrl
                        )
                        showZipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text(if (lang == "es") "Enviar ZIP" else if (lang == "pt") "Enviar ZIP" else "Send ZIP", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showZipDialog = false }) {
                    Text(if (lang == "es") "Cancelar" else if (lang == "pt") "Cancelar" else "Cancel", color = Color(0xFF49454F))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Conversation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (lang == "es") "Colaboración Sprint" else if (lang == "pt") "Colaboração do Sprint" else "Sprint Collaboration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = if (activeRecipient == null) {
                        if (lang == "es") "Canal: @general #sprint" else if (lang == "pt") "Canal: @geral #sprint" else "Channel: @general #sprint"
                    } else {
                        "${if (lang == "es") "Chat privado con" else if (lang == "pt") "Chat privado com" else "DM with"} ${activeRecipient.displayName}"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(
                onClick = { onChatInputChanged("") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync feed",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Horizontal Registered Users Directory Row
        Text(
            text = if (lang == "es") "Miembros del Equipo:" else if (lang == "pt") "Membros da Equipe:" else "Team Directory:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF49454F),
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option 1: GENERAL CHANNEL Selection
            item {
                val isGeneralActive = activeRecipient == null
                FilterChip(
                    selected = isGeneralActive,
                    onClick = { onRecipientSelected(null) },
                    label = {
                        Text(
                            text = if (lang == "es") "📢 Grupo General" else if (lang == "pt") "📢 Grupo Geral" else "📢 General Channel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6750A4),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("recipient_general")
                )
            }
            
            // Loop through registered members
            items(registeredUsers) { user ->
                val isMe = user.email == currentUser?.email
                val nameLabel = if (isMe) "${user.displayName} (Me)" else user.displayName
                val isSelected = activeRecipient?.email == user.email
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onRecipientSelected(user) },
                    label = {
                        Text(
                            text = "👤 $nameLabel",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF006A6A),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF0FDF4)
                    ),
                    modifier = Modifier.testTag("recipient_${user.email}")
                )
            }
        }

        Divider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))

        // Chat conversation bubble list
        if (chatMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (lang == "es") "👥 ¡No hay mensajes aún!" else if (lang == "pt") "👥 Nenhum sinal de mensagens!" else "👥 No messages yet!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F)
                    )
                    Text(
                        text = if (lang == "es") "Sé el primero en enviar un mensaje u adjuntar un archivo ZIP." 
                               else if (lang == "pt") "Seja o primeiro a enviar uma mensagem ou anexar um arquivo ZIP." 
                               else "Be the first to say hello or upload a ZIP folder here.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    val isSelf = msg.senderEmail == currentUser?.email
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isSelf) {
                            val userInit = if (msg.senderName.isNotBlank()) {
                                msg.senderName.trim().split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase().take(2)
                            } else {
                                "U"
                            }
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp, top = 4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6750A4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userInit, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelf) Color(0xFFEADDFF) else Color(0xFFF3EDF7))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                                
                                val timeLabel = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                Text(
                                    text = timeLabel,
                                    fontSize = 9.sp,
                                    color = Color(0xFF49454F).copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = msg.messageText,
                                fontSize = 13.sp,
                                color = Color(0xFF1D1B20)
                            )
                            
                            // ZIP file uploader decoration item
                            msg.fileUrl?.let { url ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .border(1.dp, Color(0xFFE6E1E5), RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("ZIP", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF006A6A))
                                            }
                                            
                                            Column {
                                                val fileName = url.substringAfterLast("/")
                                                Text(
                                                    text = fileName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D1B20)
                                                )
                                                Text(
                                                    text = if (lang == "es") "Haga clic para descargar el archivo" else if (lang == "pt") "Clique para baixar o arquivo" else "Click to download resource file",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF49454F)
                                                )
                                            }
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Host: ${url.take(25)}...",
                                                fontSize = 9.sp,
                                                color = Color(0xFF6750A4),
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    try {
                                                        val browseIntent = android.content.Intent(
                                                            android.content.Intent.ACTION_VIEW,
                                                            android.net.Uri.parse(url)
                                                        )
                                                        context.startActivity(browseIntent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A6A)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp).testTag("zip_dl_btn")
                                            ) {
                                                Text(
                                                    text = if (lang == "es") "Descargar" else if (lang == "pt") "Baixar" else "Download",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isSelf) {
                            val userInit = if (msg.senderName.isNotBlank()) {
                                msg.senderName.trim().split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase().take(2)
                            } else {
                                "U"
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp, top = 4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF006A6A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(userInit, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Prompt Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showZipDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1))
                    .testTag("btn_add_zip_file")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Attach ZIP File",
                    tint = Color(0xFF006A6A)
                )
            }

            OutlinedTextField(
                value = chatInputText,
                onValueChange = onChatInputChanged,
                placeholder = {
                    Text(
                        text = if (lang == "es") "Escribe un mensaje de sprint..." else if (lang == "pt") "Escrever mensagem de sprint..." else "Type a sprint message...",
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color(0xFF6750A4)
                ),
                modifier = Modifier.weight(1f).testTag("chat_input_text_field")
            )
            
            IconButton(
                onClick = {
                    if (chatInputText.isNotBlank()) {
                        onSendMessage(chatInputText, null)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4))
                    .testTag("chat_send_button"),
                enabled = chatInputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.White
                )
            }
        }
    }
}

// ==========================================
// TAB 3: ADMIN SETTINGS PANEL
// ==========================================
@Composable
fun SettingsTabContent(
    viewModel: BacklogViewModel,
    cards: List<BacklogCard>,
    lang: String
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    
    val userInitial = remember(currentUser) {
        val name = currentUser?.displayName ?: "U"
        if (name.isNotBlank()) {
            name.trim().split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase().take(2)
        } else {
            "U"
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Localization.get("tab_settings_title", lang),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D1B20)
            )
        }

        // Dynamic Language selection card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE6E1E5), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Localization.get("tab_settings_language_title", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = Localization.get("tab_settings_language_desc", lang),
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                    
                    val langs = listOf(
                        "en" to "English",
                        "es" to "Español",
                        "pt" to "Português"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        langs.forEach { (code, name) ->
                            val isSelected = lang == code
                            Button(
                                onClick = { viewModel.setLanguage(code) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7),
                                    contentColor = if (isSelected) Color.White else Color(0xFF49454F)
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier.weight(1f).testTag("lang_btn_$code"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userInitial.isBlank()) "U" else userInitial,
                                color = Color(0xFF21005D),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName ?: "Workspace Collaborator",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = "${Localization.get("lbl_role", lang)}: Core Sprint Stakeholder",
                                fontSize = 11.sp,
                                color = Color(0xFF6750A4)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.logOut() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out Icon",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("btn_logout", lang), fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Close admin test session and return to AdminLoginScreen
                    OutlinedButton(
                        onClick = { viewModel.adminLogOut() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6750A4))
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Cerrar sesión Admin",
                            tint = Color(0xFF6750A4)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión Admin", fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = Localization.get("admin_actions", lang),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Seeding database controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE6E1E5), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Localization.get("data_mgmt", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = Localization.get("data_mgmt_desc", lang),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset SeedTest button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val seedData = listOf(
                                        BacklogCard(title = "Dashboard", columnId = 0, priority = "High", description = "Main landing portal displaying visual metrics and core stats overview."),
                                        BacklogCard(title = "Calendar Integration", columnId = 0, priority = "High", description = "Bi-directional scheduling synchronizations for calendars and events."),
                                        BacklogCard(title = "Task Management", columnId = 0, priority = "High", description = "Workflow controls to assign, delegate, write, and audit backlog tasks."),
                                        BacklogCard(title = "Project Management", columnId = 1, priority = "None", description = "Track project boards, milestones, and status metrics."),
                                        BacklogCard(title = "Team Search", columnId = 1, priority = "None", description = "Advanced filtering engine for department, skills, and availability."),
                                        BacklogCard(title = "Notifications System", columnId = 1, priority = "None", description = "Deliver key events via real-time alerts or emails."),
                                        BacklogCard(title = "Student Profiles", columnId = 2, priority = "None", description = "Complete database record of progress and profile characteristics."),
                                        BacklogCard(title = "Statistics Dashboard", columnId = 3, priority = "Low", description = "Overview of charts, histograms, data logs, and visual performance charts."),
                                        BacklogCard(title = "Performance Analytics", columnId = 3, priority = "Low", description = "Deeper data queries mapping historical trends and backlog velocity.")
                                    )
                                    // Drop first to avoid multiples
                                    cards.forEach { viewModel.deleteCard(it.id) }
                                    seedData.forEach { viewModel.addCard(it.title, it.columnId, it.priority, it.description) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Localization.get("btn_seed", lang), fontSize = 12.sp)
                        }

                        // Wipe Db button
                        OutlinedButton(
                            onClick = {
                                cards.forEach { viewModel.deleteCard(it.id) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFD32F2F)))
                        ) {
                            Text(Localization.get("btn_clear", lang), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Dialog to show detailed statistical counts
@Composable
fun StatsBreakdownDialog(
    cards: List<BacklogCard>,
    columns: List<ColumnInfo>,
    lang: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "es") "Detalles de Tareas" else if (lang == "pt") "Detalhes de Tarefas" else "Feature Backlog Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111E38)
                )

                columns.forEach { col ->
                    val colCards = cards.filter { it.columnId == col.id }
                    val highCount = colCards.count { it.priority.lowercase() == "high" }
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(col.color))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(col.getLocalizedName(lang), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1B20))
                            }
                            val cardsSuffix = if (lang == "es") "tarjetas" else if (lang == "pt") "cartões" else "cards"
                            Text("${colCards.size} $cardsSuffix", fontSize = 12.sp, color = Color(0xFF49454F))
                        }
                        if (colCards.isNotEmpty()) {
                            val hiLabel = if (lang == "es") "Alta prioridad" else if (lang == "pt") "Alta prioridade" else "High priority"
                            val loLabel = if (lang == "es") "Baja/Ninguna" else if (lang == "pt") "Baixa/Nenhuma" else "Low/None"
                            Text(
                                text = "$hiLabel: $highCount  •  $loLabel: ${colCards.size - highCount}",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F).copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text(Localization.get("btn_close", lang))
                    }
                }
            }
        }
    }
}

// Dialog to show team roster details
@Composable
fun TeamMembersDialog(
    onDismiss: () -> Unit
) {
    val members = listOf(
        Triple("JD", "Juan Diaz", "Lead Architect • Systems"),
        Triple("MC", "Miler Cenizario", "Lead Creator • Design"),
        Triple("AB", "Alice Brown", "Senior Tech Lead • Quality"),
        Triple("TS", "Tom Stark", "Interactive Product Owner")
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Project Contributors",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )

                members.forEach { (avatar, name, details) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatar, color = Color(0xFF21005D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1B20))
                            Text(details, fontSize = 12.sp, color = Color(0xFF49454F))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// ==========================================
// PRE-EXISTING ORIGINAL METHOD SIGNATURE COMPATIBILITY COPIES
// ==========================================

@Composable
fun HeaderRow(
    searchQuery: String,
    lang: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (lang == "es") "Backlog de Funciones" else if (lang == "pt") "Backlog de Recursos" else "Feature Backlog",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1B20),
            modifier = Modifier.testTag("app_title")
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        val searchPlaceholder = if (lang == "es") "Buscar..." else if (lang == "pt") "Buscar..." else "Search..."
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(searchPlaceholder, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color(0xFF6750A4)
            ),
            modifier = Modifier
                .width(160.dp)
                .testTag("search_input")
        )
    }
}

@Composable
fun FilterChipsRow(
    columns: List<ColumnInfo>,
    selectedId: Int,
    lang: String,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedId == -1,
            onClick = { onSelect(-1) },
            label = {
                val allLabel = if (lang == "es") "Todo el Backlog" else if (lang == "pt") "Todo o Backlog" else "All Backlog"
                Text(allLabel, fontSize = 12.sp)
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF6750A4),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFFF3EDF7),
                labelColor = Color(0xFF49454F)
            ),
            modifier = Modifier.testTag("filter_chip_all")
        )
        
        columns.forEach { col ->
            FilterChip(
                selected = selectedId == col.id,
                onClick = { onSelect(col.id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(col.color))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(col.getLocalizedName(lang), fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(col.color),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF3EDF7),
                    labelColor = Color(0xFF49454F)
                ),
                modifier = Modifier.testTag("filter_chip_${col.id}")
            )
        }
    }
}

@Composable
fun KanbanBoardView(
    columns: List<ColumnInfo>,
    cards: List<BacklogCard>,
    lang: String,
    onCardClick: (BacklogCard) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        columns.forEach { col ->
            val colCards = cards.filter { it.columnId == col.id }
            
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3EDF7))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(col.color))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = col.getLocalizedName(lang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D1B20)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = colCards.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F),
                            modifier = Modifier
                                .background(Color(0xFFEADDFF), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    
                    if (colCards.isEmpty()) {
                        EmptyStateCard(col.getLocalizedName(lang), lang)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(colCards) { card ->
                                TaskCard(
                                    card = card,
                                    stripeColor = Color(col.color),
                                    lang = lang,
                                    onClick = { onCardClick(card) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalFeedView(
    columns: List<ColumnInfo>,
    cards: List<BacklogCard>,
    lang: String,
    onCardClick: (BacklogCard) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        columns.forEach { col ->
            val colCards = cards.filter { it.columnId == col.id }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(col.color))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = col.getLocalizedName(lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D1B20)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val itemsSuffix = if (lang == "es") "elementos" else if (lang == "pt") "itens" else "items"
                    Text(
                        text = "${colCards.size} $itemsSuffix",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
            
            if (colCards.isEmpty()) {
                item {
                    EmptyStateCard(col.getLocalizedName(lang), lang)
                }
            } else {
                items(colCards) { card ->
                    TaskCard(
                        card = card,
                        stripeColor = Color(col.color),
                        lang = lang,
                        onClick = { onCardClick(card) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    card: BacklogCard,
    stripeColor: Color,
    lang: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_card_${card.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1B20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (card.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.description,
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (card.priority != "None") {
                    Spacer(modifier = Modifier.height(8.dp))
                    PriorityBadge(priority = card.priority, lang = lang)
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String, lang: String) {
    val isHigh = priority.equals("High", ignoreCase = true)
    val bg = if (isHigh) Color(0xFFFFE9EC) else Color(0xFFE8F5E9)
    val fg = if (isHigh) Color(0xFFC2185B) else Color(0xFF2E7D32)
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        val mappedPriority = if (lang == "es") {
            if (isHigh) "Alta" else "Baja"
        } else if (lang == "pt") {
            if (isHigh) "Alta" else "Baixa"
        } else {
            priority
        }
        Text(
            text = mappedPriority,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateCard(categoryName: String, lang: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF49454F),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            val emptyText = if (lang == "es") {
                "Sin elementos en $categoryName"
            } else if (lang == "pt") {
                "Nenhum item em $categoryName"
            } else {
                "No items in $categoryName"
            }
            Text(
                text = emptyText,
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AddCardDialog(
    columns: List<ColumnInfo>,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, columnId: Int, priority: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedColId by remember { mutableStateOf(columns.firstOrNull()?.id ?: 0) }
    var priority by remember { mutableStateOf("None") }
    
    var colDropdownExpanded by remember { mutableStateOf(false) }
    var priDropdownExpanded by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Localization.get("dialog_card_add", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.get("field_title", lang)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedLabelColor = Color(0xFF6750A4),
                        focusedIndicatorColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_title_input")
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(Localization.get("field_description", lang)) },
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedLabelColor = Color(0xFF6750A4),
                        focusedIndicatorColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_desc_input")
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { colDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_column_dropdown")
                    ) {
                        val activeCol = columns.firstOrNull { it.id == selectedColId }
                        Text("${Localization.get("field_column", lang)}: ${activeCol?.getLocalizedName(lang) ?: "Select"}", color = Color(0xFF6750A4))
                    }
                    
                    DropdownMenu(
                        expanded = colDropdownExpanded,
                        onDismissRequest = { colDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        columns.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.getLocalizedName(lang)) },
                                onClick = {
                                    selectedColId = col.id
                                    colDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { priDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_priority_dropdown")
                    ) {
                        val localizedPri = if (lang == "es") {
                            if (priority == "High") "Alta" else if (priority == "Low") "Baja" else "Ninguna"
                        } else if (lang == "pt") {
                            if (priority == "High") "Alta" else if (priority == "Low") "Baixa" else "Nenhuma"
                        } else {
                            priority
                        }
                        Text("${Localization.get("field_priority", lang)}: $localizedPri", color = Color(0xFF6750A4))
                    }
                    
                    DropdownMenu(
                        expanded = priDropdownExpanded,
                        onDismissRequest = { priDropdownExpanded = false }
                    ) {
                        val options = listOf("None", "High", "Low")
                        options.forEach { pri ->
                            val itemLabel = if (lang == "es") {
                                if (pri == "High") "Alta" else if (pri == "Low") "Baja" else "Ninguna"
                            } else if (lang == "pt") {
                                if (pri == "High") "Alta" else if (pri == "Low") "Baixa" else "Nenhuma"
                            } else {
                                pri
                            }
                            DropdownMenuItem(
                                text = { Text(itemLabel) },
                                onClick = {
                                    priority = pri
                                    priDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("add_cancel_button")) {
                        Text(Localization.get("btn_cancel", lang), color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, selectedColId, priority, desc)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("add_confirm_button")
                    ) {
                        Text(Localization.get("btn_add", lang))
                    }
                }
            }
        }
    }
}

@Composable
fun EditCardDialog(
    card: BacklogCard,
    columns: List<ColumnInfo>,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (BacklogCard) -> Unit,
    onDelete: () -> Unit,
    onExport: (File?) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var desc by remember { mutableStateOf(card.description) }
    var selectedColId by remember { mutableStateOf(card.columnId) }
    var priority by remember { mutableStateOf(card.priority) }
    
    var colDropdownExpanded by remember { mutableStateOf(false) }
    var priDropdownExpanded by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Localization.get("edit_item", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.get("field_title", lang)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedLabelColor = Color(0xFF6750A4),
                        focusedIndicatorColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(Localization.get("field_description", lang)) },
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedLabelColor = Color(0xFF6750A4),
                        focusedIndicatorColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_desc_input")
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { colDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("edit_column_dropdown")
                    ) {
                        val activeCol = columns.firstOrNull { it.id == selectedColId }
                        Text("${Localization.get("field_column", lang)}: ${activeCol?.name ?: "Select"}", color = Color(0xFF6750A4))
                    }
                    
                    DropdownMenu(
                        expanded = colDropdownExpanded,
                        onDismissRequest = { colDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        columns.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name) },
                                onClick = {
                                    selectedColId = col.id
                                    colDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { priDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("edit_priority_dropdown")
                    ) {
                        Text("${Localization.get("home_priority", lang)}: $priority", color = Color(0xFF6750A4))
                    }
                    
                    DropdownMenu(
                        expanded = priDropdownExpanded,
                        onDismissRequest = { priDropdownExpanded = false }
                    ) {
                        listOf("None", "High", "Low").forEach { pri ->
                            DropdownMenuItem(
                                text = { Text(pri) },
                                onClick = {
                                    priority = pri
                                    priDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("edit_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFD32F2F)
                            )
                        }

                        IconButton(
                            onClick = {
                                val context = card.id // fallback context identifier / get native context next:
                            },
                            modifier = Modifier.testTag("edit_export_card_dummy_trigger")
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Card",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        val activeColName = columns.firstOrNull { it.id == selectedColId }?.name ?: "Feature"
                                        val file = ImageExporter.generateCardImage(
                                            context,
                                            card.copy(title = title, description = desc, columnId = selectedColId, priority = priority),
                                            activeColName,
                                            lang
                                        )
                                        onExport(file)
                                    }
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_cancel_button")) {
                            Text(Localization.get("btn_cancel", lang), color = Color(0xFF49454F))
                        }
                        
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(card.copy(
                                        title = title,
                                        description = desc,
                                        columnId = selectedColId,
                                        priority = priority,
                                        timestamp = System.currentTimeMillis()
                                    ))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            modifier = Modifier.testTag("edit_save_button")
                        ) {
                            Text(Localization.get("btn_save", lang))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportSuccessDialog(
    file: File,
    lang: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(file) {
        try {
            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2F0D9)), // light green circle representing success
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF385723),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = Localization.get("export_dialog_title", lang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${Localization.get("export_dialog_desc", lang)}\n${Localization.get("export_dialog_status", lang)}",
                    fontSize = 12.sp,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center
                )

                if (bitmap != null) {
                    Text(
                        text = Localization.get("export_preview_label", lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Exported PNG Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Localization.get("btn_close", lang), color = Color(0xFF49454F))
                    }

                    Button(
                        onClick = {
                            ImageExporter.shareImage(context, file)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.get("export_btn_share", lang), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
