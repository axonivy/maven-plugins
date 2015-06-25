package ch.ivyteam.db.meta.model.internal;

import java.util.Arrays;
import java.util.List;

/**
 * Base class of all sql objects that can be specified inside an sql meta information
 * @author rwei
 */
public class SqlObject extends SqlArtifact implements Comparable<SqlObject>
{
  /** The id of the object */
  private String fId;
  
  /**
   * Constructor
   * @param id table name
   * @param dbSysHints 
   * @param comment comment
   * @throws MetaException 
   */
  public SqlObject(String id, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(dbSysHints, comment);
    assert id != null : "Parameter id must not be null";
    fId = id;
  }
    
  /**
   * Gets the id
   * @return id
   */
  public String getId()
  {
    return fId;
  }
  
  /**
   * Generates an identifier if the given identifier is null
   * @param prefix the prefix of the identifier
   * @param id the identifier
   * @param column the column
   * @return identifier
   */
  public static String generateId(String prefix, String id, String column)
  {
    return generateId(prefix, id, Arrays.asList(column));
  }

  /**
   * Generates an identifier if the given identifier is null
   * @param prefix the prefix of the identifier
   * @param id the identifier
   * @param columns the columns
   * @return identifier
   */
  public static String generateId(String prefix, String id, List<String> columns)
  {
    StringBuilder builder = new StringBuilder(100);
    if (id != null)
    {
      return id;
    }
    builder.append(prefix);
    for (String column : columns)
    {
      builder.append("_");
      builder.append(column);
    }
    return builder.toString();
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return fId;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(SqlObject o)
  {
    return fId.compareTo(o.fId);
  }
  
}
