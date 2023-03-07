package ch.ivyteam.db.meta.model.internal;

/**
 * A WHEN ... THEN ... expression
 * @author rwei
 * @since 12.07.2017
 */
public class SqlComplexWhenThen {

  private SqlSimpleExpr condition;
  private SqlAtom action;

  public SqlComplexWhenThen(SqlSimpleExpr condition, SqlAtom action) {
    this.condition = condition;
    this.action = action;
  }

  public SqlSimpleExpr getCondition() {
    return condition;
  }

  public SqlAtom getAction() {
    return action;
  }

  @Override
  public String toString() {
    return "WHEN " + condition + " THEN " + action;
  }
}
