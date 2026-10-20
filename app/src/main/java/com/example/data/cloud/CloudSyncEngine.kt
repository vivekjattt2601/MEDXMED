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
     * Publishes the current catalog of Folders, Notes/PDFs, and Shorts
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

            // 2. Study Contents (Notes, PDFs, Videos)
            val contentsArr = JSONArray()
            MedRepository.contents.value.forEach { c ->
                val isDel = MedRepository.isContentDeleted(c.id)
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

            // 4. Deleted Records
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
     * all admin-uploaded Notes, PDFs, and Shorts.
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

            // 3. Sync Study Contents (Notes, PDFs, Videos)
            val contentsArr = root.optJSONArray("contents")
            if (contentsArr != null && db != null) {
                val toInsert = mutableListOf<StudyContentEntity>()
                for (i in 0 until contentsArr.length()) {
                    val obj = contentsArr.getJSONObject(i)
                    val id = obj.optString("id")
                    val isDel = MedRepository.isContentDeleted(id)
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
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
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
            root.put("deletedRecords", JSONObject())
            CloudflareR2Manager.uploadString(CATALOG_OBJECT_KEY, root.toString(2), "application/json")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
