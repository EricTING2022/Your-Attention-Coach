# Room and DataStore Persistence Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist Attention Coach tasks, reviews, settings, demo seed data, and active focus recovery so user-created data survives app/process restarts.

**Architecture:** Use Room as the local source of truth for structured planning data (`tasks` and one-to-one `task_reviews`). Use DataStore Preferences for user settings and active focus session recovery state. Add a repository layer and a lightweight ViewModel so Compose UI observes persistent data instead of owning task/settings lists in memory.

**Tech Stack:** Kotlin, Jetpack Compose, Room, DataStore Preferences, Kotlin Flow/StateFlow, AndroidX Lifecycle ViewModel, existing AlarmManager/Notification platform classes, JUnit, Android instrumentation tests for Room DAO behavior.

---

## Confirmed Product Decisions

- First launch should be clean. Do not automatically seed demo tasks.
- Settings should include a `Seed demo day` action.
- Tapping `Seed demo day` should insert/refresh the May 5, 2026 demo tasks and immediately switch Tasks to May 5.
- Repeated taps on `Seed demo day` are treated as accidental. Add a small cooldown and make the action idempotent.
- Demo seeding should replace only demo-owned May 5 data, not user-created tasks on May 5.
- Use multiple tables:
  - `tasks`
  - `task_reviews`
- Do not implement review history. A task has at most one review. Saving review overwrites the previous review row.
- Do not implement `focus_sessions` history now. Future analytics such as pause count, overtime frequency, exit frequency, and daily focus block count are out of scope for this persistence task.
- Use Room for structured domain data.
- Use DataStore for preferences/settings.
- Migrate active focus session state from SharedPreferences to DataStore.
- If the app is killed during an active focus session, relaunching the app should restore the focus screen instead of treating it as an implicit exit.
- Deleting a task must cancel its reminder and remove any due/reminder state for that task.
- If a focus session is active, the app should restore focus immediately; the user should not reach the task plan page and delete the active task unless they explicitly Exit first.
- Add lightweight sync-ready metadata now:
  - `createdAtMillis`
  - `updatedAtMillis`
- Record future sync TODOs but do not implement them now:
  - `deletedAtMillis`
  - `syncStatus`
  - `remoteId`

## Scope Boundaries

This plan does not add cloud sync, account login, MCP endpoints, focus-session history analytics, review history, or a full UI redesign. It prepares for those directions by introducing a clean local data boundary.

## File Structure

### Build files

- Modify: `app/build.gradle.kts`
  - Add Room.
  - Add DataStore Preferences.
  - Add Lifecycle ViewModel Compose.
  - Add Room test dependencies.
- Modify: `build.gradle.kts`
  - Add KSP plugin if using KSP for Room.

Preferred Room compiler path:

```kotlin
// root build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "<KSP version matching Kotlin 2.1.0>" apply false
}
```

If KSP version resolution becomes a blocker, use `org.jetbrains.kotlin.kapt` with Kotlin `2.1.0` and Room compiler through `kapt`. The implementation should choose the path that compiles reliably in this project.

### Domain files

- Modify: `app/src/main/java/com/example/attentioncoach/domain/Models.kt`
  - Keep `PlannedTask` as the UI/domain model.
  - Add a `TaskReview` domain model if useful for repository APIs.
  - Add `createdAtMillis` / `updatedAtMillis` to domain only if UI/repository needs them; otherwise keep metadata inside entities.
- Modify: `app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt`
  - Add stable demo ownership markers through repository seed helpers, not necessarily domain model fields.

### Data layer files

- Create: `app/src/main/java/com/example/attentioncoach/data/local/TaskEntity.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/local/TaskReviewEntity.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/local/TaskWithReview.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/local/Converters.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/local/AttentionCoachDao.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/local/AttentionCoachDatabase.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/TaskRepository.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/RoomTaskRepository.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/TaskMappers.kt`

### Settings and runtime state files

- Create: `app/src/main/java/com/example/attentioncoach/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/example/attentioncoach/data/DataStoreSettingsRepository.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt`
  - Replace SharedPreferences implementation with DataStore-backed implementation.
  - Store full `ActiveWork` recovery state, not only task id.
