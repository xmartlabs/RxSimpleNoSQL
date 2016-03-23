package com.xmartlabs.rxsimplenosql;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.ActivityUnitTestCase;

import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.OperationObserver;
import com.colintmiller.simplenosql.RetrievalCallback;
import com.colintmiller.simplenosql.threading.QueryDelivery;
import com.colintmiller.simplenosql.toolbox.SynchronousRetrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import rx.functions.Action0;
import rx.functions.Action1;

public class SynchronousTest extends ActivityUnitTestCase<Activity> {

  private Context context;

  public SynchronousTest() {
    super(Activity.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.context = getInstrumentation().getTargetContext();
  }

  public void testSynchronousGet() throws Throwable {
    final CountDownLatch latch = new CountDownLatch(1);
    final List<SampleBean> result = new ArrayList<>();
    assertEquals(0, result.size());
    new Bucket<>(context, SampleBean.class, "dne")
        .newQuery()
        .retrieve()
        .toList()
        .subscribe(new Action1<List<SampleBean>>() {
          @Override
          public void call(List<SampleBean> sampleBeen) {
            result.add(new SampleBean());
            latch.countDown();
          }
        });
    latch.await();
    assertEquals(1, result.size());
  }
}
