package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * Sql foreign key definition
 * @author rwei
 */
public class SqlForeignKey extends SqlTableContentDefinition
{
  /** The column name of the foreign key */ 
  private String fColumnName;
  
  /** The reference definition */
  private SqlReference fReference;
  
  /**
   * Constructor
   * @param id 
   * @param columnName
   * @param reference
   * @param dbSysHints
   * @param comment
   * @throws MetaException 
   */
  public SqlForeignKey(String id, String columnName, SqlReference reference, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(generateId("FK", id, columnName), dbSysHints, comment);
    assert columnName != null : "Parameter columnName must not be null";
    assert reference != null : "Parameter reference must not be null";
    fColumnName = columnName;
    fReference = reference;
  }

  /**
   * Gets the column name
   * @return column name
   */
  public String getColumnName()
  {
    return fColumnName;
  }
  
  /**
   * Gets the reference definition
   * @return reference definition
   */
  public SqlReference getReference()
  {
    return fReference;
  }
  
  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(512);
    builder.append("FOREIGN KEY ");
    builder.append(getId());
    builder.append(" (");
    builder.append(fColumnName);
    builder.append(") ");
    builder.append(fReference);
    return builder.toString();
  }
  
}
