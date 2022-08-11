package ch.ivyteam.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import ch.ivyteam.ivy.persistence.IPersistentTransaction;
import ch.ivyteam.ivy.persistence.restricted.db.meta.KeyType;
import ch.ivyteam.ivy.persistence.restricted.db.meta.Table;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn.Type;
import ch.ivyteam.ivy.persistence.restricted.db.meta.TableColumn.Option;
import ch.ivyteam.ivy.persistence.restricted.db.meta.ViewColumn;
import ch.ivyteam.ivy.persistence.PersistencyException;
import ch.ivyteam.ivy.persistence.db.DatabasePersistencyService;
import ch.ivyteam.ivy.persistence.db.DatabaseTableClassPersistencyService;
import ch.ivyteam.db.sql.ColumnName;
import ch.ivyteam.data.CaseData;

public class DbCaseData extends DatabaseTableClassPersistencyService<CaseData> {

  public static final String TABLENAME = "IWA_Case";

  public static final String QUERY_TABLENAME = "IWA_CaseQuery";

  private static final String PRIMARY_KEY_COLUMN_NAME = "CaseId";
  public static final ColumnName PRIMARY_KEY_COLUMN = new ColumnName(TABLENAME, PRIMARY_KEY_COLUMN_NAME);

  private static final String COLUMN_NAME_CASE_ID = "CaseId";
  public static final ColumnName COLUMN_CASE_ID = new ColumnName(TABLENAME, COLUMN_NAME_CASE_ID);

  public DbCaseData(DatabasePersistencyService database) {
    super(database, 
      CaseData.class,
      new Table(
        TABLENAME, 
        KeyType.LONG,
        Arrays.asList( 
          new TableColumn(PRIMARY_KEY_COLUMN, Type.BIGINT, Option.PRIMARY_KEY)
        ),
        QUERY_TABLENAME,
        Arrays.asList( 
          new ViewColumn(QueryView.VIEW_COLUMN_CASE_ID),
          new ViewColumn(QueryView.VIEW_COLUMN_NAME),
          new ViewColumn(QueryView.VIEW_COLUMN_LANGUAGE_ID, ViewColumn.Option.MANDATORY_FILTER)
        )
      )
    ); 
  }

  @Override
  protected CaseData createObjectFromResultSet(IPersistentTransaction transaction, ResultSet result) throws PersistencyException, SQLException {
    return new CaseData(
      database.getLong(result, 1)
    );
  }

  @Override
  protected void writeDataToUpdateStatement(IPersistentTransaction transaction, CaseData data, PreparedStatement stmt) {
    database.setLong(stmt, 1, data.getCaseId(), PRIMARY_KEY_COLUMN);
  }

  @Override
  protected void writeDataToInsertStatement(IPersistentTransaction transaction, CaseData data, PreparedStatement stmt) {
    database.setLong(stmt, 1, data.getCaseId(), PRIMARY_KEY_COLUMN);
  }

  public static class QueryView {

    private static final String VIEW_COLUMN_NAME_CASE_ID = "CaseId";
    public static final ColumnName VIEW_COLUMN_CASE_ID = new ColumnName(QUERY_TABLENAME, VIEW_COLUMN_NAME_CASE_ID);

    private static final String VIEW_COLUMN_NAME_NAME = "Name";
    public static final ColumnName VIEW_COLUMN_NAME = new ColumnName(QUERY_TABLENAME, VIEW_COLUMN_NAME_NAME);

    private static final String VIEW_COLUMN_NAME_LANGUAGE_ID = "LanguageId";
    public static final ColumnName VIEW_COLUMN_LANGUAGE_ID = new ColumnName(QUERY_TABLENAME, VIEW_COLUMN_NAME_LANGUAGE_ID);
  }
}
