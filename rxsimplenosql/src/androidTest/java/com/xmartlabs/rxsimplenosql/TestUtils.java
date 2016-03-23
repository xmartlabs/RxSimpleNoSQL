package com.xmartlabs.rxsimplenosql;

import android.content.Context;

import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.OperationObserver;

import java.util.concurrent.CountDownLatch;

import rx.functions.Action0;

class TestUtils {

  public static <T extends Entity> CountDownLatch cleanBucket(Context context, Class<T> type, String bucket) {
    final CountDownLatch signal = new CountDownLatch(1);

    new Bucket<>(context, type, bucket)
        .newQuery()
        .delete()
        .subscribe(new Action0() {
          @Override
          public void call() {
            signal.countDown();
          }
        });
    return signal;
  }
}
