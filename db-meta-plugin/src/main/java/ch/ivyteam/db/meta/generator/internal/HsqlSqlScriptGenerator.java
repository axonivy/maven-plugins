package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlTrigger;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for HSQL database systems
 * @author rwei
 */
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
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case CLOB:
        pr.append("LONGVARCHAR");
        break;
      case BLOB:
        pr.append("VARBINARY");
        break;
     default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
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
  public boolean isForeignKeyReferenceInColumnDefinitionSupported()
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
  protected void generateSqlCaseExpression(PrintWriter pr, SqlCaseExpr caseExpr)
  {
    pr.print("CASEWHEN(");
    pr.print(caseExpr.getColumnName());
    pr.print(", ");
    pr.print(caseExpr.getWhenThenList().get(0).getColumnName());
    pr.print(", ");
    pr.print(caseExpr.getWhenThenList().get(1).getColumnName());
    pr.print(")");
  }

  @Override
  protected void generateForEachStatementDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition)
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : table.getTriggers())
      {
        pr.print("CREATE TRIGGER ");
        SqlTable triggerTable = metaDefinition.findTable(trigger.getTableName());
        generateTriggerName(pr, triggerTable);
        pr.println();
        pr.print("AFTER DELETE ON ");
        pr.print(trigger.getTableName());
        pr.println(" QUEUE 0");
        pr.print("CALL \"");
        pr.print(trigger.getDatabaseManagementSystemHints(HSQL_DB).getHintValue(TRIGGER_CLASS));
        pr.print("\"");
        generateDelimiter(pr);
        pr.println();
        pr.println();
      }
    }
  }

  @Override
  protected void generateForEachRowDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition) throws MetaException
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreignKey : table.getForeignKeys())
      {
        if ((!isDatabaseSystemHintSet(foreignKey, NO_REFERENCE))&&(getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_THIS_CASCADE))
        {
          List<SqlTable> tables = new ArrayList<>();
          tables.add(table);
          if (isDatabaseSystemHintSet(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES))
          {
            for (String tableName : getDatabaseSystemHintValue(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES).split(","))
            {
              tables.add(metaDefinition.findTable(tableName.trim()));
            }
          }
          for (SqlTable triggerTable : tables)
          {
            pr.print("CREATE TRIGGER ");
            if (isDatabaseSystemHintSet(foreignKey, TRIGGER_NAME_POST_FIX))
            {
              pr.print(triggerTable.getId());
              generateDatabaseManagementHintValue(pr, foreignKey, TRIGGER_NAME_POST_FIX);
              pr.println("DeleteTrigger");
            }
            else              
            {
              generateTriggerName(pr, triggerTable);
            }
            pr.print("AFTER DELETE ON ");
            pr.print(triggerTable.getId());
            pr.println(" QUEUE 0");
            pr.print("CALL \"");
            generateDatabaseManagementHintValue(pr, foreignKey, TRIGGER_CLASS);
            pr.print("\"");
            generateDelimiter(pr);
            pr.println();
            pr.println();
          }
        }
      }
    }
  }
  
  @Override
  public void generateDropTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinitionFrom)
  {
    boolean dropped = dropForEachRowDeleteTriggers(pr, table, metaDefinitionFrom);
    if (!dropped)
    {
      super.generateDropTrigger(pr, table, metaDefinitionFrom);
    }
  }

  private boolean dropForEachRowDeleteTriggers(PrintWriter pr, SqlTable table, SqlMeta metaDefinitionFrom)
  {
    boolean dropped = false;
    for (SqlForeignKey foreignKey : table.getForeignKeys())
    {
      if ((!isDatabaseSystemHintSet(foreignKey, NO_REFERENCE))&&(getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_THIS_CASCADE))
      {
        List<SqlTable> tables = new ArrayList<>();
        tables.add(table);
        if (isDatabaseSystemHintSet(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES))
        {
          for (String tableName : getDatabaseSystemHintValue(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES).split(","))
          {
            tables.add(metaDefinitionFrom.findTable(tableName.trim()));
          }
        }
        for (SqlTable triggerTable : tables)
        {
          pr.print("DROP TRIGGER ");
          if (isDatabaseSystemHintSet(foreignKey, TRIGGER_NAME_POST_FIX))
          {
            pr.print(triggerTable.getId());
            generateDatabaseManagementHintValue(pr, foreignKey, TRIGGER_NAME_POST_FIX);
            pr.print("DeleteTrigger");
          }
          else              
          {
            generateTriggerName(pr, triggerTable);
          }
          generateDelimiter(pr);
          pr.println(); 
        }
        dropped = true;
      }
    }
    return dropped;
  }
  
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(HSQL_DB);
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
    return ":old";
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
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("CALL \"ch.ivyteam.ivy.persistence.db.hsqldb.HsqlStoredProcedure.dropUniqueConstraints\"('"+table.getId()+"')");
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("CALL \"ch.ivyteam.ivy.persistence.db.hsqldb.HsqlStoredProcedure.dropForeignKey\"('"+table.getId()+"', '"+foreignKey.getColumnName()+"')");
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
}