- Optionally create: `app/src/main/java/com/example/attentioncoach/data/DataStoreKeys.kt`
  - Centralize DataStore keys.

### App wiring files

- Create: `app/src/main/java/com/example/attentioncoach/AttentionCoachApplication.kt`
- Create: `app/src/main/java/com/example/attentioncoach/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`
  - Register `AttentionCoachApplication`.
- Create: `app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModel.kt`
- Create: `app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModelFactory.kt`
- Modify: `app/src/main/java/com/example/attentioncoach/ui/AppShell.kt`
  - Replace in-memory task/settings state with ViewModel state.
  - Preserve existing screen composables and UI behavior as much as possible.
- Modify: `app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt`
  - Add `Seed demo day` button in Settings.
  - Wire button to ViewModel event.
- Modify: `app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt`
  - Use DataStore-backed focus active check.
  - Keep receiver work short; use `runBlocking` only where unavoidable for a quick DataStore read.

### Tests

- Create: `app/src/test/java/com/example/attentioncoach/data/TaskMappersTest.kt`
- Create: `app/src/test/java/com/example/attentioncoach/data/SettingsRepositoryRulesTest.kt`
- Create: `app/src/test/java/com/example/attentioncoach/platform/FocusSessionStoreRulesTest.kt`
- Create: `app/src/androidTest/java/com/example/attentioncoach/data/AttentionCoachDaoTest.kt`
- Modify: existing tests that assume in-memory `AppShell` state if needed.

---

## Chunk 1: Add Persistence Dependencies and App Container

### Task 1: Add Room/DataStore/ViewModel Dependencies

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add persistence dependencies**

Add:

```kotlin
implementation("androidx.room:room-runtime:<stable>")
implementation("androidx.room:room-ktx:<stable>")
ksp("androidx.room:room-compiler:<same stable>")
implementation("androidx.datastore:datastore-preferences:<stable>")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
androidTestImplementation("androidx.room:room-testing:<same stable>")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test:runner:1.6.2")
```

Use KSP if version resolution works with Kotlin `2.1.0`. If it does not, switch this task to kapt and document the reason in the commit message.

- [ ] **Step 2: Run dependency/build check**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: build succeeds after dependencies resolve.

- [ ] **Step 3: Commit**

```powershell
git add build.gradle.kts app/build.gradle.kts
git commit -m "build: add room datastore persistence dependencies"
```

### Task 2: Add Application Container

**Files:**
- Create: `app/src/main/java/com/example/attentioncoach/AttentionCoachApplication.kt`
- Create: `app/src/main/java/com/example/attentioncoach/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create app container skeleton**

```kotlin
class AppContainer(applicationContext: Context) {
    // Database, repositories, and platform stores will be wired in later tasks.
}
```

```kotlin
class AttentionCoachApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 2: Register application class**

In `AndroidManifest.xml`:

```xml
<application
    android:name=".AttentionCoachApplication"
    ...>
```

- [ ] **Step 3: Run verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: app still builds.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/AttentionCoachApplication.kt app/src/main/java/com/example/attentioncoach/AppContainer.kt app/src/main/AndroidManifest.xml
git commit -m "chore: add app container for persistence"
```

---

## Chunk 2: Room Schema and Mapping

### Task 3: Add Room Entities and Converters

**Files:**
- Create: `data/local/TaskEntity.kt`
- Create: `data/local/TaskReviewEntity.kt`
- Create: `data/local/TaskWithReview.kt`
- Create: `data/local/Converters.kt`

- [ ] **Step 1: Create `TaskEntity`**

Schema:

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val title: String,
    val target: String,
    val startTimeMinuteOfDay: Int?,
    val durationMinutes: Int,
    val priority: Priority,
    val status: TaskStatus,
    val isDemo: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
```

Use `isDemo` so `Seed demo day` can replace demo tasks only.

- [ ] **Step 2: Create `TaskReviewEntity`**

Schema:

