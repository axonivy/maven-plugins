package ch.ivyteam.db.meta.parser.internal;

import java.text.MessageFormat;


/**
 * A Syntax error hold information about a syntax error
 * @author rwei
 * @since 15.09.2006
 */
public class SyntaxError 
{  
  /** The error message */
  private String message;
  
  /** The start position of this error. */
  private int left;
  
  /** The end position of this error. */
  private int right;
  
  /**
   * Constructor.
   * @param _message  the error message.
   * @param _left The start position of this error
   * @param _right The end position of this error.
   */
  public SyntaxError(String _message, int _left, int _right)
  {
    message = _message;
    left = _left;
    right = _right;
  }
  
  /**
   * Constructor.
   * @param _message the error message.
   * @param symbol the symbol that is wrong.
   */
  public SyntaxError(String _message, TerminalSymbol symbol)
  {
    if (symbol.value!=null)
    {
      message = MessageFormat.format(_message, new Object[]{symbol.getName(), symbol.value});
    }
    else
    {
      message = MessageFormat.format(_message, new Object[]{symbol.getName()});
    }
    left = symbol.left;
    right = symbol.right;
  }

  /**
   * Gets the message
   * @return the message
   */
  public String getMessage()
  {
    return message;
  }
  
  /**
   * Gets the start position of this error.
   * @return the left char index.
   */
  public int getStartPosition()
  {
    return left;
  }
  
  /**
   * Gets the end position of this error.
   * @return the right char index
   */
  public int getEndPosition()
  {
    return right;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(100+message.length());
    builder.append("Line ");
    builder.append(getStartPosition());
    builder.append(" Column ");
    builder.append(getEndPosition());
    builder.append(' ');
    builder.append(getMessage());
    return builder.toString();
  }
  
  
}
