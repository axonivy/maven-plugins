package ch.ivyteam.db.meta.model.internal;

import java.util.List;

public class SqlInsertWithSelect extends SqlInsert {

  private final SqlSelect select;

  public SqlInsertWithSelect(String table, List<String> columns, SqlSelect select,
          List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException {
    super(table, columns, dbSysHints, comment);
    assert select != null : "Parameter select must not be null";
    this.select = select;
  }

  public SqlSelect getSelect() {
    return select;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(4096);
    builder.append(super.toString());
    builder.append("\n");
    builder.append(select.toString());
    return builder.toString();
  }
}
