package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.util.collections.CollectionsUtil;

/**
 * Generates the sql script for Db2 (Linux Unix Windows) database systems
 * @author rwei
 */
public class Db2LuwSqlScriptGenerator extends Db2SqlScriptGenerator
{
  /** Database hint constant for Db2Luw */
  public static final String DB2_LUW = String.valueOf("Db2Luw");
  
  private static final String DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE = "IWA_DROP_ALL_UNIQUE_CONSTRAINTS";

  /**
   * @see SqlScriptGenerator#generateTableStorage(PrintWriter, SqlTable)
   */
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.print(" IN {1}");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.Db2SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return " IBM DB2 for LUW (Linux / Unix / Windows)";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePrefix(java.io.PrintWriter)
   */
  @Override
  protected void generatePrefix(PrintWriter pr)
  {
    pr.print("SET SCHEMA {0}");
    generateDelimiter(pr);
    pr.print('\n');
    pr.print('\n');
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return CollectionsUtil.listify(DB2_LUW, DB2);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateTableReorganisation(PrintWriter, SqlTable)
   */
  @Override
  public void generateTableReorganisation(PrintWriter pr, SqlTable newTable)
  {
    generateCommentLine(pr, "Reorganizes table " + newTable.getId());
    pr.print("CALL SYSPROC.ADMIN_CMD ('REORG TABLE ");
    generateIdentifier(pr, newTable.getId());
    pr.print("')");
    generateDelimiter(pr);
    pr.print('\n');
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    generateDropUniqueConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL {0}.");
    pr.print(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
    pr.print(" ('{0}', '");
    generateIdentifier(pr, table.getId().toUpperCase());
    pr.print("')"); 
  }
  
  /**
   * Generates a stored procedure to drop unique constraints
   * @param pr
   * @param createdTemporaryStoredProcedures 
   */
  private void generateDropUniqueConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);      

      generateCommentLine(pr, "Store Procedure to drop a unique constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      pr.println("(");
      pr.println("  IN schemaName VARCHAR(128),");
      pr.println("  IN tableName VARCHAR(128)");
      pr.println(")");
      pr.println("MODIFIES SQL DATA NO EXTERNAL ACTION DETERMINISTIC");
      pr.println("BEGIN");
      pr.println("  -- Drop all unique key constraints");
      pr.println("  FOR theseUniqueConstraints");
      pr.println("  AS");
      pr.println("  SELECT 'ALTER TABLE '||TRIM(schemaName)||'.'||TRIM(tableName)||");
      pr.println("    ' DROP CONSTRAINT '||constname AS UniqueConst");
      pr.println("  FROM SYSCAT.TABCONST");
      pr.println("  WHERE tabschema = schemaName AND tabname = tableName");
      pr.println("    AND type = 'U' ");
      pr.println("  DO");
      pr.println("    EXECUTE IMMEDIATE theseUniqueConstraints.UniqueConst;");
      pr.println("  END FOR;");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      pr.println();      
    }
  }

}
