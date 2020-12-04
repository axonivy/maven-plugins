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
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.generator.internal.NotReferencedTablesFirstComparator;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlInsertWithValues;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlPrimaryKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

/**
 * Generates a SQL script to convert from one meta-model to the other meta-model.<br>
 * @author fs
 * @since 30.11.2011
 */
public class MetaOutputDifferenceGenerator
{  
  private static final String VERSION_TABLE = "IWA_Version";
  private final SqlScriptGenerator generator;
  private final int newVersionId;
  private final SqlMeta metaDefinitionFrom;
  private final SqlMeta metaDefinitionTo;   
  private List<String> createdTemporaryStoredProcedures = new ArrayList<>();
  private SqlMeta additionalConversionMeta;
  
  private final IndexGenerator indexes = new IndexGenerator();
  private final TriggerGenerator triggers = new TriggerGenerator();
  private final ConstraintGenerator constraints = new ConstraintGenerator();
  private final ForeignKeyGenerator foreignKeys = new ForeignKeyGenerator();
  
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
   * Parses the meta definitions
   * @param files 
   * @return meta definition
   * @throws Exception 
   */
  public static SqlMeta parseMetaDefinitions(File... files) throws Exception
  {
    SqlMeta overallMeta = null;
    for (File file: files)
    {
      SqlMeta meta = parseMetaDefinition(file);
      if (overallMeta == null)
      {
        overallMeta = meta;
      }
      else
      {
        overallMeta.merge(meta);
      }
    }
    return overallMeta;
  }

