package ${packageName};

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ch.ivyteam.ivy.persistence.IPersistentObject;
import ch.ivyteam.ivy.persistence.IPersistentTransaction;
import ch.ivyteam.ivy.persistence.KeyType;
import ch.ivyteam.ivy.persistence.PersistencyException;
import ch.ivyteam.ivy.persistence.db.DatabasePersistencyService;
import ch.ivyteam.ivy.persistence.db.DatabaseTableClassPersistencyService;
import ch.ivyteam.db.sql.ColumnName;
import ${table.entityClass};

/**
 * ----------------------------------------------------------------------<br>
 * This code was generated by the JavaClassPersistencyServiceImplemenationGenerator.<br>
 * Do not edit this class instead edit the meta information this class was generated of.<br>
 * ----------------------------------------------------------------------<br>
 * Class Persistency Service Implementation for table ${table.name}<br>
 * ----------------------------------------------------------------------<br>
 * This class is responsible to provide CRUD operations for the table ${table.name}<br>
 * and a relation to object mapping for the entity class ${table.simpleEntityClass}<br>
 * ----------------------------------------------------------------------<br>
 * ${table.comment}
 * ----------------------------------------------------------------------<br>
 */

public class Db${table.simpleEntityClass} extends DatabaseTableClassPersistencyService<${table.simpleEntityClass}>
{
  /** Name of the DB Table */
  public static final String TABLENAME = "${table.name}";
<#if table.query??>  
  /** The Name of the table to use in the query */
  public static final String QUERY_TABLENAME = "${table.query}";
<#else>
  /** The Name of the table to use in the query */
  public static final String QUERY_TABLENAME = "${table.name}";
</#if>
  /** Prefix of the table name */
  private static final String PREFIX_TABLENAME = "IWA_";
  /** Name of the primary key column */
  private static final String PRIMARY_KEY_COLUMN_NAME = "${table.primaryKey.name}";
  /** Full qualified name of the parent key column  */
  public static final ColumnName PRIMARY_KEY_COLUMN = new ColumnName(TABLENAME, PRIMARY_KEY_COLUMN_NAME);
<#if table.parentKey??>  
  /** Name of the parent Key column */
  private static final String PARENT_FOREIGN_KEY_COLUMN_NAME = "${table.parentKey.name}";
  /** Full qualified name of the parent key column  */
  public static final ColumnName PARENT_FOREIGN_KEY_COLUMN = new ColumnName(TABLENAME, PARENT_FOREIGN_KEY_COLUMN_NAME);
</#if>
<#list columns as column>  
  /** Name of the column ${column.name} */
  private static final String COLUMN_NAME_${column.constant} = "${column.name}";
  /** Full qualified name of the column ${column.name} */
  public static final ColumnName COLUMN_${column.constant} = new ColumnName(TABLENAME, COLUMN_NAME_${column.constant});
</#list>
<#list associations as association>  
  /** Table name of association ${association.name} */
  public static final String ASSOCIATION_${association.constant}_TABLE_NAME = "${association.table}";
  /** Foreign key column ${association.foreignKey} of association table ${association.table} that references rows of this table */
  public static final ColumnName ASSOCIATION_${association.constant}_COLUMN_${association.foreignKeyConstant} = new ColumnName(ASSOCIATION_${association.constant}_TABLE_NAME, "${association.foreignKey}");
</#list>

