package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.db.meta.generator.MetaOutputGenerator;

@Mojo(name="generate-meta-output", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaOutputGeneratorMojo extends AbstractMojo
{  
  static final String GOAL = "generate-meta-output";

  @Parameter(required = true)
  private String generatorClass;
  
  @Parameter(defaultValue="${project.build.directory}/generated-sources/meta")
  private File outputDirectory;
  
  @Parameter
  private File outputFile;
  
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
      File targetDirectoryOrFile = generator.getTargetDirectoryOrFile();
      if (filesAreUpToDate(sqlMetaFiles, targetDirectoryOrFile))
      {
        logUpToDate(targetDirectoryOrFile);
        return;
      }
      logGenerating(targetDirectoryOrFile);
 
      generator.parseMetaDefinition();
      generator.generateMetaOutput();
      
      logSuccess(targetDirectoryOrFile);
      
      buildContext.refresh(targetDirectoryOrFile);
    }
    catch(Throwable ex)
    {
      generator.printHelp();
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate meta output", ex);
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
      args.add(outputFile.getAbsolutePath());
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
      for (String arg : arguments)
      {
        args.add(arg);
      }
    }
    return args.toArray(new String[args.size()]);
  }

  private boolean filesAreUpToDate(List<File> sqlMetaFiles, File targetDirectoryOrFile)
  {
    if  (!targetDirectoryOrFile.exists())
    {
      getLog().debug("Target directory or file does not exist. Build needed.");
      return false;
    }
    if (targetDirectoryOrFile.isDirectory())
    {
      if (emptyOutputDirectory(targetDirectoryOrFile))
      {
        getLog().debug("Target directory is empty. Build needed.");
        return false;
      }
      if (deletedOutputFilesInDelta(targetDirectoryOrFile))
      {
        getLog().debug("Target files were deleted since last incremental build. Build needed.");
        return false;
      }
    }
    long latestInputFileChange = getLatestInputFileChangeTimestamp(sqlMetaFiles);
    if (targetDirectoryOrFile.isDirectory())
    {
      if (outputFilesOutOfDate(targetDirectoryOrFile, latestInputFileChange))
      {
        return false;
      }
      return true;
    }
    if (outputFileOutOfDate(targetDirectoryOrFile, latestInputFileChange))
    {
      return false;
    }
    return true;
  }

  private long getLatestInputFileChangeTimestamp(List<File> sqlMetaFiles)
  {
    long latestInputFileChange = Long.MIN_VALUE;
    for (File inputFile : sqlMetaFiles)
    {
      if (inputFile.lastModified() > latestInputFileChange)
      {
        latestInputFileChange = inputFile.lastModified();
      }
    }
    return latestInputFileChange;
  }

  private boolean outputFilesOutOfDate(File targetDirectoryOrFile, long latestInputFileChange)
  {
    DirectoryScanner outputFileScanner = new DirectoryScanner();
    outputFileScanner.setBasedir(targetDirectoryOrFile);
    outputFileScanner.scan();
    for (String outputFileName : outputFileScanner.getIncludedFiles())
    {
      File outFile = new File (targetDirectoryOrFile, outputFileName);
      if (outputFileOutOfDate(outFile, latestInputFileChange))
      {
        return true;
      }
    }    
    return false;
  }

  private boolean outputFileOutOfDate(File targetDirectoryOrFile, long latestInputFileChange)
  {
    if (targetDirectoryOrFile.lastModified() < latestInputFileChange)
    {
      getLog().debug("Target file "+ getProjectRelativePath(targetDirectoryOrFile) +" is not up to date. Build needed.");
      return true;
    }
    return false;
  }

  private boolean deletedOutputFilesInDelta(File targetDirectoryOrFile)
  {
    Scanner deletedOutputs = buildContext.newDeleteScanner(targetDirectoryOrFile);
    deletedOutputs.setIncludes(new String[]{"**/*"});
    deletedOutputs.scan();
    if (deletedOutputs.getIncludedFiles().length > 0 || deletedOutputs.getIncludedDirectories().length > 0)
    {
      return true;
    }
    return false;
  }

  private boolean emptyOutputDirectory(File targetDirectoryOrFile)
  {
    File[] outputFiles = targetDirectoryOrFile.listFiles();
    if (outputFiles == null || outputFiles.length == 0)
    {
      return true;
    }
    return false;
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

  private void logUpToDate(File targetDirectoryOrFile)
  {
    if (targetDirectoryOrFile.isFile())
    {
      getLog().info("Output file "+getProjectRelativePath(targetDirectoryOrFile)+" is up to date. Nothing to do.");
    }
    else
    {
      getLog().info("Output files in directory "+getProjectRelativePath(targetDirectoryOrFile)+" are up to date. Nothing to do.");
    }
  }

  private void logGenerating(File targetDirectoryOrFile)
  {
    if (targetDirectoryOrFile.isFile())
    {
      getLog().info("Generating meta output file "+getProjectRelativePath(targetDirectoryOrFile)+" using generator class "+ generatorClass +" ...");
    }
    else
    {
      getLog().info("Generating meta output files to directory "+getProjectRelativePath(targetDirectoryOrFile)+" using generator class "+ generatorClass +" ...");
    }
  }

  private void logSuccess(File targetDirectoryOrFile)
  {
    if (targetDirectoryOrFile.isFile())
    {
      getLog().info("Meta output file "+getProjectRelativePath(targetDirectoryOrFile)+" successful generated.");
    }
    else
    {
      getLog().info("Meta output files successful generated to directory "+getProjectRelativePath(targetDirectoryOrFile)+".");
    }
  }

  private String getProjectRelativePath(File targetDirectoryOrFile)
  { 
    Path base = Paths.get(project.getBasedir().getAbsolutePath());
    Path path = Paths.get(targetDirectoryOrFile.getAbsolutePath());
    Path relativePath = base.relativize(path);
    return relativePath.toString();
  }

}
