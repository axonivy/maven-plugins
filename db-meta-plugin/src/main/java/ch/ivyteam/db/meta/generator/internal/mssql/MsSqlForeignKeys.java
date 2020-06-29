package ch.ivyteam.db.meta.generator.internal.mssql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class MsSqlForeignKeys extends ForeignKeys
{
  MsSqlForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    super(dbHints, delimiter, identifiers, comments);
  }
  
  @Override
  public void generateAlterTableAdd(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey)
  {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" ADD FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }
  
  @Override
  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("EXECUTE ");
    pr.print(MsSqlServerSqlScriptGenerator.DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
    pr.print("");
    pr.print(" @tableName='");           
    pr.print(table.getId());
    pr.print("', @columnName='");           
    pr.print(foreignKey.getColumnName()); 
    pr.print("'");    
    delimiter.generate(pr);
    pr.println(); 
  } 
  
  private void generateDropForeignKeyConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(MsSqlServerSqlScriptGenerator.DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(MsSqlServerSqlScriptGenerator.DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
      
      comments.generate(pr, "Store Procedure to drop a unique constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(MsSqlServerSqlScriptGenerator.DROP_FOREIGN_CONSTRAINT_STORED_PROCUDRE);
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
      delimiter.generate(pr); 
      pr.println();
      pr.println();
    }
  }
}