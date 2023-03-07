package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlReference;
import ch.ivyteam.db.meta.model.internal.SqlTable;

public class ForeignKeys {

  protected final DbHints dbHints;
  protected final Delimiter delimiter;
  protected final Identifiers identifiers;
  protected final Comments comments;
  protected final Spaces spaces = new Spaces();

  public ForeignKeys(DbHints dbHints, Delimiter delimiter, Identifiers identifiers, Comments comments) {
    this.dbHints = dbHints;
    this.delimiter = delimiter;
    this.identifiers = identifiers;
    this.comments = comments;
  }

  /**
   * Is foreign key supported. Subclasses that does not support foreign keys may
   * override this method an return false
   * @param foreignKey the foreign key
   * @return true if they are supported, false if not
   */
  public final boolean isSupported(SqlForeignKey foreignKey) {
    return !(dbHints.NO_REFERENCE_USE_TRIGGER.isSet(foreignKey) ||
            dbHints.NO_REFERENCE.isSet(foreignKey));
  }

  /**
   * Is foreign Key reference in column definition supported. If this method
   * returns true the method
   * {@link #generateReference(PrintWriter, SqlReference, SqlForeignKey)} will
   * be called during the column definition generation. If it returns false the
   * method will not be called instead the method
   * {@link #generate(PrintWriter, SqlForeignKey)} will be called after the
   * column generation.</br>
   * Subclasses may override this method if foreign key reference in column
   * definitions are not supported and instead foreign key should be declared
   * outside the column definition.
   * @return true if it supported, false if not
   * @see #generateReference(PrintWriter, SqlReference, SqlForeignKey)
   * @see #generate(PrintWriter, SqlForeignKey)
   */
  public boolean isReferenceInColumnDefinitionSupported() {
    return true;
  }

  /**
   * Gets the foreign key action of the given foreign key
   * @param foreignKey the foreign key
   * @return foreign key action
   * @throws MetaException
   */
  public final SqlForeignKeyAction getAction(SqlForeignKey foreignKey) {
    if (dbHints.REFERENCE_ACTION.isSet(foreignKey)) {
      for (SqlForeignKeyAction action : SqlForeignKeyAction.values()) {
        if (action.toString().equals(dbHints.REFERENCE_ACTION.value(foreignKey))) {
          return action;
        }
      }
      throw new MetaException("Unknown Foreign Key Action '" + dbHints.REFERENCE_ACTION.value(foreignKey));
    }
    return foreignKey.getReference().getForeignKeyAction();
  }

  /**
   * Generates the foreign key definition. This method is only called if method
   * {@link #isReferenceInColumnDefinitionSupported()} returns false.
   * @param pr the print writer
   * @param foreignKey the foreign key to generate
   * @throws MetaException
   */
  final void generate(PrintWriter pr, SqlForeignKey foreignKey) {
    spaces.generate(pr, 2);
    pr.print("FOREIGN KEY (");
    identifiers.generate(pr, foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
  }

  /**
   * Generates a reference
   * @param pr the writer
   * @param reference the reference
   * @param foreignKey The sql foreign key the reference is defined on
   * @throws MetaException
   */
  protected final void generateReference(PrintWriter pr, SqlReference reference, SqlForeignKey foreignKey) {
    pr.append(" REFERENCES ");
    pr.append(getForeignTable(reference, foreignKey));
    pr.append('(');
    pr.append(reference.getForeignColumn());
    pr.append(')');
    if ((getAction(foreignKey) != null) &&
            (!dbHints.NO_ACTION_USE_TRIGGER.isSet(foreignKey)) &&
            (!dbHints.NO_ACTION.isSet(foreignKey))) {
      switch (getAction(foreignKey)) {
        case ON_DELETE_CASCADE:
          pr.append(' ');
          pr.append("ON DELETE CASCADE");
          break;
        case ON_DELETE_SET_NULL:
          pr.append(' ');
          pr.append("ON DELETE SET NULL");
          break;
        case ON_DELETE_THIS_CASCADE:
          // do not generate this action because sql does not offer it.
          // this action must be implemented with triggers!
          break;
      }
    }
  }

  /**
   * Gets the foreign table of a reference
   * @param reference the reference
   * @param artifact the artifact the reference was declared on
   * @return foreign table
   */
  private String getForeignTable(SqlReference reference, SqlArtifact artifact) {
    return dbHints.FOREIGN_TABLE.valueIfSet(artifact)
            .orElse(reference.getForeignTable());
  }

  /**
   * Generates an alter table add foreign key statement
   * @param pr
   * @param table the table
   * @param foreignKey the foreign key
   * @throws MetaException
   */
  public void generateAlterTableAdd(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey) {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.println(" ADD");
    pr.println("(");
    pr.print(" FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    pr.println();
    pr.print(")");
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }

  public void generateAlterTableDrop(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          @SuppressWarnings("unused") List<String> createdTemporaryStoredProcedures) {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" DROP FOREIGN KEY ");
    pr.print(foreignKey.getId());
    delimiter.generate(pr);
    pr.println();
    pr.println();
  }

  final boolean shouldGenerateTrigger(SqlForeignKey foreignKey) {
    return getAction(foreignKey) != null &&
            ((!isSupported(foreignKey) && !dbHints.NO_REFERENCE.isSet(foreignKey)) ||
                    dbHints.NO_ACTION_USE_TRIGGER.isSet(foreignKey));
  }
}
