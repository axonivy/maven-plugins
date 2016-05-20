package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import ch.ivyteam.db.meta.generator.MetaOutputDifferenceGenerator;
import ch.ivyteam.db.meta.generator.internal.NewLinePrintWriter;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;

@Mojo(name="generate-meta-output-difference", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaOutputDifferenceGeneratorMojo extends AbstractMojo
{    
  static final String GOAL = "generate-meta-output-difference";
  
  @Parameter(required = true)
  private String generatorClass;
  
  @Parameter
  private File output;

  @Parameter
  private File inputFrom;
  
  @Parameter
  private File inputTo;
  
  @Parameter
  private String oldVersionId;
  
  @Parameter
  private File additionalConversion;
  
  @Component
  private BuildContext buildContext;
  
  @Parameter(defaultValue="${project}", required=true, readonly=true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {    
    if (fileIsUpToDate())
    {
      getLog().info("Output file "+getAbsolutePath(output)+" is up to date. Nothing to do.");
      return;
    }
    try
    {
      logGenerating();
      generate();
      logSuccess();
    }
    catch(Exception ex)
    {
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate meta output difference", ex);
    }
    finally
    {
      refresh();
    }
  }

  private void generate() throws Exception, FileNotFoundException
  {
    SqlMeta metaFrom = MetaOutputDifferenceGenerator.parseMetaDefinition(inputFrom);
    SqlMeta metaTo = MetaOutputDifferenceGenerator.parseMetaDefinition(inputTo);
    SqlMeta additionalConversionMeta = MetaOutputDifferenceGenerator.parseMetaDefinition(additionalConversion);
    
    try (PrintWriter pr = new NewLinePrintWriter(output))
    {
      SqlScriptGenerator scriptGenerator = MetaOutputDifferenceGenerator.findGeneratorClass(generatorClass);
      int newVersionId = Integer.parseInt(oldVersionId) +1;
      MetaOutputDifferenceGenerator differenceGenerator = new MetaOutputDifferenceGenerator(metaFrom, metaTo, additionalConversionMeta, scriptGenerator, newVersionId);
      differenceGenerator.generate(pr);
    }
  }

  private boolean fileIsUpToDate()
  {
    if (!output.exists())
    {
      getLog().debug("Output file does not exist. Build needed.");
      return false;
    }
    long latestInputFileChange = getLatestInputFileChangeTimestamp();
    if (output.lastModified() < latestInputFileChange)
    {
      getLog().debug("Output file "+ getAbsolutePath(output) +" is not up to date. Build needed.");
      return false;
    }
    return true;
  }

  private long getLatestInputFileChangeTimestamp()
  {
    long latestInputFileChange = Math.max(inputFrom.lastModified(), inputTo.lastModified());
    return latestInputFileChange;
  }

  private void refresh()
  {
    getLog().debug("Refresh '" + getAbsolutePath(output) + "'");
    buildContext.refresh(output);
  }
  
  private void logGenerating()
  {
    getLog().info("Generating meta output difference " + getAbsolutePath(output) + " using generator class "+ generatorClass +" ...");
  }

  private void logSuccess()
  {
    getLog().info("Meta output difference " + getAbsolutePath(output) + " sucessful generated.");
  }

  private String getAbsolutePath(File file)
  {
    if (file == null)
    {
      return "";
    }
    return file.getAbsolutePath();
  }
}
