package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/**
 * Generates a list of tables. The tables are sorted in the way that the content of the table can be deleted in
 * this order without to break any references.
 * @author rwei
 * @since 22.10.2009
 */
public class SortedTableContentDeleteListGenerator extends AbstractSortedTableContentListGenerator
{
  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    PrintWriter pr;
    Map<String, List<Pair<String, SqlForeignKeyAction>>> tableReferenceGraph;
    List<String> deleteTables = new ArrayList<String>(); 
    int size;
    pr = new NewLinePrintWriter(fOutputFile);
    try
    {
      tableReferenceGraph = createTableReferenceGraph(metaDefinition);
      size = tableReferenceGraph.size();
      while (size > 0)
      {        
        for (Map.Entry<String, List<Pair<String, SqlForeignKeyAction>>> entry : tableReferenceGraph.entrySet())
        {
          if (entry.getValue().size()==0)
          {
            deleteTables.add(entry.getKey());            
          }
        }
        if (deleteTables.isEmpty())
        {
          // no tables found to delete. Search for a table that references all tables with cascade delete
          deleteTables.addAll(getAllTablesThatAreRefrencedFromRootTableOnlyWithDeleteCascadeReferences(tableReferenceGraph));
        }
        deleteTablesFromReferenceGraph(tableReferenceGraph, deleteTables);
      
        for (String table : deleteTables)
        {
          pr.println(table);
          tableReferenceGraph.remove(table);
        }
        if (size == tableReferenceGraph.size())
        {
          throw new MetaException("Cycle in table delete references");
        }
        deleteTables.clear();
        size = tableReferenceGraph.size();
      }
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
  }

  /**
   * Get all tables that are referenced from a single root table only with delete cascade references 
   * @param tableReferenceGraph the hole reference graph
   * @return list of tables
   */
  @SuppressWarnings("unchecked")
  private Set<String> getAllTablesThatAreRefrencedFromRootTableOnlyWithDeleteCascadeReferences(
          Map<String, List<Pair<String, SqlForeignKeyAction>>> tableReferenceGraph)
  {
    Set<String> tables;
    for (String table : tableReferenceGraph.keySet())
    {
       tables = getAllTablesThatAreReferencedOnlyWithDeleteCascadeReferences(table, tableReferenceGraph);
       if (tables != null)
       {
         return tables;
       }
    }
    return Collections.EMPTY_SET;
  }

  /**
   * Get all tables that are referenced from the given single root table only with delete cascade references
   * @param table the root table
   * @param tableReferenceGraph the hole reference graph
   * @return list of tables or null
   */
  private Set<String> getAllTablesThatAreReferencedOnlyWithDeleteCascadeReferences(String table,
          Map<String, List<Pair<String, SqlForeignKeyAction>>> tableReferenceGraph)
  {
    Set<String> referencedTables = new LinkedHashSet<String>();
    Set<String> deletedTables = new LinkedHashSet<String>();
    deletedTables.add(table);
    referencedTables.add(table);
    getAllTablesThatAreReferencedOnlyWithDeleteCascadeReferences(table, tableReferenceGraph, referencedTables, deletedTables);
    if ((deletedTables.size()>1)&&(deletedTables.size() == referencedTables.size()))
    {
      return deletedTables;
    }
    return null;
  }

  /**
   * Get all tables that are referenced from the given single root table only with delete cascade references
   * @param table the root table
   * @param tableReferenceGraph the hole reference graph
   * @param referencedTables tables that are references
   * @param deletedTables tables that are deleted
   */
  private void getAllTablesThatAreReferencedOnlyWithDeleteCascadeReferences(String table,
          Map<String, List<Pair<String, SqlForeignKeyAction>>> tableReferenceGraph,
          Set<String> referencedTables, Set<String> deletedTables)
  {
    for (Pair<String, SqlForeignKeyAction> reference : tableReferenceGraph.get(table))
    {
      if (reference.getRight() == SqlForeignKeyAction.ON_DELETE_CASCADE)
      {
        deletedTables.add(reference.getLeft());
      }
      if (!referencedTables.contains(reference.getLeft()))
      {
        referencedTables.add(reference.getLeft());
        getAllTablesThatAreReferencedOnlyWithDeleteCascadeReferences(reference.getLeft(), tableReferenceGraph, referencedTables, deletedTables);
      }
    }
  }
  
  /**
   * @param table
   * @param foreignKey
   * @return -
   */
  private boolean isForeignKeyRelevant(SqlTable table, SqlForeignKey foreignKey)
  {
    return (getGeneratedForeignKeyAction(foreignKey) != SqlForeignKeyAction.ON_DELETE_SET_NULL)&&
        (!foreignKey.getReference().getForeignTable().equals(table.getId()));
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    System.out.println("SortedTableContentDeleteListGenerator Options: -outputFile {outputFile}");    

  }

  /**
   * Creates a table reference graph
   * @param metaDefinition
   * @return table reference
   */
  protected Map<String, List<Pair<String, SqlForeignKeyAction>>> createTableReferenceGraph(SqlMeta metaDefinition)
  {
    Map<String, List<Pair<String, SqlForeignKeyAction>>> tableDeleteGraph = new HashMap<String, List<Pair<String, SqlForeignKeyAction>>>();
    List<Pair<String, SqlForeignKeyAction>> referencedByTables; 
  
    // add all tables
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      referencedByTables = new ArrayList<Pair<String, SqlForeignKeyAction>>();
      tableDeleteGraph.put(table.getId(), referencedByTables);
    }
    // fill reference info
    for (SqlTable table: metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreignKey : table.getForeignKeys())
      {
        if (isForeignKeyGenerated(foreignKey))
        {
          if (isForeignKeyRelevant(table, foreignKey))
          {
            referencedByTables = tableDeleteGraph.get(foreignKey.getReference().getForeignTable());
            referencedByTables.add(new ImmutablePair<String, SqlForeignKeyAction>(table.getId(), getGeneratedForeignKeyAction(foreignKey)));
          }
        }
      }
    }
    return tableDeleteGraph;
  }

  /**
   * Gets the generated foreign key action for the given database system
   * @param foreignKey the foreign key
   * @return generated foreign key action
   */
  protected SqlForeignKeyAction getGeneratedForeignKeyAction(SqlForeignKey foreignKey)
  {
    if (foreignKey.getDatabaseManagementSystemHints(fDatabaseSystem).isHintSet(SqlScriptGenerator.NO_ACTION))
    {
      return null;
    }
    return foreignKey.getReference().getForeignKeyAction();
  }

  /**
   * Deletes all reference from the table reference graph that references the given delete tables
   * @param tableReferenceGraph
   * @param deleteTables
   */
  protected void deleteTablesFromReferenceGraph(Map<String, List<Pair<String, SqlForeignKeyAction>>> tableReferenceGraph, List<String> deleteTables)
  {
    List<Pair<String, SqlForeignKeyAction>> deletedReferences = new ArrayList<Pair<String, SqlForeignKeyAction>>();
    for (List<Pair<String, SqlForeignKeyAction>> references : tableReferenceGraph.values())
    {
      for (Pair<String, SqlForeignKeyAction> reference : references)
      {
        for (String table : deleteTables)
        {
          if (reference.getLeft().equals(table))
          {
            deletedReferences.add(reference);            
          }
        }
      }
      references.removeAll(deletedReferences);
      deletedReferences.clear();
    }
  }

}
