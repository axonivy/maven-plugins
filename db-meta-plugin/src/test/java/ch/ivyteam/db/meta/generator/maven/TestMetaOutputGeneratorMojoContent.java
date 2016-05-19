package ch.ivyteam.db.meta.generator.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;

public class TestMetaOutputGeneratorMojoContent
{
  private static final String GENERATED_ORACLE_SQL = "generated/oracle.sql";
  private static final String GENERATED_MSSQL_SQL = "generated/mssql.sql";
  @Rule
  public ProjectMojoRule<MetaOutputGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputGeneratorMojo.GOAL);
  private MetaOutputGeneratorMojo mojo;

  @Before
  public void before() throws Exception
  {
    mojo = mojoRule.getMojo();
    mojoRule.setVariableValueToObject(mojo, "outputDirectory",
            new File(mojoRule.getProject().getBasedir(), "generated"));
    mojoRule.setVariableValueToObject(mojo, "inputDirectory",
            new File(mojoRule.getProject().getBasedir(), "meta"));
  }

  @Test
  public void testInsertIntoWithValuesIsGenerated() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", getProjectFile(GENERATED_ORACLE_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId) VALUES (1, 1)");
  }

  @Test
  public void testInsertIntoWithSelectIsGenerated() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", getProjectFile(GENERATED_ORACLE_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId)\nSELECT");
  }

  @Test
  public void testInsertIntoWithValuesIsGeneratedMsSql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", getProjectFile(GENERATED_MSSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId) VALUES (1, 1)");
    assertThat(sqlContent).contains("\nGO");
  }

  @Test
  public void testInsertIntoWithSelectIsGeneratedMsSql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", getProjectFile(GENERATED_MSSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId)\nSELECT");
    assertThat(sqlContent).contains("\nGO");
  }
  
  private File getProjectFile(String path)
  {
    return new File(mojoRule.getProject().getBasedir(), path);
  }

  private String getProjectFileContent(String path) throws IOException
  {
    return FileUtils.readFileToString(getProjectFile(path));
  }
}
