package com.example.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MedRepository
import com.example.model.StudyContentItem

@Composable
fun ManagePYQBankView() {
    val contents by MedRepository.contents.collectAsState()
    val folders by MedRepository.folders.collectAsState()
    val pyqItems = remember(contents) { contents.filter { it.isPyq } }

    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubfolder by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States for Edit/Rename/Create
    var creatingSection by remember { mutableStateOf(false) }
    var creatingFolder by remember { mutableStateOf(false) }
    var renamingSection by remember { mutableStateOf<String?>(null) }
    var deletingSection by remember { mutableStateOf<String?>(null) }

    var renamingFolder by remember { mutableStateOf<String?>(null) }
    var deletingFolder by remember { mutableStateOf<String?>(null) }

    var renamingSubfolder by remember { mutableStateOf<String?>(null) }
    var deletingSubfolder by remember { mutableStateOf<String?>(null) }

    var editingPaper by remember { mutableStateOf<StudyContentItem?>(null) }
    var deletingPaper by remember { mutableStateOf<StudyContentItem?>(null) }

    // BackHandler for hierarchy
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

    Column(modifier = Modifier.fillMaxSize()) {
        // =========================================================================
        // LEVEL 4: INSIDE SUBFOLDER
        // =========================================================================
        if (selectedSection != null && selectedSubfolder != null) {
            val activeSection = selectedSection!!
            val activeFolder = selectedFolder
            val activeSubfolder = selectedSubfolder!!

            val papersInSub = remember(pyqItems, activeSection, activeFolder, activeSubfolder, searchQuery) {
                val list = pyqItems.filter {
                    getPyqSectionName(it).equals(activeSection, ignoreCase = true) &&
                            (activeFolder == null || it.folderName.trim().equals(activeFolder, ignoreCase = true)) &&
                            it.chapterName.trim().equals(activeSubfolder, ignoreCase = true)
                }
                if (searchQuery.isBlank()) list
                else list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
            }

            ManageHeader(
                title = activeSubfolder,
                subtitle = "",
                leadingEmoji = "📂",
                onBack = {
                    selectedSubfolder = null
                    searchQuery = ""
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF64748B)) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                if (papersInSub.isEmpty()) {
                    item {
                        EmptyStateBox("No papers")
                    }
                }

                items(papersInSub, key = { it.id }) { item ->
                    ManagePaperCard(
                        item = item,
                        onEdit = { editingPaper = item },
                        onDelete = { deletingPaper = item }
                    )
                }
            }
        }
        // =========================================================================
        // LEVEL 3: INSIDE FOLDER
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

            val subfolders = remember(folderItems, searchQuery) {
                val list = folderItems.map { it.chapterName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                if (searchQuery.isBlank()) list
                else list.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            val directPapers = remember(folderItems, searchQuery) {
                val list = folderItems.filter { it.chapterName.trim().isBlank() }
                if (searchQuery.isBlank()) list
                else list.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            ManageHeader(
                title = activeFolder,
                subtitle = "",
                leadingEmoji = "📁",
                onBack = {
                    selectedFolder = null
                    searchQuery = ""
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF64748B)) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                // Subfolders in this folder
                if (subfolders.isNotEmpty()) {
                    items(subfolders) { sub ->
                        val count = folderItems.count { it.chapterName.trim().equals(sub, ignoreCase = true) }
                        ManageSubfolderCard(
                            name = sub,
                            count = count,
                            onClick = {
                                selectedSubfolder = sub
                                searchQuery = ""
                            },
                            onRename = { renamingSubfolder = sub },
                            onDelete = { deletingSubfolder = sub }
                        )
                    }
                }

                // Direct papers in this folder
                if (directPapers.isNotEmpty()) {
                    items(directPapers, key = { it.id }) { item ->
                        ManagePaperCard(
                            item = item,
                            onEdit = { editingPaper = item },
                            onDelete = { deletingPaper = item }
                        )
                    }
                }

                if (subfolders.isEmpty() && directPapers.isEmpty()) {
                    item {
                        EmptyStateBox("No papers")
                    }
                }
            }
        }
        // =========================================================================
        // LEVEL 2: INSIDE SECTION
        // =========================================================================
        else if (selectedSection != null) {
            val activeSection = selectedSection!!
            val sectionItems = remember(pyqItems, activeSection) {
                pyqItems.filter { getPyqSectionName(it).equals(activeSection, ignoreCase = true) }
            }

            val foldersInSection = remember(sectionItems, folders, searchQuery) {
                val fromContents = sectionItems.map { it.folderName.trim() }
                val fromFolders = folders.filter { it.sectionType.equals(activeSection, ignoreCase = true) && it.name.isNotBlank() }.map { it.name.trim() }
                val list = (fromContents + fromFolders).filter { it.isNotBlank() }.distinct().sorted()
                if (searchQuery.isBlank()) list
                else list.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            val directSubfolders = remember(sectionItems, searchQuery) {
                val list = sectionItems.filter { it.folderName.trim().isBlank() }.map { it.chapterName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                if (searchQuery.isBlank()) list
                else list.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            val directPapers = remember(sectionItems, searchQuery) {
                val list = sectionItems.filter { it.folderName.trim().isBlank() && it.chapterName.trim().isBlank() }
                if (searchQuery.isBlank()) list
                else list.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            ManageHeader(
                title = activeSection,
                subtitle = "",
                leadingEmoji = "📁",
                onBack = {
                    selectedSection = null
                    searchQuery = ""
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Folders", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Button(
                            onClick = { creatingFolder = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("+ Create Folder", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF64748B)) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                // Folders
                if (foldersInSection.isNotEmpty()) {
                    items(foldersInSection) { fName ->
                        val count = sectionItems.count { it.folderName.trim().equals(fName, ignoreCase = true) }
                        ManageFolderCard(
                            name = fName,
                            count = count,
                            onClick = {
                                selectedFolder = fName
                                searchQuery = ""
                            },
                            onRename = { renamingFolder = fName },
                            onDelete = { deletingFolder = fName }
                        )
                    }
                }

                // Direct Subfolders (when folder is skipped)
                if (directSubfolders.isNotEmpty()) {
                    items(directSubfolders) { sub ->
                        val count = sectionItems.count { it.folderName.trim().isBlank() && it.chapterName.trim().equals(sub, ignoreCase = true) }
                        ManageSubfolderCard(
                            name = sub,
                            count = count,
                            onClick = {
                                selectedSubfolder = sub
                                searchQuery = ""
                            },
                            onRename = { renamingSubfolder = sub },
                            onDelete = { deletingSubfolder = sub }
                        )
                    }
                }

                // Direct Papers (when folder and subfolder are skipped)
                if (directPapers.isNotEmpty()) {
                    items(directPapers, key = { it.id }) { item ->
                        ManagePaperCard(
                            item = item,
                            onEdit = { editingPaper = item },
                            onDelete = { deletingPaper = item }
                        )
                    }
                }

                if (foldersInSection.isEmpty() && directSubfolders.isEmpty() && directPapers.isEmpty()) {
                    item {
                        EmptyStateBox("No papers")
                    }
                }
            }
        }
        // =========================================================================
        // LEVEL 1: ROOT SECTIONS LIST
        // =========================================================================
        else {
            val sections = remember(pyqItems, folders, searchQuery) {
                val fromContents = pyqItems.map { getPyqSectionName(it) }.filter { it.isNotBlank() }
                val fromFolders = folders.filter { it.subjectId.equals("pyq", ignoreCase = true) || it.sectionType.isNotBlank() }
                    .map { it.sectionType.trim() }
                    .filter { it.isNotBlank() && !it.equals("pyq", ignoreCase = true) && !it.equals("ALL", ignoreCase = true) && !it.equals("PDF", ignoreCase = true) && !it.equals("VIDEO", ignoreCase = true) }
                val detected = (fromContents + fromFolders).distinct().sorted()
                if (searchQuery.isBlank()) detected
                else detected.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sections", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Button(
                            onClick = { creatingSection = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("+ Create Section", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF64748B)) },
                        colors = com.example.ui.theme.medTextFieldColors(),
                        singleLine = true
                    )
                }

                if (sections.isEmpty()) {
                    item {
                        EmptyStateBox("No sections created")
                    }
                }

                items(sections) { secName ->
                    val count = pyqItems.count { getPyqSectionName(it).equals(secName, ignoreCase = true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSection = secName
                                searchQuery = ""
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📁", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = secName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count Papers",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { renamingSection = secName }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { deletingSection = secName }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS: RENAME & DELETE FOR EVERY HIERARCHY LEVEL
    // =========================================================================

    // 1. Rename Section Dialog
    renamingSection?.let { sec ->
        var newName by remember { mutableStateOf(sec) }
        Dialog(onDismissRequest = { renamingSection = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Rename Section", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Section Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { renamingSection = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    MedRepository.renamePYQSection(sec, newName.trim())
                                    if (selectedSection == sec) selectedSection = newName.trim()
                                }
                                renamingSection = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // 2. Delete Section Dialog
    deletingSection?.let { sec ->
        AlertDialog(
            onDismissRequest = { deletingSection = null },
            title = { Text("Delete Section \"$sec\"?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this section and all folders, subfolders, and papers in it?\n\nFiles will be permanently deleted from Cloudflare R2 and device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deletePYQSection(sec)
                        if (selectedSection == sec) selectedSection = null
                        deletingSection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingSection = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Rename Folder Dialog
    renamingFolder?.let { fName ->
        var newName by remember { mutableStateOf(fName) }
        Dialog(onDismissRequest = { renamingFolder = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Rename Folder", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { renamingFolder = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    MedRepository.renamePYQFolder(fName, newName.trim())
                                    if (selectedFolder == fName) selectedFolder = newName.trim()
                                }
                                renamingFolder = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // 4. Delete Folder Dialog
    deletingFolder?.let { fName ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("Delete Folder \"$fName\"?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this folder and all papers in it?\n\nFiles will be permanently deleted from Cloudflare R2 and device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deletePYQFolder(fName)
                        if (selectedFolder == fName) selectedFolder = null
                        deletingFolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingFolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Rename Subfolder Dialog
    renamingSubfolder?.let { sub ->
        var newName by remember { mutableStateOf(sub) }
        Dialog(onDismissRequest = { renamingSubfolder = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Rename Subfolder", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Subfolder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { renamingSubfolder = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    MedRepository.renamePYQSubfolder(
                                        oldSubfolder = sub,
                                        newSubfolder = newName.trim(),
                                        folderName = selectedFolder ?: "",
                                        section = selectedSection ?: ""
                                    )
                                    if (selectedSubfolder == sub) selectedSubfolder = newName.trim()
                                }
                                renamingSubfolder = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // 6. Delete Subfolder Dialog
    deletingSubfolder?.let { sub ->
        AlertDialog(
            onDismissRequest = { deletingSubfolder = null },
            title = { Text("Delete Subfolder \"$sub\"?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this subfolder and its papers?\n\nFiles will be permanently deleted from Cloudflare R2 and device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deletePYQSubfolder(
                            subfolder = sub,
                            folderName = selectedFolder ?: "",
                            section = selectedSection ?: ""
                        )
                        if (selectedSubfolder == sub) selectedSubfolder = null
                        deletingSubfolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Subfolder")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingSubfolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Edit Paper Dialog
    editingPaper?.let { paper ->
        var editTitle by remember { mutableStateOf(paper.title) }
        var editChapter by remember { mutableStateOf(paper.chapterName) }
        var editDesc by remember { mutableStateOf(paper.description) }

        Dialog(onDismissRequest = { editingPaper = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Edit Paper / Chapter", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Paper Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editChapter,
                        onValueChange = { editChapter = it },
                        label = { Text("Subfolder / Chapter Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description / Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = com.example.ui.theme.medTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { editingPaper = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank()) {
                                    MedRepository.editPYQItem(
                                        id = paper.id,
                                        newTitle = editTitle.trim(),
                                        newChapter = editChapter.trim(),
                                        newDesc = editDesc.trim()
                                    )
                                }
                                editingPaper = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // 8. Delete Paper Dialog
    deletingPaper?.let { paper ->
        AlertDialog(
            onDismissRequest = { deletingPaper = null },
            title = { Text("Delete Paper?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete \"${paper.title}\"?\n\nThis will remove the file from Cloudflare R2 and your application.") },
            confirmButton = {
                Button(
                    onClick = {
                        MedRepository.deleteStudyContent(paper)
                        deletingPaper = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingPaper = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ManageHeader(
    title: String,
    subtitle: String,
    leadingEmoji: String,
    onBack: () -> Unit
) {
    Surface(
        color = Color(0xFFEFF6FF),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2563EB))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(leadingEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E3A8A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ManageFolderCard(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("📁", fontSize = 20.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("$count Paper(s)", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ManageSubfolderCard(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("📂", fontSize = 20.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("$count Paper(s)", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ManagePaperCard(
    item: StudyContentItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.chapterName.isNotBlank()) {
                        Text(
                            text = "Chapter/Sub: ${item.chapterName}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (item.cloudflareKey.isNotBlank()) {
                        Text(
                            text = "Cloudflare R2: ✓ Synced",
                            fontSize = 10.5.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color(0xFF94A3B8), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}
