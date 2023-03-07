package ch.ivyteam.db.meta.generator.internal.oracle;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class OracleTriggers extends Triggers {

  OracleTriggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys) {
    super(dbHints, delimiter, dmlStatements, foreignKeys);
  }

  @Override
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) {
    super.forEachRowDeleteTrigger(pr, table, triggerStatements, recursiveTrigger);
    pr.println();
    delimiter.generate(pr);
    pr.println();
  }

  @Override
  protected void createForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements) {
    super.createForEachStatementDeleteTrigger(pr, table, triggerStatements);
    pr.println();
    delimiter.generate(pr);
    pr.println();
  }

  @Override
  protected String getRowTriggerOldVariableName() {
    return ":old";
  }
}
