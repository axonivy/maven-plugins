package ch.ivyteam.db.meta.model.internal;

/**
 * SQL logical expression like expr AND expr or expr OR expr
 * @author rwei
 * @since 02.10.2009
 */
public class SqlLogicalExpression extends SqlSimpleExpr {

  /** First argument */
  private SqlSimpleExpr fFirst;
  /** Operator */
  private String fOperator;
  /** Second argument */
  private SqlSimpleExpr fSecond;

  /**
   * Constructor
   * @param first
   * @param operator
   * @param second
   */
  public SqlLogicalExpression(SqlSimpleExpr first, String operator, SqlSimpleExpr second) {
    assert first != null : "Parameter first must not be null";
    assert operator != null : "Parameter operator must not be null";
    assert second != null : "Parameter second must not be null";
    fFirst = first;
    fOperator = operator;
    fSecond = second;
  }

  /**
   * Returns the first
   * @return the first
   */
  public SqlSimpleExpr getFirst() {
    return fFirst;
  }

  /**
   * Returns the second
   * @return the second
   */
  public SqlSimpleExpr getSecond() {
    return fSecond;
  }

  /**
   * Returns the operator
   * @return the operator
   */
  public String getOperator() {
    return fOperator;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return fFirst.toString() + " " + fOperator + " " + fSecond.toString();
  }
}
