package com.daymark.app.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import java.time.LocalDate
import java.time.LocalTime
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTaskEntity: EntityInsertAdapter<TaskEntity>

  private val __dateConverters: DateConverters = DateConverters()

  private val __insertAdapterOfTaskDateChange: EntityInsertAdapter<TaskDateChange>

  private val __deleteAdapterOfTaskEntity: EntityDeleteOrUpdateAdapter<TaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTaskEntity = object : EntityInsertAdapter<TaskEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `tasks` (`id`,`title`,`description`,`plannedDate`,`deadlineDate`,`deadlineTime`,`priority`,`estimatedDurationMinutes`,`category`,`isCompleted`,`createdAt`,`updatedAt`,`completedAt`,`reminderAt`,`reminderEnabled`,`archivedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDescription)
        }
        val _tmpPlannedDate: LocalDate? = entity.plannedDate
        val _tmp: String? = __dateConverters.fromDate(_tmpPlannedDate)
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmp)
        }
        val _tmpDeadlineDate: LocalDate? = entity.deadlineDate
        val _tmp_1: String? = __dateConverters.fromDate(_tmpDeadlineDate)
        if (_tmp_1 == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp_1)
        }
        val _tmpDeadlineTime: LocalTime? = entity.deadlineTime
        val _tmp_2: String? = __dateConverters.fromTime(_tmpDeadlineTime)
        if (_tmp_2 == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmp_2)
        }
        val _tmp_3: String = __dateConverters.fromPriority(entity.priority)
        statement.bindText(7, _tmp_3)
        val _tmpEstimatedDurationMinutes: Int? = entity.estimatedDurationMinutes
        if (_tmpEstimatedDurationMinutes == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpEstimatedDurationMinutes.toLong())
        }
        val _tmpCategory: String? = entity.category
        if (_tmpCategory == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpCategory)
        }
        val _tmp_4: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(10, _tmp_4.toLong())
        statement.bindLong(11, entity.createdAt)
        statement.bindLong(12, entity.updatedAt)
        val _tmpCompletedAt: Long? = entity.completedAt
        if (_tmpCompletedAt == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpCompletedAt)
        }
        val _tmpReminderAt: Long? = entity.reminderAt
        if (_tmpReminderAt == null) {
          statement.bindNull(14)
        } else {
          statement.bindLong(14, _tmpReminderAt)
        }
        val _tmp_5: Int = if (entity.reminderEnabled) 1 else 0
        statement.bindLong(15, _tmp_5.toLong())
        val _tmpArchivedAt: Long? = entity.archivedAt
        if (_tmpArchivedAt == null) {
          statement.bindNull(16)
        } else {
          statement.bindLong(16, _tmpArchivedAt)
        }
      }
    }
    this.__insertAdapterOfTaskDateChange = object : EntityInsertAdapter<TaskDateChange>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `task_date_changes` (`id`,`taskId`,`priorPlannedDate`,`priorDeadlineDate`,`newPlannedDate`,`newDeadlineDate`,`changedAt`,`reason`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskDateChange) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.taskId)
        val _tmpPriorPlannedDate: LocalDate? = entity.priorPlannedDate
        val _tmp: String? = __dateConverters.fromDate(_tmpPriorPlannedDate)
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmp)
        }
        val _tmpPriorDeadlineDate: LocalDate? = entity.priorDeadlineDate
        val _tmp_1: String? = __dateConverters.fromDate(_tmpPriorDeadlineDate)
        if (_tmp_1 == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmp_1)
        }
        val _tmpNewPlannedDate: LocalDate? = entity.newPlannedDate
        val _tmp_2: String? = __dateConverters.fromDate(_tmpNewPlannedDate)
        if (_tmp_2 == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp_2)
        }
        val _tmpNewDeadlineDate: LocalDate? = entity.newDeadlineDate
        val _tmp_3: String? = __dateConverters.fromDate(_tmpNewDeadlineDate)
        if (_tmp_3 == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmp_3)
        }
        statement.bindLong(7, entity.changedAt)
        statement.bindText(8, entity.reason)
      }
    }
    this.__deleteAdapterOfTaskEntity = object : EntityDeleteOrUpdateAdapter<TaskEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `tasks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun upsert(task: TaskEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTaskEntity.insert(_connection, task)
  }

  public override suspend fun insertDateChange(change: TaskDateChange): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTaskDateChange.insert(_connection, change)
  }

  public override suspend fun delete(task: TaskEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfTaskEntity.handle(_connection, task)
  }

  public override suspend fun deleteById(id: String): Unit = performInTransactionSuspending(__db) {
    super@TaskDao_Impl.deleteById(id)
  }

  public override suspend fun deleteAll(): Unit = performInTransactionSuspending(__db) {
    super@TaskDao_Impl.deleteAll()
  }

  public override suspend fun reschedule(
    taskId: String,
    planned: LocalDate?,
    deadline: LocalDate?,
    reminderAt: Long?,
    reason: String,
  ): Unit = performInTransactionSuspending(__db) {
    super@TaskDao_Impl.reschedule(taskId, planned, deadline, reminderAt, reason)
  }

  public override suspend fun byId(id: String): TaskEntity? {
    val _sql: String = "SELECT * FROM tasks WHERE id=?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: TaskEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _result = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeActive(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE archivedAt IS NULL ORDER BY isCompleted, COALESCE(deadlineDate, plannedDate), CASE priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePlanned(date: LocalDate): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE plannedDate=? AND archivedAt IS NULL ORDER BY isCompleted, priority DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(date)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_2)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_3: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_3 = null
          } else {
            _tmp_3 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_3)
          val _tmpPriority: TaskPriority
          val _tmp_4: String
          _tmp_4 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_4)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_5 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_6 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDeadline(date: LocalDate): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE deadlineDate=? AND archivedAt IS NULL ORDER BY isCompleted, priority DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(date)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_2)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_3: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_3 = null
          } else {
            _tmp_3 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_3)
          val _tmpPriority: TaskPriority
          val _tmp_4: String
          _tmp_4 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_4)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_5 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_6 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeInbox(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE isCompleted=0 AND plannedDate IS NULL AND deadlineDate IS NULL AND archivedAt IS NULL ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeOverdue(today: LocalDate): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE isCompleted=0 AND deadlineDate < ? AND archivedAt IS NULL ORDER BY deadlineDate"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(today)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_2)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_3: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_3 = null
          } else {
            _tmp_3 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_3)
          val _tmpPriority: TaskPriority
          val _tmp_4: String
          _tmp_4 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_4)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_5 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_6 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCompleted(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE isCompleted=1 AND archivedAt IS NULL ORDER BY completedAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun search(query: String): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE title LIKE '%' || ? || '%' AND archivedAt IS NULL ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun reminders(): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE reminderEnabled=1 AND reminderAt IS NOT NULL AND isCompleted=0 AND archivedAt IS NULL"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_2)
          val _tmpPriority: TaskPriority
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_3)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_4 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_5 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun widgetTasksInRange(start: LocalDate, end: LocalDate): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE archivedAt IS NULL AND ((plannedDate BETWEEN ? AND ?) OR (deadlineDate BETWEEN ? AND ?)) ORDER BY isCompleted, COALESCE(deadlineDate, plannedDate), priority DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(start)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        _argIndex = 2
        val _tmp_1: String? = __dateConverters.fromDate(end)
        if (_tmp_1 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_1)
        }
        _argIndex = 3
        val _tmp_2: String? = __dateConverters.fromDate(start)
        if (_tmp_2 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_2)
        }
        _argIndex = 4
        val _tmp_3: String? = __dateConverters.fromDate(end)
        if (_tmp_3 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_3)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp_4: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp_4 = null
          } else {
            _tmp_4 = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp_4)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_5: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_5 = null
          } else {
            _tmp_5 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_5)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_6: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_6 = null
          } else {
            _tmp_6 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_6)
          val _tmpPriority: TaskPriority
          val _tmp_7: String
          _tmp_7 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_7)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_8: Int
          _tmp_8 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_8 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_9: Int
          _tmp_9 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_9 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun widgetUpcomingTasks(
    today: LocalDate,
    untilDate: LocalDate,
    limit: Int,
  ): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE archivedAt IS NULL AND isCompleted=0 AND ((plannedDate BETWEEN ? AND ?) OR (deadlineDate BETWEEN ? AND ?)) ORDER BY COALESCE(deadlineDate, plannedDate), priority DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(today)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        _argIndex = 2
        val _tmp_1: String? = __dateConverters.fromDate(untilDate)
        if (_tmp_1 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_1)
        }
        _argIndex = 3
        val _tmp_2: String? = __dateConverters.fromDate(today)
        if (_tmp_2 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_2)
        }
        _argIndex = 4
        val _tmp_3: String? = __dateConverters.fromDate(untilDate)
        if (_tmp_3 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_3)
        }
        _argIndex = 5
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPlannedDate: Int = getColumnIndexOrThrow(_stmt, "plannedDate")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _columnIndexOfDeadlineTime: Int = getColumnIndexOrThrow(_stmt, "deadlineTime")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfEstimatedDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedDurationMinutes")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _columnIndexOfReminderAt: Int = getColumnIndexOrThrow(_stmt, "reminderAt")
        val _columnIndexOfReminderEnabled: Int = getColumnIndexOrThrow(_stmt, "reminderEnabled")
        val _columnIndexOfArchivedAt: Int = getColumnIndexOrThrow(_stmt, "archivedAt")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpPlannedDate: LocalDate?
          val _tmp_4: String?
          if (_stmt.isNull(_columnIndexOfPlannedDate)) {
            _tmp_4 = null
          } else {
            _tmp_4 = _stmt.getText(_columnIndexOfPlannedDate)
          }
          _tmpPlannedDate = __dateConverters.toDate(_tmp_4)
          val _tmpDeadlineDate: LocalDate?
          val _tmp_5: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmp_5 = null
          } else {
            _tmp_5 = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _tmpDeadlineDate = __dateConverters.toDate(_tmp_5)
          val _tmpDeadlineTime: LocalTime?
          val _tmp_6: String?
          if (_stmt.isNull(_columnIndexOfDeadlineTime)) {
            _tmp_6 = null
          } else {
            _tmp_6 = _stmt.getText(_columnIndexOfDeadlineTime)
          }
          _tmpDeadlineTime = __dateConverters.toTime(_tmp_6)
          val _tmpPriority: TaskPriority
          val _tmp_7: String
          _tmp_7 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __dateConverters.toPriority(_tmp_7)
          val _tmpEstimatedDurationMinutes: Int?
          if (_stmt.isNull(_columnIndexOfEstimatedDurationMinutes)) {
            _tmpEstimatedDurationMinutes = null
          } else {
            _tmpEstimatedDurationMinutes = _stmt.getLong(_columnIndexOfEstimatedDurationMinutes).toInt()
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpIsCompleted: Boolean
          val _tmp_8: Int
          _tmp_8 = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp_8 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          val _tmpReminderAt: Long?
          if (_stmt.isNull(_columnIndexOfReminderAt)) {
            _tmpReminderAt = null
          } else {
            _tmpReminderAt = _stmt.getLong(_columnIndexOfReminderAt)
          }
          val _tmpReminderEnabled: Boolean
          val _tmp_9: Int
          _tmp_9 = _stmt.getLong(_columnIndexOfReminderEnabled).toInt()
          _tmpReminderEnabled = _tmp_9 != 0
          val _tmpArchivedAt: Long?
          if (_stmt.isNull(_columnIndexOfArchivedAt)) {
            _tmpArchivedAt = null
          } else {
            _tmpArchivedAt = _stmt.getLong(_columnIndexOfArchivedAt)
          }
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPlannedDate,_tmpDeadlineDate,_tmpDeadlineTime,_tmpPriority,_tmpEstimatedDurationMinutes,_tmpCategory,_tmpIsCompleted,_tmpCreatedAt,_tmpUpdatedAt,_tmpCompletedAt,_tmpReminderAt,_tmpReminderEnabled,_tmpArchivedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun widgetOverdueCount(today: LocalDate, now: LocalTime): Int {
    val _sql: String = "SELECT COUNT(*) FROM tasks WHERE archivedAt IS NULL AND isCompleted=0 AND (deadlineDate < ? OR (deadlineDate = ? AND deadlineTime < ?))"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __dateConverters.fromDate(today)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        _argIndex = 2
        val _tmp_1: String? = __dateConverters.fromDate(today)
        if (_tmp_1 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_1)
        }
        _argIndex = 3
        val _tmp_2: String? = __dateConverters.fromTime(now)
        if (_tmp_2 == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp_2)
        }
        val _result: Int
        if (_stmt.step()) {
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(0).toInt()
          _result = _tmp_3
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDateChanges(taskId: String): Flow<List<TaskDateChange>> {
    val _sql: String = "SELECT * FROM task_date_changes WHERE taskId=? ORDER BY changedAt DESC"
    return createFlow(__db, false, arrayOf("task_date_changes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskId: Int = getColumnIndexOrThrow(_stmt, "taskId")
        val _columnIndexOfPriorPlannedDate: Int = getColumnIndexOrThrow(_stmt, "priorPlannedDate")
        val _columnIndexOfPriorDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "priorDeadlineDate")
        val _columnIndexOfNewPlannedDate: Int = getColumnIndexOrThrow(_stmt, "newPlannedDate")
        val _columnIndexOfNewDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "newDeadlineDate")
        val _columnIndexOfChangedAt: Int = getColumnIndexOrThrow(_stmt, "changedAt")
        val _columnIndexOfReason: Int = getColumnIndexOrThrow(_stmt, "reason")
        val _result: MutableList<TaskDateChange> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskDateChange
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTaskId: String
          _tmpTaskId = _stmt.getText(_columnIndexOfTaskId)
          val _tmpPriorPlannedDate: LocalDate?
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfPriorPlannedDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfPriorPlannedDate)
          }
          _tmpPriorPlannedDate = __dateConverters.toDate(_tmp)
          val _tmpPriorDeadlineDate: LocalDate?
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfPriorDeadlineDate)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfPriorDeadlineDate)
          }
          _tmpPriorDeadlineDate = __dateConverters.toDate(_tmp_1)
          val _tmpNewPlannedDate: LocalDate?
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfNewPlannedDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfNewPlannedDate)
          }
          _tmpNewPlannedDate = __dateConverters.toDate(_tmp_2)
          val _tmpNewDeadlineDate: LocalDate?
          val _tmp_3: String?
          if (_stmt.isNull(_columnIndexOfNewDeadlineDate)) {
            _tmp_3 = null
          } else {
            _tmp_3 = _stmt.getText(_columnIndexOfNewDeadlineDate)
          }
          _tmpNewDeadlineDate = __dateConverters.toDate(_tmp_3)
          val _tmpChangedAt: Long
          _tmpChangedAt = _stmt.getLong(_columnIndexOfChangedAt)
          val _tmpReason: String
          _tmpReason = _stmt.getText(_columnIndexOfReason)
          _item = TaskDateChange(_tmpId,_tmpTaskId,_tmpPriorPlannedDate,_tmpPriorDeadlineDate,_tmpNewPlannedDate,_tmpNewDeadlineDate,_tmpChangedAt,_tmpReason)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTaskById(id: String) {
    val _sql: String = "DELETE FROM tasks WHERE id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteDateChanges(id: String) {
    val _sql: String = "DELETE FROM task_date_changes WHERE taskId=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun archive(id: String, at: Long) {
    val _sql: String = "UPDATE tasks SET archivedAt=?, updatedAt=? WHERE id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, at)
        _argIndex = 2
        _stmt.bindLong(_argIndex, at)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllTasks() {
    val _sql: String = "DELETE FROM tasks"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllDateChanges() {
    val _sql: String = "DELETE FROM task_date_changes"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
