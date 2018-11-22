package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import ch.ivyteam.db.meta.generator.Target;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlBinaryRelation;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexWhenThen;
import ch.ivyteam.db.meta.model.internal.SqlDatabaseSystemHints;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlFunction;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlJoinTable;
import ch.ivyteam.db.meta.model.internal.SqlLogicalExpression;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlNot;
import ch.ivyteam.db.meta.model.internal.SqlObject;
import ch.ivyteam.db.meta.model.internal.SqlParent;
import ch.ivyteam.db.meta.model.internal.SqlSelect;
import ch.ivyteam.db.meta.model.internal.SqlSimpleExpr;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlTrigger;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;
import ch.ivyteam.db.meta.model.internal.SqlWhenThen;

/**
 * Generates html documents out of the sql meta information
 * @author rwei 
 */
public class HtmlDocGenerator implements IMetaOutputGenerator
{
  /** The output directory */
  private File outputDir;
  /** The open html tags */
  private Stack<String> htmlTags = new Stack<String>();
  /** The row of a table */
  private int fRow;
  private Stack<Map<String, String>> tableAliases = new Stack<>();
  
  /**
   * @see IMetaOutputGenerator#analyseArgs(String[])
   */
  @Override
  public void analyseArgs(String[] generatorArgs) throws Exception
  {
    if (generatorArgs.length < 2)
    {
      throw new Exception("There must be at least 2 generator options");
    }
    if (!generatorArgs[0].equalsIgnoreCase("-outputDir"))
    {
      throw new Exception("First generator option must be -outputDir");
    }
    outputDir = new File(generatorArgs[1]);
    if (!outputDir.exists())
    {
      outputDir.mkdirs();
    }
  }
  
  @Override
  public Target getTarget()
  {
    return Target.createTargetDirectory(outputDir);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    writeStylesheet();
    writeOverview(metaDefinition);
    writeTables(metaDefinition);
    writeViews(metaDefinition);
  }

  /**
   * Writes the style sheet file
   * @throws FileNotFoundException 
   */
  private void writeStylesheet() throws FileNotFoundException
  {
    PrintWriter pr;
    
    pr = new NewLinePrintWriter(new File(outputDir, "style.css"));
    try
    {
      pr.append("th\n");
      pr.append("{\n");
      pr.append("  background-color: #BBBBBB;\n");
      pr.append("  font-size: 14px;\n");
      pr.append("  font-weight: bold;\n");
      pr.append("  color: #000000;\n");
      pr.append("  text-align: left;\n");
      pr.append("}\n");
      pr.append("td\n");
      pr.append("{ \n");
      pr.append("  background-color: #DDDDDD;\n");
      pr.append("  text-align: left;\n");
      pr.append("  vertical-align: text-top;\n");
      pr.append("  font-size: 14px;\n");
      pr.append("  font-weight: normal;\n");
      pr.append("  color: #000000;\n");
      pr.append("}\n");

      pr.append("td.odd\n");
      pr.append("{\n");
      pr.append("  background-color: #EEEEEE;\n");
      pr.append("  text-align: left;\n");
      pr.append("  vertical-align: text-top;\n");
      pr.append("  font-size: 14px;\n");
      pr.append("  font-weight: normal;\n");
      pr.append("  color: #000000;\n");
      pr.append("}\n");

      pr.append("h3\n");
      pr.append("{\n");
      pr.append("  font-size: 14px;\n");
      pr.append("  font-weight: bold;\n");
      pr.append("}\n");

      pr.append("h2\n");
      pr.append("{\n");
      pr.append("  font-size: 18px;\n");
      pr.append("  font-weight: bold;\n");
      pr.append("}\n");

      pr.append("h1\n");
      pr.append("{\n");
      pr.append("  font-size: 22px;\n");
      pr.append("  font-weight: bold;\n");
      pr.append("}\n");

      pr.append("p\n");
      pr.append("{\n");
      pr.append("  font-size: 14px;\n");
      pr.append("  font-weight: normal;\n");
      pr.append("}\n");
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }   
  }

