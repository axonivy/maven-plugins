package ch.ivyteam.db.meta.generator.internal.postgresql;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class PostgreSqlForeignKeys extends ForeignKeys
{
  public PostgreSqlForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    super(dbHints, delimiter, identifiers, comments);
  }

  @Override
  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,List<String> createdTemporaryStoredProcedures)
  {
    String foreignKeyName = table.getId()+"_"+StringUtils.removeStart(foreignKey.getId(), "FK_")+"_fkey";
    
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.println(" DROP");
    pr.print(" CONSTRAINT ");
    pr.println(foreignKeyName);
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }

  @Override
  public void generateAlterTableAdd(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey)
  {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" ADD FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }
}