package ch.ivyteam.db.meta.generator.internal.mssql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class MsSqlTriggers extends Triggers
{
  MsSqlTriggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys)
  {
    super(dbHints, delimiter, dmlStatements, foreignKeys);
  }

  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "deleted";
  }

  @Override
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger)
  {
    pr.print("CREATE TRIGGER ");
    triggerName(pr, table);
    pr.println();
    pr.print("  ON ");
    pr.print(table.getId());
    pr.println(" FOR DELETE AS");
    if (recursiveTrigger)
    {
      pr.println("  IF (@@ROWCOUNT > 0)");
      pr.println("  BEGIN");
    }
    pr.println();
    
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, recursiveTrigger?4:2);
      pr.println();
      pr.println();
    }        
    if (recursiveTrigger)
    {
      spaces.generate(pr, 2);
      pr.print("  END");
    }
    delimiter.generate(pr);
  }
}