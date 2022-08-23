package ch.ivyteam.parser.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import java_cup.internal_error;
import jflex.exceptions.SilentExit;
import jflex.option.Options;

@Mojo(name="generate-parser", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class ParserGeneratorMojo extends AbstractMojo
{
  static final String GOAL = "generate-parser";

  @Parameter(required = true)
  private File parserFile;

  @Parameter(required = true)
  private File scannerFile;

  @Parameter(defaultValue="${project.build.directory}/generated-sources/parser", required=true)
  private File outputDirectory;

  @Component
  private BuildContext buildContext;

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    try
    {
      boolean generated = false;
      File packageDir = parsePackageDir();
      createOutputDirectory(packageDir);
      generated = generateParser(packageDir) || generated;
      generated = generateScanner(packageDir) || generated;
      if (generated)
      {
        addAdditionalCompileSourcePath();
        addSuppressWarningsAnnotations(packageDir);
        buildContext.refresh(packageDir);
      }
    }
    catch(Exception ex)
    {
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate parser", ex);
    }
  }

  private void addAdditionalCompileSourcePath()
  {
    project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    getLog().info("Add additional source directory "+outputDirectory);
  }

  private boolean parserUpToDate(File packageDir)
  {
    File javaParser = new File(packageDir, "Parser.java");
    return fileUpToDate(parserFile, javaParser);
  }

  private boolean scannerUpToDate(File packageDir)
  {
    File javaScanner = new File(packageDir, "Scanner.java");
    return fileUpToDate(scannerFile, javaScanner);
  }

  private boolean fileUpToDate(File sourceFile, File targetFile)
  {
    if (!targetFile.exists())
    {
      return false;
    }
    if (targetFile.lastModified() < sourceFile.lastModified())
    {
      return false;
    }
    return true;
  }

  private void addSuppressWarningsAnnotations(File packageDir) throws IOException
  {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(packageDir);
    scanner.setIncludes(new String[]{"**/*.java"});
    scanner.scan();
    for (String path : scanner.getIncludedFiles())
    {
      File javaFile = new File(packageDir, path);
      String content = FileUtils.readFileToString(javaFile);
      content = content.replace("@SuppressWarnings(\"all\")", "");
      content = content.replace("public class ", "@SuppressWarnings(\"all\")\r\npublic class ");
      content = content.replace("\r\nclass ", "\r\n@SuppressWarnings(\"all\")\r\nclass ");
      content = content.replace("public interface ", "@SuppressWarnings(\"all\")\r\npublic interface ");
      FileUtils.write(javaFile, content);
    }
  }

  private File parsePackageDir() throws FileNotFoundException, IOException
  {
    try(BufferedReader br = new BufferedReader(new FileReader(parserFile)))
    {
      while (br.ready())
      {
        String line = br.readLine();
        if (line.startsWith("package") && line.contains(";"))
        {
          String packageName = line.substring(8,line.indexOf(";"));
          packageName = packageName.replace('.', File.separatorChar);
          return new File(outputDirectory, packageName);
        }
      }
    }
    return outputDirectory;
  }

  private void createOutputDirectory(File packageDir)
  {
    if (!packageDir.exists())
    {
      packageDir.mkdirs();
    }
  }

  private boolean generateParser(File packageDir) throws internal_error, IOException, Exception
  {
    if (parserUpToDate(packageDir))
    {
      getLog().info("Parser up to date. Nothing to do.");
      return false;
    }

    getLog().info("Generating parser to "+packageDir+ "...");

    List<String> args = new ArrayList<String>();
    args.add("-destdir");
    args.add(packageDir.getAbsolutePath());
    args.add("-interface");
    args.add("-parser");
    args.add("Parser");
    args.add("-symbols");
    args.add("Symbols");
    args.add(parserFile.getAbsolutePath());

    java_cup.Main.main(args.toArray(new String[args.size()]));

    getLog().info("Parser generated to "+ packageDir + ".");
    return true;

  }

  private boolean generateScanner(File packageDir) throws SilentExit
  {
    if (scannerUpToDate(packageDir))
    {
      getLog().info("Scanner up to date. Nothing to do.");
      return false;
    }
    getLog().info("Generating scanner to "+packageDir+ "...");

    Options.setRootDirectory(packageDir);
    jflex.Main.generate(new String[] { scannerFile.getAbsolutePath() });
    getLog().info("Scanner generated to "+ packageDir + ".");
    return true;
  }
}
