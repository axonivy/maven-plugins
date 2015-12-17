package ch.ivyteam.db.meta.generator;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlInsert;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

/**
 * Generates a SQL script to convert from one meta-model to the other meta-model.<br>
 * Supported conversion:
 * <ul>
 *   <li>Column length definition change (fields could be used in trigger, views and indexes) </li>
 * </ul>
 * @author fs
 * @since 30.11.2011
 */
public class MetaOutputDifferenceGenerator
{  
  private final SqlScriptGenerator generator;
  private final int newVersionId;
  private final SqlMeta metaDefinitionFrom;
  private final SqlMeta metaDefinitionTo;   
  private List<String> createdTemporaryStoredProcedures = new ArrayList<String>();

  /**
   * @param generatorClassName
   * @return -
   * @exception IllegalArgumentException If the class could not been instantiated
   */
  public static SqlScriptGenerator findGeneratorClass(String generatorClassName)
  {
    try
    {
      Class<?> generatorClass = Class.forName(generatorClassName);
      if (SqlScriptGenerator.class.isAssignableFrom(generatorClass))
      {
        return (SqlScriptGenerator) generatorClass.newInstance();
      }
      else
      {
        throw new IllegalArgumentException("Class is not assignable for " + SqlScriptGenerator.class.getName());
      }
    }
    catch (IllegalArgumentException ex) 
    {
      throw ex;
    }
    catch (Exception ex) 
    {
      throw new IllegalArgumentException("Could not find or instantiate generator class: " + generatorClassName, ex);
    }
  }

  /** 
   * Parses the old meta definition
   * @param file 
   * @return old meta definition
   * @throws Exception 
   */
  public static SqlMeta parseMetaDefinition(File file) throws Exception
  {
    FileReader fr = new FileReader(file);
    try
    {
      Parser parser = new Parser(new Scanner(fr));
      return (SqlMeta)parser.parse().value;
    }
    finally
    {
      IOUtils.closeQuietly(fr);
    }
  }
  
  /**
   * Constructor
   * @param metaDefinitionFrom 
   * @param metaDefinitionTo 
   * @param generator 
   * @param newVersionId 
   */
  public MetaOutputDifferenceGenerator(SqlMeta metaDefinitionFrom, SqlMeta metaDefinitionTo, SqlScriptGenerator generator, int newVersionId)
  {
    this.metaDefinitionFrom = metaDefinitionFrom;
    this.metaDefinitionTo = metaDefinitionTo;
    this.generator = generator;
    this.newVersionId = newVersionId;
    generator.setComment("Converts the ivy system database from version " + (newVersionId-1) + " to version " + newVersionId);
  }

  /** 
   * Generates the conversion script
   * @param pr 
   * @throws Exception 
   */
  public void generate(PrintWriter pr) throws Exception
  {
    generator.generateHeader(pr);
    
    generator.generateNonMetaDiffChangesPre(pr, newVersionId);
    
    generateDropViewOfChangedTables(pr);
    generateDropTriggersOfChangedTables(pr);
    generateDropForeignKeysOfChangedColumns(pr);
    
    generateDropTableOfDeletedTables(pr);
    generateCreateTablesOfAddedTables(pr);
    
    generateTableModifications(pr);
    generateCreateIndexOfAddedIndexes(pr);
    
    generateCreateTriggersOfAddedTriggers(pr);
    generateRecreateForeignKeysOfChangedColumns(pr);
    generateRecreateTriggersOfChangedTables(pr);
    generateCreateViewOfChangedTables(pr);
    generateCreateViewOfAddedViews(pr);
    generateDeletesOfRemovedInserts(pr);
    generateInsertsOfNewAddedInserts(pr);
    
    generator.generateNonMetaDiffChangesPost(pr, newVersionId);
    generateDropTemporaryStoredProcedures(pr);
    generator.generateVersionUpdate(pr, newVersionId);
  }

  private void generateRecreateForeignKeysOfChangedColumns(PrintWriter pr)
  {
    if (!generator.getRecreateOptions().foreignKeysOnAlterTable)
    {
      return;
    }
    List<Pair<SqlTable, SqlForeignKey>> referencingColumns = getForeignKeysReferencingChangedColumns();
    for (Pair<SqlTable, SqlForeignKey> pair : referencingColumns)
    {
      if (generator.isForeignKeySupported(pair.getRight()))
      {
        generator.generateAlterTableAddForeignKey(pr, pair.getLeft(), pair.getRight());
      }
    }
  }

