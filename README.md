# RxSimpleNoSQL

Reactive extensions for [SimpleNoSQL](https://github.com/Jearil/SimpleNoSQL). Manipulate entities using `Observable`s
and `Completable`s.

## Examples

Suppose we have the following entity we want to manipulate:

```java
class SampleBean implements Entity {
    private String name;
    private String id;
    private Map<String, String> mapping;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }
}
```

We first start creating a bucket:

```java
Bucket<SampleBean> bucket = new Bucket<>(context, SampleBean.class, "bucketId");
```

### Save

#### Single

```java
SampleBean entity = new SampleBean();
entity.setId("1");
entity.setName("Colin");
Map<String, Integer> birthday = new HashMap<String, Integer>();
birthday.put("day", 17);
birthday.put("month", 2);
birthday.put("year", 1982);
entity.setMapping(birthday);

bucket.newQuery()
        .save(entity)
        .subscribe();
```

#### Multiple

```java
SampleBean entity2 = new SampleBean();
entity2.setId("2");
entity2.setName("Santiago");

SampleBean entity3 = new SampleBean();
entity3.setId("3");
entity3.setName("Xmartlabs");

bucket.newQuery()
        .save(Arrays.asList(entity2, entity3))
        .subscribe();
```

### Retrieve

#### Single

```java
bucket.newQuery()
        .entityId("entityId")
        .retrieve()
        .subscribe(sampleBean -> System.out.println("Name: %s", sampleBean.getName()));
```

#### Multiple

```java
bucket.newQuery()
        .filter(sampleBean -> sampleBean.getName().startsWith("S"))
        .retrieve()
        .subscribe(sampleBean -> System.out.println("Name: %s", sampleBean.getName()));
```

#### All

```java
bucket.newQuery()
        .retrieve()
        .subscribe(sampleBean -> System.out.println("Name: %s", sampleBean.getName()));
```

### Delete

#### Single

```java
bucket.newQuery()
        .entityId("entityId")
        .delete()
        .subscribe();
```

#### Multiple

Currently, SimpleNoSQL does not support the usage of `filter` for the `delete` operation.
There's an issue [opened](https://github.com/Jearil/SimpleNoSQL/issues/34).

Nevertheless, this functionality can be achieved by first retrieving the entities to be deleted and then performing the actual `delete` operation individually:

```java
Observable<SampleBean> itemsToDelete = bucket.newQuery()
        .filter(sampleBean -> sampleBean.getName().startsWith("S"))
        .retrieve()

Completable deleteCompletable = Completable.concat(
                                  itemsToDelete
                                      .map(item -> bucket.newQuery()
                                                      .entityId(item.getId()))
                                                      .delete());
```

#### All

```java
bucket.newQuery()
        .delete()
        .subscribe();
```

### Sorting

[As SimpleNoSQL sorts the results in memory]
(https://github.com/Jearil/SimpleNoSQL/blob/master/SimpleNoSQL/src/main/java/com/colintmiller/simplenosql/threading/DataDispatcher.java#L140),
you can carry this out the same way with `Observable#toSortedList`.

## Development

As SimpleNoSQL, this project still isn't stable (the API can change at any time). You can use it with Gradle and
[JitPack](https://jitpack.io):

```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```

Then adding the dependency:

```groovy
compile 'com.github.xmartlabs:RxSimpleNoSQL:-SNAPSHOT'
```

RxSimpleNoSQL requires at minimum Java 7 or Android 2.2.

## Build

[![Build Status](https://travis-ci.org/xmartlabs/RxSimpleNoSQL.svg?branch=master)](https://travis-ci.org/xmartlabs/RxSimpleNoSQL)

To build:

```shell
git clone https://github.com/xmartlabs/RxSimpleNoSQL.git
cd RxSimpleNoSQL/
./gradlew build
```

## License

```
Copyright 2016 Xmartlabs SRL.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
