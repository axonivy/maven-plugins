package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * A CASE WHEN ... THEN ... WHEN ... THEN ... ELSE ... END expression
 * @author rwei
 * @since 12.07.2017
 */
public class SqlComplexCaseExpr extends SqlAtom {

  private final List<SqlComplexWhenThen> whenThenList;
  private final SqlAtom elseAction;

  public SqlComplexCaseExpr(List<SqlComplexWhenThen> whenThenList, SqlAtom elseAction) {
    this.whenThenList = whenThenList;
    this.elseAction = elseAction;
  }

  public List<SqlComplexWhenThen> getWhenThenList() {
    return whenThenList;
  }

  public SqlAtom getElseAction() {
    return elseAction;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CASE");
    for (SqlComplexWhenThen whenThen : whenThenList) {
      builder.append(" ");
      builder.append(whenThen);
    }
    if (elseAction != null) {
      builder.append(" ELSE ");
      builder.append(elseAction);
    }
    builder.append(" END");
    return builder.toString();
  }
}
