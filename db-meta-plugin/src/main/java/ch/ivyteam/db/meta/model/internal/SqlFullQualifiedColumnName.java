package ch.ivyteam.db.meta.model.internal;

/**
 * A full qualified column name
 * @author rwei
 * @since 02.10.2009
 */
public class SqlFullQualifiedColumnName extends SqlAtom {

  /** The table part of the full qualified column name */
  private String fTable;
  /** The column part of the full qualified column name */
  private String fColumn;

  /**
   * Constructor
   * @param table
   * @param column
   */
  public SqlFullQualifiedColumnName(String table, String column) {
    assert column != null : "Parameter column must not be null";
    fTable = table;
    fColumn = column;
  }

  /**
   * Returns the column
   * @return the column
   */
  public String getColumn() {
    return fColumn;
  }

  /**
   * Returns the fTable
   * @return the fTable
   */
  public String getTable() {
    return fTable;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (fTable != null) {
      return fTable + "." + fColumn;
    } else {
      return fColumn;
    }
  }
}
