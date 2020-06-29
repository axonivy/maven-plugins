package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.model.internal.SqlBinaryRelation;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlLiteral;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlNull;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTrigger;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

public class Triggers
{
  /** The default row trigger old variable name */
  private static final String DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME = "OLD";

  protected final DbHints dbHints;
  protected final Delimiter delimiter;
  protected final Spaces spaces = new Spaces();
  protected final ForeignKeys foreignKeys;
  protected final DmlStatements dmlStatements;
  
  protected Triggers(DbHints dbHints, Delimiter delimiter, DmlStatements dmlStatements, ForeignKeys foreignKeys)
  {
    this.dbHints = dbHints;
    this.delimiter = delimiter;
    this.dmlStatements = dmlStatements;
    this.foreignKeys = foreignKeys;
  }
  
  public final boolean hasTrigger(SqlMeta metaDefinition, SqlTable table)
  {
    boolean hasTriggerStatements = !getForEachRowDeleteTriggerInfo(table, metaDefinition).getRight().isEmpty();
    boolean hasForEachStatementDeleteTrigger = !getForEachStatementDeleteTrigger(table, metaDefinition).isEmpty();
    return hasTriggerStatements || hasForEachStatementDeleteTrigger;
  }

  public void generateDrop(PrintWriter pr, SqlTable table)
  {
    pr.write("DROP TRIGGER ");
    triggerName(pr, table);
    delimiter.generate(pr);
    pr.println(); 
  }

  final void generateCreate(PrintWriter pr, SqlMeta metaDefinition)
  {
    createForEachStatementDeleteTriggers(pr, metaDefinition);
    createForEachRowDeleteTriggers(pr, metaDefinition);
  }

