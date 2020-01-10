package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.filefilter.FileFileFilter;
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
import ch.ivyteam.db.meta.generator.Target;

@Mojo(name="generate-meta-output", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaOutputGeneratorMojo extends AbstractMojo
{  
  static final String GOAL = "generate-meta-output";

  @Parameter(required = true)
  private String generatorClass;
  
  @Parameter(defaultValue="${project.build.directory}/generated")
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
      if (filesAreUpToDate(sqlMetaFiles, target))
      {
        logUpToDate(target);
        return;
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
      for (String arg : arguments)
      {
        args.add(arg);
      }
    }
    return args.toArray(new String[args.size()]);
  }

  private boolean filesAreUpToDate(List<File> sqlMetaFiles, Target target)
  {
    long latestInputFileChange = getLatestInputFileChangeTimestamp(sqlMetaFiles);
    if (target.isSingleTargetFile())
    {
      if (!target.getSingleTargetFile().exists())
      {
        getLog().debug("Target file does not exist. Build needed.");
        return false;
      }
      if (outputFileOutOfDate(target.getSingleTargetFile(), latestInputFileChange))
      {
        return false;
      }
    }
    else
    {
      if (!target.getTargetDirectory().exists())
      {
        getLog().debug("Target directory does not exist. Build needed.");
        return false;
      }
      if (emptyOutputDirectory(target.getTargetDirectory()))
      {
        getLog().debug("Target directory is empty. Build needed.");
        return false;
      }
      if (deletedOutputFilesInDelta(target.getTargetDirectory()))
      {
        getLog().debug("Target files were deleted since last incremental build. Build needed.");
        return false;
      }
      if (outputFilesOutOfDate(target.getTargetDirectory(), latestInputFileChange))
      {
        return false;
      }
      if (target.numberOfTargetFiles() > 0)
      {
        int fileCount = getNumberOfFilesInDirectory(target);
        if (fileCount < target.numberOfTargetFiles())
        {
          getLog().debug("Target files are missing. Exepecting "+target.numberOfTargetFiles()+". Found "+fileCount+". Build needed");
          return false;
        }
        if (fileCount > target.numberOfTargetFiles())
        {
          getLog().debug("Too many target files. Exepecting "+target.numberOfTargetFiles()+". Found "+fileCount+". Build needed");
          return false;
        }
      }
    }
    return true;
  }

  private int getNumberOfFilesInDirectory(Target target)
  {
    File[] listFiles = target.getTargetDirectory().listFiles((FileFilter)FileFileFilter.FILE);
    if (listFiles == null)
    {
      return 0;
    }
    return listFiles.length;
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
      getLog().debug("Target file "+ getAbsolutePath(targetDirectoryOrFile) +" is not up to date. Build needed.");
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

  private void logUpToDate(Target target)
  {
    if (target.numberOfTargetFiles()<=1)
    {
      getLog().info("Output "+formatTarget(target)+" is up to date. Nothing to do.");
    }
    else
    {
      getLog().info("Output "+formatTarget(target)+" are up to date. Nothing to do.");
    }
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