  private void generateDropForeignKeysOfChangedColumns(PrintWriter pr)
  {
    if (!generator.getRecreateOptions().foreignKeysOnAlterTable)
    {
      return;
    }

    List<Pair<SqlTable, SqlForeignKey>> referencingColumns = getForeignKeysReferencingChangedColumns();
    for (Pair<SqlTable, SqlForeignKey> pair : referencingColumns)
    {
      if (generator.isForeignKeySupported(pair.getRight()))
      {
        generator.generateAlterTableDropForeignKey(pr, pair.getLeft(), pair.getRight(),
                createdTemporaryStoredProcedures);
      }
    }
  }
  
  private List<Pair<SqlTable, SqlForeignKey>> getForeignKeysReferencingChangedColumns()
  {
    List<Pair<SqlTable, SqlForeignKey>> foreignKeys = new ArrayList<Pair<SqlTable, SqlForeignKey>>();
    for (Entry<SqlTable, SqlTable> changedTable : findChangedTables().entrySet())
    {
      Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(changedTable.getKey(), changedTable.getValue());
      for (Entry<SqlTableColumn, SqlTableColumn> changedColumn : changedColumns.entrySet())
      {
        List<Pair<SqlTable, SqlForeignKey>> referencingColumns = metaDefinitionFrom.getReferencingForeignKeys(changedTable.getKey(), changedColumn.getKey());
        foreignKeys.addAll(referencingColumns);
      }
    }    
    return foreignKeys;
  }


  /**
   * @param pr
   */
  private void generateDropTemporaryStoredProcedures(PrintWriter pr)
  {
    if (createdTemporaryStoredProcedures.isEmpty())
    {
      return;
    }
    generator.generateCommentLine(pr, "Drop temporary created stored procedures needed for conversion");
    for (String storedProcedureId : createdTemporaryStoredProcedures)
    {
      generator.generateDropStoredProcedures(pr, storedProcedureId);
    }
  }

  private void generateDeletesOfRemovedInserts(PrintWriter pr)
  {
    List<SqlInsert> fromSqlInserts = metaDefinitionFrom.getArtifacts(SqlInsert.class);
    List<SqlInsert> toSqlInserts = metaDefinitionTo.getArtifacts(SqlInsert.class);
    List<String> toInsertStmts = getInsertStmts(toSqlInserts);
    
    boolean first = true;
    for (SqlInsert fromSqlInsert : fromSqlInserts)
    {
      String fromInsertStmt = getInsertStmt(fromSqlInsert);
      if (!toInsertStmts.contains(fromInsertStmt))
      {
        if (first)
        {
          generator.generateCommentLine(pr, "Delete removed default table content");
        }
        first = false;
        generator.generateDelete(pr, fromSqlInsert);
      }
    }    
  }
  private void generateInsertsOfNewAddedInserts(PrintWriter pr)
  {
    List<SqlInsert> fromSqlInserts = metaDefinitionFrom.getArtifacts(SqlInsert.class);
    List<String> fromInsertStmts = getInsertStmts(fromSqlInserts);
    List<SqlInsert> toSqlInserts = metaDefinitionTo.getArtifacts(SqlInsert.class);
    List<String> toInsertStmts = getInsertStmts(toSqlInserts);

    boolean first = true;
    for (String toInsertStmt : toInsertStmts)
    {
      if (!fromInsertStmts.contains(toInsertStmt))
      {
        if (first)
        {
          generator.generateCommentLine(pr, "Add new added default table content");
        }
        first = false;
        pr.append(toInsertStmt);
      }
    }    
  }

  private List<String> getInsertStmts(List<SqlInsert> sqlInserts)
  {
    List<String> insterts = new ArrayList<String>(sqlInserts.size());
    
    for(SqlInsert sqlInsert : sqlInserts)
    {
      String insertStmt = getInsertStmt(sqlInsert);
      insterts.add(insertStmt);      
    }
    return insterts;
  }

  private String getInsertStmt(SqlInsert sqlInsert)
  {
    StringWriter output = new StringWriter();
    generator.generateInsert(new PrintWriter(output), sqlInsert);
    return output.toString();
  }

