package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.generator.Target;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlBinaryRelation;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDatabaseSystemHints;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlInsert;
import ch.ivyteam.db.meta.model.internal.SqlLiteral;
import ch.ivyteam.db.meta.model.internal.SqlLogicalExpression;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlNot;
import ch.ivyteam.db.meta.model.internal.SqlNull;
import ch.ivyteam.db.meta.model.internal.SqlParent;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlReference;
import ch.ivyteam.db.meta.model.internal.SqlSelect;
import ch.ivyteam.db.meta.model.internal.SqlSelectExpression;
import ch.ivyteam.db.meta.model.internal.SqlSimpleExpr;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlTableContentDefinition;
import ch.ivyteam.db.meta.model.internal.SqlTrigger;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;
import ch.ivyteam.db.meta.model.internal.SqlWhenThen;

/**
 * Generates sql scripts out of the sql meta information
 * @author rwei
 */
public abstract class SqlScriptGenerator implements IMetaOutputGenerator
{
  /** The default row trigger old variable name */
  private static final String DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME = "OLD";

  /** The output file */
  protected File fOutputFile;

  /** The header comment */
  private String fComment;

  /** Stores the already generated tables */
  private Set<String> fGeneratedTables = new HashSet<String>();

  /** 
   * Database System hint datatype:
   * Use this database system hint to specify another data type 
   */
  public static final String DATA_TYPE = "DataType";
  
  /**
   * Database System Hint NoReferenceUseTrigger: 
   * If set on a column with reference or foreign key suppresses the generation of a foreign key or reference and instead 
   * supports the foreign key action with a trigger  
   */
  public static final String NO_REFERENCE_USE_TRIGGER = "NoReferenceUseTrigger";
  
  /**
   * Database System Hint NoActionUseTrigger: 
   * If set on a column with reference or foreign key suppresses the generation of the foreign key action and instead 
   * supports the foreign key action with a trigger  
   */
  public static final String NO_ACTION_USE_TRIGGER = "NoActionUseTrigger";

  /**
   * Database System Hint NoAction: 
   * If set on a column with reference or foreign key suppresses the generation of the foreign key action
   */
  public static final String NO_ACTION = "NoAction";

  /**
   * Database System Hint NoReference: 
   * If set on a column with reference or foreign key suppresses the generation of a foreign key or reference. 
   * The reference action will not be executed. 
   */
  public static final String NO_REFERENCE = "NoReference";
  
  /**
   * Database System Hint TriggerExecuteForEachStatement: 
   * If set on a trigger a FOR EACH STATEMENT trigger is generated using the statements defined in the 
   * EXECUTE FOR EACH STATEMENT section of the trigger. If not set a FOR EACH ROW trigger is generated with 
   * the statements defined in the EXECUTE FOR EACH ROW section of the trigger. 
   */
  public static final String TRIGGER_EXECUTE_FOR_EACH_STATEMENT = "TriggerExecuteForEachStatement";
  
  /**
   * System Database Hint Use unique index:
   * Instead of a UNIQUE constraint a unique index is created
   */
  public static final String USE_UNIQUE_INDEX = "UseUniqueIndex";

  /** 
   * System Database Hint Foreign Table:
   * Use to override the foreign table of a reference
   */
  public static final String FOREIGN_TABLE = "ForeignTable";

  /** 
   * System Database Hint ReferenceAction
   * Use to override the foreign key action 
   */
  public static final String REFERENCE_ACTION = "ReferenceAction";

  /**
   * System Database Hint Delete Trigger Name
   * Use to override the name of delete triggers 
   */
  public static final String DELETE_TRIGGER_NAME = "DeleteTriggerName";
  
  /** 
   * System Database Hint NoUnique
   * Use to suppress generation of unique constraint
   */
  public static final String NO_UNIQUE = "NoUnique";
  
  /** 
   * System Database Hint IndexName
   * Use to override name of index 
   */
  public static final String INDEX_NAME = "IndexName";

  /** 
   * System Database Hint NoIndex
   * Use to suppress generation of an index 
   */
  public static final String NO_INDEX = "NoIndex";

  /**
   * System Database Hint DefaultValue
   * Use to override the default column value 
   */
  public static final String DEFAULT_VALUE = "DefaultValue";

  /**
   * @see IMetaOutputGenerator#analyseArgs(String[])
   */
  @Override
  public void analyseArgs(String[] generatorArgs) throws Exception
  {
    if (generatorArgs.length < 2)
    {
      throw new Exception("There must be at least 4 generator options");
    }
    if (!generatorArgs[0].equalsIgnoreCase("-outputFile"))
    {
      throw new Exception("First generator option must be -outputFile");
    }
    fOutputFile = new File(generatorArgs[1]);
    if (!fOutputFile.exists())
    {
      fOutputFile.getParentFile().mkdirs();
    }
    if (generatorArgs.length > 2)
    {
      if (generatorArgs.length < 4)
      {
        throw new Exception("There must be at least 4 generator options");
      }
      if (!generatorArgs[2].equalsIgnoreCase("-comment"))
      {
        throw new Exception("Second generator option must be -comment");
      }
      fComment = generatorArgs[3];
    }
  }
  
  @Override
  public Target getTarget()
  {
    return Target.createSingleTargetFile(fOutputFile);
  }
  
