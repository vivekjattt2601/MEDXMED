package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedRepository
import com.example.model.ProfYear
import com.example.model.StudyContentItem

/**
 * Helper to determine which Section a PYQ item belongs to.
 * Returns only the dynamic section name, without falling back to any hardcoded prof years.
 */
fun getPyqSectionName(item: StudyContentItem): String {
    val sec = item.subjectId.trim()
    return if (sec.isNotBlank() && !sec.equals("pyq", ignoreCase = true)) {
        sec
    } else {
        ""
    }
}

@Composable
fun PYQScreen(
    onBackClick: () -> Unit,
    onPdfClick: (title: String, mediaUrl: String) -> Unit = { _, _ -> }
) {
    val contents by MedRepository.contents.collectAsState()
    val folders by MedRepository.folders.collectAsState()
    val pyqItems = remember(contents) { contents.filter { it.isPyq } }

    // Hierarchy State: Section -> Folder (Optional) -> Subfolder (Optional) -> Papers
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Back button navigation across hierarchy levels
    BackHandler(enabled = selectedSection != null) {
        if (selectedSubfolder != null) {
            selectedSubfolder = null
            searchQuery = ""
        } else if (selectedFolder != null) {
            selectedFolder = null
            searchQuery = ""
        } else {
            selectedSection = null
            searchQuery = ""
        }
    }

    // Global Theme Colors
    val currentTheme by MedRepository.currentTheme.collectAsState()
    val themeBgColor = when (currentTheme) {
        1 -> Color(0xFF0F172A)
        2 -> Color(0xFFFEF9C3)
        else -> Color(0xFFF8FAFC)
    }
    val cardBgColor = when (currentTheme) {
        1 -> Color(0xFF1E293B)
        2 -> Color(0xFFFEF08A)
        else -> Color.White
    }
    val primaryTextColor = when (currentTheme) {
        1 -> Color.White
        2 -> Color(0xFF422006)
        else -> Color(0xFF0F172A)
    }
    val secondaryTextColor = when (currentTheme) {
        1 -> Color(0xFF94A3B8)
        2 -> Color(0xFF713F12)
        else -> Color(0xFF64748B)
    }
    val backBtnBgColor = when (currentTheme) {
        1 -> Color(0xFF334155)
        2 -> Color(0xFFFDE047)
        else -> Color(0xFFF1F5F9)
    }

    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTopPadding = if (statusBarInset > 0.dp) statusBarInset + 12.dp else 26.dp

    // =========================================================================
    // LEVEL 4: SUBFOLDER VIEW (Shows Papers/PDFs inside Subfolder)
    // =========================================================================
    if (selectedSection != null && selectedSubfolder != null) {
        val activeSection = selectedSection!!
        val activeFolder = selectedFolder
        val activeSubfolder = selectedSubfolder!!

        val subfolderPapers = remember(pyqItems, activeSection, activeFolder, activeSubfolder, searchQuery) {
            val list = pyqItems.filter {
                getPyqSectionName(it).equals(activeSection, ignoreCase = true) &&
                        (activeFolder == null || it.folderName.trim().equals(activeFolder, ignoreCase = true)) &&
                        it.chapterName.trim().equals(activeSubfolder, ignoreCase = true)
            }
            if (searchQuery.isBlank()) list
            else list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                PYQHeader(
                    title = activeSubfolder,
                    subtitle = null,
                    leadingEmoji = "📂",
                    onBackClick = {
                        selectedSubfolder = null
                        searchQuery = ""
                    },
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    backBtnBgColor = backBtnBgColor,
                    topPadding = effectiveTopPadding
                )
            },
            containerColor = themeBgColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Search...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = secondaryTextColor) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }
                if (subfolderPapers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No papers match '$searchQuery'" else "No papers in this subfolder yet",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(subfolderPapers, key = { it.id }) { item ->
                    PYQPaperRowItem(
                        item = item,
                        cardBgColor = cardBgColor,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { onPdfClick(item.title, item.mediaUrl) }
                    )
                }
            }
        }
    }
    // =========================================================================
    // LEVEL 3: FOLDER VIEW (Shows Subfolders and Direct Papers inside Folder)
    // =========================================================================
    else if (selectedSection != null && selectedFolder != null) {
        val activeSection = selectedSection!!
        val activeFolder = selectedFolder!!

        val folderItems = remember(pyqItems, activeSection, activeFolder) {
            pyqItems.filter {
                getPyqSectionName(it).equals(activeSection, ignoreCase = true) &&
                        it.folderName.trim().equals(activeFolder, ignoreCase = true)
            }
        }

        // Subfolders inside this folder
        val subfolders = remember(folderItems, searchQuery) {
            val subs = folderItems
                .map { it.chapterName.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            if (searchQuery.isBlank()) subs
            else subs.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        // Direct papers inside this folder (no subfolder)
        val directPapers = remember(folderItems, searchQuery) {
            val direct = folderItems.filter { it.chapterName.trim().isBlank() }
            if (searchQuery.isBlank()) direct
            else direct.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                PYQHeader(
                    title = activeFolder,
                    subtitle = null,
                    leadingEmoji = "📁",
                    onBackClick = {
                        selectedFolder = null
                        searchQuery = ""
                    },
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    backBtnBgColor = backBtnBgColor,
                    topPadding = effectiveTopPadding
                )
            },
            containerColor = themeBgColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Search...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = secondaryTextColor) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                // Subfolders List
                if (subfolders.isNotEmpty()) {
                    items(subfolders) { sub ->
                        val count = folderItems.count { it.chapterName.trim().equals(sub, ignoreCase = true) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubfolder = sub
                                    searchQuery = ""
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (currentTheme == 1) Color(0xFF334155) else Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📂", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count Papers",
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Subfolder",
                                    tint = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Direct Papers
                if (directPapers.isNotEmpty()) {
                    item {
                        Text(
                            text = "PAPERS (${directPapers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(directPapers, key = { it.id }) { item ->
                        PYQPaperRowItem(
                            item = item,
                            cardBgColor = cardBgColor,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { onPdfClick(item.title, item.mediaUrl) }
                        )
                    }
                }

                if (subfolders.isEmpty() && directPapers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matches found" else "This folder is empty",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    // =========================================================================
    // LEVEL 2: SECTION VIEW (Shows Folders, Direct Subfolders & Direct Papers)
    // =========================================================================
    else if (selectedSection != null) {
        val activeSection = selectedSection!!

        val sectionItems = remember(pyqItems, activeSection) {
            pyqItems.filter { getPyqSectionName(it).equals(activeSection, ignoreCase = true) }
        }

        // Distinct Folders in this section
        val foldersInSection = remember(sectionItems, folders, searchQuery) {
            val fromContents = sectionItems.map { it.folderName.trim() }
            val fromFolders = folders.filter { it.sectionType.equals(activeSection, ignoreCase = true) && it.name.isNotBlank() }.map { it.name.trim() }
            val list = (fromContents + fromFolders).filter { it.isNotBlank() }.distinct().sorted()
            if (searchQuery.isBlank()) list
            else list.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        // Direct Subfolders in this section (when no folder was created)
        val directSubfolders = remember(sectionItems, searchQuery) {
            val list = sectionItems
                .filter { it.folderName.trim().isBlank() }
                .map { it.chapterName.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            if (searchQuery.isBlank()) list
            else list.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        // Direct Papers in this section (no folder & no subfolder)
        val directPapers = remember(sectionItems, searchQuery) {
            val list = sectionItems.filter { it.folderName.trim().isBlank() && it.chapterName.trim().isBlank() }
            if (searchQuery.isBlank()) list
            else list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                PYQHeader(
                    title = activeSection,
                    subtitle = null,
                    leadingEmoji = "📁",
                    onBackClick = {
                        selectedSection = null
                        searchQuery = ""
                    },
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    backBtnBgColor = backBtnBgColor,
                    topPadding = effectiveTopPadding
                )
            },
            containerColor = themeBgColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Search...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = secondaryTextColor) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                // Folders
                if (foldersInSection.isNotEmpty()) {
                    items(foldersInSection) { fName ->
                        val count = sectionItems.count { it.folderName.trim().equals(fName, ignoreCase = true) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFolder = fName
                                    searchQuery = ""
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (currentTheme == 1) Color(0xFF334155) else Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📁", fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count Papers",
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Folder",
                                    tint = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Direct Subfolders (when folder is skipped)
                if (directSubfolders.isNotEmpty()) {
                    item {
                        Text(
                            text = "SUBFOLDERS (${directSubfolders.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(directSubfolders) { sub ->
                        val count = sectionItems.count { it.folderName.trim().isBlank() && it.chapterName.trim().equals(sub, ignoreCase = true) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubfolder = sub
                                    searchQuery = ""
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (currentTheme == 1) Color(0xFF334155) else Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📂", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count Paper(s)",
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Subfolder",
                                    tint = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Direct Papers (when both folder & subfolder are skipped)
                if (directPapers.isNotEmpty()) {
                    item {
                        Text(
                            text = "PAPERS (${directPapers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(directPapers, key = { it.id }) { item ->
                        PYQPaperRowItem(
                            item = item,
                            cardBgColor = cardBgColor,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = { onPdfClick(item.title, item.mediaUrl) }
                        )
                    }
                }

                if (folders.isEmpty() && directSubfolders.isEmpty() && directPapers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matches found" else "No papers in this section yet",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    // =========================================================================
    // LEVEL 1: ROOT PYQ BANK (Shows All Sections)
    // =========================================================================
    else {
        // Collect all distinct Sections created by Admin (No hardcoded prof defaults)
        val allSections = remember(pyqItems, folders, searchQuery) {
            val fromContents = pyqItems.map { getPyqSectionName(it) }.filter { it.isNotBlank() }
            val fromFolders = folders.filter { it.subjectId.equals("pyq", ignoreCase = true) || it.sectionType.isNotBlank() }
                .map { it.sectionType.trim() }
                .filter { it.isNotBlank() && !it.equals("pyq", ignoreCase = true) && !it.equals("ALL", ignoreCase = true) && !it.equals("PDF", ignoreCase = true) && !it.equals("VIDEO", ignoreCase = true) }
            val detected = (fromContents + fromFolders).distinct().sorted()
            if (searchQuery.isBlank()) detected
            else detected.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                PYQHeader(
                    title = "PYQ Bank",
                    subtitle = null,
                    leadingEmoji = "📁",
                    onBackClick = onBackClick,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    backBtnBgColor = backBtnBgColor,
                    topPadding = effectiveTopPadding
                )
            },
            containerColor = themeBgColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Search...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = secondaryTextColor) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                if (allSections.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No PYQ sections",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(allSections) { sectionName ->
                    val paperCount = pyqItems.count { getPyqSectionName(it).equals(sectionName, ignoreCase = true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSection = sectionName
                                searchQuery = ""
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (currentTheme == 1) Color(0xFF334155) else Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📁",
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sectionName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = primaryTextColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$paperCount Papers",
                                    fontSize = 12.5.sp,
                                    color = secondaryTextColor
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Section",
                                tint = secondaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beautiful, well-inset Header Component:
 * - Positioned safely below the status bar and device rounded corners
 * - Has horizontal padding matching the content
 * - Clean circular back button and bold title
 */
@Composable
fun PYQHeader(
    title: String,
    subtitle: String? = null,
    leadingEmoji: String? = null,
    onBackClick: () -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    backBtnBgColor: Color,
    topPadding: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = backBtnBgColor,
                modifier = Modifier.size(40.dp),
                onClick = onBackClick
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            if (!leadingEmoji.isNullOrBlank()) {
                Text(text = leadingEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0).copy(alpha = 0.6f))
    }
}

/**
 * Reusable row item for a PYQ Paper / Chapter title.
 * Tapping directly opens the PDF via [onClick]!
 */
@Composable
fun PYQPaperRowItem(
    item: StudyContentItem,
    cardBgColor: Color = Color.White,
    primaryTextColor: Color = Color(0xFF0F172A),
    secondaryTextColor: Color = Color(0xFF64748B),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = primaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2563EB)
            ) {
                Text(
                    text = "View PDF",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
