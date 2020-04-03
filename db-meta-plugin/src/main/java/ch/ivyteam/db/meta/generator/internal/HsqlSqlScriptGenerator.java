package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

public class HsqlSqlScriptGenerator extends SqlScriptGenerator
{
 /** 
   * Database System Hint addition triggers for tables: 
   * Specifies other tables for wich also trigger with the same java trigger class
   * should be generated
   */
  public static final String ADDITIONAL_TRIGGERS_FOR_TABLES = String.valueOf("AdditionalTriggersForTables");
  /**
   *  Database System Hint trigger name post fix:
   *  Adds the given post fix to the name of the trigger
   */
  public static final String TRIGGER_NAME_POST_FIX = String.valueOf("TriggerNamePostFix");
  /** Database System */
  public static final String HSQL_DB = String.valueOf("HsqlDb");
  /** 
   * Database System Hint Trigger Class: 
   * Specifies the java trigger class used in hsql triggers
   */
  public static final String TRIGGER_CLASS = String.valueOf("TriggerClass");

  
  @Override
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact)
  {
    if (artifact.getDatabaseManagementSystemHints(HSQL_DB).isHintSet(DATA_TYPE))
    {
      pr.print(artifact.getDatabaseManagementSystemHints(HSQL_DB).getHintValue(DATA_TYPE));
    }
    else
    {
      super.generateDataType(pr, dataType, artifact);
    }
  }

  @Override
  protected String getDatabaseComment()
  {
    return "HsqlDb";
  }
  
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  @Override
  protected boolean isNullBeforeDefaultConstraint()
  {
    return false;
  }
  
  @Override
  public void generateAlterTableAddForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey) throws MetaException
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" ADD FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }

  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(HSQL_DB);
  }
  
  @Override
  protected void generateForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements)
  {
    pr.print("CREATE TRIGGER ");
    generateTriggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.print("END");
    generateDelimiter(pr);   
  }
  
  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table, List<SqlDmlStatement> triggerStatements, @SuppressWarnings("unused") boolean recursiveTrigger)
  {
    pr.print("CREATE TRIGGER ");
    generateTriggerName(pr, table);
    pr.println();
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("REFERENCING OLD as " + getRowTriggerOldVariableName());
    pr.println("FOR EACH ROW");    
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.print("END");
    generateDelimiter(pr);
  }
  
  @Override
  protected void generateIdentifier(PrintWriter pr, String identifier)
  {
    if (isReservedSqlKeyword(identifier))
    {
      identifier = identifier.toUpperCase();
    }
    super.generateIdentifier(pr, identifier);
  }

  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "oldrow";
  }
  
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn)
  {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn, newTable, "ALTER COLUMN");
  }

  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
}