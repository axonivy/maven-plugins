package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * An sql insert statement
 * @author rwei
 * @since 01.10.2009
 */
public class SqlInsert extends SqlDmlStatement
{
  /** The table name */
  private String fTable;
  /** The columns */
  private List<String> fColumns;
  /** The values */
  private List<SqlLiteral> fValues;
  
  /**
   * Constructor
   * @param table 
   * @param columns 
   * @param values 
   * @param dbSysHints 
   * @param comment 
   * @throws MetaException
   */
  public SqlInsert(String table, List<String> columns, List<SqlLiteral> values, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(dbSysHints, comment);
    assert table != null : "Parameter table must not be null";
    assert columns != null : "Parameter columns must not be null";
    assert values != null : "Parameter values must not be null";
    fTable = table;
    fColumns = columns;
    fValues = values;
  }
  
  /**
   * Returns the Table
   * @return the Table
   */
  public String getTable()
  {
    return fTable;
  }

  /**
   * Returns the Columns
   * @return the Columns
   */
  public List<String> getColumns()
  {
    return fColumns;
  }

  /**
   * Returns the Values
   * @return the Values
   */
  public List<SqlLiteral> getValues()
  {
    return fValues;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(4096);
    builder.append("INSERT INTO ");
    builder.append(fTable);
    builder.append(" (");
    SqlScriptUtil.formatCommaSeparated(builder, fColumns);
    builder.append(") VALUES (");
    SqlScriptUtil.formatCommaSeparated(builder, fValues);
    return builder.toString();
  }
}
