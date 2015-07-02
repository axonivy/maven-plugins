package ${packageName};

import java.util.Date;
import org.apache.commons.lang.StringUtils;

import ch.ivyteam.api.API;
import ch.ivyteam.api.IvyScriptVisibility;
import ch.ivyteam.api.PublicAPI;
import ch.ivyteam.api.Reviewed;
import ch.ivyteam.db.sql.ColumnName;
import ch.ivyteam.ivy.persistence.query.Query;
import ch.ivyteam.ivy.persistence.restricted.query.PersistentFilter;
import ch.ivyteam.ivy.workflow.internal.db.*;

/**
<#include table.queryClassName +"_JavaDoc.ftl">

 */
@PublicAPI(IvyScriptVisibility.EXPERT)
@Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
public class ${table.queryClassName} extends Query<${table.businessClassName}>
{
  /**
   * Constructor
   */
  protected ${table.queryClassName}()
  {
    super();
  }

  ${table.queryClassName} (Query<${table.businessClassName}> parentQuery)
  {
    super(parentQuery);
  }

  /**
   * Creates a new query
   * @return A new instance of ${table.queryClassName}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static ${table.queryClassName} create()
  {
    return new ${table.queryClassName}();
  }

  /**
   * <p>Returns an object which contains the aggregation part for this query.<br>
   * It provides methods to perform aggregations on the query. For each aggregation a column is added to the result set. </p>
<#include table.queryClassName +"_Aggregate_JavaDoc.ftl">

   * @return aggregate query
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public IAggregationQuery aggregate()
  {
    validate();
    return new AggregationQuery(this);
  }

  /**
   * <p>Returns an object which contains the group by part for this query.<br>
   * It provides methods to group the result by certain fields.</p>
<#include table.queryClassName +"_GroupBy_JavaDoc.ftl">

   * @return A query group by builder to add group by statements
   * @see AggregationQuery#countRows()
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public IGroupByQueryColumns groupBy()
  {
    validate();
    return new GroupByQuery(this);
  }

  /**
   * <p>Returns an object which contains the order by part for this query.<br>
   * It provides methods to order the result by certain columns.</p>
   * @return An order by query builder to add order by statements
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public IOrderByQueryColumns orderBy()
  {
    validate();
    return new OrderByQuery(this);
  }

  /**
   * <p>Returns an object which contains the where part for this query.<br>
   * It provides methods to filter the result by certain columns.</p>
   * @return An filter query builder to add where statements
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public IFilterQuery where() 
  {
    validate();
    return new FilterQuery(this);
  }

  /**
   * Provides filter functionality for {@link ${table.businessClassName}}
   * <p>
   * <b>Example:</b>
   * <code><pre>${table.queryClassName}.create().where().customVarCharField1().isEqual("Hello World").and().customDecimalField2().isGreaterThan(15.3);</pre></code>
   * Corresponds to SQL:
   * <code><pre>SELECT * FROM IWA_${table.name} WHERE CustomVarCharField1 = 'Hello World' AND CustomDecimalField1 > 15.3</pre></code>
   * </p>
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IFilterQuery extends IFilterableColumns, IFilterLink
  {
  }

  /**
   * Provides filter functionality for {@link ${table.businessClassName}}
   * <p>
   * <b>Example:</b>
   * <code><pre>${table.queryClassName}.create().where().customVarCharField1().isEqual("Hello World").and().customDecimalField2().isGreaterThan(15.3);</pre></code>
   * Corresponds to SQL:
   * <code><pre>SELECT * FROM IWA_${table.name} WHERE CustomVarCharField1 = 'Hello World' AND CustomDecimalField1 > 15.3</pre></code>
   * </p>
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IFilterableColumns
  {
<#include "Extended"+table.queryClassName +".java:InterfaceFilterQuery">

    /**
     * <p>Adds a condition, which negates a set of where conditions given by the <code>otherQuery</code> with a NOT expression.<br/>
     * Only the where clause of the given <code>otherQuery</code> is considered. All other parts are ignored.</p>
     * <p>SQL part: <code>NOT([otherSqlExpression])</code></p>
     * <p>
     *   <b>Example:</b>
     *   <code><pre>${table.queryClassName} subQuery = ${table.queryClassName}.create().where()
     *      .customVarCharField1().equals("a").or()
     *      .customVarCharField2().equals("b")
     *${table.queryClassName} query = ${table.queryClassName}.create().where()
     *      .not(subQuery)</pre></code>
     *   Corresponds to SQL:
     *   <code><pre>SELECT * FROM IWA_${table.name}
     *  WHERE NOT(
     *    customVarCharField1 = 'a'
     *    OR customVarCharField2 = 'b')</pre></code>
     * </p>
     * @param otherQuery Query from which the negated where part will be added to the current query.
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterQuery not(${table.queryClassName} otherQuery);

<#list columns as column>
    /**
     * <p>Prepares a where statement for the column <code>${column.name}</code>.<br>
     * Must be followed by a call to a condition method.</p>
<#if column.additionalComments??>
     * ${column.additionalComments}
</#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    <#if column.supportsStringOption()>
    public IStringColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#elseif column.isEnumeration()>
    public I${column.getEnumerationInfo().getSimpleName()}FilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#elseif column.supportsIntegerOption()>
    public IIntegerColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#elseif column.supportsDecimalOption()>
    public INumberColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#elseif column.supportsDateTimeOption()>
    public IDateColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#elseif column.supportsBooleanOption()>
    public IBooleanColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    <#else>
    public IColumnFilterQuery ${StringUtils.uncapitalize(column.name)}();
    </#if>

</#list>
  }

  /**
   * Provides filter functionality for {@link ${table.businessClassName}}
   * <p>
   * <b>Example:</b>
   * <code><pre>${table.queryClassName}.create().where().customVarCharField1().isEqual("Hello World").and().customDecimalField2().isGreaterThan(15.3);</pre></code>
   * Corresponds to SQL:
   * <code><pre>SELECT * FROM IWA_${table.name} WHERE CustomVarCharField1 = 'Hello World' AND CustomDecimalField1 > 15.3</pre></code>
   * </p>
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static class FilterQuery extends FilterLink implements IFilterQuery
  {
    private FilterQuery(Query<${table.businessClassName}> parentQuery)
    {
      super(parentQuery);
    }

    <#include "Extended"+table.queryClassName +".java:FilterQuery">

    /**
     * <p>Adds a condition, which negates a set of where conditions given by the <code>otherQuery</code> with a NOT expression.<br/>
     * Only the where clause of the given <code>otherQuery</code> is considered. All other parts are ignored.</p>
     * <p>SQL part: <code>NOT([otherSqlExpression])</code></p>
     * <p>
     *   <b>Example:</b>
     *   <code><pre>${table.queryClassName} subQuery = ${table.queryClassName}.create().where()
     *      .customVarCharField1().equals("a").or()
     *      .customVarCharField2().equals("b")
     *${table.queryClassName} query = ${table.queryClassName}.create().where()
     *      .not(subQuery)</pre></code>
     *   Corresponds to SQL:
     *   <code><pre>SELECT * FROM IWA_${table.name}
     *  WHERE NOT(
     *    customVarCharField1 = 'a'
     *    OR customVarCharField2 = 'b')</pre></code>
     * </p>
     * @param otherQuery Query from which the negated where part will be added to the current query.
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    @Override
    public FilterQuery not(${table.queryClassName} otherQuery)
    {
      endColumnFilter();
      getQueryBuilder().not(getFilterForSubExpression(otherQuery));
      return this;
    }

<#list columns as column>
    /**
     * @see ${packageName}.${table.queryClassName}.IFilterableColumns#${StringUtils.uncapitalize(column.name)}()
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    @Override
    <#if column.supportsStringOption()>
    public IStringColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new StringColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    <#elseif column.isEnumeration()>
    public I${column.getEnumerationInfo().getSimpleName()}FilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new ${column.getEnumerationInfo().getSimpleName()}FilterQuery(this, ${column.javaColumnConstantName});
    }
    <#elseif column.supportsIntegerOption()>
    public IIntegerColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new IntegerColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    <#elseif column.supportsDecimalOption()>
    public INumberColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new NumberColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    <#elseif column.supportsDateTimeOption()>
    public IDateColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new DateColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    <#elseif column.supportsBooleanOption()>
    public IBooleanColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new BooleanColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    <#else>
    public IColumnFilterQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new ColumnFilterQuery(this, ${column.javaColumnConstantName});
    }
    </#if>

</#list>
  }

  /**
   * Basic filter functionality provider for a column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with NULL values.</p>
     * <p>SQL part: <code>[column] IS NULL</code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNull();

    /**
     * <p>Adds a filter condition that selects rows that do not have NULL values.</p>
     * <p>SQL part: <code>[column] IS NOT NULL</code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotNull();
  }

  /**
   * Basic filter functionality provider for a column of {@link ${table.businessClassName}}
   */
  private static class ColumnFilterQuery extends ${table.queryClassName} implements IColumnFilterQuery
  {
    private ColumnName column;

