package ch.ivyteam.db.meta.generator.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ch.ivyteam.db.meta.model.internal.SqlTable;

public class NotReferencedTablesFirstComparator implements Comparator<SqlTable>
{
  private Map<SqlTable, Integer> tableWeights = new HashMap<>();

  public NotReferencedTablesFirstComparator(List<SqlTable> tables)
  {
    List<SqlTable> remainingTables = new ArrayList<>(tables);
    int weight = 0;
    while (!remainingTables.isEmpty())
    {
      SqlTable table = getAnyTableWhichIsNotReferencedBy(remainingTables);
      tableWeights.put(table, weight);
      remainingTables.remove(table);
      weight++;
    } 
  }

  private SqlTable getAnyTableWhichIsNotReferencedBy(List<SqlTable> remainingTables)
  {
    return remainingTables
        .stream()
        .filter(table -> isNotReferencedBy(table, remainingTables))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("There is a circular dependency in the following tables "+remainingTables));    
  }

  private boolean isNotReferencedBy(SqlTable table, List<SqlTable> remainingTables)
  {
    return remainingTables
            .stream()
            .noneMatch(remainingTable -> isReferencedBy(table, remainingTable));
  }
  
  private boolean isReferencedBy(SqlTable table, SqlTable referencingTable)
  {
    return referencingTable
            .getForeignKeys()
            .stream()
            .anyMatch(foreignKey -> Objects.equals(table.getId(), foreignKey.getReference().getForeignTable()));
  }

  @Override
  public int compare(SqlTable table1, SqlTable table2)
  {
    Integer weight1 = tableWeights.get(table1);
    Integer weight2 = tableWeights.get(table2);
    return weight1.compareTo(weight2);
  }
}