  private void createForEachStatementDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition)
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      createForEachStatementDeleteTrigger(pr, table, metaDefinition);
    }
  }

  public final void createForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition)
  {
    List<SqlDmlStatement> statements = getForEachStatementDeleteTrigger(table, metaDefinition);
    if (!statements.isEmpty())
    {
      createForEachStatementDeleteTrigger(pr, table, statements);
      pr.println();
      pr.println();
    }
  }

  private List<SqlDmlStatement> getForEachStatementDeleteTrigger(SqlTable table, SqlMeta metaDefinition)
  {
    List<SqlDmlStatement> statements = new ArrayList<>();
    for(SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : foreignTable.getTriggers())        
      {   
        if (trigger.getTableName().equals(table.getId()) && 
            dbHints.TRIGGER_EXECUTE_FOR_EACH_STATEMENT.isSet(trigger))
        {
          statements = trigger.getStatementsForEachStatement();
        }
      }
    }
    return statements;
  }

  protected void createForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements)
  {
    pr.print("CREATE TRIGGER ");
    triggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("BEGIN");
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

  protected final void triggerName(PrintWriter pr, SqlTable table)
  {
    if (dbHints.DELETE_TRIGGER_NAME.isSet(table))
    {
      dbHints.DELETE_TRIGGER_NAME.generate(pr, table);
    }
    else
    {
      pr.print(table.getId());
      pr.print("DeleteTrigger");
    }
  }
  
  private void createForEachRowDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition)
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      createForEachRowDeleteTrigger(pr, table, metaDefinition);
    }
  }

  public final void createForEachRowDeleteTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition)
  {
    Pair<Boolean, List<SqlDmlStatement>> triggerStatements = getForEachRowDeleteTriggerInfo(table, metaDefinition);
    List<SqlDmlStatement> statements = triggerStatements.getRight(); 
    if (!statements.isEmpty())
    {
      forEachRowDeleteTrigger(pr, table, statements, triggerStatements.getLeft());
      pr.println();
      pr.println();
    }
  }

  private Pair<Boolean, List<SqlDmlStatement>> getForEachRowDeleteTriggerInfo(SqlTable table, SqlMeta metaDefinition)
  {
    boolean recursiveTrigger=false;
    List<SqlDmlStatement> statements = new ArrayList<>();
    // First analyze triggers on all tables   
    for (SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      addTriggerStatements(table, foreignTable, statements);
    }
    // Second analyze foreign keys on all tables 
    for (SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      recursiveTrigger = addForeignKeyStatements(table, foreignTable, recursiveTrigger, statements);
    }

    // Third analyze the my foreign keys for ON DELETE THIS CASCADE
    addForeignKeyDeleteThisCascadeStatements(table, statements);
    return new ImmutablePair<>(recursiveTrigger, statements);
  }

  private void addTriggerStatements(SqlTable table, SqlTable foreignTable, List<SqlDmlStatement> statements)
  {
    for (SqlTrigger trigger : foreignTable.getTriggers())
    {
      if (trigger.getTableName().equals(table.getId()) &&
          !dbHints.TRIGGER_EXECUTE_FOR_EACH_STATEMENT.isSet(trigger))
      {
        statements.addAll(trigger.getStatementsForEachRow());
      }  
    }
  }

  private boolean addForeignKeyStatements(SqlTable table, SqlTable foreignTable, boolean recursiveTrigger, List<SqlDmlStatement> statements)
  {
    for (SqlForeignKey foreignKey : foreignTable.getForeignKeys())
    {
      if (shouldGenerateTriggerFor(table, foreignKey))
      {
        if (foreignKeys.getAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_CASCADE)
        {
          if (foreignTable.getId().equals(table.getId()))
          {
            recursiveTrigger = true;
          }
          statements.add(createDeleteStatement(foreignTable, foreignKey));

        }
        else if (foreignKeys.getAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_SET_NULL)
        {
          if (foreignTable.getId().equals(table.getId()))
          {
            recursiveTrigger = true;
          }              
          statements.add(createUpdateStatement(foreignTable, foreignKey));
        }
      }
    }
    return recursiveTrigger;
  }

  private SqlDelete createDeleteStatement(SqlTable foreignTable, SqlForeignKey foreignKey)
  {
    return new SqlDelete(foreignTable.getId(), 
        new SqlBinaryRelation(
            new SqlFullQualifiedColumnName(
                foreignTable.getId(),
                foreignKey.getColumnName()), 
            "=",
            new SqlFullQualifiedColumnName(
                DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, 
                foreignKey.getReference().getForeignColumn())));
  }

  private SqlUpdate createUpdateStatement(SqlTable foreignTable, SqlForeignKey foreignKey)
  {
    return new SqlUpdate(
        foreignTable.getId(),
        Arrays.asList(
            new SqlUpdateColumnExpression(foreignKey.getColumnName(), new SqlLiteral(SqlNull.getInstance()))),
        new SqlBinaryRelation(
            new SqlFullQualifiedColumnName(
                foreignTable.getId(),
                foreignKey.getColumnName()), 
            "=",
            new SqlFullQualifiedColumnName(
                DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, 
                foreignKey.getReference().getForeignColumn())), null, null);
  }

  private boolean shouldGenerateTriggerFor(SqlTable table, SqlForeignKey foreignKey)
  {
    return foreignKey.getReference().getForeignTable().equals(table.getId()) &&
           foreignKeys.shouldGenerateTrigger(foreignKey);
  }

  private void addForeignKeyDeleteThisCascadeStatements(SqlTable table, List<SqlDmlStatement> statements)
  {
    for (SqlForeignKey foreignKey : table.getForeignKeys())
    {
      if ((!dbHints.NO_REFERENCE.isSet(foreignKey))&&
          (foreignKeys.getAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_THIS_CASCADE))
      {
        statements.add(new SqlDelete(foreignKey.getReference().getForeignTable(),
                new SqlBinaryRelation(new SqlFullQualifiedColumnName(foreignKey.getReference()
                        .getForeignTable(), foreignKey.getReference().getForeignColumn()), "=",
                        new SqlFullQualifiedColumnName(DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, foreignKey
                                .getColumnName()))));
      }
    }
  }
  
  protected void forEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, @SuppressWarnings("unused") boolean recursiveTrigger)
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
    pr.print("END");
    delimiter.generate(pr);
  }
  
  protected final void generateDmlStatement(PrintWriter pr, SqlDmlStatement stmt, int indent)
  {
    stmt = new ReplaceOldTriggerVariable(this).replace(stmt);
    dmlStatements.generate(pr, stmt, indent);
  }

  /**
   * Gets the name of the OLD table name in row triggers that can be used to reference the values of the
   * deleted row
   * @return OLD table name
   */
  protected String getRowTriggerOldVariableName()
  {
    return ":old";
  }

  final boolean isDefaultRowTriggerOldVariableName(String variableName)
  {
    return DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME.equals(variableName);
  }
}
