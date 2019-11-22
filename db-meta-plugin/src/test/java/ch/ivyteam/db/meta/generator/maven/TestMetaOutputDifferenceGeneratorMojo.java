package ch.ivyteam.db.meta.generator.maven;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.HsqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.MySqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.PostgreSqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;

public class TestMetaOutputDifferenceGeneratorMojo
{
  @Rule
  public ProjectMojoRule<MetaOutputDifferenceGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputDifferenceGeneratorMojo.GOAL);
  private MetaOutputDifferenceGeneratorMojo mojo;

  @Before
  public void before() throws Exception
  {
    mojo = mojoRule.getMojo();
    FileSet inputFrom = new FileSet();
    inputFrom.setDirectory(mojoRule.getProject().getBasedir().getAbsolutePath());
    inputFrom.setIncludes(Arrays.asList("oldVersionMeta/simpleTestV0.meta"));
    mojoRule.setVariableValueToObject(mojo, "inputFrom", inputFrom);
    FileSet inputTo = new FileSet();
    inputTo.setDirectory(mojoRule.getProject().getBasedir().getAbsolutePath());
    inputTo.setIncludes(Arrays.asList("meta/simpleTest.meta"));

    mojoRule.setVariableValueToObject(mojo, "inputTo", inputTo); 
    mojoRule.setVariableValueToObject(mojo, "oldVersionId", "0"); 
  }

  @Test
  public void testNewVersionIsSet() throws Exception
  {
    String sqlContent = execute(OracleSqlScriptGenerator.class);
    assertThat(sqlContent).containsIgnoringCase("UPDATE IWA_VERSION SET Version=1;");
  }
  
  @Test
  public void testUniqueConstraintIsAddedOracle() throws Exception
  {
    String sqlContent = execute(OracleSqlScriptGenerator.class);
    assertThat(sqlContent)
      .containsIgnoringCase("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName);");
  }
  
  @Test
  public void testUniqueConstraintIsAddedMssql() throws Exception
  {
    String sqlContent = execute(MsSqlServerSqlScriptGenerator.class);
    assertThat(sqlContent)
      .containsIgnoringCase("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName)");
  }
  
  @Test
  public void testDropUniqueConstraintProcMssql() throws Exception
  {
    String sqlContent = execute(MsSqlServerSqlScriptGenerator.class);
    assertThat(sqlContent)
      .as("When in old table were no unique constraints, executing the procedure would fail. So, don't add it.")
      .doesNotContain("EXECUTE IWA_Drop_Unique 'IWA_ExternalDatabaseProperty'");
  }
  
  @Test
  public void testUniqueConstraintIsAddedOnlyOnceMssql() throws Exception
  {
    String sqlContent = execute(MsSqlServerSqlScriptGenerator.class);
    assertThat(sqlContent)
      .as("Unique constraints are recreated for changed columns and also new unique constraints are added." + 
              "Even though, same constraint should be only added once.")
      .containsOnlyOnce("ALTER TABLE IWA_ExternalDatabaseProperty ADD UNIQUE (ExternalDatabaseId, PropertyName)");
  }
  
  @Test
  @Ignore("Might be fixed in issue XIVY-1114")
  public void testForeignKeyAsTriggerRemovedMysql() throws Exception
  {
    String sqlContent = execute(MySqlSqlScriptGenerator.class);
    assertThat(sqlContent)
      .as("In new version, foreign key is used as reference instead of trigger. "
              + "So the existing trigger must be dropped and the new foreign key must be created.")
      .contains("DROP TRIGGER ApplicationDeleteTrigger")
      .contains("ALTER TABLE IWA_RestClient ADD\n(\n FOREIGN KEY");
  }
  
  @Test
  public void testDropColumnMySql() throws Exception
  {
    String sqlContent = execute(MySqlSqlScriptGenerator.class);
    assertThat(sqlContent)
      .as("In new version column OwnerPassword is dropped.")
      .contains("ALTER TABLE IWA_Application DROP COLUMN OwnerPassword;");
  }

  @Test
  public void testAlterColumnIntegerToVarcharHsql() throws Exception
  {
    String sqlContent = execute(HsqlSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "-- Changed columns of table IWA_Library\n"+
            "ALTER TABLE IWA_Library ALTER COLUMN\n"+
            "  Version VARCHAR(50) DEFAULT '' NOT NULL;"
    );
  }
  
  @Test
  public void testAlterColumnIntegerToVarcharMySql() throws Exception
  {
    String sqlContent = execute(MySqlSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "# Changed columns of table IWA_Library\n"+
            "ALTER TABLE IWA_Library MODIFY\n"+
            "  Version VARCHAR(50) NOT NULL DEFAULT '';"
    );
  }

  @Test
  public void testAlterColumnIntegerToVarcharPostgre() throws Exception
  {
    String sqlContent = execute(PostgreSqlSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "-- Changed columns of table IWA_Library\n"+
            "ALTER TABLE IWA_Library ALTER COLUMN Version TYPE VARCHAR(50);\n"+
            "ALTER TABLE IWA_Library ALTER COLUMN Version SET DEFAULT '';"
    );
  }
  
  @Test
  public void testAlterColumnIntegerToVarcharMsSqlServer() throws Exception
  {
    String sqlContent = execute(MsSqlServerSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "-- Changed columns of table IWA_Library\n"+
            "ALTER TABLE IWA_Library ALTER COLUMN Version VARCHAR(50) NOT NULL\n"+
            "GO\n"+
            "ALTER TABLE IWA_Library ADD CONSTRAINT DEFIWA_LibraryVersion  DEFAULT '' FOR Version\n"+
            "GO"
    );
  }

  @Test
  public void testAlterColumnIntegerToVarcharOracle() throws Exception
  {
    String sqlContent = execute(OracleSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "-- Changed columns of table IWA_Library\n"+
            "ALTER TABLE IWA_Library ADD\n"+
            "  Version_temp VARCHAR2(50) DEFAULT ' ' NOT NULL;\n"+
            "UPDATE IWA_Library\n"+
            "SET IWA_Library.Version_temp=to_char(IWA_Library.Version);\n"+
            "ALTER TABLE IWA_Library DROP COLUMN Version;\n"+
            "ALTER TABLE IWA_Library RENAME COLUMN Version_temp TO Version;"
    );
  }

  @Test
  public void testCreateColumnWithEmptyDefaultOracle() throws Exception
  {
    String sqlContent = execute(OracleSqlScriptGenerator.class);
    assertThat(sqlContent).contains(
            "-- Create new added tables\n"+
            "CREATE TABLE IWA_BusinessData\n"+
            "(\n"+
            "  BusinessDataId NUMBER(20) NOT NULL,\n"+
            "  Version NUMBER(20) NOT NULL,\n"+
            "  ObjectType VARCHAR2(50) DEFAULT '' NOT NULL,\n"+  // DEFAULT should be ''. In alter table this is ' ' see above.
            "  PRIMARY KEY (BusinessDataId)\n"+
            ")\n"+
            "TABLESPACE ${tablespaceName};"
    );
  }
  
  @Test
  public void testRemoveOnDeleteReplaceByTrigger() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    String sqlContent = execute(MySqlSqlScriptGenerator.class);
    String newTrigger = "# Create new triggers on existing table(s)\n"
            + "CREATE TRIGGER IWA_LibraryDeleteTrigger\n"
            + "AFTER DELETE ON IWA_Library\n"
            + "FOR EACH ROW\n"
            + "BEGIN\n"
            + "  UPDATE IWA_LibrarySpecification\n"
            + "  SET ResolvedLibraryId=NULL\n"
            + "  WHERE IWA_LibrarySpecification.ResolvedLibraryId=OLD.LibraryId;\n"
            + "\n"
            + "END\n"
            + ";";
    assertThat(sqlContent).contains(newTrigger);
    
    assertThat(sqlContent)
      .contains("# Remove foreign keys of table IWA_LibrarySpecification")
      .contains("CALL IWA_Drop_ForeignKey_Constraint(SCHEMA(), 'IWA_LibrarySpecification', 'ResolvedLibraryId');");
  }
  
  @Test
  public void testCreateTriggerOfAddedTableAfterAllTablesChanges() throws Exception
  {
    String sqlContent = execute(MsSqlServerSqlScriptGenerator.class);
    String createTable = "CREATE TABLE IWA_CaseMap\n"+
                        "(\n"+
                        "  CaseMapId BIGINT NOT NULL,\n"+
                        "  PRIMARY KEY (CaseMapId)\n"+
                        ")\n"+
                        "GO";
    String modifyTable = "-- Added columns of table IWA_Case\n"+
                         "ALTER TABLE IWA_Case ADD\n"+
                         "  CaseMapId BIGINT NULL\n"+
                         "GO";
    String createTrigger = "-- Create trigger which depend on new added table IWA_CaseMap\n"+
                        "CREATE TRIGGER IWA_CaseMapDeleteTrigger\n"+
                        "  ON IWA_CaseMap FOR DELETE AS\n"+
                        "\n"+
                        "  UPDATE IWA_Case  SET IWA_Case.CaseMapId=NULL\n"+
                        "  FROM IWA_Case, deleted\n"+
                        "  WHERE IWA_Case.CaseMapId = deleted.CaseMapId\n"+
                        "\n"+
                        "\n"+
                        "GO";
    assertThat(sqlContent).containsSequence(createTable, modifyTable, createTrigger);
  }

  private String execute(Class<? extends SqlScriptGenerator> generatorClass)
          throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    String outputFile = "convert"+generatorClass.getSimpleName()+".sql";
    mojoRule.setVariableValueToObject(mojo, "generatorClass", generatorClass.getName());
    mojoRule.setVariableValueToObject(mojo, "output", getProjectFile(outputFile));
    mojo.execute();
    String sqlContent = getProjectFileContent(outputFile);
    return sqlContent;
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
