package ch.ivyteam.db.meta.model.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class TestSqlObject {

  private SqlObject sqlObject1;
  private SqlObject sqlObject2;
  private SqlObject sqlObject3;
  private SqlObject sqlObject4;

  @Before
  public void setUp() {
    sqlObject1 = new SqlObject("case", null, null);
    sqlObject2 = new SqlObject("case", null, null);
    sqlObject3 = new SqlObject("task", null, null);
    sqlObject4 = new SqlObject("", null, null);
  }

  @Test
  public void testEquals() {
    assertThat(sqlObject1).isEqualTo(sqlObject1);
    assertThat(sqlObject1).isEqualTo(sqlObject2);
    assertThat(sqlObject1).isNotEqualTo(sqlObject3);
    assertThat(sqlObject1).isNotEqualTo(sqlObject4);
    assertThat(sqlObject1).isNotEqualTo(null);
    assertThat(sqlObject1).isNotEqualTo(Integer.valueOf(1));
    SqlView view = new SqlView("id", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "");
    SqlTable table = new SqlTable("id", new ArrayList<>(), new ArrayList<>(), "");
    assertThat(view).isNotEqualTo(table);
  }

  @Test
  public void testHashCode() {
    assertThat(sqlObject1.hashCode()).isEqualTo(sqlObject1.hashCode());
    assertThat(sqlObject1.hashCode()).isEqualTo(sqlObject2.hashCode());
  }
}
