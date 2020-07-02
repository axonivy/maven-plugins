package ch.ivyteam.db.meta.generator.internal.postgresql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class PostgreSqlTriggers extends Triggers
{
  PostgreSqlTriggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys)
  {
    super(dbHints, delimiter, dmlStatements, foreignKeys);
  }
  
  @Override
  public void generateDrop(PrintWriter pr, SqlTable table)
  {
    pr.write("DROP TRIGGER ");
    triggerName(pr, table);
    pr.append(" ON ");
    pr.append(table.getId());
    delimiter.generate(pr);
    pr.println(); 
  }


  @Override
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger)
  {
    String functionName = table.getId() + "DeleteTriggerFunc()";

    // Drop already existing functions (used for regeneration)
    pr.print("DROP FUNCTION IF EXISTS ");
    pr.print(functionName);
    delimiter.generate(pr);
    pr.println();
    pr.println();

    // create function
    pr.print("CREATE FUNCTION ");
    pr.print(functionName);
    pr.println(" RETURNS TRIGGER AS '");
    spaces.generate(pr, 2);
    pr.println("BEGIN");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 4);
      delimiter.generate(pr);
      pr.println();
      pr.println();      
    }
    spaces.generate(pr, 4);
    pr.print("RETURN OLD");
    delimiter.generate(pr);
    pr.println();
    spaces.generate(pr, 2);
    pr.print("END");
    delimiter.generate(pr);
    pr.println();
    pr.print("' LANGUAGE plpgsql");
    delimiter.generate(pr);
    pr.println();
    pr.println();

    // create trigger that uses function
    pr.print("CREATE TRIGGER ");
    pr.print(table.getId());
    pr.println("DeleteTrigger AFTER DELETE");
    pr.print("ON ");
    pr.print(table.getId());
    pr.println(" FOR EACH ROW");
    pr.print("EXECUTE PROCEDURE ");
    pr.print(table.getId());
    pr.print("DeleteTriggerFunc()");
    delimiter.generate(pr);
  }
  
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "OLD";
  }
}