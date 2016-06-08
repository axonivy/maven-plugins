package ch.ivyteam.db.meta.generator.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.MySqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;

public class TestMetaOutputDifferenceGeneratorMojo
{
  private static final String GENERATED_ORACLE_SQL = "generated/ConvertOracle.sql";
  private static final String GENERATED_MSSQL_SQL = "generated/ConvertMssql.sql";
  private static final String GENERATED_MYSQL_SQL = "generated/ConvertMysql.sql";
  @Rule
  public ProjectMojoRule<MetaOutputDifferenceGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputDifferenceGeneratorMojo.GOAL);
  private MetaOutputDifferenceGeneratorMojo mojo;

  @Before
  public void before() throws Exception
  {
    mojo = mojoRule.getMojo();
    mojoRule.setVariableValueToObject(mojo, "inputFrom", 
            new File(mojoRule.getProject().getBasedir(), "oldVersionMeta/simpleTestV0.meta"));
    mojoRule.setVariableValueToObject(mojo, "inputTo", 
            new File(mojoRule.getProject().getBasedir(), "meta/simpleTest.meta"));
    mojoRule.setVariableValueToObject(mojo, "oldVersionId", "0"); 
  }

  @Test
  public void testNewVersionIsSet() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_ORACLE_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent).containsIgnoringCase("UPDATE IWA_VERSION SET Version=1;");
  }
  
  @Test
  public void testUniqueConstraintIsAddedOracle() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_ORACLE_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent)
      .containsIgnoringCase("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName);");
  }
  
  @Test
  public void testUniqueConstraintIsAddedMssql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_MSSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
      .containsIgnoringCase("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName)");
  }
  
  @Test
  public void testDropUniqueConstraintProcMssql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_MSSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
      .as("When in old table were no unique constraints, executing the procedure would fail. So, don't add it.")
      .doesNotContain("EXECUTE IWA_Drop_Unique 'IWA_ExternalDatabaseProperty'");
  }
  
  @Test
  public void testUniqueConstraintIsAddedOnlyOnceMssql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_MSSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
      .as("Unique constraints are recreated for changed columns and also new unique constraints are added." + 
              "Even though, same constraint should be only added once.")
      .containsOnlyOnce("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName)");
  }
  
  @Test
  @Ignore("Might be fixed in issue XIVY-1114")
  public void testForeignKeyAsTriggerRemovedMysql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MySqlSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_MYSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MYSQL_SQL);
    assertThat(sqlContent)
      .as("In new version, foreign key is used as reference instead of trigger. "
              + "So the existing trigger must be dropped and the new foreign key must be created.")
      .contains("DROP TRIGGER ApplicationDeleteTrigger")
      .contains("ALTER TABLE IWA_RestClient ADD\n(\n FOREIGN KEY");
  }
  
  @Test
  public void testDropColumnMySql() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MySqlSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_MYSQL_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MYSQL_SQL);
    assertThat(sqlContent)
      .as("In new version column OwnerPassword is dropped.")
      .contains("ALTER TABLE IWA_Application DROP COLUMN OwnerPassword;");
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
