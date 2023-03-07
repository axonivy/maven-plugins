package ch.ivyteam.db.meta.generator.internal.postgresql;

import java.io.PrintWriter;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

final class PostgreSqlDmlStatements extends DmlStatements {

  public PostgreSqlDmlStatements(DbHints dbHints, Delimiter delimiter, Identifiers identifiers) {
    super(dbHints, delimiter, identifiers);
  }

  @Override
  protected void generateNULL(PrintWriter pr, SqlArtifact artifact) {
    super.generateNULL(pr, artifact);
    if (dbHints.CAST.isSet(artifact)) {
      dbHints.CAST.generate(pr, artifact);
    }
  }

  @Override
  public void generateUpdate(PrintWriter pr, SqlUpdate updateStmt, int indent) {
    spaces.generate(pr, indent);
    pr.print("UPDATE ");
    pr.println(updateStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("SET ");
    boolean first = true;
    for (SqlUpdateColumnExpression expr : updateStmt.getColumnExpressions()) {
      if (!first) {
        pr.print(", ");
      }
      first = false;
      identifiers.generate(pr, expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, updateStmt.getFilterExpression());
  }
}
