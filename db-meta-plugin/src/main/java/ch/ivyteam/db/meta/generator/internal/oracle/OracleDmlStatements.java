package ch.ivyteam.db.meta.generator.internal.oracle;

import java.io.PrintWriter;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlFunction;

final class OracleDmlStatements extends DmlStatements {

  public OracleDmlStatements(DbHints dbHints, Delimiter delimiter, Identifiers identifiers) {
    super(dbHints, delimiter, identifiers);
  }

  @Override
  protected void generateDelete(PrintWriter pr, SqlDelete deleteStmt, int indent) {
    spaces.generate(pr, indent);
    pr.print("DELETE ");
    pr.println(deleteStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }

  @Override
  protected SqlFunction convertFunction(SqlFunction function) {
    if ("LENGTH".equals(function.getName()) &&
            function.getArguments().size() == 1 &&
            function.getArguments().get(0) instanceof SqlFullQualifiedColumnName) {
      // A better implementation would be to check if the column is varchar and
      // nullable!
      // in this case our oracle implementation converts "" -> " " and we have
      // to use TRIM so that LENGTH(...) > 0 is correct
      return new SqlFunction(function.getName(), new SqlFunction("TRIM", function.getArguments()));
    }
    return super.convertFunction(function);
  }
}
