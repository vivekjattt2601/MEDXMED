package com.example.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.MedRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onOpenAdminSection: () -> Unit
) {
    val context = LocalContext.current

    val currentName by MedRepository.userName.collectAsState()
    val currentAvatar by MedRepository.userAvatar.collectAsState()
    val currentPhotoUri by MedRepository.userPhotoUri.collectAsState()
    val currentTheme by MedRepository.currentTheme.collectAsState()


    // App Info & Settings StateFlows (About Us, Notice, Privacy, Refer, Rating)
    val aboutUsText by MedRepository.aboutUsText.collectAsState()
    val aboutUsUrl by MedRepository.aboutUsUrl.collectAsState()
    val noticeText by MedRepository.noticeText.collectAsState()
    val noticeUrl by MedRepository.noticeUrl.collectAsState()
    val privacyText by MedRepository.privacyText.collectAsState()
    val privacyUrl by MedRepository.privacyUrl.collectAsState()
    val referText by MedRepository.referText.collectAsState()
    val referUrl by MedRepository.referUrl.collectAsState()
    val ratingUrl by MedRepository.ratingUrl.collectAsState()


    var showEditDialog by remember { mutableStateOf(false) }
    var activeInfoDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeInfoDialogText by remember { mutableStateOf<String?>(null) }
    var activeInfoDialogUrl by remember { mutableStateOf<String?>(null) }
    var lastLockClickTime by remember { mutableLongStateOf(0L) }

    // Edit Profile state
    var editNameInput by remember(showEditDialog) { mutableStateOf(currentName) }
    var editAvatar by remember(showEditDialog) { mutableStateOf(currentAvatar) }
    var editPhotoUri by remember(showEditDialog) { mutableStateOf(currentPhotoUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val fileName = "profile_avatar_image_${System.currentTimeMillis()}.jpg"
                    val file = java.io.File(context.filesDir, fileName)
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    val path = file.absolutePath
                    editPhotoUri = path
                    MedRepository.updateProfile(currentName, currentAvatar, path)
                    Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val avatarOptions = listOf("👨‍⚕️", "👩‍⚕️", "🩺", "🥼", "🧠", "💉", "🧬", "⚕️", "🦷", "🫀")
    val themeBgColor = MaterialTheme.colorScheme.background

    BackHandler { onBackClick() }
            Scaffold(
                topBar = {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "My Profile",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                },
                containerColor = themeBgColor
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Profile Hero Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (currentTheme == 1) Color(0xFF1E293B) else Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(96.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Avatar Circle
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    if (currentTheme == 1) listOf(Color(0xFF334155), Color(0xFF1E293B))
                                                    else listOf(Color(0xFFDBEAFE), Color(0xFFEFF6FF))
                                                )
                                            )
                                            .border(2.5.dp, if (currentTheme == 1) Color(0xFF00E5FF) else Color(0xFF2563EB), CircleShape)
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentPhotoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentPhotoUri,
                                                contentDescription = "Profile Photo",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(text = currentAvatar, fontSize = 42.sp)
                                        }
                                    }

                                    // Pen-type Edit Photo Badge (Clearly visible, unclipped at bottom-right)
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(30.dp)
                                            .offset(x = (-2).dp, y = (-2).dp)
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                        shape = CircleShape,
                                        color = if (currentTheme == 1) Color(0xFF00E5FF) else Color(0xFF2563EB),
                                        shadowElevation = 3.dp,
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Upload Profile Photo",
                                                tint = if (currentTheme == 1) Color.Black else Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showEditDialog = true }
                                ) {
                                    Text(
                                        text = currentName,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentTheme == 1) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Dedicated Quick Cards: History & Bookmarks (Tapping opens new interface)
                                        // 3. Theme Switcher Card
                    item {
                        val themeNames = listOf("Classic White", "Neon Night", "Study Room", "Ocean Breeze", "Midnight OLED")
                        val themeIcons = listOf("🌓", "✨", "📖", "🌊", "🌑")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { MedRepository.updateTheme((currentTheme + 1) % 5) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentTheme == 1) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = themeIcons[currentTheme % 5], fontSize = 20.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Theme: ${themeNames[currentTheme % 5]}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (currentTheme == 1) Color.White else Color(0xFF0F172A)
                                        )
                                        Text("Tap to cycle app colors", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Icon(Icons.Default.Refresh, contentDescription = "Cycle Theme", tint = Color(0xFF2563EB))
                            }
                        }
                    }

                    // 4. Quick Action Icons Card (Clean 2-Row Icon & Label Grid)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentTheme == 1) Color(0xFF1E293B) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Quick Actions & Information",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTheme == 1) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                                )

                                // Row 1: About Us, Notice, Privacy, Contact
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileQuickIcon(
                                        icon = "ℹ️",
                                        label = "About Us",
                                        bgColor = Color(0xFFEFF6FF),
                                        onClick = {
                                            activeInfoDialogTitle = "ℹ️ About Us"
                                            activeInfoDialogText = aboutUsText
                                            activeInfoDialogUrl = aboutUsUrl
                                        }
                                    )

                                    ProfileQuickIcon(
                                        icon = "📢",
                                        label = "Notice",
                                        bgColor = Color(0xFFFEF3C7),
                                        onClick = {
                                            activeInfoDialogTitle = "📢 Notice"
                                            activeInfoDialogText = noticeText
                                            activeInfoDialogUrl = noticeUrl
                                        }
                                    )

                                    ProfileQuickIcon(
                                        icon = "🔒",
                                        label = "Privacy",
                                        bgColor = Color(0xFFF1F5F9),
                                        onClick = {
                                            activeInfoDialogTitle = "🔒 Privacy Policy"
                                            activeInfoDialogText = privacyText
                                            activeInfoDialogUrl = privacyUrl.ifBlank { "https://sites.google.com/view/medxmed-privacy-policy/home" }
                                        }
                                    )

                                    ProfileQuickIcon(
                                        icon = "📧",
                                        label = "Contact",
                                        bgColor = Color(0xFFFCE7F3),
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:jattviv2601@gmail.com?subject=MedXMed%20Support%20%26%20Inquiry")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Row 2: Refer, Rate Us, Admin Lock
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileQuickIcon(
                                        icon = "🎁",
                                        label = "Refer",
                                        bgColor = Color(0xFFDCFCE7),
                                        onClick = {
                                            val shareMsg = if (referUrl.isNotBlank()) "$referText\n\nLink: $referUrl" else referText
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareMsg)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Refer MedXMed App"))
                                        }
                                    )

                                    ProfileQuickIcon(
                                        icon = "⭐",
                                        label = "Rate Us",
                                        bgColor = Color(0xFFFEF9C3),
                                        onClick = {
                                            if (ratingUrl.isNotBlank()) {
                                                try {
                                                    val formattedUrl = if (ratingUrl.startsWith("http://") || ratingUrl.startsWith("https://")) ratingUrl else "https://$ratingUrl"
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open rating link", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Rating URL not configured yet", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )

                                    ProfileQuickIcon(
                                        icon = "🔐",
                                        label = "Admin",
                                        bgColor = Color(0xFFF1F5F9),
                                        onClick = {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastLockClickTime < 1500) {
                                                onOpenAdminSection()
                                                lastLockClickTime = 0L
                                            } else {
                                                lastLockClickTime = currentTime
                                                Toast.makeText(context, "Tap again to open Admin Panel", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Unofficial App Footer
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MedXMed unofficial app",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (currentTheme == 1) Color(0xFF64748B) else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }




    // Edit Profile Modal
    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📷 Upload Photo from Phone")
                    }

                    if (!editPhotoUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                editPhotoUri = null
                                MedRepository.updateProfile(editNameInput.trim(), editAvatar, null)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Remove Custom Photo (Use Emoji)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Or Choose Avatar Emoji:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(avatarOptions) { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = if (editAvatar == emoji && editPhotoUri.isNullOrBlank()) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = if (editAvatar == emoji && editPhotoUri.isNullOrBlank()) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        editAvatar = emoji
                                        editPhotoUri = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                MedRepository.updateProfile(editNameInput.trim(), editAvatar, editPhotoUri)
                                showEditDialog = false
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Info Dialog (with Text and optional URL button)
    if (activeInfoDialogTitle != null && activeInfoDialogText != null) {
        AlertDialog(
            onDismissRequest = {
                activeInfoDialogTitle = null
                activeInfoDialogText = null
                activeInfoDialogUrl = null
            },
            title = { Text(activeInfoDialogTitle.orEmpty(), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(activeInfoDialogText.orEmpty(), fontSize = 14.sp, lineHeight = 20.sp)
                    if (!activeInfoDialogUrl.isNullOrBlank() && activeInfoDialogTitle != "🔒 Privacy Policy") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Link: ${activeInfoDialogUrl.orEmpty()}",
                            fontSize = 12.sp,
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (!activeInfoDialogUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val url = activeInfoDialogUrl.orEmpty()
                                    val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (activeInfoDialogTitle == "🔒 Privacy Policy") "🌐 Open in Web Browser" else "Open Link 🔗")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = {
                        activeInfoDialogTitle = null
                        activeInfoDialogText = null
                        activeInfoDialogUrl = null
                    }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun ProfileQuickIcon(
    icon: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon, fontSize = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
