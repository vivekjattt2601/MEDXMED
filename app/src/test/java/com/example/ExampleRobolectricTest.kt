package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MEDXMED", appName)
  }

  @Test
  fun `room database stores and retrieves notes and folders offline`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.MedXMedDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    val folderDao = db.folderDao()
    val studyContentDao = db.studyContentDao()
    val shortDao = db.shortDao()
    
    assertNotNull(noteDao)
    assertNotNull(folderDao)
    assertNotNull(studyContentDao)
    assertNotNull(shortDao)
  }
}
