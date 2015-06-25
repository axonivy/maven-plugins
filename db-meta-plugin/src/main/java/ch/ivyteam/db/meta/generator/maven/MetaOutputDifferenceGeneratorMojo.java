package ch.ivyteam.db.meta.generator.maven;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.ivyteam.db.meta.generator.MetaOutputDifferenceGenerator;
import ch.ivyteam.db.meta.generator.internal.NewLinePrintWriter;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;

@Mojo(name="generate-meta-output-difference")
public class MetaOutputDifferenceGeneratorMojo extends AbstractMojo
{    
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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    
    try
    {
      SqlMeta metaFrom = MetaOutputDifferenceGenerator.parseMetaDefinition(inputFrom);
      SqlMeta metaTo = MetaOutputDifferenceGenerator.parseMetaDefinition(inputTo);
      PrintWriter pr = new NewLinePrintWriter(output);
      try
      {
        SqlScriptGenerator scriptGenerator = MetaOutputDifferenceGenerator.findGeneratorClass(generatorClass);
        int newVersionId = Integer.parseInt(oldVersionId) +1;
        MetaOutputDifferenceGenerator differenceGenerator = new MetaOutputDifferenceGenerator(metaFrom, metaTo, scriptGenerator, newVersionId);
        differenceGenerator.generate(pr);
      }
      finally
      {
        IOUtils.closeQuietly(pr);
      }
    }
    catch(Exception ex)
    {
      getLog().error(ex);
      throw new MojoExecutionException("Could not generate meta output difference", ex);
    }
  }

}
