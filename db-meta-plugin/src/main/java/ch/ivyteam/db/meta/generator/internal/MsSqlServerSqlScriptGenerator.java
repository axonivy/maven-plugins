package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;
import ch.ivyteam.util.collections.CollectionsUtil;

/**
 * Generates the sql script for Microsoft SQL Server database systems
 * @author rwei
 */
public class MsSqlServerSqlScriptGenerator extends SqlScriptGenerator
{
  private static final String DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE  = "IWA_Drop_Unique"; 
  private static final String DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_PrimaryKey";
  private static final String DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey";
  
  /** The database system */
  public static final String MS_SQL_SERVER = String.valueOf("MsSqlServer");
  private List<String> dropUniqueForTables = new ArrayList<String>();  
  
  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case CLOB:
        pr.append("TEXT");
        break;
      case BLOB:
        pr.append("IMAGE");
        break;
      case DATE: // not supported by sql server 2005, but by 2008
      case TIME: // not supported by sql server 2005, but by 2008
        pr.append("DATETIME");
        break;
       default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  /**
   * @see SqlScriptGenerator#generateDelimiter(PrintWriter)
   */
  @Override
  protected void generateDelimiter(PrintWriter pr)
  {
    pr.println();
    pr.print("GO");
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
    return "deleted";
  }
 
  /**
   * @throws MetaException 
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
      writeSpaces(pr, 2);
      pr.print("  END");
    }
    generateDelimiter(pr);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDeleteStatement(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDelete, int)
   */
  @Override
  protected void generateDeleteStatement(PrintWriter pr, SqlDelete deleteStmt, int insets)
          throws MetaException
  {
    writeSpaces(pr, insets);
    pr.print("DELETE ");
    pr.print(deleteStmt.getTable());
    pr.print(" FROM ");
    pr.print(deleteStmt.getTable());
    pr.println(", deleted");
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateUpdateStatement(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlUpdate, int)
   */
  @Override
  protected void generateUpdateStatement(PrintWriter pr, SqlUpdate updateStmt, int insets)
          throws MetaException
  {
    boolean first = true;
    writeSpaces(pr, insets);
    pr.print("UPDATE ");
    pr.print(updateStmt.getTable());
    writeSpaces(pr, insets);
    pr.print("SET ");
    first = true;
    for (SqlUpdateColumnExpression expr: updateStmt.getColumnExpressions())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      pr.print(updateStmt.getTable());
      pr.print('.');
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    writeSpaces(pr, insets);
    pr.print("FROM ");
    pr.print(updateStmt.getTable());
    pr.println(", deleted");
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    generateFilterExpression(pr, updateStmt.getFilterExpression());  }
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "Microsoft SQL Server";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePrefix(java.io.PrintWriter)
   */
  @Override
  protected void generatePrefix(PrintWriter pr)
  {
    if (fOutputFile.getName().indexOf("Base")<0)
    {
      pr.print("COMMIT");
      generateDelimiter(pr);
      pr.println();
      pr.println();
      pr.print("SET IMPLICIT_TRANSACTIONS OFF");
      generateDelimiter(pr);
      pr.println();
      pr.println();
      pr.print("ALTER DATABASE [{0}] COLLATE Latin1_General_CI_AI");
      generateDelimiter(pr);
      pr.println();
      pr.println();
      generateAlterDatabaseForSnapshotIsolation(pr);
      pr.println();
      pr.println();
      pr.print("SET IMPLICIT_TRANSACTIONS ON");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }

  /**
   * @param pr
   */
  private void generateAlterDatabaseForSnapshotIsolation(PrintWriter pr)
  {
    generateCommentLine(pr, "");
    generateCommentLine(pr, "Alter database so that read operations are not blocked by write operations.");
    generateCommentLine(pr, "");
    pr.print("ALTER DATABASE [{0}] SET ALLOW_SNAPSHOT_ISOLATION ON");
    generateDelimiter(pr);
    pr.println();
    pr.println();
    pr.print("ALTER DATABASE [{0}] SET READ_COMMITTED_SNAPSHOT ON");
    generateDelimiter(pr);
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
      pr.print("COMMIT TRANSACTION");
      generateDelimiter(pr);
      pr.println(); 
      generateAlterDatabaseForSnapshotIsolation(pr);
      pr.println();
    }
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return CollectionsUtil.listify(MS_SQL_SERVER);
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAlterColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlTableColumn)
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable,
          SqlTableColumn oldColumn) throws MetaException
  {
    GenerateAlterTableUtil.generateAlterTableAlterColumnWithNullConstraints(pr, this, newColumn, newTable, "ALTER COLUMN");
  }  
  
