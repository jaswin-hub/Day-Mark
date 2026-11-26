package com.daymark.app.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _taskDao: Lazy<TaskDao> = lazy {
    TaskDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "a11d6a979436161e49cd5d588b4fa17a", "8fa5e402f0f24bd79eaac1ac6a4d4ca5") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `plannedDate` TEXT, `deadlineDate` TEXT, `deadlineTime` TEXT, `priority` TEXT NOT NULL, `estimatedDurationMinutes` INTEGER, `category` TEXT, `isCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `completedAt` INTEGER, `reminderAt` INTEGER, `reminderEnabled` INTEGER NOT NULL, `archivedAt` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_plannedDate` ON `tasks` (`plannedDate`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_deadlineDate` ON `tasks` (`deadlineDate`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_reminderAt` ON `tasks` (`reminderAt`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `task_date_changes` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `priorPlannedDate` TEXT, `priorDeadlineDate` TEXT, `newPlannedDate` TEXT, `newDeadlineDate` TEXT, `changedAt` INTEGER NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_task_date_changes_taskId` ON `task_date_changes` (`taskId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_task_date_changes_changedAt` ON `task_date_changes` (`changedAt`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a11d6a979436161e49cd5d588b4fa17a')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tasks`")
        connection.execSQL("DROP TABLE IF EXISTS `task_date_changes`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTasks.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("plannedDate", TableInfo.Column("plannedDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("deadlineDate", TableInfo.Column("deadlineDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("deadlineTime", TableInfo.Column("deadlineTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("priority", TableInfo.Column("priority", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("estimatedDurationMinutes", TableInfo.Column("estimatedDurationMinutes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("category", TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("isCompleted", TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("completedAt", TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("reminderAt", TableInfo.Column("reminderAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("reminderEnabled", TableInfo.Column("reminderEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("archivedAt", TableInfo.Column("archivedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTasks.add(TableInfo.Index("index_tasks_plannedDate", false, listOf("plannedDate"), listOf("ASC")))
        _indicesTasks.add(TableInfo.Index("index_tasks_deadlineDate", false, listOf("deadlineDate"), listOf("ASC")))
        _indicesTasks.add(TableInfo.Index("index_tasks_isCompleted", false, listOf("isCompleted"), listOf("ASC")))
        _indicesTasks.add(TableInfo.Index("index_tasks_reminderAt", false, listOf("reminderAt"), listOf("ASC")))
        val _infoTasks: TableInfo = TableInfo("tasks", _columnsTasks, _foreignKeysTasks, _indicesTasks)
        val _existingTasks: TableInfo = read(connection, "tasks")
        if (!_infoTasks.equals(_existingTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tasks(com.daymark.app.data.TaskEntity).
              | Expected:
              |""".trimMargin() + _infoTasks + """
              |
              | Found:
              |""".trimMargin() + _existingTasks)
        }
        val _columnsTaskDateChanges: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTaskDateChanges.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("taskId", TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("priorPlannedDate", TableInfo.Column("priorPlannedDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("priorDeadlineDate", TableInfo.Column("priorDeadlineDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("newPlannedDate", TableInfo.Column("newPlannedDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("newDeadlineDate", TableInfo.Column("newDeadlineDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("changedAt", TableInfo.Column("changedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTaskDateChanges.put("reason", TableInfo.Column("reason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTaskDateChanges: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTaskDateChanges: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTaskDateChanges.add(TableInfo.Index("index_task_date_changes_taskId", false, listOf("taskId"), listOf("ASC")))
        _indicesTaskDateChanges.add(TableInfo.Index("index_task_date_changes_changedAt", false, listOf("changedAt"), listOf("ASC")))
        val _infoTaskDateChanges: TableInfo = TableInfo("task_date_changes", _columnsTaskDateChanges, _foreignKeysTaskDateChanges, _indicesTaskDateChanges)
        val _existingTaskDateChanges: TableInfo = read(connection, "task_date_changes")
        if (!_infoTaskDateChanges.equals(_existingTaskDateChanges)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |task_date_changes(com.daymark.app.data.TaskDateChange).
              | Expected:
              |""".trimMargin() + _infoTaskDateChanges + """
              |
              | Found:
              |""".trimMargin() + _existingTaskDateChanges)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tasks", "task_date_changes")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tasks", "task_date_changes")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TaskDao::class, TaskDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun taskDao(): TaskDao = _taskDao.value
}