  /**
   * Sets the fComment to the given parameter
   * @param comment the fComment to set
   */
  public void setComment(String comment)
  {
    this.fComment = comment;
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    PrintWriter pr = new NewLinePrintWriter(fOutputFile);
    try
    {
      generateHeader(pr);
      generatePrefix(pr);
      
      List<SqlTable> tables = metaDefinition.getArtifacts(SqlTable.class);
      generateTables(pr, tables);
      
      for (SqlView view : metaDefinition.getArtifacts(SqlView.class))
      {
        generateView(pr, view);
      }
      
      generateTriggers(pr, metaDefinition);
      
      for (SqlInsert insert : metaDefinition.getArtifacts(SqlInsert.class))
      {
        generateInsert(pr, insert);
      }
      generatePostfix(pr);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
  }

  /**
   * Generate all given tables
   * @param pr
   * @param tables
   * @throws MetaException
   */
  public final void generateTables(PrintWriter pr, List<SqlTable> tables) throws MetaException
  {
    Map<SqlTable, List<SqlForeignKey>> alterTables = new LinkedHashMap<SqlTable, List<SqlForeignKey>>();

    for (SqlTable table : tables)
    {
      generateTable(pr, table, alterTables);
    }
    for (Entry<SqlTable, List<SqlForeignKey>> entry : alterTables.entrySet())
    {
      SqlTable table = entry.getKey();
      for (SqlForeignKey foreignKey : entry.getValue())
      {
        generateAlterTableAddForeignKey(pr, table, foreignKey);
      }
    }
  }

  /**
   * Generates an alter table add foreign key statement
   * @param pr
   * @param table the table 
   * @param foreignKey the foreign key
   * @throws MetaException 
   */
  public void generateAlterTableAddForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey) throws MetaException
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.println(" ADD");
    pr.println("(");
    pr.print(" FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    pr.println();
    pr.print(")");
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" DROP FOREIGN KEY ");
    pr.print(foreignKey.getId());
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }


  /**
   * Could overridden from different database types
   * @param pr
   * @param newColumn 
   * @param newTable
   * @param oldColumn 
   * @throws MetaException 
   */
  public abstract void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException;

  /**
   * @param pr
   * @param newColumn
   * @param newTable
   */
  public abstract void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable);
  
  /**
   * @param pr
   * @param view
   */
  public void generateDropView(PrintWriter pr, SqlView view)
  {
    pr.print("DROP VIEW ");
    pr.print(view.getId());
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  /**
   * Generates a view
   * @param pr the writer
   * @param view the view
   * @throws MetaException 
   */
  public void generateView(PrintWriter pr, SqlView view) throws MetaException
  {
    boolean first = true;
    pr.print("CREATE VIEW ");
    pr.print(view.getId());
    pr.println();
    pr.print("(");
    pr.println();
    for (SqlViewColumn column : view.getColumns())
    {
      if (!first)
      {
        pr.println(",");
      }
      first = false;
      writeSpaces(pr, 2);
      generateIdentifier(pr, column.getId());
    }
    pr.println();
    pr.println(")");
    pr.println("AS");
    first = true;
    for (SqlSelect select : view.getSelects())
    {
      if (!first)
      {
        pr.println();
        pr.println("UNION ALL");
      }
      first = false;
      generateSelect(pr, select, 2);
    }
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }

  /**
   * @param pr
   * @param select
   * @param indent
   */
  private void generateSelect(PrintWriter pr, SqlSelect select, int indent)
  {
    boolean first = true;
    writeSpaces(pr, indent);
    pr.println("SELECT");
    for (SqlSelectExpression expression : select.getExpressions())
    {
      if (!first)
      {
        pr.println(",");
      }
      first = false;
      writeSpaces(pr, indent+2);
      generateViewExpression(pr, expression);      
    }
    pr.println();
    writeSpaces(pr, indent);
    pr.print("FROM ");
    first = true;
    for (String table : select.getTables())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      generateIdentifier(pr, table);
    }
    pr.println();
    writeSpaces(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, select.getCondition());    
  }

  /**
   * Generates the given sql view expression 
   * @param pr the print writer
   * @param expression the expression that should be generated
   * @throws MetaException 
   */
  protected void generateViewExpression(PrintWriter pr, SqlSelectExpression expression) throws MetaException
  {
    generateSqlAtom(pr, expression.getExpression());
  }

  /**
   * Generates a sql atom
   * @param pr
   * @param expression
   * @throws MetaException 
   */
  protected void generateSqlAtom(PrintWriter pr, SqlAtom expression) throws MetaException
  {
    if (expression instanceof SqlFullQualifiedColumnName)
    {
      generateFullQualifiedColumnName(pr, (SqlFullQualifiedColumnName)expression);
    }
    else if (expression instanceof SqlCaseExpr)
    {
      generateSqlCaseExpression(pr, (SqlCaseExpr)expression);
    }
    else if (expression instanceof SqlLiteral)
    {
      generateValue(pr, ((SqlLiteral)expression).getValue());
    }
    else
    {
      throw new MetaException("Unknown expression "+expression);
    }
  }

  /**
   * Generates a case expression
   * @param pr
   * @param caseExpr
   */
  protected void generateSqlCaseExpression(PrintWriter pr, SqlCaseExpr caseExpr)
  {
    pr.print("CASE ");
    generateFullQualifiedColumnName(pr, caseExpr.getColumnName());
    
    for (SqlWhenThen whenThen : caseExpr.getWhenThenList())
    {
      pr.print(' ');
 
      pr.print("WHEN ");
      generateValue(pr, whenThen.getLiteral());
      pr.print(" THEN ");
      generateFullQualifiedColumnName(pr, whenThen.getColumnName());
    }
    pr.print(" END");
  }

  /**
   * Generates a full qualified column name
   * @param pr 
   * @param fullQualifiedColumnName
   */
  protected void generateFullQualifiedColumnName(PrintWriter pr, SqlFullQualifiedColumnName fullQualifiedColumnName)
  {
    if (fullQualifiedColumnName.getTable() != null)
    {
      if (DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME.equals(fullQualifiedColumnName.getTable()))
      {
        pr.print(getRowTriggerOldVariableName());
      }
      else
      {
        generateIdentifier(pr, fullQualifiedColumnName.getTable());
      }
      pr.print('.');
    }
    generateIdentifier(pr, fullQualifiedColumnName.getColumn());
  }

  /**
   * Hook to generate sql script before any other script is created
   * @param pr the writer
   */
  protected void generatePostfix(@SuppressWarnings("unused") PrintWriter pr)
  {

  }

  /**
   * Hook to generate sql script after any other script is created
   * @param pr the writer
   */
  protected void generatePrefix(@SuppressWarnings("unused") PrintWriter pr)
  {

  }

  /**
   * Generates the header
   * @param pr
   */
  public void generateHeader(PrintWriter pr)
  {
    if (fComment != null)
    {
      generateCommentLine(pr,
              "---------------------------------------------------------------------------------");
      generateCommentLine(pr, "");
      for (String comment : fComment.split("\n"))
      {
        generateCommentLine(pr, comment);
      }
      generateCommentLine(pr, "");
    }
    generateCommentLine(pr,
            "---------------------------------------------------------------------------------");
    generateCommentLine(pr, "");
    generateCommentLine(pr,
            "This script was automatically generated. Do not edit it. Instead edit the source file");
    generateCommentLine(pr, "");
    generateCommentLine(pr,
            "---------------------------------------------------------------------------------");
    generateCommentLine(pr, "Database: " + getDatabaseComment());
    generateCommentLine(pr,
            "---------------------------------------------------------------------------------");
    generateCommentLine(pr, "Copyright:");
    generateCommentLine(pr, "ivyTeam AG, Alpenstrasse 9, 6304 Zug");
    generateCommentLine(pr,
            "---------------------------------------------------------------------------------");
    pr.append('\n');
  }

  /**
   * Generates the header
   * @param pr
   * @param newVersionId 
   */
  public void generateVersionUpdate(PrintWriter pr, int newVersionId)
  {
    pr.println();
    generateCommentLine(pr, "");
    generateCommentLine(pr, "Update Version");
    generateCommentLine(pr, "");
    pr.append("UPDATE IWA_Version SET Version=" + newVersionId);
    generateDelimiter(pr);
    pr.println();
  }
  

  public void generateNonMetaDiffChangesPre(@SuppressWarnings("unused") PrintWriter pr, @SuppressWarnings("unused") int newVersionId)
  {
  }
  
  /**
   * Generates the DDL updates that are not reflected by a meta data difference 
   * @param pr
   * @param newVersionId
   */
  public void generateNonMetaDiffChangesPost(PrintWriter pr, int newVersionId)
  {
    // Delete all process models that belong to the SYSTEM application. 
    // This triggers a redeploy of the system projects at the first ivy Server restart.
    generateCommentLine(pr, "Delete process models that belong to the SYSTEM application");
    pr.append("DELETE FROM IWA_ProcessModel WHERE ApplicationId=0");    
    generateDelimiter(pr);
    pr.println();
    pr.println();
    
    if (newVersionId == 32)
    {
      generateCommentLine(pr, "Issue 23540: Deletion of a PMV must remove/delete associated library");
      pr.append("DELETE FROM IWA_Library " +
      		"WHERE ProcessModelVersionId IN (SELECT ProcessModelVersionId FROM IWA_ProcessModelVersion WHERE ReleaseState=4)"); // ReleaseState=DELETED    
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    if (newVersionId == 33)
    {
      SqlTable table = new SqlTable("IWA_Identifier", Collections.<SqlTableContentDefinition>emptyList(), Collections.<SqlDatabaseSystemHints>emptyList(), null);
      generateAlterTableAlterColumn(pr, 
              new SqlTableColumn("IdentifierValue", new SqlDataType(DataType.BIGINT), false, null, null, null, null), 
              table, 
              new SqlTableColumn("IdentifierValue", new SqlDataType(DataType.INTEGER), false, null, null, null, null));
      pr.println();
      pr.println();
      generateTableReorganisation(pr, table);
    }
  }

  /**
   * Gets the database name
   * @return database name
   */
  protected abstract String getDatabaseComment();

  /**
   * Gets the database system names that are valid for the generator
   * @return list with the valid database system names
   */
  protected abstract List<String> getDatabaseSystemNames();
  
  /**
   * Checks if the given database system hint is set on the given sql artifact.
   * All database systems returned by {@link #getDatabaseSystemNames()} are considered.
   * @param artifact the artifact on which to check if the hint is set
   * @param hint the database system hint to check
   * @return true if hint is set, otherwise false
   */
  protected boolean isDatabaseSystemHintSet(SqlArtifact artifact, String hint)
  {
    for (String databaseSystem : getDatabaseSystemNames())
    {
      if (artifact.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint))
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Gets the value of the given database system hint on the given sql artifact.
   * All database systems returned by {@link #getDatabaseSystemNames()} are considered.
   * If the hint is set on multiple valid database systems the first found is returned. 
   * @param artifact the artifact on which the hint value is get
   * @param hint the database system hint 
   * @return hint value.
   */
  protected String getDatabaseSystemHintValue(SqlArtifact artifact, String hint)
  {
    for (String databaseSystem : getDatabaseSystemNames())
    {
      if (artifact.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint))
      {
        return artifact.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint);
      }
    }
    return null;
  }
  
  /**
   * Generates the database system hint value 
   * @param pr the print writer
   * @param artifact the artifact
   * @param hint the hint 
   */
  protected void generateDatabaseManagementHintValue(PrintWriter pr, SqlArtifact artifact, String hint)
  {
    pr.print(getDatabaseSystemHintValue(artifact, hint));
  }
  

  /**
   * Generates a comment line
   * @param pr the writer
   * @param comment the comment to generate
   */
  public final void generateCommentLine(PrintWriter pr, String comment)
  {
    generateComment(pr);
    pr.append(comment);
    pr.println();
  }

  /**
   * Generates a comment
   * @param pr the writer
   */
  protected void generateComment(PrintWriter pr)
  {
    pr.append("-- ");
  }

  /**
   * Generates an insert statement
   * @param pr the writer to write to
   * @param insert the insert statement
   */
  public void generateInsert(PrintWriter pr, SqlInsert insert)
  {
    boolean first = true;
    pr.append("INSERT INTO ");
    pr.append(insert.getTable());
    pr.append(" (");
    for (String column : insert.getColumns())
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      pr.append(column);
    }
    pr.append(") VALUES (");
    first = true;
    for (SqlLiteral value : insert.getValues())
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      generateValue(pr, value.getValue());
    }
    pr.append(")");
    generateDelimiter(pr);
    pr.append("\n\n");
  }
  
  public void generateDelete(PrintWriter pr, SqlInsert insert)
  {
    pr.append("DELETE FROM ");
    pr.append(insert.getTable());
    pr.append(" WHERE ");
    
    boolean first = true;
    for (int pos = 0; pos < insert.getColumns().size(); pos++)
    {
      if (!first)
      {
        pr.append(" AND ");
      }
      first = false;
      String column = insert.getColumns().get(pos);
      pr.append(column);
      pr.append("=");
      Object value = insert.getValues().get(pos).getValue();
      generateValue(pr, value);      
    }
    generateDelimiter(pr);
    pr.append("\n\n");
  }

  /**
   * Generates the trigger definitions.
   * @param pr the print writer
   * @param metaDefinition the meta definition
   * @throws MetaException
   */
  protected void generateTriggers(PrintWriter pr, SqlMeta metaDefinition) throws MetaException
  {
    generateForEachStatementDeleteTriggers(pr, metaDefinition);
    generateForEachRowDeleteTriggers(pr, metaDefinition);
  }
  
  /**
   * Generate drop table for the given table
   * @param pr
   * @param table
   */
  public final void generateDropTable(PrintWriter pr, SqlTable table)
  {
    pr.write("DROP TABLE ");
    generateIdentifier(pr, table.getId());
    generateDelimiter(pr);
    pr.println(); 
  }
  
  /**
   * @param pr
   * @param table
   */
  public void generateDropTrigger(PrintWriter pr, SqlTable table)
  {
    pr.write("DROP TRIGGER ");
    generateTriggerName(pr, table);
    generateDelimiter(pr);
    pr.println(); 
  }

  /**
   * @param metaDefinition
   * @param table
   * @return true, if this table has a generated trigger
   * @throws MetaException
   */
  public boolean hasTrigger(SqlMeta metaDefinition, SqlTable table) throws MetaException
  {
    boolean hasTriggerStatements = getForEachRowDeleteTriggerInfo(table, metaDefinition).getRight().size() > 0;
    boolean hasForEachStatementDeleteTrigger = getForEachStatementDeleteTrigger(table, metaDefinition).size() > 0;
    return hasTriggerStatements || hasForEachStatementDeleteTrigger;
  }
  
  /**
   * Generate for each row delete triggers. Subclasses may override this method
   * @param pr
   * @param metaDefinition
   * @throws MetaException
   */
  protected void generateForEachRowDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition) throws MetaException
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      generateForEachRowDeleteTrigger(pr, table, metaDefinition);
    }
  }

  /**
   * generate for each row delete trigger
   * @param pr the print writer
   * @param table the table 
   * @param metaDefinition the meta definition
   * @throws MetaException  
   */
  public final void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    Pair<Boolean, List<SqlDmlStatement>> triggerStatements = getForEachRowDeleteTriggerInfo(table, metaDefinition);
    List<SqlDmlStatement> statements = triggerStatements.getRight(); 
    if (!statements.isEmpty())
    {
      generateForEachRowDeleteTrigger(pr, table, statements, triggerStatements.getLeft());
      pr.println();
      pr.println();
    }
  }

  /**
   * @param table
   * @param metaDefinition
   * @return -
   * @throws MetaException
   */
  private Pair<Boolean, List<SqlDmlStatement>> getForEachRowDeleteTriggerInfo(SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    SqlReference reference;
    boolean recursiveTrigger=false;
    List<SqlDmlStatement> statements = new ArrayList<SqlDmlStatement>();
    // First analyze triggers on all tables   
    for (SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : foreignTable.getTriggers())
      {
        if (trigger.getTableName().equals(table.getId()))
        {
          if (!isDatabaseSystemHintSet(trigger, TRIGGER_EXECUTE_FOR_EACH_STATEMENT))
          {
            statements.addAll(trigger.getStatementsForEachRow());
          }  
        }
      }
    }
    // Second analyze foreign keys on all tables 
    for (SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreignKey : foreignTable.getForeignKeys())
      {
        reference = foreignKey.getReference();
        if ((reference.getForeignTable().equals(table.getId()))&&
            (getForeignKeyAction(foreignKey) != null))
        {
          if ((!isForeignKeySupported(foreignKey)&&(!isDatabaseSystemHintSet(foreignKey, NO_REFERENCE)))||
               isDatabaseSystemHintSet(foreignKey, NO_ACTION_USE_TRIGGER))
          {
            if (getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_CASCADE)
            {
              if (foreignTable.getId().equals(table.getId()))
              {
                recursiveTrigger = true;
              }
              statements.add(
                      new SqlDelete(foreignTable.getId(), 
                              new SqlBinaryRelation(
                                      new SqlFullQualifiedColumnName(
                                              foreignTable.getId(),
                                              foreignKey.getColumnName()), 
                                      "=",
                                      new SqlFullQualifiedColumnName(
                                              DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, 
                                              foreignKey.getReference().getForeignColumn()))));

            }
            else if (getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_SET_NULL)
            {
              if (foreignTable.getId().equals(table.getId()))
              {
                recursiveTrigger = true;
              }              
              statements.add(
                      new SqlUpdate(
                              foreignTable.getId(),
                              Arrays.asList(
                                      new SqlUpdateColumnExpression(foreignKey.getColumnName(),
                                        new SqlLiteral(SqlNull.getInstance()))),
                              new SqlBinaryRelation(
                                      new SqlFullQualifiedColumnName(
                                              foreignTable.getId(),
                                              foreignKey.getColumnName()), 
                                      "=",
                                      new SqlFullQualifiedColumnName(
                                              DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, 
                                              foreignKey.getReference().getForeignColumn())), null, null));
            }
          }
        }
      }
    }

    // Third analyze the my foreign keys for ON DELETE THIS CASCADE
    for (SqlForeignKey foreignKey : table.getForeignKeys())
    {
      if ((!isDatabaseSystemHintSet(foreignKey, NO_REFERENCE))&&
          (getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_THIS_CASCADE))
      {
        statements.add(new SqlDelete(foreignKey.getReference().getForeignTable(),
                new SqlBinaryRelation(new SqlFullQualifiedColumnName(foreignKey.getReference()
                        .getForeignTable(), foreignKey.getReference().getForeignColumn()), "=",
                        new SqlFullQualifiedColumnName(DEFAULT_ROW_TRIGGER_OLD_VARIABLE_NAME, foreignKey
                                .getColumnName()))));
      }
    }
    return new ImmutablePair<Boolean, List<SqlDmlStatement>>(recursiveTrigger, statements);
  }

  /**
   * Gets the name of the OLD table name in row triggers that can be used to reference the values of the
   * deleted row
   * @return OLD table name
   */
  protected String getRowTriggerOldVariableName()
  {
    return ":old";
  }

  /**
   * Generate for each statement delete trigger. Subclasses may override this method
   * @param pr
   * @param metaDefinition
   * @throws MetaException 
   */
  protected void generateForEachStatementDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition) throws MetaException
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      genrateForEachStatementDeleteTrigger(pr, table, metaDefinition);
    }
  }

  /**
   * Generate for each statement delete trigger. Subclasses may override this method
   * @param pr
   * @param table
   * @param metaDefinition
   * @throws MetaException 
   */
  public final void genrateForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    List<SqlDmlStatement> statements = getForEachStatementDeleteTrigger(table, metaDefinition);
    if (!statements.isEmpty())
    {
      generateForEachStatementDeleteTrigger(pr, table, statements);
      pr.println();
      pr.println();
    }
  }

  /**
   * @param table
   * @param metaDefinition
   * @return -
   */
  private List<SqlDmlStatement> getForEachStatementDeleteTrigger(SqlTable table, SqlMeta metaDefinition)
  {
    List<SqlDmlStatement> statements = new ArrayList<SqlDmlStatement>();
    for(SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : foreignTable.getTriggers())        
      {   
        if (trigger.getTableName().equals(table.getId()))
        {  
          if (isDatabaseSystemHintSet(trigger, TRIGGER_EXECUTE_FOR_EACH_STATEMENT))
          {
            statements = trigger.getStatementsForEachStatement();
          }
        }
      }
    }
    return statements;
  }

  /**
   * Generates a table row delete trigger. Subclasses may override this method.
   * @param pr the print writer to generate to
   * @param table the table which triggers the trigger
   * @param triggerStatements the statements that have to be executed by the trigger
   * @param recursiveTrigger flag indicating if this trigger is recursive
   * @throws MetaException 
   */
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, @SuppressWarnings("unused") boolean recursiveTrigger) throws MetaException
  {
    pr.print("CREATE TRIGGER ");
    generateTriggerName(pr, table);
    pr.println();
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
    pr.print("END");
    generateDelimiter(pr);
  }

  /**
   * @param pr
   * @param table
   */
  private void generateTriggerName(PrintWriter pr, SqlTable table)
  {
    if (isDatabaseSystemHintSet(table, DELETE_TRIGGER_NAME))
    {
      pr.print(getDatabaseSystemHintValue(table, DELETE_TRIGGER_NAME));
    }
    else
    {
      pr.print(table.getId());
      pr.print("DeleteTrigger");
    }
  }

  /**
   * Generates a dml statement
   * @param pr the print write
   * @param stmt the statement
   * @param insets the insets
   * @throws MetaException 
   */
  protected void generateDmlStatement(PrintWriter pr, SqlDmlStatement stmt, int insets) throws MetaException
  {
    if (stmt instanceof SqlDelete)
    {
      generateDeleteStatement(pr, (SqlDelete)stmt, insets);
    }
    else if (stmt instanceof SqlUpdate)
    {
      generateUpdateStatement(pr, (SqlUpdate)stmt, insets);
    }    
  }

  /**
   * Generates the update statement
   * @param pr the print writer
   * @param updateStmt the update statement
   * @param insets the insets
   * @throws MetaException 
   */
  protected void generateUpdateStatement(PrintWriter pr, SqlUpdate updateStmt, int insets) throws MetaException
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
      pr.print(updateStmt.getTable());
      pr.print('.');
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    if (updateStmt.getFilterExpression() != null)
    {
      pr.println();
      writeSpaces(pr, insets);
      pr.print("WHERE ");
      generateFilterExpression(pr, updateStmt.getFilterExpression());
    }
  }

  /**
   * Generates a filter expression
   * @param pr 
   * @param filterExpression
   * @throws MetaException 
   */
  protected void generateFilterExpression(PrintWriter pr, SqlSimpleExpr filterExpression) throws MetaException
  {
    if (filterExpression instanceof SqlBinaryRelation)
    {
      generateSqlAtom(pr, ((SqlBinaryRelation)filterExpression).getFirst());
      pr.print(' ');
      pr.print(((SqlBinaryRelation)filterExpression).getOperator());
      pr.print(' ');
      generateSqlAtom(pr, ((SqlBinaryRelation)filterExpression).getSecond());      
    }
    else if (filterExpression instanceof SqlLogicalExpression)
    {
      generateFilterExpression(pr, ((SqlLogicalExpression)filterExpression).getFirst());
      pr.print(' ');
      pr.print(((SqlLogicalExpression)filterExpression).getOperator());
      pr.print(' ');
      generateFilterExpression(pr, ((SqlLogicalExpression)filterExpression).getSecond());
    }
    else if (filterExpression instanceof SqlNot)
    {
      pr.print("NOT ");
      generateFilterExpression(pr, ((SqlNot)filterExpression).getExpression());      
    }
    else if (filterExpression instanceof SqlParent)
    {
      pr.print('(');
      generateFilterExpression(pr, ((SqlParent)filterExpression).getExpression());
      pr.print(')');
    }
  }

  /**
   * Generates the delete statement
   * @param pr the print writer
   * @param deleteStmt the delete stmt
   * @param insets the insets
   * @throws MetaException 
   */
  protected void generateDeleteStatement(PrintWriter pr, SqlDelete deleteStmt, int insets) throws MetaException
  {
    writeSpaces(pr, insets);
    pr.print("DELETE FROM ");
    pr.println(deleteStmt.getTable());
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }

  /**
   * Generates a table delete trigger. Subclasses may override this method.
   * @param pr the print writer to generate to
   * @param table the table which triggers the trigger
   * @param triggerStatements the statements that have to be executed by the trigger
   * @throws MetaException 
   */
  protected void generateForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements) throws MetaException
  {
    pr.print("CREATE TRIGGER ");
    pr.print(table.getId());
    pr.println("DeleteTrigger");
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
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
   * Generates the table
   * @param pr the writer
   * @param table the table
   * @throws MetaException
   */
  private void generateTable(PrintWriter pr, SqlTable table, Map<SqlTable, List<SqlForeignKey>> alterTables) throws MetaException
  {
    boolean firstColumn = true;

    generatePreTable(pr, table);
    pr.append("CREATE TABLE ");
    generateIdentifier(pr, table.getId());
    pr.append("\n");
    pr.append("(\n");
    for (SqlTableColumn column : table.getColumns())
    {
      if (!firstColumn)
      {
        pr.append(",\n");
      }
      firstColumn = false;
      generateColumn(pr, table, column, alterTables);
    }
    if (table.getPrimaryKey() != null)
    {
      if (!firstColumn)
      {
        pr.append(",\n");
      }
      firstColumn = firstColumn || !generatePrimaryKey(pr, table.getPrimaryKey());
    }
    if (!isForeignKeyReferenceInColumnDefinitionSupported())
    {
      for (SqlForeignKey foreignKey : table.getForeignKeys())
      {
        if (isForeignKeySupported(foreignKey))
        {
          if (isTableAlreadyGenerated(foreignKey.getReference().getForeignTable()))
          {
            if (!firstColumn)
            {
              pr.append(",\n");
            }
            firstColumn = false;
            generateForeignKey(pr, foreignKey);
          }
          else
          {
            addAlterTableAddForeignKey(alterTables, table, foreignKey);
          }
        }
      }
    }

    for (SqlUniqueConstraint unique : table.getUniqueConstraints())
    {
      if (isUniqueConstraintInTableSupported(unique))
      {
        if (!firstColumn)
        {
          pr.append(",\n");
        }
        firstColumn = firstColumn || !generateUniqueConstraint(pr, unique);
      }
    }

    if (isIndexInTableSupported())
    {
      for (SqlIndex index : getIndexes(table))
      {
        if (!firstColumn)
        {
          pr.append(",\n");
        }
        firstColumn = firstColumn || !generateIndexInTable(pr, table, index);
      }
    }
    
    pr.append("\n");
    pr.append(')');
    generateTableStorage(pr, table);
    generateDelimiter(pr);
    pr.append("\n\n");
    generatePostTable(pr, table);
    if (table.getPrimaryKey() != null)
    {
      generateIndex(pr, table, table.getPrimaryKey());
    }
    for (SqlUniqueConstraint unique : table.getUniqueConstraints())
    {
      if (isUniqueConstraintOutsideTableSupported(unique))
      {
          generateUniqueConstraint(pr, table, unique);
      }
    }
    if (!isIndexInTableSupported())
    {
      for (SqlIndex index : getIndexes(table))
      {
        generateIndex(pr, table, index);
      }
    }
    for (SqlForeignKey foreignKey : table.getForeignKeys())
    {
      generateIndex(pr, table, foreignKey);
    }
    addGeneratedTable(table.getId());
  }

  /**
   * @param table
   * @return index of this table
   */
  public List<SqlIndex> getIndexes(SqlTable table)
  {
    List<SqlIndex> tableIndexes = new ArrayList<SqlIndex>();
    for (SqlIndex index : table.getIndexes())
    {
      if (!isDatabaseSystemHintSet(index, NO_INDEX))
      {
        tableIndexes.add(index);
      }
    }
    return tableIndexes;
  }

  /**
   * Adds the given table to the generated tables
   * @param tableName
   */
  public final void addGeneratedTable(String tableName)
  {
    assert tableName != null : "Parameter tableName must not be null";
    assert fGeneratedTables.contains(tableName) == false : "Parameter tableName must not yet be generated";
    fGeneratedTables.add(tableName);
  }

  /**
   * Is the table with the given name already generated
   * @param tableName the name of the table
   * @return true if the table is already generated false if not.
   */
  protected final boolean isTableAlreadyGenerated(String tableName)
  {
    assert tableName != null : "Parameter tableName must not be null";
    return fGeneratedTables.contains(tableName);
  }

  /**
   * Returns if the database system supports indexes inside the create table definition
   * @return true if it supports, otherwise false
   */
  protected boolean isIndexInTableSupported()
  {
    return true;
  }

  /**
   * Returns if the database system supports unique constraints outside the create table definition
   * @param uniqueConstraint the unique constraint 
   * @return true if it supports, otherwise false
   * @see #isUniqueConstraintInTableSupported(SqlUniqueConstraint)
   */
  public boolean isUniqueConstraintOutsideTableSupported(SqlUniqueConstraint uniqueConstraint)
  {
    return isDatabaseSystemHintSet(uniqueConstraint, USE_UNIQUE_INDEX);
  }

  /**
   * Returns if the database system supports unique constraints inside the create table definition
   * @param uniqueConstraint the unique constraint 
   * @return true if it supports, otherwise false
   * @see #isUniqueConstraintOutsideTableSupported(SqlUniqueConstraint)
   */
  public boolean isUniqueConstraintInTableSupported(SqlUniqueConstraint uniqueConstraint)
  {
    return !isDatabaseSystemHintSet(uniqueConstraint, USE_UNIQUE_INDEX) && !isDatabaseSystemHintSet(uniqueConstraint, NO_UNIQUE);
  }

  /**
   * Called before a table definition is generated. Subclasses can use this hook to generate things before the
   * table definition is generated
   * @param pr the print writer
   * @param table the table
   * @throws MetaException
   */
  @SuppressWarnings("unused")
  protected void generatePreTable(PrintWriter pr, SqlTable table) throws MetaException
  {
    // Hook for subclasses to add things before the table definition
  }

  /**
   * Called after a table definition was generated. Subclasses can use this hook to generate things after the
   * table definition was generated
   * @param pr the print writer
   * @param table the table
   * @throws MetaException
   */
  @SuppressWarnings("unused")
  protected void generatePostTable(PrintWriter pr, SqlTable table) throws MetaException
  {
    // Hook for subclasses to add things before the table definition
  }

  /**
   * Called after the closing parentis of the create table sql statement but before the ending delimitier.
   * Subclasses can use this hook to generate table storage information
   * @param pr the print writer
   * @param table the table
   * @throws MetaException
   */
  @SuppressWarnings("unused")
  protected void generateTableStorage(PrintWriter pr, SqlTable table) throws MetaException
  {
    // Hook for subclasses to add things between the closing parentis and the delimiter
  }

  /**
   * Generates a unique constraint inside a table definition. If a database system cannot define unique
   * constraints in the table definition override the method {@link #isUniqueConstraintOutsideTableSupported(SqlUniqueConstraint)} and
   * return false. The {@link #generateUniqueConstraint(PrintWriter, SqlTable, SqlUniqueConstraint)} will then generate a unique index outside of the
   * table.
   * @param pr the writer to write to
   * @param unique the unique constraint
   * @return true if unique constraint was generated, false if not
   */
  protected boolean generateUniqueConstraint(PrintWriter pr, SqlUniqueConstraint unique)
  {
    writeSpaces(pr, 2);
    pr.print("UNIQUE ");
    pr.print('(');
    generateColumnList(pr, unique.getColumns());
    pr.print(')');
    return true;
  }

  /**
   * Generates an index inside a table definition. If a database system cannot define indexes in the table
   * definition override the method {@link #isIndexInTableSupported} and return false. To generate an index
   * outside the table declaration override the method {@link #generateIndex(PrintWriter, SqlTable, SqlIndex)}
   * .
   * @param pr the print writer to write to
   * @param table
   * @param index the index to generate
   * @return true if index was generated if not return false
   */
  protected boolean generateIndexInTable(PrintWriter pr, @SuppressWarnings("unused") SqlTable table, SqlIndex index)
  {
    writeSpaces(pr, 2);
    pr.print("INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.print(' ');
    pr.print('(');
    generateColumnList(pr, index.getColumns());
    pr.print(')');
    return true;
  }
  
  /**
   * Generates a column list
   * @param pr the writer
   * @param columns the columns to write
   */
  protected void generateColumnList(PrintWriter pr, List<String> columns)
  {
    boolean first = true;
    for (String column : columns)
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      generateIdentifier(pr, column);
    }
  }

  /**
   * Hook to create a index for the given primary key after the table definition
   * <p>
   * Override this method if a database system does not explicit create indexes for primary keys.
   * </p>
   * @param pr the writer
   * @param table the table
   * @param primaryKey the primary key
   */
  protected void generateIndex(@SuppressWarnings("unused") PrintWriter pr,
          @SuppressWarnings("unused") SqlTable table, @SuppressWarnings("unused") SqlPrimaryKey primaryKey)
  {
    // On most systems a primary key definition will explicit create an index.
    // Therefore we do not generate by default an index for primary keys
  }

  /**
   * <p>
   * Hook to create a index for the given foreign key after the table definition.
   * </p>
   * <p>
   * Override this method if a database system does not explicit create indexes for foreign keys.
   * </p>
   * @param pr the writer
   * @param table the table
   * @param foreignKey the foreign key
   */
  protected void generateIndex(@SuppressWarnings("unused") PrintWriter pr,
          @SuppressWarnings("unused") SqlTable table, @SuppressWarnings("unused") SqlForeignKey foreignKey)
  {
    // On most systems a foreign key definition will explicit create an index.
    // Therefore we do not generate by default an index for foreign keys.
  }

  /**
   * <p>
   * Generates an index after a table definition
   * </p>
   * <p>
   * This method is only called if the method {@link #isIndexInTableSupported()} overridden an return false.
   * </p>
   * @param pr the writer
   * @param table the table
   * @param index the index to create
   */
  public void generateIndex(PrintWriter pr,
          SqlTable table, SqlIndex index)
  {
    pr.print("CREATE INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    generateIdentifier(pr, table.getId());
    pr.print(" (");
    generateColumnList(pr, index.getColumns());
    pr.print(")");
    generateDelimiter(pr);
    pr.println();
    pr.println();    
  }

  /**
   * @param index
   */
  protected String getIndexName(SqlIndex index)
  {
    if (isDatabaseSystemHintSet(index, INDEX_NAME))
    {
      return getDatabaseSystemHintValue(index, INDEX_NAME);
    }
    else
    {
      return index.getId();
    }
  }

  
  /**
   * <p>
   * Creates a index for the given unique constraint after the table definition.
   * </p>
   * @param pr the writer
   * @param table the table
   * @param unique the unique constraint
   */
  public void generateUniqueConstraint(PrintWriter pr,
          SqlTable table, SqlUniqueConstraint unique)
  {
    pr.print("CREATE UNIQUE INDEX ");
    pr.write(getUniqueConstraintName(unique));
    pr.println();
    pr.print("ON ");
    pr.print(table.getId());
    pr.print(" (");
    generateColumnList(pr, unique.getColumns());
    pr.print(')');
    generateDelimiter(pr);
    pr.println();
  }
  
  /**
   * @param pr
   * @param table
   * @param unique
   */
  public final void generateAddUniqueConstraint(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique)
  {
    if (isUniqueConstraintInTableSupported(unique))
    {
      pr.print("ALTER TABLE ");
      generateIdentifier(pr, table.getId());
      pr.print(" ADD UNIQUE (");
      generateColumnList(pr, unique.getColumns());
      pr.print(")");
      generateDelimiter(pr);
      pr.println();
    }
    else if (isUniqueConstraintOutsideTableSupported(unique))
    {
      generateUniqueConstraint(pr, table, unique);
    }
  }
  
  /**
   * Generates a statement to drop a unique constraint. If the method generates also a temporary helper stored procedure add the id of it to the
   * given createdTemporaryStoredProcedures list then it will be dropped at the end of the conversion script
   * @param pr
   * @param table 
   * @param unique
   * @param createdTemporaryStoredProcedures 
   */
  public void generateDropUniqueConstraint(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    if (!isUniqueConstraintInTableSupported(unique) && !isUniqueConstraintOutsideTableSupported(unique))
    {
      return;
    }
      
    if (isUniqueConstraintOutsideTableSupported(unique))
    {
      generateDropUniqueIndex(pr, table, unique, createdTemporaryStoredProcedures);
    }
    else
    {
      generateAlterTableDropUniqueConstraint(pr, table, unique, createdTemporaryStoredProcedures);
    }
    generateDelimiter(pr);
    pr.println();
  }
  
  /**
   * Generates a statement to drop a unique constraint. If the method generates also a temporary helper stored procedure add the id of it to the
   * given createdTemporaryStoredProcedures list then it will be dropped at the end of the conversion script
   * @param pr
   * @param table
   * @param unique
   * @param createdTemporaryStoredProcedures 
   */
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" DROP UNIQUE (");
    generateColumnList(pr, unique.getColumns());
    pr.print(")");
  }

  /**
   * Generates a statement to drop a unique index. If the method generates also a temporary helper stored procedure add the id of it to the
   * given createdTemporaryStoredProcedures list then it will be dropped at the end of the conversion script
   * @param pr
   * @param table
   * @param unique
   * @param createdTemporaryStoredProcedures 
   */
  protected void generateDropUniqueIndex(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique, @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" DROP INDEX ");
    generateIdentifier(pr, getUniqueConstraintName(unique));
  }

  /** 
   * @param unique
   * @return unique constraint name
   */
  protected final String getUniqueConstraintName(SqlUniqueConstraint unique)
  {
    if (getDatabaseSystemHintValue(unique, USE_UNIQUE_INDEX)!=null)
    {
      return getDatabaseSystemHintValue(unique, USE_UNIQUE_INDEX);
    }
    else
    {
      return unique.getId();
    }
  }

  /**
   * Generate primary key
   * @param pr the print writer
   * @param primaryKey the primary key to generate
   * @return true if primary key was generated, false if not
   */
  protected boolean generatePrimaryKey(PrintWriter pr, SqlPrimaryKey primaryKey)
  {
    writeSpaces(pr, 2);
    pr.append("PRIMARY KEY (");
    generateColumnList(pr, primaryKey.getPrimaryKeyColumns());
    pr.append(")");
    return true;
  }

  /**
   * Generates a statement delimiter
   * @param pr
   */
  protected void generateDelimiter(PrintWriter pr)
  {
    pr.append(';');
  }

  /**
   * Generates the column
   * @param pr the writer
   * @param table the table
   * @param column the column
   * @param alterTables
   * @throws MetaException 
   */
  public final void generateColumn(PrintWriter pr, SqlTable table, SqlTableColumn column, Map<SqlTable, List<SqlForeignKey>> alterTables) throws MetaException
  {
    writeSpaces(pr, 2);
    generateIdentifier(pr, column.getId());
    pr.append(' ');
    generateDataType(pr, column.getDataType(), column);
    if (isNullBeforeDefaultConstraint())
    {
      generateNullConstraint(pr, column.isCanBeNull(), column);
      generateDefaultValue(pr, column);      
    }
    else
    {
      generateDefaultValue(pr, column);
      generateNullConstraint(pr, column.isCanBeNull(), column);      
    }
    if ((column.getReference()!=null)&&
        (isForeignKeyReferenceInColumnDefinitionSupported())&&            
        (isForeignKeySupported(table.findForeignKey(column))))
    {
      if (isTableAlreadyGenerated(column.getReference().getForeignTable()))
      {
        generateReference(pr, column.getReference(), table.findForeignKey(column));
      }
      else
      {
        addAlterTableAddForeignKey(alterTables, table, table.findForeignKey(column));
      }
    }    
  }
  
  /**
   * Generates an identifier 
   * @param pr the print writer
   * @param identifier the identifier
   */
  protected void generateIdentifier(PrintWriter pr, String identifier)
  {
    if (isReservedSqlKeyword(identifier))
    {
      generateIdentifierQuote(pr);
      pr.print(identifier);
      generateIdentifierQuote(pr);
    }
    else
    {
      pr.print(identifier);
    }
  }

  /**
   * Generates an identifier quote
   * @param pr print writer
   */
  protected void generateIdentifierQuote(PrintWriter pr)
  {
    pr.print("\"");
  }

  /**
   * Checks if the given identifier is a reserved sql keyword
   * @param identifier the identifer to check
   * @return true is it is a reserved sql word, otherwise false
   */
  protected boolean isReservedSqlKeyword(String identifier)
  {
    return ReservedSqlKeywords.get().contains(identifier.toUpperCase());
  }

  /**
   * Generates the foreign key definition. This method is only called if method {@link #isForeignKeyReferenceInColumnDefinitionSupported()} 
   * returns false.
   * @param pr the print writer
   * @param foreignKey the foreign key to generate
   * @throws MetaException 
   */
  protected void generateForeignKey(PrintWriter pr, SqlForeignKey foreignKey) throws MetaException
  {
    writeSpaces(pr, 2);
    pr.print("FOREIGN KEY (");
    generateIdentifier(pr, foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
  }

  /**
   * Is foreign Key reference in column definition supported. If this method returns 
   * true the method {@link #generateReference(PrintWriter, SqlReference, SqlForeignKey)} will be called
   * during the column definition generation. If it returns false the method will not be called instead
   * the method {@link #generateForeignKey(PrintWriter, SqlForeignKey)} will be called after the column generation.</br>
   * Subclasses may override this method if foreign key reference in column definitions are not 
   * supported and instead foreign key should be declared outside the column definition.
   * @return true if it supported, false if not
   * @see #generateReference(PrintWriter, SqlReference, SqlForeignKey)
   * @see #generateForeignKey(PrintWriter, SqlForeignKey)
   */
  public boolean isForeignKeyReferenceInColumnDefinitionSupported()
  {
    return true;
  }

  /**
   * Is foreign key supported. Subclasses that does not support foreign keys may override this method an return false
   * @param foreignKey the foreign key
   * @return true if they are supported, false if not
   */
  public boolean isForeignKeySupported(SqlForeignKey foreignKey)
  {
    return !(isDatabaseSystemHintSet(foreignKey, NO_REFERENCE_USE_TRIGGER)||isDatabaseSystemHintSet(foreignKey, NO_REFERENCE));
  }

  /**
   * Should the null / not null constraint be generated before the default value of a column
   * @return true if null should be before default, false otherwise
   */
  protected boolean isNullBeforeDefaultConstraint()
  {
    return true;
  }

  /**
   * Adds a foreign key that should be created with an alter table statement
   * @param table the table 
   * @param foreignKey the foreign key
   */
  private static void addAlterTableAddForeignKey(Map<SqlTable, List<SqlForeignKey>> alterTables, SqlTable table, SqlForeignKey foreignKey)
  {
    List<SqlForeignKey> foreignKeys = alterTables.get(table);
    if (foreignKeys == null)
    {
      foreignKeys = new ArrayList<SqlForeignKey>();
      alterTables.put(table, foreignKeys);
    }
    foreignKeys.add(foreignKey);
  }

  /**
   * Generates a reference
   * @param pr the writer
   * @param reference the reference
   * @param foreignKey The sql foreign key the reference is defined on
   * @throws MetaException 
   */
  protected void generateReference(PrintWriter pr, SqlReference reference, SqlForeignKey foreignKey) throws MetaException
  {
    pr.append(" REFERENCES ");
    pr.append(getForeignTable(reference, foreignKey));
    pr.append('(');
    pr.append(reference.getForeignColumn());
    pr.append(')');
    if ((getForeignKeyAction(foreignKey) != null)&&
        (!isDatabaseSystemHintSet(foreignKey, NO_ACTION_USE_TRIGGER))&&
        (!isDatabaseSystemHintSet(foreignKey, NO_ACTION)))
    {
      switch(getForeignKeyAction(foreignKey))
      {
        case ON_DELETE_CASCADE:
          pr.append(' ');
          pr.append("ON DELETE CASCADE");
          break;
        case ON_DELETE_SET_NULL:
          pr.append(' ');
          pr.append("ON DELETE SET NULL");
          break;
        case ON_DELETE_THIS_CASCADE:
          // do not generate this action because sql does not offer it.
          // this action must be implemented with triggers!
          break;
      }
    }
  }

  /**
   * Gets the foreign key action of the given foreign key
   * @param foreignKey the foreign key
   * @return foreign key action
   * @throws MetaException 
   */
  protected SqlForeignKeyAction getForeignKeyAction(SqlForeignKey foreignKey) throws MetaException
  {
    if (isDatabaseSystemHintSet(foreignKey, REFERENCE_ACTION))
    {
      for (SqlForeignKeyAction action : SqlForeignKeyAction.values())
      {
        if (action.toString().equals(getDatabaseSystemHintValue(foreignKey, REFERENCE_ACTION)))
        {
          return action;
        }
      }
      throw new MetaException("Unknown Foreign Key Action '"+getDatabaseSystemHintValue(foreignKey, REFERENCE_ACTION));
    }
      
    return foreignKey.getReference().getForeignKeyAction();
  }

  /**
   * Gets the foreign table of a reference
   * @param reference the reference 
   * @param artifact the artifact the reference was declared on
   * @return foreign table
   */
  protected String getForeignTable(SqlReference reference, SqlArtifact artifact)
  {
    if (isDatabaseSystemHintSet(artifact, FOREIGN_TABLE))
    {
      return getDatabaseSystemHintValue(artifact, FOREIGN_TABLE);
    }
    return reference.getForeignTable();
  }

  /**
   * Generate null constraint
   * @param pr the writer
   * @param canBeNull can be null or not
   * @param column the full column definition
   */
  protected void generateNullConstraint(PrintWriter pr, boolean canBeNull,
          @SuppressWarnings("unused") SqlTableColumn column)
  {
    if (!canBeNull)
    {
      pr.append(" NOT NULL");
    }
  }

  /**
   * Generate default value
   * @param pr the writer
   * @param column the column
   */
  protected void generateDefaultValue(PrintWriter pr, SqlTableColumn column)
  {
    if (column.getDefaultValue() != null)
    {
      pr.append(" DEFAULT ");
      if (isDatabaseSystemHintSet(column, DEFAULT_VALUE))
      {
        generateValue(pr, getDatabaseSystemHintValue(column, DEFAULT_VALUE));
      }
      else
      {
        generateValue(pr, column.getDefaultValue().getValue());
      }
    }
  }

  /**
   * Generates a value
   * @param pr the writer
   * @param value the value to generate
   */
  protected void generateValue(PrintWriter pr, Object value)
  {
    if (value == SqlNull.getInstance())
    {
      pr.append("NULL");
    }
    else if (value instanceof String)
    {
      pr.append("'");
      pr.append(value.toString());
      pr.append("'");
    }
    else
    {
      pr.append(value.toString());
    }
  }

  /**
   * Generates the data type
   * @param pr the writer
   * @param dataType the data type
   * @param artifact the artifact the data type is generated for
   */
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact)
  {
    if (isDatabaseSystemHintSet(artifact, DATA_TYPE))
    {
      generateDatabaseManagementHintValue(pr, artifact, DATA_TYPE);
    }
    else
    {
      generateDataType(pr, dataType.getDataType());
      if (dataType.getLength() >= 0)
      {
        pr.print('(');
        pr.print(dataType.getLength());
        if (dataType.getPrecision() >= 0)
        {
          pr.print(", ");
          pr.print(dataType.getPrecision());
        }
        pr.print(')');
      }      
    }   
  }

  /**
   * Generates the data type
   * @param pr the writer
   * @param dataType the data type
   */
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    pr.append(dataType.toString());
  }

  /**
   * Prints spaces
   * @param pr
   * @param numberOfSpaces
   */
  protected void writeSpaces(PrintWriter pr, int numberOfSpaces)
  {
    for (int pos = 0; pos < numberOfSpaces; pos++)
    {
      pr.append(" ");
    }
  }

  /**
   * @see IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    System.out.println("SqlScriptGenerator Options: -outputFile {outputFile} [-comment {comment}]");
  }

  /**
   * @return true, if the triggers must be recreated on a table modification
   */
  public boolean isRecreationOfTriggerOnAlterTableNeeded()
  {
    return false;
  }

  /**
   * @return true, if index has to be recreated
   */
  public boolean isRecreationOfUniqueConstraintsOnAlterTableNeeded()
  {
    return false;
  }

  /**
   * Generates the table reorganisation statement, if needed
   * @param pr
   * @param newTable
   */
  @SuppressWarnings("unused")
  public void generateTableReorganisation(PrintWriter pr, SqlTable newTable)
  {
  }

  /**
   * Generates a drop stored procedure statement 
   * @param pr
   * @param storedProcedureId
   */
  public void generateDropStoredProcedures(PrintWriter pr, String storedProcedureId)
  {
    pr.print("DROP PROCEDURE ");
    generateIdentifier(pr, storedProcedureId);
    generateDelimiter(pr);
    pr.println();
  }

  public boolean isRecreationOfIndexesOnAlterTableNeeded()
  {
    return false;
  }

  /**
   * Currently only used for Databases returning true for {@link #isRecreationOfIndexesOnAlterTableNeeded()}
   * This is currently {@link MsSqlServerSqlScriptGenerator}
   * @param pr
   * @param table 
   * @param index
   */
  public void generateDropIndex(PrintWriter pr, SqlTable table, SqlIndex index) 
  {
    pr.print("DROP INDEX ");
    generateIdentifier(pr, table.getId()+"."+getIndexName(index));
    generateDelimiter(pr);
    pr.println(); 
  }

  public boolean isRecreationOfPrimaryKeysOnAlterTableNeeded()
  {
    return false;
  }

  /**
   * Currently only used for Databases returning true for {@link #isRecreationOfPrimaryKeysOnAlterTableNeeded()}
   * This is currently {@link MsSqlServerSqlScriptGenerator}
   * @param pr
   * @param table 
   * @param primaryKey
   * @param createdTemporaryStoredProcedures 
   */
  public void generateDropPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey, @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures)
  {
    pr.print("DROP PRIMARY KEY ");
    generateIdentifier(pr, table.getId()+"."+primaryKey.getId());
    generateDelimiter(pr);
    pr.println();
  }

  public void generateAlterTableAddPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey)
  {
    pr.print("ALTER TABLE ");
    pr.print( table.getId());
    pr.print(" ADD PRIMARY KEY (");
    generateColumnList(pr, primaryKey.getPrimaryKeyColumns());
    pr.append(")");
    generateDelimiter(pr); 
    
  }

  public boolean isRecreationOfForeignKeysOnAlterTableNeeded()
  {
    return false;
  }

  public boolean isRecreationOfDefaultConstrainsNeeded()
  {
    return false;
  }

  public void generateDropDefaultConstraint(PrintWriter pr, SqlTable table, SqlTableColumn col)
  {
    pr.println("DECLARE @defname VARCHAR(100), @cmd VARCHAR(1000)");
    pr.println("SET @defname = ");
    pr.println("(SELECT name"); 
    pr.println("FROM sysobjects so JOIN sysconstraints sc");
    pr.println("ON so.id = sc.constid"); 
    pr.println("WHERE object_name(so.parent_obj) = '"+table.getId()+"'"); 
    pr.println("AND so.xtype = 'D'");
    pr.println("AND sc.colid ="); 
    pr.println(" (SELECT colid FROM syscolumns"); 
    pr.println(" WHERE id = object_id('dbo."+table.getId()+"') AND"); 
    pr.println(" name = '"+col.getId()+"'))");
    pr.println("SET @cmd = 'ALTER TABLE "+table.getId()+" DROP CONSTRAINT '");
    pr.println("+ @defname");
    pr.println("EXEC(@cmd)");
    generateDelimiter(pr);
    pr.println();
  }

  public void generateRecreateDefaultConstraint(PrintWriter pr, SqlTable table, SqlTableColumn col)
  {
    pr.print("ALTER TABLE "+table.getId()+" ADD CONSTRAINT DEF"+table.getId()+col.getId()+" ");
    generateDefaultValue(pr, col);
    pr.print(" FOR "+col.getId());
    generateDelimiter(pr);
    pr.println();
  }
}