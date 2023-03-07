package ch.ivyteam.db.meta.model.internal;

public class SqlTableId {

  private String name;
  private String alias;

  public SqlTableId(String name) {
    this(name, null);
  }

  public SqlTableId(String name, String alias) {
    this.name = name;
    this.alias = alias;
  }

  public String getName() {
    return name;
  }

  public String getAlias() {
    return alias;
  }

  @Override
  public String toString() {
    return name + (alias == null ? "" : " AS " + alias);
  }
}
