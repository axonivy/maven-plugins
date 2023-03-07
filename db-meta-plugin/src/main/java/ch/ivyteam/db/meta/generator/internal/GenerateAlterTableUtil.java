package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * @author fs
 * @since 02.12.2011
 */
public class GenerateAlterTableUtil {

  private GenerateAlterTableUtil() {}

  public static void generateAlterTableAlterColumnType(PrintWriter pr, SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String tableChangeTag, String columnChangeTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, newTable.getId());
    pr.print(" ");
    pr.print(tableChangeTag);
    pr.print(" ");
    generator.identifiers.generate(pr, newColumn.getId());
    pr.print(" ");
    pr.print(columnChangeTag);
    pr.print(" ");
    generator.generateDataType(pr, newColumn.getDataType(), newColumn);
    generator.delimiter.generate(pr);
  }

  public static void generateAlterTableAlterColumnNotNull(PrintWriter pr, SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String tableChangeTag, String setNotNullTag,
          String dropNotNullTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, newTable.getId());
    pr.print(" ");
    pr.print(tableChangeTag);
    pr.print(" ");
    generator.identifiers.generate(pr, newColumn.getId());
    pr.print(" ");
    if (newColumn.isCanBeNull()) {
      pr.print(dropNotNullTag);
    } else {
      pr.print(setNotNullTag);
    }
    generator.delimiter.generate(pr);
  }

  public static void generateAlterTableAlterColumnDefault(PrintWriter pr,
          SqlScriptGenerator generator, SqlTableColumn newColumn,
          SqlTable newTable, String tableChangeTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, newTable.getId());
    pr.print(" ");
    pr.print(tableChangeTag);
    pr.print(" ");
    generator.identifiers.generate(pr, newColumn.getId());
    if (newColumn.getDefaultValue() == null) {
      pr.print(" DROP DEFAULT");
    } else {
      pr.print(" SET");
      generator.generateDefaultValue(pr, newColumn);
    }
    generator.delimiter.generate(pr);
  }

  public static void generateAlterTableAlterColumnWithNullConstraints(PrintWriter pr,
          SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String changeColumnTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, newTable.getId());
    pr.print(" ");
    pr.print(changeColumnTag);
    pr.print(" ");
    generator.identifiers.generate(pr, newColumn.getId());
    pr.print(" ");
    generator.generateDataType(pr, newColumn.getDataType(), newColumn);
    boolean defaultWillBeSetAfterAltering = generator.getRecreateOptions().defaultConstraints;
    if (!defaultWillBeSetAfterAltering) {
      generator.generateDefaultValue(pr, newColumn);
    }
    generator.generateNullConstraint(pr, newColumn.isCanBeNull(), newColumn);
    generator.delimiter.generate(pr);
  }

  public static void generateAlterTableChangeColumnWithDefaultAndNullConstraints(PrintWriter pr,
          SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String changeTag) throws MetaException {
    generateAlterTableForColumn(pr, generator, newColumn, newTable, changeTag);
  }

  public static void generateAlterTableAddColumn(PrintWriter pr, SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String changeTag) throws MetaException {
    generateAlterTableForColumn(pr, generator, newColumn, newTable, changeTag);
  }

  private static void generateAlterTableForColumn(PrintWriter pr, SqlScriptGenerator generator,
          SqlTableColumn newColumn, SqlTable newTable, String changeTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, newTable.getId());
    pr.print(" ");
    pr.println(changeTag);
    generator.generateColumn(pr, newTable, newColumn, new LinkedHashMap<SqlTable, List<SqlForeignKey>>());
    generator.delimiter.generate(pr);
  }

  public static void generateAlterTableDropColumn(PrintWriter pr, SqlScriptGenerator generator,
          SqlTable table, SqlTableColumn columnToDrop) {
    generateAlterTableDropColumn(pr, generator, table, columnToDrop, "DROP COLUMN");
  }

  public static void generateAlterTableDropColumn(PrintWriter pr,
          SqlScriptGenerator generator, SqlTable table,
          SqlTableColumn columnToDrop, String dropTag) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, table.getId());
    pr.print(" ");
    pr.print(dropTag);
    pr.print(" ");
    pr.print(columnToDrop.getId());
    generator.delimiter.generate(pr);
    pr.println();
  }

  public static void renameColumn(PrintWriter pr,
          SqlScriptGenerator generator, SqlTable table, SqlTableColumn oldColumn,
          SqlTableColumn newColumn) {
    pr.print("ALTER TABLE ");
    generator.identifiers.generate(pr, table.getId());
    pr.print(" ");
    pr.print("RENAME COLUMN");
    pr.print(" ");
    pr.print(oldColumn.getId());
    pr.print(" TO ");
    pr.print(newColumn.getId());
    generator.delimiter.generate(pr);
    pr.println();
  }
}
