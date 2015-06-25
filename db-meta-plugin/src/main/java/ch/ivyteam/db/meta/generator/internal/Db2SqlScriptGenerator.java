package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * Generates the sql script for Db2 database systems
 * @author rwei
 */
public abstract class Db2SqlScriptGenerator extends SqlScriptGenerator
{
  /** Database management system hint */
  public static final String DB2 = String.valueOf("Db2");
  /** The row trigger old variable name */
  private String rowTriggerOldVariableName;

  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case BIT:
        pr.append("DECIMAL(1)");
        break;
      case DATETIME:
        pr.print("TIMESTAMP");
        break;
      case NUMBER:
        pr.print("DECIMAL");
        break;        
      default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isNullBeforeDefaultConstraint()
   */
  @Override
  protected boolean isNullBeforeDefaultConstraint()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isIndexInTableSupported()
   */
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDataType(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDataType, ch.ivyteam.db.meta.model.internal.SqlArtifact)
   */
  @Override
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact)
  {
    if (!artifact.getDatabaseManagementSystemHints(DB2).isHintSet(DATA_TYPE))
    {
      super.generateDataType(pr, dataType, artifact);  
    }
    else
    {
      pr.append(artifact.getDatabaseManagementSystemHints(DB2).getHintValue(DATA_TYPE));
    }    
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddForeignKey(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlForeignKey)
   */
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
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return rowTriggerOldVariableName;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachRowDeleteTrigger(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, java.util.List, boolean)
   */
  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) throws MetaException
  {
    pr.print("CREATE TRIGGER ");
    if (isDatabaseSystemHintSet(table, DELETE_TRIGGER_NAME))
    {
      pr.println(getDatabaseSystemHintValue(table, DELETE_TRIGGER_NAME));
    }
    else
    {
      pr.print(table.getId());
      pr.println("DeleteTrigger");
    }
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.print("REFERENCING OLD AS ");
    rowTriggerOldVariableName = "Old"+getTableNameWithoutPrefix(table);
    pr.println(getRowTriggerOldVariableName());
    pr.println("FOR EACH ROW MODE DB2SQL");
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.println("END");
    pr.print("#");
  }
  
  /**
   * Gets the table name without prefix
   * @param table the table
   * @return the table name without prefix
   */
  private String getTableNameWithoutPrefix(SqlTable table)
  {
    if (table.getId().startsWith("IWA_"))
    {
      return table.getId().substring(4);
    }
    return table.getId();
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachStatementDeleteTrigger(PrintWriter, SqlTable, List)
   */
  @Override
  protected void generateForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements) throws MetaException
  {
    pr.print("CREATE TRIGGER ");
    if (isDatabaseSystemHintSet(table, DELETE_TRIGGER_NAME))
    {
      pr.println(getDatabaseSystemHintValue(table, DELETE_TRIGGER_NAME));
    }
    else
    {
      pr.print(table.getId());
      pr.println("DeleteTrigger");
    }
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("FOR EACH STATEMENT MODE DB2SQL");
    pr.println("BEGIN ATOMIC");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.println("END");
    pr.print("#");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIdentifier(java.io.PrintWriter, java.lang.String)
   */
  @Override
  protected void generateIdentifier(PrintWriter pr, String identifier)
  {
    if (isReservedSqlKeyword(identifier))
    {
      identifier = identifier.toUpperCase();
    }
    super.generateIdentifier(pr, identifier);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isReservedSqlKeyword(java.lang.String)
   */
  @Override
  protected boolean isReservedSqlKeyword(String identifier)
  {
    if (identifier.equalsIgnoreCase("State"))
    {
      return false;
    }
    return super.isReservedSqlKeyword(identifier);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAlterColumn(PrintWriter, SqlTableColumn, SqlTable, SqlTableColumn)
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException
  {
    boolean changed=false;
    if (!Objects.equals(newColumn.getDataType(), oldColumn.getDataType()))
    {
      GenerateAlterTableUtil.generateAlterTableAlterColumnType(pr, this, newColumn, newTable, "ALTER", "SET DATA TYPE");
      changed=true;
    }
    if (newColumn.isCanBeNull() != oldColumn.isCanBeNull())
    {
      if (changed)
      {
        pr.println();
      }
      GenerateAlterTableUtil.generateAlterTableAlterColumnNotNull(pr, this, newColumn, newTable, "ALTER", "SET NOT NULL", "DROP NOT NULL");
      changed=true;
    }
    
    if (!changed)
    {
      throw new IllegalArgumentException("Only changing of the data type and NOT NULL is supported");
    }
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isRecreationOfTriggerOnAlterTableNeeded()
   */
  @Override
  public boolean isRecreationOfTriggerOnAlterTableNeeded()
  {
    return true;
  }
}