  /**
   * Writes the tables output files
   * @param metaDefinition the meta definition
   * @throws FileNotFoundException if file cannot be created
   * @throws MetaException 
   */
  private void writeTables(SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      writeTable(metaDefinition, table);
    }  
  }
  

  /**
   * Writes the tables output files
   * @param metaDefinition the meta definition
   * @throws FileNotFoundException if file cannot be created
   * @throws MetaException 
   */
  private void writeViews(SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    for (SqlView view : metaDefinition.getArtifacts(SqlView.class))
    {
      writeView(metaDefinition, view);
    }  
  }
  
  /**
   * Writes view output file
   * @param metaDefinition the meta definition
   * @param view the table
   * @throws FileNotFoundException
   * @throws MetaException 
   */
  private void writeView(SqlMeta metaDefinition, SqlView view) throws FileNotFoundException, MetaException
  {
    PrintWriter pr;
    
    pr = new NewLinePrintWriter(new File(outputDir, view.getId()+".html"));
    try
    {
      writeHeader(pr, "View "+view.getId());
      writeStartTag(pr, "body");
      writeStartTag(pr, "h1");
      writeText(pr, "View " + view.getId());
      writeEndTag(pr);
      writeStartTag(pr, "p");
      writeComment(pr, view.getComment());
      writeEndTag(pr);      
      writeViewColumns(pr, view);
      int pos = 1;
      for (SqlSelect select : view.getSelects())
      {
        writeStartTag(pr, "h2");
        if (view.getSelects().size()>1)
        {
          writeText(pr, "Select "+pos++);
        }
        else
        {
          writeText(pr, "Select");
        }
        writeEndTag(pr);
        writeSelectTables(pr, metaDefinition, select);
        writeSelectCondition(pr, select);
      }
      writeEndTags(pr, 2);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
  }

  /**
   * Writes table output file
   * @param metaDefinition the meta definition
   * @param table the table
   * @throws FileNotFoundException
   * @throws MetaException 
   */
  private void writeTable(SqlMeta metaDefinition, SqlTable table) throws FileNotFoundException, MetaException
  {
    PrintWriter pr;
    
    pr = new NewLinePrintWriter(new File(outputDir, table.getId()+".html"));
    try
    {
      writeHeader(pr, "Table "+table.getId());
      writeStartTag(pr, "body");
      writeStartTag(pr, "h1");
      writeText(pr, "Table " + table.getId());
      writeEndTag(pr);
      writeColumns(pr, table);
      writeConstraints(pr, table);      
      writeIndexes(pr, table);
      writeTriggers(pr, table);
      writeReferencedBy(pr, metaDefinition, table);
      writeDatabaseSystemHints(pr, table);
      writeEndTags(pr, 2);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
  }

  /**
   * @param pr
   * @param table
   */
  private void writeDatabaseSystemHints(PrintWriter pr, SqlTable table)
  {
    for (String databaseSystem : getAllDatabaseSystemsWithDatabaseSystemHints(table))
    {
      writeDatabaseSystemHints(pr, databaseSystem, table);
    }
  }

  /**
   * Gets all database systems with database system hints on any sql object declared by the given table
   * @param table the table
   * @return set with the database systems
   */
  private Set<String> getAllDatabaseSystemsWithDatabaseSystemHints(SqlTable table)
  {
    Set<String> databaseSystems = new LinkedHashSet<String>();
    for (SqlObject object: getSqlObjectsOfTable(table))
    {
      for (SqlDatabaseSystemHints databaseSystemHints : object.getDatabaseManagementSystemHints())
      {
        if (!object.getDatabaseManagementSystemHints(databaseSystemHints.getDatabaseManagementSystem()).isEmpty())
        {
          databaseSystems.add(databaseSystemHints.getDatabaseManagementSystem());
        }
      }
    }
    return databaseSystems;
  }

  /**
   * @param pr
   * @param databaseSystem
   * @param table
   */
  private void writeDatabaseSystemHints(PrintWriter pr, String databaseSystem, SqlTable table)
  {
    writeStartTag(pr, "h2");
    writeText(pr, databaseSystem);
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Object");
    writeEndTag(pr);
    for (String hint : getAllDatabaseSystemHints(databaseSystem, table))
    {
      writeStartTag(pr, "th");
      writeText(pr, hint);
      writeEndTag(pr);
    }
    writeEndTag(pr);
    for (SqlObject object : getAllSqlObjectsWithDatabaseSystemHints(databaseSystem, table))
    {
      writeNewRow(pr);
      writeNewColumn(pr);
      writeAnchor(pr, databaseSystem+"_"+object.getId());
      writeReference(pr, object.getId(), null, object.getId());
      writeEndTag(pr);
      for (String hint : getAllDatabaseSystemHints(databaseSystem, table))
      {
        writeNewColumn(pr);
        if (object.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint))
        {
          if (object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint)!=null)
          {
            writeText(pr, object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint));
          }
          else
          {
            writeText(pr, "true");
          }
        }
        else
        {  
          writeText(pr, "");
        }
        writeEndTag(pr);       
      }
      writeEndTag(pr);
    }
    writeEndTag(pr);
  }

  /**
   * Gets all database system hints for the given database system on any sql object declared by the given table
   * @param databaseSystem the database system
   * @param table the table
   * @return set of database system hins
   */
  private Set<String> getAllDatabaseSystemHints(String databaseSystem, SqlTable table)
  {
    Set<String> hints = new TreeSet<String>();
    for (SqlObject object : getSqlObjectsOfTable(table))
    {
      hints.addAll(object.getDatabaseManagementSystemHints(databaseSystem).getHintNames());
    }
    return hints;
  }

  /**
   * Gets all sql object of the given table that has database system hints for the given database system set
   * @param databaseSystem the database system
   * @param table the table
   * @return set of sql object that has database system hints set
   */
  private Set<SqlObject> getAllSqlObjectsWithDatabaseSystemHints(String databaseSystem, SqlTable table)
  {
    Set<SqlObject> objectsWithHints = new LinkedHashSet<SqlObject>();
    
    for (SqlObject object: getSqlObjectsOfTable(table))
    {
      if (!object.getDatabaseManagementSystemHints(databaseSystem).isEmpty())
      {
        objectsWithHints.add(object);
      }
    }
    return objectsWithHints;
  }

  /**
   * Gets the sql objects of the given table
   * @param table the table
   * @return set of sql objects that are declared by the table
   */
  private Set<SqlObject> getSqlObjectsOfTable(SqlTable table)
  {
    Set<SqlObject> objects = new LinkedHashSet<SqlObject>();
    
    objects.add(table);
    objects.addAll(table.getColumns());
    objects.addAll(table.getIndexes());
    objects.addAll(table.getUniqueConstraints());
    objects.addAll(table.getTriggers());
    return objects;
  }

  /**
   * Writes referenced by documentation 
   * @param pr the print writer to write to
   * @param metaDefinition the meta definition
   * @param table the table to write the referenced by documentation
   * @throws MetaException 
   */
  private void writeReferencedBy(PrintWriter pr, SqlMeta metaDefinition,
          SqlTable table) throws MetaException
  {
    boolean first = true;
    for (SqlTable foreignTable : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTableColumn column : foreignTable.getColumns())
      {
        if ((column.getReference() != null)&&(column.getReference().getForeignTable().equals(table.getId())))
        {
          if (first)
          {
            writeStartTag(pr, "h2");
            writeText(pr, "Referenced By");
            writeEndTag(pr);
            writeStartTag(pr, "table");
            writeNewRow(pr);
            writeStartTag(pr, "th");
            writeText(pr, "Foreign Table");
            writeEndTag(pr);
            writeStartTag(pr, "th");
            writeText(pr, "Column");
            writeEndTag(pr);   
            writeStartTag(pr, "th");
            writeText(pr, "Comment");
            writeEndTags(pr, 2);   
          }
          first = false;
          writeNewRow(pr);
          writeNewColumn(pr);
          writeTableReference(pr, foreignTable.getId());
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableColumnReference(pr, foreignTable.getId(), column.getId());
          writeEndTag(pr);
          writeNewColumn(pr);
          writeComment(pr, column.getComment());
          writeEndTags(pr, 2);
        }
      }
    }
    if (!first)
    {
      writeEndTag(pr);
    }
  }

  /**
   * Writes a new row
   * @param pr 
   */
  private void writeNewRow(PrintWriter pr)
  {
    writeStartTag(pr, "tr");
    fRow++;
  }

  /**
   * Writes a new column
   * @param pr 
   */
  private void writeNewColumn(PrintWriter pr)
  {
    writeStartTag(pr, "td", "class", fRow%2==0?"even":"odd");
  }
  
  /**
   * Writes a table column reference to the given column
   * @param pr the print writer
   * @param tableName the name of the table of the column
   * @param columnName the name of the column
   */
  private void writeTableColumnReference(PrintWriter pr, String tableName, String columnName)
  {
    if (tableName == null)
    {
      writeColumnReference(pr, columnName);
    }
    else
    {
      writeReference(pr, columnName, tableName+".html", columnName);
    }
  }
  
  /**
   * Writes a table column reference to the given column
   * @param pr the print writer
   * @param columnName the name of the column
   */
  private void writeColumnReference(PrintWriter pr, String columnName)
  {
    writeReference(pr, columnName, null, columnName);
  }
  
  /**
   * Writes reference to a full qualified column name
   * @param pr the print writer
   * @param columnName the full qualified column name
   */
  private void writeTableColumnReference(PrintWriter pr, SqlFullQualifiedColumnName columnName)
  {
    if (columnName.getTable() == null)
    {
      writeColumnReference(pr, columnName.getColumn());
    }
    else
    {     
      writeTableReference(pr, columnName.getTable());
      writeText(pr, ".");
      writeTableColumnReference(pr, columnName.getTable(), columnName.getColumn());
    }
  }

  /**
   * Writes a reference to the given table
   * @param pr the print writer
   * @param tableName the name of the table
   */
  private void writeTableReference(PrintWriter pr, String tableName)
  {
    String originalTableName = resolveTableAlias(tableName);
    writeReference(pr, tableName, originalTableName+".html", null);
  }
  
  /**
   * Writes a reference. Either file or anchor or both must be defined.
   * @param pr the print writer
   * @param text the text that is printed
   * @param file the file to reference. Maybe null
   * @param anchor the name of the anchor  to reference. Maybe null.
   */
  private void writeReference(PrintWriter pr, String text, String file, String anchor)
  {
    String ref = "";
    if (file != null)
    {
      ref = file;
    }
    if (anchor != null)
    {
      ref += "#"+anchor;
    }
    writeStartTag(pr, "a", "href", ref);
    writeText(pr, text);
    writeEndTags(pr, 1);
  }

  /**
   * Writes the triggers of a table
   * @param pr the print writer to write to
   * @param table the table definition
   * @throws MetaException 
   */
  private void writeTriggers(PrintWriter pr, SqlTable table) throws MetaException
  {
    if (table.getTriggers().size()>0)
    {
      writeStartTag(pr, "h2");
      writeText(pr, "Triggers");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Table");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Statements (For Each Row)");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Statements (For Each Stmt)");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Hints");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Comment");
      writeEndTags(pr, 2);
      for (SqlTrigger trigger : table.getTriggers())
      {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeAnchor(pr, trigger.getId());
        writeText(pr, trigger.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeTableReference(pr, trigger.getTableName());
        writeEndTag(pr);
        writeNewColumn(pr);
        for (SqlDmlStatement stmt : trigger.getStatementsForEachRow())
        {
          writeDmlStatement(pr, stmt);
          writeTag(pr, "br");
          writeTag(pr, "br");
        }        
        writeEndTag(pr);
        writeNewColumn(pr);
        for (SqlDmlStatement stmt : trigger.getStatementsForEachStatement())
        {
          writeDmlStatement(pr, stmt);
          writeTag(pr, "br");
          writeTag(pr, "br");
        }        
        writeEndTag(pr);
        writeNewColumn(pr);
        writeSystemDatabaseHintRefrences(pr, trigger);
        writeEndTag(pr);
        writeNewColumn(pr);
        writeComment(pr, trigger.getComment());        
        writeEndTags(pr, 2);
      }
      writeEndTag(pr);
    }

  }

  /**
   * Writes a dml statment
   * @param pr print writer
   * @param stmt dml statement
   */
  private void writeDmlStatement(PrintWriter pr, SqlDmlStatement stmt)
  {
    if (stmt instanceof SqlUpdate)
    {
      writeUpdateStatement(pr, (SqlUpdate)stmt);
    }
  }


  /**
   * Writes an update statement
   * @param pr the print writer
   * @param stmt the statement
   */
  private void writeUpdateStatement(PrintWriter pr, SqlUpdate stmt)
  {
    String table;
    boolean first = true;
    
    table = stmt.getTable();
    writeText(pr, "UPDATE ");
    if (table != null)
    {
      writeTableReference(pr, table);
    }
    writeText(pr, "SET ");
    for (SqlUpdateColumnExpression columnExpr : stmt.getColumnExpressions())
    {
      if (!first)
      {
        writeText(pr, ", ");
      }
      first = false;
      writeTableColumnReference(pr, table, columnExpr.getColumnName());
      writeText(pr, "=");
      writeAtom(pr, columnExpr.getExpression());          
    }
    if (stmt.getFilterExpression() != null)
    {
      writeText(pr, " WHERE ");
      writeSimpleExpr(pr, stmt.getFilterExpression());
    }    
  }

  /**
   * Writes the indexes of a table
   * @param pr the print writer to write to
   * @param table the table definition
   * @throws MetaException 
   */
  private void writeIndexes(PrintWriter pr, SqlTable table) throws MetaException
  {
    if (table.getIndexes().size()>0)
    {
      writeStartTag(pr, "h2");
      writeText(pr, "Indexes");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Columns");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Hints");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Comment");
      writeEndTags(pr, 2);
      for (SqlIndex index : table.getIndexes())
      {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeAnchor(pr, index.getId());
        writeText(pr, index.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeColumnList(pr, index.getColumns());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeSystemDatabaseHintRefrences(pr, index);
        writeEndTag(pr);
        writeNewColumn(pr);
        writeComment(pr, index.getComment());
        writeEndTags(pr, 2);
      }
      writeEndTag(pr);
    }
  }

  /**
   * Writes the constraints of a table
   * @param pr the print writer to write to
   * @param table the table definition
   * @throws MetaException 
   */
  private void writeConstraints(PrintWriter pr, SqlTable table) throws MetaException
  {
    if (table.getUniqueConstraints().size()>0)
    {
      writeStartTag(pr, "h2");
      writeText(pr, "Constraints");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Constraint");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);      
      writeStartTag(pr, "th");
      writeText(pr, "Columns");
      writeEndTag(pr);      
      writeStartTag(pr, "th");
      writeText(pr, "Hints");
      writeEndTag(pr);      
      writeStartTag(pr, "th");
      writeText(pr, "Comment");
      writeEndTags(pr, 2);
      for (SqlUniqueConstraint unique : table.getUniqueConstraints())
      {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeText(pr, "UNIQUE");
        writeEndTag(pr);
        writeNewColumn(pr);
        writeAnchor(pr, unique.getId());
        writeText(pr, unique.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeColumnList(pr, unique.getColumns());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeSystemDatabaseHintRefrences(pr, unique);
        writeEndTag(pr);
        writeNewColumn(pr);
        writeComment(pr, unique.getComment());
        writeEndTags(pr, 2);
      }
      writeEndTag(pr);
    }
  }

  /**
   * Writes the columns of a table
   * @param pr the print writer to write to 
   * @param table the table definition
   * @throws MetaException 
   */
  private void writeColumns(PrintWriter pr, SqlTable table) throws MetaException
  {
    writeStartTag(pr, "p");
    writeComment(pr, table.getComment());
    writeEndTag(pr);
    writeStartTag(pr, "h2");
    writeText(pr, "Columns");
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Primary Key");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Name");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Type");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Length");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Precision");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Can Be Null");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Default");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "References");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Action");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Hints");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Comment");
    writeEndTags(pr, 2);
    for (SqlTableColumn column : table.getColumns())
    {
      writeNewRow(pr);
      writeNewColumn(pr);
      writeAnchor(pr, column.getId());
      writeText(pr, isPrimaryKeyColumn(table, column)?"true":"false");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getId());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getDataType().toString());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getLength() > 0 ? ""+column.getDataType().getLength() : "");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getPrecision() > 0 ? ""+column.getDataType().getPrecision() : "");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, ""+column.isCanBeNull());
      writeEndTag(pr);
      writeNewColumn(pr);
      if (column.getDefaultValue()!=null)
      {
        String defaultValue;
        defaultValue = column.getDefaultValue().toString();
        if (column.getDefaultValue().getValue() instanceof String)
        {
          defaultValue = "'"+defaultValue+"'";
        }
        else
        {
          writeText(pr, ""+defaultValue);
        }
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      if (column.getReference() != null)
      {
        writeStartTag(pr, "a", "href", column.getReference().getForeignTable()+".html#"+column.getReference().getForeignColumn());
        writeText(pr, column.getReference().getForeignTable()+"("+column.getReference().getForeignColumn()+")");
        writeEndTag(pr);
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      if ((column.getReference()!=null)&&(column.getReference().getForeignKeyAction()!=null))
      {
        writeText(pr, column.getReference().getForeignKeyAction().toString());            
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      writeSystemDatabaseHintRefrences(pr, column);
      writeEndTag(pr);
      writeNewColumn(pr);
      writeComment(pr, column.getComment());
      writeEndTags(pr, 2);
    }
    writeEndTag(pr);
  }

  /**
   * Writes references to the system database hints defined on the given sql object
   * @param pr the print writer
   * @param object the object 
   */
  private void writeSystemDatabaseHintRefrences(PrintWriter pr, SqlObject object)
  {
    Set<String> systemDatabases = new TreeSet<String>();
    boolean first = true;
    for (SqlDatabaseSystemHints hints : object.getDatabaseManagementSystemHints())
    {
      if (!hints.isEmpty())
      {
        systemDatabases.add(hints.getDatabaseManagementSystem());
      }
    }
    for (String systemDatabase : systemDatabases)
    {
      if (!first)
      {
        writeText(pr, ", ");        
      }
      first = false;
      writeReference(pr, systemDatabase, null, systemDatabase+"_"+object.getId());      
    }
  }

  /**
   * Writes an anchor
   * @param pr 
   * @param anchor the anchor name
   */
  private void writeAnchor(PrintWriter pr, String anchor)
  {
    writeStartTag(pr, "a", "name", anchor);
    writeEndTag(pr);
  }
  
  /**
   * Writes the tables the view is build upon
   * @param pr the print writer
   * @param metaDefinition the meta definition
   * @param select the select
   * @throws MetaException 
   */
  private void writeSelectTables(PrintWriter pr, SqlMeta metaDefinition, SqlSelect select) throws MetaException
  {
    writeStartTag(pr, "h3");
    writeText(pr, "Tables");
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Table");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Alias");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Join");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "On");
    writeEndTag(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Comment");
    writeEndTags(pr, 2);
    for (SqlJoinTable table : select.getJoinTables())
    {
      writeNewRow(pr);
      writeNewColumn(pr);
      writeTableReference(pr, table.getTable().getName());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, table.getTable().getAlias());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, table.getJoinKind());
      writeEndTag(pr);
      writeNewColumn(pr);
      if (table.getJoinCondition() != null)
      {
        writeSimpleExpr(pr, table.getJoinCondition());
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      SqlTable joinTable = metaDefinition.findTable(table.getTable().getName());
      if (joinTable == null)
      {
        throw new MetaException("Could not find table "+table.getTable().getName());
      }
      writeComment(pr, joinTable.getComment());
      writeEndTags(pr, 2);
    }
    writeEndTag(pr);    
  }
  
  /**
   * Writes the view condition
   * @param pr the print writer
   * @param select the select
   */
  private void writeSelectCondition(PrintWriter pr, SqlSelect select)
  {
    if (select.getCondition() != null)
    {
      writeStartTag(pr, "h3");
      writeText(pr, "Condition");
      writeEndTag(pr);
      writeStartTag(pr, "p");
      writeSimpleExpr(pr, select.getCondition());
      writeEndTag(pr);
    }
  }


  /**
   * Writes a simple expression
   * @param pr the print writer 
   * @param simpleExpr the simple expression
   */
  private void writeSimpleExpr(PrintWriter pr, SqlSimpleExpr simpleExpr)
  {
    if (simpleExpr instanceof SqlBinaryRelation)
    {
      SqlBinaryRelation binaryExpr;
      
      binaryExpr = (SqlBinaryRelation)simpleExpr;
      writeAtom(pr, binaryExpr.getFirst());
      writeText(pr, binaryExpr.getOperator());
      writeAtom(pr, binaryExpr.getSecond());      
    }
    else if (simpleExpr instanceof SqlLogicalExpression)
    {
      SqlLogicalExpression logicalExpr;
      
      logicalExpr = (SqlLogicalExpression)simpleExpr;
      writeSimpleExpr(pr, logicalExpr.getFirst());
      writeText(pr, logicalExpr.getOperator());
      writeSimpleExpr(pr, logicalExpr.getSecond());
    }
    else if (simpleExpr instanceof SqlNot)
    {
      writeText(pr, "NOT ");
      writeSimpleExpr(pr, ((SqlNot)simpleExpr).getExpression());
    }
    else if (simpleExpr instanceof SqlParent)
    {
      writeText(pr, "(");
      writeSimpleExpr(pr, ((SqlParent)simpleExpr).getExpression());
      writeText(pr, ")");
    }
    else 
    {
      writeText(pr, simpleExpr.toString());
    }
  }

  /**
   * Writes the columns of a view
   * @param pr the print writer to write to 
   * @param view the view definition
   * @throws MetaException 
   */
  private void writeViewColumns(PrintWriter pr, SqlView view) throws MetaException
  {
    writeStartTag(pr, "h2");
    writeText(pr, "Columns");
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Name");
    writeEndTag(pr);
    for (int pos = 1; pos <= view.getSelects().size(); pos++)
    {
      writeStartTag(pr, "th");
      if (view.getSelects().size()>1)
      {
        writeText(pr, "Expression on Select "+pos);
      }
      else
      {
        writeText(pr, "Expression");
      }
      writeEndTag(pr);
    }
    writeStartTag(pr, "th");
    writeText(pr, "Comment");
    writeEndTags(pr, 2);
    int col=0;
    for (SqlViewColumn column : view.getColumns())
    {
      writeStartTag(pr, "tr");
      writeNewColumn(pr);
      writeAnchor(pr, column.getId());
      writeText(pr, column.getId());
      writeEndTag(pr);
      for (SqlSelect select: view.getSelects())
      {
        pushTableAliases(select.getTableAliases());
        writeNewColumn(pr);
        writeAtom(pr, select.getExpressions().get(col).getExpression());
        writeEndTag(pr);
        popTableAliases();
      }
      col++;
      writeNewColumn(pr);
      writeComment(pr, column.getComment());
      writeEndTags(pr, 2);
    }
    writeEndTag(pr);
  }

  /**
   * Writes a atom expression
   * @param pr the print writer
   * @param atom the atom expression
   */
  private void writeAtom(PrintWriter pr, SqlAtom atom)
  {
    if (atom instanceof SqlCaseExpr)
    {
      SqlCaseExpr caseExpr = (SqlCaseExpr)atom;
      writeText(pr, "CASE ");
      writeTableColumnReference(pr, caseExpr.getColumnName());
      for (SqlWhenThen whenThen : caseExpr.getWhenThenList())
      {
        writeText(pr, " WHEN ");
        writeText(pr, whenThen.getLiteral().toString());
        writeText(pr, " THEN ");
        writeTableColumnReference(pr, whenThen.getColumnName());
      }
    }
    if (atom instanceof SqlComplexCaseExpr)
    {
      SqlComplexCaseExpr caseExpr = (SqlComplexCaseExpr)atom;
      writeText(pr, "CASE ");
      writeTag(pr, "br");
      for (SqlComplexWhenThen whenThen : caseExpr.getWhenThenList())
      {
        writeText(pr, " WHEN ");
        writeSimpleExpr(pr, whenThen.getCondition());
        writeText(pr, " THEN ");
        writeAtom(pr, whenThen.getAction());
        writeTag(pr, "br");
      }
      if (caseExpr.getElseAction() != null)
      {
        writeText(pr, " ELSE ");
        writeAtom(pr, caseExpr.getElseAction());
        writeTag(pr, "br");
      }
      writeText(pr, "END");
    }
    else if (atom instanceof SqlFullQualifiedColumnName)
    {
      SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName)atom;
      writeTableColumnReference(pr, columnName);
    }
    else if (atom instanceof SqlFunction)
    {
      SqlFunction function = (SqlFunction)atom;
      writeText(pr, function.getName());
      writeText(pr, "(");
      boolean first = true;
      for (SqlAtom argument : function.getArguments())
      {
        if (!first)
        {
          writeText(pr, ", ");
        }
        first = false;
        writeAtom(pr, argument);        
      }
      writeText(pr, ")");
    }
    else
    {
      writeText(pr, atom.toString());
    }
  }



  /**
   * Writes a list of columns 
   * @param pr the print writer to write to
   * @param list the list to write
   */
  private void writeColumnList(PrintWriter pr, List<String> list)
  {
    boolean first = true;
    writeSpaces(pr);
    for (String column: list)
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      writeColumnReference(pr, column);
    }
  }

  /**
   * Checks if the given column is a primary key column
   * @param table the table definition
   * @param column the column
   * @return true if column is part of the primary key, otherwise false
   */
  private boolean isPrimaryKeyColumn(SqlTable table, SqlTableColumn column)
  {
    if (table.getPrimaryKey() != null)
    {
      return table.getPrimaryKey().getPrimaryKeyColumns().contains(column.getId());
    }
    return false;
  }

  /**
   * Writes the overview page
   * @param metaDefinition the meta definition
   * @throws FileNotFoundException if page file cannot be written
   * @throws MetaException 
   */
  private void writeOverview(SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    PrintWriter pr;
    Set<String> databaseSystems;
    pr = new NewLinePrintWriter(new File(outputDir, "index.html"));
    try
    {
      writeHeader(pr, "Database Schema Overview");
      writeStartTag(pr, "body");
      writeStartTag(pr, "h1");
      writeText(pr, "Database Schema Overview");
      writeEndTag(pr);
      writeStartTag(pr, "p");
      writeReference(pr, "Tables", "tables.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Views", "views.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Columns", "columns.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Primary Keys", "primarykeys.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Foreign Keys", "foreignkeys.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Indexes", "indexes.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Constraints", "constraints.html", null);
      writeTag(pr, "br");
      writeReference(pr, "Triggers", "triggers.html", null);
      databaseSystems = getAllDatabaseSystemsWithDatabaseSystemHints(metaDefinition);
      for (String databaseSystem : databaseSystems)
      {
        writeTag(pr, "br");
        writeReference(pr, databaseSystem, databaseSystem+".html", null);
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
    writeTableOverview(metaDefinition);
    writeViewOverview(metaDefinition);
    writeColumnOverview(metaDefinition);
    writePrimaryKeyOverview(metaDefinition);
    writeForeignKeyOverview(metaDefinition);
    writeIndexOverview(metaDefinition);
    writeConstraintsOverview(metaDefinition);
    writeTriggerOverview(metaDefinition);
    for (String databaseSystem : databaseSystems)
    {
      writeDatabaseSystemOverview(metaDefinition, databaseSystem);
    }
  }
  
  /**
   * Writes the database system overview
   * @param metaDefinition the meta definition
   * @param databaseSystem the database system
   * @throws FileNotFoundException 
   */
  private void writeDatabaseSystemOverview(SqlMeta metaDefinition, String databaseSystem) throws FileNotFoundException
  {
    Set<String> hints;
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, databaseSystem+".html"));
    try
    {
      writeHeader(pr, databaseSystem);
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, databaseSystem);
      writeText(pr, databaseSystem);
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Table");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Object");
      writeEndTag(pr);
      hints = getAllDatabaseSystemHints(databaseSystem, metaDefinition);
      for (String hint : hints)
      {
        writeStartTag(pr, "th");
        writeText(pr, hint);
        writeEndTag(pr);        
      }
      writeEndTags(pr, 1);
      for (SqlTable table: metaDefinition.getArtifacts(SqlTable.class))
      {
        for (SqlObject object : getAllSqlObjectsWithDatabaseSystemHints(databaseSystem, table))
        {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, table.getId(), table.getId()+".html", null);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeReference(pr, object.getId(), table.getId()+".html", databaseSystem+"_"+object.getId());
          writeEndTag(pr);
          for (String hint : hints)
          {
            writeNewColumn(pr);
            if (object.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint))
            {
              if (object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint)!=null)
              {
                writeText(pr, object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint));
              }
              else
              {
                writeText(pr, "true");
              }
            }
            else
            {  
              writeText(pr, "");
            }
            writeEndTag(pr);
          }
          writeEndTag(pr);
        }
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
  }

  /**
   * Gets all hints that are defined for the given database system
   * @param databaseSystem the database system
   * @param metaDefinition the meta definition
   * @return set of hints
   */
  private Set<String> getAllDatabaseSystemHints(String databaseSystem, SqlMeta metaDefinition)
  {
    Set<String> hints = new TreeSet<String>();
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      hints.addAll(getAllDatabaseSystemHints(databaseSystem, table));
    }
    return hints;
  }

  /**
   * Get all database systems with database system hints
   * @param metaDefinition the meta definition
   * @return set of database systems
   */
  private Set<String> getAllDatabaseSystemsWithDatabaseSystemHints(SqlMeta metaDefinition)
  {
    Set<String> databaseSystems = new TreeSet<String>();
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      databaseSystems.addAll(getAllDatabaseSystemsWithDatabaseSystemHints(table));
    }
    return databaseSystems;
  }

  /**
   * Writes the foreign key overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writeForeignKeyOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlTable>> foreignKeys = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> foreignKeysSorted;
    SqlForeignKey foreignKey;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreign : table.getForeignKeys())
      {
        references = foreignKeys.get(foreign.getId());
        if (references == null)
        {
          references = new ArrayList<SqlTable>();
          foreignKeys.put(foreign.getId(), references);
        }
        references.add(table);
      }
    }
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "foreignkeys.html"));
    try
    {
      writeHeader(pr, "Foreign Keys");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "foreignKeys");
      writeText(pr, "Foreign Keys");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Definition");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "References");
      writeEndTags(pr, 2);
      foreignKeysSorted = new TreeSet<String>(foreignKeys.keySet());
      for (String foreignKeyName : foreignKeysSorted)
      {
        for (SqlTable table: foreignKeys.get(foreignKeyName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);      
          writeText(pr, foreignKeyName);
          writeEndTag(pr);
          writeNewColumn(pr);
          foreignKey = table.findForeignKey(foreignKeyName);
          writeTableReference(pr, table.getId());
          writeText(pr, "(");
          writeTableColumnReference(pr, table.getId(), foreignKey.getColumnName());
          writeText(pr, ")");
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableReference(pr, foreignKey.getReference().getForeignTable());
          writeText(pr, "(");
          writeTableColumnReference(pr, foreignKey.getReference().getForeignTable(), foreignKey.getReference().getForeignColumn());
          writeText(pr, ")");
          writeEndTags(pr, 2);
        }      
      }
      writeEndTags(pr, 3);
      }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
  }


  /**
   * Writes the primary key overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writePrimaryKeyOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlTable>> primaryKeys = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> primaryKeySorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      if (table.getPrimaryKey() != null)
      {
        references = primaryKeys.get(table.getPrimaryKey().getId());
        if (references == null)
        {
          references = new ArrayList<SqlTable>();
          primaryKeys.put(table.getPrimaryKey().getId(), references);
        }
        references.add(table);
      }
    }
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "primarykeys.html"));
    try
    {
      writeHeader(pr, "Primary Keys");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "primaryKeys");
      writeText(pr, "Primary Keys");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Definition");
      writeEndTags(pr, 2);
      primaryKeySorted = new TreeSet<String>(primaryKeys.keySet());
      for (String primaryKeyName : primaryKeySorted)
      {
        for (SqlTable table: primaryKeys.get(primaryKeyName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);      
          writeText(pr, primaryKeyName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableReference(pr, table.getId());
          writeText(pr, "(");
          writeColumnList(pr, table.getPrimaryKey().getPrimaryKeyColumns());
          writeText(pr, ")");
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }
  
  /**
   * Writes the index overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writeIndexOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlTable>> indexes = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> indexSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlIndex index : table.getIndexes())
      {
        references = indexes.get(index.getId());
        if (references == null)
        {
          references = new ArrayList<SqlTable>();
          indexes.put(index.getId(), references);
        }
        references.add(table);
      }
    }
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "indexes.html"));
    try
    {
      writeHeader(pr, "Indexes");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "indexes");
      writeText(pr, "Indexes");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Definition");
      writeEndTags(pr, 2);
      indexSorted = new TreeSet<String>(indexes.keySet());
      for (String indexName : indexSorted)
      {
        for (SqlTable table: indexes.get(indexName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, indexName, table.getId()+".html", indexName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableReference(pr, table.getId());
          writeText(pr, "(");
          writeColumnList(pr, table.findIndex(indexName).getColumns());
          writeText(pr, ")");
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }
  
  /**
   * Writes the constraints overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writeConstraintsOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlTable>> constraints = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> constraintsSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlUniqueConstraint constraint : table.getUniqueConstraints())
      {
        references = constraints.get(constraint.getId());
        if (references == null)
        {
          references = new ArrayList<SqlTable>();
          constraints.put(constraint.getId(), references);
        }
        references.add(table);
      }
    }
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "constraints.html"));
    try
    {
      writeHeader(pr, "Constraints");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "constraints");
      writeText(pr, "Constraints");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Definition");
      writeEndTags(pr, 2);
      constraintsSorted = new TreeSet<String>(constraints.keySet());
      for (String constraintName : constraintsSorted)
      {
        for (SqlTable table: constraints.get(constraintName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, constraintName, table.getId()+".html", constraintName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableReference(pr, table.getId());
          writeText(pr, "(");
          writeColumnList(pr, table.findUniqueConstraint(constraintName).getColumns());
          writeText(pr, ")");
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }

  /**
   * Writes the trigger overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writeTriggerOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlTable>> triggers = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> triggersSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : table.getTriggers())
      {
        references = triggers.get(trigger.getId());
        if (references == null)
        {
          references = new ArrayList<SqlTable>();
          triggers.put(trigger.getId(), references);
        }
        references.add(table);
      }
    }

    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "triggers.html"));
    try
    {
      writeHeader(pr, "Triggers");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "triggers");
      writeText(pr, "Triggers");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Triggered By");
      writeEndTags(pr, 2);
      triggersSorted = new TreeSet<String>(triggers.keySet());
      for (String triggerName : triggersSorted)
      {
        for (SqlTable table: triggers.get(triggerName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, triggerName, table.getId()+".html", triggerName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeText(pr, "DELETE ON ");
          writeTableReference(pr, table.findTrigger(triggerName).getTableName());
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }

  /**
   * Writes the column overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   */
  private void writeColumnOverview(SqlMeta metaDefinition) throws FileNotFoundException
  {
    Map<String, List<SqlObject>> columns = new HashMap<String, List<SqlObject>>();
    List<SqlObject> references;
    SortedSet<String> columnsSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTableColumn column : table.getColumns())
      {
        references = columns.get(column.getId());
        if (references == null)
        {
          references = new ArrayList<SqlObject>();
          columns.put(column.getId(), references);
        }
        references.add(table);
      }
    }
    for (SqlView view : metaDefinition.getArtifacts(SqlView.class))
    {
      for (SqlViewColumn column : view.getColumns())
      {
        references = columns.get(column.getId());
        if (references == null)
        {
          references = new ArrayList<SqlObject>();
          columns.put(column.getId(), references);
        }
        references.add(view);
      }
    }
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "columns.html"));
    try
    {
      writeHeader(pr, "Columns");
      writeStartTag(pr, "body");
      writeStartTag(pr, "h1");
      writeAnchor(pr, "columns");
      writeText(pr, "Columns");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Name");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Definition");
      writeEndTags(pr, 2);
      columnsSorted = new TreeSet<String>(columns.keySet());
      for (String columnName : columnsSorted)
      {
        for (SqlObject object: columns.get(columnName))
        {
          writeNewRow(pr);
          writeNewColumn(pr);      
          writeText(pr, columnName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeTableReference(pr, object.getId());
          writeText(pr, ".");
          writeTableColumnReference(pr, object.getId(), columnName);
          writeEndTags(pr, 2);
        }      
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }

  /**
   * Writes the view overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   * @throws MetaException 
   */
  private void writeViewOverview(SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "views.html"));
    try
    {
      writeHeader(pr, "Views");
      writeStartTag(pr, "body");

      writeStartTag(pr, "h1");
      writeAnchor(pr, "views");
      writeText(pr, "Views");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "View");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Comment");
      writeEndTags(pr, 2);
      for (SqlView view : metaDefinition.getSqlObjects(SqlView.class))
      {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeTableReference(pr, view.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeComment(pr, view.getComment());
        writeEndTags(pr, 2);
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";

  }

  /**
   * Writes the table overview
   * @param metaDefinition
   * @throws FileNotFoundException 
   * @throws MetaException 
   */
  private void writeTableOverview(SqlMeta metaDefinition) throws FileNotFoundException, MetaException
  {
    PrintWriter pr = new NewLinePrintWriter(new File(outputDir, "tables.html"));
    try
    {
      writeHeader(pr, "Tables");
      writeStartTag(pr, "body");
  
      writeStartTag(pr, "h1");
      writeAnchor(pr, "tables");
      writeText(pr, "Tables");
      writeEndTag(pr);
      writeStartTag(pr, "table");
      writeNewRow(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Table");
      writeEndTag(pr);
      writeStartTag(pr, "th");
      writeText(pr, "Comment");
      writeEndTags(pr, 2);
      for (SqlTable table : metaDefinition.getSqlObjects(SqlTable.class))
      {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeTableReference(pr, table.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeComment(pr, table.getComment());
        writeEndTags(pr, 2);
      }
      writeEndTags(pr, 3);
    }
    finally
    {
      IOUtils.closeQuietly(pr);
    }
    assert htmlTags.isEmpty() : "Stack with html tags must be empty after finishing writing a html page";
  }

  /**
   * Writes a comment
   * @param pr
   * @param comment
   * @throws MetaException 
   */
  private void writeComment(PrintWriter pr, String comment) throws MetaException
  {
    int pos;
    String tag;
    String cmt;
    
    cmt = comment;
    
    pos = cmt.indexOf("{@");
    while (pos >= 0)
    {
      writeText(pr, cmt.substring(0, pos));
      cmt = cmt.substring(pos);
      pos = cmt.indexOf('}');
      if (pos >= 0)
      {
        tag = cmt.substring(0, pos);
        cmt = cmt.substring(pos+1);
        if (tag.startsWith("{@ref"))
        {
          tag = tag.substring("{@ref".length());
          pos = tag.indexOf('.');
          if (pos >= 0)
          {
            writeReference(pr, tag.substring(pos).trim(), tag.substring(0, pos).trim()+".html", tag.substring(pos).trim());
          }
          else
          {            
            writeReference(pr, tag.trim(), tag.trim()+".html", null);
          }
        }
        else
        {
          throw new MetaException("Unknown comment tag "+tag);
        }
      }
      else
      {
        throw new MetaException("Comment tag not finished correctly. Missing '}' in comment:\n"+comment);
      }
      pos = cmt.indexOf("{@");
    }
    writeText(pr, cmt);
  }

  /**
   * Writing end tags
   * @param pr the print writer
   * @param endTags the number of end tags to write
   */
  private void writeEndTags(PrintWriter pr, int endTags)
  {
    for(int endTag = 0; endTag < endTags; endTag++)
    {
      writeEndTag(pr);
    }
  }

  /**
   * Writes the end tag
   * @param pr print writer
   */
  private void writeEndTag(PrintWriter pr)
  {
    String tag;
    tag = htmlTags.pop();
    writeSpaces(pr);
    pr.append("</");
    pr.append(tag);
    pr.append('>');
    pr.append("\n");
  }

  /**
   * Writes text
   * @param pr the print writer
   * @param text the text
   */
  private void writeText(PrintWriter pr, String text)
  {
    writeSpaces(pr);
    if (text != null)
    {
      pr.append(text);
    }
    pr.append('\n');
  }

  /**
   * Writes a start tag
   * @param pr the print writer
   * @param tag the tag
   */
  private void writeStartTag(PrintWriter pr, String tag)
  {
    writeSpaces(pr);
    pr.append('<');
    pr.append(tag);
    pr.append('>');
    pr.append("\n");
    htmlTags.push(tag);
  }

  /**
   * Writes spaces
   * @param pr the print writer
   */
  private void writeSpaces(PrintWriter pr)
  {
    for (int pos = 0; pos < htmlTags.size(); pos++)
    {
      pr.append("  ");
    }
  }

  /**
   * Writes the html header
   * @param pr the print writer to write to
   * @param title the title of the document
   */
  private void writeHeader(PrintWriter pr, String title)
  {
    // Reset row count so that even and row columns so that a document always start with an event column.
    // This ensures that the generated html does not change if nothing has changed in the meta data for the given document. 
    // This is important for source control systems 
    fRow=0; 
    pr.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
    writeStartTag(pr, "html");
    writeStartTag(pr, "head");
    writeTag(pr, "meta", "http-equiv", "content-type", "content", "text/html", "charset", "ISO-8859-1");
    writeTag(pr, "link", "rel", "STYLESHEET", "type", "text/css", "href", "style.css");
    writeStartTag(pr, "title");
    writeText(pr, title);
    writeEndTags(pr, 2);
  }

  /**
   * Writes a tag
   * @param pr
   * @param tag
   * @param attributes
   */
  private void writeTag(PrintWriter pr, String tag, String... attributes)
  {
    writeTag(pr, tag, false, attributes);
  }

  /**
   * Writes a tag
   * @param pr
   * @param tag
   * @param onlyStartTag
   * @param attributes
   */
  private void writeTag(PrintWriter pr, String tag, boolean onlyStartTag,
          String[] attributes)
  {
    writeSpaces(pr);
    pr.append('<');
    pr.append(tag);
    if ((attributes != null)&&(attributes.length>0))
    {
      assert attributes.length%2 == 0 : "There must be for each attribute a value";
      for (int pos = 0; pos < attributes.length; pos=pos+2)
      {
        pr.append(' ');
        pr.append(attributes[pos]);
        pr.append("=\"");
        pr.append(attributes[pos+1]);
        pr.append("\"");

      }
    }
    if (!onlyStartTag)
    {
      pr.append('/');
    }
    pr.append('>');
    pr.append('\n');
  }

  /**
   * Writes a start tag
   * @param pr
   * @param tag
   * @param attributes
   */
  private void writeStartTag(PrintWriter pr, String tag, String... attributes)
  {
    writeTag(pr, tag, true, attributes);
    htmlTags.push(tag);
  }

  /**
   * @see IMetaOutputGenerator#printHelp()
   */
  @Override
  public void printHelp()
  {
    System.out.println("HtmlDocGenerator Options: -outputDir {directory}");
  }

  private String resolveTableAlias(String alias)
  {
    if (tableAliases.isEmpty())
    {
      return alias;
    }
    Map<String, String> aliases = tableAliases.peek();
    String tableName = aliases.get(alias);
    if (tableName != null)
    {
      return tableName;
    }
    return alias;
  }

  private void pushTableAliases(Map<String, String> aliases)
  {
    tableAliases.push(aliases);
  }

  private void popTableAliases()
  {
    tableAliases.pop();
  }

}
