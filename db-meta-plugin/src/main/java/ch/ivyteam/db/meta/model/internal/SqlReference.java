package ch.ivyteam.db.meta.model.internal;

/**
 * A reference to a column in a table 
 * @author rwei
 */
public class SqlReference
{
  /** The name of the table this reference references */
  private String fForeignTable;
  
  /** The name of the column this reference referneces */
  private String fForeignColumn;
  
  /** Action that should be exectuted if the referneced row is deleted, updated or inserted */
  private SqlForeignKeyAction fForeignKeyAction;
  
  /**
   * Constructor
   * @param foreignTable the referenced table
   * @param foreignColumn the referenced column
   * @param foreignKeyAction the foreign key action 
   */
  public SqlReference(String foreignTable, String foreignColumn,
          SqlForeignKeyAction foreignKeyAction)
  {
    assert foreignTable != null : "Parameter foreignKey must not be null";
    assert foreignColumn != null : "Parameter foreignColumn must not be null";
    fForeignTable = foreignTable;
    fForeignColumn = foreignColumn;
    fForeignKeyAction = foreignKeyAction;
  }
  
  /**
   * Gets the foreign table
   * @return foreign table
   */
  public String getForeignTable()
  {
    return fForeignTable;
  }
  
  /**
   * Gets the foreign column
   * @return foreign column
   */
  public String getForeignColumn()
  {
    return fForeignColumn;
  }
  
  /**
   * Gets the foreign key action
   * @return foreign key action. May be null.
   */
  public SqlForeignKeyAction getForeignKeyAction()
  {
    return fForeignKeyAction;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(512);
    builder.append("REFERENCES ");
    builder.append(fForeignTable);
    builder.append("(");
    builder.append(fForeignColumn);
    builder.append(") ");
    builder.append(fForeignKeyAction);
    return builder.toString();
  }

}
