package com.xmartlabs.rxsimplenosql;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityUnitTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Tests for the RetrieveTask for querying data from the DB. This includes querying a single entity or
 * an entire bucket.
 */
public class NoSQLRetrieveTaskTest extends ActivityUnitTestCase<Activity> {
  private final String bucketId;
  private CountDownLatch signal;
  private final List<SampleBean> results;
  private Context context;

  public NoSQLRetrieveTaskTest() {
    super(Activity.class);
    bucketId = "retrieveTests";
    results = new ArrayList<>();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.context = getInstrumentation().getTargetContext();

    try {
      runTestOnUiThread(new Runnable() {
        @Override
        public void run() {
          signal = TestUtils.cleanBucket(context, SampleBean.class, bucketId);
        }
      });
    } catch (Throwable throwable) { // TODO: check this
      // an error happened
      throw new Exception(throwable);
    }
    signal.await(2, TimeUnit.SECONDS);

    signal = new CountDownLatch(1);
  }

  public void testRetrievalBuilder() throws Throwable {
    SampleBean entity = new SampleBean();
    entity.setId("entity");
    entity.setName("Colin");

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, bucketId);

    saveEntities(entity);

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .retrieve()
            .toList()
            .subscribe(new Action1<List<SampleBean>>() {
              @Override
              public void call(List<SampleBean> entities) {
                results.addAll(entities);
                signal.countDown();
              }
            });
      }
    });

    signal.await(2, TimeUnit.SECONDS);
    assertFalse("We should have results", results.isEmpty());
  }

  public void testGettingStoredData() throws Throwable {
    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, bucketId);
    final String entityId = "entityId";

    Runnable run = new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .entityId(entityId)
            .retrieve()
            .toList()
            .subscribe(new Action1<List<SampleBean>>() {
              @Override
              public void call(List<SampleBean> entities) {
                results.addAll(entities);
                signal.countDown();
              }
            });
      }
    };
    gettingStoredDataWithRunnable(entityId, run);
  }

  private void gettingStoredDataWithRunnable(final String entityId, Runnable runnable) throws Throwable {
    SampleBean entity = getTestEntry(entityId);
    saveEntities(entity);

    runTestOnUiThread(runnable);

    signal.await(2, TimeUnit.SECONDS);

    assertNotNull("We should have retrieved the entities", results);
    assertEquals(1, results.size());

    SampleBean retEntity = results.get(0);
    assertNotNull("The retrieved entity should be non-null", retEntity);
    assertEquals(entityId, retEntity.getId());
    assertEquals(entity, retEntity);
    assertEquals(entity.getName(), retEntity.getName());
    assertEquals(4, retEntity.getListing().size());
    List<String> ids = retEntity.getListing();
    for (int i = 0; i < ids.size(); i++) {
      assertEquals("ID" + i, ids.get(i));
    }
  }

  public void testGettingFilteredResults() throws Throwable {
    SampleBean entity1 = getTestEntry("entity1");
    SampleBean entity2 = getTestEntry("entity2");

    saveEntities(entity1, entity2);

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, bucketId);

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .filter(new Func1<SampleBean, Boolean>() {
              @Override
              public Boolean call(SampleBean sampleBean) {
                return sampleBean.getId().equals("entity2");
              }
            })
            .retrieve()
            .toList()
            .subscribe(new Action1<List<SampleBean>>() {
              @Override
              public void call(List<SampleBean> entities) {
                results.addAll(entities);
                signal.countDown();
              }
            });
      }
    });
    signal.await(2, TimeUnit.SECONDS);

    assertNotNull("The list of entities should not be null", results);
    assertEquals(1, results.size());
    assertEquals("entity2", results.get(0).getId());
  }

  public void testOldData() throws Throwable {
    final OldSampleBean oldEntity = new OldSampleBean();
    oldEntity.setName("Colin");
    oldEntity.setField1("Developer");
    oldEntity.setId("old");

    final Bucket<OldSampleBean> oldBucket = new Bucket<>(context, OldSampleBean.class, "oldBucketId");
    final Bucket<SampleBean> newBucket = new Bucket<>(context, SampleBean.class, "oldBucketId"); // With the same id

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        oldBucket
            .newQuery()
            .save(oldEntity)
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
        newBucket
            .newQuery()
            .entityId("old")
            .retrieve()
            .toList()
            .subscribe(new Action1<List<SampleBean>>() {
              @Override
              public void call(List<SampleBean> entities) {
                results.addAll(entities);
                signal.countDown();
              }
            });
      }
    });
    signal.await(2, TimeUnit.SECONDS);

    assertFalse("Should have gotten results", results.isEmpty());
    SampleBean entity = results.get(0);
    assertEquals("Name data should match between old and new", oldEntity.getName(), entity.getName());
    assertEquals("Field1 data should match between old and new", oldEntity.getField1(), entity.getField1());
    assertEquals("ID data should match between old and new", oldEntity.getId(), entity.getId());
    assertNull("Old entity didn't have an innerBean, so this one shouldn't either", entity.getInnerBean());
  }

  private void saveEntities(SampleBean... entitiesArray) throws Throwable {
    final List<SampleBean> entities = new ArrayList<>(entitiesArray.length);

    Collections.addAll(entities, entitiesArray);

    final Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, bucketId);

    final CountDownLatch saveLatch = new CountDownLatch(1);
    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        bucket
            .newQuery()
            .save(entities)
            .subscribe(new Action0() {
              @Override
              public void call() {
                saveLatch.countDown();
              }
            });
      }
    });

    saveLatch.await(3, TimeUnit.SECONDS);
  }

  //TODO: Add a test for getting all entities of a bucket

  private SampleBean getTestEntry(String entityId) {
    SampleBean entity = new SampleBean();
    entity.setId(entityId);
    entity.setName("SimpleNoSQL");
    List<String> ids = new ArrayList<>(4);
    for (int i = 0; i < 4; i++) {
      ids.add("ID" + i);
    }
    entity.setListing(ids);
    return entity;
  }
}
