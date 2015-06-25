package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.db.meta.generator.MetaOutputGenerator;

@Mojo(name="generate-meta-output", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaOutputGeneratorMojo extends AbstractMojo
{  
  @Parameter(required = true)
  private String generatorClass;
  
  @Parameter(defaultValue="${project.build.directory}/generated-sources/meta")
  private File outputDirectory;
  
  @Parameter(defaultValue="meta")
  private File inputDirectory;
  
  @Parameter
  private String[] includes = {"**/*.meta"};

  @Parameter()
  private List<String> arguments;
  
  @Component
  private BuildContext buildContext;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    try
    {
      List<File> sqlMetaFiles = getSqlMetaFiles();
      if (sqlMetaFiles.isEmpty())
      {
        getLog().warn("No meta input files found. Nothing to do.");
        return;
      }
      if (filesAreUpToDate(sqlMetaFiles))
      {
        getLog().info("Output files are up to date. Nothing to do.");
        return;
      }
      List<String> args = getArguments(sqlMetaFiles);
      getLog().info("Generating meta output files using generator class "+ generatorClass +" ...");
      MetaOutputGenerator.mainWithoutSystemExit(args.toArray(new String[args.size()]));
      getLog().info("Meta output files successful generated.");
      buildContext.refresh(outputDirectory);
    }
    catch(Throwable ex)
    {
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate meta output", ex);
    }   
  }

  private List<String> getArguments(List<File> sqlMetaFiles)
  {
    List<String> args = new ArrayList<String>();
    args.add("-sql");
    for (File sqlMetaFile : sqlMetaFiles)
    {
      args.add(sqlMetaFile.getAbsolutePath());
    }
    args.add("-generator");
    args.add(generatorClass);
    args.add("-outputDir");
    if (!outputDirectory.exists())
    {
      outputDirectory.mkdirs();
    }
    args.add(outputDirectory.getAbsolutePath());
    for (String arg : arguments)
    {
      args.add(arg);
    }
    return args;
  }

  private boolean filesAreUpToDate(List<File> sqlMetaFiles)
  {
    if (emptyOutputDirectory())
    {
      getLog().debug("Output directory is empty. Build needed.");
      return false;
    }
    if (deletedOutputFilesInDelta())
    {
      getLog().debug("Output files were deleted since last incremental build. Build needed.");
      return false;
    }
    if (outputFileOutOfDate(sqlMetaFiles))
    {
      return false;
    }    
    return true;
  }

  private boolean outputFileOutOfDate(List<File> sqlMetaFiles)
  {
    long latestInputFileChange = Long.MIN_VALUE;
    for (File inputFile : sqlMetaFiles)
    {
      if (inputFile.lastModified() > latestInputFileChange)
      {
        latestInputFileChange = inputFile.lastModified();
      }
    }
    DirectoryScanner outputFileScanner = new DirectoryScanner();
    outputFileScanner.setBasedir(outputDirectory);
    outputFileScanner.scan();
    for (String outputFileName : outputFileScanner.getIncludedFiles())
    {
      File outputFile = new File (outputDirectory, outputFileName);
      if (outputFile.lastModified() < latestInputFileChange)
      {
        getLog().debug("Output file "+ outputFile +" is not up to date. Build needed.");
        return true;
      }
    }
    
    return false;
  }

  private boolean deletedOutputFilesInDelta()
  {
    Scanner deletedOutputs = buildContext.newDeleteScanner(outputDirectory);
    deletedOutputs.setIncludes(new String[]{"**/*"});
    deletedOutputs.scan();
    if (deletedOutputs.getIncludedFiles().length > 0 || deletedOutputs.getIncludedDirectories().length > 0)
    {
      return true;
    }
    return false;
  }

  private boolean emptyOutputDirectory()
  {
    if (!outputDirectory.exists())
    {
      return true;
    }
    File[] outputFiles = outputDirectory.listFiles();
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

}
