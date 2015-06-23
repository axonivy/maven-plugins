package ch.ivyteam.db.meta.model.internal;


/**
 * @author rwei
 * @since 13.10.2009
 */
public class SqlDelete extends SqlDmlStatement
{
  /** Filter expression to filter the rows to delete */
  private SqlSimpleExpr fFilterExpression;
  /** The table to delete rows from */
  private String fTable;

  /**
   * Constructor
   * @param table
   * @param filterExpression 
   * @throws MetaException 
   */
  public SqlDelete(String table, SqlSimpleExpr filterExpression) throws MetaException
  {
    super(null, null);
    assert table != null : "Parameter foreignTable must not be null";
    fTable = table;
    fFilterExpression = filterExpression;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(1024);
    builder.append("DELETE FROM ");
    builder.append(fTable);
    if (fFilterExpression != null)
    {
      builder.append(" WHERE ");
      builder.append(fFilterExpression);
    }
    return builder.toString();
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
   * Returns the filterExpression
   * @return the filterExpression
   */
  public SqlSimpleExpr getFilterExpression()
  {
    return fFilterExpression;
  }

}