  private void generateCreateIndexOfAddedIndexes(PrintWriter pr)
  {
    for (Entry<SqlTable, SqlTable> changedTable : findTablesWithAddedIndexes().entrySet())
    {
      SqlTable newTable = changedTable.getKey();
      SqlTable oldTable = changedTable.getValue();
      generateCreateIndexes(pr, newTable, oldTable);    
    }
  }

  private Map<SqlTable, SqlTable> findTablesWithAddedIndexes()
  {
    Map<SqlTable, SqlTable> tables = new LinkedHashMap<SqlTable, SqlTable>();
    for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
    {
      SqlTable newTable = comonTable.getKey();
      SqlTable oldTable = comonTable.getValue();

      boolean hasAddedIndexes = findAddedIndexes(newTable, oldTable).size() > 0;
      if (hasAddedIndexes)
      {
        tables.put(newTable, oldTable);
      }      
    }
    return tables;
  }

  private void generateDropTriggersOfChangedTables(PrintWriter pr) throws MetaException
  {
    Map<SqlTable, SqlTable> tablesWithChangedTriggers = findTablesWithChangedTriggers();
    
    for (SqlTable oldTableWithTrigger : tablesWithChangedTriggers.values())
    {
      pr.println();
      generator.generateCommentLine(pr, "Drop trigger which depend on changed table(s)");
      generator.generateDropTrigger(pr, oldTableWithTrigger);
    }
  }
  
  private void generateCreateTriggersOfAddedTriggers(PrintWriter pr) throws MetaException
  {
    Map<SqlTable, SqlTable> tablesWithAddedTriggers = findTablesWithAddedTriggers();
    for (SqlTable newTableWithTrigger : tablesWithAddedTriggers.keySet())
    {
      pr.println();
      generator.generateCommentLine(pr, "Create new triggers on existing table(s)");
      generateTrigger(pr, newTableWithTrigger, metaDefinitionTo);
    }
  }
  
  private void generateRecreateTriggersOfChangedTables(PrintWriter pr) throws MetaException
  {
    Map<SqlTable, SqlTable> tablesWithChangedTriggers = findTablesWithChangedTriggers();

    for (SqlTable newTableWithTrigger : tablesWithChangedTriggers.keySet())
    {
      pr.println();
      generator.generateCommentLine(pr, "Recreate trigger which depend on changed table(s)");
      generateTrigger(pr, newTableWithTrigger, metaDefinitionTo);
    }
  }
  
  private Map<SqlTable, SqlTable> findTablesWithAddedTriggers() throws MetaException
  {
    Map<SqlTable, SqlTable> tablesOfAddedTriggers = new LinkedHashMap<SqlTable, SqlTable>();
    for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
    {
      SqlTable newTable = comonTable.getKey();
      SqlTable oldTable = comonTable.getValue();
      if (hasAddedTrigger(newTable, oldTable))
      {
        tablesOfAddedTriggers.put(newTable, oldTable);
      }
    }
    return tablesOfAddedTriggers;
  }

  private Map<SqlTable, SqlTable> findTablesWithChangedTriggers() throws MetaException
  {
    Map<SqlTable, SqlTable> tablesOfChangedTriggers = new LinkedHashMap<SqlTable, SqlTable>();
    // fist check all common tables if they have a trigger which has changed
    for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
    {
      SqlTable newTable = comonTable.getKey();
      SqlTable oldTable = comonTable.getValue();
      if (hasTriggerChanged(newTable, oldTable))
      {
        tablesOfChangedTriggers.put(newTable, oldTable);
      }
    }

    // if recreation of trigger is needed on table modification check if changed tables have triggers
    if (generator.getRecreateOptions().triggerOnAlterTable)
    {
      for (Entry<SqlTable, SqlTable> comonTable : findChangedTables().entrySet())
      {
        SqlTable newTable = comonTable.getKey();
        SqlTable oldTable = comonTable.getValue();
        if (generator.hasTrigger(metaDefinitionTo, newTable))
        {
          tablesOfChangedTriggers.put(newTable, oldTable);
        }
      }
    }
    return tablesOfChangedTriggers;
  }
  
  private void generateDropTableOfDeletedTables(PrintWriter pr) throws MetaException
  {
    List<SqlTable> deletedTables = findDeletedTables();
    if (deletedTables.isEmpty())
    {
      return;
    }
    
    for (SqlTable deletedTable : deletedTables)
    {
      if (generator.hasTrigger(metaDefinitionFrom, deletedTable))
      {
        pr.println();
        generator.generateCommentLine(pr, "Drop trigger of no longer exisiting table");
        generator.generateDropTrigger(pr, deletedTable);
      }
    }
    
    for (SqlTable deletedTable : deletedTables)
    {
      pr.println();
      generator.generateCommentLine(pr, "Drop no longer exisiting table");
      generator.generateDropTable(pr, deletedTable);
    }
  }

