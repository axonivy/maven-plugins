package ch.ivyteam.db.meta.model.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * A SQL table definition
 * @author rwei
 */
public class SqlTable extends SqlObject {

  /** The column definitions */
  private List<SqlTableColumn> fColumns = new ArrayList<SqlTableColumn>();
  /** The primary key definition */
  private SqlPrimaryKey fPrimaryKey;
  /** The foreign key definition */
  private List<SqlForeignKey> fForeignKeys = new ArrayList<SqlForeignKey>();
  /** The unique constraints definition */
  private List<SqlUniqueConstraint> fUniqueConstraints = new ArrayList<SqlUniqueConstraint>();
  /** The index definitions */
  private List<SqlIndex> fIndexes = new ArrayList<SqlIndex>();
  /** The trigger definitions */
  private List<SqlTrigger> fTriggers = new ArrayList<SqlTrigger>();

  /**
   * Constructor
   * @param id table name
   * @param definitions table content definitions
   * @param dbSysHints
   * @param comment comment
   * @throws MetaException
   */
  public SqlTable(String id, List<SqlTableContentDefinition> definitions,
          List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException {
    super(id, dbSysHints, comment);
    assert definitions != null : "Parameter columns must not be null";
    boolean found;
    for (SqlTableContentDefinition definition : definitions) {
      if (definition instanceof SqlTableColumn) {
        fColumns.add((SqlTableColumn) definition);
      } else if (definition instanceof SqlPrimaryKey) {
        if (fPrimaryKey != null) {
          throw new MetaException("Only one primary key can be defined in table '" + getId() + "'");
        }
        fPrimaryKey = (SqlPrimaryKey) definition;
      } else if (definition instanceof SqlUniqueConstraint) {
        fUniqueConstraints.add((SqlUniqueConstraint) definition);
      } else if (definition instanceof SqlIndex) {
        fIndexes.add((SqlIndex) definition);
      } else if (definition instanceof SqlForeignKey) {
        fForeignKeys.add((SqlForeignKey) definition);
      } else if (definition instanceof SqlTrigger) {
        fTriggers.add((SqlTrigger) definition);
        setTableNameOnTriggerStatements((SqlTrigger) definition);
      }
    }
    for (SqlForeignKey foreignKey : fForeignKeys) {
      found = false;
      for (SqlTableColumn column : fColumns) {
        if (column.getId().equals(foreignKey.getColumnName())) {
          if (column.getReference() != null) {
            throw new MetaException("Foreign key reference already declared for column '" + column.getId()
                    + "' in table '" + getId() + "'");
          }
          column.setReference(foreignKey.getReference());
          found = true;
          break;
        }
      }
      if (!found) {
        throw new MetaException("Column '" + foreignKey.getColumnName()
                + "' specified in foreign key definition not defined in table '" + getId() + "'");
      }
    }
    for (SqlTableColumn column : fColumns) {
      if (column.getReference() != null) {
        found = false;
        for (SqlForeignKey foreignKey : fForeignKeys) {
          if (column.getId().equals(foreignKey.getColumnName())) {
            found = true;
            break;
          }
        }
        if (!found) {
          fForeignKeys.add(new SqlForeignKey(null, column.getId(), column.getReference(),
                  column.getDatabaseManagementSystemHints(), null));
        }
      }
    }
    for (SqlIndex index : fIndexes) {
      checkColumnsExists(index.getColumns());
    }
    for (SqlUniqueConstraint unique : fUniqueConstraints) {
      checkColumnsExists(unique.getColumns());
    }
    for (SqlForeignKey foreignKey : fForeignKeys) {
      checkColumnExists(foreignKey.getColumnName());
    }
    if (fPrimaryKey != null) {
      checkColumnsExists(fPrimaryKey.getPrimaryKeyColumns());
    }
  }

  /**
   * Sets the table name on the statements of the given trigger
   * @param trigger the trigger
   */
  private void setTableNameOnTriggerStatements(SqlTrigger trigger) {
    for (SqlDmlStatement statement : trigger.getStatementsForEachRow()) {
      if (statement instanceof SqlUpdate) {
        ((SqlUpdate) statement).setTable(getId());
      }
    }
    for (SqlDmlStatement statement : trigger.getStatementsForEachStatement()) {
      if (statement instanceof SqlUpdate) {
        ((SqlUpdate) statement).setTable(getId());
      }
    }
  }

  /**
   * Checks if the given columns exists
   * @param columns names of the column to check
   * @throws MetaException if column does not exists
   */
  private void checkColumnsExists(List<String> columns) throws MetaException {
    for (String column : columns) {
      checkColumnExists(column);
    }
  }

  /**
   * Checks if the given column exists
   * @param columnName name of the column to check
   * @throws MetaException if column does not exists
   */
  private void checkColumnExists(String columnName) throws MetaException {
    for (SqlTableColumn column : fColumns) {
      if (column.getId().equals(columnName)) {
        return;
      }
    }
    throw new MetaException("Unknown column. Table '" + getId() + "' has no column '" + columnName + "'");
  }

  /**
   * Gets the columns
   * @return columns
   */
  public List<SqlTableColumn> getColumns() {
    return fColumns;
  }

  /**
   * Gets the foreign keys
   * @return foreign keys
   */
  public List<SqlForeignKey> getForeignKeys() {
    return fForeignKeys;
  }

  /**
   * Gets the indexes
   * @return indexes
   */
  public List<SqlIndex> getIndexes() {
    return fIndexes;
  }

  /**
   * Gets the primary key
   * @return primary key. Maybe null.
   */
  public SqlPrimaryKey getPrimaryKey() {
    return fPrimaryKey;
  }

  /**
   * Gets the unique contraints
   * @return unique constraints
   */
  public List<SqlUniqueConstraint> getUniqueConstraints() {
    return fUniqueConstraints;
  }

  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString() {
    boolean first = true;
    StringBuilder builder = new StringBuilder(32512);
    builder.append("CREATE TABLE ");
    builder.append(getId());
    builder.append("(\n");
    for (SqlTableColumn column : fColumns) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(column);
    }
    if (fPrimaryKey != null) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(fPrimaryKey);
    }
    for (SqlUniqueConstraint unique : fUniqueConstraints) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(unique);
    }
    for (SqlIndex index : fIndexes) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(index);
    }
    for (SqlForeignKey foreignKey : fForeignKeys) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(foreignKey);
    }
    for (SqlTrigger trigger : fTriggers) {
      if (!first) {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(trigger);
    }
    builder.append(")");
    return builder.toString();
  }

  /**
   * Gets the triggers
   * @return triggers
   */
  public List<SqlTrigger> getTriggers() {
    return fTriggers;
  }

  /**
   * Finds the foreign key with the given name
   * @param foreignKeyName the name of the foreign key
   * @return foreign key or null
   */
  public SqlForeignKey findForeignKey(String foreignKeyName) {
    for (SqlForeignKey foreignKey : fForeignKeys) {
      if (foreignKey.getId().equals(foreignKeyName)) {
        return foreignKey;
      }
    }
    return null;
  }

  /**
   * Finds the index with the given name
   * @param indexName the name of the index
   * @return index or null
   */
  public SqlIndex findIndex(String indexName) {
    for (SqlIndex index : fIndexes) {
      if (index.getId().equals(indexName)) {
        return index;
      }
    }
    return null;
  }

  /**
   * Finds a unique constraint with the given name
   * @param constraintName the name of the unique constraint
   * @return unique constraint or null
   */
  public SqlUniqueConstraint findUniqueConstraint(String constraintName) {
    for (SqlUniqueConstraint uniqueConstraint : fUniqueConstraints) {
      if (uniqueConstraint.getId().equals(constraintName)) {
        return uniqueConstraint;
      }
    }
    return null;
  }

  /**
   * Finds a trigger with the given name
   * @param triggerName the name of the trigger
   * @return trigger or null
   */
  public SqlTrigger findTrigger(String triggerName) {
    for (SqlTrigger trigger : fTriggers) {
      if (trigger.getId().equals(triggerName)) {
        return trigger;
      }
    }
    return null;
  }

  /**
   * Finds the foreign key defined on the given column
   * @param column the column
   * @return foreign key or null
   */
  public SqlForeignKey findForeignKey(SqlTableColumn column) {
    if (column.getReference() == null) {
      return null;
    }
    for (SqlForeignKey foreignKey : fForeignKeys) {
      if (foreignKey.getColumnName().equals(column.getId())) {
        return foreignKey;
      }
    }
    return null;
  }

  /**
   * Finds the column with the given column name
   * @param columnName the name of the column
   * @return table column or null
   */
  public SqlTableColumn findColumn(String columnName) {
    for (SqlTableColumn column : fColumns) {
      if (column.getId().equals(columnName)) {
        return column;
      }
    }
    return null;
  }

  /**
   * Checks if the given column is part of the primary key
   * @param column the column
   * @return true if column is part of the primary key, otherwise false
   */
  public boolean isPrimaryKeyColumn(SqlTableColumn column) {
    return (getPrimaryKey() != null) && (getPrimaryKey().getPrimaryKeyColumns().contains(column.getId()));
  }
}
