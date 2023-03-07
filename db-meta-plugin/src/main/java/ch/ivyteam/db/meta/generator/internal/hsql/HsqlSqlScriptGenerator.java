package ch.ivyteam.db.meta.generator.internal.hsql;

import java.io.PrintWriter;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.GenerateAlterTableUtil;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

public class HsqlSqlScriptGenerator extends SqlScriptGenerator {

  public static final String HSQL_DB = "HsqlDb";

  public HsqlSqlScriptGenerator() {
    super(HSQL_DB, Delimiter.STANDARD, new Identifiers(Identifiers.STANDARD_QUOTE, true), Comments.STANDARD);
  }

  @Override
  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts,
          ForeignKeys fKeys) {
    return new HsqlTriggers(hints, delim, dmlStmts, fKeys);
  }

  @Override
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident,
          Comments cmmnts) {
    return new HsqlForeignKeys(hints, delim, ident, cmmnts);
  }

  @Override
  public String dbName() {
    return "HsqlDb";
  }

  @Override
  protected boolean isIndexInTableSupported() {
    return false;
  }

  @Override
  protected boolean isNullBeforeDefaultConstraint() {
    return false;
  }

  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable,
          SqlTableColumn oldColumn) {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn,
            newTable, "ALTER COLUMN");
  }

  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable) {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }

  @Override
  public RecreateOptions getRecreateOptions() {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
}