  @Override
  protected void generateNullConstraint(PrintWriter pr, boolean canBeNull, SqlTableColumn column)
  {
    if (!canBeNull)
    {
      pr.append(" NOT NULL");
    }
    else
    {
      pr.append(" NULL");
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    generateDropUniqueConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    
    String tableName = table.getId();
    if (!dropUniqueForTables.contains(tableName))
    {
      pr.print("EXECUTE ");
      pr.print(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      pr.print(" '");
      pr.print(tableName);
      pr.print("'");
      dropUniqueForTables.add(tableName);
    }
    else
    {
      pr.print("select null");
    }
  }

  private void generateDropUniqueConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      
      generateCommentLine(pr, "Store Procedure to drop a unique constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      pr.println("@tablename varchar(100)");
      pr.println("AS");
      pr.println("DECLARE @parentid integer");
      pr.println("DECLARE @uqname varchar(100)");
      pr.println("DECLARE table_cursor CURSOR FOR SELECT id FROM sysobjects WHERE name = @tablename");
      pr.println("OPEN  table_cursor");
      pr.println("FETCH NEXT FROM table_cursor INTO @parentid");
      pr.println("IF @@FETCH_STATUS<>0");
      pr.println("BEGIN");
      pr.println("  RAISERROR ('TABLE %s not found in table sysobjects', 16, 1, @tablename)");
      pr.println("  CLOSE table_cursor");
      pr.println("  DEALLOCATE table_cursor");
      pr.println("  RETURN");
      pr.println("END");
      pr.println("DECLARE uq_cursor CURSOR FOR");
      pr.println("SELECT name from sysobjects WHERE parent_obj = @parentid AND xtype = 'UQ'"); 
      pr.println("CLOSE table_cursor");
      pr.println("DEALLOCATE table_cursor");
      pr.println("OPEN uq_cursor");
      pr.println("FETCH NEXT FROM uq_cursor INTO @uqname");
      pr.println("IF @@FETCH_STATUS<>0");
      pr.println("BEGIN");
      pr.println("  RAISERROR('Unique constraint of table %s not found in table sysobjects', 16, 1, @tablename);");
      pr.println("  CLOSE uq_cursor");
      pr.println("  DEALLOCATE uq_cursor");
      pr.println("  RETURN");
      pr.println("END");
      pr.println("WHILE @@FETCH_STATUS=0");
      pr.println("BEGIN");
      pr.println("  EXEC('ALTER TABLE '+@tablename+' DROP CONSTRAINT '+@uqname)");
      pr.println("  FETCH NEXT FROM uq_cursor INTO @uqname");
      pr.println("END");
      pr.println("CLOSE uq_cursor");
      pr.print("DEALLOCATE uq_cursor");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isRecreationOfUniqueConstraintsOnAlterTableNeeded()
   */
  @Override
  public boolean isRecreationOfUniqueConstraintsOnAlterTableNeeded()
  {
    // DO NOT REMOVE THIS METHOD OR CHANGE THE BEHAVIOUR OF IT
    //
    // There are some rare cases where altering a column on which a UNIQUE constraint is set can lead to the following error message:
    // -----
    // Msg 5074, Level 16, State 1, Line 9
    // The object 'UniqueConstraintKey' is dependent on column 'ColumnName'.
    // Msg 4922, Level 16, State 9, Line 9
    // ALTER TABLE ALTER COLUMN ColumnName failed because one or more objects access this column.
    // ----
    // It is not clear when this happens. We never had this problem with our test cases or when testing manually. 
    // But we have some customers that had this error. 
    // See issue #23610 for details
    return true;
  }
 
  @Override
  public boolean isRecreationOfIndexesOnAlterTableNeeded()
  {
    return true;
  }
  
  @Override
  public boolean isRecreationOfPrimaryKeysOnAlterTableNeeded()
  {
    return true;
  }
  
  @Override
  public void generateDropPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey,
          List<String> createdTemporaryStoredProcedures)
  {
    generateDropPrimaryKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("EXECUTE ");
    pr.print(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("");
    pr.print(" @tableName='");           
    pr.print(table.getId()); 
    pr.print("'");    
    generateDelimiter(pr);
    pr.println(); 
  } 
  
  private void generateDropPrimaryKeyConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE);
      
      generateCommentLine(pr, "Store Procedure to drop a primary key constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE);
      pr.println("@tableName varchar(100)");
      pr.println("AS");
      pr.println("DECLARE @pkName Varchar(255)");
      pr.println("SET @pkName= (");
      pr.println("  SELECT [name] FROM sysobjects");
      pr.println("    WHERE [xtype] = 'PK'");
      pr.println("          AND [parent_obj] = OBJECT_ID(N'[dbo].['+@tableName+N']')");
      pr.println(")" );
      pr.println("DECLARE @dropSql varchar(4000)");
      pr.println("SET @dropSql=");
      pr.println("  'ALTER TABLE [dbo].['+@tableName+']");
      pr.println("    DROP CONSTRAINT ['+@PkName+']'");
      pr.println("EXEC(@dropSql)");
      generateDelimiter(pr); 
      pr.println();
      pr.println();
    }
  }
  
  @Override
  public boolean isRecreationOfForeignKeysOnAlterTableNeeded()
  {
    return true;
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("EXECUTE ");
    pr.print(DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
    pr.print("");
    pr.print(" @tableName='");           
    pr.print(table.getId());
    pr.print("', @columnName='");           
    pr.print(foreignKey.getColumnName()); 
    pr.print("'");    
    generateDelimiter(pr);
    pr.println(); 
  } 
  
  private void generateDropForeignKeyConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
      
      generateCommentLine(pr, "Store Procedure to drop a unique constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
      pr.println("@tableName varchar(100),");
      pr.println("@columnName varchar(100)");
      pr.println("AS");
      pr.println("DECLARE @fkName Varchar(255)");
      pr.println("SET @fkName= (");
      pr.println("  SELECT fk.name");
      pr.println("  FROM sys.foreign_keys fk");
      pr.println("  INNER JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id");
      pr.println("  INNER JOIN sys.columns c1 ON fkc.parent_column_id = c1.column_id AND fkc.parent_object_id = c1.object_id");
      pr.println("  INNER JOIN sys.columns c2 ON fkc.referenced_column_id = c2.column_id AND fkc.referenced_object_id = c2.object_id");
      pr.println("  WHERE OBJECT_NAME(fk.parent_object_id)=@tableName AND c2.name=@columnName");
      pr.println(")" );
      pr.println("DECLARE @dropSql varchar(4000)");
      pr.println("SET @dropSql=");
      pr.println("  'ALTER TABLE [dbo].['+@tableName+']");
      pr.println("    DROP CONSTRAINT ['+@fkName+']'");
      pr.println("EXEC(@dropSql)");
      generateDelimiter(pr); 
      pr.println();
      pr.println();
    }
  }
  
  @Override
  public boolean isRecreationOfDefaultConstrainsNeeded()
  {
    return true;
  }
  
  @Override
  public void generateNonMetaDiffChangesPre(PrintWriter pr, int newVersionId)
  {
    super.generateNonMetaDiffChangesPre(pr, newVersionId);
    if (newVersionId == 33) {
      pr.println("IF EXISTS (");
      pr.println("   SELECT 'X'");
      pr.println("   FROM sysindexes");
      pr.println("   WHERE id = (SELECT OBJECT_ID('IWA_ContentManagementSystem'))");
      pr.println("   AND name = 'IWA_ContentManagementSystem_AppIdIndex'");
      pr.println(") ");
      pr.println("BEGIN");
      pr.println("DROP INDEX IWA_ContentManagementSystem.IWA_ContentManagementSystem_AppIdIndex;");
      pr.println("CREATE INDEX IWA_ContentManagementSystem_ApplicationIdIndex ON IWA_ContentManagementSystem(ApplicationId);");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      
      pr.println("IF EXISTS (");
      pr.println("   SELECT 'X'");
      pr.println("   FROM sysindexes");
      pr.println("   WHERE id = (SELECT OBJECT_ID('IWA_IntermediateEvent'))");
      pr.println("   AND name = 'IWA_IntermediateEvent_AppId'");
      pr.println(") ");
      pr.println("BEGIN");
      pr.println("DROP INDEX IWA_IntermediateEvent.IWA_IntermediateEvent_AppId;");
      pr.println("CREATE INDEX IWA_IntermediateEvent_ApplicationId ON IWA_IntermediateEvent(ApplicationId);");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      
      pr.println("IF EXISTS (");
      pr.println("   SELECT 'X'");
      pr.println("   FROM sysindexes");
      pr.println("   WHERE id = (SELECT OBJECT_ID('IWA_ContentManagementSystem'))");
      pr.println("   AND name = 'IWA_ContentManagementSystem_ProcessModelVersionIdIndex'");
      pr.println(") ");
      pr.println("BEGIN");
      pr.println("DROP INDEX IWA_ContentManagementSystem.IWA_ContentManagementSystem_ProcessModelVersionIdIndex;");
      pr.println("CREATE INDEX IWA_ContentManagementSystem_ProcessModelVersionId ON IWA_ContentManagementSystem(ProcessModelVersionId);");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
    }
  }
  
    
}
