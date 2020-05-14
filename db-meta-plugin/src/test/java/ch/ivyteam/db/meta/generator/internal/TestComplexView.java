package ch.ivyteam.db.meta.generator.internal;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.ivyteam.db.meta.generator.internal.query.JavaQueryClassGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

public class TestComplexView
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Test
  public void parseAst() throws Exception
  {
    SqlMeta meta = parse();
    SqlView view = meta.getArtifacts(SqlView.class).get(0);
    System.out.println("generated:\n"+view);

    assertThat(view.toString()).isEqualTo("CREATE VIEW IWA_TaskQuery(\n" + 
            "  TaskId,\n" + 
            "  ActivatorDisplayName,\n" + 
            "  ExpiryActivatorDisplayName,\n" + 
            "  CurrentActivatorDisplayName,\n" + 
            "  IsUnassigned)\n" + 
            "AS SELECT\n"+
            "  IWA_Task.TaskId,\n" + 
            "  CASE WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(ActivatorUser.FullName)>0 THEN ActivatorUser.FullName WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(ActivatorUser.FullName)=0 THEN ActivatorUser.Name WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(ActivatorRole.DisplayNameTemplate)>0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(ActivatorRole.DisplayNameTemplate)=0 THEN ActivatorRole.Name END,\n" + 
            "  CASE WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(ExpiryActivatorUser.FullName)>0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(ExpiryActivatorUser.FullName)=0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(ExpiryActivatorRole.DisplayNameTemplate)>0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(ExpiryActivatorRole.DisplayNameTemplate)=0 THEN ExpiryActivatorRole.Name ELSE NULL END,\n" + 
            "  CASE WHEN IWA_Task.IsExpired=1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(ExpiryActivatorUser.FullName)>0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.IsExpired=1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(ExpiryActivatorUser.FullName)=0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.IsExpired=1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(ExpiryActivatorRole.DisplayNameTemplate)>0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired=1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(ExpiryActivatorRole.DisplayNameTemplate)=0 THEN ExpiryActivatorRole.Name WHEN IWA_Task.IsExpired=0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(ActivatorUser.FullName)>0 THEN ActivatorUser.FullName WHEN IWA_Task.IsExpired=0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(ActivatorUser.FullName)=0 THEN ActivatorUser.Name WHEN IWA_Task.IsExpired=0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(ActivatorRole.DisplayNameTemplate)>0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired=0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(ActivatorRole.DisplayNameTemplate)=0 THEN ActivatorRole.Name END,\n" +
            "  CASE WHEN IWA_Task.ActivatorRoleId IS NULL AND IWA_Task.ActivatorUserId IS NULL THEN 1 WHEN IWA_Task.ActivatorUserId IS NOT NULL AND ActivatorUser.UserState>0 THEN 1 ELSE 0 END\n" + 
            "FROM IWA_Task INNER JOIN IWA_Case ON IWA_Task.CaseId=IWA_Case.CaseId LEFT OUTER JOIN IWA_User AS ActivatorUser ON IWA_Task.ActivatorUserId=ActivatorUser.UserId LEFT OUTER JOIN IWA_Role AS ActivatorRole ON IWA_Task.ActivatorRoleId=ActivatorRole.RoleId LEFT OUTER JOIN IWA_User AS ExpiryActivatorUser ON IWA_Task.ExpiryActivatorUserId=ExpiryActivatorUser.UserId LEFT OUTER JOIN IWA_Role AS ExpiryActivatorRole ON IWA_Task.ExpiryActivatorRoleId=ExpiryActivatorRole.RoleId"); 
  }
  
  @Test
  public void generateHtml() throws Exception
  {
    SqlMeta meta = parse();
    HtmlDocGenerator html = new HtmlDocGenerator();
    File outputDir = tempFolder.newFolder();
    html.analyseArgs(new String[] {"-outputDir", outputDir.getAbsolutePath()});
    html.generateMetaOutput(meta);    
    String generatedHtml = FileUtils.readFileToString(new File(outputDir, "IWA_TaskQuery.html"));
    String originalHtml = loadTestResource("IWA_TaskQuery.html");
    assertThat(generatedHtml).isEqualTo(originalHtml);
  }
  
  @Test
  public void generateHsqlSql() throws Exception
  {
    generateSql(new HsqlSqlScriptGenerator(), "hsql.sql");
  }

  @Test
  public void generateMsSqlServerSql() throws Exception
  {
    generateSql(new MsSqlServerSqlScriptGenerator(), "mssqlserver.sql");
  }
  
  @Test
  public void generateOracleSql() throws Exception
  {
    generateSql(new OracleSqlScriptGenerator(), "oracle.sql");
  }
  
  @Test
  public void generateQueryJavaClass() throws Exception
  {
    SqlMeta meta = parse();
    File outputDirectory = tempFolder.newFolder();
    File templateDirectory = tempFolder.newFolder();
    String template = loadTestResource("QueryClass.ftl");
    File templateFile = new File(templateDirectory, "QueryClass.ftl");
    FileUtils.write(templateFile, template);
    File sourceDirectory = tempFolder.newFolder();
    
    JavaQueryClassGenerator generator = new JavaQueryClassGenerator();
    generator.analyseArgs(new String[] {
        "-outputDir", outputDirectory.getAbsolutePath(), 
        "-package", "ch.ivyteam.meta.query", 
        "-tables", "IWA_Task",
        "-templateDir", templateDirectory.getAbsolutePath(), 
        "-sourceDir", sourceDirectory.getAbsolutePath()});
    generator.generateMetaOutput(meta);
    String generatedJavaClass = FileUtils.readFileToString(new File(outputDirectory, "ch/ivyteam/meta/query/TaskQuery.java"));
    String originalJavaClass = loadTestResource("TaskQuery.java");
    generatedJavaClass = removeGenerationDate(generatedJavaClass);
    originalJavaClass = removeGenerationDate(originalJavaClass);
    assertThat(generatedJavaClass).isEqualTo(originalJavaClass);    
  }

  private String removeGenerationDate(String text)
  {
    int start = text.indexOf("@Generated");
    int end = text.indexOf("\n", start);
    return text.substring(0, start)+text.substring(end);
  }

  private void generateSql(SqlScriptGenerator generator, String fileName) throws Exception
  {
    SqlMeta meta = parse();
    File outputFile = tempFolder.newFile(fileName);
    generator.analyseArgs(new String[] {"-outputFile", outputFile.getAbsolutePath()});
    generator.generateMetaOutput(meta);        
    String generatedSql = FileUtils.readFileToString(outputFile);
    String originalSql = loadTestResource(fileName);
    assertThat(generatedSql).isEqualTo(originalSql);
  }

  private String loadTestResource(String name) throws IOException
  {
    try (InputStream is = TestComplexView.class.getResourceAsStream(name))
    {
      return IOUtils.toString(is);
    }
  }
   
  private SqlMeta parse() throws FileNotFoundException, Exception
  {
    try(InputStreamReader isr = new InputStreamReader(TestComplexView.class.getResourceAsStream("complexView.meta")))
    {
      Scanner scanner = new Scanner(isr);
      Parser parser = new Parser(scanner);
      SqlMeta sqlMetaDefinition = (SqlMeta)parser.parse().value;
      return sqlMetaDefinition;
    }
  }
}
