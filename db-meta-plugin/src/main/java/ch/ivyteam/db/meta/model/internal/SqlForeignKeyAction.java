package ch.ivyteam.db.meta.model.internal;

/**
 * Defines the sql foreign key actions that could be executed on certain events
 * @author rwei
 */
public enum SqlForeignKeyAction
{
  /** Set Reference to NULL if referencee is deleted */
  ON_DELETE_SET_NULL,
  
  /** Delete the row containing the reference if the referencee is deleted */
  ON_DELETE_CASCADE,
  
  /** Delete the refeferenced row in the referenced table if the row containing the reference is deleted */ 
  ON_DELETE_THIS_CASCADE;
  
  /**
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() 
  {
    switch (this)
    {
      case ON_DELETE_CASCADE:
        return "ON DELETE CASCADE";
      case ON_DELETE_SET_NULL:
        return "ON DELETE SET NULL";
      case ON_DELETE_THIS_CASCADE:
        return "ON DELETE THIS CASCADE";
    }
    return null;
  }
}
