package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Optional;

import ch.ivyteam.db.meta.generator.internal.mysql.MySqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.oracle.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.postgresql.PostgreSqlSqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;

public final class DbHints {

  public final DbHint DATA_TYPE = new DbHint(SqlScriptGenerator.DATA_TYPE);
  public final DbHint NO_REFERENCE_USE_TRIGGER = new DbHint(SqlScriptGenerator.NO_REFERENCE_USE_TRIGGER);
  public final DbHint NO_ACTION_USE_TRIGGER = new DbHint(SqlScriptGenerator.NO_ACTION_USE_TRIGGER);
  public final DbHint NO_ACTION = new DbHint(SqlScriptGenerator.NO_ACTION);
  public final DbHint NO_REFERENCE = new DbHint(SqlScriptGenerator.NO_REFERENCE);
  public final DbHint TRIGGER_EXECUTE_FOR_EACH_STATEMENT = new DbHint(
          SqlScriptGenerator.TRIGGER_EXECUTE_FOR_EACH_STATEMENT);
  public final DbHint USE_UNIQUE_INDEX = new DbHint(SqlScriptGenerator.USE_UNIQUE_INDEX);
  public final DbHint FOREIGN_TABLE = new DbHint(SqlScriptGenerator.FOREIGN_TABLE);
  public final DbHint REFERENCE_ACTION = new DbHint(SqlScriptGenerator.REFERENCE_ACTION);
  public final DbHint DELETE_TRIGGER_NAME = new DbHint(SqlScriptGenerator.DELETE_TRIGGER_NAME);
  public final DbHint NO_UNIQUE = new DbHint(SqlScriptGenerator.NO_UNIQUE);
  public final DbHint INDEX_NAME = new DbHint(SqlScriptGenerator.INDEX_NAME);
  public final DbHint NO_INDEX = new DbHint(SqlScriptGenerator.NO_INDEX);
  public final DbHint DEFAULT_VALUE = new DbHint(SqlScriptGenerator.DEFAULT_VALUE);
  public final DbHint CAST = new DbHint(PostgreSqlSqlScriptGenerator.CAST);
  public final DbHint CONVERT_EMPTY_STRING_TO_NULL = new DbHint(
          OracleSqlScriptGenerator.CONVERT_EMPTY_STRING_TO_NULL);
  public final DbHint INDEX_COLUMN_LENGTH = new DbHint(MySqlSqlScriptGenerator.INDEX_COLUMN_LENGTH);
  private final String databaseSystemName;

  public DbHints(String databaseSystemName) {
    this.databaseSystemName = databaseSystemName;
  }

  public final class DbHint {

    private final String name;

    private DbHint(String name) {
      this.name = name;
    }

    /**
     * Checks if the given database system hint is set on the given sql
     * artifact. All database systems returned by {@link #databaseSystemName}
     * are considered.
     * @param artifact the artifact on which to check if the hint is set
     * @return true if hint is set, otherwise false
     */
    public boolean isSet(SqlArtifact artifact) {
      return artifact.getDatabaseManagementSystemHints(databaseSystemName).isHintSet(name);
    }

    /**
     * Gets the value of the given database system hint on the given sql
     * artifact. All database systems returned by {@link #databaseSystemName}
     * are considered. If the hint is set on multiple valid database systems the
     * first found is returned.
     * @param artifact the artifact on which the hint value is get
     * @return hint value.
     */
    public String value(SqlArtifact artifact) {
      if (isSet(artifact)) {
        return artifact.getDatabaseManagementSystemHints(databaseSystemName).getHintValue(name);
      }
      return null;
    }

    public Optional<String> valueIfSet(SqlArtifact artifact) {
      if (isSet(artifact)) {
        return Optional.of(value(artifact));
      }
      return Optional.empty();
    }

    public void generate(PrintWriter pr, SqlArtifact artifact) {
      pr.print(value(artifact));
    }

    public String name() {
      return name;
    }
  }
}
