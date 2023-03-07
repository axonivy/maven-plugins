package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlTableId;

public class Identifiers {

  private final String quote;
  private final boolean upperCaseQuoted;
  private final List<String> keywords;
  public static final String STANDARD_QUOTE = "\"";
  public static final Identifiers STANDARD = new Identifiers(STANDARD_QUOTE, false);

  public Identifiers(String quote, boolean upperCaseQuoted) {
    this(quote, upperCaseQuoted, Collections.emptyList());
  }

  public Identifiers(String quote, boolean upperCaseQuoted, List<String> additionalKeywords) {
    this(quote, upperCaseQuoted, additionalKeywords, Collections.emptyList());
  }

  public Identifiers(String quote, boolean upperCaseQuoted, List<String> additionalKeywords,
          List<String> nonKeywords) {
    this.quote = quote;
    this.upperCaseQuoted = upperCaseQuoted;
    this.keywords = new ArrayList<>(ReservedSqlKeywords.get());
    this.keywords.addAll(additionalKeywords);
    this.keywords.removeAll(nonKeywords);
  }

  /**
   * Generates an identifier
   * @param pr the print writer
   * @param identifier the identifier
   */
  public final void generate(PrintWriter pr, String identifier) {
    if (isReservedSqlKeyword(identifier)) {
      pr.print(quote);
      if (upperCaseQuoted) {
        identifier = identifier.toUpperCase();
      }
      pr.print(identifier);
      pr.print(quote);
    } else {
      pr.print(identifier);
    }
  }

  /**
   * Generates a full qualified column name
   * @param pr
   * @param fullQualifiedColumnName
   */
  final void generateFullQualifiedColumnName(PrintWriter pr,
          SqlFullQualifiedColumnName fullQualifiedColumnName) {
    if (fullQualifiedColumnName.getTable() != null) {
      generate(pr, fullQualifiedColumnName.getTable());
      pr.print('.');
    }
    generate(pr, fullQualifiedColumnName.getColumn());
  }

  final void generateSqlTableId(PrintWriter pr, SqlTableId tableId) {
    generate(pr, tableId.getName());
    if (tableId.getAlias() != null) {
      generateAs(pr);
      generate(pr, tableId.getAlias());
    }
  }

  protected void generateAs(PrintWriter pr) {
    pr.print(" AS ");
  }

  /**
   * Checks if the given identifier is a reserved sql keyword
   * @param identifier the identifer to check
   * @return true is it is a reserved sql word, otherwise false
   */
  private boolean isReservedSqlKeyword(String identifier) {
    identifier = identifier.toUpperCase();
    return keywords.contains(identifier);
  }

  SqlFullQualifiedColumnName replaceTriggerVariable(SqlFullQualifiedColumnName columnName,
          String triggerVariable) {
    String column = columnName.getColumn();
    if (isReservedSqlKeyword(column)) {
      column = quote + column + quote;
    }
    return new SqlFullQualifiedColumnName(null, triggerVariable + "." + column);
  }
}
