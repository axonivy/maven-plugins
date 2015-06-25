package ch.ivyteam.db.meta.model.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;


/**
 * The sql meta information. This is the root of the meta information
 * @author rwei
 */
public class SqlMeta
{
  /** All sql artefacts defined in the meta information */
  private List<SqlArtifact> fSqlArtifact = new ArrayList<SqlArtifact>();
  
  /**
   * Constructor
   */
  public SqlMeta()
  {}
  
  /**
   * Adds a new sql artifact to the meta information
   * @param artifact the sql artifact to add
   */
  public void addArtifact(SqlArtifact artifact)
  {
    assert artifact != null : "Parameter artifact must not be null";
    fSqlArtifact.add(artifact);
  }
  
  /**
   * Gets all sql artifacts 
   * @return sql artifacts
   */
  public List<SqlArtifact> getArtifacts()
  {
    return fSqlArtifact;
  }
  
  /**
   * Gets all sql artifact of a given type 
   * @param <T> the type of the sql artifact
   * @param artifactType the type of the sql artifacts to get
   * @return sql artifacts
   */
  @SuppressWarnings("unchecked")
  public <T extends SqlArtifact> List<T> getArtifacts(Class<T> artifactType)
  {
    List<T> objects = new ArrayList<T>();
    
    for (SqlArtifact object : fSqlArtifact)
    {
      if (artifactType.isAssignableFrom(object.getClass()))
      {
        objects.add((T)object);
      }
    }
    return objects;
  }
  
  /**
   * Gets all sql objects of a given type. The returned list is sorted
   * @param <T> the type of the sql object
   * @param objectType the typ eof the sql object to get
   * @return sql objects
   */
  @SuppressWarnings("unchecked")
  public <T extends SqlObject> List<T> getSqlObjects(Class<T> objectType)
  {
    List<T> objects = new ArrayList<T>();
    
    for (SqlArtifact object : fSqlArtifact)
    {
      if (objectType.isAssignableFrom(object.getClass()))
      {
        objects.add((T)object);
      }
    }
    Collections.sort(objects);
    return objects;
    
  }

  /**
   * Finds a table
   * @param tableName the name of the table
   * @return the table or null
   */
  public SqlTable findTable(String tableName)
  {
    return findSqlObject(SqlTable.class, tableName);
  }
  
  /**
   * Finds a view
   * @param viewName the name of the view
   * @return the view or null
   */
  public SqlView findView(String viewName)
  {
    return findSqlObject(SqlView.class, viewName);
  }

  /**
   * Finds a sql object
   * @param <T> the type of the sql object
   * @param clazz the class of the sql object to find
   * @param id the id of the sql object
   * @return sql object or null
   */
  private <T extends SqlObject> T findSqlObject(Class<T> clazz, String id)
  {
    for (T sqlObject : getArtifacts(clazz))
    {
      if (sqlObject.getId().equals(id))
      {
        return sqlObject;
      }
    }
    return null;
  }

  /**
   * Merges this meta definition with the given one
   * @param metaDefinition the meta definition to merge to this one
   */
  public void merge(SqlMeta metaDefinition)
  {
    fSqlArtifact.addAll(metaDefinition.fSqlArtifact);
  }
  

  /**
   * Returns all foreign keys referencing the given <code>column</code>
   * @param table
   * @param column
   * @return empty list if not referenced
   */
  public List<Pair<SqlTable, SqlForeignKey>> getReferencingForeignKeys(SqlTable table, SqlTableColumn column)
  {
    List<Pair<SqlTable, SqlForeignKey>> result = new ArrayList<Pair<SqlTable, SqlForeignKey>>();
    for (SqlTable foreignTable : getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreignKey : foreignTable.getForeignKeys())
      {
        SqlReference reference = foreignKey.getReference();
        if (reference.getForeignTable().equals(table.getId()) &&
            reference.getForeignColumn().equals(column.getId()))
        {
          result.add(new ImmutablePair<SqlTable, SqlForeignKey>(foreignTable, foreignKey));
        }
      }
    }
    return result;
  }

}