  private void generateCreateTablesOfAddedTables(PrintWriter pr) throws MetaException
  {
    registerOldTables();
    
    List<SqlTable> addedTables = findAddedTables();
    if (addedTables.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create new added tables");
      generator.generateTables(pr, addedTables);
    }
    
    for (SqlTable addedTable : addedTables)
    {
      if (generator.hasTrigger(metaDefinitionTo, addedTable))
      {
        pr.println();
        generator.generateCommentLine(pr, "Create trigger which depend on new added table");
        generateTrigger(pr, addedTable, metaDefinitionTo);
      }
    }
  }

  private void registerOldTables()
  {
    List<SqlTable> oldTables = metaDefinitionFrom.getArtifacts(SqlTable.class);
    for (SqlTable oldTable : oldTables)
    {
      generator.addGeneratedTable(oldTable.getId());
    }
  }

  private void generateTableModifications(PrintWriter pr) throws MetaException
  {
    for (Entry<SqlTable, SqlTable> changedTable : findChangedTables().entrySet())
    {
      SqlTable newTable = changedTable.getKey();
      SqlTable oldTable = changedTable.getValue();
      generateDeleteUniqueConstraints(pr, newTable, oldTable);
      generateDropIndexes(pr, newTable, oldTable);
      generateDropPrimaryKeys(pr, newTable, oldTable);
      generateDropDefaultConstraints(pr, newTable, oldTable);
      generateAlterTable(pr, newTable, oldTable);
      generateAlterForeignKeys(pr, newTable, oldTable);
      generateRecreateDefaultConstraints(pr, newTable, oldTable);
      generateRecreatePrimaryKeys(pr, newTable, oldTable);
      generateRecreateIndexes(pr, newTable, oldTable);
      generateRecreateUniqueConstraints(pr, newTable, oldTable);
      generateTableReorganisation(pr, newTable, oldTable);
    }
  }

