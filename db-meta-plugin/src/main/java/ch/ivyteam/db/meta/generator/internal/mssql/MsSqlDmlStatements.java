package ch.ivyteam.db.meta.generator.internal.mssql;

import java.io.PrintWriter;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlFunction;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

final class MsSqlDmlStatements extends DmlStatements {

  public MsSqlDmlStatements(DbHints dbHints, Delimiter delimiter, Identifiers identifiers) {
    super(dbHints, delimiter, identifiers);
  }

  @Override
  protected void generateDelete(PrintWriter pr, SqlDelete deleteStmt, int indent) throws MetaException {
    spaces.generate(pr, indent);
    pr.print("DELETE ");
    pr.print(deleteStmt.getTable());
    pr.print(" FROM ");
    pr.print(deleteStmt.getTable());
    pr.println(", deleted");
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }

  @Override
  public void generateUpdate(PrintWriter pr, SqlUpdate updateStmt, int indent) {
    spaces.generate(pr, indent);
    pr.print("UPDATE ");
    pr.print(updateStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("SET ");
    boolean first = true;
    for (SqlUpdateColumnExpression expr : updateStmt.getColumnExpressions()) {
      if (!first) {
        pr.print(", ");
      }
      first = false;
      pr.print(updateStmt.getTable());
      pr.print('.');
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    spaces.generate(pr, indent);
    pr.print("FROM ");
    pr.print(updateStmt.getTable());
    pr.println(", deleted");
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, updateStmt.getFilterExpression());
  }

  @Override
  protected SqlFunction convertFunction(SqlFunction function) {
    if ("LENGTH".equalsIgnoreCase(function.getName())) {
      return new SqlFunction("LEN", function.getArguments());
    }
    return function;
  }
}
