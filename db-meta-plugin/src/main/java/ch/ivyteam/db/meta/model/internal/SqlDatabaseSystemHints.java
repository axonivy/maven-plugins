package ch.ivyteam.db.meta.model.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * Database System Hints. These Hints are used to control the generation of the sql script for the given database system
 * @author rwei
  */
public class SqlDatabaseSystemHints
{
  /** The database management system */
  private String fDatabaseManagementSystem;
  
  /** The hints for this database management system */
  private Map<String, SqlDatabaseSystemHint> fHints = new HashMap<String, SqlDatabaseSystemHint>();
  
  /**
   * Constructor
   * @param dbms the database management system
   * @param hints the hints
   * @throws MetaException if same hint is defined more than once
   */
  public SqlDatabaseSystemHints(String dbms, List<SqlDatabaseSystemHint> hints) throws MetaException
  {
    this(dbms);
    assert hints != null : "Parameter hints must not be null";
    for (SqlDatabaseSystemHint hint: hints)
    {
      String hintName = hint.getName();
      if (fHints.containsKey(hintName))
      {
        throw new MetaException("System database hint '"+hint.getName()+"' already definied");
      }
      if (!SqlDatabaseSystemHintValidator.isKnownHint(dbms, hintName))
      {
        throw new IllegalStateException("Unknown hint '"+ dbms + ":" + hintName +"'");
      }
      fHints.put(hint.getName(), hint);
    }
  }
  
  /**
   * Constructor
   * @param dbms the database management system
   */
  SqlDatabaseSystemHints(String dbms)
  {
    assert dbms != null : "Paramter dbms must not be null";
    fDatabaseManagementSystem = dbms;
  }

  /**
   * Gets the database management system
   * @return database management system
   */
  public String getDatabaseManagementSystem()
  {
    return fDatabaseManagementSystem;
  }

  /**
   * Checks if a hint is set or not
   * @param hint the hint to check
   * @return true if hint was set otherwise false
   */
  public boolean isHintSet(String hint)
  {
    return fHints.containsKey(hint);
  }
  
  /**
   * Gets the value of the given hint
   * @param hint the hint
   * @return value or null if hint has no value or hint was not set
   */
  public String getHintValue(String hint)
  {
    SqlDatabaseSystemHint value;
    
    value = fHints.get(hint);
    if (value != null)
    {
      return value.getValue();
    }
    return null;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(1024);
    builder.append("FOR ");
    builder.append(fDatabaseManagementSystem);
    builder.append(" USE (");
    SqlScriptUtil.formatCommaSeparated(builder, fHints.values());
    builder.append(")");
    return builder.toString();
  }

  /**
   * @return true if no system database hints are set
   */
  public boolean isEmpty()
  {
    return fHints.isEmpty();
  }

  /**
   * Gets the names of all defined hints
   * @return set with the names of all defined hints
   */
  public Set<String> getHintNames()
  {
    return fHints.keySet();
  }
}
