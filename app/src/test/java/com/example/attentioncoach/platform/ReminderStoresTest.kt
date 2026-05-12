package com.example.attentioncoach.platform

import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.attentioncoach.domain.ActiveWork
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderStoresTest {
    @Test
    fun focusSessionStorePersistsFullActiveWork() = runBlocking {
        val store = FocusSessionStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File.createTempFile("focus-session", ".preferences_pb") }
            )
        )
        val work = ActiveWork(
            taskId = 42L,
            isActive = true,
            plannedDurationMinutes = 30,
            startedAtMillis = 1000L,
            accumulatedActiveMillis = 5000L,
            pauseStartedAtMillis = 7000L,
            isPaused = true
        )

        store.setActive(work)

        assertTrue(store.isActive())
        assertEquals(42L, store.activeTaskId())
        assertEquals(work, store.activeWork.first())

        store.clearActive()

        assertFalse(store.isActive())
        assertEquals(null, store.activeTaskId())
    }

    @Test
    fun startReminderStoreDefersAndReleasesTasks() {
        val store = StartReminderStore(FakeSharedPreferences())

        store.defer(10L)
        store.defer(12L)

        assertEquals(setOf(10L, 12L), store.deferredTaskIds())
        assertEquals(emptySet<Long>(), store.activeDueIds())

        assertEquals(setOf(10L, 12L), store.releaseDeferred())
        assertEquals(emptySet<Long>(), store.deferredTaskIds())
        assertEquals(setOf(10L, 12L), store.activeDueIds())
    }

    @Test
    fun startReminderStoreAcknowledgeClearsDeferredAndActiveDue() {
        val store = StartReminderStore(FakeSharedPreferences())

        store.defer(8L)
        store.markActiveDue(9L)
        store.acknowledge(8L)
        store.acknowledge(9L)

        assertTrue(store.isAcknowledged(8L))
        assertTrue(store.isAcknowledged(9L))
        assertEquals(emptySet<Long>(), store.deferredTaskIds())
        assertEquals(emptySet<Long>(), store.activeDueIds())
    }

    @Test
    fun startReminderStoreCanClearAcknowledgementWhenScheduleChanges() {
        val store = StartReminderStore(FakeSharedPreferences())

        store.acknowledge(5L)
        store.clearAcknowledged(5L)

        assertFalse(store.isAcknowledged(5L))
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? {
        return values[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val value = values[key] as? Set<*> ?: return defValues
        return value.filterIsInstance<String>().toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(values)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit
}

private class FakeEditor(
    private val values: MutableMap<String, Any>
) : SharedPreferences.Editor {
    private val updates = mutableMapOf<String, Any?>()
    private var clear = false

    override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putStringSet(key: String?, value: MutableSet<String>?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value?.toSet()
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun remove(key: String?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = null
    }

    override fun clear(): SharedPreferences.Editor = apply {
        clear = true
    }

    override fun commit(): Boolean {
        apply()
        return true
    }

    override fun apply() {
        if (clear) values.clear()
        updates.forEach { (key, value) ->
            if (value == null) {
                values.remove(key)
            } else {
                values[key] = value
            }
        }
    }
}
