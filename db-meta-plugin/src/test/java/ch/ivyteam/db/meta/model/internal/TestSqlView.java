package ch.ivyteam.db.meta.model.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class TestSqlView {

  private SqlView sqlView;
  private SqlView sqlViewWithoutColumns;
  private List<SqlViewColumn> columns;
  private SqlViewColumn column1;
  private SqlViewColumn column2;

  @Before
  public void setUp() {
    column1 = new SqlViewColumn("column1", new ArrayList<>(), "comment1");
    column2 = new SqlViewColumn("column2", new ArrayList<>(), "comment2");
    columns = new ArrayList<>();
    columns.add(column1);
    columns.add(column2);
    sqlView = new SqlView("id", columns, new ArrayList<>(), new ArrayList<>(), "commentview");
    sqlViewWithoutColumns = new SqlView("id", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            "commentview");
  }

  @Test
  public void hasColumnExisting() {
    assertThat(sqlView.hasColumn("column1")).isTrue();
    assertThat(sqlView.hasColumn("column2")).isTrue();
  }

  @Test
  public void hasColumnNonExisting() {
    assertThat(sqlView.hasColumn("nonexistingcolumn")).isFalse();
    assertThat(sqlView.hasColumn("")).isFalse();
    assertThat(sqlView.hasColumn(null)).isFalse();
    assertThat(sqlViewWithoutColumns.hasColumn("nonexistingcolumn")).isFalse();
  }

  @Test
  public void findColumnExisting() {
    assertThat(sqlView.findColumn("column1").getId()).isEqualTo("column1");
    assertThat(sqlView.findColumn("column2").getId()).isEqualTo("column2");
  }

  @Test
  public void findColumnNonExisting() {
    assertThat(sqlView.findColumn("nonexistingcolumn")).isNull();
    assertThat(sqlView.findColumn("")).isNull();
    assertThat(sqlView.findColumn(null)).isNull();
    assertThat(sqlViewWithoutColumns.findColumn("nonexistingcolumn")).isNull();
  }

  @Test
  public void testEquals() {
    Set<SqlView> views = new HashSet<>();
    views.add(new SqlView("id", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "comment"));
    assertThat(views).hasSize(1);
    views.add(new SqlView("id", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "comment"));
    assertThat(views).hasSize(1);
    views.add(new SqlView("id2", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "comment"));
    assertThat(views).hasSize(2);
  }
}
