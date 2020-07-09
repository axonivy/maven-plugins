package ch.ivyteam.db.meta.model.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaEntityClassGenerator;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.hsql.HsqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.mssql.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.mysql.MySqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.oracle.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.postgresql.PostgreSqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.query.TableInfo;

/**
 * Used to check that only known system hints are used inside the meta-file,
 * to prevent typos etc.
 * @author fs
 * @since 13.12.2011
 */
public class SqlDatabaseSystemHintValidator
{
  private static final String COMMON_HINT_KEY = "COMMON";
  private static final Map<String, Set<String>> HINTS_PER_TYPE = new HashMap<>();
  private static final Set<String> HINT_TYPES = new HashSet<>();

  static
  {
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.DATA_TYPE);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_REFERENCE_USE_TRIGGER);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_ACTION_USE_TRIGGER);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_ACTION);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_REFERENCE);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.TRIGGER_EXECUTE_FOR_EACH_STATEMENT);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.USE_UNIQUE_INDEX);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.FOREIGN_TABLE);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.REFERENCE_ACTION);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.DELETE_TRIGGER_NAME);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_UNIQUE);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.INDEX_NAME);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.NO_INDEX);
    registerHint(COMMON_HINT_KEY, SqlScriptGenerator.DEFAULT_VALUE);
    
    registerType(JavaClassGenerator.JAVA);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.ADDITIONAL_SET_METHODS);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.AS_ASSOCIATION);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.AS_PARENT);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.ATTRIBUTE_NAME);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.CLASS_NAME);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.DATA_TYPE);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.ENUM);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.PARENT_CAN_BE_MODIFIED);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.PASSWORD);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.QUERY_TABLE_NAME);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.QUERY_FIELD_NAME);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.HIDE_FIELD_ON_QUERY);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.FILTER_QUERY_DATA_TYPE);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.SECONDARY_KEYS);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.TRUNCATE);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.FIELD_FOR_OPTIMISTIC_LOCKING);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.DEPRECATED);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.CUSTOM_FIELDS);
    registerHint(JavaClassGenerator.JAVA, JavaClassGenerator.SECURITY_MEMBER_LEGACY_FIELDS);
    registerHint(JavaClassGenerator.JAVA, TableInfo.BUSINESS_CLASS);

    registerType(JavaEntityClassGenerator.CACHE);
    registerHint(JavaEntityClassGenerator.CACHE, JavaEntityClassGenerator.STRATEGY);
    registerHint(JavaEntityClassGenerator.CACHE, JavaEntityClassGenerator.COUNT_LIMIT);
    registerHint(JavaEntityClassGenerator.CACHE, JavaEntityClassGenerator.USAGE_LIMIT);

    registerType(HsqlSqlScriptGenerator.HSQL_DB);

    registerType(MsSqlServerSqlScriptGenerator.MS_SQL_SERVER);

    registerType(MySqlSqlScriptGenerator.MYSQL);
    registerHint(MySqlSqlScriptGenerator.MYSQL, MySqlSqlScriptGenerator.INDEX_COLUMN_LENGTH);

    registerType(OracleSqlScriptGenerator.ORACLE);
    registerHint(OracleSqlScriptGenerator.ORACLE, OracleSqlScriptGenerator.CONVERT_EMPTY_STRING_TO_NULL);

    registerType(PostgreSqlSqlScriptGenerator.POSTGRESQL);
    registerHint(PostgreSqlSqlScriptGenerator.POSTGRESQL, PostgreSqlSqlScriptGenerator.CAST);
  }
    
  private static void registerHint(String type, String hint)
  {
    Set<String> databaseManagementSystemHint = HINTS_PER_TYPE.get(type);
    
    if (databaseManagementSystemHint == null)
    {
      databaseManagementSystemHint = new HashSet<String>();
      HINTS_PER_TYPE.put(type, databaseManagementSystemHint);
    }
    databaseManagementSystemHint.add(hint);
  }

  /**
   * @param type 
   * @param hint
   * @return - 
   */
  public static boolean isKnownHint(String type, String hint)
  {
    if (!checkHint(COMMON_HINT_KEY, hint))
    {
      return checkHint(type, hint);
    }
    return true;
  }
  
  private static boolean checkHint(String type, String hint)
  {
    Set<String> databaseManagementSystemHint = HINTS_PER_TYPE.get(type);
    if (databaseManagementSystemHint == null)
    {
      return false;
    }
    return databaseManagementSystemHint.contains(hint);
  }
  
  private static void registerType(String type)
  {
    HINT_TYPES.add(type);
  }
  
  /**
   * @param type 
   * @return -
   */
  public static boolean isKnownType(String type)
  {
    return HINT_TYPES.contains(type);
  }
}