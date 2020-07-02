package ch.ivyteam.db.meta.generator.internal.mysql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class MySqlTriggers extends Triggers
{
  MySqlTriggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys)
  {
    super(dbHints, delimiter, dmlStatements, foreignKeys);
  }

  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "OLD";
  }

  @Override
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger)
  {
    pr.print("CREATE TRIGGER ");
    triggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("FOR EACH ROW");
    pr.println("BEGIN");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      delimiter.generate(pr);
      pr.println();
      pr.println();
    }
    pr.println("END");
    delimiter.generate(pr);
  }
}