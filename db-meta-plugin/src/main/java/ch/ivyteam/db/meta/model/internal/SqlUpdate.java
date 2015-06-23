package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * An sql update statement
 * @author rwei
 * @since 12.10.2009
 */
public class SqlUpdate extends SqlDmlStatement
{
  /** The name of the table to update */
  private String fTable;
  /** List with the columns and the expressions use in the SET clause */ 
  private List<SqlUpdateColumnExpression> fColumnExpressions;
  /** Ther filter expression used in the WHERE clause */
  private SqlSimpleExpr fFilterExpression;

  /**
   * Constructor
   * @param table
   * @param columnExpressions
   * @param filterExpr
   * @param dbSysHints
   * @param comment
   * @throws MetaException 
   */
  public SqlUpdate(String table, List<SqlUpdateColumnExpression> columnExpressions, SqlSimpleExpr filterExpr, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(dbSysHints, comment);
    fTable = table;
    assert columnExpressions != null : "Parameter columnExpressions must not be null";
    fColumnExpressions = columnExpressions;
    fFilterExpression = filterExpr;    
  }
  
  /**
   * Returns the table
   * @return the table
   */
  public String getTable()
  {
    return fTable;
  }
  
  /**
   * Returns the columnExpressions
   * @return the columnExpressions
   */
  public List<SqlUpdateColumnExpression> getColumnExpressions()
  {
    return fColumnExpressions;
  }
  
  /**
   * Returns the filterExpression
   * @return the filterExpression
   */
  public SqlSimpleExpr getFilterExpression()
  {
    return fFilterExpression;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(1024);
    builder.append("UPDATE ");
    if (fTable != null)
    {
      builder.append(fTable);
      builder.append(" ");
    }
    builder.append("SET ");
    SqlScriptUtil.formatCommaSeparated(builder, fColumnExpressions);
    if (fFilterExpression != null)
    {
      builder.append(" WHERE ");
      builder.append(fFilterExpression);
    }
    return builder.toString();
  }

  /**
   * Sets the table
   * @param table
   */
  public void setTable(String table)
  {
    assert table != null : "Parameter table must not be null";
    fTable = table;
  }

}
