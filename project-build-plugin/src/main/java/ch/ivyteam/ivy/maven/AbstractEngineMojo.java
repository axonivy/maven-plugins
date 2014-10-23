package ch.ivyteam.ivy.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A MOJO that relies on an unpacked ivy engine.
 * 
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
public abstract class AbstractEngineMojo extends AbstractMojo
{
  /**
   * Location where an unpacked ivy Engine exists. 
   * If the Engine does not yet exist, it can be automatically downloaded. 
   */
  @Parameter(defaultValue = "${java.io.tmpdir}/ivyEngine")
  protected File engineDirectory;
  
  /**
   * The ivy Engine version that is used.
   */
  @Parameter(defaultValue = "6.0.0", required = true)
  protected String ivyVersion;

  public AbstractEngineMojo()
  {
    super();
  }

}