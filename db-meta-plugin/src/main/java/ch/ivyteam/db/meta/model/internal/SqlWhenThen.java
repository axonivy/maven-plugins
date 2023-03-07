package ch.ivyteam.db.meta.model.internal;

/**
 * expression WHEN ... THEN ...
 * @author rwei
 * @since 02.10.2009
 */
public class SqlWhenThen {

  /** Literal to test in the WHEN part */
  private Object fLiteral;
  /** column to use in the THEN part */
  private SqlFullQualifiedColumnName fColumnName;

  /**
   * Constructor
   * @param literal
   * @param columnName
   */
  public SqlWhenThen(Object literal, SqlFullQualifiedColumnName columnName) {
    assert literal != null : "Parameter literal must not be null";
    fLiteral = literal;
    assert columnName != null : "Parameter columnName must not be null";
    fColumnName = columnName;
  }

  /**
   * Returns the literal
   * @return the literal
   */
  public Object getLiteral() {
    return fLiteral;
  }

  /**
   * Returns the columnName
   * @return the columnName
   */
  public SqlFullQualifiedColumnName getColumnName() {
    return fColumnName;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("WHEN ");
    builder.append(fLiteral);
    builder.append(" THEN ");
    builder.append(fColumnName);
    return builder.toString();
  }
}
