package com.xmartlabs.rxsimplenosql;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.ActivityUnitTestCase;

import com.colintmiller.simplenosql.GsonSerialization;
import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.OperationObserver;
import com.colintmiller.simplenosql.db.SimpleNoSQLContract;
import com.colintmiller.simplenosql.db.SimpleNoSQLDBHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.functions.Action0;

/**
 * Tests to verify the deletion asyncTask performs as expected
 */
public class NoSQLDeleteTaskTest extends ActivityUnitTestCase<Activity> {
  private final GsonSerialization serialization;
  private Context context;
  private CountDownLatch signal;

  public NoSQLDeleteTaskTest() {
    super(Activity.class);
    serialization = new GsonSerialization();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    context = getInstrumentation().getTargetContext();
    signal = new CountDownLatch(1);
  }

  public void testDeleteEntity() throws Throwable {
    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, "delete");

    SampleBean entity1 = new SampleBean();
    SampleBean entity2 = new SampleBean();
    entity1.setId("1");
    entity2.setId("2");

    final List<SampleBean> entities = Arrays.asList(entity1, entity2);
    new ArrayList<>(entities);

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

    signal = new CountDownLatch(1);

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .entityId("1")
            .delete()
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
    String[] selectionArgs = {"delete"};
    Cursor cursor = db.query(SimpleNoSQLContract.EntityEntry.TABLE_NAME, columns,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID + "=?",
        selectionArgs, null, null, null);
    assertNotNull(cursor);

    assertEquals(1, cursor.getCount());
  }

  public void testDeleteBucket() throws Throwable {
    final List<SampleBean> lots = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      SampleBean entity = new SampleBean();
      entity.setId(String.valueOf(i));
      entity.setExists(i % 2 == 0);
      lots.add(entity);
    }

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, "delete");

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .save(lots)
            .subscribe();
      }
    });

    signal.await(2, TimeUnit.SECONDS);
    signal = new CountDownLatch(1);

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .delete()
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
    String[] selectionArgs = {"delete"};
    Cursor cursor = db.query(SimpleNoSQLContract.EntityEntry.TABLE_NAME, columns,
        SimpleNoSQLContract.EntityEntry.COLUMN_NAME_BUCKET_ID + "=?",
        selectionArgs, null, null, null);
    assertNotNull(cursor);

    assertEquals(0, cursor.getCount());
  }
}