  private static SqlMeta parseMetaDefinition(File file) throws Exception
  {
    if (file == null || !file.exists())
    {
      return null;
    }
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
  
  public MetaOutputDifferenceGenerator(SqlMeta metaDefinitionFrom, SqlMeta metaDefinitionTo, SqlMeta additionalConversionMeta, SqlScriptGenerator generator, int newVersionId)
  {
    this.metaDefinitionFrom = metaDefinitionFrom;
    this.metaDefinitionTo = metaDefinitionTo;
    this.additionalConversionMeta = additionalConversionMeta;
    this.generator = generator;
    this.newVersionId = newVersionId;
  }

  public void generate(PrintWriter pr) throws Exception
  {
    generator.generateHeader(pr, "SQL script to convert the database for " + generator.dbName() + " from version " + (newVersionId - 1) + " to version " + newVersionId);
    generator.generateNonMetaDiffChangesPre(pr, newVersionId);
    
    generateDropViews(pr);
    indexes.generateDropIndexesOfRemovedIndexes(pr);
    triggers.generateDropTriggersOfChangedTables(pr);
    triggers.generateDropTriggersOfDeletedTriggers(pr);
    foreignKeys.generateDropForeignKeysReferencingChangedOrDeletedColumns(pr);
    
    generateDropTableOfDeletedTables(pr);
    generateCreateTablesOfAddedTables(pr);
    
    generateTableModifications(pr);
    indexes.generateCreateIndexOfAddedIndexes(pr);
    constraints.generateCreateUniqueOfAddedUniqueConstraints(pr);
    
    triggers.generateCreateTriggersOfAddedTables(pr);
    triggers.generateCreateTriggersOfAddedTriggers(pr);
    foreignKeys.generateRecreateForeignKeysOfChangedColumns(pr);
    triggers.generateRecreateTriggersOfChangedTables(pr);
    generateCreateViews(pr);
    generateDeletesOfRemovedInserts(pr);
    generateInsertsOfNewAddedInserts(pr);
    
    if (additionalConversionMeta != null)
    {
      generator.generateMetaOutputStatements(pr, additionalConversionMeta);
    }
   
    generator.generateNonMetaDiffChangesPost(pr, metaDefinitionFrom, metaDefinitionTo, newVersionId);
    generateDropTemporaryStoredProcedures(pr);
    generator.generateVersionUpdate(pr, newVersionId);
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
    generator.comments.generate(pr, "Drop temporary created stored procedures needed for conversion");
    for (String storedProcedureId : createdTemporaryStoredProcedures)
    {
      generator.generateDropStoredProcedures(pr, storedProcedureId);
    }
  }

  private void generateDeletesOfRemovedInserts(PrintWriter pr)
  {
    List<SqlInsertWithValues> fromSqlInserts = removeInsertsToVersionTable(metaDefinitionFrom.getArtifacts(SqlInsertWithValues.class));
    List<SqlInsertWithValues> toSqlInserts = removeInsertsToVersionTable(metaDefinitionTo.getArtifacts(SqlInsertWithValues.class));
    List<String> toInsertStmts = getInsertStmts(toSqlInserts);
    
    boolean first = true;
    for (SqlInsertWithValues fromSqlInsert : fromSqlInserts)
    {
      String fromInsertStmt = getInsertStmt(fromSqlInsert);
      if (!toInsertStmts.contains(fromInsertStmt))
      {
        if (first)
        {
          generator.comments.generate(pr, "Delete removed default table content");
        }
        first = false;
        generator.dmlStatements.generateDelete(pr, fromSqlInsert);
      }
    }    
  }
  
  private void generateInsertsOfNewAddedInserts(PrintWriter pr)
  {
    List<SqlInsertWithValues> fromSqlInserts = removeInsertsToVersionTable(metaDefinitionFrom.getArtifacts(SqlInsertWithValues.class));
    List<String> fromInsertStmts = getInsertStmts(fromSqlInserts);
    List<SqlInsertWithValues> toSqlInserts = removeInsertsToVersionTable(metaDefinitionTo.getArtifacts(SqlInsertWithValues.class));
    List<String> toInsertStmts = getInsertStmts(toSqlInserts);

    boolean first = true;
    for (String toInsertStmt : toInsertStmts)
    {
      if (!fromInsertStmts.contains(toInsertStmt))
      {
        if (first)
        {
          generator.comments.generate(pr, "Add new added default table content");
        }
        first = false;
        pr.append(toInsertStmt);
      }
    }    
  }

  private List<SqlInsertWithValues> removeInsertsToVersionTable(List<SqlInsertWithValues> inserts)
  {
    return inserts.stream().filter(insert -> !insert.getTable().equalsIgnoreCase(VERSION_TABLE)).collect(Collectors.toList());
  }
  
  private List<String> getInsertStmts(List<SqlInsertWithValues> sqlInserts)
  {
    List<String> insterts = new ArrayList<>(sqlInserts.size());
    
    for(SqlInsertWithValues sqlInsert : sqlInserts)
    {
      String insertStmt = getInsertStmt(sqlInsert);
      insterts.add(insertStmt);      
    }
    return insterts;
  }

  private String getInsertStmt(SqlInsertWithValues sqlInsert)
  {
    StringWriter output = new StringWriter();
    generator.dmlStatements.generateInsert(new PrintWriter(output), sqlInsert);
    return output.toString();
  }

  class IndexGenerator
  {

    void generateCreateIndexOfAddedIndexes(PrintWriter pr)
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
      Map<SqlTable, SqlTable> tables = new LinkedHashMap<>();
      for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
      {
        SqlTable newTable = comonTable.getKey();
        SqlTable oldTable = comonTable.getValue();
    
        boolean hasAddedIndexes = !findAddedIndexes(newTable, oldTable).isEmpty();
        if (hasAddedIndexes)
        {
          tables.put(newTable, oldTable);
        }      
      }
      return tables;
    }

    private void generateCreateIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      List<SqlIndex> addedIndexes = findAddedIndexes(newTable, oldTable);
      if (addedIndexes.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Create new indexes of table " + newTable.getId());
      for (SqlIndex addedIndex : addedIndexes)
      {
        generator.generateIndex(pr, newTable, addedIndex);
      }
    }

    private List<SqlIndex> findAddedIndexes(SqlTable newTable, SqlTable oldTable)
    {
      return newTable
              .getIndexes()
              .stream()
              .filter(newIndex -> oldTable.findIndex(newIndex.getId()) == null)
              .collect(Collectors.toList());
    }

    void generateDropIndexesOfChangedColumns(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      if (!generator.getRecreateOptions().indexesOnAlterTable)
      {
        return;
      }
      
      Set<SqlIndex> changedIndexes = getIndexesFromChangedColumns(newTable, oldTable);
      if (changedIndexes.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Drop indexes which depend on changed columns");
      for (SqlIndex sqlIndex : changedIndexes)
      {
        generator.generateDropIndex(pr, oldTable, sqlIndex);
      }
    }
    
    void generateDropIndexesOfRemovedIndexes(PrintWriter pr)
    {
      for (Entry<SqlTable, SqlTable> changedTable : findTablesWithRemovedIndexes().entrySet())
      {
        SqlTable newTable = changedTable.getKey();
        SqlTable oldTable = changedTable.getValue();
        generateDroppedIndexes(pr, newTable, oldTable);    
      }
    }

    private Map<SqlTable, SqlTable> findTablesWithRemovedIndexes()
    {
      Map<SqlTable, SqlTable> tables = new LinkedHashMap<>();
      for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
      {
        SqlTable newTable = comonTable.getKey();
        SqlTable oldTable = comonTable.getValue();
    
        boolean hasRemovedIndexes = !findRemovedIndexes(newTable, oldTable).isEmpty();
        if (hasRemovedIndexes)
        {
          tables.put(newTable, oldTable);
        }      
      }
      return tables;
    }
    
    private List<SqlIndex> findRemovedIndexes(SqlTable newTable, SqlTable oldTable)
    {
      return oldTable
              .getIndexes()
              .stream()
              .filter(oldIndex -> newTable.findIndex(oldIndex.getId()) == null)
              .collect(Collectors.toList());
    }

    private void generateDroppedIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      List<SqlIndex> changedIndexes = findRemovedIndexes(newTable, oldTable);
      if (changedIndexes.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Drop removed indexes of table " + newTable.getId());
      for (SqlIndex sqlIndex : changedIndexes)
      {
        generator.generateDropIndex(pr, oldTable, sqlIndex);
      }
    }
    
    private Set<SqlIndex> getIndexesFromChangedColumns(SqlTable newTable, SqlTable oldTable)
    {
      Set<SqlIndex> result = new LinkedHashSet<>();
      List<SqlIndex> sqlIndexes = generator.getIndexes(oldTable);
      Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
      for (SqlTableColumn changedColumn : changedColumns.keySet())
      {
        for (SqlIndex sqlIndex : sqlIndexes)
        {
          if (sqlIndex.getColumns().contains(changedColumn.getId()))
          {
            result.add(sqlIndex);
          }
        }
      }
      return result;
    }

    void generateRecreateIndexes(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      if (!generator.getRecreateOptions().indexesOnAlterTable)
      {
        return;
      }
      
      Set<SqlIndex> changedIndexes = getIndexesFromChangedColumns(newTable, oldTable);
      if (changedIndexes.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Create index which depend on changed columns");
      for (SqlIndex sqlIndex : changedIndexes)
      {
        generator.generateIndex(pr, newTable, sqlIndex);
      }
    }
  }
  
  class ConstraintGenerator
  {

    void generateRecreateDefaultConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
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

    void generateDropDefaultConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      if (!generator.getRecreateOptions().defaultConstraints)
      {
        return;
      }
      List<SqlTableColumn> affectedColumns = getChangedColumnsWithDefaultConstraint(newTable, oldTable);
      affectedColumns.addAll(getDroppedColumnsWithDefaultConstraint(newTable, oldTable));
      for (SqlTableColumn col : affectedColumns)
      {
        generator.generateDropDefaultConstraint(pr, oldTable, col);
      }
    }

    private List<SqlTableColumn> getChangedColumnsWithDefaultConstraint(SqlTable newTable, SqlTable oldTable)
    {
      List<SqlTableColumn> result = new ArrayList<>();
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

    private List<SqlTableColumn> getDroppedColumnsWithDefaultConstraint(SqlTable newTable, SqlTable oldTable)
    {
      List<SqlTableColumn> result = new ArrayList<>();
      for (SqlTableColumn changedColumn : findDroppedColumns(newTable, oldTable))
      {
        if (changedColumn.getDefaultValue() != null)
        {
          result.add(changedColumn);
        }
      }
      return result;
    }

    void generateCreateUniqueOfAddedUniqueConstraints(PrintWriter pr)
    {
      for (Entry<SqlTable, SqlTable> changedTable : findTablesWithAddedUniqueConstraints().entrySet())
      {
        SqlTable newTable = changedTable.getKey();
        SqlTable oldTable = changedTable.getValue();
        generateCreateUniqueConstraint(pr, newTable, oldTable);    
      }
    }

    private Map<SqlTable, SqlTable> findTablesWithAddedUniqueConstraints()
    {
      Map<SqlTable, SqlTable> tables = new LinkedHashMap<>();
      for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
      {
        SqlTable newTable = comonTable.getKey();
        SqlTable oldTable = comonTable.getValue();
    
        boolean hasAddedIndexes = !findAddedUniqueConstraints(newTable, oldTable).isEmpty();
        if (hasAddedIndexes)
        {
          tables.put(newTable, oldTable);
        }      
      }
      return tables;
    }

    private void generateCreateUniqueConstraint(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      List<SqlUniqueConstraint> addedUniques = findAddedUniqueConstraints(newTable, oldTable);
      if (addedUniques.isEmpty())
      {
        return;
      }
      Set<SqlUniqueConstraint> alreadyRecreatedUniqueConstraints = getUniqueConstraintsToRecreate(newTable, oldTable);
      
      pr.println();
      generator.comments.generate(pr, "Create new unique constraint of table " + newTable.getId());
      for (SqlUniqueConstraint addedUnique : addedUniques)
      {
        if (alreadyRecreatedUniqueConstraints.contains(addedUnique))
        {
          generator.comments.generate(pr, "Skipping generation of constraint '" + addedUnique + "'."
                  + " It was already re-generated in this script.");
        }
        else
        {
          generator.generateAddUniqueConstraint(pr, newTable, addedUnique);
        }
      }
      pr.println();
    }

    private List<SqlUniqueConstraint> findAddedUniqueConstraints(SqlTable newTable, SqlTable oldTable)
    {
      List<SqlUniqueConstraint> addedUniques = new ArrayList<>();
      for (SqlUniqueConstraint newUnique : newTable.getUniqueConstraints())
      {
        SqlUniqueConstraint oldIndex = oldTable.findUniqueConstraint(newUnique.getId());
        if (oldIndex == null)
        {
          addedUniques.add(newUnique);
        }
      }
      return addedUniques;
    }

    void generateDeleteUniqueConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable) 
    {
      Set<SqlUniqueConstraint> changedUniqueConstraints = new LinkedHashSet<>();
      if (generator.getRecreateOptions().uniqueConstraintsOnAlterTable)
      {
        if (generator.getRecreateOptions().allUniqueConstraintsOnAlterTable)
        {
          changedUniqueConstraints.addAll(oldTable.getUniqueConstraints());
        }
        else
        {
          changedUniqueConstraints.addAll(getUniqueConstraintsFromChangedColumns(newTable, oldTable));
        }
      }
      
      changedUniqueConstraints.addAll(findDeletedUniqueConstraints(newTable, oldTable));
      if (changedUniqueConstraints.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Drop unique constraint which depend on changed columns or was deleted");
      for (SqlUniqueConstraint uniqueConstraint : changedUniqueConstraints)
      {
        generator.generateDropUniqueConstraint(pr, oldTable, uniqueConstraint, createdTemporaryStoredProcedures);
      }
    }

    private Set<SqlUniqueConstraint> findDeletedUniqueConstraints(SqlTable newTable,
            SqlTable oldTable)
    {
      Set<SqlUniqueConstraint> deletedUniqueConstraints = new LinkedHashSet<>();
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

    void generateRecreateUniqueConstraints(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      Set<SqlUniqueConstraint> uniqueConstraints = getUniqueConstraintsToRecreate(newTable, oldTable);
      generateCreateUniqueConstraints(pr, newTable, uniqueConstraints);
    }

    private Set<SqlUniqueConstraint> getUniqueConstraintsToRecreate(SqlTable newTable, SqlTable oldTable)
    {
      if (!generator.getRecreateOptions().uniqueConstraintsOnAlterTable)
      {
        return Collections.emptySet();
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
      return uniqueConstraints;
    }

    private void generateCreateUniqueConstraints(PrintWriter pr, SqlTable newTable, Collection<SqlUniqueConstraint> uniqueConstraints)
    {
      if (uniqueConstraints.isEmpty())
      {
        return;
      }
      pr.println();
      generator.comments.generate(pr, "Create unique constraints which depend on changed columns");
      for (SqlUniqueConstraint uniqueConstraint : uniqueConstraints)
      {
        generator.generateAddUniqueConstraint(pr, newTable, uniqueConstraint);
      }
    }

    private Set<SqlUniqueConstraint> getUniqueConstraintsFromChangedColumns(SqlTable newTable, SqlTable oldTable)
    {
      List<String> changedColumNames = new ArrayList<>();
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
    
      Set<SqlUniqueConstraint> dependentUniqueConstraints = new LinkedHashSet<>(); 
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
  }
  
  class TriggerGenerator
  {
    void generateTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinition)
    {
      generator.triggers.createForEachStatementDeleteTrigger(pr, table, metaDefinition);
      generator.triggers.createForEachRowDeleteTrigger(pr, table, metaDefinition);
    }

    public void generateCreateTriggersOfAddedTables(PrintWriter pr)
    {
      for (SqlTable addedTable : findAddedTables())
      {
        if (generator.triggers.hasTrigger(metaDefinitionTo, addedTable))
        {
          pr.println();
          generator.comments.generate(pr, "Create trigger which depend on new added table "+addedTable.getId());
          triggers.generateTrigger(pr, addedTable, metaDefinitionTo);
        }
      }
    }

    void generateDropTriggersOfChangedTables(PrintWriter pr)
    {
      Map<SqlTable, SqlTable> tablesWithChangedTriggers = findTablesWithChangedTriggers();
      
      for (SqlTable oldTableWithTrigger : tablesWithChangedTriggers.values())
      {
        pr.println();
        generator.comments.generate(pr, "Drop trigger which depend on changed table(s)");
        generator.triggers.generateDrop(pr, oldTableWithTrigger);
      }
    }
    
    void generateDropTriggersOfDeletedTriggers(PrintWriter pr)
    {
      Map<SqlTable, SqlTable> tablesWithChangedTriggers = findTablesWithDeletedTriggers();
      
      for (SqlTable oldTableWithTrigger : tablesWithChangedTriggers.values())
      {
        pr.println();
        generator.comments.generate(pr, "Drop trigger that has been deleted");
        generator.triggers.generateDrop(pr, oldTableWithTrigger);
      }
    }
    
    void generateCreateTriggersOfAddedTriggers(PrintWriter pr)
    {
      Map<SqlTable, SqlTable> tablesWithAddedTriggers = findTablesWithAddedTriggers();
      for (SqlTable newTableWithTrigger : tablesWithAddedTriggers.keySet())
      {
        pr.println();
        generator.comments.generate(pr, "Create new triggers on existing table(s)");
        generateTrigger(pr, newTableWithTrigger, metaDefinitionTo);
      }
    }
    
    void generateRecreateTriggersOfChangedTables(PrintWriter pr)
    {
      Map<SqlTable, SqlTable> tablesWithChangedTriggers = findTablesWithChangedTriggers();
      
      for (SqlTable newTableWithTrigger : tablesWithChangedTriggers.keySet())
      {
        pr.println();
        generator.comments.generate(pr, "Recreate trigger which depend on changed table(s)");
        generateTrigger(pr, newTableWithTrigger, metaDefinitionTo);
      }
    }
    
    private Map<SqlTable, SqlTable> findTablesWithAddedTriggers()
    {
      Map<SqlTable, SqlTable> tablesOfAddedTriggers = new LinkedHashMap<>();
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

    private Map<SqlTable, SqlTable> findTablesWithChangedTriggers()
    {
      Map<SqlTable, SqlTable> tablesOfChangedTriggers = new LinkedHashMap<>();
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
          if (generator.triggers.hasTrigger(metaDefinitionTo, newTable))
          {
            tablesOfChangedTriggers.put(newTable, oldTable);
          }
        }
      }
      return tablesOfChangedTriggers;
    }
    
    private Map<SqlTable, SqlTable> findTablesWithDeletedTriggers()
    {
      Map<SqlTable, SqlTable> tablesOfDeletedTriggers = new LinkedHashMap<>();
      // check all common tables if they have triggers which have been deleted
      for (Entry<SqlTable, SqlTable> comonTable : findCommonTables().entrySet())
      {
        SqlTable newTable = comonTable.getKey();
        SqlTable oldTable = comonTable.getValue();
        if (hasTriggerDeleted(newTable, oldTable))
        {
          tablesOfDeletedTriggers.put(newTable, oldTable);
        }
      }

      return tablesOfDeletedTriggers;
    }
    
    private boolean hasTriggerChanged(SqlTable newTable, SqlTable oldTable)
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
    
    private boolean hasTriggerDeleted(SqlTable newTable, SqlTable oldTable)
    {
      if (newTable == null || oldTable == null)
      {
        return true;
      }
      
      StringWriter fromOut = new StringWriter();
      StringWriter toOut = new StringWriter();

      generateTrigger(new PrintWriter(fromOut), newTable, metaDefinitionFrom);
      generateTrigger(new PrintWriter(toOut), oldTable, metaDefinitionTo);
      
      if (StringUtils.isBlank(toOut.toString()) && !StringUtils.isBlank(fromOut.toString()))
      {
        return true;
      }
      return false; 
    }
    
    private boolean hasAddedTrigger(SqlTable newTable, SqlTable oldTable)
    {
      if (newTable == null || oldTable == null)
      {
        return true;
      }

      StringWriter fromOut = new StringWriter();
      StringWriter toOut = new StringWriter();

      generateTrigger(new PrintWriter(fromOut), newTable, metaDefinitionFrom);
      generateTrigger(new PrintWriter(toOut), oldTable, metaDefinitionTo);
      
      return StringUtils.isNotEmpty(toOut.toString()) && StringUtils.isBlank(fromOut.toString());
    }
  }
  
  class ForeignKeyGenerator
  {

    void generateRecreateForeignKeysOfChangedColumns(PrintWriter pr)
    {
      if (!generator.getRecreateOptions().foreignKeysOnAlterTable)
      {
        return;
      }
      List<Pair<SqlTable, SqlForeignKey>> referencingColumns = getForeignKeysReferencingChangedColumns();
      for (Pair<SqlTable, SqlForeignKey> pair : referencingColumns)
      {
        if (generator.foreignKeys.isSupported(pair.getRight()))
        {
          generator.foreignKeys.generateAlterTableAdd(pr, pair.getLeft(), pair.getRight());
        }
      }
    }

    void generateDropForeignKeysReferencingChangedOrDeletedColumns(PrintWriter pr)
    {
      if (!generator.getRecreateOptions().foreignKeysOnAlterTable)
      {
        return;
      }
    
      List<Pair<SqlTable, SqlForeignKey>> referencingColumns = getForeignKeysReferencingChangedColumns();
      referencingColumns.addAll(getForeignKeysReferencingDroppedColumns());
      referencingColumns.addAll(getForeignKeysReferencingDroppedTables());
      for (Pair<SqlTable, SqlForeignKey> pair : referencingColumns)
      {
        if (generator.foreignKeys.isSupported(pair.getRight()))
        {
          generator.foreignKeys.generateAlterTableDrop(pr, pair.getLeft(), pair.getRight(),
                  createdTemporaryStoredProcedures);
        }
      }
    }

    private List<Pair<SqlTable, SqlForeignKey>> getForeignKeysReferencingChangedColumns()
    {
      List<Pair<SqlTable, SqlForeignKey>> sqlForeignKeys = new ArrayList<>();
      for (Entry<SqlTable, SqlTable> changedTable : findChangedTables().entrySet())
      {
        Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(changedTable.getKey(), changedTable.getValue());
        for (Entry<SqlTableColumn, SqlTableColumn> changedColumn : changedColumns.entrySet())
        {
          List<Pair<SqlTable, SqlForeignKey>> referencingColumns = metaDefinitionFrom.getReferencingForeignKeys(changedTable.getKey(), changedColumn.getKey());
          sqlForeignKeys.addAll(referencingColumns);
        }
      }    
      return sqlForeignKeys;
    }

    private List<Pair<SqlTable, SqlForeignKey>> getForeignKeysReferencingDroppedColumns()
    {
      List<Pair<SqlTable, SqlForeignKey>> sqlForeignKeys = new ArrayList<>();
      for (Entry<SqlTable, SqlTable> changedTable : findChangedTables().entrySet())
      {
        List<SqlTableColumn> droppedColumns = findDroppedColumns(changedTable.getKey(), changedTable.getValue());
        for (SqlTableColumn droppedColumn : droppedColumns)
        {
          List<Pair<SqlTable, SqlForeignKey>> referencingColumns = metaDefinitionFrom.getReferencingForeignKeys(changedTable.getValue(), droppedColumn);
          sqlForeignKeys.addAll(referencingColumns);
        }
      }    
      return sqlForeignKeys;
    }

    private List<Pair<SqlTable, SqlForeignKey>> getForeignKeysReferencingDroppedTables()
    {
      List<Pair<SqlTable, SqlForeignKey>> sqlForeignKeys = new ArrayList<>();
      for (SqlTable droppedTable : findDroppedTables())
      {
        List<Pair<SqlTable, SqlForeignKey>> referencingColumns = metaDefinitionFrom.getReferencingForeignKeys(droppedTable);
        sqlForeignKeys.addAll(referencingColumns);
      }    
      return sqlForeignKeys;
    }

    void generateAlterForeignKeys(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
    {
      if (generator.foreignKeys.isReferenceInColumnDefinitionSupported())
      {
        return;
      }
    
      List<SqlForeignKey> addedForeignKeys = findChangedForeignKeys(newTable, oldTable);
      if (!addedForeignKeys.isEmpty())
      {
        pr.println();
        generator.comments.generate(pr, "Create added foreign keys of table " + newTable.getId());
        for (SqlForeignKey sqlForeignKey : addedForeignKeys)
        {
          generator.foreignKeys.generateAlterTableAdd(pr, newTable, sqlForeignKey);
        }
      }

      List<SqlForeignKey> removedForeignKeys = findChangedForeignKeys(oldTable, newTable);
      removedForeignKeys = removeForeignKeysOnDroppedColumns(newTable, removedForeignKeys);
      if (!removedForeignKeys.isEmpty())
      {
        pr.println();
        generator.comments.generate(pr, "Remove foreign keys of table " + newTable.getId());
        for (SqlForeignKey sqlForeignKey : removedForeignKeys)
        {
          generator.foreignKeys.generateAlterTableDrop(pr, newTable, sqlForeignKey, createdTemporaryStoredProcedures);
        }
      }
    }

    private List<SqlForeignKey> removeForeignKeysOnDroppedColumns(SqlTable newTable,
            List<SqlForeignKey> removedForeignKeys)
    {
      return removedForeignKeys
              .stream()
              .filter(foreignKey -> newTable.findColumn(foreignKey.getColumnName())!=null)
              .collect(Collectors.toList());
    }
    
    boolean hasForeignKeyDefinitionChanged(SqlTable newTable, SqlTable oldTable)
    {
      List<SqlForeignKey> added = findChangedForeignKeys(newTable, oldTable);
      List<SqlForeignKey> removed = findChangedForeignKeys(oldTable, newTable);
      return !(added.isEmpty() && removed.isEmpty());
    }
    
    private List<SqlForeignKey> findChangedForeignKeys(SqlTable newTable, SqlTable oldTable)
    {
      List<SqlForeignKey> changedKeys = new ArrayList<>();
      List<SqlForeignKey> newForeignKeys = newTable.getForeignKeys();
      for (SqlForeignKey newForeignKey : newForeignKeys)
      {
        if (generator.foreignKeys.isSupported(newForeignKey))
        {
          SqlForeignKey oldForeignKey = oldTable.findForeignKey(newForeignKey.getId());
          if (!isForeignKey(oldForeignKey))
          {
            changedKeys.add(newForeignKey);
          }
        }
      }
      return changedKeys;
    }
    
    private boolean isForeignKey(SqlForeignKey foreignKey)
    {
      if (foreignKey == null)
      {
        return false;
      }
      return generator.foreignKeys.isSupported(foreignKey);
    }
  }
  
  private void generateDropTableOfDeletedTables(PrintWriter pr)
  {
    List<SqlTable> deletedTables = findDroppedTables();
    if (deletedTables.isEmpty())
    {
      return;
    }
    
    for (SqlTable deletedTable : deletedTables)
    {
      if (generator.triggers.hasTrigger(metaDefinitionFrom, deletedTable))
      {
        pr.println();
        generator.comments.generate(pr, "Drop trigger of no longer exisiting table");
        generator.triggers.generateDrop(pr, deletedTable);
      }
    }
    
    for (SqlTable deletedTable : deletedTables)
    {
      pr.println();
      generator.comments.generate(pr, "Drop no longer exisiting table");
      generator.generateDropTable(pr, deletedTable);
    }
  }

  private void generateCreateTablesOfAddedTables(PrintWriter pr)
  {
    registerOldTables();
    
    List<SqlTable> addedTables = findAddedTables();
    if (addedTables.isEmpty())
    {
      return;
    }
    pr.println();
    generator.comments.generate(pr, "Create new added tables");
    generator.generateTables(pr, addedTables);
  }

  private void registerOldTables()
  {
    List<SqlTable> oldTables = metaDefinitionFrom.getArtifacts(SqlTable.class);
    for (SqlTable oldTable : oldTables)
    {
      generator.addGeneratedTable(oldTable.getId());
    }
  }

  private void generateTableModifications(PrintWriter pr)
  {
    for (Entry<SqlTable, SqlTable> changedTable : findChangedTables().entrySet())
    {
      SqlTable newTable = changedTable.getKey();
      SqlTable oldTable = changedTable.getValue();
      constraints.generateDeleteUniqueConstraints(pr, newTable, oldTable);
      indexes.generateDropIndexesOfChangedColumns(pr, newTable, oldTable);
      generateDropPrimaryKeys(pr, newTable, oldTable);
      constraints.generateDropDefaultConstraints(pr, newTable, oldTable);
      generateAlterTable(pr, newTable, oldTable);
      foreignKeys.generateAlterForeignKeys(pr, newTable, oldTable);
      constraints.generateRecreateDefaultConstraints(pr, newTable, oldTable);
      generateRecreatePrimaryKeys(pr, newTable, oldTable);
      indexes.generateRecreateIndexes(pr, newTable, oldTable);
      constraints.generateRecreateUniqueConstraints(pr, newTable, oldTable);
      generateTableReorganisation(pr, newTable, oldTable);
    }
  }

  private void generateAlterTable(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    generateAlterTableDropColumns(pr, newTable, oldTable);
    generateAlterTableAlterColumns(pr, newTable, oldTable);
    generateAlterTableAddColumns(pr, newTable, oldTable);
  }

  private void generateAlterTableDropColumns(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> droppedColumns = findDroppedColumns(newTable, oldTable);
    if (droppedColumns.isEmpty())
    {
      return;
    }
    pr.println();
    generator.comments.generate(pr, "Dropped columns of table " + newTable.getId());
    for (SqlTableColumn droppedColumn : droppedColumns)
    {
      generator.generateAlterTableDropColumn(pr, droppedColumn, newTable);
      pr.println();
    }
  }

  private void generateAlterTableAlterColumns(PrintWriter pr, SqlTable newTable, SqlTable oldTable)
  {
    Map<SqlTableColumn, SqlTableColumn> changedColumns = findChangedColumns(newTable, oldTable);
    if (changedColumns.size() > 0)
    {
      pr.println();
      generator.comments.generate(pr, "Changed columns of table " + newTable.getId());
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
    if (addedColumns.isEmpty())
    {
      return;
    }
    pr.println();
    generator.comments.generate(pr, "Added columns of table " + newTable.getId());
    for (SqlTableColumn addedColumn : addedColumns)
    {
      generator.generateAlterTableAddColumn(pr, addedColumn, newTable);
      pr.println();
    }
  }

  private void generateDropViews(PrintWriter pr)
  {
    Set<SqlView> changedViews = findChangedViews(true);
    if (!changedViews.isEmpty())
    {
      generator.comments.generate(pr, "Drop views which has a new definition or depend on changed tables");
      for (SqlView sqlView : changedViews)
      {
        generator.generateDropView(pr, sqlView);
      }
    }
  }

  private Set<SqlView> findChangedViews(boolean includeDropped)
  {
    Set<SqlView> changedViews = new HashSet<>();
    changedViews.addAll(findViewsWhichDependOnChangedTables());
    changedViews.addAll(findViewsWhichHasNewDefinition());
    if (includeDropped)
    {
      changedViews.addAll(findDroppedViews());
    }
    return changedViews;
  }

  private void generateCreateViews(PrintWriter pr)
  {
    generateViews(pr, findAddedViews(), "Create added views");
    generateViews(pr, findChangedViews(false), "Recreate views which has a new definition or depend on changed tables");
  }

  private void generateViews(PrintWriter pr, Set<SqlView> views, String commentLine)
  {
    if (!views.isEmpty())
    {
      pr.println();
      generator.comments.generate(pr, commentLine);
      for (SqlView sqlView : views)
      {
        generator.generateView(pr, sqlView);
      }
    }
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
  private Set<SqlView> findViewsWhichDependOnChangedTables()
  {
    Map<SqlTable, SqlTable> changedTables = findChangedTables();
    List<SqlView> views = metaDefinitionTo.getArtifacts(SqlView.class);
    Set<SqlView> changedViews = new LinkedHashSet<>();
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
    List<SqlTable> newTables = new ArrayList<>(metaDefinitionTo.getArtifacts(SqlTable.class));
    List<SqlTable> addedTables = new ArrayList<>();
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
    List<SqlView> newViews = new ArrayList<>(metaDefinitionTo.getArtifacts(SqlView.class));
    Set<SqlView> addedViews = new LinkedHashSet<>();
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

  private List<SqlView> findDroppedViews()
  {
    return metaDefinitionFrom.getArtifacts(SqlView.class).stream()
        .filter(oldView -> metaDefinitionTo.findView(oldView.getId()) == null)
        .collect(Collectors.toList());
  }

  /**
   * @return tables which are deleted in the new meta definition (only exists in old meta definition)
   */
  private List<SqlTable> findDroppedTables()
  {
    List<SqlTable> oldTables = new ArrayList<>(metaDefinitionFrom.getArtifacts(SqlTable.class));
    List<SqlTable> deletedTables = new ArrayList<>();
    for (SqlTable oldTable: oldTables)
    {
      SqlTable newTable = metaDefinitionTo.findTable(oldTable.getId());
      if (newTable == null)
      {
        deletedTables.add(oldTable);
      }
    }
    Collections.sort(deletedTables, new NotReferencedTablesFirstComparator(deletedTables));
    return deletedTables;
  }

  /**
   * Tables which exists in old and new meta definition
   * @return map with new and old tables with the same id
   */
  private Map<SqlTable, SqlTable> findCommonTables()
  {
    List<SqlTable> oldTables = new ArrayList<>(metaDefinitionFrom.getArtifacts(SqlTable.class));
    Map<SqlTable, SqlTable> tables = new LinkedHashMap<>();
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
  private Map<SqlTable, SqlTable> findChangedTables()
  {
    Map<SqlTable, SqlTable> tables = new LinkedHashMap<>();
    for (Entry<SqlTable, SqlTable> commonTable : findCommonTables().entrySet())
    {
      SqlTable newTable = commonTable.getKey();
      SqlTable oldTable = commonTable.getValue();

      boolean hasChangedColumns = !findChangedColumns(newTable, oldTable).isEmpty();
      boolean hasAddedColumns = !findAddedColumns(newTable, oldTable).isEmpty();
      boolean hasDroppedColumns = !findDroppedColumns(newTable, oldTable).isEmpty();
      boolean hasDeletedUniqueConstraints = !constraints.findDeletedUniqueConstraints(newTable, oldTable).isEmpty();
      boolean hasForeignKeysChanges = foreignKeys.hasForeignKeyDefinitionChanged(newTable, oldTable);
      if (hasChangedColumns || hasAddedColumns || hasDroppedColumns || hasDeletedUniqueConstraints || hasForeignKeysChanges)
      {
        tables.put(newTable, oldTable);
      }
    }
    return tables;
  }

  private Set<SqlView> findViewsWhichHasNewDefinition()
  {
    Set<SqlView> views = new HashSet<>();
    for (Entry<SqlView, SqlView> newAndOldView : findOldViewsWithNewViews().entrySet())
    {
      SqlView newView = newAndOldView.getKey();
      SqlView oldView = newAndOldView.getValue();
      
      boolean hasAddedColumns = !findAddedColumns(newView, oldView).isEmpty();
      boolean hasDroppedColumns = !findDroppedColumns(newView, oldView).isEmpty();
      
      if (hasAddedColumns || hasDroppedColumns)
      {
        views.add(newView);
      }
    }
    return views;
  }

  private Map<SqlView, SqlView> findOldViewsWithNewViews()
  {
    List<SqlView> oldViews = new ArrayList<>(metaDefinitionFrom.getArtifacts(SqlView.class));
    Map<SqlView, SqlView> views = new LinkedHashMap<>();
    for (SqlView oldView : oldViews)
    {
      SqlView newView = metaDefinitionTo.findView(oldView.getId());
      if (newView != null)
      {
        views.put(newView, oldView);
      }
    }
    return views;
  }

  /**
   * Finds changed columns (deleted and added are not collected)
   * @param newTable the new table
   * @param oldTable the old table
   * @return map with new and old columns of changed columns of a table
   * @throws MetaException 
   */
  private Map<SqlTableColumn, SqlTableColumn> findChangedColumns(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> oldColumns = oldTable.getColumns();
    Map<SqlTableColumn, SqlTableColumn> changedColumns = new LinkedHashMap<>(oldColumns.size());
    for (SqlTableColumn oldColumn: oldColumns)
    {
      SqlTableColumn newColumn = newTable.findColumn(oldColumn.getId());
      if (newColumn != null && 
          hasColumnChanged(newTable, newColumn, oldColumn))
      {
        changedColumns.put(newColumn, oldColumn);
      }
    }
    return changedColumns;
  }

  private List<SqlTableColumn> findAddedColumns(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> addedColumns = new ArrayList<>();
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

  private List<SqlViewColumn> findAddedColumns(SqlView newView, SqlView oldView)
  {
    return newView.getColumns().stream()
      .filter(newColumn -> !oldView.hasColumn(newColumn.getId()))
      .collect(Collectors.toList());
  }

  private List<SqlTableColumn> findDroppedColumns(SqlTable newTable, SqlTable oldTable)
  {
    List<SqlTableColumn> droppedColumns = new ArrayList<>();
    for (SqlTableColumn oldColumn : oldTable.getColumns())
    {
      SqlTableColumn newColumn = newTable.findColumn(oldColumn.getId());
      if (newColumn == null)
      {
        droppedColumns.add(oldColumn);
      }
    }
    return droppedColumns;
  }

  private List<SqlViewColumn> findDroppedColumns(SqlView newView, SqlView oldView)
  {
    return oldView.getColumns().stream()
            .filter(oldColumn -> !newView.hasColumn(oldColumn.getId()))
            .collect(Collectors.toList());
  }

  private boolean hasColumnChanged(SqlTable newTable, SqlTableColumn newColumn, SqlTableColumn oldColumn)
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