    private ColumnFilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery);
      startColumnFilter(column);
      this.column = column;
    }

    /**
     * @return -
     */
    protected ColumnName getColumn()
    {
      return column;
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IColumnFilterQuery#isNull()
     */
    @Override
    public FilterLink isNull()
    {
      endColumnFilter();
      getQueryBuilder().isNull(getColumn());
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IColumnFilterQuery#isNull()
     */
    @Override
    public FilterLink isNotNull()
    {
      endColumnFilter();
      getQueryBuilder().isNotNull(getColumn());
      return new FilterLink(this);
    }
  }

<#list enumerationInfos as enumInfo>
  /**
   * <p>Provides filter functionality for a {@link ${enumInfo.getName()}} column of {@link ${table.businessClassName}}</p>
   * <p>Example:
   * <code><pre>TaskQuery.create().where().state().isEqual(TaskState.DONE)</pre></code>
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface I${enumInfo.getSimpleName()}FilterQuery extends IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code> or <code>[column] IS NULL</code> </p>
     * @param value 
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(${enumInfo.getName()} value);

    /**
     * <p>Adds a filter condition that selects the rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code> or <code>[column] IS NOT NULL</code> </p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(${enumInfo.getName()} value);
  }

  /**
   * <p>Provides filter functionality for a {@link ${enumInfo.getName()}} column of {@link ${table.businessClassName}}</p>
   * <p>Example:
   * <code><pre>TaskQuery.create().where().state().isEqual(TaskState.DONE)</pre></code>
   */
  private static class ${enumInfo.getSimpleName()}FilterQuery extends ColumnFilterQuery implements I${enumInfo.getSimpleName()}FilterQuery
  {
    private ${enumInfo.getSimpleName()}FilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.I${enumInfo.getSimpleName()}FilterQuery#isEqual(${enumInfo.getName()})
     */
    @Override
    public FilterLink isEqual(${enumInfo.getName()} value)
    {
      endColumnFilter();
      getQueryBuilder().equal(getColumn(), (value==null) ? null : value.intValue());
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.I${enumInfo.getSimpleName()}FilterQuery#isNotEqual(${enumInfo.getName()})
     */
    @Override
    public FilterLink isNotEqual(${enumInfo.getName()} value)
    {
      endColumnFilter();
      getQueryBuilder().unequal(getColumn(), (value==null) ? null : value.intValue());
      return new FilterLink(this);
    }
  }

</#list>

<#if table.hasStringColumns>
  /**
   * Provides filter functionality for a string column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IStringColumnFilterQuery extends IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = '[value]'</code> or <code>[column] IS NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(String value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt; '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterThan(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt;= '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterOrEqualThan(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt; '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerThan(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;= '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerOrEqualThan(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are like the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] IS LIKE '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLike(String value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are not like the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] IS NOT LIKE '[value]'</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotLike(String value);
  }

  /**
   * Provides filter functionality for a string column of {@link ${table.businessClassName}}
   */
  static class StringColumnFilterQuery extends ColumnFilterQuery implements IStringColumnFilterQuery
  {
    public StringColumnFilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isEqual(String)
     */
    @Override
    public FilterLink isEqual(String value)
    {
      endColumnFilter();
      getQueryBuilder().equal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isNotEqual(String)
     */
    @Override
    public FilterLink isNotEqual(String value)
    {
      endColumnFilter();
      getQueryBuilder().unequal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isGreaterThan(String)
     */
    @Override
    public FilterLink isGreaterThan(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isGreaterOrEqualThan(String)
     */
    @Override
    public FilterLink isGreaterOrEqualThan(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isLowerThan(String)
     */
    @Override
    public FilterLink isLowerThan(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isLowerOrEqualThan(String)
     */
    @Override
    public FilterLink isLowerOrEqualThan(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isLike(String)
     */
    @Override
    public FilterLink isLike(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().like(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IStringColumnFilterQuery#isNotLike(String)
     */
    @Override
    public FilterLink isNotLike(String value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().notLike(getColumn(), value);
      return new FilterLink(this);
    }
  }
</#if>

<#if table.hasNumberColumns>
  /**
   * Provides filter functionality for a decimal number column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface INumberColumnFilterQuery extends IColumnFilterQuery
  {

    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code> or <code>[column] IS NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(Number value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code> or <code>[column] IS NOT NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(Number value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt; [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterThan(Number value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt;= [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterOrEqualThan(Number value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt; [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerThan(Number value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;= [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerOrEqualThan(Number value);
  }

  /**
   * Provides filter functionality for a decimal number column of {@link ${table.businessClassName}}
   */
  private static class NumberColumnFilterQuery extends ColumnFilterQuery implements INumberColumnFilterQuery
  {
    private NumberColumnFilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isEqual(Number)
     */
    @Override
    public FilterLink isEqual(Number value)
    {
      endColumnFilter();
      getQueryBuilder().equal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isNotEqual(Number)
     */
    @Override
    public FilterLink isNotEqual(Number value)
    {
      endColumnFilter();
      getQueryBuilder().unequal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isGreaterThan(Number)
     */
    @Override
    public FilterLink isGreaterThan(Number value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isGreaterOrEqualThan(Number)
     */
    @Override
    public FilterLink isGreaterOrEqualThan(Number value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isLowerThan(Number)
     */
    @Override
    public FilterLink isLowerThan(Number value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.INumberColumnFilterQuery#isLowerOrEqualThan(Number)
     */
    @Override
    public FilterLink isLowerOrEqualThan(Number value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }
  }
</#if>

<#if table.hasIntegerColumns>
  /**
   * Provides filter functionality for an integer column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IIntegerColumnFilterQuery extends IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(int value);

    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(long value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(int value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(long value);
    
    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterThan(int value);
    
    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterThan(long value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt;= [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterOrEqualThan(int value);
    
        /**
     * <p>Adds a filter condition that selects rows that have values that are greater or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt;= [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterOrEqualThan(long value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerThan(int value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt; [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerThan(long value);
    
    /**
     * <p>Adds a filter condition that selects rows that have values that are lower or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;= [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.HIDDEN)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerOrEqualThan(int value);
    
        /**
     * <p>Adds a filter condition that selects rows that have values that are lower or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;= [value]</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerOrEqualThan(long value);
  }

  /**
   * Provides filter functionality for an integer column of {@link ${table.businessClassName}}
   */
  private static class IntegerColumnFilterQuery extends ColumnFilterQuery implements IIntegerColumnFilterQuery
  {
    private NumberColumnFilterQuery numberFilter;

    private IntegerColumnFilterQuery(Query<${table.businessClassName}> parentQuery, ColumnName column)
    {
      super(parentQuery, column);
      numberFilter = new NumberColumnFilterQuery(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isEqual(int)
     */
    @Override
    public FilterLink isEqual(int value)
    {
      return numberFilter.isEqual(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isEqual(long)
     */
    @Override
    public FilterLink isEqual(long value)
    {
      return numberFilter.isEqual(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isNotEqual(int)
     */
    @Override
    public FilterLink isNotEqual(int value)
    {
      return numberFilter.isNotEqual(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isNotEqual(long)
     */
    @Override
    public FilterLink isNotEqual(long value)
    {
      return numberFilter.isNotEqual(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isGreaterThan(int)
     */
    @Override
    public FilterLink isGreaterThan(int value)
    {
      return numberFilter.isGreaterThan(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isGreaterThan(long)
     */
    @Override
    public FilterLink isGreaterThan(long value)
    {
      return numberFilter.isGreaterThan(value);
    }
    
    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isGreaterOrEqualThan(int)
     */
    @Override
    public FilterLink isGreaterOrEqualThan(int value)
    {
      return numberFilter.isGreaterOrEqualThan(value);
    }
    
    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isGreaterOrEqualThan(long)
     */
    @Override
    public FilterLink isGreaterOrEqualThan(long value)
    {
      return numberFilter.isGreaterOrEqualThan(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isLowerThan(int)
     */
    @Override
    public FilterLink isLowerThan(int value)
    {
      return numberFilter.isLowerThan(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isLowerThan(long)
     */
    @Override
    public FilterLink isLowerThan(long value)
    {
      return numberFilter.isLowerThan(value);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isLowerOrEqualThan(int)
     */
    @Override
    public FilterLink isLowerOrEqualThan(int value)
    {
      return numberFilter.isLowerOrEqualThan(value);
    }
    
    /**
     * @see ${packageName}.${table.queryClassName}.IIntegerColumnFilterQuery#isLowerOrEqualThan(long)
     */
    @Override
    public FilterLink isLowerOrEqualThan(long value)
    {
      return numberFilter.isLowerOrEqualThan(value);
    }
  }
</#if>

<#if table.hasDateColumns>
  /**
   * Provides filter functionality for a date column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IDateColumnFilterQuery extends IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code> or <code>[column] IS NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(Date value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code> or <code>[column] IS NOT NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(Date value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt; [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterThan(Date value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are greater or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &gt;= [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isGreaterOrEqualThan(Date value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt; [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerThan(Date value);

    /**
     * <p>Adds a filter condition that selects rows that have values that are lower or equal than the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;= [value]</code></p>
     * @param value
     * @return query for further composition
     * @throws IllegalArgumentException If the given value is null
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isLowerOrEqualThan(Date value);
  }

  /**
   * Provides filter functionality for a date column of {@link ${table.businessClassName}}
   */
  private static class DateColumnFilterQuery extends ColumnFilterQuery implements IDateColumnFilterQuery
  {
    private DateColumnFilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isEqual(Date)
     */
    @Override
    public FilterLink isEqual(Date value)
    {
      endColumnFilter();
      getQueryBuilder().equal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isNotEqual(Date)
     */
    @Override
    public FilterLink isNotEqual(Date value)
    {
      endColumnFilter();
      getQueryBuilder().unequal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isGreaterThan(Date)
     */
    @Override
    public FilterLink isGreaterThan(Date value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isGreaterOrEqualThan(Date)
     */
    @Override
    public FilterLink isGreaterOrEqualThan(Date value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().greaterOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isLowerThan(Date)
     */
    @Override
    public FilterLink isLowerThan(Date value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerThan(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IDateColumnFilterQuery#isLowerOrEqualThan(Date)
     */
    @Override
    public FilterLink isLowerOrEqualThan(Date value)
    {
      API.checkParameterNotNull(value, "value");
      endColumnFilter();
      getQueryBuilder().lowerOrEqualThan(getColumn(), value);
      return new FilterLink(this);
    }
  }
</#if>

<#if table.hasBooleanColumns>
  /**
   * Provides filter functionality for a boolean column of {@link ${table.businessClassName}}
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IBooleanColumnFilterQuery extends IColumnFilterQuery
  {
    /**
     * <p>Adds a filter condition that selects rows with the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] = [value]</code> or <code>[column] IS NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isEqual(Boolean value);

    /**
     * <p>Adds a filter condition that selects rows that do not have the given <code>value</code>.</p>
     * <p>SQL part: <code>[column] &lt;&gt; [value]</code> or <code>[column] IS NOT NULL</code></p>
     * @param value
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink isNotEqual(Boolean value);
  }

  /**
   * Provides filter functionality for a boolean column of {@link ${table.businessClassName}}
   */
  private static class BooleanColumnFilterQuery extends ColumnFilterQuery implements IBooleanColumnFilterQuery
  {
    private BooleanColumnFilterQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery, column);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IBooleanColumnFilterQuery#isEqual(Boolean)
     */
    @Override
    public FilterLink isEqual(Boolean value)
    {
      endColumnFilter();
      getQueryBuilder().equal(getColumn(), value);
      return new FilterLink(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IBooleanColumnFilterQuery#isNotEqual(Boolean)
     */
    @Override
    public FilterLink isNotEqual(Boolean value)
    {
      endColumnFilter();
      getQueryBuilder().unequal(getColumn(), value);
      return new FilterLink(this);
    }
  }
</#if>

  /**
   * Links a where condition with another.
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IFilterLink 
  {
    /**
     * <p>Adds an AND statement to the where condition. <br/>
     * Must be followed by other query conditions.</p>
     * <p>Note that {@link FilterLink#and() and} operations are evaluated before {@link FilterLink#or() or} operations.
     * E.g. the expression <code>a and b or c</code> is evaluated like <code>(a and b) or c</code>. 
     * If you want to evaluate <code>a and (b or c)</code>, then use {@link FilterLink#and(${table.queryClassName})}</p>
     * <p>SQL part: <code> AND </code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterQuery and();

    /**
     * <p>Adds and AND statement with the given filter <code>subQuery</code>.
     * Only the where clause of the given <code>subQuery</code> is considered. All other parts are ignored. 
     * The whole where clause of the given filter <code>subQuery</code> is and-ed to the query as one term with 
     * brackets around it.</p>  
     * 
     * <p>Note that {@link FilterLink#and() and} operations are always evaluated before {@link FilterLink#or() or} 
     * operations, e.g. the expression <code>A AND B OR C</code> is evaluated to <code>(A AND B) OR C</code>. 
     * If you need to get <code>A AND (B OR C)</code>, then use this method to add a sub query (<code>B OR C</code>) 
     * with a <code>AND</code> operation to the current query (A).</p>
     * <p>Example:
     * <code><pre>${table.queryClassName}.create().name().isEqual("Name").and(
     *  ${table.queryClassName}.create().description().isEqual("Desc").or().description().isEqual("Description")
     * );</pre></code>
     * <p>SQL part: <code> AND([subQueryWhereClause]) </code></p>
     * @param subQuery query with a set of where conditions.
     * @return query for further composition
     * @throws IllegalArgumentException when the query parameter is the same instance or null.
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink and(${table.queryClassName} subQuery);

    /**
     * <p>Adds an OR statement to the where condition. <br/>
     * Must be followed by other query conditions.</p>
     * <p>Note that {@link FilterLink#and() and} operations are evaluated before {@link FilterLink#or() or} operations.
     * E.g. the expression <code>a and b or c</code> is evaluated like <code>(a and b) or c</code></p>
     * <p>SQL part: <code> OR </code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterQuery or();

    /**
     * <p>Adds and OR statement with the given filter <code>subQuery</code>.<br>
     * Only the where close of the given <code>subQuery</code> is considered. All other parts are ignored.
     * The whole where clause of the given filter <code>subQuery</code> is or-ed to the query as one term with 
     * brackets around it.</p>      
     * <p>Note that {@link FilterLink#and() and} operations are always evaluated before {@link FilterLink#or() or} 
     * operations, e.g. the expression <code>A AND B OR C</code> is evaluated to <code>(A AND B) OR C</code>. 
     * If you need to get <code>A AND (B OR C)</code>, then use this method to add a sub query (<code>B OR C</code>) 
     * with a <code>AND</code> operation to the current query (A).</p>
     * <p>SQL part: <code> OR([subQueryWhereClause]) </code></p>
     * @param subQuery query with a set of where conditions.
     * @return query for further composition
     * @throws IllegalArgumentException when the query parameter is the same instance or null.
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink or(${table.queryClassName} subQuery);
  }
  
  /**
   * Links a where condition with another.
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static class FilterLink extends ${table.queryClassName} implements IFilterLink
  {
    FilterLink(Query<${table.businessClassName}> parentQuery)
    {
      super(parentQuery);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IFilterLink#and()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterQuery and()
    {
      getQueryBuilder().and();
      startAndOrOperation("and()");
      return new FilterQuery(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IFilterLink#and(${table.queryClassName})
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink and(${table.queryClassName} subQuery)
    {
      getQueryBuilder().and(getFilterForSubExpression(subQuery));
      return this;
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IFilterLink#or()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterQuery or()
    {
      getQueryBuilder().or();
      startAndOrOperation("or()");
      return new FilterQuery(this);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IFilterLink#or(${table.queryClassName})
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public FilterLink or(${table.queryClassName} subQuery)
    {
      getQueryBuilder().or(getFilterForSubExpression(subQuery));
      return this;
    }
  }

  /**
   * Provides methods to group the result by certain fields.
<#include table.queryClassName +"_GroupBy_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IGroupByQueryColumns
  {
<#list columns as column>
  <#if column.isGroupAndOrderBySupported()>
    /**
     * <p>Groups the result of the query by the field <code>${column.name}</code>.</p>
     * <p>SQL part: <code>GROUP BY ${column.name}</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public GroupByQuery ${StringUtils.uncapitalize(column.name)}();

  </#if>
</#list>
  }

  /**
   * Provides methods to group the result by certain fields.
<#include table.queryClassName +"_GroupBy_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static class GroupByQuery extends ${table.queryClassName} implements IGroupByQueryColumns
  {
    GroupByQuery(Query<${table.businessClassName}> parentQuery)
    {
      super(parentQuery);
    }

<#list columns as column>
  <#if column.isGroupAndOrderBySupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IGroupByQueryColumns#${StringUtils.uncapitalize(column.name)}()
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    @Override
    public GroupByQuery ${StringUtils.uncapitalize(column.name)}()
    {
      getQueryBuilder().groupBy(${column.javaColumnConstantName}, "${column.name}");
      return this;
    }

  </#if>
</#list>
  }

  /**
   * Provides methods to order the result by columns of {@link ${table.businessClassName}}.
<#include table.queryClassName +"_OrderBy_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public interface IOrderByQueryColumns
  {
<#list columns as column>
  <#if column.isGroupAndOrderBySupported()>
    /**
     * <p>Adds an order by statement for the column <code>${column.name}</code> in the default direction ascending.<br>
     * To change the order to descending use <code>${StringUtils.uncapitalize(column.name)}().descending()</code></p>
     * <p><b>Example:</b><br>
     * <code><pre>TaskQuery.create().orderBy().${StringUtils.uncapitalize(column.name)}()</pre></code>
     * </p>
     * <p>SQL part: <code>ORDER BY ${column.name} ASC</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     * @see OrderByColumnQuery#descending()
     * @see OrderByColumnQuery#ascending()
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public OrderByColumnQuery ${StringUtils.uncapitalize(column.name)}();

  </#if>
</#list>
  }

  /**
   * Provides methods to order the result by columns of {@link ${table.businessClassName}}.
<#include table.queryClassName +"_OrderBy_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static class OrderByQuery extends ${table.queryClassName} implements IOrderByQueryColumns
  {
    OrderByQuery(Query<${table.businessClassName}> parentQuery)
    {
      super(parentQuery);
    }

<#list columns as column>
  <#if column.isGroupAndOrderBySupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IOrderByQueryColumns#${StringUtils.uncapitalize(column.name)}()
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    @Override
    public OrderByColumnQuery ${StringUtils.uncapitalize(column.name)}()
    {
      return new OrderByColumnQuery(this, ${column.javaColumnConstantName});
    }

  </#if>
</#list>
  }

  /**
   * Provides methods to define the direction of the sorting either ascending or descending.
   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
  public static class OrderByColumnQuery extends OrderByQuery
  {
    private ColumnName column;

    private OrderByColumnQuery(Query<${table.businessClassName}> parentQuery,  ColumnName column)
    {
      super(parentQuery);
      this.column = column;
      getQueryBuilder().orderByAsc(column);
    }

    /**
     * <p>Sorts the column in ascending direction.</p>
     * <p><b>Example:</b><br>
     * <code><pre>${table.queryClassName}.create().orderBy().name().ascending()</pre></code>
     * </p>
     * <p>SQL part: <code>ORDER BY [column] ASC</code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public OrderByQuery ascending()
    {
      getQueryBuilder().removeLastOrderBy().orderByAsc(column);
      return new OrderByQuery(this);
    }

    /**
     * <p>Sorts the column in descending direction.</p>
     * <p><b>Example:</b><br>
     * <code><pre>${table.queryClassName}.create().orderBy().name().descending()</pre></code>
     * </p>
     * <p>SQL part: <code>ORDER BY [column] DESC</code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="17.1.2012", reviewers="bb,fs,rwei")
    public OrderByQuery descending()
    {
      getQueryBuilder().removeLastOrderBy().orderByDesc(column);
      return new OrderByQuery(this);
    }
  }
  
  /**
   * Provides methods to perform aggregations on the query. For each aggregation a column is added to the result set.
<#include table.queryClassName +"_Aggregate_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
  public interface IAggregationQuery
  {

    /**
     * <p>Adds a result column <code>Count</code> to the query, that contains the number of (grouped) rows.</p>
     * <p>SQL part: <code>COUNT(*) AS Count</code></p>
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery countRows();

<#list columns as column>
  <#if column.isSumSupported()>
    /**
     * <p>Adds a result column <code>Sum${column.name}</code> to the query, that contains the sum of the field <code>${column.name}</code> of all (grouped) rows.</p>
     * <p>SQL part: <code>SUM(${column.name}) AS Sum${column.name}</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery sum${column.name}();

  </#if>
  <#if column.isAvgSupported()>
    /**
     * <p>Adds a result column <code>Avg${column.name}</code> to the query, that contains the average of the field <code>${column.name}</code> of all (grouped) rows.</p>
     * <p>SQL part: <code>AVG(${column.name}) AS Avg${column.name}</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery avg${column.name}();

  </#if>
  <#if column.isMinSupported()>
    /**
     * <p>Adds a result column <code>Min${column.name}</code> to the query, that contains the minimum value of the field <code>${column.name}</code> of all (grouped) rows.</p>
     * <p>SQL part: <code>MIN(${column.name}) AS Min${column.name}</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery min${column.name}();

  </#if>
  <#if column.isMaxSupported()>
    /**
     * <p>Adds a result column <code>Max${column.name}</code> to the query, that contains the maximum value of the field <code>${column.name}</code> of all (grouped) rows.</p>
     * <p>SQL part: <code>MAX(${column.name}) AS Max${column.name}</code></p>
   <#if column.additionalComments??>  
     * ${column.additionalComments}
   </#if>     
     * @return query for further composition
     */
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery max${column.name}();

  </#if>
</#list>
  }

  /**
   * This class provides methods to perform aggregations on the query. For each aggregation a column is added to the result set.
<#include table.queryClassName +"_Aggregate_JavaDoc.ftl">

   */
  @PublicAPI(IvyScriptVisibility.EXPERT)
  @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
  public static class AggregationQuery extends ${table.queryClassName} implements IAggregationQuery
  {
    /**
     * Constructor
     * @param parentQuery
     */
    AggregationQuery(Query<${table.businessClassName}> parentQuery)
    {
      super(parentQuery);
    }

    /**
     * @see ${packageName}.${table.queryClassName}.IAggregationQuery#countRows()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery countRows()
    {
      getQueryBuilder().addCount("Count");
      return this;
    }

<#list columns as column>
  <#if column.isSumSupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IAggregationQuery#sum${column.name}()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery sum${column.name}()
    {
      getQueryBuilder().addSum(${column.javaColumnConstantName}, "Sum${column.name}");
      return this;
    }

  </#if>
  <#if column.isAvgSupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IAggregationQuery#avg${column.name}()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery avg${column.name}()
    {
      getQueryBuilder().addAvg(${column.javaColumnConstantName}, "Avg${column.name}");
      return this;
    }

  </#if>
  <#if column.isMinSupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IAggregationQuery#min${column.name}()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery min${column.name}()
    {
      getQueryBuilder().addMin(${column.javaColumnConstantName}, "Min${column.name}");
      return this;
    }

  </#if>
  <#if column.isMaxSupported()>
    /**
     * @see ${packageName}.${table.queryClassName}.IAggregationQuery#max${column.name}()
     */
    @Override
    @PublicAPI(IvyScriptVisibility.EXPERT)
    @Reviewed(date="16.01.2012", reviewers="mda,bb,fs")
    public AggregationQuery max${column.name}()
    {
      getQueryBuilder().addMax(${column.javaColumnConstantName}, "Max${column.name}");
      return this;
    }

  </#if>
</#list>
  }
  
  @Override
  public String toString()
  {
    String sql = super.toString();
    return StringUtils.replace(sql, "FROM ? ", "FROM ${table.name} ");
  }
  
}
