package com.leeam.cryptowidget.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AlertDao_Impl implements AlertDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AlertEntity> __insertionAdapterOfAlertEntity;

  private final AlertDatabase.Converters __converters = new AlertDatabase.Converters();

  private final EntityDeletionOrUpdateAdapter<AlertEntity> __deletionAdapterOfAlertEntity;

  private final EntityDeletionOrUpdateAdapter<AlertEntity> __updateAdapterOfAlertEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkAlertFired;

  private final SharedSQLiteStatement __preparedStmtOfSetAlertEnabled;

  public AlertDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAlertEntity = new EntityInsertionAdapter<AlertEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `price_alerts` (`id`,`coinId`,`symbol`,`direction`,`thresholdUsd`,`isEnabled`,`createdAtMs`,`firedAtMs`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlertEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCoinId());
        statement.bindString(3, entity.getSymbol());
        final String _tmp = __converters.fromDirection(entity.getDirection());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getThresholdUsd());
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindLong(7, entity.getCreatedAtMs());
        if (entity.getFiredAtMs() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFiredAtMs());
        }
      }
    };
    this.__deletionAdapterOfAlertEntity = new EntityDeletionOrUpdateAdapter<AlertEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `price_alerts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlertEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfAlertEntity = new EntityDeletionOrUpdateAdapter<AlertEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `price_alerts` SET `id` = ?,`coinId` = ?,`symbol` = ?,`direction` = ?,`thresholdUsd` = ?,`isEnabled` = ?,`createdAtMs` = ?,`firedAtMs` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlertEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCoinId());
        statement.bindString(3, entity.getSymbol());
        final String _tmp = __converters.fromDirection(entity.getDirection());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getThresholdUsd());
        final int _tmp_1 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindLong(7, entity.getCreatedAtMs());
        if (entity.getFiredAtMs() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFiredAtMs());
        }
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfMarkAlertFired = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE price_alerts SET firedAtMs = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetAlertEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE price_alerts SET isEnabled = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAlert(final AlertEntity alert, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAlertEntity.insertAndReturnId(alert);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAlert(final AlertEntity alert, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAlertEntity.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlert(final AlertEntity alert, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAlertEntity.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAlertFired(final int id, final long firedAtMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAlertFired.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, firedAtMs);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAlertFired.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setAlertEnabled(final int id, final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetAlertEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetAlertEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AlertEntity>> getAllAlerts() {
    final String _sql = "SELECT * FROM price_alerts ORDER BY createdAtMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"price_alerts"}, new Callable<List<AlertEntity>>() {
      @Override
      @NonNull
      public List<AlertEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoinId = CursorUtil.getColumnIndexOrThrow(_cursor, "coinId");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfThresholdUsd = CursorUtil.getColumnIndexOrThrow(_cursor, "thresholdUsd");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfCreatedAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMs");
          final int _cursorIndexOfFiredAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "firedAtMs");
          final List<AlertEntity> _result = new ArrayList<AlertEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlertEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpCoinId;
            _tmpCoinId = _cursor.getString(_cursorIndexOfCoinId);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final AlertDirection _tmpDirection;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfDirection);
            _tmpDirection = __converters.toDirection(_tmp);
            final double _tmpThresholdUsd;
            _tmpThresholdUsd = _cursor.getDouble(_cursorIndexOfThresholdUsd);
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final long _tmpCreatedAtMs;
            _tmpCreatedAtMs = _cursor.getLong(_cursorIndexOfCreatedAtMs);
            final Long _tmpFiredAtMs;
            if (_cursor.isNull(_cursorIndexOfFiredAtMs)) {
              _tmpFiredAtMs = null;
            } else {
              _tmpFiredAtMs = _cursor.getLong(_cursorIndexOfFiredAtMs);
            }
            _item = new AlertEntity(_tmpId,_tmpCoinId,_tmpSymbol,_tmpDirection,_tmpThresholdUsd,_tmpIsEnabled,_tmpCreatedAtMs,_tmpFiredAtMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getEnabledAlertsForCoin(final String coinId,
      final Continuation<? super List<AlertEntity>> $completion) {
    final String _sql = "SELECT * FROM price_alerts WHERE coinId = ? AND isEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, coinId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AlertEntity>>() {
      @Override
      @NonNull
      public List<AlertEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoinId = CursorUtil.getColumnIndexOrThrow(_cursor, "coinId");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfThresholdUsd = CursorUtil.getColumnIndexOrThrow(_cursor, "thresholdUsd");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfCreatedAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMs");
          final int _cursorIndexOfFiredAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "firedAtMs");
          final List<AlertEntity> _result = new ArrayList<AlertEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlertEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpCoinId;
            _tmpCoinId = _cursor.getString(_cursorIndexOfCoinId);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final AlertDirection _tmpDirection;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfDirection);
            _tmpDirection = __converters.toDirection(_tmp);
            final double _tmpThresholdUsd;
            _tmpThresholdUsd = _cursor.getDouble(_cursorIndexOfThresholdUsd);
            final boolean _tmpIsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_1 != 0;
            final long _tmpCreatedAtMs;
            _tmpCreatedAtMs = _cursor.getLong(_cursorIndexOfCreatedAtMs);
            final Long _tmpFiredAtMs;
            if (_cursor.isNull(_cursorIndexOfFiredAtMs)) {
              _tmpFiredAtMs = null;
            } else {
              _tmpFiredAtMs = _cursor.getLong(_cursorIndexOfFiredAtMs);
            }
            _item = new AlertEntity(_tmpId,_tmpCoinId,_tmpSymbol,_tmpDirection,_tmpThresholdUsd,_tmpIsEnabled,_tmpCreatedAtMs,_tmpFiredAtMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
