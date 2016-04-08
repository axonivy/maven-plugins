package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

public class SqlInsertWithValues extends SqlInsert
{
  private final List<SqlLiteral> values;

  public SqlInsertWithValues(String table, List<String> columns, List<SqlLiteral> values,
          List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(table, columns, dbSysHints, comment);
    assert values != null : "Parameter values must not be null";
    this.values = values;
  }

  public List<SqlLiteral> getValues()
  {
    return values;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(4096);
    builder.append(super.toString());
    builder.append("VALUES (");
    SqlScriptUtil.formatCommaSeparated(builder, values);
    builder.append(")");
    return builder.toString();
  }
}
