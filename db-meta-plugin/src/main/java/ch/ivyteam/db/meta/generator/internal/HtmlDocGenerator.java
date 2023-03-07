package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import org.apache.commons.io.FileUtils;

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

public class HtmlDocGenerator implements IMetaOutputGenerator {

  private File outputDir;
  private Stack<String> htmlTags = new Stack<>();
  private int fRow;
  private Stack<Map<String, String>> tableAliases = new Stack<>();

  @Override
  public void analyseArgs(String[] generatorArgs) throws Exception {
    if (generatorArgs.length < 2) {
      throw new Exception("There must be at least 2 generator options");
    }
    if (!generatorArgs[0].equalsIgnoreCase("-outputDir")) {
      throw new Exception("First generator option must be -outputDir");
    }
    outputDir = new File(generatorArgs[1]);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
  }

  @Override
  public Target getTarget() {
    return Target.createTargetDirectory(outputDir);
  }

  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception {
    writeStylesheet();
    writeOverview(metaDefinition);
    writeTables(metaDefinition);
    writeViews(metaDefinition);
  }

  private void writeStylesheet() throws IOException {
    try (var in = HtmlDocGenerator.class.getResourceAsStream("style.css")) {
      var styleCss = outputDir.toPath().resolve("style.css");
      FileUtils.copyInputStreamToFile(in, styleCss.toFile());
    }
  }

  private void writeTables(SqlMeta metaDefinition) throws FileNotFoundException {
    for (var table : metaDefinition.getArtifacts(SqlTable.class)) {
      writeTable(metaDefinition, table);
    }
  }

  private void writeViews(SqlMeta metaDefinition) throws FileNotFoundException {
    for (var view : metaDefinition.getArtifacts(SqlView.class)) {
      writeView(metaDefinition, view);
    }
  }

  private void writeView(SqlMeta metaDefinition, SqlView view) throws FileNotFoundException {
    try (var pr = new NewLinePrintWriter(new File(outputDir, view.getId() + ".html"))) {
      writeHeader(pr, "View " + view.getId());
      writeStartTag(pr, "body");
      writeStartTag(pr, "h1");
      writeText(pr, "View " + view.getId());
      writeEndTag(pr);
      writeStartTag(pr, "p");
      writeComment(pr, view.getComment());
      writeEndTag(pr);
      writeViewColumns(pr, view);
      int pos = 1;
      for (SqlSelect select : view.getSelects()) {
        writeStartTag(pr, "h2");
        if (view.getSelects().size() > 1) {
          writeText(pr, "Select " + pos++);
        } else {
          writeText(pr, "Select");
        }
        writeEndTag(pr);
        writeSelectTables(pr, metaDefinition, select);
        writeSelectCondition(pr, select);
      }
      writeEndTags(pr, 2);
    }
  }

