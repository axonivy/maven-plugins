package ch.ivyteam.db.meta.generator.internal.mssql;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.GenerateAlterTableUtil;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for Microsoft SQL Server database systems
 * @author rwei
 */
public class MsSqlServerSqlScriptGenerator extends SqlScriptGenerator
{
  private static final String DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE  = "IWA_Drop_Unique"; 
  private static final String DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_PrimaryKey";
  static final String DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey";
  private static final Delimiter DELIMITER = new Delimiter("\nGO");
  public static final String MS_SQL_SERVER = String.valueOf("MsSqlServer");  
  private List<String> dropUniqueForTables = new ArrayList<>();
  
  public MsSqlServerSqlScriptGenerator()
  {
    super(MS_SQL_SERVER, DELIMITER, Identifiers.STANDARD, Comments.STANDARD);
  }
  
  @Override
  protected DmlStatements createDmlStatementsGenerator(DbHints hints, Delimiter delim, Identifiers ident)
  {
    return new MsSqlDmlStatements(hints, delim, ident);
  }
  
  @Override
  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts, ForeignKeys fKeys)
  {
    return new MsSqlTriggers(hints, delim, dmlStmts, fKeys);
  }
  
  @Override
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident, Comments cmmnts)
  {
    return new MsSqlForeignKeys(hints, delim, ident, cmmnts);
  }

  @Override
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact) {
    if (dbHints.DATA_TYPE.isSet(artifact)) {
      dbHints.DATA_TYPE.generate(pr, artifact);
    } else {
      generateDataType(pr, dataType.getDataType());
      if (dataType.getLength() >= 0) {
        pr.print('(');
        var length = dataType.getLength();
        // because MS SQL Server uses bytes as varchar length, we reserve 4 bytes per character
        if (dataType.getDataType().equals(DataType.VARCHAR)) {
          if (artifact instanceof SqlTableColumn) {
            var column = (SqlTableColumn) artifact;
            if (!column.isPrimaryKey() && column.getReference() == null) {
              length *= 4;
            }
          }
        }
        pr.print(length);
        if (dataType.getPrecision() >= 0) {
          pr.print(", ");
          pr.print(dataType.getPrecision());
        }
        pr.print(')');
      }
    }
  }

  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case CLOB:
        pr.append("VARCHAR(MAX)");
        break;
      case BLOB:
        pr.append("VARBINARY(MAX)");
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
    
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  @Override
  public String dbName()
  {
    return "Microsoft SQL Server";
  }

  @Override
  protected void generatePrefix(PrintWriter pr)
  {
    var path = fOutputFile.getAbsolutePath();
    if (!path.contains("mssqlserver.sql") && !path.contains("CreateDatabase.sql"))
    {
      return;
    }

    pr.print("SET IMPLICIT_TRANSACTIONS OFF");
    delimiter.generate(pr);
    pr.println();
    pr.println();
    generateAlterDatabaseForSnapshotIsolation(pr);
    pr.println();
    pr.println();
    generateAlterDatabaseForRecursiveTriggers(pr);
    pr.println();
    pr.print("SET IMPLICIT_TRANSACTIONS ON");
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }

  private void generateAlterDatabaseForSnapshotIsolation(PrintWriter pr)
  {
    comments.generate(pr, "Alter database so that read operations are not blocked by write operations.");
    pr.print("ALTER DATABASE [${databaseName}] SET ALLOW_SNAPSHOT_ISOLATION ON");
    delimiter.generate(pr);
    pr.println();
    pr.println();
    pr.print("ALTER DATABASE [${databaseName}] SET READ_COMMITTED_SNAPSHOT ON");
    delimiter.generate(pr);
  }

  private void generateAlterDatabaseForRecursiveTriggers(PrintWriter pr)
  {
    comments.generate(pr, "Alter database so that recursive triggers work.");
    pr.print("ALTER DATABASE [${databaseName}] SET RECURSIVE_TRIGGERS ON");
    delimiter.generate(pr);
    pr.println();
  }

  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn)
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
  
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD");
  }
  
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
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
      
      comments.generate(pr, "Store Procedure to drop a unique constraint");

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
      delimiter.generate(pr);
      pr.println();
      pr.println();
    }
  }
  
  @Override
  public void generateDropPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey, List<String> createdTemporaryStoredProcedures)
  {
    generateDropPrimaryKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("EXECUTE ");
    pr.print(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("");
    pr.print(" @tableName='");           
    pr.print(table.getId()); 
    pr.print("'");    
    delimiter.generate(pr);
    pr.println(); 
  } 
  
  private void generateDropPrimaryKeyConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_PRIMARY_CONSTRAINT_STORED_PROCUDRE);
      
      comments.generate(pr, "Store Procedure to drop a primary key constraint");

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
      delimiter.generate(pr); 
      pr.println();
      pr.println();
    }
  }
  

  @Override
  public void generateDropIndex(PrintWriter pr, SqlTable table, SqlIndex index) 
  {
    pr.print("DROP INDEX ");
    identifiers.generate(pr, table.getId()+"."+getIndexName(index));
    delimiter.generate(pr);
    pr.println(); 
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.defaultConstraints = true;
    options.foreignKeysOnAlterTable = true;
    options.primaryKeysOnAlterTable = true;
    options.indexesOnAlterTable = true;
    
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
    options.uniqueConstraintsOnAlterTable = true;
    options.allUniqueConstraintsOnAlterTable = true;
    
    return options;
  }
}
