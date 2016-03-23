package com.xmartlabs.rxsimplenosql;

/**
 * This is going to represent SampleBean in an older form. Basically before we added additional fields into it.
 */
class OldSampleBean implements Entity {
  private String name;
  private String field1;
  private String id;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getField1() {
    return field1;
  }

  public void setField1(String field1) {
    this.field1 = field1;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
