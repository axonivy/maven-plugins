package ch.ivyteam.db.meta.model.internal;

/**
 * SQL expression in parentheses. E.g. '(' + Expression + ')'
 * @author rwei
 * @since 02.10.2009
 */
public class SqlParent extends SqlSimpleExpr
{
  /** The expression in parentheses */
  private SqlSimpleExpr fExpression;

  /**
   * Constructor
   * @param expr
   */
  public SqlParent(SqlSimpleExpr expr)
  {
    assert expr != null : "Parameter expr must not be null";
    fExpression = expr;
  }
  
  /**
   * Returns the expression
   * @return the expression
   */
  public SqlSimpleExpr getExpression()
  {
    return fExpression;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return "("+fExpression.toString()+")";
  }

}
