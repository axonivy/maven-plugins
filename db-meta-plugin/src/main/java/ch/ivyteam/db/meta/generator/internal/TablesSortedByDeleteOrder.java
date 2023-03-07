package ch.ivyteam.db.meta.generator.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

class TablesSortedByDeleteOrder {

  private final DbHints dbHints;
  private final SqlMeta metaInformation;

  TablesSortedByDeleteOrder(DbHints dbHints, SqlMeta metaInformation) {
    this.dbHints = dbHints;
    this.metaInformation = metaInformation;
  }

  List<SqlTable> byDeleteOrder() {
    List<SqlTable> result = new ArrayList<>();
    TableReferenceGraph graph = new TableReferenceGraph();
    int size = graph.size();
    while (size > 0) {
      Set<SqlTable> deleteTables = graph.getTablesNotReferenced();
      if (deleteTables.isEmpty()) {
        // no tables found to delete. Search for a table that references all
        // tables with cascade delete
        deleteTables = graph.getTablesOnlyReferencedByDeleteCascade();
      }
      graph.removeTables(deleteTables);
      result.addAll(deleteTables);
      if (size == graph.size()) {
        throw new MetaException("Cycle in table delete references");
      }
      size = graph.size();
    }
    return result;
  }

  List<SqlTable> byDeleteOrder(Collection<SqlTable> toDelete) {
    TableReferenceGraph graph = new TableReferenceGraph();
    List<SqlTable> result = new ArrayList<>();
    Map<SqlTable, DeleteInfo> remaining = new HashMap<>();
    for (SqlTable table : toDelete) {
      remaining.put(table, new DeleteInfo(table, graph));
    }
    while (!remaining.isEmpty()) {
      SqlTable nextToDelete = findDeletableTable(remaining);
      result.add(nextToDelete);
      DeleteInfo deleted = remaining.remove(nextToDelete);
      for (DeleteInfo deleteInfo : remaining.values()) {
        deleteInfo.refsWithNoAction.removeAll(deleted.refsWithCascade);
        deleteInfo.refsWithNoAction.remove(nextToDelete);
      }
    }
    return result;
  }

  private SqlTable findDeletableTable(Map<SqlTable, DeleteInfo> remaining) {
    return remaining
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().refsWithNoAction.isEmpty())
            .map(Entry::getKey)
            .findAny()
            .orElseGet(() -> remaining.keySet().iterator().next());
  }

  private final class TableReferenceGraph {

    private Map<SqlTable, List<ReferencedBy>> graph = new HashMap<>();

    private TableReferenceGraph() {
      // add all tables
      for (SqlTable table : metaInformation.getArtifacts(SqlTable.class)) {
        graph.put(table, new ArrayList<>());
      }
      // fill reference info
      for (SqlTable table : metaInformation.getArtifacts(SqlTable.class)) {
        for (SqlForeignKey foreignKey : table.getForeignKeys()) {
          if (isForeignKeyGenerated(foreignKey) &&
                  isForeignKeyRelevant(table, foreignKey)) {
            SqlTable foreignTable = metaInformation.findTable(foreignKey.getReference().getForeignTable());
            List<ReferencedBy> referencedByTables = graph.get(foreignTable);
            ReferencedBy referencedBy = new ReferencedBy(table, getGeneratedForeignKeyAction(foreignKey));
            referencedByTables.add(referencedBy);
          }
        }
      }
    }

    private Set<SqlTable> getTablesNotReferenced() {
      return graph
              .entrySet()
              .stream()
              .filter(entry -> entry.getValue().isEmpty())
              .map(Entry::getKey)
              .collect(Collectors.toSet());
    }

    private Set<SqlTable> getTablesOnlyReferencedByDeleteCascade() {
      for (SqlTable table : graph.keySet()) {
        Set<SqlTable> tables = getTablesOnlyReferencedByDeleteCascade(table);
        if (!tables.isEmpty()) {
          return tables;
        }
      }
      return Collections.emptySet();
    }

    private Set<SqlTable> getTablesOnlyReferencedByDeleteCascade(SqlTable table) {
      Set<SqlTable> referencedTables = new LinkedHashSet<>();
      Set<SqlTable> cascadedTables = new LinkedHashSet<>();
      cascadedTables.add(table);
      referencedTables.add(table);
      getTablesReferencedByDeleteCascade(table, referencedTables, cascadedTables);
      if ((cascadedTables.size() > 1) && (cascadedTables.size() == referencedTables.size())) {
        return cascadedTables;
      }
      return Collections.emptySet();
    }

    private void getTablesReferencedByDeleteCascade(SqlTable table,
            Set<SqlTable> referencedTables, Set<SqlTable> cascadedTables) {
      for (ReferencedBy reference : graph.get(table)) {
        if (reference.deleteAction == SqlForeignKeyAction.ON_DELETE_CASCADE) {
          cascadedTables.add(reference.table);
        }
        if (referencedTables.add(reference.table)) {
          getTablesReferencedByDeleteCascade(reference.table, referencedTables, cascadedTables);
        }
      }
    }

    private Set<SqlTable> getTablesReferencedByDeleteCascade(SqlTable table) {
      Set<SqlTable> referencedTables = new LinkedHashSet<>();
      Set<SqlTable> cascadedTables = new LinkedHashSet<>();
      getTablesReferencedByDeleteCascade(table, referencedTables, cascadedTables);
      return cascadedTables;
    }

    private int size() {
      return graph.size();
    }

    private boolean isForeignKeyRelevant(SqlTable table, SqlForeignKey foreignKey) {
      return (getGeneratedForeignKeyAction(foreignKey) != SqlForeignKeyAction.ON_DELETE_SET_NULL) &&
              (!foreignKey.getReference().getForeignTable().equals(table.getId()));
    }

    private SqlForeignKeyAction getGeneratedForeignKeyAction(SqlForeignKey foreignKey) {
      if (dbHints.NO_ACTION.isSet(foreignKey)) {
        return null;
      }
      return foreignKey.getReference().getForeignKeyAction();
    }

    private void removeTables(Set<SqlTable> deleteTables) {
      for (List<ReferencedBy> references : graph.values()) {
        List<ReferencedBy> deletedReferences = references
                .stream()
                .filter(ref -> deleteTables.contains(ref.table))
                .collect(Collectors.toList());
        references.removeAll(deletedReferences);
      }
      deleteTables.forEach(table -> graph.remove(table));
    }

    private boolean isForeignKeyGenerated(SqlForeignKey foreignKey) {
      return !dbHints.NO_REFERENCE.isSet(foreignKey);
    }

    private Set<SqlTable> getTablesDirectReferencedByNoAction(SqlTable table) {
      return graph.get(table)
              .stream()
              .filter(ref -> ref.deleteAction == null)
              .map(ref -> ref.table)
              .collect(Collectors.toSet());
    }
  }

  private static final class ReferencedBy {

    public final SqlTable table;
    public final SqlForeignKeyAction deleteAction;

    private ReferencedBy(SqlTable table, SqlForeignKeyAction deleteAction) {
      this.table = table;
      this.deleteAction = deleteAction;
    }
  }

  private static final class DeleteInfo {

    public final Set<SqlTable> refsWithCascade;
    public final Set<SqlTable> refsWithNoAction;

    public DeleteInfo(SqlTable table, TableReferenceGraph graph) {
      refsWithNoAction = graph.getTablesDirectReferencedByNoAction(table);
      refsWithCascade = graph.getTablesReferencedByDeleteCascade(table);
      refsWithCascade.remove(table);
      refsWithNoAction.removeAll(refsWithCascade);
    }
  }
}
