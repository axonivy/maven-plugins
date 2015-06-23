package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * An sql select definition
 * @author rwei
 * @since 25.07.2012
 */
public class SqlSelect
{
  /** The select columns expressions */
  private List<SqlSelectExpression> fExpressions;
  /** the tables the view gather data from */
  private List<String> fTables;
  /** The condition of the view */
  private SqlSimpleExpr fCondition;
  
   
  /**
   * @param expressions
   * @param tables
   * @param condition
   */
  public SqlSelect(List<SqlSelectExpression> expressions, List<String> tables, SqlSimpleExpr condition)
  {
    super();
    this.fExpressions = expressions;
    this.fTables = tables;
    this.fCondition = condition;
  }


  /**
   * @return -
   */
  public List<SqlSelectExpression> getExpressions()
  {
    return fExpressions;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (SqlSelectExpression expression : fExpressions)
    {
      if (!first)
      {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(expression);
    }
    builder.append("\n");
    builder.append("FROM ");
    SqlScriptUtil.formatCommaSeparated(builder, fTables);
    builder.append("\nWHERE ");
    builder.append(fCondition);
    return builder.toString();
  }


  /**
   * @return -
   */
  public List<String> getTables()
  {
    return fTables;
  }


  /**
   * @return -
   */
  public SqlSimpleExpr getCondition()
  {
    return fCondition;
  }
}
