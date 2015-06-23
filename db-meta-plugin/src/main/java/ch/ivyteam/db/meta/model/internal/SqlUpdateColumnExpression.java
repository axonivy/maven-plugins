package ch.ivyteam.db.meta.model.internal;


/**
 * Column Update expression like columnname = expr
 * @author rwei
 * @since 12.10.2009
 */
public class SqlUpdateColumnExpression
{
  /** The name of the column to update */
  private String fColumnName;
  /** The expression to evaluate the value to update the column with */
  private SqlAtom fExpression;

  /**
   * Constructor
   * @param columnName
   * @param expr
   */
  public SqlUpdateColumnExpression(String columnName, SqlAtom expr)
  {
    assert columnName != null : "Parameter columnName must not be null";
    assert expr != null : "Parameter expr must not be null";
    fColumnName = columnName;
    fExpression = expr;
  }
  
  /**
   * Returns the columnName
   * @return the columnName
   */
  public String getColumnName()
  {
    return fColumnName;
  }
  
  /**
   * Returns the expression
   * @return the expression
   */
  public SqlAtom getExpression()
  {
    return fExpression;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return fColumnName+"="+fExpression;
  }

}