```kotlin
@Entity(
    tableName = "task_reviews",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TaskReviewEntity(
    @PrimaryKey val taskId: Long,
    val actualFocusMinutes: Int,
    val actualCompletion: String,
    val mismatchReason: String,
    val nextAdjustment: String,
    val reviewedAtMillis: Long?,
    val updatedAtMillis: Long
)
```

Saving review should upsert this row. No review history table.

- [ ] **Step 3: Create relation wrapper**

```kotlin
data class TaskWithReview(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val review: TaskReviewEntity?
)
```

- [ ] **Step 4: Create converters if needed**

Room can store enums by name with converters:

```kotlin
class Converters {
    @TypeConverter fun priorityToString(value: Priority): String = value.name
    @TypeConverter fun stringToPriority(value: String): Priority = Priority.valueOf(value)
    @TypeConverter fun statusToString(value: TaskStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
```

- [ ] **Step 5: Add mapper unit tests first**

Create `TaskMappersTest.kt` with tests for:

```kotlin
@Test fun maps_task_with_review_to_planned_task()
@Test fun maps_task_without_review_to_planned_task_defaults()
@Test fun maps_start_time_null_and_non_null()
@Test fun preserves_created_and_updated_metadata_on_entity()
```

- [ ] **Step 6: Run tests and expect failure before mappers exist**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: mapper tests fail because mapper functions are missing.

### Task 4: Add Mappers

**Files:**
- Create: `data/TaskMappers.kt`
- Test: `TaskMappersTest.kt`

- [ ] **Step 1: Implement mapper functions**

Expected helpers:

```kotlin
fun TaskWithReview.toDomain(): PlannedTask
fun PlannedTask.toTaskEntity(nowMillis: Long, isDemo: Boolean = false): TaskEntity
fun reviewEntityFromTask(task: PlannedTask, nowMillis: Long): TaskReviewEntity?
```

Important mapping rules:

- `LocalDate` <-> `dateEpochDay`
- `LocalTime?` <-> `startTimeMinuteOfDay`
- Missing review maps to:
  - `actualFocusMinutes = 0`
  - `actualCompletion = ""`
  - `mismatchReason = ""`
  - `nextAdjustment = ""`
- Saving review overwrites existing review row.

- [ ] **Step 2: Run mapper tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: mapper tests pass.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/data app/src/test/java/com/example/attentioncoach/data/TaskMappersTest.kt
git commit -m "feat: add room task review schema"
```

---

## Chunk 3: DAO, Database, and Repository

### Task 5: Add DAO and Database

**Files:**
- Create: `data/local/AttentionCoachDao.kt`
- Create: `data/local/AttentionCoachDatabase.kt`
- Test: `androidTest/java/com/example/attentioncoach/data/AttentionCoachDaoTest.kt`

- [ ] **Step 1: Write DAO instrumentation tests first**

Test cases:

```kotlin
@Test fun insert_task_then_observe_all_tasks()
@Test fun upsert_review_overwrites_previous_review()
@Test fun delete_task_cascades_review()
@Test fun seed_demo_replaces_only_demo_tasks_for_may_5()
```

- [ ] **Step 2: Implement DAO**

Required DAO methods:

```kotlin
@Transaction
@Query("SELECT * FROM tasks ORDER BY dateEpochDay ASC, createdAtMillis ASC")
fun observeTasksWithReviews(): Flow<List<TaskWithReview>>

@Transaction
@Query("SELECT * FROM tasks WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtMillis ASC")
fun observeTasksWithReviewsForDate(dateEpochDay: Long): Flow<List<TaskWithReview>>

@Transaction
@Query("SELECT * FROM tasks WHERE id = :taskId")
suspend fun taskWithReviewById(taskId: Long): TaskWithReview?

@Insert
suspend fun insertTask(task: TaskEntity): Long

@Update
suspend fun updateTask(task: TaskEntity)

@Upsert
suspend fun upsertReview(review: TaskReviewEntity)

@Query("DELETE FROM task_reviews WHERE taskId = :taskId")
suspend fun deleteReview(taskId: Long)

@Query("DELETE FROM tasks WHERE id = :taskId")
suspend fun deleteTask(taskId: Long)

