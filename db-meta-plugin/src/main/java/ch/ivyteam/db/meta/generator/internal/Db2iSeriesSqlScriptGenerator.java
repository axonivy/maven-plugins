package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for Db2 (iSeries) database systems
 * @author rwei
 */
public class Db2iSeriesSqlScriptGenerator extends Db2SqlScriptGenerator
{
  /** Database management system hint */
  public static final String DB2_ISERIES = String.valueOf("Db2iSeries");
  
  private static final String DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE = "IWA_DROP_ALL_UNIQUE_CONSTRAINTS";
  private static final String DROP_FOREIGN_KEY_STORED_PROCUDRE = "IWA_DROP_FOREIGN_KEY";
    
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "IBM DB2 for iSeries";
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
    return Arrays.asList(DB2_ISERIES, DB2);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    generateDropUniqueConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
    pr.print(" ('");
    generateIdentifier(pr, table.getId().toUpperCase());
    pr.print("')"); 
  }
  
  private void generateDropUniqueConstraintStoredProcedure(PrintWriter pr, List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      generateCommentLine(pr, "Store Procedure to drop a unique constraint");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_UNIQUE_CONSTRAINT_STORED_PROCUDRE);
      pr.println("(");
      pr.println("  IN tableName VARCHAR(128)");       
      pr.println(")");
      pr.println("LANGUAGE SQL MODIFIES SQL DATA");
      pr.println("BEGIN");
      pr.println("  -- Drop all unique key constraints");
      pr.println("  FOR theseUniqueConstraints");
      pr.println("  AS");
      pr.println("  SELECT 'ALTER TABLE '||TRIM(tableName)||");
      pr.println("    ' DROP CONSTRAINT '||CONSTRAINT_NAME AS UniqueConst");
      pr.println("  FROM SYSCST");
      pr.println("  WHERE TABLE_NAME = tableName");
      pr.println("    AND CONSTRAINT_TYPE = 'UNIQUE' ");
      pr.println("  DO");
      pr.println("    EXECUTE IMMEDIATE theseUniqueConstraints.UniqueConst;");
      pr.println("  END FOR;");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      pr.println();      
    }
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(DROP_FOREIGN_KEY_STORED_PROCUDRE);
    pr.print(" ('");
    generateIdentifier(pr, table.getId().toUpperCase());
    pr.print("', '");
    generateIdentifier(pr, foreignKey.getColumnName().toUpperCase());
    pr.print("')"); 
    generateDelimiter(pr);
    pr.println();
    pr.println();      
  }
  
  private void generateDropForeignKeyStoredProcedure(PrintWriter pr,
          List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_FOREIGN_KEY_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_FOREIGN_KEY_STORED_PROCUDRE);
      pr.println();
      generateCommentLine(pr, "Store Procedure to drop a foreign key");

      pr.print("CREATE PROCEDURE ");
      pr.println(DROP_FOREIGN_KEY_STORED_PROCUDRE);
      pr.println("(");
      pr.println("  IN tableName VARCHAR(128),");
      pr.println("  IN columnName VARCHAR(128)");
      pr.println(")");
      pr.println("LANGUAGE SQL MODIFIES SQL DATA");
      pr.println("BEGIN");
      pr.println("  -- Drop foreign key");
      pr.println("  FOR theseForeignKeys");
      pr.println("  AS");
      pr.println("  SELECT 'ALTER TABLE '||TRIM(tableName)||");
      pr.println("    ' DROP FOREIGN KEY '||SYSCST.CONSTRAINT_NAME AS ForeignKey");
      pr.println("  FROM SYSCST, SYSKEYCST");
      pr.println("  WHERE SYSCST.TABLE_NAME = tableName");
      pr.println("    AND SYSCST.CONSTRAINT_TYPE = 'FOREIGN KEY'");
      pr.println("    AND SYSKEYCST.COLUMN_NAME = columnName");
      pr.println("    AND SYSCST.CONSTRAINT_NAME = SYSKEYCST.CONSTRAINT_NAME");      
      pr.println("  DO");
      pr.println("    EXECUTE IMMEDIATE theseForeignKeys.ForeignKey;");
      pr.println("  END FOR;");
      pr.println("END");
      generateDelimiter(pr);
      pr.println();
      pr.println();      
    }
  }

  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateNonMetaDiffChangesPre(java.io.PrintWriter, int)
   */
  @Override
  public void generateNonMetaDiffChangesPre(PrintWriter pr, int newVersionId)
  {
    if (newVersionId==33)
    {
      commit(pr);
      pr.println();
      pr.println();
      generateCommentLine(pr, "This reconfigures the current AS400 job (jdbc connection) so that answers to system messages is not requested (RQST) by the user (jdbc)");
      generateCommentLine(pr, "but from the system (SYSRPYL)");
      generateCommentLine(pr, "On the system itself an auto reply must be configured using the following command:");
      generateCommentLine(pr, "ADDRPYLE SEQNBR(9999) MSGID(CPA32B2) RPY(I)");
      generateCommentLine(pr, "This will prevent the conversion script from throwing a SQL0952 Error.");
      pr.print("CALL QSYS.QCMDEXC('CHGJOB INQMSGRPY(*SYSRPYL)', 0000000026.00000)");
      generateDelimiter(pr);
      pr.println();
      pr.println();       
    }
    super.generateNonMetaDiffChangesPre(pr, newVersionId);
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
  
  @Override
  protected void generateAlterTableAlterColumnIncompatibleDataTypes(PrintWriter pr, SqlTableColumn newColumn,
          SqlTable table, SqlTableColumn oldColumn)
  {
    SqlTableColumn tmpColumn = newColumn.changeId(newColumn.getId()+"_temp");
    generateAlterTableAddColumn(pr, tmpColumn, table);
    pr.println();
    copyValues(pr, table, oldColumn, tmpColumn, "CHAR");
    pr.println();
    commit(pr);
    pr.println();
    GenerateAlterTableUtil.generateAlterTableDropColumn(pr, this, table, oldColumn);
    generateAlterTableAddColumn(pr, newColumn, table);
    pr.println();
    copyValues(pr, table, tmpColumn, newColumn, null);
    pr.println();
    commit(pr);
    pr.println();
    GenerateAlterTableUtil.generateAlterTableDropColumn(pr, this, table, tmpColumn);
  }

  /**
   * @param pr
   */
  private void commit(PrintWriter pr)
  {
    pr.print("COMMIT");
    generateDelimiter(pr);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }


}
