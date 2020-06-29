package ch.ivyteam.db.meta.generator.internal.mysql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class MySqlForeignKeys extends ForeignKeys
{
  public MySqlForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    super(dbHints, delimiter, identifiers, comments);
  }
  
  @Override
  public boolean isReferenceInColumnDefinitionSupported()
  {
    return false;
  }
  
  @Override
  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(MySqlSqlScriptGenerator.DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("(");
    pr.print("SCHEMA()"); 
    pr.print(", '");
    pr.print(table.getId()); 
    pr.print("', '");
    pr.print(foreignKey.getColumnName());
    pr.print("')");    
    delimiter.generate(pr);
    pr.println(); 
  }

  private void generateDropForeignKeyConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(MySqlSqlScriptGenerator.DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(MySqlSqlScriptGenerator.DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      
      comments.generate(pr, "Store Procedure to drop a foreign key constraint");

      pr.print("CREATE PROCEDURE ");
      pr.print(MySqlSqlScriptGenerator.DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      pr.println("(fk_schema VARCHAR(64), fk_table VARCHAR(64), fk_column VARCHAR(64))");
      pr.println("BEGIN");
      pr.println("  WHILE EXISTS(");
      pr.println("    SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("    WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("      AND CAST(TABLE_SCHEMA AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_schema AS CHAR CHARACTER SET ascii)");
      pr.println("      AND CAST(TABLE_NAME AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_table AS CHAR CHARACTER SET ascii)");
      pr.println("      AND CAST(COLUMN_NAME AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_column AS CHAR CHARACTER SET ascii)");
      pr.println("  ) ");
      pr.println("  DO");
      pr.println("    BEGIN");
      pr.println("      SET @sqlstmt = (");
      pr.println("        SELECT CONCAT('ALTER TABLE ',TABLE_SCHEMA,'.',TABLE_NAME,' DROP FOREIGN KEY ',CONSTRAINT_NAME)");
      pr.println("        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("        WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("          AND CAST(TABLE_SCHEMA AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_schema AS CHAR CHARACTER SET ascii)");
      pr.println("          AND CAST(TABLE_NAME AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_table AS CHAR CHARACTER SET ascii)");
      pr.println("          AND CAST(COLUMN_NAME AS CHAR CHARACTER SET ascii) COLLATE ascii_general_ci = CAST(fk_column AS CHAR CHARACTER SET ascii)");
      pr.println("        LIMIT 1");
      pr.println("      );");
      pr.println("      PREPARE stmt1 FROM @sqlstmt;");
      pr.println("      EXECUTE stmt1;");
      pr.println("    END;");
      pr.println("  END WHILE;");
      pr.println("END;");
      delimiter.generate(pr);
      pr.println();
      pr.println();
    }
  }
}