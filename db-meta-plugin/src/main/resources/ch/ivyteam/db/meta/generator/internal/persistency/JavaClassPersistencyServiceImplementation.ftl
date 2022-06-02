package ${packageName};

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import ch.ivyteam.ivy.persistence.IPersistentTransaction;
import ch.ivyteam.ivy.persistence.restricted.db.meta.KeyType;
import ch.ivyteam.ivy.persistence.restricted.db.meta.Table;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn.Type;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn.Option;
<#if table.query??>  
import ch.ivyteam.ivy.persistence.restricted.db.meta.ViewColumn;
</#if>
import ch.ivyteam.ivy.persistence.PersistencyException;
import ch.ivyteam.ivy.persistence.db.DatabasePersistencyService;
import ch.ivyteam.ivy.persistence.db.DatabaseTableClassPersistencyService;
import ch.ivyteam.db.sql.ColumnName;
import ${table.entityClass};

public class Db${table.simpleEntityClass} extends DatabaseTableClassPersistencyService<${table.simpleEntityClass}> {

  public static final String TABLENAME = "${table.name}";

<#if table.query??>  
  public static final String QUERY_TABLENAME = "${table.query}";

</#if>
  private static final String PRIMARY_KEY_COLUMN_NAME = "${table.primaryKey.name}";
  public static final ColumnName PRIMARY_KEY_COLUMN = new ColumnName(TABLENAME, PRIMARY_KEY_COLUMN_NAME);

<#if table.parentKey??>  
  private static final String PARENT_FOREIGN_KEY_COLUMN_NAME = "${table.parentKey.name}";
  public static final ColumnName PARENT_FOREIGN_KEY_COLUMN = new ColumnName(TABLENAME, PARENT_FOREIGN_KEY_COLUMN_NAME);

</#if>
<#if table.fieldForOptimisticLocking??>  
  private static final String OPTIMISTIC_LOCKING_COLUMN_NAME = "${table.fieldForOptimisticLocking.name}";

  public static final ColumnName OPTIMISTIC_LOCKING_COLUMN = new ColumnName(TABLENAME, OPTIMISTIC_LOCKING_COLUMN_NAME);

</#if>
<#list columns as column>  
  private static final String COLUMN_NAME_${column.constant} = "${column.name}";
  public static final ColumnName COLUMN_${column.constant} = new ColumnName(TABLENAME, COLUMN_NAME_${column.constant});

</#list>
<#list associations as association>  
  public static final String ASSOCIATION_${association.constant}_TABLE_NAME = "${association.table}";