  /**
   * Constructor
   * @param database the database persistency service
   */
  public Db${table.simpleEntityClass}(DatabasePersistencyService database)
  {
    super(database, 
      ${table.simpleEntityClass}.class,
      PREFIX_TABLENAME,
      TABLENAME,
      QUERY_TABLENAME,
      KeyType.${table.primaryKey.keyType},
      PRIMARY_KEY_COLUMN_NAME,
<#if table.parentKey??>
      PARENT_FOREIGN_KEY_COLUMN_NAME,
<#else>
      null, // parent foreign key
</#if> 
      new String[]{ // all columns
        PRIMARY_KEY_COLUMN_NAME,
<#if table.parentKey??>
        PARENT_FOREIGN_KEY_COLUMN_NAME,
</#if>      
<#list columnsWithoutPrimaryAndParent as column>
        COLUMN_NAME_${column.constant}<#if column_has_next>,</#if>
</#list>
      },
      new String[]{ // long binary (blob) columns
<#list longBinaryColumns as column>
        COLUMN_NAME_${column.constant}<#if column_has_next>,</#if>
</#list>
      },
      new String[]{ // long character (clob) columns
<#list longCharacterColumns as column>
        COLUMN_NAME_${column.constant}<#if column_has_next>,</#if>
</#list>
      },
<#if queryViewColumns??> 
      new String[]{ // view columns
   <#list queryViewColumns as column>
        QueryView.VIEW_COLUMN_NAME_${column.constant}<#if column_has_next>,</#if>
  </#list>
      },
      new String[]{  // view column aliases
  <#list queryViewColumns as column>
    <#if column.alias??>
        "${column.alias}"<#if column_has_next>,</#if> // alias for ${column.name}
    <#else>
        null<#if column_has_next>,</#if>
    </#if>
  </#list>
      });    
<#else>
      null, // view columns
      null); // view column aliases
</#if>            
  }

  /**
   * @see DatabaseTableClassPersistencyService#createObjectFromResultSet(IPersistentTransaction, ResultSet)
   */
  @Override
  protected ${table.simpleEntityClass} createObjectFromResultSet(IPersistentTransaction transaction, ResultSet result) throws PersistencyException, SQLException
  {
<#assign row=1>
    return new ${table.simpleEntityClass}(
      database.get${table.primaryKey.method}(result, ${row}${table.primaryKey.additionalReadArgs})<#if (columnsWithoutPrimaryParentAndLob?size > 0 || table.parentKey??)>,</#if> // ${table.primaryKey.sql}
<#assign row=row+1>      
<#if table.parentKey??>
      database.get${table.parentKey.method}(result, ${row}${table.parentKey.additionalReadArgs})<#if (columnsWithoutPrimaryParentAndLob?size > 0)>,</#if> // ${table.parentKey.sql}
  <#assign row=row+1>      
</#if>            
<#list columnsWithoutPrimaryParentAndLob as column>
  <#if column.isPassword()>
      database.decode(transaction, database.get${column.method}(result, ${row}${column.additionalReadArgs}))<#if column_has_next>,</#if> // ${column.sql}
  <#else>
      database.get${column.method}(result, ${row}${column.additionalReadArgs})<#if column_has_next>,</#if> // ${column.sql}
  </#if>
  <#assign row=row+1>      
</#list>
    );
  }

  /**
   * @see DatabaseTableClassPersistencyService#writeDataToUpdateStatement(IPersistentTransaction, IPersistentObject, PreparedStatement)   */
  @Override
  protected void writeDataToUpdateStatement(IPersistentTransaction transaction, ${table.simpleEntityClass} data, PreparedStatement stmt) throws PersistencyException
  {
<#assign row=1>
<#if table.parentKey??>      
    // ${table.parentKey.sql}
    database.set${table.parentKey.method}(stmt, ${row}, ${table.parentKey.additionalWriteArgs}, "${table.parentKey.fullName}");
  <#assign row=row+1>      
</#if>
<#list columnsWithoutPrimaryParentAndLob as column>
    // ${column.sql}
    database.set${column.method}(stmt, ${row}, ${column.additionalWriteArgs}, "${column.fullName}");
  <#assign row=row+1>
</#list>
    // ${table.primaryKey.sql}
    database.set${table.primaryKey.method}(stmt, ${row}, ${table.primaryKey.additionalWriteArgs}, "${table.primaryKey.fullName}");
  }

  /**
   * @see DatabaseTableClassPersistencyService#writeDataToInsertStatement(IPersistentTransaction, IPersistentObject, PreparedStatement)   */
  @Override
  protected void writeDataToInsertStatement(IPersistentTransaction transaction, ${table.simpleEntityClass} data, PreparedStatement stmt) throws PersistencyException
  {
<#assign row=1>
    // ${table.primaryKey.sql}
    database.set${table.primaryKey.method}(stmt, ${row}, ${table.primaryKey.additionalWriteArgs}, "${table.primaryKey.fullName}");
<#assign row=row+1>
<#if table.parentKey??>      
    // ${table.parentKey.sql}
    database.set${table.parentKey.method}(stmt, ${row}, ${table.parentKey.additionalWriteArgs}, "${table.parentKey.fullName}");
  <#assign row=row+1>
</#if>
<#list columnsWithoutPrimaryAndParent as column>
    // ${column.sql}
    database.set${column.method}(stmt, ${row}, ${column.additionalWriteArgs}, "${column.fullName}");
  <#assign row=row+1>
</#list>
  }
  
<#if table.query??>
  /**
   * This class defines constants for the columns that the ${table.query} view declares
   */
  public static class QueryView
  {
<#list queryViewColumns as column>
    /** Name of the column ${column.name} */
    private static final String VIEW_COLUMN_NAME_${column.constant} = "${column.name}";
    /** Full qualified name of the column ${column.name} */
    public static final ColumnName VIEW_COLUMN_${column.constant} = new ColumnName(QUERY_TABLENAME, VIEW_COLUMN_NAME_${column.constant});
</#list>  
  }
</#if>  
}