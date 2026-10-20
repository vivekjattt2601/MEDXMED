package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY cachedAtTimestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE profCode = :profCode ORDER BY cachedAtTimestamp DESC")
    fun getNotesByProf(profCode: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isCachedOffline = 1 ORDER BY cachedAtTimestamp DESC")
    fun getCachedOfflineNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("UPDATE notes SET isCachedOffline = :isCached WHERE id = :id")
    suspend fun updateOfflineStatus(id: String, isCached: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNotesCount(): Int
}

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteLectureById(id: String)

    @Query("DELETE FROM lectures")
    suspend fun clearAllLectures()

    @Query("SELECT COUNT(*) FROM lectures")
    suspend fun getLecturesCount(): Int
}

@Dao
interface ShortDao {
    @Query("SELECT * FROM shorts ORDER BY createdAt DESC")
    fun getAllShorts(): Flow<List<ShortEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShorts(shorts: List<ShortEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShort(short: ShortEntity)

    @Query("UPDATE shorts SET title = :newTitle, description = :newDesc WHERE id = :id")
    suspend fun updateShort(id: String, newTitle: String, newDesc: String)

    @Query("DELETE FROM shorts WHERE id = :id")
    suspend fun deleteShortById(id: String)

    @Query("DELETE FROM shorts")
    suspend fun clearAllShorts()

    @Query("SELECT COUNT(*) FROM shorts")
    suspend fun getShortsCount(): Int
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<ContentFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ContentFolderEntity)

    @Query("UPDATE folders SET name = :newName, emoji = :newEmoji WHERE id = :id")
    suspend fun renameFolder(id: String, newName: String, newEmoji: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deleteFoldersByName(name: String)

    @Query("DELETE FROM folders WHERE id = :id OR LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deleteFolderByIdOrName(id: String, name: String)

    @Query("DELETE FROM folders WHERE subjectId = :subjectId")
    suspend fun deleteFoldersBySubjectId(subjectId: String)

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFoldersCount(): Int
}

@Dao
interface StudyContentDao {
    @Query("SELECT * FROM study_contents ORDER BY createdAt DESC")
    fun getAllContents(): Flow<List<StudyContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: StudyContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(contents: List<StudyContentEntity>)

    @Query("UPDATE study_contents SET title = :newTitle, description = :newDesc WHERE id = :id")
    suspend fun updateContent(id: String, newTitle: String, newDesc: String)

    @Query("UPDATE study_contents SET mediaUrl = :newUrl, cloudflareKey = :newUrl WHERE id = :id")
    suspend fun updateMediaUrl(id: String, newUrl: String)

    @Query("DELETE FROM study_contents WHERE id = :id")
    suspend fun deleteContentById(id: String)

    @Query("DELETE FROM study_contents WHERE folderId = :folderId")
    suspend fun deleteContentsByFolderId(folderId: String)

    @Query("DELETE FROM study_contents WHERE LOWER(TRIM(folderName)) = LOWER(TRIM(:folderName))")
    suspend fun deleteContentsByFolderName(folderName: String)

    @Query("DELETE FROM study_contents WHERE folderId = :folderId OR LOWER(TRIM(folderName)) = LOWER(TRIM(:folderName))")
    suspend fun deleteContentsByFolderIdOrName(folderId: String, folderName: String)

    @Query("DELETE FROM study_contents WHERE subjectId = :subjectId")
    suspend fun deleteContentsBySubjectId(subjectId: String)

    @Query("DELETE FROM study_contents")
    suspend fun clearAllContents()
}

@Dao
interface PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY completedAtTimestamp DESC")
    fun getAllSessions(): Flow<List<PomodoroSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSessionEntity)

    @Query("DELETE FROM pomodoro_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun clearAllSessions()
}
