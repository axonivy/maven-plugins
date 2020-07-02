package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.ivyteam.db.meta.generator.Target;
import ch.ivyteam.db.meta.generator.internal.mssql.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDatabaseSystemHints;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlInsertWithSelect;
import ch.ivyteam.db.meta.model.internal.SqlInsertWithValues;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlSelect;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlTableContentDefinition;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;

/**
 * Generates sql scripts out of the sql meta information
 * @author rwei
 */
public abstract class SqlScriptGenerator implements IMetaOutputGenerator
{
  /** The output file */
  protected File fOutputFile;

  /** The header comment */
  private String fComment;

  /** Stores the already generated tables */
  private Set<String> fGeneratedTables = new HashSet<>();

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
  
  protected final DbHints dbHints;
  public final Triggers triggers; 
  protected final Delimiter delimiter;
  protected final Spaces spaces = new Spaces();
  public final ForeignKeys foreignKeys;
  public final Identifiers identifiers;
  public final Comments comments;
  public final DmlStatements dmlStatements;
  
  protected SqlScriptGenerator(String databaseSystemName, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    this.dbHints = new DbHints(databaseSystemName);
    this.delimiter = delimiter;
    this.identifiers = identifiers;
    this.comments = comments;
    this.dmlStatements = createDmlStatementsGenerator(dbHints, delimiter, identifiers);
    this.foreignKeys = createForeignKeysGenerator(dbHints, delimiter, identifiers, comments);
    this.triggers = createTriggersGenerator(dbHints, delimiter, dmlStatements, foreignKeys);
  }
    
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
  
  public void setComment(String comment)
  {
    this.fComment = comment;
  }