  private void generateRecreateDefaultConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().defaultConstraints)
    {
      return;
    }
    for (SqlTableColumn col : getChangedColumnsWithDefaultConstraint(newTable, oldTable))
    {
      generator.generateRecreateDefaultConstraint(pr, oldTable, col);
    }
  }

  private void generateDropDefaultConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().defaultConstraints)
    {
      return;
    }
    for (SqlTableColumn col : getChangedColumnsWithDefaultConstraint(newTable, oldTable))
    {
      generator.generateDropDefaultConstraint(pr, oldTable, col);
    }
  }

  private List<SqlTableColumn> getChangedColumnsWithDefaultConstraint(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> result = new ArrayList<SqlTableColumn>();
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    for (SqlTableColumn changedColumn : changedColumns.keySet())
    {
      if (changedColumn.getDefaultValue() != null)
      {
        result.add(changedColumn);
      }
    }
    return result;
  }

  private void generateAlterForeignKeys(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (generator.isForeignKeyReferenceInColumnDefinitionSupported())
    {
      return;
    }
   
    List<SqlForeignKey> addedForeignKeys = findAddedForeignKeys(newTable, oldTable);
    if (addedForeignKeys.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create added foreign keys of table " + newTable.getId());
      for (SqlForeignKey foreignKey : addedForeignKeys)
      {
        generator.generateAlterTableAddForeignKey(pr, newTable, foreignKey);
      }
    }
  }

  private List<SqlForeignKey> findAddedForeignKeys(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlForeignKey> addedForeignKeys = new ArrayList<SqlForeignKey>();
    List<SqlForeignKey> newForeignKeys = newTable.getForeignKeys();
    for (SqlForeignKey newForeignKey : newForeignKeys)
    {
      if (generator.isForeignKeySupported(newForeignKey))
      {
        SqlForeignKey oldForeignKey = oldTable.findForeignKey(newForeignKey.getId());
        if (oldForeignKey == null || generator.isForeignKeySupported(oldForeignKey) == false)
        {
          addedForeignKeys.add(newForeignKey);
        }
      }
    }
    return addedForeignKeys;
  }

  private void generateAlterTable(PrintWriter pr, SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    generateAlterTableAlterColumns(pr, newTable, oldTable);
    generateAlterTableAddColumns(pr, newTable, oldTable);
  }
  
  private void generateAlterTableAlterColumns(PrintWriter pr, SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    if (changedColumns.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Changed columns of table " + newTable.getId());
      for (Entry<SqlTableColumn, SqlTableColumn> changedColumn : changedColumns.entrySet())
      {
        SqlTableColumn newColumn = changedColumn.getKey();
        SqlTableColumn oldColumn = changedColumn.getValue();
        generator.generateAlterTableAlterColumn(pr, newColumn, newTable, oldColumn);
        pr.println();
      }
    }
  }
  
  private void generateAlterTableAddColumns(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> addedColumns = findAddedColumns(newTable, oldTable);
    if (addedColumns.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Added columns of table " + newTable.getId());
      for (SqlTableColumn addedColumn : addedColumns)
      {
        generator.generateAlterTableAddColumn(pr, addedColumn, newTable);
        pr.println();
      }
    }
  }

  private void generateDropViewOfChangedTables(PrintWriter pr) throws MetaException
  {
    Set<SqlView> changedViews = findChangedViews();
    if (changedViews.size() > 0)
    {
      generator.generateCommentLine(pr, "Drop views which depend on changed tables");
      for (SqlView sqlView : changedViews)
      {
        generator.generateDropView(pr, sqlView);
      }
    }
  }

  private void generateCreateViewOfChangedTables(PrintWriter pr) throws MetaException
  {
    Set<SqlView> changedViews = findChangedViews();
    if (changedViews.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Recreate views which depend on changed tables");
      for (SqlView sqlView : changedViews)
      {
        generator.generateView(pr, sqlView);
      }
    }
  }
  
  private void generateCreateViewOfAddedViews(PrintWriter pr) throws MetaException
  {
    Set<SqlView> addedViews = findAddedViews();
    if (addedViews.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create added views");
      for (SqlView sqlView : addedViews)
      {
        generator.generateView(pr, sqlView);
      }
    }
  }

  private void generateCreateIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    List<SqlIndex> addedIndexes = findAddedIndexes(newTable, oldTable);
    if (addedIndexes.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create new indexes of table " + newTable.getId());
      for (SqlIndex addedIndex : addedIndexes)
      {
        generator.generateIndex(pr, newTable, addedIndex);
      }
    }
  }
  
  private List<SqlIndex> findAddedIndexes(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlIndex> addedIndexes = new ArrayList<SqlIndex>();
    for (SqlIndex newIndex : newTable.getIndexes())
    {
      SqlIndex oldIndex = oldTable.findIndex(newIndex.getId());
      if (oldIndex == null)
      {
        addedIndexes.add(newIndex);
      }
    }
    return addedIndexes;
  }
  
  private void generateTableReorganisation(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    if (changedColumns.size() > 0)
    {
      pr.println();
      generator.generateTableReorganisation(pr, newTable);
    }
  }
  
  private boolean hasTriggerChanged(SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    if (newTable == null || oldTable == null)
    {
      return true;
    }
    
    StringWriter fromOut = new StringWriter();
    StringWriter toOut = new StringWriter();

    generateTrigger(new PrintWriter(fromOut), newTable, metaDefinitionFrom);
    generateTrigger(new PrintWriter(toOut), oldTable, metaDefinitionTo);
    
    if (StringUtils.isBlank(toOut.toString()) || StringUtils.isBlank(fromOut.toString()))
    {
      // trigger was deleted or added (not changed)
      return false;
    }
    return !toOut.toString().equals(fromOut.toString()); 
  }
  
  private boolean hasAddedTrigger(SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    if (newTable == null || oldTable == null)
    {
      return true;
    }

    StringWriter fromOut = new StringWriter();
    StringWriter toOut = new StringWriter();

    generateTrigger(new PrintWriter(fromOut), newTable, metaDefinitionFrom);
    generateTrigger(new PrintWriter(toOut), oldTable, metaDefinitionTo);
    
    if (StringUtils.isNotEmpty(toOut.toString()) && StringUtils.isBlank(fromOut.toString()))
    {
      return true;
    }
    return false; 
  }
  
  
  private void generateTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    generator.genrateForEachStatementDeleteTrigger(pr, table, metaDefinition);
    generator.generateForEachRowDeleteTrigger(pr, table, metaDefinition);
  }
  
  private void generateDeleteUniqueConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    Set<SqlUniqueConstraint> changedUniqueConstraints = new LinkedHashSet<SqlUniqueConstraint>();
    if (generator.getRecreateOptions().uniqueConstraintsOnAlterTable)
    {
      if (generator.getRecreateOptions().allUniqueConstraintsOnAlterTable)
      {
        changedUniqueConstraints.addAll(newTable.getUniqueConstraints());
      }
      else
      {
        changedUniqueConstraints.addAll(getUniqueConstraintsFromChangedColumns(newTable, oldTable));
      }
    }
    
    changedUniqueConstraints.addAll(findDeletedUniqueConstraints(newTable, oldTable));
    if (changedUniqueConstraints.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Drop unique constraint which depend on changed columns or was deleted");
      for (SqlUniqueConstraint uniqueConstraint : changedUniqueConstraints)
      {
        generator.generateDropUniqueConstraint(pr, oldTable, uniqueConstraint, createdTemporaryStoredProcedures);
      }
    }
  }
  
  private Set<SqlUniqueConstraint> findDeletedUniqueConstraints(SqlTable newTable,
          SqlTable oldTable)
  {
    Set<SqlUniqueConstraint> deletedUniqueConstraints = new LinkedHashSet<SqlUniqueConstraint>();
    for (SqlUniqueConstraint oldConstraint : oldTable.getUniqueConstraints())
    {
      SqlUniqueConstraint newConstraint = newTable.findUniqueConstraint(oldConstraint.getId());
      if (newConstraint == null)
      {
        deletedUniqueConstraints.add(oldConstraint);
      }      
    }
    return deletedUniqueConstraints;
  }

  private void generateRecreateUniqueConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    if (!generator.getRecreateOptions().uniqueConstraintsOnAlterTable)
    {
      return;
    }
    
    Set<SqlUniqueConstraint> uniqueConstraints = new HashSet<>();
    if (generator.getRecreateOptions().allUniqueConstraintsOnAlterTable)
    {
      uniqueConstraints.addAll(newTable.getUniqueConstraints());
    }
    else
    {
      uniqueConstraints.addAll(getUniqueConstraintsFromChangedColumns(newTable, oldTable));
    }
    
    generateCreateUniqueConstraints(pr, newTable, uniqueConstraints);
  }
  

  private void generateCreateUniqueConstraints(PrintWriter pr, SqlTable newTable, Collection<SqlUniqueConstraint> uniqueConstraints)
  {
    if (uniqueConstraints.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create unique constraints which depend on changed columns");
      for (SqlUniqueConstraint uniqueConstraint : uniqueConstraints)
      {
        generator.generateAddUniqueConstraint(pr, newTable, uniqueConstraint);
      }
    }
  }

  private Set<SqlUniqueConstraint> getUniqueConstraintsFromChangedColumns(SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    List<String> changedColumNames = new ArrayList<String>();
    for (Entry<SqlTableColumn, SqlTableColumn> changedColumn : findChangedColumns(newTable, oldTable).entrySet())
    {
      SqlTableColumn newColumn = changedColumn.getKey();
      SqlTableColumn oldColumn = changedColumn.getValue();
      if (oldColumn != null)  // null if column not exists in old version!
      {
        changedColumNames.add(newColumn.getId());
      }
    }

    return getUniqeConstraints(newTable, changedColumNames);
  }

  /**
   * @return unique constraints form the given table depending on the given column names
   */
  private Set<SqlUniqueConstraint> getUniqeConstraints(SqlTable tables, List<String> columNames)
  {
    if (columNames.isEmpty())
    {
      return Collections.emptySet();
    }

    Set<SqlUniqueConstraint> dependentUniqueConstraints = new LinkedHashSet<SqlUniqueConstraint>(); 
    for (SqlUniqueConstraint uniqueConstraint : tables.getUniqueConstraints())
    {
      for (String constraintColumn : uniqueConstraint.getColumns())
      {
        if (columNames.contains(constraintColumn))
        {
          dependentUniqueConstraints.add(uniqueConstraint);
          break;
        }
      }
    }
    return dependentUniqueConstraints;
  }
  
  private void generateDropIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().indexesOnAlterTable)
    {
      return;
    }
    
    Set<SqlIndex> changedIndexes = getIndexesFromChangedColumns(newTable, oldTable);
    if (changedIndexes.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Drop indexes which depend on changed columns");
      for (SqlIndex index : changedIndexes)
      {
        generator.generateDropIndex(pr, oldTable, index);
      }
    }
  }
  
  
  private Set<SqlIndex> getIndexesFromChangedColumns(SqlTable newTable, SqlTable oldTable)
  {
    Set<SqlIndex> result = new LinkedHashSet<SqlIndex>();
    List<SqlIndex> indexes = generator.getIndexes(oldTable);
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    for (SqlTableColumn changedColumn : changedColumns.keySet())
    {
      for (SqlIndex index : indexes)
      {
        if (index.getColumns().contains(changedColumn.getId()))
        {
          result.add(index);
        }
      }
    }
    return result;
  }

  private void generateRecreateIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().indexesOnAlterTable)
    {
      return;
    }
    
    Set<SqlIndex> changedIndexes = getIndexesFromChangedColumns(newTable, oldTable);
    if (changedIndexes.size() > 0)
    {
      pr.println();
      generator.generateCommentLine(pr, "Create index which depend on changed columns");
      for (SqlIndex index : changedIndexes)
      {
        generator.generateIndex(pr, newTable, index);
      }
    }
  }
  
  private void generateDropPrimaryKeys(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().primaryKeysOnAlterTable)
    {
      return;
    }
    
    SqlPrimaryKey changedPrimaryKey = getPrimaryKeysFromChangedColumns(newTable, oldTable);
    if (changedPrimaryKey != null)
    {
      generator.generateDropPrimaryKey(pr, oldTable, changedPrimaryKey, createdTemporaryStoredProcedures);
    }
  }
  
  
  private SqlPrimaryKey getPrimaryKeysFromChangedColumns(SqlTable newTable, SqlTable oldTable)
  {
    SqlPrimaryKey primaryKey = oldTable.getPrimaryKey();
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    for (SqlTableColumn changedColumn : changedColumns.keySet())
    {
        if (primaryKey.getPrimaryKeyColumns().contains(changedColumn.getId()))
        {
           return primaryKey;
        }
    }
    return null;
  }

  private void generateRecreatePrimaryKeys(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    if (!generator.getRecreateOptions().primaryKeysOnAlterTable)
    {
      return;
    }
    
    SqlPrimaryKey changedPrimaryKey = getPrimaryKeysFromChangedColumns(newTable, oldTable);
    if (changedPrimaryKey != null)
    {
        generator.generateAlterTableAddPrimaryKey(pr, oldTable, changedPrimaryKey);
    }
  }
  
  /**
   * Returns a list of views, which are used in at least one changed table
   * @return never null
   * @throws MetaException 
   */
  private Set<SqlView> findChangedViews() throws MetaException
  {
    Map<SqlTable, SqlTable> changedTables = findChangedTables();
    List<SqlView> views = metaDefinitionTo.getArtifacts(SqlView.class);
    Set<SqlView> changedViews = new LinkedHashSet<SqlView>();
    for (SqlView sqlView : views)
    {
      // only recreated view if it was already there in old version
      if (metaDefinitionFrom.findView(sqlView.getId())!=null)
      {
        Set<String> viewTables = sqlView.getTables();
        for (SqlTable table : changedTables.keySet())
        {
          if (viewTables.contains(table.getId()))
          {
            // A table which has changed, are used in this view
            changedViews.add(sqlView);
          }
        }
      }
    }
    return changedViews;
  }

  /**
   * @return tables which are added in the new meta definition (does not exist in the new meta definition)
   */
  private List<SqlTable> findAddedTables()
  {
    List<SqlTable> newTables = new ArrayList<SqlTable>(metaDefinitionTo.getArtifacts(SqlTable.class));
    List<SqlTable> addedTables = new ArrayList<SqlTable>();
    for (SqlTable newTable: newTables)
    {
      SqlTable oldTable = metaDefinitionFrom.findTable(newTable.getId());
      if (oldTable == null)
      {
        addedTables.add(newTable);
      }
    }
    return addedTables;
  }
  
  private Set<SqlView> findAddedViews()
  {
    List<SqlView> newViews = new ArrayList<SqlView>(metaDefinitionTo.getArtifacts(SqlView.class));
    Set<SqlView> addedViews = new LinkedHashSet<SqlView>();
    for (SqlView newView: newViews)
    {
      SqlView oldView = metaDefinitionFrom.findView(newView.getId());
      if (oldView == null)
      {
        addedViews.add(newView);
      }
    }
    return addedViews;
  }

  
  /**
   * @return tables which are deleted in the new meta definition (only exists in old meta definition)
   */
  private List<SqlTable> findDeletedTables()
  {
    List<SqlTable> oldTables = new ArrayList<SqlTable>(metaDefinitionFrom.getArtifacts(SqlTable.class));
    List<SqlTable> deletedTables = new ArrayList<SqlTable>();
    for (SqlTable oldTable: oldTables)
    {
      SqlTable newTable = metaDefinitionTo.findTable(oldTable.getId());
      if (newTable == null)
      {
        deletedTables.add(oldTable);
      }
    }
    return deletedTables;
  }

  /**
   * Tables which exists in old and new meta definition
   * @return map with new and old tables with the same id
   */
  private Map<SqlTable, SqlTable> findCommonTables()
  {
    List<SqlTable> oldTables = new ArrayList<SqlTable>(metaDefinitionFrom.getArtifacts(SqlTable.class));
    Map<SqlTable, SqlTable> tables = new LinkedHashMap<SqlTable, SqlTable>();
    for (SqlTable oldTable: oldTables)
    {
      SqlTable newTable = metaDefinitionTo.findTable(oldTable.getId());
      if (newTable != null)
      {
        tables.put(newTable, oldTable);
      }
    }
    return tables;
  }
  
  /** 
   * @return map with new and old tables of changed tables (tables with same id and added or changed columns)
   */
  private Map<SqlTable, SqlTable> findChangedTables() throws MetaException
  {
    Map<SqlTable, SqlTable> tables = new LinkedHashMap<SqlTable, SqlTable>();
    for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
    {
      SqlTable newTable = comonTable.getKey();
      SqlTable oldTable = comonTable.getValue();

      boolean hasChangedColumns = findChangedColumns(newTable, oldTable).size() > 0;
      boolean hasAddedColumns = findAddedColumns(newTable, oldTable).size() > 0;
      boolean hasDeletedUniqueConstraints = findDeletedUniqueConstraints(newTable, oldTable).size() > 0;
      if (hasChangedColumns || hasAddedColumns || hasDeletedUniqueConstraints)
      {
        tables.put(newTable, oldTable);
      }
    }
    return tables;
  }

  /**
   * Finds changed columns (deleted and added are not collected)
   * @param newTable the new table
   * @param oldTable the old table
   * @return map with new and old columns of changed columns of a table
   * @throws MetaException 
   */
  private Map<SqlTableColumn, SqlTableColumn> findChangedColumns(SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    List<SqlTableColumn> oldColumns = oldTable.getColumns();
    Map<SqlTableColumn, SqlTableColumn> changedColumns = new LinkedHashMap<SqlTableColumn, SqlTableColumn>(oldColumns.size());
    for (SqlTableColumn oldColumn: oldColumns)
    {
      SqlTableColumn newColumn = newTable.findColumn(oldColumn.getId());
      if (newColumn != null)
      {
        if (hasColumnChanged(newTable, newColumn, oldColumn))
        {
          changedColumns.put(newColumn, oldColumn);
        }
      }
    }
    return changedColumns;
  }
  
  private List<SqlTableColumn> findAddedColumns(SqlTable newTable, SqlTable oldTable) throws MetaException
  {
    List<SqlTableColumn> addedColumns = new ArrayList<SqlTableColumn>();
    for (SqlTableColumn newColumn : newTable.getColumns())
    {
      SqlTableColumn oldColumn = oldTable.findColumn(newColumn.getId());
      if (oldColumn == null)
      {
        addedColumns.add(newColumn);
      }
    }
    return addedColumns;
  }

  private boolean hasColumnChanged(SqlTable newTable, SqlTableColumn newColumn, SqlTableColumn oldColumn) throws MetaException
  {
    if (!newColumn.getId().equals(oldColumn.getId()))
    {
      throw new UnsupportedOperationException("Changing of the column name is not supported.");
    }
    
    StringWriter fromOut = new StringWriter();
    StringWriter toOut = new StringWriter();

    generator.generateColumn(new PrintWriter(fromOut), newTable, oldColumn, new LinkedHashMap<SqlTable, List<SqlForeignKey>>());
    generator.generateColumn(new PrintWriter(toOut), newTable, newColumn, new LinkedHashMap<SqlTable, List<SqlForeignKey>>());
    
    return !toOut.toString().equals(fromOut.toString());
  }
}