@Query("DELETE FROM tasks WHERE isDemo = 1 AND dateEpochDay = :dateEpochDay")
suspend fun deleteDemoTasksForDate(dateEpochDay: Long)
```

Add a transaction method for seed demo:

```kotlin
@Transaction
suspend fun replaceDemoTasksForDate(date: LocalDate, demoTasks: List<PlannedTask>, nowMillis: Long)
```

If Room does not support a default method cleanly in the DAO interface, implement this transaction in repository using multiple DAO calls inside `database.withTransaction`.

- [ ] **Step 3: Implement database**

```kotlin
@Database(
    entities = [TaskEntity::class, TaskReviewEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AttentionCoachDatabase : RoomDatabase() {
    abstract fun dao(): AttentionCoachDao
}
```

- [ ] **Step 4: Run tests**

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

If emulator is available, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Expected: DAO instrumentation tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/data/local app/src/androidTest/java/com/example/attentioncoach/data/AttentionCoachDaoTest.kt
git commit -m "feat: add room dao database"
```

### Task 6: Add Task Repository

**Files:**
- Create: `data/TaskRepository.kt`
- Create: `data/RoomTaskRepository.kt`
- Modify: `AppContainer.kt`
- Test: `TaskRepository` tests if feasible with fake DAO; otherwise rely on DAO + mapper tests.

- [ ] **Step 1: Define repository interface**

```kotlin
interface TaskRepository {
    fun observeTasks(): Flow<List<PlannedTask>>
    fun observeTasksForDate(date: LocalDate): Flow<List<PlannedTask>>
    suspend fun taskById(taskId: Long): PlannedTask?
    suspend fun createTask(task: PlannedTask): PlannedTask
    suspend fun updateTask(task: PlannedTask)
    suspend fun deleteTask(taskId: Long)
    suspend fun toggleCompletion(taskId: Long)
    suspend fun saveFocusFinish(taskId: Long, actualFocusMinutes: Int)
    suspend fun saveReview(taskId: Long, completion: String, reason: String, adjustment: String)
    suspend fun replaceDemoDay(date: LocalDate, demoTasks: List<PlannedTask>)
}
```

- [ ] **Step 2: Implement Room repository**

Rules:

- `createTask` inserts `TaskEntity` with `createdAtMillis` and `updatedAtMillis`.
- `updateTask` preserves `createdAtMillis`, updates `updatedAtMillis`.
- `deleteTask` deletes task; Room cascades review deletion.
- `toggleCompletion`:
  - if `FINISHED` or `REVIEWED`, set `PLANNED` and delete review row.
  - otherwise set `FINISHED`, no review row required.
- `saveFocusFinish`:
  - set task status `FINISHED`.
  - upsert review with `actualFocusMinutes`, preserving existing completion/reason/adjustment if present.
- `saveReview`:
  - set task status `REVIEWED`.
  - upsert review row and preserve existing `actualFocusMinutes`.
- `replaceDemoDay`:
  - delete only `isDemo = true` tasks for May 5, 2026.
  - insert demo tasks with `isDemo = true`.
  - do not delete user-created May 5 tasks.

- [ ] **Step 3: Wire repository in app container**

```kotlin
class AppContainer(context: Context) {
    val database = Room.databaseBuilder(...)
        .fallbackToDestructiveMigration(false)
        .build()

    val taskRepository: TaskRepository = RoomTaskRepository(database)
}
```

- [ ] **Step 4: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/data app/src/main/java/com/example/attentioncoach/AppContainer.kt
git commit -m "feat: add task repository"
```

---

## Chunk 4: DataStore Settings and Active Focus Recovery

### Task 7: Add Settings Repository with DataStore

**Files:**
- Create: `data/SettingsRepository.kt`
- Create: `data/DataStoreSettingsRepository.kt`
- Modify: `AppContainer.kt`
- Test: `SettingsRepositoryRulesTest.kt`

- [ ] **Step 1: Define settings repository**

```kotlin
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun addNeededApp(app: NeededApp)
    suspend fun removeNeededApp(packageName: String)
    suspend fun setNotificationInterval(seconds: Int)
}
```

- [ ] **Step 2: Implement DataStore-backed settings**

Use Preferences DataStore keys:

```kotlin
intPreferencesKey("notification_interval_seconds")
stringPreferencesKey("needed_apps_json")
```

Store whitelist as JSON array using `org.json`:

```json
[
  {"packageName":"com.android.chrome","label":"Chrome"},
  {"packageName":"com.google.android.apps.docs","label":"Docs"}
]
```

Rules:

- If no settings are stored, use `AppSettingsDefaults`.
- Adding an existing package is a no-op.
- Removing a missing package is a no-op.
- Invalid interval values are ignored.

- [ ] **Step 3: Test settings encoding rules**

Test:

```kotlin
@Test fun default_settings_are_used_when_datastore_empty()
@Test fun adding_duplicate_whitelist_app_is_idempotent()
@Test fun invalid_interval_is_ignored()
```

- [ ] **Step 4: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/data/SettingsRepository.kt app/src/main/java/com/example/attentioncoach/data/DataStoreSettingsRepository.kt app/src/test/java/com/example/attentioncoach/data/SettingsRepositoryRulesTest.kt app/src/main/java/com/example/attentioncoach/AppContainer.kt
git commit -m "feat: persist settings with datastore"
```

### Task 8: Migrate Active Focus Session Store to DataStore

**Files:**
- Modify: `platform/FocusSessionStore.kt`
- Modify: `platform/TaskReminderReceiver.kt`
- Test: `FocusSessionStoreRulesTest.kt`

- [ ] **Step 1: Expand stored focus state**

Persist full `ActiveWork` recovery fields:

```text
focus_active: Boolean
active_task_id: Long
planned_duration_minutes: Int
started_at_millis: Long
accumulated_active_millis: Long
pause_started_at_millis: Long?
is_paused: Boolean
```

- [ ] **Step 2: Replace SharedPreferences with DataStore**

Expose suspend APIs:

```kotlin
class FocusSessionStore(...) {
    val activeWork: Flow<ActiveWork?>
    suspend fun saveActive(work: ActiveWork)
    suspend fun clearActive()
    suspend fun activeWorkOnce(): ActiveWork?
    suspend fun isActiveOnce(): Boolean
}
```

- [ ] **Step 3: Update receiver usage**

`TaskReminderReceiver` currently needs a quick focus-active check. Use:

```kotlin
val focusActive = runBlocking {
    FocusSessionStore(context).isActiveOnce()
}
```

Keep the receiver code short and avoid long-running work.

- [ ] **Step 4: Test DataStore serialization logic**

Test:

```kotlin
@Test fun saves_and_restores_active_work()
@Test fun clear_active_removes_recovery_state()
@Test fun paused_state_round_trips()
```

- [ ] **Step 5: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt app/src/test/java/com/example/attentioncoach/platform/FocusSessionStoreRulesTest.kt
git commit -m "feat: persist active focus session"
```

---

## Chunk 5: ViewModel and AppShell Integration

### Task 9: Add AttentionCoachViewModel

**Files:**
- Create: `ui/AttentionCoachViewModel.kt`
- Create: `ui/AttentionCoachViewModelFactory.kt`
- Modify: `ui/AppShell.kt`

- [ ] **Step 1: Define UI state**

```kotlin
data class AttentionCoachUiState(
    val tasks: List<PlannedTask> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val activeWork: ActiveWork? = null,
    val selectedDate: LocalDate = CalendarRules.today(),
    val isLoading: Boolean = true
)
```

- [ ] **Step 2: Create ViewModel**

Responsibilities:

- Collect `taskRepository.observeTasks()`.
- Collect `settingsRepository.settings`.
- Collect `focusSessionStore.activeWork`.
- Expose `StateFlow<AttentionCoachUiState>`.
- Provide event methods:
  - `setSelectedDate(date)`
  - `createTask(draft)`
  - `savePlan(task)`
  - `deleteTask(taskId)`
  - `toggleTaskComplete(taskId)`
  - `finishFocus(taskId, actualFocusMinutes)`
  - `exitFocus()`
  - `saveReview(...)`
  - `addNeededApp(app)`
  - `removeNeededApp(packageName)`
  - `setNotificationInterval(seconds)`
  - `seedDemoDay()`

- [ ] **Step 3: Keep transient sheet/dialog state local**

Do not move everything into ViewModel. Keep purely visual/transient state in `AppShell.kt`:

- selected task sheet id
- draft task open/closed state
- re-entry screen open flag
- alarm permission prompt open flag

This keeps the persistence change focused.

- [ ] **Step 4: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModel.kt app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModelFactory.kt app/src/main/java/com/example/attentioncoach/ui/AppShell.kt
git commit -m "feat: add persistent app viewmodel"
```

### Task 10: Replace In-Memory Tasks with Repository State

**Files:**
- Modify: `ui/AppShell.kt`
- Modify: `ui/TaskDetailSheet.kt` only if create mode needs id `0L`.

- [ ] **Step 1: Remove in-memory seed state**

Remove:

```kotlin
val initialTasks = remember { DemoTaskRepository.seed() }
var tasks by remember { mutableStateOf(initialTasks) }
var nextTaskId by remember { mutableStateOf(initialTasks.nextTaskId()) }
```

Use:

```kotlin
val uiState by viewModel.uiState.collectAsState()
val tasks = uiState.tasks
val appSettings = uiState.settings
```

- [ ] **Step 2: Update create mode**

New draft tasks should use a temporary id:

```kotlin
PlannedTask(
    id = 0L,
    date = selectedDate,
    title = "",
    target = "",
    durationMinutes = 30,
    priority = Priority.IMPORTANT,
    status = TaskStatus.PLANNED
)
```

When the user saves, call `viewModel.createTask(created)`. The repository returns a persisted task id internally. Schedule reminders only after the repository returns a task with a real id.

- [ ] **Step 3: Update all task mutations**

Replace direct list mutation with ViewModel events:

- `tasks = tasks + created` -> `viewModel.createTask(created)`
- `tasks = tasks.replaceTask(updated)` -> `viewModel.savePlan(updated)`
- `tasks = tasks.filterNot { it.id == taskId }` -> `viewModel.deleteTask(taskId)`
- `tasks = tasks.map { ... toggled ... }` -> `viewModel.toggleTaskComplete(taskId)`
- `tasks = tasks.map { ... finish ... }` -> `viewModel.finishFocus(taskId, actualFocusMinutes)`
- `tasks = tasks.map { ... review ... }` -> `viewModel.saveReview(...)`

- [ ] **Step 4: Reminder integration**

After `createTask` or `savePlan`, schedule reminder for the persisted task id.

Implementation option:

```kotlin
val saved = viewModel.createTaskAndReturn(created)
scheduleReminderIfNeeded(saved)
```

If keeping ViewModel methods fire-and-forget, schedule inside ViewModel by injecting `TaskReminderScheduler`. Prefer repository-return approach to keep platform scheduling visible in `AppShell`.

- [ ] **Step 5: Delete task cleanup**

On delete:

- Cancel visible notification.
- Acknowledge/cancel start reminder.
- Delete Room task.
- Room cascade deletes review.
- Clear selected task sheet.

- [ ] **Step 6: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/TaskDetailSheet.kt
git commit -m "feat: persist tasks and reviews"
```

---

## Chunk 6: Focus Restore Behavior

### Task 11: Restore Active Focus on App Launch

**Files:**
- Modify: `ui/AppShell.kt`
- Modify: `ui/AttentionCoachViewModel.kt`
- Test: add unit tests for focus restore rules if rules are extracted.

- [ ] **Step 1: Define restore rules**

On app start:

```text
if DataStore activeWork exists:
    find task in Room
    if task exists and status allows focus:
        show WorkScreen or PauseScreen based on activeWork.isPaused
    else:
        clear active focus state
        show Tasks
```

Status allows focus:

- `PLANNED`
- `FINISHED` only if the session started before status changed should be treated carefully; MVP should clear if task is already `FINISHED` or `REVIEWED`.

- [ ] **Step 2: Persist every focus state transition**

When starting work:

```kotlin
focusSessionStore.saveActive(task.toActiveWork())
```

When pausing:

```kotlin
focusSessionStore.saveActive(pausedWork)
```

When resuming:

```kotlin
focusSessionStore.saveActive(resumedWork)
```

When finishing or exiting:

```kotlin
focusSessionStore.clearActive()
```

- [ ] **Step 3: Keep active task deletion unreachable**

Because app launch restores focus first, the plan page for an active task should not be reachable. Still, repository delete should defensively clear focus state if asked to delete active task.

- [ ] **Step 4: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Manual smoke test**

1. Start a focus session.
2. Press home.
3. Kill app from recents.
4. Relaunch app.
5. Expected: app immediately restores focus screen and timer continues based on wall-clock time.
6. Tap Exit.
7. Expected: app returns to Tasks and cleared focus does not restore on next relaunch.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModel.kt app/src/main/java/com/example/attentioncoach/platform/FocusSessionStore.kt
git commit -m "feat: restore active focus session"
```

---

## Chunk 7: Seed Demo Day and Settings Integration

### Task 12: Add Idempotent Seed Demo Day

**Files:**
- Modify: `domain/DemoTaskRepository.kt`
- Modify: `data/RoomTaskRepository.kt`
- Modify: `ui/AttentionCoachViewModel.kt`
- Modify: `ui/TopLevelInfoScreens.kt`
- Test: DAO/repository seed tests.

- [ ] **Step 1: Confirm demo date**

Use:

```kotlin
LocalDate.of(2026, 5, 5)
```

- [ ] **Step 2: Add repository seed method**

```kotlin
suspend fun seedDemoDay(): LocalDate {
    replaceDemoDay(DemoTaskRepository.demoDate, DemoTaskRepository.seed())
    return DemoTaskRepository.demoDate
}
```

The ViewModel should call this, then set `selectedDate` to May 5.

- [ ] **Step 3: Add cooldown**

In ViewModel, store:

```kotlin
private var lastSeedDemoAtMillis = 0L
private val seedDemoCooldownMillis = 1500L
```

Rule:

- If tapped again inside cooldown, ignore.
- If tapped after cooldown, replace demo tasks again.
- Repeated valid taps should still produce one standard demo set, not duplicates.

- [ ] **Step 4: Add Settings button**

In `SettingsScreen`, add a button row:

```text
Seed demo day
```

Design should match existing Google-inspired settings rows. Do not redesign the settings page beyond this row.

- [ ] **Step 5: Switch Tasks to May 5**

After `seedDemoDay()`:

- `selectedDate = 2026-05-05`
- `destination = TASKS`

If destination is controlled locally in `AppShell`, the ViewModel can expose a one-shot event:

```kotlin
sealed interface AttentionCoachEvent {
    data class ShowDate(val date: LocalDate) : AttentionCoachEvent
}
```

Simpler MVP: pass `onSeedDemoDay` from `AppShell`, and after ViewModel call succeeds, set local `destination` and `selectedDate`.

- [ ] **Step 6: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 7: Manual smoke test**

1. Clear app data.
2. Open app.
3. Expected: no automatic demo task list.
4. Open Settings.
5. Tap `Seed demo day`.
6. Return to Tasks.
7. Expected: selected date is May 5, 2026 and demo tasks are visible.
8. Tap `Seed demo day` multiple times.
9. Expected: no duplicate demo tasks.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/domain/DemoTaskRepository.kt app/src/main/java/com/example/attentioncoach/data/RoomTaskRepository.kt app/src/main/java/com/example/attentioncoach/ui/AttentionCoachViewModel.kt app/src/main/java/com/example/attentioncoach/ui/TopLevelInfoScreens.kt
git commit -m "feat: add seed demo day persistence"
```

---

## Chunk 8: Reminder Consistency with Persistent Data

### Task 13: Ensure Reminder Scheduling Uses Persisted Task IDs

**Files:**
- Modify: `ui/AppShell.kt`
- Modify: `platform/TaskReminderScheduler.kt` only if needed.
- Modify: `platform/TaskReminderReceiver.kt` only if needed.

- [ ] **Step 1: Audit create/save plan flow**

Checklist:

- New task gets Room-generated id.
- Reminder schedule uses generated id, not temporary id `0L`.
- Editing start time cancels/replaces previous reminder if needed.
- Disabling start time cancels previous reminder.

- [ ] **Step 2: Add cancel API if missing**

If `TaskReminderScheduler` lacks explicit cancel by task id, add:

```kotlin
fun cancel(taskId: Long)
```

Use same PendingIntent request code strategy as schedule.

- [ ] **Step 3: Delete task cleanup**

On delete:

```kotlin
reminderScheduler.cancel(taskId)
TaskReminderReceiver.acknowledgeReminder(context, taskId)
TaskReminderReceiver.cancelVisibleNotification(context, taskId)
taskRepository.deleteTask(taskId)
```

- [ ] **Step 4: Run verification**

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- [ ] **Step 5: Manual smoke test**

1. Create a task with start time.
2. Restart app.
3. Confirm task remains.
4. Delete task before start time.
5. Confirm no reminder appears for deleted task.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/example/attentioncoach/ui/AppShell.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderScheduler.kt app/src/main/java/com/example/attentioncoach/platform/TaskReminderReceiver.kt
git commit -m "fix: keep reminders consistent with persisted tasks"
```

---

## Chunk 9: Final Verification and Documentation

### Task 14: Persistence Smoke Test Notes

**Files:**
- Create: `docs/persistence_design/room_datastore_persistence_smoke_test.md`
- Modify: `docs/persistence_design/seed_demo_day_requirement.md` if implementation details changed.

- [ ] **Step 1: Run required build verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expected: both pass.

- [ ] **Step 2: Run emulator smoke tests**

Use emulator if available.

Smoke cases:

1. Create task -> force stop/reopen -> task remains.
2. Edit task plan -> reopen -> edits remain.
3. Finish task -> reopen -> actual focus remains.
4. Save review -> reopen -> review remains.
5. Change whitelist -> reopen -> whitelist remains.
6. Change notification interval -> reopen -> interval remains.
7. Start focus -> kill app -> reopen -> focus screen restores.
8. Exit restored focus -> reopen -> focus no longer restores.
9. Seed demo day -> Tasks switches to May 5.
10. Tap Seed demo day repeatedly -> no duplicates.
11. Delete task with reminder -> reminder does not fire.

- [ ] **Step 3: Record known caveats**

Document:

- Room schema version is `1`.
- Future sync metadata TODOs:
  - `deletedAtMillis`
  - `syncStatus`
  - `remoteId`
- `focus_sessions` history is intentionally out of scope.
- Review history is intentionally out of scope; save review overwrites one row.

- [ ] **Step 4: Commit docs**

```powershell
git add docs/persistence_design/room_datastore_persistence_smoke_test.md docs/persistence_design/seed_demo_day_requirement.md
git commit -m "docs: record persistence smoke coverage"
```

---

## Final Acceptance Criteria

- User-created tasks survive app restart.
- Edited task plans survive app restart.
- Saved reviews survive app restart.
- Settings whitelist survives app restart.
- Notification interval survives app restart.
- Active focus session restores after app kill/relaunch.
- User must explicitly use in-app Exit to abandon active focus.
- Deleting task removes its review and cancels its reminders.
- Settings `Seed demo day` inserts/replaces demo tasks idempotently.
- After seeding, Tasks immediately shows May 5, 2026.
- No duplicate demo tasks after repeated seed taps.
- `testDebugUnitTest` passes.
- `assembleDebug` passes.
- DAO instrumentation tests pass when emulator is available.

## Future TODOs Not Implemented In This Plan

- Add `deletedAtMillis` for soft delete and cloud sync tombstones.
- Add `syncStatus` for offline upload/conflict tracking.
- Add `remoteId` for cloud database identity.
- Add `focus_sessions` table for pause/overtime/exit analytics.
- Add cloud account login and backup.
- Add MCP/agent interface over repository-level operations.
