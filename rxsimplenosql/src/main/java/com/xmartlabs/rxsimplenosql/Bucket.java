package com.xmartlabs.rxsimplenosql;

import android.content.Context;

import com.colintmiller.simplenosql.DataFilter;
import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.NoSQLQuery;
import com.colintmiller.simplenosql.OperationObserver;
import com.colintmiller.simplenosql.QueryBuilder;
import com.colintmiller.simplenosql.RetrievalCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rx.Completable;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/** Reactive bucket that operates with {@link NoSQLEntity}, {@link NoSQLQuery} and {@link QueryBuilder} */
public class Bucket<T extends Entity> {
  private final Context context;
  private final Class<T> type;
  private final String bucketId;

  public Bucket(Context context, Class<T> type, String bucketId) {
    this.context = context;
    this.type = type;
    this.bucketId = bucketId;
  }

  public BucketQueryBuilder newQuery() {
    return new BucketQueryBuilder(context, type, bucketId);
  }

  class BucketQueryBuilder {
    private QueryBuilder<T> queryBuilder;

    public BucketQueryBuilder(Context context, Class<T> type, String bucketId) {
      queryBuilder = NoSQL.with(context).using(type);
      queryBuilder.bucketId(bucketId);
    }

    public Completable save(T entity) {
      return save(Collections.singletonList(entity));
    }

    public Completable save(Collection<? extends T> entities) {
      final List<NoSQLEntity<T>> noSQLEntities = new ArrayList<>();
      for (T entity : entities) {
        NoSQLEntity<T> noSQLEntity = new NoSQLEntity<>(bucketId, entity.getId(), entity);
        noSQLEntities.add(noSQLEntity);
      }

      return Completable.create(new Completable.CompletableOnSubscribe() {
        @Override
        public void call(final Completable.CompletableSubscriber completableSubscriber) {
          queryBuilder
              .addObserver(new OperationObserver() {
                @Override
                public void hasFinished() {
                  completableSubscriber.onCompleted();
                }
              })
              .save(noSQLEntities);
        }
      });
    }

    @SuppressWarnings("unused")
    public BucketQueryBuilder entity(T t) {
      return entityId(t.getId());
    }

    public BucketQueryBuilder entityId(String entityId) {
      queryBuilder.entityId(entityId);
      return this;
    }

    public BucketQueryBuilder filter(final Func1<T, Boolean> predicate) {
      queryBuilder.filter(new DataFilter<T>() {
        @Override
        public boolean isIncluded(NoSQLEntity<T> item) {
          return predicate.call(item.getData());
        }
      });
      return this;
    }

    public Observable<T> retrieve() {
      return Observable.create(new Observable.OnSubscribe<T>() {
        @Override
        public void call(final Subscriber<? super T> subscriber) {
          queryBuilder.retrieve(new RetrievalCallback<T>() {
            @Override
            public void retrievedResults(List<NoSQLEntity<T>> noSQLEntities) {
              for (NoSQLEntity<T> noSQLEntity : noSQLEntities) {
                T t = noSQLEntity.getData();
                subscriber.onNext(t);
              }
              subscriber.onCompleted();
            }
          });
        }
      });
    }

    public Completable delete() {
      return Completable.create(new Completable.CompletableOnSubscribe() {
        @Override
        public void call(final Completable.CompletableSubscriber completableSubscriber) {
          queryBuilder
              .addObserver(new OperationObserver() {
                @Override
                public void hasFinished() {
                  completableSubscriber.onCompleted();
                }
              })
              .delete();
        }
      });
    }
  }
}