  protected DmlStatements createDmlStatementsGenerator(DbHints hints, Delimiter delim, Identifiers ident)
  {
    return new DmlStatements(hints, delim, ident);
  }
  
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident, Comments cmmnts)
  {
    return new ForeignKeys(hints, delim, ident, cmmnts);
  }

  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts, ForeignKeys fKeys)
  {
    return new Triggers(hints, delim, dmlStmts, fKeys);
  }
  
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    try (PrintWriter pr = new NewLinePrintWriter(fOutputFile))
    {
      generateHeader(pr);
      generatePrefix(pr);
      generateMetaOutputStatements(pr, metaDefinition);
      generatePostfix(pr);
    }
  }
  
  public void generateMetaOutputStatements(PrintWriter pr, SqlMeta metaDefinition) throws Exception
  {
    List<SqlTable> tables = metaDefinition.getArtifacts(SqlTable.class);
    generateTables(pr, tables);

    for (SqlView view : metaDefinition.getArtifacts(SqlView.class))
    {
      generateView(pr, view);
    }

    triggers.generateCreate(pr, metaDefinition);

    for (SqlInsertWithValues insert : metaDefinition.getArtifacts(SqlInsertWithValues.class))
    {
      dmlStatements.generateInsert(pr, insert);
    }

    for (SqlInsertWithSelect insert : metaDefinition.getArtifacts(SqlInsertWithSelect.class))
    {
      dmlStatements.generateInsertWithSelect(pr, insert);
    }
  }

  /**
   * Generate all given tables
   * @param pr
   * @param tables
   * @throws MetaException
   */
  public final void generateTables(PrintWriter pr, List<SqlTable> tables)
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
        foreignKeys.generateAlterTableAdd(pr, table, foreignKey);
      }
    }
  }

  public abstract void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn);

  public abstract void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable);
  
  public void generateAlterTableDropColumn(PrintWriter pr, SqlTableColumn droppedColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableDropColumn(pr, this, newTable, droppedColumn);    
  }
  
  public void generateDropView(PrintWriter pr, SqlView view)
  {
    pr.print("DROP VIEW ");
    pr.print(view.getId());
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }
  
  public void generateView(PrintWriter pr, SqlView view)
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
      spaces.generate(pr, 2);
      identifiers.generate(pr, column.getId());
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
      dmlStatements.generateSelect(pr, select, 2);
    }
    delimiter.generate(pr);
    pr.println();
    pr.println();
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
      comments.generate(pr,
              "---------------------------------------------------------------------------------");
      comments.generate(pr, "");
      for (String comment : fComment.split("\n"))
      {
        comments.generate(pr, comment);
      }
      comments.generate(pr, "");
    }
    comments.generate(pr,
            "---------------------------------------------------------------------------------");
    comments.generate(pr, "");
    comments.generate(pr,
            "This script was automatically generated. Do not edit it. Instead edit the source file");
    comments.generate(pr, "");
    comments.generate(pr,
            "---------------------------------------------------------------------------------");
    comments.generate(pr, "Database: " + getDatabaseComment());
    comments.generate(pr,
            "---------------------------------------------------------------------------------");
    comments.generate(pr, "Copyright:");
    comments.generate(pr, "AXON IVY AG, Baarerstrasse 12, 6300 Zug");
    comments.generate(pr,
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
    comments.generate(pr, "");
    comments.generate(pr, "Update Version");
    comments.generate(pr, "");
    pr.append("UPDATE IWA_Version SET Version=" + newVersionId);
    delimiter.generate(pr);
    pr.println();
  }
  

  public void generateNonMetaDiffChangesPre(@SuppressWarnings("unused") PrintWriter pr, @SuppressWarnings("unused") int newVersionId)
  {
  }
  
  /**
   * Generates the DDL updates that are not reflected by a meta data difference 
   * @param pr
   * @param metaDefinitionTo 
   * @param metaDefinitionFrom 
   * @param newVersionId
   */
  public void generateNonMetaDiffChangesPost(PrintWriter pr, 
          @SuppressWarnings("unused") SqlMeta metaDefinitionFrom, 
          @SuppressWarnings("unused") SqlMeta metaDefinitionTo, 
          int newVersionId)
  {
    pr.println();
    
    if (newVersionId == 32)
    {
      comments.generate(pr, "Issue 23540: Deletion of a PMV must remove/delete associated library");
      pr.append("DELETE FROM IWA_Library " +
      		"WHERE ProcessModelVersionId IN (SELECT ProcessModelVersionId FROM IWA_ProcessModelVersion WHERE ReleaseState=4)"); // ReleaseState=DELETED    
      delimiter.generate(pr);
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
   * Generate drop table for the given table
   * @param pr
   * @param table
   */
  public final void generateDropTable(PrintWriter pr, SqlTable table)
  {
    pr.write("DROP TABLE ");
    identifiers.generate(pr, table.getId());
    delimiter.generate(pr);
    pr.println(); 
  }

  private void generateTable(PrintWriter pr, SqlTable table, Map<SqlTable, List<SqlForeignKey>> alterTables)
  {
    boolean firstColumn = true;
    generatePreTable(pr, table);
    pr.append("CREATE TABLE ");
    identifiers.generate(pr, table.getId());
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
    if (!foreignKeys.isReferenceInColumnDefinitionSupported())
    {
      for (SqlForeignKey fKey : table.getForeignKeys())
      {
        if (foreignKeys.isSupported(fKey))
        {
          if (isTableAlreadyGenerated(fKey.getReference().getForeignTable()))
          {
            if (!firstColumn)
            {
              pr.append(",\n");
            }
            firstColumn = false;
            foreignKeys.generate(pr, fKey);
          }
          else
          {
            addAlterTableAddForeignKey(alterTables, table, fKey);
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
    delimiter.generate(pr);
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
    List<SqlIndex> tableIndexes = new ArrayList<>();
    for (SqlIndex index : table.getIndexes())
    {
      if (!dbHints.NO_INDEX.isSet(index))
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
    fGeneratedTables.add(tableName);
  }

  /**
   * Is the table with the given name already generated
   * @param tableName the name of the table
   * @return true if the table is already generated false if not.
   */
  protected final boolean isTableAlreadyGenerated(String tableName)
  {
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
    return dbHints.USE_UNIQUE_INDEX.isSet(uniqueConstraint);
  }

  /**
   * Returns if the database system supports unique constraints inside the create table definition
   * @param uniqueConstraint the unique constraint 
   * @return true if it supports, otherwise false
   * @see #isUniqueConstraintOutsideTableSupported(SqlUniqueConstraint)
   */
  public boolean isUniqueConstraintInTableSupported(SqlUniqueConstraint uniqueConstraint)
  {
    return !dbHints.USE_UNIQUE_INDEX.isSet(uniqueConstraint) && !dbHints.NO_UNIQUE.isSet(uniqueConstraint);
  }

  /**
   * Called before a table definition is generated. Subclasses can use this hook to generate things before the
   * table definition is generated
   * @param pr the print writer
   * @param table the table
   * @throws MetaException
   */
  @SuppressWarnings("unused")
  protected void generatePreTable(PrintWriter pr, SqlTable table)
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
  protected void generatePostTable(PrintWriter pr, SqlTable table)
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
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
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
    spaces.generate(pr, 2);
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
    spaces.generate(pr, 2);
    pr.print("INDEX ");
    identifiers.generate(pr, getIndexName(index));
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
      identifiers.generate(pr, column);
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
    identifiers.generate(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    identifiers.generate(pr, table.getId());
    pr.print(" (");
    generateColumnList(pr, index.getColumns());
    pr.print(")");
    delimiter.generate(pr);
    pr.println();
    pr.println();    
  }

  /**
   * @param index
   */
  protected String getIndexName(SqlIndex index)
  {
    return dbHints.INDEX_NAME.valueIfSet(index)
        .orElse(index.getId());
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
    delimiter.generate(pr);
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
      generateUniqueConstraintInTable(pr, table, unique);
    }
    else if (isUniqueConstraintOutsideTableSupported(unique))
    {
      generateUniqueConstraint(pr, table, unique);
    }
  }

  protected void generateUniqueConstraintInTable(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique)
  {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" ADD UNIQUE (");
    generateColumnList(pr, unique.getColumns());
    pr.print(")");
    delimiter.generate(pr);
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
    delimiter.generate(pr);
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
    identifiers.generate(pr, table.getId());
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
    identifiers.generate(pr, table.getId());
    pr.print(" DROP INDEX ");
    identifiers.generate(pr, getUniqueConstraintName(unique));
  }

  /** 
   * @param unique
   * @return unique constraint name
   */
  protected final String getUniqueConstraintName(SqlUniqueConstraint unique)
  {
    return dbHints.USE_UNIQUE_INDEX.valueIfSet(unique)
        .orElse(unique.getId());
  }

  /**
   * Generate primary key
   * @param pr the print writer
   * @param primaryKey the primary key to generate
   * @return true if primary key was generated, false if not
   */
  protected boolean generatePrimaryKey(PrintWriter pr, SqlPrimaryKey primaryKey)
  {
    spaces.generate(pr, 2);
    pr.append("PRIMARY KEY (");
    generateColumnList(pr, primaryKey.getPrimaryKeyColumns());
    pr.append(")");
    return true;
  }

  /**
   * Generates the column
   * @param pr the writer
   * @param table the table
   * @param column the column
   * @param alterTables
   * @throws MetaException 
   */
  public final void generateColumn(PrintWriter pr, SqlTable table, SqlTableColumn column, Map<SqlTable, List<SqlForeignKey>> alterTables)
  {
    spaces.generate(pr, 2);
    identifiers.generate(pr, column.getId());
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
        (foreignKeys.isReferenceInColumnDefinitionSupported())&&            
        (foreignKeys.isSupported(table.findForeignKey(column))))
    {
      if (isTableAlreadyGenerated(column.getReference().getForeignTable()))
      {
        foreignKeys.generateReference(pr, column.getReference(), table.findForeignKey(column));
      }
      else
      {
        addAlterTableAddForeignKey(alterTables, table, table.findForeignKey(column));
      }
    }    
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

  protected void generateDefaultValue(PrintWriter pr, SqlTableColumn column)
  {
    if (column.getDefaultValue() != null)
    {
      pr.append(" DEFAULT ");
      dmlStatements.generateValue(pr, 
          dbHints.DEFAULT_VALUE.valueIfSet(column)
            .map(value -> (Object)value)
            .orElse(column.getDefaultValue().getValue()));
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
    if (dbHints.DATA_TYPE.isSet(artifact))
    {
      dbHints.DATA_TYPE.generate(pr, artifact);
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
   * @see IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    System.out.println("SqlScriptGenerator Options: -outputFile {outputFile} [-comment {comment}]");
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
    identifiers.generate(pr, storedProcedureId);
    delimiter.generate(pr);
    pr.println();
  }

  /**
   * Drops the index
   * @param pr
   * @param table 
   * @param index
   */
  public void generateDropIndex(PrintWriter pr, @SuppressWarnings("unused") SqlTable table, SqlIndex index) 
  {
    pr.print("DROP INDEX ");
    identifiers.generate(pr, getIndexName(index));
    delimiter.generate(pr);
    pr.println(); 
  }

  /**
   * Currently only used for Databases returning true for {@link RecreateOptions#primaryKeysOnAlterTable}
   * This is currently {@link MsSqlServerSqlScriptGenerator}
   * @param pr
   * @param table 
   * @param primaryKey
   * @param createdTemporaryStoredProcedures 
   */
  public void generateDropPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey, @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures)
  {
    pr.print("DROP PRIMARY KEY ");
    identifiers.generate(pr, table.getId()+"."+primaryKey.getId());
    delimiter.generate(pr);
    pr.println();
  }

  public void generateAlterTableAddPrimaryKey(PrintWriter pr, SqlTable table, SqlPrimaryKey primaryKey)
  {
    pr.print("ALTER TABLE ");
    pr.print( table.getId());
    pr.print(" ADD PRIMARY KEY (");
    generateColumnList(pr, primaryKey.getPrimaryKeyColumns());
    pr.append(")");
    delimiter.generate(pr); 
    
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
    delimiter.generate(pr);
    pr.println();
  }

  public void generateRecreateDefaultConstraint(PrintWriter pr, SqlTable table, SqlTableColumn col)
  {
    pr.print("ALTER TABLE "+table.getId()+" ADD CONSTRAINT DEF"+table.getId()+col.getId()+" ");
    generateDefaultValue(pr, col);
    pr.print(" FOR "+col.getId());
    delimiter.generate(pr);
    pr.println();
  }
  
  public RecreateOptions getRecreateOptions()
  {
    return new RecreateOptions();
  }
  
  public static class RecreateOptions
  {
    public boolean defaultConstraints = false;
    public boolean foreignKeysOnAlterTable = false;
    public boolean primaryKeysOnAlterTable = false;
    public boolean indexesOnAlterTable = false;
    public boolean triggerOnAlterTable = false;
    
    public boolean uniqueConstraintsOnAlterTable = false;
    public boolean allUniqueConstraintsOnAlterTable = false;
  }
}