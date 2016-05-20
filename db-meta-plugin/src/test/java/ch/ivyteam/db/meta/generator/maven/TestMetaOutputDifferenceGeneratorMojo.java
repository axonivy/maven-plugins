package ch.ivyteam.db.meta.generator.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;

public class TestMetaOutputDifferenceGeneratorMojo
{
  private static final String GENERATED_ORACLE_SQL = "generated/ConvertOracle.sql";
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
  public void testUniqueConstraintIsAdded() throws Exception
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(GENERATED_ORACLE_SQL));
    mojo.execute();
    String sqlContent = getProjectFileContent(GENERATED_ORACLE_SQL);
    assertThat(sqlContent).containsIgnoringCase("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName);");
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
