package ch.ivyteam.db.meta.model.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An sql select definition
 * @author rwei
 * @since 25.07.2012
 */
public class SqlSelect
{
  /** The select columns expressions */
  private List<SqlSelectExpression> expressions;
  /** the tables the view gather data from */
  private List<SqlJoinTable> joinTables;
  /** The condition of the view */
  private SqlSimpleExpr condition;
   
  /**
   * @param expressions
   * @param joinTables
   * @param condition
   */
  public SqlSelect(List<SqlSelectExpression> expressions, List<SqlJoinTable> joinTables, SqlSimpleExpr condition)
  {
    super();
    this.expressions = expressions;
    this.joinTables = joinTables;
    this.condition = condition;
  }

  public List<SqlSelectExpression> getExpressions()
  {
    return expressions;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    builder.append("SELECT\n");
    for (SqlSelectExpression expression : expressions)
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
    first = true;
    for (SqlJoinTable joinTable : joinTables)
    {
      if (!first)
      {
        if (joinTable.getJoinKind() == null)
        {
          builder.append(",");
        }
        builder.append(" ");
      }
      first = false;
      builder.append(joinTable);
    }
    if (condition != null)
    {
      builder.append("\nWHERE ");
      builder.append(condition);
    }
    return builder.toString();
  }

  public List<SqlJoinTable> getJoinTables()
  {
    return joinTables;
  }

  public Map<String, String> getTableAliases()
  {
    Map<String, String> aliases = joinTables
        .stream()
        .map(joinTable -> joinTable.getTable())
        .filter(table-> table.getAlias() != null)
        .collect(Collectors.toMap(SqlTableId::getAlias, SqlTableId::getName));
    return aliases;
  }

  public SqlSimpleExpr getCondition()
  {
    return condition;
  }
}
