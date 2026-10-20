package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.*

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val profCode: String,
    val title: String,
    val chapter: String,
    val pagesCount: Int,
    val readMinutes: Int,
    val highYieldSummary: String,
    val pdfMockUrl: String = "",
    val isCachedOffline: Boolean = true,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): NoteItem {
        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
        return NoteItem(
            id = id,
            subjectId = subjectId,
            prof = prof,
            title = title,
            chapter = chapter,
            pagesCount = pagesCount,
            readMinutes = readMinutes,
            highYieldSummary = highYieldSummary,
            pdfMockUrl = pdfMockUrl,
            isCachedOffline = isCachedOffline
        )
    }

    companion object {
        fun fromDomain(domain: NoteItem): NoteEntity {
            return NoteEntity(
                id = domain.id,
                subjectId = domain.subjectId,
                profCode = domain.prof.code,
                title = domain.title,
                chapter = domain.chapter,
                pagesCount = domain.pagesCount,
                readMinutes = domain.readMinutes,
                highYieldSummary = domain.highYieldSummary,
                pdfMockUrl = domain.pdfMockUrl,
                isCachedOffline = domain.isCachedOffline
            )
        }
    }
}

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val profCode: String,
    val title: String,
    val chapter: String,
    val faculty: String,
    val duration: String,
    val videoUrl: String,
    val isWatched: Boolean = false
) {
    fun toDomain(): LectureItem {
        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
        return LectureItem(
            id = id,
            subjectId = subjectId,
            prof = prof,
            title = title,
            chapter = chapter,
            faculty = faculty,
            duration = duration,
            videoUrl = videoUrl,
            isWatched = isWatched
        )
    }

    companion object {
        fun fromDomain(domain: LectureItem): LectureEntity {
            return LectureEntity(
                id = domain.id,
                subjectId = domain.subjectId,
                profCode = domain.prof.code,
                title = domain.title,
                chapter = domain.chapter,
                faculty = domain.faculty,
                duration = domain.duration,
                videoUrl = domain.videoUrl,
                isWatched = domain.isWatched
            )
        }
    }
}

@Entity(tableName = "shorts")
data class ShortEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val youtubeUrl: String = "",
    val youtubeVideoId: String = "",
    val thumbnailUrl: String = "",
    val cloudflareKey: String = "",
    val subjectName: String,
    val emoji: String,
    val profCode: String,
    val doctorName: String,
    val durationSec: Int,
    val likesCount: Int,
    val mnemonicTip: String,
    val audioTitle: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ShortItem {
        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
        return ShortItem(
            id = id,
            title = title,
            description = description,
            youtubeUrl = youtubeUrl,
            youtubeVideoId = youtubeVideoId,
            thumbnailUrl = thumbnailUrl,
            cloudflareKey = cloudflareKey,
            subjectName = subjectName,
            emoji = emoji,
            prof = prof,
            doctorName = doctorName,
            durationSec = durationSec,
            likesCount = likesCount,
            mnemonicTip = mnemonicTip,
            audioTitle = audioTitle,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: ShortItem): ShortEntity {
            return ShortEntity(
                id = domain.id,
                title = domain.title,
                description = domain.description,
                youtubeUrl = domain.youtubeUrl,
                youtubeVideoId = domain.youtubeVideoId,
                thumbnailUrl = domain.thumbnailUrl,
                cloudflareKey = domain.cloudflareKey,
                subjectName = domain.subjectName,
                emoji = domain.emoji,
                profCode = domain.prof.code,
                doctorName = domain.doctorName,
                durationSec = domain.durationSec,
                likesCount = domain.likesCount,
                mnemonicTip = domain.mnemonicTip,
                audioTitle = domain.audioTitle,
                createdAt = domain.createdAt
            )
        }
    }
}

@Entity(tableName = "folders")
data class ContentFolderEntity(
    @PrimaryKey val id: String,
    val profCode: String,
    val subjectId: String,
    val name: String,
    val emoji: String = "📁",
    val sectionType: String = "ALL"
) {
    fun toDomain(): ContentFolder {
        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
        return ContentFolder(
            id = id,
            prof = prof,
            subjectId = subjectId,
            name = name,
            emoji = emoji,
            sectionType = sectionType
        )
    }

    companion object {
        fun fromDomain(domain: ContentFolder): ContentFolderEntity {
            return ContentFolderEntity(
                id = domain.id,
                profCode = domain.prof.code,
                subjectId = domain.subjectId,
                name = domain.name,
                emoji = domain.emoji,
                sectionType = domain.sectionType
            )
        }
    }
}

@Entity(tableName = "study_contents")
data class StudyContentEntity(
    @PrimaryKey val id: String,
    val profCode: String,
    val subjectId: String,
    val folderId: String,
    val folderName: String,
    val chapterName: String,
    val typeName: String, // VIDEO or PDF
    val title: String,
    val description: String,
    val mediaUrl: String,
    val cloudflareKey: String,
    val createdAt: Long
) {
    fun toDomain(): StudyContentItem {
        val prof = ProfYear.values().firstOrNull { it.code == profCode } ?: ProfYear.FIRST_PROF
        val type = if (typeName == "PDF") StudyContentType.PDF else StudyContentType.VIDEO
        return StudyContentItem(
            id = id,
            prof = prof,
            subjectId = subjectId,
            folderId = folderId,
            folderName = folderName,
            chapterName = chapterName,
            type = type,
            title = title,
            description = description,
            mediaUrl = mediaUrl,
            cloudflareKey = cloudflareKey,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: StudyContentItem): StudyContentEntity {
            return StudyContentEntity(
                id = domain.id,
                profCode = domain.prof.code,
                subjectId = domain.subjectId,
                folderId = domain.folderId,
                folderName = domain.folderName,
                chapterName = domain.chapterName,
                typeName = domain.type.name,
                title = domain.title,
                description = domain.description,
                mediaUrl = domain.mediaUrl,
                cloudflareKey = domain.cloudflareKey,
                createdAt = domain.createdAt
            )
        }
    }
}

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val durationMinutes: Int,
    val completedAtTimestamp: Long,
    val formattedDate: String,
    val modeName: String
)