  public static final ColumnName ASSOCIATION_${association.constant}_COLUMN_${association.foreignKeyConstant} = new ColumnName(ASSOCIATION_${association.constant}_TABLE_NAME, "${association.foreignKey}");

</#list>
  public Db${table.simpleEntityClass}(DatabasePersistencyService database) {
    super(database, 
      ${table.simpleEntityClass}.class,
      new Table(
        TABLENAME, 
        KeyType.${table.primaryKey.keyType},
        Arrays.asList( 
<#list columns as column>
  <#if column.isPrimaryKey()>
          new TableColumn(PRIMARY_KEY_COLUMN, Type.${table.primaryKey.sqlDataType}, Option.PRIMARY_KEY)<#if column_has_next>,</#if>
  <#elseif column.isParentKey()>
          new TableColumn(PARENT_FOREIGN_KEY_COLUMN, Type.${table.primaryKey.sqlDataType}, Option.PARENT_KEY)<#if column_has_next>,</#if>
  <#else> 
          new TableColumn(COLUMN_${column.constant}, Type.${column.sqlDataType}<#if column.isOptimisticLockingColumn()>, Option.USE_FOR_OPTIMISTICAL_LOCKING</#if><#if column.isLob()>, Option.LARGE_OBJECT</#if>)<#if column_has_next>,</#if>
  </#if>         
</#list>
<#if table.query??>
        ),
<#else>
        )
</#if>        
<#if table.query??>
        QUERY_TABLENAME,
        Arrays.asList( 
   <#list queryViewColumns as column>
          new ViewColumn(QueryView.VIEW_COLUMN_${column.constant}<#if column.alias??>, "${column.alias}"</#if>)<#if column_has_next>,</#if>
  </#list>
        )
</#if>
      )
    ); 
  }

  @Override
  protected ${table.simpleEntityClass} createObjectFromResultSet(IPersistentTransaction transaction, ResultSet result) throws PersistencyException, SQLException {
<#assign row=1>
    return new ${table.simpleEntityClass}(
      database.get${table.primaryKey.method}(result, ${row}${table.primaryKey.additionalReadArgs})<#if (columnsWithoutPrimaryParentAndLob?size > 0 || table.parentKey??)>,</#if>
<#assign row=row+1>      
<#if table.parentKey??>
      database.get${table.parentKey.method}(result, ${row}${table.parentKey.additionalReadArgs})<#if (columnsWithoutPrimaryParentAndLob?size > 0)>,</#if>
  <#assign row=row+1>      
</#if>            
<#list columnsWithoutPrimaryParentAndLob as column>
  <#if column.isPassword()>
    <#if column.optionalPasswordColumn??>
      database.optionalDecode(transaction, database.get${column.optionalPasswordColumn.method}(result, ${column.optionalPasswordColumn.resultSetRowNumber}${column.optionalPasswordColumn.additionalReadArgs}), database.get${column.method}(result, ${row}${column.additionalReadArgs}))<#if column_has_next>,</#if>
    <#else> 
      database.decode(transaction, database.get${column.method}(result, ${row}${column.additionalReadArgs}))<#if column_has_next>,</#if>
    </#if>
  <#else>
      database.get${column.method}(result, ${row}${column.additionalReadArgs})<#if column_has_next>,</#if>
  </#if>
  <#assign row=row+1>      
</#list>
    );
  }

  @Override
  protected void writeDataToUpdateStatement(IPersistentTransaction transaction, ${table.simpleEntityClass} data, PreparedStatement stmt) {
<#assign row=1>
<#if table.parentKey??>      
    database.set${table.parentKey.method}(stmt, ${row}, ${table.parentKey.additionalWriteArgs}, PARENT_FOREIGN_KEY_COLUMN);
  <#assign row=row+1>      
</#if>
<#list columnsWithoutPrimaryParentAndLob as column>
  <#if column.isOptimisticLockingColumn()>
  <#else>
    database.set${column.method}(stmt, ${row}, ${column.additionalWriteArgs}, COLUMN_${column.constant});
    <#assign row=row+1>
  </#if>
</#list>
    database.set${table.primaryKey.method}(stmt, ${row}, ${table.primaryKey.additionalWriteArgs}, PRIMARY_KEY_COLUMN);
  }

  @Override
  protected void writeDataToInsertStatement(IPersistentTransaction transaction, ${table.simpleEntityClass} data, PreparedStatement stmt) {
<#assign row=1>
    database.set${table.primaryKey.method}(stmt, ${row}, ${table.primaryKey.additionalWriteArgs}, PRIMARY_KEY_COLUMN);
<#assign row=row+1>
<#if table.parentKey??>      
    database.set${table.parentKey.method}(stmt, ${row}, ${table.parentKey.additionalWriteArgs}, PARENT_FOREIGN_KEY_COLUMN);
  <#assign row=row+1>
</#if>
<#list columnsWithoutPrimaryAndParent as column>
    database.set${column.method}(stmt, ${row}, ${column.additionalWriteArgs}, COLUMN_${column.constant});
  <#assign row=row+1>
</#list>
  }

<#if table.fieldForOptimisticLocking??>
  @Override
  protected void writeDataToOptimisticUpdateStatement(IPersistentTransaction transaction, ${table.simpleEntityClass} data, PreparedStatement stmt) {
    database.set${table.fieldForOptimisticLocking.method}(stmt, ${numberOfColumns}, data.get${table.fieldForOptimisticLocking.name}(), COLUMN_${table.fieldForOptimisticLocking.constant});
  }
</#if>  

<#if table.query??>
  public static class QueryView {
<#list queryViewColumns as column>
    private static final String VIEW_COLUMN_NAME_${column.constant} = "${column.name}";

    public static final ColumnName VIEW_COLUMN_${column.constant} = new ColumnName(QUERY_TABLENAME, VIEW_COLUMN_NAME_${column.constant});

</#list>
  }
</#if>  
}
