package com.daymark.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskRepository
import com.daymark.app.data.DaymarkSettings
import com.daymark.app.data.CalendarViewMode
import com.daymark.app.data.DateFormatMode
import com.daymark.app.reminders.ReminderScheduler
import com.daymark.app.ui.DaymarkTheme
import com.daymark.app.ui.DaymarkMotion
import com.daymark.app.ui.daymarkTween
import com.daymark.app.widget.WidgetRefresh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private object LaunchState {
    var date: LocalDate? = null
    var taskId: String? = null
    var openQuickAdd = false
}

class MainActivity : ComponentActivity() {
    private val askNotification by lazy { registerForActivityResult(ActivityResultContracts.RequestPermission()) { } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LaunchState.date = intent.getStringExtra("quick_add_date")?.let(::parseDate)
            ?: intent.getStringExtra("selected_date")?.let(::parseDate)
        LaunchState.taskId = intent.getStringExtra("task_id")
        LaunchState.openQuickAdd = intent.hasExtra("quick_add_date")
        val app = application as DaymarkApp
        app.reminderScheduler.createChannel()
        if (Build.VERSION.SDK_INT >= 33) lifecycleScope.launch {
            val enabled = app.settings.settings.first().notificationsEnabled
            val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (enabled && !granted) askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app, app.repository))
            val settings by vm.settings.collectAsStateWithLifecycle()
            DaymarkTheme(settings) { DaymarkHome(vm) }
        }
    }
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

class HomeViewModel(private val app: DaymarkApp, private val repo: TaskRepository) : ViewModel() {
    private val loadingState = MutableStateFlow(true)
    val loading = loadingState
    val tasks = repo.observeAll().onEach { loadingState.value = false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val inbox = repo.observeInbox().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completed = repo.observeCompleted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DaymarkSettings())
    fun updateSettings(transform: (DaymarkSettings) -> DaymarkSettings) = viewModelScope.launch {
        val previous = app.settings.settings.first()
        val updated = transform(previous)
        app.settings.update { updated }
        if (previous.notificationsEnabled != updated.notificationsEnabled) {
            if (!updated.notificationsEnabled) repo.reminders().forEach { app.reminderScheduler.cancel(it.id) }
            else app.reminderScheduler.rescheduleAll()
        }
        if (previous.theme != updated.theme || previous.weekStart != updated.weekStart || previous.use24HourTime != updated.use24HourTime || previous.dateFormat != updated.dateFormat) {
            WidgetRefresh.update(app)
        }
    }
    fun save(task: TaskEntity) = viewModelScope.launch {
        repo.save(task)
        val enabled = app.settings.settings.first().notificationsEnabled && task.reminderEnabled
        app.reminderScheduler.schedule(task.id, task.title, task.reminderAt.takeIf { enabled })
        WidgetRefresh.update(app)
    }
    fun delete(task: TaskEntity) = viewModelScope.launch {
        repo.delete(task.id); app.reminderScheduler.cancel(task.id); WidgetRefresh.update(app)
    }
    fun complete(task: TaskEntity, done: Boolean) = viewModelScope.launch {
        repo.complete(task.id, done)
        if (done) app.reminderScheduler.cancel(task.id)
        else if (app.settings.settings.first().notificationsEnabled) repo.get(task.id)?.let { updated ->
            app.reminderScheduler.schedule(updated.id, updated.title, updated.reminderAt.takeIf { updated.reminderEnabled })
        }
        WidgetRefresh.update(app)
    }
    fun archive(task: TaskEntity) = viewModelScope.launch { repo.archive(task.id); app.reminderScheduler.cancel(task.id); WidgetRefresh.update(app) }
    fun reschedule(task: TaskEntity, date: LocalDate) = viewModelScope.launch {
        repo.reschedule(task, date, "User rescheduled from overdue recovery")
        repo.get(task.id)?.let { updated -> app.reminderScheduler.schedule(updated.id, updated.title, updated.reminderAt.takeIf { updated.reminderEnabled && app.settings.settings.first().notificationsEnabled }) }
        WidgetRefresh.update(app)
    }
    fun clearAllData() = viewModelScope.launch {
        repo.reminders().forEach { app.reminderScheduler.cancel(it.id) }
        repo.deleteAll()
        app.settings.reset()
        WidgetRefresh.update(app)
    }

    class Factory(private val app: DaymarkApp, private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app, repository) as T
    }
}

