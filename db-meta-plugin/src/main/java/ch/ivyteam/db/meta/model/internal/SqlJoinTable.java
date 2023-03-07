package ch.ivyteam.db.meta.model.internal;

public class SqlJoinTable {

  private SqlTableId table;
  private String joinKind;
  private SqlSimpleExpr joinCondition;

  public SqlJoinTable(SqlTableId table) {
    this(table, null, null);
  }

  public SqlJoinTable(SqlTableId table, String joinKind, SqlSimpleExpr joinCondition) {
    this.table = table;
    this.joinKind = joinKind;
    this.joinCondition = joinCondition;
  }

  public SqlTableId getTable() {
    return table;
  }

  public String getJoinKind() {
    return joinKind;
  }

  public SqlSimpleExpr getJoinCondition() {
    return joinCondition;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (joinKind != null) {
      builder.append(joinKind);
      builder.append(" ");
    }
    builder.append(table);
    if (joinCondition != null) {
      builder.append(" ON ");
      builder.append(joinCondition);
    }
    return builder.toString();
  }
}
