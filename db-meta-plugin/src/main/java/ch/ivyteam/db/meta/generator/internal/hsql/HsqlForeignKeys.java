package ch.ivyteam.db.meta.generator.internal.hsql;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlTable;

final class HsqlForeignKeys extends ForeignKeys
{
  HsqlForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments)
  {
    super(dbHints, delimiter, identifiers, comments);
  }
  
  @Override
  public void generateAlterTableAdd(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey) throws MetaException
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

  @Override
  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    // do nothing
    // in HSQLDB we do not give names to the FOREIGN KEYs
    // therefore they can not be deleted
  }
}
