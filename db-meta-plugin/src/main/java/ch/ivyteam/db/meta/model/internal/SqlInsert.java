package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

public abstract class SqlInsert extends SqlDmlStatement
{
  private final String table;
  private final List<String> columns;

  public SqlInsert(String table, List<String> columns, List<SqlDatabaseSystemHints> dbSysHints,
          String comment) throws MetaException
  {
    super(dbSysHints, comment);
    assert table != null : "Parameter table must not be null";
    assert columns != null : "Parameter columns must not be null";
    this.table = table;
    this.columns = columns;
  }

  public String getTable()
  {
    return table;
  }

  public List<String> getColumns()
  {
    return columns;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(4096);
    builder.append("INSERT INTO ");
    builder.append(table);
    builder.append(" (");
    SqlScriptUtil.formatCommaSeparated(builder, columns);
    builder.append(")");
    return builder.toString();
  }
}
