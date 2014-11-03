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
  protected static final String DEFAULT_VERSION = "6.0.0";
  
  /**
   * Location where an unpacked (may pre-configured) ivy Engine in the {@link #ivyVersion required version} exists. 
   * <p>If parameter is not set it will be a sub-directory of the {@link #engineCacheDirectory}.
   * 
   * <p>If the Engine does not yet exist, it can be automatically downloaded. 
   */
  @Parameter
  protected File engineDirectory;
  
  /**
   * Location where ivy engines in required version can be extracted to. 
   * <p>If the Engine does not yet exist, it can be automatically downloaded. 
   */
  @Parameter(defaultValue = "${settings.localRepository}/.cache/ivy")
  protected File engineCacheDirectory;
  
  /**
   * The ivy Engine version that is used.
   */
  @Parameter(defaultValue = DEFAULT_VERSION, required = true)
  protected String ivyVersion;

  public AbstractEngineMojo()
  {
    super();
  }

}