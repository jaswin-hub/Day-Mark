package com.daymark.app

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.glance.appwidget.updateAll
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.TaskRepository
import com.daymark.app.data.TaskStatusCalculator
import com.daymark.app.reminders.ReminderScheduler
import com.daymark.app.ui.DaymarkTheme
import com.daymark.app.ui.DaymarkMotion
import com.daymark.app.ui.ThemeMode
import com.daymark.app.ui.daymarkTween
import com.daymark.app.widget.MonthWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
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
        if (Build.VERSION.SDK_INT >= 33) askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        val app = application as DaymarkApp
        app.reminderScheduler.createChannel()
        setContent {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app, app.repository))
            val theme by vm.theme.collectAsStateWithLifecycle()
            DaymarkTheme(theme) { DaymarkHome(vm) }
        }
    }
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val app: DaymarkApp, private val repo: TaskRepository) : ViewModel() {
    private val loadingState = MutableStateFlow(true)
    val loading = loadingState
    val tasks = repo.observeAll().onEach { loadingState.value = false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val inbox = repo.observeInbox().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completed = repo.observeCompleted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val theme = app.settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val query = MutableStateFlow("")
    val results = query.flatMapLatest(repo::search).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) { query.value = value }
    fun setTheme(value: ThemeMode) = viewModelScope.launch { app.settings.setTheme(value); MonthWidget().updateAll(app) }
    fun save(task: TaskEntity) = viewModelScope.launch {
        repo.save(task)
        app.reminderScheduler.schedule(task.id, task.title, task.reminderAt.takeIf { task.reminderEnabled })
        MonthWidget().updateAll(app)
    }
    fun delete(task: TaskEntity) = viewModelScope.launch {
        repo.delete(task.id); app.reminderScheduler.cancel(task.id); MonthWidget().updateAll(app)
    }
    fun complete(task: TaskEntity, done: Boolean) = viewModelScope.launch {
        repo.complete(task.id, done)
        if (done) app.reminderScheduler.cancel(task.id)
        else repo.get(task.id)?.let { app.reminderScheduler.schedule(it.id, it.title, it.reminderAt) }
        MonthWidget().updateAll(app)
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
    val searchResults by vm.results.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "Calendar"
    var month by remember { mutableStateOf(YearMonth.from(LaunchState.date ?: LocalDate.now())) }
    var selectedDate by remember { mutableStateOf(LaunchState.date ?: LocalDate.now()) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(LaunchState.openQuickAdd) }
    var deletePending by remember { mutableStateOf<TaskEntity?>(null) }
    var themeMenu by remember { mutableStateOf(false) }
    var taskFilter by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }

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
                onTheme = { themeMenu = true },
                menuExpanded = themeMenu,
                onMenuDismiss = { themeMenu = false },
                onThemeSelected = { vm.setTheme(it); themeMenu = false }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = palette.surface, windowInsets = WindowInsets.navigationBars) {
                listOf(
                    Triple("Calendar", Icons.Default.CalendarMonth, "Calendar"),
                    Triple("Tasks", Icons.Default.Checklist, "My Tasks"),
                    Triple("Inbox", Icons.Default.Inbox, "Inbox"),
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
            if (route != "Calendar") FloatingActionButton(onClick = { editing = null; showEditor = true }) {
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
                    onPrevious = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) },
                    onToday = { month = YearMonth.now(); selectedDate = LocalDate.now() },
                    onSelectDate = { selectedDate = it }, onEdit = { editing = it; showEditor = true },
                    onComplete = { task, done -> vm.complete(task, done) },
                    onAdd = { date -> selectedDate = date; editing = TaskEntity(title = "", plannedDate = date); showEditor = true }
                )
            }
            composable("Tasks") {
                val source = if (search.isBlank()) tasks else searchResults
                TaskWorkspace(
                    tasks = source, filter = taskFilter, search = search,
                    onSearch = { search = it; vm.search(it) }, onFilter = { taskFilter = it },
                    onEdit = { editing = it; showEditor = true }, onComplete = { task, done -> vm.complete(task, done) },
                    onDelete = { deletePending = it }
                )
            }
            composable("Inbox") {
                InboxScreen(inbox, onEdit = { editing = it; showEditor = true }, onComplete = { task, done -> vm.complete(task, done) })
            }
            composable("Insights") {
                InsightsScreen(tasks, completed) { date ->
                    selectedDate = date
                    month = YearMonth.from(date)
                    nav.navigate("Calendar") { launchSingleTop = true }
                }
            }
        }
    }

    if (showEditor) TaskEditorSheet(
        existing = editing, initialDate = selectedDate,
        onDismiss = { showEditor = false; editing = null },
        onSave = { vm.save(it); showEditor = false; editing = null },
        onDelete = { task -> showEditor = false; editing = null; deletePending = task }
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
