package ch.ivyteam.db.meta.generator.internal.hsql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class HsqlTriggers extends Triggers
{
  HsqlTriggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys)
  {
    super(dbHints, delimiter, dmlStatements, foreignKeys);
  }

  @Override
  protected void createForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements)
  {
    pr.print("CREATE TRIGGER ");
    triggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      delimiter.generate(pr);
      pr.println();
      pr.println();
    }
    pr.print("END");
    delimiter.generate(pr);   
  }
  
  @Override
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements, @SuppressWarnings("unused") boolean recursiveTrigger)
  {
    pr.print("CREATE TRIGGER ");
    triggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("REFERENCING OLD as " + getRowTriggerOldVariableName());
    pr.println("FOR EACH ROW");    
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      delimiter.generate(pr);
      pr.println();
      pr.println();
    }
    pr.print("END");
    delimiter.generate(pr);
  }

  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "oldrow";
  }
}