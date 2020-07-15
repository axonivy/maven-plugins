package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * A Meta data generator that creates entity java classes
 * @author rwei
 * @since 16.10.2009
 */
public class JavaEntityClassGenerator extends JavaClassGenerator
{
  /** The cache hint */
  public static final String CACHE = String.valueOf("Cache");
  /** The strategy for the cache */
  public static final String STRATEGY = "Strategy";
  /** Cache count limit hint */
  public static final String COUNT_LIMIT = "countLimit";
  /** Cache usage limit hint */
  public static final String USAGE_LIMIT = "usageLimit";

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    SqlTable table;
    for (String tableName : getTablesToGenerateJavaClassFor())
    {
      table = metaDefinition.findTable(tableName);
      if (table == null)
      {
        throw new MetaException("Could not find table "+tableName);
      }
      writeJavaEntityClass(table, metaDefinition);
    }
  }

  /**
   * Writes the java entry class for a table
   * @param table the table to generate the java entity class for
   * @param metaDefinition meta definition 
   * @throws FileNotFoundException 
   * @throws MetaException 
   */
  private void writeJavaEntityClass(SqlTable table, SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    String className;
    File javaSourceFile;
    PrintWriter pr;
    
    className = getEntityClassName(table);

    javaSourceFile = new File(getTargetDirectory(), className+".java");
    javaSourceFile.getParentFile().mkdirs();
    pr = new NewLinePrintWriter(javaSourceFile);
    try
    {
      writePackage(pr);
      writeImports(pr);
      writeHeader(pr, table);
      writeCacheAnnotation(pr, table);
      writeCacheTriggerAnnotation(pr, metaDefinition, table);
      writeClass(pr, className, table);
      writeAssociations(pr, table, metaDefinition);
      writeAttributes(pr, metaDefinition, table);
      writeConstructor(pr, className, table);
      writeGetterSetter(pr, className, table);
      writeEquals(pr, className, table);
      writeHashCode(pr, table);
      writeReferenceCheckerClasses(pr, table);
      pr.println("}");
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
  }

  /**
   * @param pr
   * @param table
   */
  private void writeReferenceCheckerClasses(PrintWriter pr, SqlTable table)
  {
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table))
    {
      writeReferenceCheckerClass(pr, table, column);
    }
  }

  /**
   * @param pr
   * @param table
   * @param column
   */
  private void writeReferenceCheckerClass(PrintWriter pr, SqlTable table, SqlTableColumn column)
  {
    if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).isHintSet(STRATEGY) &&
        column.getReference() != null && 
        (column.getReference().getForeignKeyAction() == SqlForeignKeyAction.ON_DELETE_CASCADE ||
         column.getReference().getForeignKeyAction() == SqlForeignKeyAction.ON_DELETE_SET_NULL))
    {
      writeIndent(pr, 2);
      pr.println("/**");
      writeIndent(pr, 2);
      pr.print(" * Checks references for the foreign key ");
      pr.println(column.getId());
      writeIndent(pr, 2);
      pr.println(" */");
      writeIndent(pr, 2);
      pr.print("private static class ");
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.print("ReferenceChecker implements ch.ivyteam.ivy.persistence.cache.trigger.IReferenceChecker<");
      pr.print(getEntityClassName(table));
      pr.println(">");
      writeIndent(pr, 2);
      pr.println("{");
      writeIndent(pr, 4);
      pr.println("/**");
      writeIndent(pr, 4);
      pr.println(" * @see ch.ivyteam.ivy.persistence.cache.trigger.IReferenceChecker#isReferencedObject(ch.ivyteam.ivy.persistence.IPersistentObject, java.lang.Object)");
      writeIndent(pr, 4);
      pr.println(" */");
      writeIndent(pr, 4);
      pr.println("@Override");
      writeIndent(pr, 4);
      pr.print("public boolean isReferencedObject(");
      pr.print(getEntityClassName(table));
      pr.println(" object, Object keyOfForeignEntity)");
      writeIndent(pr, 4);
      pr.println("{");
      writeIndent(pr, 6);
      pr.print("return keyOfForeignEntity.equals(object.f");
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.println(");");
      writeIndent(pr, 4);
      pr.println("}");
      writeIndent(pr, 2);
      pr.println("}");
    }
 
  }

  /**
   * @param pr
   * @param metaDefinition
   * @param table
   */
  private void writeCacheTriggerAnnotation(PrintWriter pr, SqlMeta metaDefinition, SqlTable table)
  {
    if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).isHintSet(STRATEGY) &&
        isChildPersistentObject(table))    
    {
      pr.print("@ch.ivyteam.ivy.persistence.cache.trigger.OnDeleteParentInvalidateThisCache(\"");
      pr.print(getEntityClassName(metaDefinition.findTable(JavaClassGeneratorUtil.getParentKeyColumn(table).getReference().getForeignTable())));
      pr.println("\")");
    }
  }

  /**
   * Writes a cache annotation to the entity class if the table has a CACHE hint
   * @param pr
   * @param table
   */
  private void writeCacheAnnotation(PrintWriter pr, SqlTable table)
  {
    String strategy;
    boolean first;
    if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).isHintSet(STRATEGY))
    {
      pr.print("@ch.ivyteam.ivy.persistence.cache.Cache(");
      strategy = table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintValue(STRATEGY); 
      if ("ALL".equalsIgnoreCase(strategy))
      {
        strategy = "ch.ivyteam.ivy.persistence.cache.advisor.CacheAllAdvisor";
      }
      else if ("NOTHING".equalsIgnoreCase(strategy))
      {
        strategy = "ch.ivyteam.ivy.persistence.cache.advisor.CacheNothingAdvisor";
      }
      else if ("ALL_REMOVE_UNUSED".equalsIgnoreCase(strategy))
      {
        strategy = "ch.ivyteam.ivy.persistence.cache.advisor.CacheAllRemoveUnusedAdvisor";
      }
      if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintNames().size()>1)
      {
        pr.print("value=");
      }
      pr.print(strategy);
      pr.print(".class");
      
      if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintNames().size()>1)
      {
        pr.print(", ");
        pr.println();
        writeIndent(pr, 2);
        pr.print("argNames={");
        first = true;
        for (String name: table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintNames())
        {
          if (!name.equals(STRATEGY))
          {
            if (!first)
            {
              pr.print(", ");
            }
            first = false;
            pr.print('"');
            pr.print(name);
            pr.print('"');
          }         
        }
        pr.print("},");
        pr.println();
        writeIndent(pr, 2);
        pr.print("argValues={");
        first = true;
        for (String name: table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintNames())
        {
          if (!name.equals(STRATEGY))
          {
            if (!first)
            {
              pr.print(", ");
            }
            first = false;
            pr.print('"');
            pr.print(table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).getHintValue(name));
            pr.print('"');
          }         
        }
        pr.print('}');
      }
      pr.println(")");
    }
  }

  /**
   * Writes the {@link #hashCode()} method
   * @param pr
   * @param table
   */
  private void writeHashCode(PrintWriter pr, SqlTable table)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * @see Object#hashCode()");
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.println("@Override");
    writeIndent(pr, 2);
    pr.println("public int hashCode()");
    writeIndent(pr, 2);
    pr.println("{");    
    writeIndent(pr, 4);    
    pr.print("return new SqlHashCodeBuilder().appendSuper(super.hashCode())");
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table))
    {
      pr.println();
      writeIndent(pr, 6);
      pr.print(".append(f");
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.print(')');
    }
    pr.println(".toHashCode();");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println(); 
  }

  /**
   * Writes the {@link #equals(Object)} method
   * @param pr
   * @param className 
   * @param table
   */
  private void writeEquals(PrintWriter pr, String className, SqlTable table)
  {
    writeIndent(pr, 2);
    pr.println("/**");

    writeIndent(pr, 2);
    pr.print(" * @see Object#equals(Object)");
    
    writeIndent(pr, 2);
    pr.println(" */");
    
    writeIndent(pr, 2);
    pr.println("@Override");
    
    writeIndent(pr, 2);
    pr.println("public boolean equals(Object obj)");
    
    writeIndent(pr, 2);
    pr.println("{");    
    
    writeIndent(pr, 4);    
    pr.println("if (this == obj)");
    
    writeIndent(pr, 4);    
    pr.println("{");
    
    writeIndent(pr, 6);    
    pr.println("return true;");
    
    writeIndent(pr, 4);    
    pr.println("}");
    
    writeIndent(pr, 4);    
    pr.print("if (obj instanceof ");
    pr.print(className);
    pr.println(")");    
    
    writeIndent(pr, 4);    
    pr.println("{");
    
    if (!JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table).isEmpty())
    {
      writeIndent(pr, 6);
      pr.print(className);
      pr.print(" other = (");
      pr.print(className);
      pr.println(")obj;");
    }
    
    writeIndent(pr, 6);
    pr.print("return new SqlEqualsBuilder().appendSuper(super.equals(obj))");
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table))
    {
      pr.println();
    
      writeIndent(pr, 8);
      pr.print(".append(f");
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.print(", other.f");
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.print(')');
    }
    pr.println(".isEquals();");
    
    writeIndent(pr, 4);    
    pr.println("}");
    
    writeIndent(pr, 4);    
    pr.println("return false;");
    
    writeIndent(pr, 2);
    pr.println('}');
    pr.println(); 
  }

  /**
   * Write associations
   * @param pr
   * @param table
   * @param metaDefinition
   * @throws MetaException 
   */
  private void writeAssociations(PrintWriter pr, SqlTable table, SqlMeta metaDefinition) throws MetaException
  {
    for (SqlTable associationTable : getAssociationTables(table, metaDefinition))
    {
      writeAssociation(pr, table, metaDefinition, associationTable);
    }
  }

  private void writeAssociation(PrintWriter pr, SqlTable table, SqlMeta metaDefinition,
          SqlTable associationTable) throws MetaException
  {
    SqlTable foreignTable = null;
    String foreignColumn = null;
    String tableColumn = null;
    for (SqlTableColumn column : associationTable.getColumns())
    {
      if (column.getReference().getForeignTable().equals(table.getId())
              && tableColumn == null) // needed if foreignTable is the same as the table
      {
        tableColumn = column.getId();
      }
      else
      {
        foreignTable = metaDefinition.findTable(column.getReference().getForeignTable());
        foreignColumn = column.getId();
      }  
    }
      
    writeAssociation(pr, table, foreignTable, associationTable, tableColumn, foreignColumn);
    if(isSelfAssociation(associationTable))
    {
      writeReverseSelfAssociation(pr, table, associationTable);
    }
  }

  private void writeAssociation(PrintWriter pr, SqlTable table, SqlTable foreignTable, SqlTable associationTable, String tableColumn,
          String foreignColumn) throws MetaException
  {
    writeAssociation(pr, table, foreignTable, associationTable, "", tableColumn, foreignColumn);
  }

  private void writeReverseSelfAssociation(PrintWriter pr, SqlTable table,SqlTable associationTable)
  {
    List<SqlTableColumn> columns = associationTable.getColumns();
    SqlTableColumn column1 = columns.get(0);
    SqlTableColumn column2 = columns.get(1);

    SqlTable foreignTable = table;
    String foreignColumn = column1.getId();
    String tableColumn = column2.getId();
    
    writeAssociation(pr, table, foreignTable, associationTable, "_REVERSE", tableColumn, foreignColumn);
  }

  private boolean isSelfAssociation(SqlTable associationTable)
  {
    List<SqlTableColumn> columns = associationTable.getColumns();
    if (columns.size() != 2)
    {
      return false;
    }
    String foreignTable0 = columns.get(0).getReference().getForeignTable();
    String foreignTable1 = columns.get(1).getReference().getForeignTable();
    return foreignTable0.equals(foreignTable1);
  }
  
  private void writeAssociation(PrintWriter pr, SqlTable table, SqlTable foreignTable, SqlTable associationTable, String associationPostfix, String tableColumn,
          String foreignColumn) throws MetaException
  {
    String associationTableName;
    String primaryKey = JavaClassGeneratorUtil.getPrimaryKeyColumn(table).getId();
    
    associationTableName = JavaClassGeneratorUtil.removeTablePrefix(associationTable.getId());
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Use this association to navigate between a entity of this table (");
    pr.print(table.getId());
    pr.println(')');
    writeIndent(pr, 2);
    pr.print(" * and entities of table ");
    pr.print(foreignTable.getId());
    pr.print(" using association table ");
    pr.println(associationTable.getId());
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("@ch.ivyteam.ivy.persistence.cache.trigger.OnAddOrRemoveRelatedInvalidateAssociationCache(\"");
    pr.print(getEntityClassName(foreignTable));
    pr.println("\")");
    writeIndent(pr, 2);
    pr.print("public static final ch.ivyteam.ivy.persistence.Association ");
    pr.print(associationTableName.toUpperCase());
    pr.print(associationPostfix);
    pr.print(" = new ch.ivyteam.ivy.persistence.Association(\"");
    pr.print(associationTable.getId());
    pr.print("\", \"");
    pr.print(primaryKey);
    pr.print("\", \"");
    pr.print(foreignColumn);
    pr.print("\"");
    if (!primaryKey.equals(tableColumn))
    {
      pr.print(", \"");
      pr.print(tableColumn);
      pr.print("\"");
    }
    pr.println(");");
    pr.println();
  }

  /**
   * Writes the getter and setter methods
   * @param pr
   * @param className 
   * @param table
   * @throws MetaException 
   */
  private void writeGetterSetter(PrintWriter pr, String className, SqlTable table) throws MetaException  
  {
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table))
    {
      writeGetter(pr, column);
      writeSetter(pr, className, column);
    }
    for (SqlTableColumn column : JavaClassGeneratorUtil.getPrimaryKeyColumns(table))
    {
      writePrimaryGetter(pr, column);
    }
    for (SqlTableColumn column : JavaClassGeneratorUtil.getParentKeyColumns(table))
    {
      writeParentGetter(pr, column);
      if (column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(PARENT_CAN_BE_MODIFIED))
      {
        writeParentSetter(pr, className, column);
      }
    }
    if (table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(ADDITIONAL_SET_METHODS))
    {
      writeAdditionalSetMethods(pr, className, table, table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(ADDITIONAL_SET_METHODS));
    }
    if (table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(SECONDARY_KEYS))
    {
      writeGetSecondaryKeys(pr, table, table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(SECONDARY_KEYS));
    }
  }

  /**
   * Writes the secondary keys get method
   * @param pr
   * @param table
   * @param secondaryKeys
   * @throws MetaException 
   */
  private void writeGetSecondaryKeys(PrintWriter pr, SqlTable table, String secondaryKeys) throws MetaException
  {
    boolean first = true;
    SqlTableColumn secondaryKeyColumn;
    List<SqlTableColumn> secondaryKeyColumns = new ArrayList<SqlTableColumn>();
    for (String column: secondaryKeys.split(","))
    {
      secondaryKeyColumn = table.findColumn(column.trim());
      if (secondaryKeyColumn == null)
      {
        throw new MetaException("Secondary key column "+column.trim()+" not found in table "+table.getId());
      }
      secondaryKeyColumns.add(secondaryKeyColumn);
    }
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Gets the values of the columns ");
    pr.print(secondaryKeys);
    pr.println(" as secondary keys"); 
    writeIndent(pr, 2);
    pr.println(" * @return object array with the secondary keys");
    writeIndent(pr, 2);
    pr.println(" * @see ch.ivyteam.ivy.persistence.IPersistentObject#getSecondaryKeys()");
    for (SqlTableColumn column: secondaryKeyColumns)
    {
      writeIndent(pr, 2);
      pr.print(" * @see #f");
      pr.println(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    }
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.println("@Override");
    writeIndent(pr, 2);
    pr.println("public Object[] getSecondaryKeys()");
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print("return new Object[]{");
    for (SqlTableColumn column: secondaryKeyColumns)
    {
      if (!first)
      {
        pr.print(", ");       
      }
      first = false;
      pr.print('f');
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    }
    pr.println("};");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();
  }

  /**
   * Writes a parent setter
   * @param pr
   * @param className
   * @param column
   */
  private void writeParentSetter(PrintWriter pr, String className, SqlTableColumn column)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Sets the value of the column ");
    pr.print(column.getId());
    pr.println(" (parent key)");
    writeIndent(pr, 2);
    pr.print(" * @param ");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.print(" new value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.println(" * @return new entity object with the new value set on the column");
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    pr.print(className);
    pr.print(" set");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.print('(');
    writeDataType(pr, column);
    pr.print(" ");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println(')');
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print(className);
    pr.println(" data;");
    writeIndent(pr, 4);
    pr.print("data = (");
    pr.print(className);
    pr.println(")clone();");
    writeIndent(pr, 4);
    pr.print("data.modifyParentKey(");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println(");");
    writeIndent(pr, 4);
    pr.println("return data;");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();
  }

  /**
   * @param pr
   * @param column
   */
  private void writeParentGetter(PrintWriter pr, SqlTableColumn column)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Gets the value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.print(" * @return value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    writeDataType(pr, column);
    pr.print(" get");
    pr.print(StringUtils.capitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println("()");
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print("return ");
    if (!"Object".equals(JavaClassGeneratorUtil.getJavaDataType(column)))
    {
      pr.print("(");
      writeNoNativeDataType(pr, column);
      pr.print(")");
    }
    pr.println("getParentKey();");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();
  }

  /**
   * Writes additional set methods
   * @param pr print writer
   * @param className the class name to use
   * @param table the table
   * @param setMethods list of set methods
   * @throws MetaException 
   */
  private void writeAdditionalSetMethods(PrintWriter pr, String className, SqlTable table, String setMethods) throws MetaException
  {
    List<SqlTableColumn> columns = new ArrayList<SqlTableColumn>();
    SqlTableColumn col;
    String columnNames;
    int pos;
    boolean first = true;
    
    for (String method : setMethods.split("\\)"))
    {
      pos = method.indexOf('(');
      if (pos < 0)
      {
        throw new MetaException("Syntax error in additonal set methods of table "+table.getId());
      }
      columnNames = method.substring(pos+1);
      method = method.substring(0, pos).trim();
      columns.clear();
      for (String columnName : columnNames.split(","))
      {
        col = table.findColumn(columnName.trim());
        if (col == null)
        {
          throw new MetaException("Column "+columnName +" not found in table "+table.getId());
        }
        columns.add(col);
      }
      writeIndent(pr, 2);
      pr.println("/**");
      writeIndent(pr, 2);
      pr.print(" * Sets the values of the columns ");
      for (SqlTableColumn column : columns)
      {
        if (!first)
        {
          pr.print(", ");
        }
        first = false;
        pr.print(column.getId());
      }
      pr.println();
      for (SqlTableColumn column: columns)
      {
        writeIndent(pr, 2);
        pr.print(" * @param ");
        pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
        pr.print(" new value of column ");
        pr.println(column.getId());
      }
      writeIndent(pr, 2);
      pr.println(" * @return new entity object with the new value set on the column");
      for (SqlTableColumn column: columns)
      {
        writeIndent(pr, 2);
        pr.print(" * @see #f");
        pr.println(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      }
      writeIndent(pr, 2);
      pr.println(" */");
      writeIndent(pr, 2);
      pr.print("public ");
      pr.print(className);
      pr.print(" ");
      pr.print(method);
      pr.print('(');
      first=true;
      for (SqlTableColumn column : columns)
      {
        if (!first)
        {
          pr.print(", ");
        }
        first = false;
        writeDataType(pr, column);
        pr.print(" ");
        pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
      }
      pr.println(')');
      writeIndent(pr, 2);
      pr.println("{");
      writeIndent(pr, 4);
      pr.print(className);
      pr.println(" data;");
      writeIndent(pr, 4);
      pr.print("data = (");
      pr.print(className);
      pr.println(")clone();");
      for (SqlTableColumn column : columns)
      {
        writeIndent(pr, 4);
        pr.print("data.f");
        pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
        pr.print(" = ");
        pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
        pr.println(";");
      }
      writeIndent(pr, 4);
      pr.println("return data;");
      writeIndent(pr, 2);
      pr.println('}');
      pr.println();
    }
  }

  /**
   * Writes the setter for the given column
   * @param pr
   * @param className
   * @param column
   */
  private void writeSetter(PrintWriter pr, String className, SqlTableColumn column)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Sets the value of the column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.print(" * @param ");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.print(" new value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.println(" * @return new entity object with the new value set on the column");
    writeIndent(pr, 2);
    pr.print(" * @see #f");
    pr.println(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    pr.print(className);
    pr.print(" set");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.print('(');
    writeDataType(pr, column);
    pr.print(" ");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println(')');
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print(className);
    pr.println(" data;");
    writeIndent(pr, 4);
    pr.print("data = (");
    pr.print(className);
    pr.println(")clone();");
    writeIndent(pr, 4);
    pr.print("data.f");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.print(" = ");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println(";");
    writeIndent(pr, 4);
    pr.println("return data;");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();
  }

  /**
   * Writes a getter for the given column
   * @param pr
   * @param column
   */
  private void writeGetter(PrintWriter pr, SqlTableColumn column)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Gets the value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.print(" * @return value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.print(" * @see #f");
    pr.println(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    writeDataType(pr, column);
    pr.print(" get");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.println("()");
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print("return f");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.println(";");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();
  }
  
  /**
   * The primary key getter
   * @param pr
   * @param column
   */
  private void writePrimaryGetter(PrintWriter pr, SqlTableColumn column)
  {
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" * Gets the value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.print(" * @return value of column ");
    pr.println(column.getId());
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    writeDataType(pr, column);
    pr.print(" get");
    pr.print(StringUtils.capitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    pr.println("()");
    writeIndent(pr, 2);
    pr.println("{");
    writeIndent(pr, 4);
    pr.print("return ");
    if (!"Object".equals(JavaClassGeneratorUtil.getJavaDataType(column)))
    {
      pr.print("(");
      writeNoNativeDataType(pr, column);
      pr.print(")");
    }
    pr.print("getKey()");
    pr.println(";");
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();  }

  /**
   * Writes the constructor
   * @param pr
   * @param className 
   * @param table
   */
  private void writeConstructor(PrintWriter pr, String className, SqlTable table)
  {
    boolean first = true;
    List<SqlTableColumn> columns = new ArrayList<SqlTableColumn>();
    
    columns.addAll(JavaClassGeneratorUtil.getPrimaryKeyColumns(table));
    columns.addAll(JavaClassGeneratorUtil.getParentKeyColumns(table));
    columns.addAll(JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table));
    writeIndent(pr, 2);
    pr.println("/**");
    writeIndent(pr, 2);
    pr.print(" *");    
    pr.println(" Constructor");
    for (SqlTableColumn column : columns)
    {
      writeIndent(pr, 2);
      pr.print(" *");
      pr.print(" @param ");
      pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
      pr.print(' ');
      pr.println(column.getComment());
    }
    writeIndent(pr, 2);
    pr.println(" */");
    writeIndent(pr, 2);
    pr.print("public ");
    pr.print(className);
    pr.print("(");
    for (SqlTableColumn column : columns)
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      if (table.isPrimaryKeyColumn(column))
      {
        writeNoNativeDataType(pr, column);        
      }
      else
      {
        writeDataType(pr, column);
      }
      pr.print(" ");      
      pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    }
    pr.println(')');
    writeIndent(pr, 2);
    pr.println('{');
    writeIndent(pr, 4);
    pr.print("super(");
    pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(table.findColumn(table.getPrimaryKey().getPrimaryKeyColumns().get(0)))));
    if (isChildPersistentObject(table))
    {
      pr.print(", ");
      pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(table.findColumn(JavaClassGeneratorUtil.getParentKey(table)))));      
    }
    pr.println(");");
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyAndLobColumns(table))
    {
      writeIndent(pr, 4);
      pr.print('f');
      pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
      pr.print(" = ");
      pr.print(" ");      
      pr.print(StringUtils.uncapitalize(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
      pr.println(';');
    }
    writeIndent(pr, 2);
    pr.println('}');
    pr.println();    
  }

  /**
   * Writes the import
   * @param pr
   */
  private void writeImports(PrintWriter pr)
  {
    pr.println("import ch.ivyteam.db.jdbc.SqlEqualsBuilder;");
    pr.println("import ch.ivyteam.db.jdbc.SqlHashCodeBuilder;");

    pr.println();
  }

  /**
   * Writes the attributes
   * @param pr
   * @param metaDefinition 
   * @param table
   * @throws MetaException 
   */
  private void writeAttributes(PrintWriter pr, SqlMeta metaDefinition, SqlTable table) throws MetaException
  {
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyColumns(table))
    {
      writeAttribute(pr, metaDefinition, table, column);
    }
  }

  /**
   * Writes an attribute
   * @param pr
   * @param metaDefinition 
   * @param table
   * @param column
   * @throws MetaException 
   */
  private void writeAttribute(PrintWriter pr, SqlMeta metaDefinition, SqlTable table, SqlTableColumn column) throws MetaException
  {
    writeAttributeComment(pr, table, column);
    if (JavaClassGeneratorUtil.isLobColumn(column))
    {
      writeIndent(pr, 2);
      pr.println("@SuppressWarnings(\"unused\")");
    }
    writeCacheTriggerAnnotation(pr, metaDefinition, table, column);
    writeIndent(pr, 2);
    pr.print("private ");
    writeDataType(pr, column);
    pr.print(" f");
    pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
    pr.println(';');
    pr.println();    
  }

  /**
   * @param pr
   * @param metaDefinition 
   * @param column
   * @param table 
   */
  private void writeCacheTriggerAnnotation(PrintWriter pr, SqlMeta metaDefinition, SqlTable table, SqlTableColumn column)
  {
    if (table.getDatabaseManagementSystemHints(JavaEntityClassGenerator.CACHE).isHintSet(STRATEGY) &&
        column.getReference() != null && 
        column.getReference().getForeignKeyAction() != null)
    {
      String referencedClass = getEntityClassName(metaDefinition.findTable(column.getReference().getForeignTable()));
      writeIndent(pr, 2);
      pr.print("@ch.ivyteam.ivy.persistence.cache.trigger.");
      switch (column.getReference().getForeignKeyAction())
      {
        case ON_DELETE_CASCADE:
          pr.print("OnDeleteForeignInvalidateThisCache(value=\"");
          pr.print(referencedClass);
          pr.print("\", referenceChecker=");
          pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
          pr.println("ReferenceChecker.class)");
          break;
        case ON_DELETE_SET_NULL:
          pr.print("OnDeleteForeignInvalidateThisCache(value=\"");
          pr.print(referencedClass);
          pr.print("\", isCascading=false, referenceChecker=");
          pr.print(JavaClassGeneratorUtil.generateJavaIdentifier(column));
          pr.println("ReferenceChecker.class)");
          break;
        case ON_DELETE_THIS_CASCADE:
          pr.print("OnDeleteThisInvalidateForeignCache(\"");
          pr.print(referencedClass);
          pr.println("\")");
          break;
      }
    }  
  }

  /**
   * Writes the comment of an attribute
   * @param pr
   * @param table 
   * @param column
   * @throws MetaException 
   */
  private void writeAttributeComment(PrintWriter pr, SqlTable table, SqlTableColumn column) throws MetaException
  {
    writeIndent(pr, 2);    
    pr.println("/**");
    if (column.getComment().trim().length()>0)
    {
      writeJavaDocComment(pr, 2, column.getComment());
      writeIndent(pr, 2);
      pr.println(" * <br>");
    }
    writeIndent(pr, 2);
    pr.print(" * <b>Column:</b> ");
    writeColumnInformation(pr, table, column);
    pr.println();
    writeIndent(pr, 2);
    pr.println(" */");
    
  }

  /**
   * Writes the java type for the given column
   * @param pr
   * @param column
   */
  private void writeDataType(PrintWriter pr, SqlTableColumn column)
  {
    pr.print(JavaClassGeneratorUtil.getJavaDataType(column));
  }
  
  /**
   * Writes the a non native java type for the given column
   * @param pr
   * @param column
   */
  private void writeNoNativeDataType(PrintWriter pr, SqlTableColumn column)
  {
    pr.print(JavaClassGeneratorUtil.getJavaDataType(column, true));
  }

  /**
   * Writes the class
   * @param pr
   * @param className
   * @param table
   */
  private void writeClass(PrintWriter pr, String className, SqlTable table)
  {
    pr.print("public class ");
    pr.print(className);
    pr.print(" extends ");
    if (isChildPersistentObject(table))
    {
      pr.println("ch.ivyteam.ivy.persistence.base.PersistentChildObject");    
    }
    else
    {
      pr.println("ch.ivyteam.ivy.persistence.base.PersistentObject");
    }
    pr.println('{');    
  }

  /**
   * Is this entity class a child persistent object class
   * @param table
   * @return true if it is, otherwise false
   */
  private boolean isChildPersistentObject(SqlTable table)
  {
    return JavaClassGeneratorUtil.getParentKey(table)!=null;
  }

  /**
   * Write the header
   * @param pr
   * @param table 
   * @throws MetaException 
   */
  private void writeHeader(PrintWriter pr, SqlTable table) throws MetaException
  {
    pr.println("/**");
    pr.println(" * ----------------------------------------------------------------------<br>");
    pr.println(" * This code was generated by the JavaEntityClassGenerator.<br>");
    pr.println(" * Do not edit this class instead edit the meta information this class was generated of.<br>");
    pr.println(" * ----------------------------------------------------------------------<br>");
    pr.print(" * Java Entity Class for table ");
    pr.print(table.getId());
    pr.println("<br>");
    pr.println(" * ----------------------------------------------------------------------<br>");
    writeJavaDocComment(pr, 0, table.getComment());
    pr.println(" * ----------------------------------------------------------------------<br>");
    pr.print(" * <b>Key:</b> ");
    writeColumnInformation(pr, table, JavaClassGeneratorUtil.getPrimaryKeyColumn(table));
    pr.println("<br>");
    if (JavaClassGeneratorUtil.getParentKeyColumn(table)!=null)
    {
      pr.print(" * <b>Parent Key:</b> ");
      writeColumnInformation(pr, table, JavaClassGeneratorUtil.getParentKeyColumn(table));
      pr.println("<br>");
    }
    pr.println(" * ----------------------------------------------------------------------<br>");
    pr.println(" */");
  }

  /**
   * Write the package
   * @param pr the print writer
   */
  private void writePackage(PrintWriter pr)
  {
    pr.print("package ");
    pr.print(getTargetPackage());
    pr.println(';');
    pr.println();
  }
}
