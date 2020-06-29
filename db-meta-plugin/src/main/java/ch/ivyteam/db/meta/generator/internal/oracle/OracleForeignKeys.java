package ch.ivyteam.db.meta.generator.internal.oracle;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class OracleForeignKeys extends ForeignKeys
{
  OracleForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    super(dbHints, delimiter, identifiers, comments);
  }

  @Override
  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          List<String> createdTemporaryStoredProcedures)
  {
    pr.println("DECLARE");
    pr.println("FK_NAME VARCHAR(30);");
    pr.println("BEGIN");
    pr.println("  SELECT UC.CONSTRAINT_NAME INTO FK_NAME");
    pr.println("  FROM USER_CONSTRAINTS UC INNER JOIN USER_CONS_COLUMNS UCC ON UC.CONSTRAINT_NAME = UCC.CONSTRAINT_NAME");
    pr.println("  WHERE UC.CONSTRAINT_TYPE='R' AND UC.TABLE_NAME='"+table.getId().toUpperCase()+"' AND UCC.COLUMN_NAME='"+foreignKey.getColumnName().toUpperCase()+"';");
    pr.println();
    pr.println("  EXECUTE IMMEDIATE 'ALTER TABLE "+table.getId()+" DROP CONSTRAINT ' || FK_NAME;");
    pr.println("END;");
    pr.println(";");
    pr.println();
  }
}