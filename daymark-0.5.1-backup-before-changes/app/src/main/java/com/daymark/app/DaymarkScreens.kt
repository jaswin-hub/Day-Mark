package com.daymark.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalLocale
import com.daymark.app.data.*
import com.daymark.app.ui.DaymarkPalette
import com.daymark.app.ui.LocalDaymarkPalette
import com.daymark.app.ui.DaymarkMotion
import com.daymark.app.ui.taskStatusColor
import com.daymark.app.ui.DaymarkMark
import com.daymark.app.ui.LocalDateFormat
import com.daymark.app.ui.daymarkTween
import com.daymark.app.ui.motionDuration
import com.daymark.app.data.formatForDaymark
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun PageHeader(
    route: String,
    month: YearMonth,
    onSettings: () -> Unit
) {
    val palette = LocalDaymarkPalette.current
    var wordmarkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { wordmarkVisible = true }
    val title = when (route) {
        "Calendar" -> month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        "Tasks" -> "My Tasks"
        "Unscheduled" -> "Unscheduled"
        "Settings" -> "Settings"
        else -> "Insights"
    }
    val subtitle = when (route) {
        "Calendar" -> "Plan work dates and deadlines."
        "Tasks" -> "Manage your workload."
        "Unscheduled" -> "Tasks waiting for a date. Capture now, schedule later."
        "Settings" -> "Make Daymark work the way you plan."
        else -> "A clear view of your progress."
    }
    Column(
        Modifier.fillMaxWidth().background(palette.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DaymarkMark(size = 25.dp, description = "Daymark date marker logo")
                AnimatedVisibility(visible = wordmarkVisible, enter = fadeIn(tween(durationMillis = motionDuration(DaymarkMotion.MICRO)))) {
                    Text("DAYMARK", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = palette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    }
}

@Composable
fun CalendarScreen(
    month: YearMonth,
    selected: LocalDate,
    tasks: List<TaskEntity>,
    viewMode: CalendarViewMode,
    weekStart: WeekStart,
    showWeekends: Boolean,
    onViewMode: (CalendarViewMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onComplete: (TaskEntity, Boolean) -> Unit,
    onAdd: (LocalDate) -> Unit
) {
    val palette = LocalDaymarkPalette.current
    val dateFormat = LocalDateFormat.current
    val locale = LocalLocale.current.platformLocale
    val today = LocalDate.now()
    val startDay = if (weekStart == WeekStart.MONDAY) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val occurrences = remember(tasks) { CalendarLogic.occurrences(tasks) }
    val monthDates = remember(month, weekStart, showWeekends) {
        CalendarLogic.monthDates(month, startDay).filter { showWeekends || it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
    }
    val weekDates = remember(selected, weekStart, showWeekends) {
        val start = selected.with(TemporalAdjusters.previousOrSame(startDay))
        (0L..6L).map { start.plusDays(it) }.filter { showWeekends || it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
    }
    val dateCells = when (viewMode) { CalendarViewMode.MONTH -> monthDates; CalendarViewMode.WEEK -> weekDates; CalendarViewMode.AGENDA -> emptyList() }
    val occurrencesByDate = remember(occurrences) { occurrences.groupBy { it.date } }
    val columnCount = if (showWeekends) 7 else 5
    val monthTasks = remember(tasks, month) { tasks.filter { it.plannedDate?.let { date -> YearMonth.from(date) == month } == true || it.deadlineDate?.let { date -> YearMonth.from(date) == month } == true }.distinctBy { it.id } }
    val activeMonthTasks = monthTasks.filterNot { it.isCompleted }
    val completeCount = monthTasks.count { it.isCompleted }
    val overdueCount = activeMonthTasks.count { TaskStatusCalculator.calculate(it) == TaskStatus.OVERDUE }
    val selectedTasks = tasks.filter { it.plannedDate == selected || it.deadlineDate == selected }.distinctBy { it.id }
    val planned = selectedTasks.filter { it.plannedDate == selected }
    val deadlines = selectedTasks.filter { it.deadlineDate == selected && it.plannedDate != selected }
    val done = selectedTasks.count { it.isCompleted }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(CalendarViewMode.MONTH, CalendarViewMode.WEEK, CalendarViewMode.AGENDA).forEach { mode ->
                FilterChip(selected = viewMode == mode, onClick = { onViewMode(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase), fontSize = 11.sp) }, modifier = Modifier.padding(end = 5.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.ChevronLeft, "Previous period") }
            TextButton(onClick = onToday) { Text("Today") }
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.ChevronRight, "Next period") }
        }
        if (viewMode != CalendarViewMode.AGENDA) {
            val labels = remember(startDay, showWeekends, locale) {
                (0..6).map { startDay.plus(it.toLong()).getDisplayName(java.time.format.TextStyle.SHORT, locale).take(1) }.let { all ->
                    if (showWeekends) all else all.filterIndexed { index, _ ->
                        val day = startDay.plus(index.toLong()); day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                labels.forEach { day -> Text(day, Modifier.weight(1f), color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            }
        }
        AnimatedContent(
            targetState = viewMode to dateCells,
            transitionSpec = { fadeIn(daymarkTween(DaymarkMotion.CONTENT)) togetherWith fadeOut(tween(90)) },
            label = "calendar-view"
        ) { (visibleMode, visibleDates) ->
            Column(Modifier.animateContentSize(daymarkTween(DaymarkMotion.EXPAND))) {
                if (visibleMode != CalendarViewMode.AGENDA) {
                    visibleDates.chunked(columnCount).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                CalendarDayCell(
                                    date = date, inMonth = YearMonth.from(date) == month,
                                    selected = date == selected, isToday = date == today,
                                    statuses = occurrencesByDate[date].orEmpty().map { it.calculatedStatus }.distinct(),
                                    onClick = { onSelectDate(date) }, modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (viewMode == CalendarViewMode.MONTH) MonthlyOverview(monthTasks.size, completeCount, overdueCount, activeMonthTasks.size)
        if (selected == today && viewMode != CalendarViewMode.AGENDA) TodayFocus(tasks, today)
        if (viewMode == CalendarViewMode.AGENDA) {
            AgendaView(tasks, selected, onSelectDate, onEdit, onComplete, Modifier.weight(1f))
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected.formatForDaymark(dateFormat), color = palette.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("$done of ${selectedTasks.size} complete", color = palette.textSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = { onAdd(selected) }) { Text("+ Add task", color = palette.accent) }
            }
            AnimatedContent(
                targetState = selected, modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn(daymarkTween(DaymarkMotion.CONTENT)) togetherWith fadeOut(tween(100)) },
                label = "selected-day-agenda"
            ) { agendaDate ->
            val agendaTasks = tasks.filter { it.plannedDate == agendaDate || it.deadlineDate == agendaDate }.distinctBy { it.id }
            if (agendaTasks.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { agendaTasks.count { it.isCompleted }.toFloat() / agendaTasks.size },
                    modifier = Modifier.fillMaxWidth().height(3.dp), color = palette.completed, trackColor = palette.border
                )
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val agendaPlanned = agendaTasks.filter { it.plannedDate == agendaDate }
                    val agendaDeadlines = agendaTasks.filter { it.deadlineDate == agendaDate && it.plannedDate != agendaDate }
                    if (agendaPlanned.isNotEmpty()) {
                        item { TaskSectionHeader("PLANNED FOR THIS DAY", agendaPlanned.size) }
                        items(agendaPlanned, key = { "planned-${it.id}" }) { task -> TaskCard(task, agendaDate, { onEdit(task) }, { onComplete(task, !task.isCompleted) }, modifier = Modifier.animateItem()) }
                    }
                    if (agendaDeadlines.isNotEmpty()) {
                        item { TaskSectionHeader("DEADLINES ON THIS DAY", agendaDeadlines.size, Modifier.padding(top = 6.dp)) }
                        items(agendaDeadlines, key = { "deadline-${it.id}" }) { task -> TaskCard(task, agendaDate, { onEdit(task) }, { onComplete(task, !task.isCompleted) }, modifier = Modifier.animateItem()) }
                    }
                }
            } else {
                EmptyState(
                    title = "Your schedule is clear.",
                    message = "Add a task to start organising your schedule.",
                    action = "Add task", onAction = { onAdd(agendaDate) }, modifier = Modifier.fillMaxSize()
                )
            }
            }
        }
    }
}

@Composable
private fun TodayFocus(tasks: List<TaskEntity>, today: LocalDate) {
    val palette = LocalDaymarkPalette.current
    val todayTasks = remember(tasks, today) { tasks.filter { it.plannedDate == today || it.deadlineDate == today }.distinctBy { it.id } }
    val overdue = remember(tasks, today) { tasks.count { TaskStatusCalculator.calculate(it, today) == TaskStatus.OVERDUE } }
    val nextDeadline = remember(tasks, today) { tasks.filter { !it.isCompleted && it.deadlineDate != null && it.deadlineDate >= today }.minByOrNull { it.deadlineDate!! } }
    val minutes = todayTasks.mapNotNull { it.estimatedDurationMinutes }.sum()
    val done = todayTasks.count { it.isCompleted }
    Surface(Modifier.fillMaxWidth().padding(top = 6.dp), color = palette.surface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, palette.border)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TODAY FOCUS", color = palette.textMuted, fontSize = 9.sp, letterSpacing = .8.sp, fontWeight = FontWeight.SemiBold)
                Text("$done/${todayTasks.size} done", color = palette.textSecondary, fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${if (minutes > 0) "${minutes / 60}h ${minutes % 60}m planned" else "No estimates"}", color = palette.textPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$overdue overdue", color = if (overdue > 0) palette.missed else palette.textSecondary, fontSize = 11.sp, maxLines = 1)
            }
            LinearProgressIndicator(
                progress = { if (todayTasks.isEmpty()) 0f else done.toFloat() / todayTasks.size },
                modifier = Modifier.fillMaxWidth().height(3.dp), color = palette.completed, trackColor = palette.border
            )
            Text(nextDeadline?.let { "Next deadline · ${it.title}" } ?: "No upcoming deadline", color = palette.textSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AgendaView(tasks: List<TaskEntity>, start: LocalDate, onSelectDate: (LocalDate) -> Unit, onEdit: (TaskEntity) -> Unit, onComplete: (TaskEntity, Boolean) -> Unit, modifier: Modifier = Modifier) {
    val end = remember(start) { start.plusDays(90) }
    val entries = remember(tasks, start, end) { CalendarLogic.occurrences(tasks).filter { it.date in start..end }.groupBy { it.date }.toSortedMap() }
    if (entries.isEmpty()) {
        EmptyState("Nothing ahead just yet.", "Scheduled tasks and deadlines will appear here.", modifier = modifier.fillMaxWidth())
    } else {
        LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { (date, occurrences) ->
                item(key = "agenda-date-$date") {
                    TextButton(onClick = { onSelectDate(date) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(date.formatForDaymark(LocalDateFormat.current), modifier = Modifier.weight(1f), color = LocalDaymarkPalette.current.textPrimary)
                        Text("${occurrences.distinctBy { it.taskId }.size} tasks", color = LocalDaymarkPalette.current.textSecondary, fontSize = 11.sp)
                    }
                }
                items(occurrences.distinctBy { it.taskId }, key = { "${date}-${it.taskId}" }) { occurrence ->
                    val task = tasks.firstOrNull { it.id == occurrence.taskId }
                    if (task != null) TaskCard(task, date, { onEdit(task) }, { onComplete(task, !task.isCompleted) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(date: LocalDate, inMonth: Boolean, selected: Boolean, isToday: Boolean, statuses: List<TaskStatus>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalDaymarkPalette.current
    val selectedFill by animateColorAsState(if (selected) palette.accent else androidx.compose.ui.graphics.Color.Transparent, daymarkTween(DaymarkMotion.MICRO), label = "calendar-day-selected")
    val statusSummary = statuses.joinToString { it.name.lowercase().replace('_', ' ') }
    Column(
        modifier.heightIn(min = 48.dp).clickable(onClick = onClick).semantics {
            role = Role.Button
            contentDescription = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) + if (statusSummary.isBlank()) "" else ", $statusSummary"
            stateDescription = if (selected) "Selected" else if (isToday) "Today" else ""
        }.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(29.dp)
                .background(selectedFill, CircleShape)
                .then(if (isToday) Modifier.border(1.dp, palette.accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(date.dayOfMonth.toString(), color = when { selected -> palette.onAccent; inMonth -> palette.textPrimary; else -> palette.textMuted }, fontSize = 12.sp, fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
            statuses.take(4).forEach { status -> Box(Modifier.size(4.dp).background(taskStatusColor(palette, status), CircleShape)) }
        }
    }
}

@Composable
private fun MonthlyOverview(total: Int, completed: Int, overdue: Int, open: Int) {
    val palette = LocalDaymarkPalette.current
    Surface(Modifier.fillMaxWidth().padding(top = 7.dp), color = palette.surfaceSecondary, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, palette.border)) {
        Row(Modifier.padding(vertical = 10.dp, horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniStat("SCHEDULED", total.toString(), Modifier.weight(1f))
            MiniStat("DONE", completed.toString(), Modifier.weight(1f), palette.completed)
            MiniStat("OVERDUE", overdue.toString(), Modifier.weight(1f), palette.missed)
            MiniStat("OPEN", open.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, color: Color = LocalDaymarkPalette.current.textPrimary) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(label, color = LocalDaymarkPalette.current.textMuted, fontSize = 8.sp, letterSpacing = .4.sp)
    }
}

@Composable
fun TaskWorkspace(
    tasks: List<TaskEntity>, filter: String, search: String,
    onSearch: (String) -> Unit, onFilter: (String) -> Unit,
    onEdit: (TaskEntity) -> Unit, onComplete: (TaskEntity, Boolean) -> Unit, onDelete: (TaskEntity) -> Unit,
    onRescheduleTomorrow: (TaskEntity) -> Unit, onArchive: (TaskEntity) -> Unit
) {
    val palette = LocalDaymarkPalette.current
    var categoryFilter by remember { mutableStateOf("All categories") }
    var sortOrder by remember { mutableStateOf("Priority") }
    var showSortMenu by remember { mutableStateOf(false) }
    val filters = listOf("All", "Today", "Upcoming", "Overdue", "Completed")
    val categories = remember(tasks) { tasks.mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted() }
    val matched = tasks.filter { task ->
        val status = TaskStatusCalculator.calculate(task)
        (search.isBlank() || task.title.contains(search, true) || task.description.orEmpty().contains(search, true) || task.category.orEmpty().contains(search, true)) && when (filter) {
            "Today" -> status == TaskStatus.PLANNED_TODAY || status == TaskStatus.DUE_TODAY
            "Upcoming" -> status == TaskStatus.UPCOMING
            "Overdue" -> status == TaskStatus.OVERDUE
            "Completed" -> task.isCompleted
            else -> true
        }
    }.filter { categoryFilter == "All categories" || it.category == categoryFilter }
    val matches = when (sortOrder) {
        "Due date" -> matched.sortedWith(compareBy<TaskEntity> { it.deadlineDate ?: it.plannedDate ?: LocalDate.MAX }.thenBy { it.isCompleted })
        "Recently updated" -> matched.sortedByDescending { it.updatedAt }
        else -> matched.sortedWith(compareByDescending<TaskEntity> { it.priority.ordinal }.thenBy { it.deadlineDate ?: it.plannedDate ?: LocalDate.MAX })
    }
    val sections = listOf(
        "OVERDUE" to matches.filter { TaskStatusCalculator.calculate(it) == TaskStatus.OVERDUE },
        "PLAN MISSED" to matches.filter { TaskStatusCalculator.calculate(it) == TaskStatus.MISSED_PLAN },
        "TODAY" to matches.filter { TaskStatusCalculator.calculate(it) == TaskStatus.DUE_TODAY || TaskStatusCalculator.calculate(it) == TaskStatus.PLANNED_TODAY },
        "UPCOMING" to matches.filter { TaskStatusCalculator.calculate(it) == TaskStatus.UPCOMING },
        "UNSCHEDULED" to matches.filter { TaskStatusCalculator.calculate(it) == TaskStatus.UNSCHEDULED },
        "COMPLETED" to matches.filter { it.isCompleted }
    ).filter { it.second.isNotEmpty() }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        OutlinedTextField(
            value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            placeholder = { Text("Search tasks") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true, shape = RoundedCornerShape(14.dp)
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            filters.forEach { item -> FilterChip(selected = filter == item, onClick = { onFilter(item) }, label = { Text(item) }) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (categories.isNotEmpty()) (listOf("All categories") + categories).forEach { item ->
                FilterChip(selected = categoryFilter == item, onClick = { categoryFilter = item }, label = { Text(item) })
            }
            Box {
                TextButton(onClick = { showSortMenu = true }) { Text("Sort: $sortOrder", color = palette.accent) }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, containerColor = palette.surfaceRaised) {
                    listOf("Priority", "Due date", "Recently updated").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { sortOrder = option; showSortMenu = false }) }
                }
            }
        }
        if (matches.isEmpty()) {
            val emptyTitle = when { search.isNotBlank() -> "No matching tasks"; filter == "Completed" -> "All caught up."; else -> "No tasks in this view" }
            EmptyState(emptyTitle, "Try another filter or add a task.", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEach { (heading, sectionTasks) ->
                    item(key = "section-$heading") { TaskSectionHeader(heading, sectionTasks.size, Modifier.padding(top = 12.dp, bottom = 2.dp)) }
                    items(sectionTasks, key = { it.id }) { task ->
                        Column(Modifier.animateItem()) {
                            TaskCard(task, null, { onEdit(task) }, { onComplete(task, !task.isCompleted) }, { onDelete(task) })
                            if (heading == "OVERDUE" || heading == "PLAN MISSED") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onRescheduleTomorrow(task) }) { Text("Tomorrow") }
                                TextButton(onClick = { onEdit(task) }) { Text("Choose date") }
                                TextButton(onClick = { onArchive(task) }) { Text("Archive", color = palette.textSecondary) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnscheduledScreen(tasks: List<TaskEntity>, search: String, onSearch: (String) -> Unit, onEdit: (TaskEntity) -> Unit, onComplete: (TaskEntity, Boolean) -> Unit, onDelete: (TaskEntity) -> Unit) {
    val palette = LocalDaymarkPalette.current
    val visible = remember(tasks, search) { tasks.filter { search.isBlank() || it.title.contains(search, true) || it.description.orEmpty().contains(search, true) || it.category.orEmpty().contains(search, true) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Tasks waiting for a date. Capture now, schedule later.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Search unscheduled tasks") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
        if (visible.isEmpty()) {
            EmptyState(if (search.isBlank()) "Nothing captured yet." else "No matching tasks", "Capture a task here, then assign a planned date or actual deadline.", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { task -> TaskCard(task, null, { onEdit(task) }, { onComplete(task, !task.isCompleted) }, { onDelete(task) }, actionLabel = "Schedule", modifier = Modifier.animateItem()) }
            }
        }
    }
}

@Composable
fun InsightsScreen(tasks: List<TaskEntity>, completed: List<TaskEntity>, weekStart: WeekStart, onOpenDate: (LocalDate) -> Unit) {
    val palette = LocalDaymarkPalette.current
    val total = tasks.size
    val doneCount = tasks.count { it.isCompleted }
    val open = tasks.count { !it.isCompleted }
    val overdue = tasks.count { TaskStatusCalculator.calculate(it) == TaskStatus.OVERDUE }
    val missed = tasks.count { TaskStatusCalculator.calculate(it) == TaskStatus.MISSED_PLAN }
    val rate = if (total == 0) 0 else doneCount * 100 / total
    val today = LocalDate.now()
    val weekStartDay = if (weekStart == WeekStart.MONDAY) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val firstDay = today.with(TemporalAdjusters.previousOrSame(weekStartDay))
    val week = (0L..6L).map { firstDay.plusDays(it) }
    val counts = week.map { date -> completed.count { item -> item.completedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == date } == true } }
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val occurrences = CalendarLogic.occurrences(tasks.filterNot { it.isCompleted }, today).groupBy { it.date }
    val busiest = occurrences.entries.filter { !it.key.isBefore(today) }.maxByOrNull { it.value.size }
    val weekEnd = week.last()
    val plannedEffort = tasks.filter { it.plannedDate?.let { d -> d in firstDay..weekEnd } == true }
    val completedEffort = completed.filter { task -> task.completedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() in firstDay..weekEnd } == true }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Total tasks", total.toString(), palette.textPrimary, Modifier.weight(1f))
                StatCard("Completed", doneCount.toString(), palette.completed, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Still open", open.toString(), palette.accent, Modifier.weight(1f))
                StatCard("Completion", "$rate%", palette.accent, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Overdue", overdue.toString(), palette.missed, Modifier.weight(1f))
                StatCard("Plan missed", missed.toString(), palette.dueToday, Modifier.weight(1f))
            }
        }
        item {
            SectionPanel(title = "COMPLETED THIS WEEK", subtitle = "Daily completions from your task history.") {
                Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    week.forEachIndexed { index, date ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(counts[index].toString(), color = palette.textMuted, fontSize = 10.sp)
                            Box(Modifier.fillMaxWidth(.62f).height((12 + 68f * counts[index] / maxCount).dp).background(palette.accent, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                            Text(date.dayOfWeek.name.take(1), color = palette.textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        item {
            WorkloadBalance(planned = plannedEffort, completed = completedEffort)
        }
        item {
            SectionPanel(
                title = "WEEKLY WORKLOAD",
                subtitle = busiest?.let { "Busiest upcoming day: ${it.key.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} · ${it.value.distinctBy { task -> task.taskId }.size} tasks" } ?: "Choose a day to see its calendar agenda."
            ) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    week.forEach { date ->
                        val dayTasks = tasks.filter { it.plannedDate == date || it.deadlineDate == date }.distinctBy { it.id }
                        val estimated = dayTasks.mapNotNull { it.estimatedDurationMinutes }.sum()
                        val finished = dayTasks.count { it.isCompleted }
                        val overdueDay = dayTasks.any { task ->
                            !task.isCompleted && task.deadlineDate != null &&
                                (task.deadlineDate < today || (task.deadlineDate == today && TaskStatusCalculator.calculate(task) == TaskStatus.OVERDUE))
                        }
                        WorkloadDay(
                            date = date, taskCount = dayTasks.size, estimatedMinutes = estimated,
                            unestimatedCount = dayTasks.count { it.estimatedDurationMinutes == null },
                            completed = finished, overdue = overdueDay, today = today,
                            onClick = { onOpenDate(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadBalance(planned: List<TaskEntity>, completed: List<TaskEntity>) {
    val palette = LocalDaymarkPalette.current
    val plannedMinutes = planned.mapNotNull { it.estimatedDurationMinutes }.sum()
    val completedMinutes = completed.mapNotNull { it.estimatedDurationMinutes }.sum()
    val plannedMissing = planned.count { it.estimatedDurationMinutes == null }
    val completedMissing = completed.count { it.estimatedDurationMinutes == null }
    val scale = maxOf(plannedMinutes, completedMinutes, 1)
    SectionPanel("EFFORT BALANCE", "Estimated planned effort compared with completed effort this week.") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
            EffortPan("PLANNED", plannedMinutes, plannedMissing, Modifier.weight(1f))
            EffortPan("COMPLETED", completedMinutes, completedMissing, Modifier.weight(1f))
        }
        Text("Planned effort", color = palette.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
        LinearProgressIndicator(progress = { plannedMinutes.toFloat() / scale }, modifier = Modifier.fillMaxWidth().height(5.dp), color = palette.accent, trackColor = palette.border)
        Text("Completed effort", color = palette.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp, bottom = 3.dp))
        LinearProgressIndicator(progress = { completedMinutes.toFloat() / scale }, modifier = Modifier.fillMaxWidth().height(5.dp), color = palette.completed, trackColor = palette.border)
        if (planned.isEmpty() && completed.isEmpty()) {
            Text("No estimated effort recorded for this week yet.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text("Unestimated tasks are counted separately and excluded from the effort totals.", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun EffortPan(label: String, minutes: Int, missing: Int, modifier: Modifier) {
    val palette = LocalDaymarkPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = palette.textMuted, fontSize = 9.sp, letterSpacing = .8.sp)
        Text(if (minutes == 0) "—" else formatMinutes(minutes), color = palette.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1)
        Text("$missing without estimate", color = palette.textSecondary, fontSize = 9.sp, maxLines = 2)
    }
}

@Composable
private fun WorkloadDay(
    date: LocalDate, taskCount: Int, estimatedMinutes: Int, unestimatedCount: Int,
    completed: Int, overdue: Boolean, today: LocalDate, onClick: () -> Unit
) {
    val palette = LocalDaymarkPalette.current
    val progress = if (taskCount == 0) 0f else completed.toFloat() / taskCount
    val dayDescription = buildString {
        append(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
        append(": $taskCount scheduled ${if (taskCount == 1) "task" else "tasks"}")
        if (estimatedMinutes > 0) append(", ${formatMinutes(estimatedMinutes)} estimated")
        append(", $completed completed")
        if (overdue) append(", overdue task")
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.width(78.dp).heightIn(min = 124.dp).semantics { contentDescription = dayDescription; role = Role.Button },
        color = if (date == today) palette.selected else palette.surfaceSecondary,
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (overdue) palette.missed else palette.border)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar(Char::uppercase), color = palette.textSecondary, fontSize = 10.sp, maxLines = 1)
            Text(date.dayOfMonth.toString(), color = palette.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text("$taskCount ${if (taskCount == 1) "task" else "tasks"}", color = palette.textSecondary, fontSize = 9.sp, maxLines = 1)
            if (estimatedMinutes > 0) Text(formatMinutes(estimatedMinutes), color = palette.accent, fontSize = 9.sp, maxLines = 1)
            else Text(if (unestimatedCount > 0) "$unestimatedCount no estimate" else "No estimate", color = palette.textMuted, fontSize = 8.sp, maxLines = 1)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = palette.completed, trackColor = palette.border)
            Text("$completed/$taskCount done", color = palette.textSecondary, fontSize = 8.sp, maxLines = 1)
            if (overdue) Text("Overdue", color = palette.missed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatMinutes(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"

@Composable
fun TaskCard(
    task: TaskEntity, contextDate: LocalDate?, onClick: () -> Unit, onComplete: () -> Unit,
    onDelete: (() -> Unit)? = null, actionLabel: String? = null, modifier: Modifier = Modifier
) {
    val palette = LocalDaymarkPalette.current
    val dateFormat = LocalDateFormat.current
    val status = TaskStatusCalculator.calculate(task)
    val cardColor by animateColorAsState(if (task.isCompleted) palette.surfaceSecondary else palette.surface, daymarkTween(DaymarkMotion.MICRO), label = "task-card-state")
    val whenText = when {
        contextDate != null && task.plannedDate == contextDate && task.deadlineDate == contextDate -> "Planned work · actual deadline"
        contextDate != null && task.plannedDate == contextDate -> "Planned work date"
        contextDate != null && task.deadlineDate == contextDate -> "Actual deadline"
        task.plannedDate != null && task.deadlineDate != null -> "Plan ${task.plannedDate.formatForDaymark(dateFormat)}  ·  Due ${task.deadlineDate.formatForDaymark(dateFormat)}"
        task.deadlineDate != null -> "Deadline ${task.deadlineDate.formatForDaymark(dateFormat)}"
        task.plannedDate != null -> "Planned ${task.plannedDate.formatForDaymark(dateFormat)}"
        else -> "Unscheduled"
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().animateContentSize(daymarkTween(DaymarkMotion.EXPAND)),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(start = 7.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onComplete() },
                modifier = Modifier.semantics { contentDescription = "Mark ${task.title} ${if (task.isCompleted) "not complete" else "complete"}" },
                colors = CheckboxDefaults.colors(checkedColor = palette.completed)
            )
            Column(Modifier.weight(1f).clickable(onClick = onClick).padding(start = 3.dp, end = 6.dp)) {
                Text(task.title, color = palette.textPrimary, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(task.priority)
                    task.category?.takeIf { it.isNotBlank() }?.let { MetadataPill(it) }
                    if (task.estimatedDurationMinutes != null) MetadataPill("${task.estimatedDurationMinutes} min")
                }
                Text(whenText, color = palette.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (status == TaskStatus.OVERDUE || status == TaskStatus.MISSED_PLAN || status == TaskStatus.DUE_TODAY) {
                    val label = when (status) { TaskStatus.OVERDUE -> "Overdue"; TaskStatus.MISSED_PLAN -> "Plan missed · deadline still ahead"; else -> "Due today" }
                    Text(label, color = taskStatusColor(palette, status), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (actionLabel != null) TextButton(onClick = onClick) { Text(actionLabel, color = palette.accent, fontSize = 12.sp) }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Default.MoreVert, contentDescription = "Task options", tint = palette.textMuted) }
            Box(Modifier.size(7.dp).background(taskStatusColor(palette, status), CircleShape))
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val palette = LocalDaymarkPalette.current
    val foreground = when (priority) { TaskPriority.URGENT, TaskPriority.HIGH -> palette.missed; TaskPriority.MEDIUM -> palette.accent; TaskPriority.LOW -> palette.textMuted }
    MetadataPill(priority.name.lowercase().replaceFirstChar(Char::uppercase), foreground)
}

@Composable
private fun MetadataPill(label: String, color: Color = LocalDaymarkPalette.current.textSecondary) {
    val palette = LocalDaymarkPalette.current
    Text(label, color = color, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.background(palette.surfaceSecondary, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
}

@Composable
fun TaskSectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    val palette = LocalDaymarkPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = palette.textMuted, fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(7.dp))
        Text(count.toString(), color = palette.textMuted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        HorizontalDivider(Modifier.width(36.dp), color = palette.border)
    }
}

@Composable
private fun SectionPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalDaymarkPalette.current
    Surface(color = palette.surface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, palette.border)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(title, color = palette.textMuted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = palette.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 9.dp))
            content()
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val palette = LocalDaymarkPalette.current
    Surface(modifier, color = palette.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, palette.border)) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
            Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(label, color = palette.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val palette = LocalDaymarkPalette.current
    Column(modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        DaymarkMark(size = 44.dp, description = "Daymark date marker")
        Spacer(Modifier.height(14.dp))
        Text(title, color = palette.textPrimary, fontWeight = FontWeight.Medium)
        Text(message, color = palette.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        if (action != null && onAction != null) TextButton(onClick = onAction, modifier = Modifier.padding(top = 4.dp)) { Text(action, color = palette.accent) }
    }
}

@Composable
fun DaymarkLoadingState(modifier: Modifier = Modifier) {
    val palette = LocalDaymarkPalette.current
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(color = palette.surfaceSecondary, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(.55f).height(18.dp)) { }
        Surface(color = palette.surfaceSecondary, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(.85f).height(12.dp)) { }
        Spacer(Modifier.height(5.dp))
        repeat(4) {
            Surface(color = palette.surface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, palette.border), modifier = Modifier.fillMaxWidth().height(68.dp)) { }
        }
    }
}
