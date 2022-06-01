package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.db.meta.generator.MetaOutputGenerator;
import ch.ivyteam.db.meta.generator.Target;

@Mojo(name="generate-meta-output", defaultPhase=LifecyclePhase.GENERATE_SOURCES, threadSafe=true)
public class MetaOutputGeneratorMojo extends AbstractMojo
{
  static final String GOAL = "generate-meta-output";

  @Parameter(required = true)
  private String generatorClass;

  @Parameter(defaultValue="generated-db")
  private File outputDirectory;

  @Parameter
  private String outputFile;

  @Parameter(defaultValue="meta")
  private File inputDirectory;

  @Parameter
  private String[] includes = {"**/*.meta"};

  @Parameter()
  private List<String> arguments;

  @Component
  private BuildContext buildContext;

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    MetaOutputGenerator generator = new MetaOutputGenerator();
    try
    {
      List<File> sqlMetaFiles = getSqlMetaFiles();
      if (sqlMetaFiles.isEmpty())
      {
        getLog().warn("No meta input files found. Nothing to do.");
        return;
      }
      String[] args = getArguments(sqlMetaFiles);
      generator.analyseArgs(args);
      Target target = generator.getTarget();

      var targetDir = target.getTargetDirectory();
      if (targetDir == null) {
        target.getSingleTargetFile().delete();
      } else {
        delete(targetDir.toPath());
      }
      logGenerating(target);

      generator.parseMetaDefinition();
      generator.generateMetaOutput();

      logSuccess(target);

      refresh(target);
    }
    catch(Throwable ex)
    {
      generator.printHelp();
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate meta output", ex);
    }
  }

  private void delete(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    Files.walk(path)
            .map(Path::toFile)
            .forEach(File::delete);
  }

  private void refresh(Target target)
  {
    if (target.isSingleTargetFile())
    {
      buildContext.refresh(target.getSingleTargetFile());
    }
    else
    {
      buildContext.refresh(target.getTargetDirectory());
    }
  }

  private String[] getArguments(List<File> sqlMetaFiles)
  {
    List<String> args = new ArrayList<String>();
    args.add("-sql");
    for (File sqlMetaFile : sqlMetaFiles)
    {
      args.add(sqlMetaFile.getAbsolutePath());
    }
    args.add("-generator");
    args.add(generatorClass);
    if (outputFile != null)
    {
      args.add("-outputFile");
      args.add(new File(outputDirectory, outputFile).getAbsolutePath());
    }
    else
    {
      args.add("-outputDir");
      if (!outputDirectory.exists())
      {
        outputDirectory.mkdirs();
      }
      args.add(outputDirectory.getAbsolutePath());
    }
    if (arguments != null)
    {
      args.addAll(arguments);
    }
    return args.toArray(new String[args.size()]);
  }

  private List<File> getSqlMetaFiles() throws IOException
  {
    inputDirectory = inputDirectory.getCanonicalFile();
    Scanner inputScanner = buildContext.newScanner(inputDirectory, true);
    inputScanner.setIncludes(includes);
    inputScanner.scan();
    String[] includedFiles = inputScanner.getIncludedFiles();
    List<File> sqlMetaFiles = new ArrayList<>();
    for (String input : includedFiles)
    {
      sqlMetaFiles.add(new File(inputDirectory, input));
    }
    return sqlMetaFiles;
  }

  private void logGenerating(Target target)
  {
    getLog().info("Generating meta output "+formatTarget(target)+" using generator class "+ generatorClass +" ...");
  }

  private void logSuccess(Target target)
  {
    getLog().info("Meta output "+formatTarget(target)+" sucessful generated.");
  }

  private String formatTarget(Target target)
  {
    if (target.isSingleTargetFile())
    {
      return "file "+getAbsolutePath(target.getSingleTargetFile());
    }
    else
    {
      if (target.numberOfTargetFiles() > 0)
      {
        return "files ("+Integer.toString(target.numberOfTargetFiles())+") in directory "+getAbsolutePath(target.getTargetDirectory());
      }
      else
      {
        return "files in directory "+getAbsolutePath(target.getTargetDirectory());
      }
    }
  }

  private String getAbsolutePath(File targetDirectoryOrFile)
  {
    if (targetDirectoryOrFile == null)
    {
      return "";
    }
    return targetDirectoryOrFile.getAbsolutePath();
  }

}
