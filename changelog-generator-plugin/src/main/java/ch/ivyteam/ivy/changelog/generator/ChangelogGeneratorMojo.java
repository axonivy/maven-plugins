package ch.ivyteam.ivy.changelog.generator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;
import ch.ivyteam.ivy.changelog.generator.jira.JiraService;
import ch.ivyteam.ivy.changelog.generator.util.TemplateExpander;
import ch.ivyteam.ivy.changelog.generator.util.TokenReplacer;

@Mojo(name = "generate-changelog", requiresProject = false)
public class ChangelogGeneratorMojo extends AbstractMojo
{
  /** server id which is configured in settings.xml */
  @Parameter(property = "jiraServerId")
  public String jiraServerId;

  /** jira base url */
  @Parameter(property = "jiraServerUri", defaultValue = "https://jira.axonivy.com/jira")
  public String jiraServerUri;

  /*** version to generate the changelog from. for example 7.1 or 7.0.3 */
  @Parameter(property = "fixVersion", required = true)
  public String fixVersion;

  /** comma separated list of jira projects for example: XIVY, IVYPORTAL */
  @Parameter(property = "jiraProjects", defaultValue = "XIVY")
  public String jiraProjects;

  /** comma separated list of labels which will be parsed as batches for example: security, performance, usability */
  @Parameter(property = "whitelistJiraLabels", defaultValue = "")
  public String whitelistJiraLabels;
  
  /** */
  @Parameter(property = "type", defaultValue = "")
  public String type;

  /** files which tokens must be replaced */
  @Parameter(property="fileset", required = true)
  public FileSet fileset;

  @Parameter(property = "markdownTemplate", defaultValue = "* [${key}](${uri}) ${summary} ${labelsWithHtmlBatches}")
  public String markdownTemplate;
  @Parameter(property = "markdownTemplateImprovements", defaultValue = "* ${summary} ${htmlLinkIcon} ${labelsWithHtmlBatches}")
  public String markdownTemplateImprovements;

  @Parameter(property = "asciiTemplate", defaultValue = "${kind} ${key}${spacesKey} ${type}${spacesType} ${summary}")
  public String asciiTemplate;

  @Parameter(property = "project", required = false, readonly = true)
  MavenProject project;
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("generating changelog for fixVersion " + fixVersion + " and jiraProjects " + jiraProjects);

    Server server = session.getSettings().getServer(jiraServerId);
    if (server == null)
    {
      getLog().warn("can not generate changelog because server '" + jiraServerId + "' is not definied in setting.xml");
      return;
    }

    List<Issue> issues = loadIssuesFromJira(server);
    for (File file : getAllFiles())
    {
      TemplateExpander expander = createExpanderForFile(file);
      if (expander != null)
      {
        getLog().info("replace tokens in " + file.getAbsolutePath());
        Map<String, String> tokens = generateTokens(issues, expander);
        new TokenReplacer(file, tokens).replaceTokens();
      }
      if (StringUtils.equals(type, "deb"))
      {
        compressMaxGzipFile(file);
      }
    }
  }
  
  private void compressMaxGzipFile(File file)
  {
    String gzipFile = getGzipFile(file);
    GzipParameters parameters = new GzipParameters();
    parameters.setCompressionLevel(9);
    try (InputStream in = new FileInputStream(file);
            OutputStream os = new FileOutputStream(gzipFile);
            BufferedOutputStream out = new BufferedOutputStream(os);
            GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(out, parameters);)
    {
      final byte[] buffer = new byte[1024];
      int n = 0;
      while (-1 != (n = in.read(buffer))) {
          gzOut.write(buffer, 0, n);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    file.delete();
  }

  private String getGzipFile(File file)
  {
    String gzipFile = file.getAbsolutePath() + ".gz";
    try
    {
      new File(gzipFile).createNewFile();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    return gzipFile;
  }
  
//  private static void compressGzipFile(File file)
//  {
//    String gzipFile = file.getAbsolutePath() + ".gz";
//    try
//    {
//      new File(gzipFile).createNewFile();
//    }
//    catch (IOException ex)
//    {
//      ex.printStackTrace();
//    }
//    try (FileInputStream fis = new FileInputStream(file);
//            FileOutputStream fos = new FileOutputStream(gzipFile);
//            OutputStream gzipOS = new GZIPOutputStream(fos) {{def.setLevel(Deflater.BEST_COMPRESSION);}};)
//    {
//      byte[] buffer = new byte[1024];
//      int len;
//      while ((len = fis.read(buffer)) != -1)
//      {
//        gzipOS.write(buffer, 0, len);
//      }
//    }
//    catch (IOException e)
//    {
//      e.printStackTrace();
//    }
//    file.delete();
//  }

  private List<Issue> loadIssuesFromJira(Server server) throws MojoExecutionException
  {
    try
    {
      JiraService jiraService = new JiraService(jiraServerUri, server, getLog());
      return jiraService.getIssuesWithFixVersion(fixVersion,jiraProjects);
    }
    catch (RuntimeException ex)
    {
      throw new MojoExecutionException("could not load issues from jira", ex);
    }
  }

  private List<File> getAllFiles()
  {
    String sourceDir = fileset.getDirectory();
    String outputDir = fileset.getOutputDirectory();
    List<File> files = new ArrayList<>();
    if (StringUtils.isNotEmpty(outputDir))
    {
      for (String file : new FileSetManager().getIncludedFiles(fileset))
      {
        File sourceFile = new File(sourceDir + File.separatorChar + file);
        File outputFile = new File(outputDir + File.separatorChar + file);
        try
        {
          FileUtils.copyFile(sourceFile, outputFile);
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
        }
        files.add(outputFile);
      }
      return files;
    }
    else
    {
      return Arrays.stream(new FileSetManager().getIncludedFiles(fileset))
              .map(f -> new File(fileset.getDirectory() + File.separatorChar + f))
              .collect(Collectors.toList());
    }
  }

  private TemplateExpander createExpanderForFile(File file)
  {
    if (file.getName().endsWith(".md"))
    {
      return new TemplateExpander(markdownTemplate, markdownTemplateImprovements, whitelistJiraLabels);
    }
    return new TemplateExpander(asciiTemplate, asciiTemplate, whitelistJiraLabels);
  }

  private Map<String, String> generateTokens(List<Issue> issues, TemplateExpander expander)
  {
    Map<String, String> tokens = new HashMap<>();
    tokens.put("changelog", expander.expand(issues));
    tokens.put("changelog#bugs", expander.expand(onlyBugs(issues)));
    tokens.put("changelog#improvements", expander.expandImprovements(onlyImprovements(issues)));
    return tokens;
  }

  private static List<Issue> onlyBugs(List<Issue> issues)
  {
    return issues.stream().filter(i -> i.isBug()).collect(Collectors.toList());
  }

  private static List<Issue> onlyImprovements(List<Issue> issues)
  {
    return issues.stream().filter(i -> i.isImprovement()).collect(Collectors.toList());
  }
}
