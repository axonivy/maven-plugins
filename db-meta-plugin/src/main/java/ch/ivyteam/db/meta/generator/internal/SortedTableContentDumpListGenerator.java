package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/**
 * Generates a list of tables. The tables are sorted in the way that the content of the table can be dump in
 * and then restored in this order without to break any references.
 * @author rwei
 * @since 22.10.2009
 */
public class SortedTableContentDumpListGenerator extends AbstractSortedTableContentListGenerator
{
  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    PrintWriter pr;
    Map<String, List<Pair<String, String>>> tableReferenceGraph;
    List<String> dumpTables = new ArrayList<String>(); 
    int size;
    pr = new NewLinePrintWriter(fOutputFile);
    try
    {
      tableReferenceGraph = createTableReferenceGraph(metaDefinition);
      size = tableReferenceGraph.size();
      while (size > 0)
      {        
        for (Map.Entry<String, List<Pair<String, String>>> entry : tableReferenceGraph.entrySet())
        {
          if (entry.getValue().size()==0)
          {
            pr.println(entry.getKey());
            dumpTables.add(entry.getKey());            
          }
        }
        if (dumpTables.isEmpty())
        {
          for (Map.Entry<String, List<Pair<String, String>>> entry : tableReferenceGraph.entrySet())
          {
            boolean allNullable = true;
            for(Pair<String, String> referenceTable : entry.getValue())
            {
              if (referenceTable.getRight() == null)
              {
                allNullable = false;
                break;
              }        
            }
            if (allNullable)
            {
              dumpTables.add(entry.getKey());
              pr.print(entry.getKey());
              pr.print('[');
              pr.print(metaDefinition.findTable(entry.getKey()).getPrimaryKey().getPrimaryKeyColumns().get(0));
              for(Pair<String, String> referenceTable : entry.getValue())
              {
                pr.print(", ");
                pr.print(referenceTable.getRight());                
              }
              pr.println(']');
              break;
            }
          }
        }
        deleteTablesFromReferenceGraph(tableReferenceGraph, dumpTables);
        if (size == tableReferenceGraph.size())
        {
          String tables="";
          for (String table : tableReferenceGraph.keySet())
          {
            if (!tables.isEmpty())
            {
              tables += ", ";
            }
            tables += table;
          }
          throw new MetaException("Cycle in table delete references: "+tables);
        }
        dumpTables.clear();
        size = tableReferenceGraph.size();
      }
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
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
  private Map<String, List<Pair<String, String>>> createTableReferenceGraph(SqlMeta metaDefinition)
  {
    Map<String, List<Pair<String, String>>> tableReferenceGraph = new HashMap<String, List<Pair<String, String>>>();
    List<Pair<String, String>> referencedTables; 
  
    // add all tables
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      referencedTables = new ArrayList<Pair<String, String>>();
      for (SqlForeignKey foreignKey : table.getForeignKeys())
      {
        if (isForeignKeyGenerated(foreignKey))
        {
          if (table.findColumn(foreignKey.getColumnName()).isCanBeNull())
          {
            referencedTables.add(new ImmutablePair<String, String>(foreignKey.getReference().getForeignTable(), foreignKey.getColumnName()));  
          }
          else
          {
            referencedTables.add(new ImmutablePair<String, String>(foreignKey.getReference().getForeignTable(), null));
          }
        }
      }
      tableReferenceGraph.put(table.getId(), referencedTables);
    }
    return tableReferenceGraph;
  }

  /**
   * Deletes all reference from the table reference graph that references the given delete tables
   * @param tableReferenceGraph
   * @param deleteTables
   */
  private void deleteTablesFromReferenceGraph(Map<String, List<Pair<String, String>>> tableReferenceGraph, List<String> deleteTables)
  {
    for (List<Pair<String, String>> references : tableReferenceGraph.values())
    {
      List<Pair<String, String>> toRemove = new ArrayList<Pair<String, String>>();
      for (Pair<String, String> table : references)
      {
        if(deleteTables.contains(table.getLeft()))
        {
          toRemove.add(table);
        }
      }
      references.removeAll(toRemove);
    }
    for (String table : deleteTables)
    {
      tableReferenceGraph.remove(table);
    }
  }

}