  private void writeTable(SqlMeta metaDefinition, SqlTable table) throws FileNotFoundException {
    try (var pr = new NewLinePrintWriter(new File(outputDir, table.getId() + ".html"))) {
      writeHeader(pr, "Table " + table.getId());
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
  }

  private void writeDatabaseSystemHints(PrintWriter pr, SqlTable table) {
    for (String databaseSystem : getAllDatabaseSystemsWithDatabaseSystemHints(table)) {
      writeDatabaseSystemHints(pr, databaseSystem, table);
    }
  }

  private Set<String> getAllDatabaseSystemsWithDatabaseSystemHints(SqlTable table) {
    var databaseSystems = new LinkedHashSet<String>();
    for (var object : getSqlObjectsOfTable(table)) {
      for (var databaseSystemHints : object.getDatabaseManagementSystemHints()) {
        if (!object.getDatabaseManagementSystemHints(databaseSystemHints.getDatabaseManagementSystem())
                .isEmpty()) {
          databaseSystems.add(databaseSystemHints.getDatabaseManagementSystem());
        }
      }
    }
    return databaseSystems;
  }

  private void writeDatabaseSystemHints(PrintWriter pr, String databaseSystem, SqlTable table) {
    writeStartTag(pr, "h2");
    writeText(pr, databaseSystem);
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Object");
    writeEndTag(pr);
    for (String hint : getAllDatabaseSystemHints(databaseSystem, table)) {
      writeStartTag(pr, "th");
      writeText(pr, hint);
      writeEndTag(pr);
    }
    writeEndTag(pr);
    for (SqlObject object : getAllSqlObjectsWithDatabaseSystemHints(databaseSystem, table)) {
      writeNewRow(pr);
      writeNewColumn(pr);
      writeAnchor(pr, databaseSystem + "_" + object.getId());
      writeReference(pr, object.getId(), null, object.getId());
      writeEndTag(pr);
      for (String hint : getAllDatabaseSystemHints(databaseSystem, table)) {
        writeNewColumn(pr);
        if (object.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint)) {
          if (object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint) != null) {
            writeText(pr, object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint));
          } else {
            writeText(pr, "true");
          }
        } else {
          writeText(pr, "");
        }
        writeEndTag(pr);
      }
      writeEndTag(pr);
    }
    writeEndTag(pr);
  }

  private Set<String> getAllDatabaseSystemHints(String databaseSystem, SqlTable table) {
    var hints = new TreeSet<String>();
    for (var object : getSqlObjectsOfTable(table)) {
      hints.addAll(object.getDatabaseManagementSystemHints(databaseSystem).getHintNames());
    }
    return hints;
  }

  private Set<SqlObject> getAllSqlObjectsWithDatabaseSystemHints(String databaseSystem, SqlTable table) {
    var objectsWithHints = new LinkedHashSet<SqlObject>();
    for (var object : getSqlObjectsOfTable(table)) {
      if (!object.getDatabaseManagementSystemHints(databaseSystem).isEmpty()) {
        objectsWithHints.add(object);
      }
    }
    return objectsWithHints;
  }

  private Set<SqlObject> getSqlObjectsOfTable(SqlTable table) {
    var objects = new LinkedHashSet<SqlObject>();
    objects.add(table);
    objects.addAll(table.getColumns());
    objects.addAll(table.getIndexes());
    objects.addAll(table.getUniqueConstraints());
    objects.addAll(table.getTriggers());
    return objects;
  }

  private void writeReferencedBy(PrintWriter pr, SqlMeta metaDefinition, SqlTable table) {
    boolean first = true;
    for (var foreignTable : metaDefinition.getArtifacts(SqlTable.class)) {
      for (var column : foreignTable.getColumns()) {
        if ((column.getReference() != null)
                && (column.getReference().getForeignTable().equals(table.getId()))) {
          if (first) {
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
    if (!first) {
      writeEndTag(pr);
    }
  }

  private void writeNewRow(PrintWriter pr) {
    writeStartTag(pr, "tr");
    fRow++;
  }

  private void writeNewColumn(PrintWriter pr) {
    writeStartTag(pr, "td", "class", fRow % 2 == 0 ? "even" : "odd");
  }

  private void writeTableColumnReference(PrintWriter pr, String tableName, String columnName) {
    if (tableName == null) {
      writeColumnReference(pr, columnName);
    } else {
      writeReference(pr, columnName, tableName + ".html", columnName);
    }
  }

  private void writeColumnReference(PrintWriter pr, String columnName) {
    writeReference(pr, columnName, null, columnName);
  }

  private void writeTableColumnReference(PrintWriter pr, SqlFullQualifiedColumnName columnName) {
    if (columnName.getTable() == null) {
      writeColumnReference(pr, columnName.getColumn());
    } else {
      writeTableReference(pr, columnName.getTable());
      writeText(pr, ".");
      writeTableColumnReference(pr, columnName.getTable(), columnName.getColumn());
    }
  }

  private void writeTableReference(PrintWriter pr, String tableName) {
    var originalTableName = resolveTableAlias(tableName);
    writeReference(pr, tableName, originalTableName + ".html", null);
  }

  private void writeReference(PrintWriter pr, String text, String file, String anchor) {
    String ref = "";
    if (file != null) {
      ref = file;
    }
    if (anchor != null) {
      ref += "#" + anchor;
    }
    writeStartTag(pr, "a", "href", ref);
    writeText(pr, text);
    writeEndTags(pr, 1);
  }

  private void writeTriggers(PrintWriter pr, SqlTable table) {
    if (table.getTriggers().size() > 0) {
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
      for (SqlTrigger trigger : table.getTriggers()) {
        writeNewRow(pr);
        writeNewColumn(pr);
        writeAnchor(pr, trigger.getId());
        writeText(pr, trigger.getId());
        writeEndTag(pr);
        writeNewColumn(pr);
        writeTableReference(pr, trigger.getTableName());
        writeEndTag(pr);
        writeNewColumn(pr);
        for (SqlDmlStatement stmt : trigger.getStatementsForEachRow()) {
          writeDmlStatement(pr, stmt);
          writeTag(pr, "br");
          writeTag(pr, "br");
        }
        writeEndTag(pr);
        writeNewColumn(pr);
        for (SqlDmlStatement stmt : trigger.getStatementsForEachStatement()) {
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

  private void writeDmlStatement(PrintWriter pr, SqlDmlStatement stmt) {
    if (stmt instanceof SqlUpdate) {
      writeUpdateStatement(pr, (SqlUpdate) stmt);
    }
  }

  private void writeUpdateStatement(PrintWriter pr, SqlUpdate stmt) {
    String table;
    boolean first = true;
    table = stmt.getTable();
    writeText(pr, "UPDATE ");
    if (table != null) {
      writeTableReference(pr, table);
    }
    writeText(pr, "SET ");
    for (SqlUpdateColumnExpression columnExpr : stmt.getColumnExpressions()) {
      if (!first) {
        writeText(pr, ", ");
      }
      first = false;
      writeTableColumnReference(pr, table, columnExpr.getColumnName());
      writeText(pr, "=");
      writeAtom(pr, columnExpr.getExpression());
    }
    if (stmt.getFilterExpression() != null) {
      writeText(pr, " WHERE ");
      writeSimpleExpr(pr, stmt.getFilterExpression());
    }
  }

  private void writeIndexes(PrintWriter pr, SqlTable table) {
    if (table.getIndexes().size() > 0) {
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
      for (SqlIndex index : table.getIndexes()) {
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

  private void writeConstraints(PrintWriter pr, SqlTable table) {
    if (table.getUniqueConstraints().size() > 0) {
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
      for (SqlUniqueConstraint unique : table.getUniqueConstraints()) {
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

  private void writeColumns(PrintWriter pr, SqlTable table) {
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
    for (var column : table.getColumns()) {
      writeNewRow(pr);
      writeNewColumn(pr);
      writeAnchor(pr, column.getId());
      writeText(pr, isPrimaryKeyColumn(table, column) ? "true" : "false");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getId());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getDataType().toString());
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getLength() > 0 ? "" + column.getDataType().getLength() : "");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, column.getDataType().getPrecision() > 0 ? "" + column.getDataType().getPrecision() : "");
      writeEndTag(pr);
      writeNewColumn(pr);
      writeText(pr, "" + column.isCanBeNull());
      writeEndTag(pr);
      writeNewColumn(pr);
      if (column.getDefaultValue() != null) {
        String defaultValue;
        defaultValue = column.getDefaultValue().toString();
        if (column.getDefaultValue().getValue() instanceof String) {
          defaultValue = "'" + defaultValue + "'";
        } else {
          writeText(pr, "" + defaultValue);
        }
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      if (column.getReference() != null) {
        writeStartTag(pr, "a", "href", column.getReference().getForeignTable() + ".html#"
                + column.getReference().getForeignColumn());
        writeText(pr, column.getReference().getForeignTable() + "(" + column.getReference().getForeignColumn()
                + ")");
        writeEndTag(pr);
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      if ((column.getReference() != null) && (column.getReference().getForeignKeyAction() != null)) {
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

  private void writeSystemDatabaseHintRefrences(PrintWriter pr, SqlObject object) {
    var systemDatabases = new TreeSet<String>();
    boolean first = true;
    for (SqlDatabaseSystemHints hints : object.getDatabaseManagementSystemHints()) {
      if (!hints.isEmpty()) {
        systemDatabases.add(hints.getDatabaseManagementSystem());
      }
    }
    for (String systemDatabase : systemDatabases) {
      if (!first) {
        writeText(pr, ", ");
      }
      first = false;
      writeReference(pr, systemDatabase, null, systemDatabase + "_" + object.getId());
    }
  }

  private void writeAnchor(PrintWriter pr, String anchor) {
    writeStartTag(pr, "a", "name", anchor);
    writeEndTag(pr);
  }

  private void writeSelectTables(PrintWriter pr, SqlMeta metaDefinition, SqlSelect select) {
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
    for (SqlJoinTable table : select.getJoinTables()) {
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
      if (table.getJoinCondition() != null) {
        writeSimpleExpr(pr, table.getJoinCondition());
      }
      writeEndTag(pr);
      writeNewColumn(pr);
      SqlTable joinTable = metaDefinition.findTable(table.getTable().getName());
      if (joinTable == null) {
        throw new MetaException("Could not find table " + table.getTable().getName());
      }
      writeComment(pr, joinTable.getComment());
      writeEndTags(pr, 2);
    }
    writeEndTag(pr);
  }

  private void writeSelectCondition(PrintWriter pr, SqlSelect select) {
    if (select.getCondition() != null) {
      writeStartTag(pr, "h3");
      writeText(pr, "Condition");
      writeEndTag(pr);
      writeStartTag(pr, "p");
      writeSimpleExpr(pr, select.getCondition());
      writeEndTag(pr);
    }
  }

  private void writeSimpleExpr(PrintWriter pr, SqlSimpleExpr simpleExpr) {
    if (simpleExpr instanceof SqlBinaryRelation) {
      SqlBinaryRelation binaryExpr;
      binaryExpr = (SqlBinaryRelation) simpleExpr;
      writeAtom(pr, binaryExpr.getFirst());
      writeText(pr, binaryExpr.getOperator());
      writeAtom(pr, binaryExpr.getSecond());
    } else if (simpleExpr instanceof SqlLogicalExpression) {
      SqlLogicalExpression logicalExpr;
      logicalExpr = (SqlLogicalExpression) simpleExpr;
      writeSimpleExpr(pr, logicalExpr.getFirst());
      writeText(pr, logicalExpr.getOperator());
      writeSimpleExpr(pr, logicalExpr.getSecond());
    } else if (simpleExpr instanceof SqlNot) {
      writeText(pr, "NOT ");
      writeSimpleExpr(pr, ((SqlNot) simpleExpr).getExpression());
    } else if (simpleExpr instanceof SqlParent) {
      writeText(pr, "(");
      writeSimpleExpr(pr, ((SqlParent) simpleExpr).getExpression());
      writeText(pr, ")");
    } else {
      writeText(pr, simpleExpr.toString());
    }
  }

  private void writeViewColumns(PrintWriter pr, SqlView view) {
    writeStartTag(pr, "h2");
    writeText(pr, "Columns");
    writeEndTag(pr);
    writeStartTag(pr, "table");
    writeNewRow(pr);
    writeStartTag(pr, "th");
    writeText(pr, "Name");
    writeEndTag(pr);
    for (int pos = 1; pos <= view.getSelects().size(); pos++) {
      writeStartTag(pr, "th");
      if (view.getSelects().size() > 1) {
        writeText(pr, "Expression on Select " + pos);
      } else {
        writeText(pr, "Expression");
      }
      writeEndTag(pr);
    }
    writeStartTag(pr, "th");
    writeText(pr, "Comment");
    writeEndTags(pr, 2);
    int col = 0;
    for (SqlViewColumn column : view.getColumns()) {
      writeStartTag(pr, "tr");
      writeNewColumn(pr);
      writeAnchor(pr, column.getId());
      writeText(pr, column.getId());
      writeEndTag(pr);
      for (SqlSelect select : view.getSelects()) {
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

  private void writeAtom(PrintWriter pr, SqlAtom atom) {
    if (atom instanceof SqlCaseExpr) {
      SqlCaseExpr caseExpr = (SqlCaseExpr) atom;
      writeText(pr, "CASE ");
      writeTableColumnReference(pr, caseExpr.getColumnName());
      for (SqlWhenThen whenThen : caseExpr.getWhenThenList()) {
        writeText(pr, " WHEN ");
        writeText(pr, whenThen.getLiteral().toString());
        writeText(pr, " THEN ");
        writeTableColumnReference(pr, whenThen.getColumnName());
      }
    }
    if (atom instanceof SqlComplexCaseExpr) {
      SqlComplexCaseExpr caseExpr = (SqlComplexCaseExpr) atom;
      writeText(pr, "CASE ");
      writeTag(pr, "br");
      for (SqlComplexWhenThen whenThen : caseExpr.getWhenThenList()) {
        writeText(pr, " WHEN ");
        writeSimpleExpr(pr, whenThen.getCondition());
        writeText(pr, " THEN ");
        writeAtom(pr, whenThen.getAction());
        writeTag(pr, "br");
      }
      if (caseExpr.getElseAction() != null) {
        writeText(pr, " ELSE ");
        writeAtom(pr, caseExpr.getElseAction());
        writeTag(pr, "br");
      }
      writeText(pr, "END");
    } else if (atom instanceof SqlFullQualifiedColumnName) {
      SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName) atom;
      writeTableColumnReference(pr, columnName);
    } else if (atom instanceof SqlFunction) {
      SqlFunction function = (SqlFunction) atom;
      writeText(pr, function.getName());
      writeText(pr, "(");
      boolean first = true;
      for (SqlAtom argument : function.getArguments()) {
        if (!first) {
          writeText(pr, ", ");
        }
        first = false;
        writeAtom(pr, argument);
      }
      writeText(pr, ")");
    } else {
      writeText(pr, atom.toString());
    }
  }

  private void writeColumnList(PrintWriter pr, List<String> list) {
    boolean first = true;
    writeSpaces(pr);
    for (String column : list) {
      if (!first) {
        pr.append(", ");
      }
      first = false;
      writeColumnReference(pr, column);
    }
  }

  private boolean isPrimaryKeyColumn(SqlTable table, SqlTableColumn column) {
    if (table.getPrimaryKey() != null) {
      return table.getPrimaryKey().getPrimaryKeyColumns().contains(column.getId());
    }
    return false;
  }

  private void writeOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    Set<String> databaseSystems;
    try (var pr = new NewLinePrintWriter(new File(outputDir, "index.html"))) {
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
      for (String databaseSystem : databaseSystems) {
        writeTag(pr, "br");
        writeReference(pr, databaseSystem, databaseSystem + ".html", null);
      }
      writeEndTags(pr, 3);
    }
    writeTableOverview(metaDefinition);
    writeViewOverview(metaDefinition);
    writeColumnOverview(metaDefinition);
    writePrimaryKeyOverview(metaDefinition);
    writeForeignKeyOverview(metaDefinition);
    writeIndexOverview(metaDefinition);
    writeConstraintsOverview(metaDefinition);
    writeTriggerOverview(metaDefinition);
    for (var databaseSystem : databaseSystems) {
      writeDatabaseSystemOverview(metaDefinition, databaseSystem);
    }
  }

  private void writeDatabaseSystemOverview(SqlMeta metaDefinition, String databaseSystem)
          throws FileNotFoundException {
    Set<String> hints;
    try (var pr = new NewLinePrintWriter(new File(outputDir, databaseSystem + ".html"))) {
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
      for (String hint : hints) {
        writeStartTag(pr, "th");
        writeText(pr, hint);
        writeEndTag(pr);
      }
      writeEndTags(pr, 1);
      for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
        for (SqlObject object : getAllSqlObjectsWithDatabaseSystemHints(databaseSystem, table)) {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, table.getId(), table.getId() + ".html", null);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeReference(pr, object.getId(), table.getId() + ".html", databaseSystem + "_" + object.getId());
          writeEndTag(pr);
          for (String hint : hints) {
            writeNewColumn(pr);
            if (object.getDatabaseManagementSystemHints(databaseSystem).isHintSet(hint)) {
              if (object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint) != null) {
                writeText(pr, object.getDatabaseManagementSystemHints(databaseSystem).getHintValue(hint));
              } else {
                writeText(pr, "true");
              }
            } else {
              writeText(pr, "");
            }
            writeEndTag(pr);
          }
          writeEndTag(pr);
        }
      }
      writeEndTags(pr, 3);
    }
  }

  private Set<String> getAllDatabaseSystemHints(String databaseSystem, SqlMeta metaDefinition) {
    var hints = new TreeSet<String>();
    for (var table : metaDefinition.getArtifacts(SqlTable.class)) {
      hints.addAll(getAllDatabaseSystemHints(databaseSystem, table));
    }
    return hints;
  }

  private Set<String> getAllDatabaseSystemsWithDatabaseSystemHints(SqlMeta metaDefinition) {
    var databaseSystems = new TreeSet<String>();
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
      databaseSystems.addAll(getAllDatabaseSystemsWithDatabaseSystemHints(table));
    }
    return databaseSystems;
  }

  private void writeForeignKeyOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    Map<String, List<SqlTable>> foreignKeys = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> foreignKeysSorted;
    SqlForeignKey foreignKey;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
      for (SqlForeignKey foreign : table.getForeignKeys()) {
        references = foreignKeys.get(foreign.getId());
        if (references == null) {
          references = new ArrayList<SqlTable>();
          foreignKeys.put(foreign.getId(), references);
        }
        references.add(table);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "foreignkeys.html"))) {
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
      for (String foreignKeyName : foreignKeysSorted) {
        for (SqlTable table : foreignKeys.get(foreignKeyName)) {
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
          writeTableColumnReference(pr, foreignKey.getReference().getForeignTable(),
                  foreignKey.getReference().getForeignColumn());
          writeText(pr, ")");
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
  }

  private void writePrimaryKeyOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    var primaryKeys = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> primaryKeySorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
      if (table.getPrimaryKey() != null) {
        references = primaryKeys.get(table.getPrimaryKey().getId());
        if (references == null) {
          references = new ArrayList<SqlTable>();
          primaryKeys.put(table.getPrimaryKey().getId(), references);
        }
        references.add(table);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "primarykeys.html"))) {
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
      for (String primaryKeyName : primaryKeySorted) {
        for (SqlTable table : primaryKeys.get(primaryKeyName)) {
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
  }

  private void writeIndexOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    Map<String, List<SqlTable>> indexes = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> indexSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
      for (SqlIndex index : table.getIndexes()) {
        references = indexes.get(index.getId());
        if (references == null) {
          references = new ArrayList<SqlTable>();
          indexes.put(index.getId(), references);
        }
        references.add(table);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "indexes.html"))) {
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
      for (String indexName : indexSorted) {
        for (SqlTable table : indexes.get(indexName)) {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, indexName, table.getId() + ".html", indexName);
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
  }

  private void writeConstraintsOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    Map<String, List<SqlTable>> constraints = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> constraintsSorted;
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class)) {
      for (SqlUniqueConstraint constraint : table.getUniqueConstraints()) {
        references = constraints.get(constraint.getId());
        if (references == null) {
          references = new ArrayList<SqlTable>();
          constraints.put(constraint.getId(), references);
        }
        references.add(table);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "constraints.html"))) {
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
      for (String constraintName : constraintsSorted) {
        for (SqlTable table : constraints.get(constraintName)) {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, constraintName, table.getId() + ".html", constraintName);
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
  }

  private void writeTriggerOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    Map<String, List<SqlTable>> triggers = new HashMap<String, List<SqlTable>>();
    List<SqlTable> references;
    SortedSet<String> triggersSorted;
    for (var table : metaDefinition.getArtifacts(SqlTable.class)) {
      for (var trigger : table.getTriggers()) {
        references = triggers.get(trigger.getId());
        if (references == null) {
          references = new ArrayList<SqlTable>();
          triggers.put(trigger.getId(), references);
        }
        references.add(table);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "triggers.html"))) {
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
      for (String triggerName : triggersSorted) {
        for (SqlTable table : triggers.get(triggerName)) {
          writeNewRow(pr);
          writeNewColumn(pr);
          writeReference(pr, triggerName, table.getId() + ".html", triggerName);
          writeEndTag(pr);
          writeNewColumn(pr);
          writeText(pr, "DELETE ON ");
          writeTableReference(pr, table.findTrigger(triggerName).getTableName());
          writeEndTags(pr, 2);
        }
      }
      writeEndTags(pr, 3);
    }
  }

  private void writeColumnOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    var columns = new HashMap<String, List<SqlObject>>();
    List<SqlObject> references;
    SortedSet<String> columnsSorted;
    for (var table : metaDefinition.getArtifacts(SqlTable.class)) {
      for (var column : table.getColumns()) {
        references = columns.get(column.getId());
        if (references == null) {
          references = new ArrayList<SqlObject>();
          columns.put(column.getId(), references);
        }
        references.add(table);
      }
    }
    for (var view : metaDefinition.getArtifacts(SqlView.class)) {
      for (var column : view.getColumns()) {
        references = columns.get(column.getId());
        if (references == null) {
          references = new ArrayList<SqlObject>();
          columns.put(column.getId(), references);
        }
        references.add(view);
      }
    }
    try (var pr = new NewLinePrintWriter(new File(outputDir, "columns.html"))) {
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
      for (String columnName : columnsSorted) {
        for (SqlObject object : columns.get(columnName)) {
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
  }

  private void writeViewOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    try (var pr = new NewLinePrintWriter(new File(outputDir, "views.html"))) {
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
      for (SqlView view : metaDefinition.getSqlObjects(SqlView.class)) {
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
  }

  private void writeTableOverview(SqlMeta metaDefinition) throws FileNotFoundException {
    try (var pr = new NewLinePrintWriter(new File(outputDir, "tables.html"))) {
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
      for (SqlTable table : metaDefinition.getSqlObjects(SqlTable.class)) {
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
  }

  private void writeComment(PrintWriter pr, String comment) {
    int pos;
    String tag;
    String cmt;
    cmt = comment;
    pos = cmt.indexOf("{@");
    while (pos >= 0) {
      writeText(pr, cmt.substring(0, pos));
      cmt = cmt.substring(pos);
      pos = cmt.indexOf('}');
      if (pos >= 0) {
        tag = cmt.substring(0, pos);
        cmt = cmt.substring(pos + 1);
        if (tag.startsWith("{@ref")) {
          tag = tag.substring("{@ref".length());
          pos = tag.indexOf('.');
          if (pos >= 0) {
            writeReference(pr, tag.substring(pos).trim(), tag.substring(0, pos).trim() + ".html",
                    tag.substring(pos).trim());
          } else {
            writeReference(pr, tag.trim(), tag.trim() + ".html", null);
          }
        } else {
          throw new MetaException("Unknown comment tag " + tag);
        }
      } else {
        throw new MetaException("Comment tag not finished correctly. Missing '}' in comment:\n" + comment);
      }
      pos = cmt.indexOf("{@");
    }
    writeText(pr, cmt);
  }

  private void writeEndTags(PrintWriter pr, int endTags) {
    for (int endTag = 0; endTag < endTags; endTag++) {
      writeEndTag(pr);
    }
  }

  private void writeEndTag(PrintWriter pr) {
    var tag = htmlTags.pop();
    writeSpaces(pr);
    pr.append("</");
    pr.append(tag);
    pr.append('>');
    pr.append("\n");
  }

  private void writeText(PrintWriter pr, String text) {
    writeSpaces(pr);
    if (text != null) {
      pr.append(text);
    }
    pr.append('\n');
  }

  private void writeStartTag(PrintWriter pr, String tag) {
    writeSpaces(pr);
    pr.append('<');
    pr.append(tag);
    pr.append('>');
    pr.append("\n");
    htmlTags.push(tag);
  }

  private void writeSpaces(PrintWriter pr) {
    for (int pos = 0; pos < htmlTags.size(); pos++) {
      pr.append("  ");
    }
  }

  private void writeHeader(PrintWriter pr, String title) {
    // Reset row count so that even and row columns so that a document always
    // start with an event column.
    // This ensures that the generated html does not change if nothing has
    // changed in the meta data for the given document.
    // This is important for source control systems
    fRow = 0;
    pr.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
    writeStartTag(pr, "html");
    writeStartTag(pr, "head");
    writeTag(pr, "meta", "http-equiv", "content-type", "content", "text/html", "charset", "ISO-8859-1");
    writeTag(pr, "link", "rel", "STYLESHEET", "type", "text/css", "href", "style.css");
    writeStartTag(pr, "title");
    writeText(pr, title);
    writeEndTags(pr, 2);
  }

  private void writeTag(PrintWriter pr, String tag, String... attributes) {
    writeTag(pr, tag, false, attributes);
  }

  private void writeTag(PrintWriter pr, String tag, boolean onlyStartTag, String[] attributes) {
    writeSpaces(pr);
    pr.append('<');
    pr.append(tag);
    if ((attributes != null) && (attributes.length > 0)) {
      for (int pos = 0; pos < attributes.length; pos = pos + 2) {
        pr.append(' ');
        pr.append(attributes[pos]);
        pr.append("=\"");
        pr.append(attributes[pos + 1]);
        pr.append("\"");
      }
    }
    if (!onlyStartTag) {
      pr.append('/');
    }
    pr.append('>');
    pr.append('\n');
  }

  private void writeStartTag(PrintWriter pr, String tag, String... attributes) {
    writeTag(pr, tag, true, attributes);
    htmlTags.push(tag);
  }

  @Override
  public void printHelp() {
    System.out.println("HtmlDocGenerator Options: -outputDir {directory}");
  }

  private String resolveTableAlias(String alias) {
    if (tableAliases.isEmpty()) {
      return alias;
    }
    Map<String, String> aliases = tableAliases.peek();
    String tableName = aliases.get(alias);
    if (tableName != null) {
      return tableName;
    }
    return alias;
  }

  private void pushTableAliases(Map<String, String> aliases) {
    tableAliases.push(aliases);
  }

  private void popTableAliases() {
    tableAliases.pop();
  }
}
