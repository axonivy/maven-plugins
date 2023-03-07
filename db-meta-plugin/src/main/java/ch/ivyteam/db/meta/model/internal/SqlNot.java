package ch.ivyteam.db.meta.model.internal;

/**
 * NOT expr
 * @author rwei
 * @since 02.10.2009
 */
public class SqlNot extends SqlSimpleExpr {

  /** The expression */
  private SqlSimpleExpr fExpression;

  /**
   * Constructor
   * @param expr
   */
  public SqlNot(SqlSimpleExpr expr) {
    assert expr != null : "Parameter expr must not be null";
    fExpression = expr;
  }

  /**
   * Returns the expression
   * @return the expression
   */
  public SqlSimpleExpr getExpression() {
    return fExpression;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NOT " + super.toString();
  }
}
