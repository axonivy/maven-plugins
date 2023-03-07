package ch.ivyteam.db.meta.model.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class of all sql artefacts that can be specified inside an sql meta
 * information
 * @author rwei
 */
public class SqlArtifact {

  /** The comment */
  private String fComment;
  /** The database system hints */
  private Map<String, SqlDatabaseSystemHints> fDatabaseSystemHints = new HashMap<String, SqlDatabaseSystemHints>();
  public static final SqlArtifact UNDEFINED = new SqlArtifact(Collections.emptyList(), "");

  /**
   * Constructor
   * @param dbSysHints
   * @param comment comment
   * @throws MetaException
   */
  public SqlArtifact(List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException {
    fComment = comment != null ? comment : "";
    if (dbSysHints != null) {
      for (SqlDatabaseSystemHints hints : dbSysHints) {
        if (fDatabaseSystemHints.containsKey(hints.getDatabaseManagementSystem())) {
          throw new MetaException(
                  "Hints for database system '" + hints.getDatabaseManagementSystem() + "' already defined");
        }
        fDatabaseSystemHints.put(hints.getDatabaseManagementSystem(), hints);
      }
    }
  }

  /**
   * Gets the comment
   * @return comment
   */
  public String getComment() {
    return fComment;
  }

  /**
   * Gets the database management system hints
   * @return database system management hints
   */
  public List<SqlDatabaseSystemHints> getDatabaseManagementSystemHints() {
    return new ArrayList<SqlDatabaseSystemHints>(fDatabaseSystemHints.values());
  }

  /**
   * Gets the hints for the given databaseManagemenSystem
   * @param databaseManagementSystem the database management system
   * @return the hints. Never null.
   */
  public SqlDatabaseSystemHints getDatabaseManagementSystemHints(String databaseManagementSystem) {
    SqlDatabaseSystemHints hints;
    hints = fDatabaseSystemHints.get(databaseManagementSystem);
    if (hints == null) {
      if (!SqlDatabaseSystemHintValidator.isKnownType(databaseManagementSystem)) {
        throw new IllegalArgumentException("Unknown hint type: " + databaseManagementSystem);
      }
      hints = new SqlDatabaseSystemHints(databaseManagementSystem);
    }
    return hints;
  }
}
