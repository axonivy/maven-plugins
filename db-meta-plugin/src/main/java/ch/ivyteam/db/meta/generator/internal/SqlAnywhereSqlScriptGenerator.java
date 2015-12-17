package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for Sql Anywhere database systems
 * @author rwei
 */
public class SqlAnywhereSqlScriptGenerator extends SqlScriptGenerator
{
  /** The Sql Anywhere system database */ 
  public static final String SQL_ANYWHERE = String.valueOf("SqlAnywhere");
  
  private static final String DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey_Constraint";

  
  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case DATETIME:
        pr.append("TIMESTAMP");
        break;
      case CLOB:
        pr.append("TEXT");
        break;
      case BLOB:
        pr.append("IMAGE");
        break;
      case NUMBER:
        pr.append("DECIMAL");
        break;
      default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "Sybase SQL Anywhere";
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(SQL_ANYWHERE);
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateNullConstraint(java.io.PrintWriter, boolean, ch.ivyteam.db.meta.model.internal.SqlTableColumn)
   */
  @Override
  protected void generateNullConstraint(PrintWriter pr, boolean canBeNull, SqlTableColumn column)
  {
    if (canBeNull)
    {
      pr.print(" NULL");
    }
    else
    {
      super.generateNullConstraint(pr, canBeNull, column);
    }
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "old";
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
    pr.println("REFERENCING OLD AS old");
    pr.println("FOR EACH ROW");    
    pr.println("BEGIN");
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
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isReservedSqlKeyword(java.lang.String)
   */
  @Override
  protected boolean isReservedSqlKeyword(String identifier)
  {
    return "Message".equalsIgnoreCase(identifier)||
           super.isReservedSqlKeyword(identifier);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAlterColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlTableColumn)
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable,
          SqlTableColumn oldColumn) throws MetaException
  {
    GenerateAlterTableUtil.generateAlterTableAlterColumnWithNullConstraints(pr, this, newColumn, newTable, "MODIFY");
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePrefix(java.io.PrintWriter)
   */
  @Override
  protected void generatePrefix(PrintWriter pr)
  {
    generateAlterDatabaseSnapshotIsolation(pr);
    pr.println();
  }

  /**
   * @param pr
   */
  private void generateAlterDatabaseSnapshotIsolation(PrintWriter pr)
  {
    generateCommentLine(pr, "");
    generateCommentLine(pr, "Alter database so that read operations are not blocked by write operations.");
    generateCommentLine(pr, "");
    pr.print("SET OPTION PUBLIC.allow_snapshot_isolation = 'On'");
    generateDelimiter(pr);
    pr.println();
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateNonMetaDiffChangesPost(java.io.PrintWriter, int)
   */
  @Override
  public void generateNonMetaDiffChangesPost(PrintWriter pr, int newVersionId)
  {
    super.generateNonMetaDiffChangesPost(pr, newVersionId);
    if (newVersionId == 29)
    {
      generateAlterDatabaseSnapshotIsolation(pr);
    }
  }
  
  @Override
  public void generateNonMetaDiffChangesPre(PrintWriter pr, int newVersionId)
  {
    super.generateNonMetaDiffChangesPre(pr, newVersionId);
    if (newVersionId == 33)
    {
      pr.print("IF EXISTS (SELECT 1 FROM dbo.sysindexes WHERE id = object_id('IWA_GlobalVariable') ");
      pr.println("AND name ='IWA_GlobalVariable_AppIdEnvIdNameIdx')");
      pr.println("BEGIN");
      pr.println("  ALTER INDEX IWA_GlobalVariable_AppIdEnvIdNameIdx ON IWA_GlobalVariable RENAME TO IWA_GlobalVariable_AppIdEnvIdNameIndex");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }
  
  @Override
  protected void generateDropUniqueIndex(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique,
          List<String> createdTemporaryStoredProcedures)
  {
    pr.print("DROP INDEX ");
    generateIdentifier(pr, table.getId());
    pr.print(".");
    generateIdentifier(pr, getUniqueConstraintName(unique));
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("(");
    pr.print("CURRENT USER"); 
    pr.print(", '");
    pr.print(table.getId()); 
    pr.print("', '");
    pr.print(foreignKey.getColumnName());
    pr.print("')");    
    generateDelimiter(pr);
    pr.println(); 
  }

  private void generateDropForeignKeyConstraintStoredProcedure(PrintWriter pr,
          List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      
      generateCommentLine(pr, "Store Procedure to drop a foreign key constraint");

      pr.print("CREATE PROCEDURE ");
      pr.print(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      pr.println(" (IN fk_schema VARCHAR(64), IN fk_table VARCHAR(64), IN fk_column VARCHAR(64))");
      pr.println("BEGIN");
      pr.println("  DECLARE err_notfound");        
      pr.println("  EXCEPTION FOR SQLSTATE '02000';");
      pr.println("  DECLARE sql LONG VARCHAR;");
      pr.println("  DECLARE fk_index CHAR(64);");
      pr.println("  DECLARE foreignKeyCursor CURSOR FOR");
      pr.println("     SELECT iname");
      pr.println("     FROM sys.sysindexes"); 
      pr.println("     WHERE indextype='Foreign Key' AND"); 
      pr.println("     icreator=fk_schema AND ");
      pr.println("     tname=fk_table AND ");
      pr.println("     colnames LIKE fk_column || ' %';");
      pr.println("   OPEN foreignKeyCursor;");
      pr.println("   IndexLoop:");
      pr.println("   LOOP");
      pr.println("      FETCH NEXT foreignKeyCursor");
      pr.println("      INTO fk_index;");
      pr.println("      IF SQLSTATE = err_notfound THEN");
      pr.println("         LEAVE IndexLoop;");
      pr.println("      END IF;");
      pr.println("      SET sql = 'ALTER TABLE ' || fk_table || ' DROP FOREIGN KEY ' || fk_index;");
      pr.println("      EXECUTE IMMEDIATE sql;");
      pr.println("   END LOOP IndexLoop;");
      pr.println("   CLOSE foreignKeyCursor;");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }
  
  @Override
  public void generateDropPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey,
          List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE "+table.getId()+" DROP PRIMARY KEY");
    generateDelimiter(pr);
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    options.primaryKeysOnAlterTable = true;
    options.indexesOnAlterTable = true;
    options.uniqueConstraintsOnAlterTable = true;
    return options;
  }
  
}