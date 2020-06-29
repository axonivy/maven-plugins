package ch.ivyteam.db.meta.generator.internal;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.mssql.MsSqlServerSqlScriptGenerator;

/**
 * Regression test for Issue #23610
 * @author rwei
 * @since 29.06.2012
 */
public class TestIssue23610
{
  /**
   * Regression test for ISSUE 23610
   * 
   * IMPORTANT: 
   * Not dropping unique constraints before altering table may not break system database conversion test. Also manually test may work.
   * But there are some SQL Server databases out there which cannot be converted when not dropping unique constraints! See Issue 23610 for details. 
   */
  @Test
  public void testDropUniqueConstraintsBeforeAlterColumn()
  {
    assertThat(new MsSqlServerSqlScriptGenerator().getRecreateOptions().uniqueConstraintsOnAlterTable).isTrue();
  }
}
