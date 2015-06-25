package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for Db2 (zOS) database systems
 * @author rwei
 */
public class Db2zOsSqlScriptGenerator extends Db2SqlScriptGenerator
{
  /** 
   * System hint tablespace: 
   * Specifies the tablespace of a table or lob 
   */
  public static final String TABLESPACE = "Tablespace";
  /** Database management system hint */
  public static final String DB2_ZOS = "Db2zOs";
  /**
   * System hint create index:
   * Specifies to create an index for the sql artifact it is set on
   */
  public static final String CREATE_INDEX = "CreateIndex";
  /** 
   * System hint tablespace primary quantity (priqty):
   * Specifies the primary quantity of a tablespace
   */
  public static final String PRIQTY = "PRIQTY";
  /** 
   * System hint tablespace buffer pool:
   * Specifies the buffer pool to use by a tablespace
   */
  public static final String BUFFERPOOL = "BUFFERPOOL";
  /** 
   * System hint tablespace seg size:
   * Specifies the seg size of a tablespace
   */
  public static final String SEG_SIZE = "SEGSIZE";
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePreTable(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  protected void generatePreTable(PrintWriter pr, SqlTable table) throws MetaException
  {
    pr.print("CREATE TABLESPACE ");
    pr.print(getTablespace(table.getId(), table));
    pr.println(" IN {1}");
    pr.println("USING STOGROUP SYSDEFLT");
    pr.print("PRIQTY ");
    if (table.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(PRIQTY))
    {
      pr.println(table.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(PRIQTY));
    }
    else
    {
      pr.println(100);
    }
    if (table.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(BUFFERPOOL))
    {
      pr.print("BUFFERPOOL ");
      pr.println(table.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(BUFFERPOOL));
    }
    pr.print("SEGSIZE ");
    if (table.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(SEG_SIZE))
    {
      pr.print(table.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(SEG_SIZE));
    }
    else
    {
      pr.print(24);
    }
    pr.println(';');

    pr.println();
  }
  
  /**
   * @see SqlScriptGenerator#generateTableStorage(PrintWriter, SqlTable)
   */
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table) throws MetaException
  {
    pr.print(" IN {1}.");
    pr.print(getTablespace(table.getId(), table));
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "IBM DB2 for zOs";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePrefix(java.io.PrintWriter)
   */
  @Override
  protected void generatePrefix(PrintWriter pr)
  {
    pr.print("SET CURRENT SQLID = '{0}'");
    generateDelimiter(pr);
    pr.println();
    pr.println();
  } 

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIndex(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlPrimaryKey)
   */
  @Override
  protected void generateIndex(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey)
  {
    pr.print("CREATE UNIQUE INDEX ");
    pr.print(table.getId());
    pr.print('_');
    pr.println("PkIndex");
    pr.print("ON ");
    pr.print(table.getId());
    pr.print('(');
    generateColumnList(pr, primaryKey.getPrimaryKeyColumns());
    pr.println(");");
    pr.println();
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generatePostTable(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  protected void generatePostTable(PrintWriter pr, SqlTable table) throws MetaException
  {
    for (SqlTableColumn column : table.getColumns())
    {
      if ((column.getDataType().getDataType() == SqlDataType.DataType.BLOB)&&
         (!column.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(DATA_TYPE))&&
         (!column.getDatabaseManagementSystemHints(DB2).isHintSet(DATA_TYPE)))
      {
        generateAuxillaryTableForLob(pr, table, column);
      }
      else if ((column.getDataType().getDataType() == SqlDataType.DataType.CLOB)&&
              (!column.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(DATA_TYPE))&&
              (!column.getDatabaseManagementSystemHints(DB2).isHintSet(DATA_TYPE)))
      {
        generateAuxillaryTableForLob(pr, table, column);        
      }
    }
    super.generatePostTable(pr, table);
  }

  /**
   * Generates an auxillary table for a lob column
   * @param pr 
   * @param table 
   * @param column 
   * @throws MetaException 
   */
  private void generateAuxillaryTableForLob(PrintWriter pr, SqlTable table, SqlTableColumn column) throws MetaException
  {
    pr.print("CREATE LOB TABLESPACE ");
    pr.print(getTablespace(table.getId(), column));
    pr.println(" IN {1}");
    pr.println("USING STOGROUP SYSDEFLT");
    pr.print("PRIQTY ");
    if (column.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(PRIQTY))
    {
      pr.print(column.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(PRIQTY));
    }
    else
    {
      pr.print("10000");
    }
    pr.println(';');
    pr.println();
     
    pr.print("CREATE AUXILIARY TABLE ");
    pr.print(table.getId());
    pr.print('_');
    pr.print(column.getId());
    pr.print(" IN {1}.");
    pr.print(getTablespace(table.getId(), column));
    pr.print(" STORES ");
    pr.print(table.getId());
    pr.print(" COLUMN ");
    pr.print(column.getId());
    pr.println(";");
    pr.println();
    
    pr.print("CREATE UNIQUE INDEX ");
    pr.print(table.getId());
    pr.print("_");
    pr.print(column.getId());
    pr.print("Index");
    pr.print(" ON ");
    pr.print(table.getId());
    pr.print('_');
    pr.print(column.getId());
    pr.println(";");
    pr.println();
  }
  
  /**
   * Gets the tablespace 
   * @param table
   * @param artifact the artifact to get the tablespace for
   * @return tablespace
   * @throws MetaException 
   */
  private String getTablespace(String table, SqlArtifact artifact) throws MetaException
  {
    String tablespace;
    tablespace = artifact.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(TABLESPACE);
    if ((tablespace == null)||(tablespace.trim().length()==0))
    {
      throw new MetaException("No "+DB2_ZOS+" "+TABLESPACE+" system hint defined for "+artifact+" on table "+table);
    }
    return tablespace;
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isUniqueConstraintOutsideTableSupported(ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint)
   */
  @Override
  public boolean isUniqueConstraintOutsideTableSupported(SqlUniqueConstraint uniqueConstraint)
  {
    return true;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDataType(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDataType, ch.ivyteam.db.meta.model.internal.SqlArtifact)
   */
  @Override
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact)
  {
    if (!artifact.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(DATA_TYPE))
    {
      super.generateDataType(pr, dataType, artifact);  
    }
    else
    {
      pr.print(artifact.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(DATA_TYPE));
    }    
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIndex(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlForeignKey)
   */
  @Override
  protected void generateIndex(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey)
  {
    if (foreignKey.getDatabaseManagementSystemHints(DB2_ZOS).isHintSet(CREATE_INDEX))
    {
      pr.print("CREATE INDEX ");
      pr.println(foreignKey.getDatabaseManagementSystemHints(DB2_ZOS).getHintValue(CREATE_INDEX));
      pr.print("ON ");
      pr.print(table.getId());
      pr.print('(');
      pr.print(foreignKey.getColumnName());
      pr.print(')');
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    super.generateIndex(pr, table, foreignKey);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(DB2_ZOS, DB2);
  }

}
