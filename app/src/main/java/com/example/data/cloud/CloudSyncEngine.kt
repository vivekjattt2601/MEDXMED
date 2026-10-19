package com.example.data.cloud

import android.content.Context
import com.example.data.MedRepository
import com.example.data.local.*
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object CloudSyncEngine {

    const val CATALOG_OBJECT_KEY = "catalog.json"
    val CATALOG_PUBLIC_URL: String get() = "${CloudflareR2Manager.publicBaseUrl.value.trimEnd('/')}/$CATALOG_OBJECT_KEY"

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<String>("Online (Direct R2 Storage)")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    /**
     * Publishes the current catalog of Folders, Notes/PDFs, Shorts, Flashcards, PYQs, and MCQs
     * to Cloudflare R2 so all users see the exact same content.
     */
    suspend fun publishCatalogToCloud(): Result<String> = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Cloudflare R2 is not configured. Please set R2 credentials first.")
            )
        }

        try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("updatedAt", System.currentTimeMillis())

            // 1. Folders
            val foldersArr = JSONArray()
            MedRepository.folders.value.forEach { f ->
                if (!MedRepository.isFolderDeleted(f.id, f.name)) {
                    val obj = JSONObject()
                    obj.put("id", f.id)
                    obj.put("prof", f.prof.code)
                    obj.put("subjectId", f.subjectId)
                    obj.put("name", f.name)
                    obj.put("emoji", f.emoji)
                    obj.put("sectionType", f.sectionType)
                    foldersArr.put(obj)
                }
            }
            root.put("folders", foldersArr)

            // 2. Study Contents (Notes, PDFs, Videos, PYQs)
            val contentsArr = JSONArray()
            MedRepository.contents.value.forEach { c ->
                val isDel = MedRepository.isContentDeleted(c.id) || (c.isPyq && MedRepository.isPYQDeleted(c.id))
                if (!isDel) {
                    val obj = JSONObject()
                    obj.put("id", c.id)
                    obj.put("prof", c.prof.code)
                    obj.put("subjectId", c.subjectId)
                    obj.put("folderId", c.folderId)
                    obj.put("folderName", c.folderName)
                    obj.put("chapterName", c.chapterName)
                    obj.put("type", c.type.name)
                    obj.put("title", c.title)
                    obj.put("description", c.description)
                    obj.put("mediaUrl", c.mediaUrl)
                    obj.put("cloudflareKey", c.cloudflareKey)
                    obj.put("createdAt", c.createdAt)
                    obj.put("isPyq", c.isPyq)
                    contentsArr.put(obj)
                }
            }
            root.put("contents", contentsArr)

            // 3. Shorts
            val shortsArr = JSONArray()
            MedRepository.shorts.value.forEach { s ->
                if (!MedRepository.isShortDeleted(s.id)) {
                    val obj = JSONObject()
                    obj.put("id", s.id)
                    obj.put("title", s.title)
                    obj.put("description", s.description)
                    obj.put("youtubeUrl", s.youtubeUrl)
                    obj.put("youtubeVideoId", s.youtubeVideoId)
                    obj.put("thumbnailUrl", s.thumbnailUrl)
                    obj.put("cloudflareKey", s.cloudflareKey)
                    obj.put("subjectName", s.subjectName)
                    obj.put("emoji", s.emoji)
                    obj.put("prof", s.prof.code)
                    obj.put("doctorName", s.doctorName)
                    obj.put("durationSec", s.durationSec)
                    obj.put("likesCount", s.likesCount)
                    obj.put("mnemonicTip", s.mnemonicTip)
                    obj.put("audioTitle", s.audioTitle)
                    obj.put("createdAt", s.createdAt)
                    shortsArr.put(obj)
                }
            }
            root.put("shorts", shortsArr)

            // 4. MCQ Sections
            val mcqSectionsArr = JSONArray()
            MedRepository.mcqSections.value.forEach { sec ->
                val obj = JSONObject()
                obj.put("id", sec.id)
                obj.put("title", sec.title)
                obj.put("subtitle", sec.subtitle)
                obj.put("emoji", sec.emoji)
                obj.put("colorHex1", sec.colorHex1)
                obj.put("colorHex2", sec.colorHex2)
                obj.put("prof", sec.prof.code)
                obj.put("orderIndex", sec.orderIndex)
                mcqSectionsArr.put(obj)
            }
            root.put("mcqSections", mcqSectionsArr)

            // 5. MCQs
            val mcqsArr = JSONArray()
            MedRepository.mcqs.value.forEach { m ->
                if (!MedRepository.isMCQDeleted(m.id)) {
                    val obj = JSONObject()
                    obj.put("id", m.id)
                    obj.put("prof", m.prof.code)
                    obj.put("subjectId", m.subjectId)
                    obj.put("sectionId", m.sectionId)
                    obj.put("sectionName", m.sectionName)
                    obj.put("folderId", m.folderId)
                    obj.put("folderName", m.folderName)
                    obj.put("subfolderName", m.subfolderName)
                    obj.put("chapterName", m.chapterName)
                    obj.put("question", m.question)
                    obj.put("questionImageUrl", m.questionImageUrl)
                    val opts = JSONArray()
                    m.options.forEach { opt ->
                        val optObj = JSONObject()
                        optObj.put("id", opt.id)
                        optObj.put("text", opt.text)
                        optObj.put("imageUrl", opt.imageUrl)
                        opts.put(optObj)
                    }
                    obj.put("options", opts)
                    obj.put("correctOptionIndex", m.correctOptionIndex)
                    obj.put("explanation", m.explanation)
                    obj.put("explanationImageUrl", m.explanationImageUrl)
                    obj.put("difficulty", m.difficulty)
                    obj.put("tags", m.tags)
                    obj.put("createdAt", m.createdAt)
                    obj.put("questionType", m.questionType)
                    obj.put("assertion", m.assertion)
                    obj.put("reason", m.reason)
                    obj.put("statements", JSONArray(m.statements))
                    obj.put("matchColumnA", JSONArray(m.matchColumnA))
                    obj.put("matchColumnB", JSONArray(m.matchColumnB))
                    obj.put("flowchartSteps", JSONArray(m.flowchartSteps))
                    mcqsArr.put(obj)
                }
            }
            root.put("mcqs", mcqsArr)

            // 6. Flashcards
            val flashcardsArr = JSONArray()
            MedRepository.flashcards.value.forEach { f ->
                if (!MedRepository.isFlashcardDeleted(f.id)) {
                    val obj = JSONObject()
                    obj.put("id", f.id)
                    obj.put("subjectId", f.subjectId)
                    obj.put("prof", f.prof.code)
                    obj.put("folderId", f.folderId)
                    obj.put("folderName", f.folderName)
                    obj.put("subfolderName", f.subfolderName)
                    obj.put("chapterName", f.chapterName)
                    obj.put("questionType", f.questionType.name)
                    obj.put("question", f.question)
                    obj.put("questionOptions", JSONArray(f.questionOptions))
                    obj.put("questionImageUrl", f.questionImageUrl)
                    obj.put("answer", f.answer)
                    obj.put("answerImageUrl", f.answerImageUrl)
                    obj.put("explanation", f.explanation)
                    obj.put("hint", f.hint)
                    obj.put("tags", f.tags)
                    obj.put("mnemonic", f.mnemonic ?: "")
                    obj.put("clinicalPearl", f.clinicalPearl ?: "")
                    obj.put("isMastered", f.isMastered)
                    obj.put("isSavedOffline", f.isSavedOffline)
                    obj.put("difficulty", f.difficulty)
                    flashcardsArr.put(obj)
                }
            }
            root.put("flashcards", flashcardsArr)

            // 7. Deleted Records
            val delObj = JSONObject()
            MedRepository.getDeletedRecords().forEach { (k, v) ->
                delObj.put(k, JSONArray(v))
            }
            root.put("deletedRecords", delObj)

            val jsonContent = root.toString(2)
            val uploadRes = CloudflareR2Manager.uploadString(CATALOG_OBJECT_KEY, jsonContent, "application/json")
            if (uploadRes.isSuccess) {
                _lastSyncStatus.value = "✓ Synced & Published to Cloudflare R2"
                _lastSyncTimestamp.value = System.currentTimeMillis()
                Result.success(uploadRes.getOrThrow())
            } else {
                Result.failure(uploadRes.exceptionOrNull() ?: Exception("Upload catalog failed"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Synchronizes content from Cloudflare R2 catalog into local storage so users receive
     * all admin-uploaded Notes, PDFs, Shorts, Flashcards, PYQs, and MCQs.
     */
    suspend fun syncFromCloud(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare R2 not configured"))
        }

        _isSyncing.value = true
        try {
            val downloadRes = CloudflareR2Manager.downloadString(CATALOG_OBJECT_KEY)
            if (downloadRes.isFailure) {
                _isSyncing.value = false
                _lastSyncStatus.value = "Cloud online (no remote catalog found)"
                return@withContext Result.success(0)
            }

            val rawJson = downloadRes.getOrThrow().trim()
            if (rawJson.isBlank()) {
                _isSyncing.value = false
                return@withContext Result.success(0)
            }

            val root = JSONObject(rawJson)

            // 1. Merge Deleted Records so stale local items are purged
            val delObj = root.optJSONObject("deletedRecords")
            if (delObj != null) {
                val cloudDeleted = mutableMapOf<String, Set<String>>()
                delObj.keys().forEach { key ->
                    val arr = delObj.optJSONArray(key)
                    if (arr != null) {
                        val set = mutableSetOf<String>()
                        for (i in 0 until arr.length()) {
                            set.add(arr.optString(i))
                        }
                        cloudDeleted[key] = set
                    }
                }
                MedRepository.mergeDeletedRecordsFromCloud(cloudDeleted)
            }

            val db = MedRepository.database
            var count = 0

            // 2. Sync Folders
            val foldersArr = root.optJSONArray("folders")
            if (foldersArr != null && db != null) {
                for (i in 0 until foldersArr.length()) {
                    val obj = foldersArr.getJSONObject(i)
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    if (!MedRepository.isFolderDeleted(id, name)) {
                        val profCode = obj.optString("prof", "1st")
                        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                        val f = ContentFolder(
                            id = id,
                            prof = prof,
                            subjectId = obj.optString("subjectId", "general"),
                            name = name,
                            emoji = obj.optString("emoji", "📁"),
                            sectionType = obj.optString("sectionType", "ALL")
                        )
                        db.folderDao().insertFolder(ContentFolderEntity.fromDomain(f))
                        count++
                    }
                }
            }

            // 3. Sync Study Contents (Notes, PDFs, Videos, PYQs)
            val contentsArr = root.optJSONArray("contents")
            if (contentsArr != null && db != null) {
                val toInsert = mutableListOf<StudyContentEntity>()
                for (i in 0 until contentsArr.length()) {
                    val obj = contentsArr.getJSONObject(i)
                    val id = obj.optString("id")
                    val isPyq = obj.optBoolean("isPyq", false)
                    val isDel = MedRepository.isContentDeleted(id) || (isPyq && MedRepository.isPYQDeleted(id))
                    if (!isDel) {
                        val profCode = obj.optString("prof", "1st")
                        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                        val typeName = obj.optString("type", "PDF")
                        val type = if (typeName.equals("VIDEO", ignoreCase = true)) StudyContentType.VIDEO else StudyContentType.PDF
                        val item = StudyContentItem(
                            id = id,
                            prof = prof,
                            subjectId = obj.optString("subjectId", "general"),
                            folderId = obj.optString("folderId", ""),
                            folderName = obj.optString("folderName", ""),
                            chapterName = obj.optString("chapterName", ""),
                            type = type,
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            cloudflareKey = obj.optString("cloudflareKey", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isPyq = isPyq
                        )
                        toInsert.add(StudyContentEntity.fromDomain(item))
                        count++
                    }
                }
                if (toInsert.isNotEmpty()) {
                    db.studyContentDao().insertContents(toInsert)
                }
            }

            // 4. Sync Shorts
            val shortsArr = root.optJSONArray("shorts")
            if (shortsArr != null && db != null) {
                val toInsert = mutableListOf<ShortEntity>()
                for (i in 0 until shortsArr.length()) {
                    val obj = shortsArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (!MedRepository.isShortDeleted(id)) {
                        val profCode = obj.optString("prof", "1st")
                        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                        val s = ShortItem(
                            id = id,
                            title = obj.optString("title", "High Yield Short"),
                            description = obj.optString("description", ""),
                            youtubeUrl = obj.optString("youtubeUrl", ""),
                            youtubeVideoId = obj.optString("youtubeVideoId", ""),
                            thumbnailUrl = obj.optString("thumbnailUrl", ""),
                            cloudflareKey = obj.optString("cloudflareKey", ""),
                            subjectName = obj.optString("subjectName", "General"),
                            emoji = obj.optString("emoji", "⚡"),
                            prof = prof,
                            doctorName = obj.optString("doctorName", "Faculty"),
                            durationSec = obj.optInt("durationSec", 60),
                            likesCount = obj.optInt("likesCount", 120),
                            mnemonicTip = obj.optString("mnemonicTip", ""),
                            audioTitle = obj.optString("audioTitle", "MedXMed Rapid Mnemonics 🩺"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        toInsert.add(ShortEntity.fromDomain(s))
                        count++
                    }
                }
                if (toInsert.isNotEmpty()) {
                    db.shortDao().insertShorts(toInsert)
                }
            }

            // 5. Sync MCQ Sections
            val mcqSecArr = root.optJSONArray("mcqSections")
            if (mcqSecArr != null && db != null) {
                val toInsert = mutableListOf<MCQSectionEntity>()
                for (i in 0 until mcqSecArr.length()) {
                    val obj = mcqSecArr.getJSONObject(i)
                    val profCode = obj.optString("prof", "1st")
                    val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                    val sec = MCQSection(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        subtitle = obj.optString("subtitle", ""),
                        emoji = obj.optString("emoji", "🎯"),
                        colorHex1 = obj.optString("colorHex1", "#0D9488"),
                        colorHex2 = obj.optString("colorHex2", "#0F766E"),
                        prof = prof,
                        orderIndex = obj.optInt("orderIndex", 0)
                    )
                    toInsert.add(MCQSectionEntity.fromDomain(sec))
                    count++
                }
                if (toInsert.isNotEmpty()) {
                    db.mcqSectionDao().insertSections(toInsert)
                }
            }

            // 6. Sync MCQs
            val mcqsArr = root.optJSONArray("mcqs")
            if (mcqsArr != null && db != null) {
                val toInsert = mutableListOf<MCQEntity>()
                for (i in 0 until mcqsArr.length()) {
                    val obj = mcqsArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (!MedRepository.isMCQDeleted(id)) {
                        val profCode = obj.optString("prof", "1st")
                        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                        val opts = mutableListOf<MCQOption>()
                        val optsArr = obj.optJSONArray("options")
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) {
                                val optObj = optsArr.optJSONObject(j)
                                if (optObj != null) {
                                    opts.add(MCQOption(id = optObj.optString("id", "opt_$j"), text = optObj.optString("text", ""), imageUrl = optObj.optString("imageUrl", "")))
                                } else {
                                    opts.add(MCQOption(id = "opt_$j", text = optsArr.optString(j)))
                                }
                            }
                        }
                        val stmtsList = mutableListOf<String>()
                        val sArr = obj.optJSONArray("statements")
                        if (sArr != null) {
                            for (j in 0 until sArr.length()) stmtsList.add(sArr.optString(j))
                        }
                        val colAList = mutableListOf<String>()
                        val aArr = obj.optJSONArray("matchColumnA")
                        if (aArr != null) {
                            for (j in 0 until aArr.length()) colAList.add(aArr.optString(j))
                        }
                        val colBList = mutableListOf<String>()
                        val bArr = obj.optJSONArray("matchColumnB")
                        if (bArr != null) {
                            for (j in 0 until bArr.length()) colBList.add(bArr.optString(j))
                        }
                        val fcList = mutableListOf<String>()
                        val fArr = obj.optJSONArray("flowchartSteps")
                        if (fArr != null) {
                            for (j in 0 until fArr.length()) fcList.add(fArr.optString(j))
                        }

                        val mcq = MCQItem(
                            id = id,
                            prof = prof,
                            subjectId = obj.optString("subjectId", "mcq"),
                            sectionId = obj.optString("sectionId", ""),
                            sectionName = obj.optString("sectionName", ""),
                            folderId = obj.optString("folderId", ""),
                            folderName = obj.optString("folderName", ""),
                            subfolderName = obj.optString("subfolderName", ""),
                            chapterName = obj.optString("chapterName", ""),
                            question = obj.optString("question", ""),
                            questionImageUrl = obj.optString("questionImageUrl", ""),
                            options = opts,
                            correctOptionIndex = obj.optInt("correctOptionIndex", 0),
                            explanation = obj.optString("explanation", ""),
                            explanationImageUrl = obj.optString("explanationImageUrl", ""),
                            difficulty = obj.optString("difficulty", "Medium"),
                            tags = obj.optString("tags", "High-Yield"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            questionType = obj.optString("questionType", "MCQ"),
                            assertion = obj.optString("assertion", ""),
                            reason = obj.optString("reason", ""),
                            statements = stmtsList,
                            matchColumnA = colAList,
                            matchColumnB = colBList,
                            flowchartSteps = fcList
                        )
                        toInsert.add(MCQEntity.fromDomain(mcq))
                        count++
                    }
                }
                if (toInsert.isNotEmpty()) {
                    db.mcqDao().insertMCQs(toInsert)
                }
            }

            // 7. Sync Flashcards
            val flashcardsArr = root.optJSONArray("flashcards")
            if (flashcardsArr != null && db != null) {
                val toInsert = mutableListOf<FlashcardEntity>()
                for (i in 0 until flashcardsArr.length()) {
                    val obj = flashcardsArr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (!MedRepository.isFlashcardDeleted(id)) {
                        val profCode = obj.optString("prof", "1st")
                        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
                        val qTypeName = obj.optString("questionType", "TEXT")
                        val qType = try { QuestionType.valueOf(qTypeName) } catch (_: Exception) { QuestionType.TEXT }
                        val opts = mutableListOf<String>()
                        val optsArr = obj.optJSONArray("questionOptions")
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) opts.add(optsArr.optString(j))
                        }
                        val card = FlashcardItem(
                            id = id,
                            subjectId = obj.optString("subjectId", "flashcard"),
                            prof = prof,
                            folderId = obj.optString("folderId", ""),
                            folderName = obj.optString("folderName", ""),
                            subfolderName = obj.optString("subfolderName", ""),
                            chapterName = obj.optString("chapterName", ""),
                            questionType = qType,
                            question = obj.optString("question", ""),
                            questionOptions = opts,
                            questionImageUrl = obj.optString("questionImageUrl", ""),
                            answer = obj.optString("answer", ""),
                            answerImageUrl = obj.optString("answerImageUrl", ""),
                            explanation = obj.optString("explanation", ""),
                            hint = obj.optString("hint", ""),
                            tags = obj.optString("tags", "High-Yield"),
                            mnemonic = obj.optString("mnemonic").takeIf { it.isNotBlank() },
                            clinicalPearl = obj.optString("clinicalPearl").takeIf { it.isNotBlank() },
                            isMastered = obj.optBoolean("isMastered", false),
                            isSavedOffline = obj.optBoolean("isSavedOffline", true),
                            difficulty = obj.optString("difficulty", "Medium")
                        )
                        toInsert.add(FlashcardEntity.fromDomain(card))
                        count++
                    }
                }
                if (toInsert.isNotEmpty()) {
                    db.flashcardDao().insertFlashcards(toInsert)
                }
            }

            _lastSyncStatus.value = "✓ Synced $count items from Cloudflare R2"
            _lastSyncTimestamp.value = System.currentTimeMillis()
            _isSyncing.value = false
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            _lastSyncStatus.value = "Sync error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * Uploads a chapter's Flashcards JSON file to Cloudflare R2 storage.
     */
    suspend fun uploadFlashcardsChapterToR2(
        profName: String,
        folderName: String,
        subfolderName: String,
        chapterName: String,
        cards: List<FlashcardItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured() || cards.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("R2 not configured or cards empty"))
        }
        try {
            val cleanProf = profName.ifBlank { "FIRST_PROF" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanFolder = folderName.ifBlank { "general_folder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanSub = subfolderName.ifBlank { "general_subfolder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanChapter = chapterName.trim().ifBlank { "flashcards" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val objectKey = "flashcards/$cleanProf/$cleanFolder/$cleanSub/$cleanChapter.json"

            val jsonArray = JSONArray()
            cards.forEach { card ->
                val obj = JSONObject()
                obj.put("id", card.id)
                obj.put("question", card.question)
                obj.put("answer", card.answer)
                obj.put("hint", card.hint)
                obj.put("explanation", card.explanation)
                obj.put("tags", card.tags)
                obj.put("questionImageUrl", card.questionImageUrl)
                obj.put("answerImageUrl", card.answerImageUrl)
                obj.put("folder", card.folderName)
                obj.put("subfolder", card.subfolderName)
                obj.put("chapter", card.chapterName)
                jsonArray.put(obj)
            }

            CloudflareR2Manager.uploadString(objectKey, jsonArray.toString(2), "application/json")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a chapter's MCQ JSON file to Cloudflare R2 storage.
     */
    suspend fun uploadMCQChapterToR2(
        sectionName: String,
        folderName: String,
        subfolderName: String,
        chapterName: String,
        mcqs: List<MCQItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured() || mcqs.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("R2 not configured or mcqs empty"))
        }
        try {
            val cleanSec = sectionName.ifBlank { "general_section" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanFolder = folderName.ifBlank { "general_folder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanSub = subfolderName.ifBlank { "general_subfolder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanChapter = chapterName.trim().ifBlank { "mcqs" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val objectKey = "mcqs/$cleanSec/$cleanFolder/$cleanSub/$cleanChapter.json"

            val jsonArray = JSONArray()
            mcqs.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("question", item.question)
                val opts = JSONArray()
                item.options.forEach { opt ->
                    val o = JSONObject()
                    o.put("id", opt.id)
                    o.put("text", opt.text)
                    o.put("imageUrl", opt.imageUrl)
                    opts.put(o)
                }
                obj.put("options", opts)
                obj.put("correctOptionIndex", item.correctOptionIndex)
                obj.put("answer", item.correctOptionIndex)
                obj.put("explanation", item.explanation)
                obj.put("difficulty", item.difficulty)
                obj.put("tags", item.tags)
                obj.put("questionType", item.questionType)
                obj.put("assertion", item.assertion)
                obj.put("reason", item.reason)
                obj.put("statements", JSONArray(item.statements))
                obj.put("matchColumnA", JSONArray(item.matchColumnA))
                obj.put("matchColumnB", JSONArray(item.matchColumnB))
                obj.put("flowchartSteps", JSONArray(item.flowchartSteps))
                obj.put("section", item.sectionName)
                obj.put("folder", item.folderName)
                obj.put("subfolder", item.subfolderName)
                obj.put("chapter", item.chapterName)
                jsonArray.put(obj)
            }

            CloudflareR2Manager.uploadString(objectKey, jsonArray.toString(2), "application/json")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudChapter(sectionName: String, folderName: String, subfolderName: String, chapterName: String) = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) return@withContext
        try {
            val cleanSec = sectionName.ifBlank { "general_section" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanFolder = folderName.ifBlank { "general_folder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanSub = subfolderName.ifBlank { "general_subfolder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanChapter = chapterName.trim().ifBlank { "general" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            CloudflareR2Manager.deleteFile("mcqs/${cleanSec}/${cleanFolder}/${cleanSub}/${cleanChapter}.json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCloudFlashcardChapter(profName: String, folderName: String, subfolderName: String, chapterName: String) = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) return@withContext
        try {
            val cleanProf = profName.ifBlank { "FIRST_PROF" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanFolder = folderName.ifBlank { "general_folder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanSub = subfolderName.ifBlank { "general_subfolder" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val cleanChapter = chapterName.trim().ifBlank { "general" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            CloudflareR2Manager.deleteFile("flashcards/${cleanProf}/${cleanFolder}/${cleanSub}/${cleanChapter}.json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun purgeLegacyCloudCatalogs() = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) return@withContext
        try {
            CloudflareR2Manager.deleteFile("mcqs/sections.json")
            CloudflareR2Manager.deleteFile("flashcards/manifest.json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun publishEmptyCatalogToCloud(): Result<String> = withContext(Dispatchers.IO) {
        if (!CloudflareR2Manager.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Cloudflare R2 not configured"))
        }
        try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("updatedAt", System.currentTimeMillis())
            root.put("folders", JSONArray())
            root.put("contents", JSONArray())
            root.put("shorts", JSONArray())
            root.put("mcqSections", JSONArray())
            root.put("mcqs", JSONArray())
            root.put("flashcards", JSONArray())
            root.put("deletedRecords", JSONObject())
            CloudflareR2Manager.uploadString(CATALOG_OBJECT_KEY, root.toString(2), "application/json")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
