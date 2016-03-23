package com.xmartlabs.rxsimplenosql;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ActivityUnitTestCase;

import com.colintmiller.simplenosql.GsonSerialization;
import com.colintmiller.simplenosql.db.SimpleNoSQLContract;
import com.colintmiller.simplenosql.db.SimpleNoSQLDBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.functions.Action0;

/**
 * Tests for saving entities to the DB. This includes saving a single entity or saving multiple entities.
 */
public class NoSQLSaveTaskTest extends ActivityUnitTestCase<Activity> {
  private final GsonSerialization serialization;
  private CountDownLatch signal;
  private Context context;

  public NoSQLSaveTaskTest() {
    super(Activity.class);
    serialization = new GsonSerialization();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    signal = new CountDownLatch(1);
    this.context = getInstrumentation().getTargetContext();
  }

  public void testSaveEntity() throws Throwable {
    final SampleBean entity = new SampleBean();
    entity.setName("SimpleNoSQL");
    entity.setId("first");

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, "test");

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .save(entity)
            .subscribe(new Action0() {
              @Override
              public void call() {
                signal.countDown();
              }
            });
      }
    });
    signal.await(3, TimeUnit.SECONDS);

    assertNotNull("Activity is null when it should not have been", getInstrumentation().getTargetContext());
    SimpleNoSQLDBHelper sqlDbHelper = new SimpleNoSQLDBHelper(getInstrumentation().getTargetContext(), serialization, serialization);
    SQLiteDatabase db = sqlDbHelper.getReadableDatabase();
    String[] columns = {SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_ENTITY_ID, SimpleNoSQLContract.EntityEntry.COLUMN_NAME_DATA};
    String[] selectionArgs = {"test", "first"};
    Cursor cursor = db.query(SimpleNoSQLContract.EntityEntry.TABLE_NAME, columns,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID + "=? and " + SimpleNoSQLContract.EntityEntry.COLUMN_NAME_ENTITY_ID + "=?",
        selectionArgs, null, null, null);
    assertNotNull(cursor);
    assertEquals(cursor.getCount(), 1);
  }

  public void testSaveEntities() throws Throwable {
    final List<SampleBean> entities = new ArrayList<>(3);
    for (int i = 0; i < 3; i++) {
      SampleBean entity = new SampleBean();
      entity.setId(String.valueOf(i));
      entity.setExists(i % 2 == 0);
      entities.add(entity);
    }

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, "sample");

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .save(entities)
            .subscribe(new Action0() {
              @Override
              public void call() {
                signal.countDown();
              }
            });
      }
    });

    signal.await(2, TimeUnit.SECONDS);

    SimpleNoSQLDBHelper sqlDbHelper = new SimpleNoSQLDBHelper(getInstrumentation().getTargetContext(), serialization, serialization);
    SQLiteDatabase db = sqlDbHelper.getReadableDatabase();
    String[] columns = {SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_ENTITY_ID, SimpleNoSQLContract.EntityEntry.COLUMN_NAME_DATA};
    String[] selectionArgs = {"sample"};
    Cursor cursor = db.query(SimpleNoSQLContract.EntityEntry.TABLE_NAME, columns,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID + "=?",
        selectionArgs, null, null, null);
    assertNotNull(cursor);

    assertEquals(3, cursor.getCount());
    int counter = 0;
    while (cursor.moveToNext()) {
      String data = cursor.getString(cursor.getColumnIndex(SimpleNoSQLContract.EntityEntry.COLUMN_NAME_DATA));
      SampleBean entity = serialization.deserialize(data, SampleBean.class);
      assertEquals(String.valueOf(counter), entity.getId());
      assertEquals(counter % 2 == 0, entity.isExists());
      counter++;
    }
  }

  // TODO: Write an "update" test (overrides an already saved entity)
}