@Composable
private fun DaymarkHome(vm: HomeViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val inbox by vm.inbox.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val preferences by vm.settings.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "Calendar"
    var month by remember { mutableStateOf(YearMonth.from(LaunchState.date ?: LocalDate.now())) }
    var selectedDate by remember { mutableStateOf(LaunchState.date ?: LocalDate.now()) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(LaunchState.openQuickAdd) }
    var deletePending by remember { mutableStateOf<TaskEntity?>(null) }
    var taskFilter by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var unscheduledSearch by remember { mutableStateOf("") }
    var clearDataConfirmation by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        LaunchState.openQuickAdd = false
        LaunchState.date = null
    }
    LaunchedEffect(LaunchState.taskId, tasks) {
        LaunchState.taskId?.let { id -> tasks.firstOrNull { it.id == id }?.let { editing = it; showEditor = true; LaunchState.taskId = null } }
    }

    val palette = com.daymark.app.ui.LocalDaymarkPalette.current
    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageHeader(
                route = route,
                month = month,
                onSettings = { if (route == "Settings") nav.popBackStack() else nav.navigate("Settings") { launchSingleTop = true } }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = palette.surface, windowInsets = WindowInsets.navigationBars) {
                listOf(
                    Triple("Calendar", Icons.Default.CalendarMonth, "Calendar"),
                    Triple("Tasks", Icons.Default.Checklist, "My Tasks"),
                    Triple("Unscheduled", Icons.Default.Inbox, "Unscheduled"),
                    Triple("Insights", Icons.Default.Insights, "Insights")
                ).forEach { (destination, icon, label) ->
                    NavigationBarItem(
                        selected = route == destination,
                        onClick = { nav.navigate(destination) { launchSingleTop = true; restoreState = true; popUpTo(nav.graph.startDestinationId) { saveState = true } } },
                        icon = { Icon(icon, contentDescription = label) }, label = { androidx.compose.material3.Text(label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (route != "Calendar" && route != "Settings") FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Checklist, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        if (loading) {
            DaymarkLoadingState(Modifier.padding(innerPadding))
        } else NavHost(
            navController = nav,
            startDestination = "Calendar",
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            enterTransition = { fadeIn(daymarkTween(DaymarkMotion.SCREEN)) },
            exitTransition = { fadeOut(daymarkTween(DaymarkMotion.CONTENT)) },
            popEnterTransition = { fadeIn(daymarkTween(DaymarkMotion.SCREEN)) },
            popExitTransition = { fadeOut(daymarkTween(DaymarkMotion.CONTENT)) }
        ) {
            composable("Calendar") {
                CalendarScreen(
                    month = month, selected = selectedDate, tasks = tasks,
                    viewMode = preferences.defaultCalendarView, weekStart = preferences.weekStart, showWeekends = preferences.showWeekends,
                    onViewMode = { mode -> vm.updateSettings { it.copy(defaultCalendarView = mode) } },
                    onPrevious = {
                        when (preferences.defaultCalendarView) {
                            CalendarViewMode.MONTH -> {
                                month = month.minusMonths(1)
                                selectedDate = month.atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                            }
                            CalendarViewMode.WEEK -> selectedDate = selectedDate.minusWeeks(1)
                            CalendarViewMode.AGENDA -> selectedDate = selectedDate.minusWeeks(1)
                        }
                        month = YearMonth.from(selectedDate)
                    }, onNext = {
                        when (preferences.defaultCalendarView) {
                            CalendarViewMode.MONTH -> {
                                month = month.plusMonths(1)
                                selectedDate = month.atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                            }
                            CalendarViewMode.WEEK -> selectedDate = selectedDate.plusWeeks(1)
                            CalendarViewMode.AGENDA -> selectedDate = selectedDate.plusWeeks(1)
                        }
                        month = YearMonth.from(selectedDate)
                    },
                    onToday = { month = YearMonth.now(); selectedDate = LocalDate.now() },
                    onSelectDate = { selectedDate = it; month = YearMonth.from(it); if (preferences.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, onEdit = { editing = it; showEditor = true },
                    onComplete = { task, done -> vm.complete(task, done) },
                    onAdd = { date -> selectedDate = date; editing = null; showEditor = true }
                )
            }
            composable("Tasks") {
                TaskWorkspace(
                    tasks = tasks, filter = taskFilter, search = search,
                    onSearch = { search = it }, onFilter = { taskFilter = it },
                    onEdit = { editing = it; showEditor = true }, onComplete = { task, done -> vm.complete(task, done) },
                    onDelete = { deletePending = it },
                    onRescheduleTomorrow = { vm.reschedule(it, LocalDate.now().plusDays(1)) },
                    onArchive = vm::archive
                )
            }
            composable("Unscheduled") {
                UnscheduledScreen(inbox, unscheduledSearch, { unscheduledSearch = it }, onEdit = { editing = it; showEditor = true }, onComplete = { task, done -> vm.complete(task, done) }, onDelete = { deletePending = it })
            }
            composable("Settings") {
                SettingsScreen(
                    settings = preferences,
                    onSettingsChange = { vm.updateSettings(it) },
                    onClearData = { clearDataConfirmation = true }
                )
            }
            composable("Insights") {
                InsightsScreen(tasks, completed, preferences.weekStart) { date ->
                    selectedDate = date
                    month = YearMonth.from(date)
                    nav.navigate("Calendar") { launchSingleTop = true }
                }
            }
        }
    }

    if (showEditor) TaskEditorSheet(
        existing = editing, initialDate = selectedDate.takeIf { route == "Calendar" },
        defaultReminderMinutes = preferences.defaultReminderMinutes,
        use24HourTime = preferences.use24HourTime,
        dateFormat = preferences.dateFormat,
        onDismiss = { showEditor = false; editing = null },
        onSave = { vm.save(it); showEditor = false; editing = null },
        onDelete = { task -> showEditor = false; editing = null; deletePending = task }
    )
    if (clearDataConfirmation) androidx.compose.material3.AlertDialog(
        onDismissRequest = { clearDataConfirmation = false },
        title = { androidx.compose.material3.Text("Clear all Daymark data?") },
        text = { androidx.compose.material3.Text("This permanently deletes your local tasks and date-change history. Reminders will be cancelled.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.clearAllData(); clearDataConfirmation = false }) { androidx.compose.material3.Text("Clear data") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { clearDataConfirmation = false }) { androidx.compose.material3.Text("Cancel") } }
    )
    deletePending?.let { task ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletePending = null },
            title = { androidx.compose.material3.Text("Delete task?") },
            text = { androidx.compose.material3.Text("${task.title} will be permanently deleted.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.delete(task); deletePending = null }) { androidx.compose.material3.Text("Delete") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deletePending = null }) { androidx.compose.material3.Text("Cancel") } }
        )
    }
}
