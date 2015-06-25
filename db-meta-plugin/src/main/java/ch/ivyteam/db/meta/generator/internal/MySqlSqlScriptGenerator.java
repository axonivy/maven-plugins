package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

/**
 * Generates the sql script for MySql database systems
 * @author rwei
 */
public class MySqlSqlScriptGenerator extends SqlScriptGenerator
{
  /** Database System */
  public static final String MYSQL = String.valueOf("MySql");
  
  private static final String DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey_Constraint";

  
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
        pr.append("MEDIUMBLOB");
        break;
      default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isForeignKeyReferenceInColumnDefinitionSupported()
   */
  @Override
  public boolean isForeignKeyReferenceInColumnDefinitionSupported()
  {
    return false;
  }
  
  @Override
  public boolean isRecreationOfForeignKeysOnAlterTableNeeded()
  {
    return true;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateTableStorage(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.append(" ENGINE={0}");
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "MySQL";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateComment(java.io.PrintWriter)
   */
  @Override
  protected void generateComment(PrintWriter pr)
  {
    pr.append("# ");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(MYSQL);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "OLD";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIdentifierQuote(java.io.PrintWriter)
   */
  @Override
  protected void generateIdentifierQuote(PrintWriter pr)
  {
    pr.print("`");
  }
  
 /**
  * Generates a table row delete trigger. Subclasses may override this method.
  * @param pr the print writer to generate to
  * @param table the table which triggers the trigger
  * @param triggerStatements the statements that have to be executed by the trigger
  * @param recursiveTrigger flag indicating if this trigger is recursive
 * @throws MetaException 
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
    pr.println("FOR EACH ROW");
    pr.println("BEGIN");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.println("END");
    generateDelimiter(pr);
  }
  
  /**
   * Generates the update statement
   * @param pr the print writer
   * @param updateStmt the update statement
   * @param insets the insets
   */
  @Override
  protected void generateUpdateStatement(PrintWriter pr, SqlUpdate updateStmt, int insets)
  {
    boolean first = true;
    writeSpaces(pr, insets);
    pr.print("UPDATE ");
    pr.println(updateStmt.getTable());
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
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    pr.print(updateStmt.getFilterExpression());
  }
  
  /**
   * Could overridden from different database types
   * @param pr
   * @param newColumn 
   * @param newTable
   * @param oldColumn 
   * @throws MetaException 
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException
  {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn, newTable, "MODIFY");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" DROP INDEX ");
    generateIdentifier(pr, unique.getColumns().get(0));
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("(");
    pr.print("SCHEMA()"); 
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
      pr.println("(fk_schema VARCHAR(64), fk_table VARCHAR(64), fk_column VARCHAR(64))");
      pr.println("BEGIN");
      pr.println("  WHILE EXISTS(");
      pr.println("    SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("    WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("      AND TABLE_SCHEMA = fk_schema");
      pr.println("      AND TABLE_NAME = fk_table");
      pr.println("      AND COLUMN_NAME = fk_column");
      pr.println("  ) ");
      pr.println("  DO");
      pr.println("    BEGIN");
      pr.println("      SET @sqlstmt = (");
      pr.println("        SELECT CONCAT('ALTER TABLE ',TABLE_SCHEMA,'.',TABLE_NAME,' DROP FOREIGN KEY ',CONSTRAINT_NAME)");
      pr.println("        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("        WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("          AND TABLE_SCHEMA = fk_schema");
      pr.println("          AND TABLE_NAME = fk_table");
      pr.println("          AND COLUMN_NAME = fk_column");
      pr.println("        LIMIT 1");
      pr.println("      );");
      pr.println("      PREPARE stmt1 FROM @sqlstmt;");
      pr.println("      EXECUTE stmt1;");
      pr.println("    END;");
      pr.println("  END WHILE;");
      pr.println("END;");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }
}