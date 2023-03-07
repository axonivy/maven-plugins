package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * A CASE ... WHEN ... THEN ... WHEN ... THEN ... END expression
 * @author rwei
 * @since 02.10.2009
 */
public class SqlCaseExpr extends SqlAtom {

  /** The column to use in the CASE part */
  private SqlFullQualifiedColumnName fColumnName;
  /** WHEN THEN pairs */
  private List<SqlWhenThen> fWhenThenList;

  /**
   * Constructor
   * @param columnName
   * @param whenThenList
   */
  public SqlCaseExpr(SqlFullQualifiedColumnName columnName, List<SqlWhenThen> whenThenList) {
    assert columnName != null : "Parameter columnName must not be null";
    assert whenThenList != null : "Parameter whenThenList must not be null";
    fColumnName = columnName;
    fWhenThenList = whenThenList;
  }

  /**
   * Returns the columnName
   * @return the columnName
   */
  public SqlFullQualifiedColumnName getColumnName() {
    return fColumnName;
  }

  /**
   * Returns the whenThenList
   * @return the whenThenList
   */
  public List<SqlWhenThen> getWhenThenList() {
    return fWhenThenList;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CASE ");
    builder.append(fColumnName);
    for (SqlWhenThen whenThen : fWhenThenList) {
      builder.append(" ");
      builder.append(whenThen);
    }
    builder.append(" END");
    return builder.toString();
  }
}
