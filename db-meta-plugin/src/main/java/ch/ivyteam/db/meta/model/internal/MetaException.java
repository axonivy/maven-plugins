package ch.ivyteam.db.meta.model.internal;

/**
 * Meta exception
 * @author rwei
 */
public class MetaException extends RuntimeException
{

  /**
   * serial version uid
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   * @param message 
   */
  public MetaException(String message)
  {
    super(message);
  }
  
}
