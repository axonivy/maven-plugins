package ch.ivyteam.db.meta.generator.internal.mysql;

import java.io.PrintWriter;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

final class MySqlDmlStatements extends DmlStatements
{

  public MySqlDmlStatements(DbHints dbHints, Delimiter delimiter, Identifiers identifiers)
  {
    super(dbHints, delimiter, identifiers);
  }
  
  @Override
  public void generateUpdate(PrintWriter pr, SqlUpdate updateStmt, int indent)
  {
    spaces.generate(pr, indent);
    pr.print("UPDATE ");
    pr.println(updateStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("SET ");
    boolean first = true;
    for (SqlUpdateColumnExpression expr: updateStmt.getColumnExpressions())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    pr.print(updateStmt.getFilterExpression());
  }
}