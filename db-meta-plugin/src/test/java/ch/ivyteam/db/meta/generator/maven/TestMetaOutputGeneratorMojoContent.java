package ch.ivyteam.db.meta.generator.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.JavaClassPersistencyServiceImplementationGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaEntityClassGenerator;
import ch.ivyteam.db.meta.generator.internal.mssql.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.oracle.OracleSqlScriptGenerator;

public class TestMetaOutputGeneratorMojoContent {

  private static final String GENERATED_ORACLE_SQL = "generated/oracle.sql";
  private static final String GENERATED_MSSQL_SQL = "generated/mssql.sql";
  @Rule
  public ProjectMojoRule<MetaOutputGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputGeneratorMojo.GOAL);
  private MetaOutputGeneratorMojo mojo;

  @Before
  public void before() throws Exception {
    mojo = mojoRule.getMojo();
    mojoRule.setVariableValueToObject(mojo, "outputDirectory", mojoRule.getProject().getBasedir());
    mojoRule.setVariableValueToObject(mojo, "inputDirectory",
            new File(mojoRule.getProject().getBasedir(), "meta"));
  }

  @Test
  public void testInsertIntoWithValuesIsGenerated() throws Exception {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", GENERATED_ORACLE_SQL);
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId) VALUES (1, 1)");
  }

  @Test
  public void testInsertIntoWithSelectIsGenerated() throws Exception {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", GENERATED_ORACLE_SQL);
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId)\nSELECT");
  }

  @Test
  public void testInsertIntoWithValuesIsGeneratedMsSql() throws Exception {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", GENERATED_MSSQL_SQL);
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId) VALUES (1, 1)");
    assertThat(sqlContent).contains("\nGO");
  }

  @Test
  public void testInsertIntoWithSelectIsGeneratedMsSql() throws Exception {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", MsSqlServerSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", GENERATED_MSSQL_SQL);
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_MSSQL_SQL);
    assertThat(sqlContent)
            .containsIgnoringCase("INSERT INTO IWA_RoleRoleMember (RoleId, RoleMemberId)\nSELECT");
    assertThat(sqlContent).contains("\nGO");
  }

  @Test
  public void testEntityAndImplClassesCreated()
          throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", JavaEntityClassGenerator.class.getName());
    List<String> args = Arrays.asList("-package", "ch.ivy.data", "-tables", "IWA_BusinessData");
    mojoRule.setVariableValueToObject(mojo, "arguments", args);
    mojo.execute();
    String dataContent = getProjectFileContent("ch/ivy/data/BusinessDataData.java");
    assertThat(dataContent)
            .contains("BusinessDataData setVersion(");
    assertThat(dataContent)
            .contains("public int hashCode()");
    mojoRule.setVariableValueToObject(mojo, "generatorClass",
            JavaClassPersistencyServiceImplementationGenerator.class.getName());
    args = Arrays.asList("-package", "ch.ivy.db", "-entityPackage",
            "ch.ivyteam.ivy.application.internal.data", "-tables", "IWA_BusinessData");
    mojoRule.setVariableValueToObject(mojo, "arguments", args);
    mojo.execute();
    String dbContent = getProjectFileContent("ch/ivy/db/DbBusinessDataData.java");
    // Test for the different methods, if the freemaker (ftl) generation fails,
    // some methods are typically missing.
    assertThat(dbContent)
            .contains(
                    "protected BusinessDataData createObjectFromResultSet(IPersistentTransaction transaction");
    assertThat(dbContent)
            .contains("protected void writeDataToUpdateStatement(IPersistentTransaction transaction");
    assertThat(dbContent)
            .contains("protected void writeDataToInsertStatement(IPersistentTransaction transaction");
    assertThat(dbContent)
            .contains(
                    "protected void writeDataToOptimisticUpdateStatement(IPersistentTransaction transaction");
  }

  private File getProjectFile(String path) {
    return new File(mojoRule.getProject().getBasedir(), path);
  }

  private String getProjectFileContent(String path) throws IOException {
    return Files.readString(getProjectFile(path).toPath());
  }
}
