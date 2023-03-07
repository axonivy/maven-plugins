package ch.ivyteam.db.meta.parser.internal;

import java.util.List;

/**
 * Meta Parser Exception is thrown by the meta parser if there are syntax errors
 * in the meta information
 * @author rwei
 */
public class MetaParserException extends Exception {

  /**
   * Serial version uid
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   * @param message the error message
   * @param errors the syntax errors
   */
  MetaParserException(String message, List<SyntaxError> errors) {
    super(buildErrorMessage(message, errors));
  }

  /**
   * Builds the exception message
   * @param message error message
   * @param errors syntax errors
   * @return exception message
   */
  private static String buildErrorMessage(String message,
          List<SyntaxError> errors) {
    StringBuilder builder = new StringBuilder();
    builder.append(message);
    for (SyntaxError error : errors) {
      builder.append('\n');
      builder.append(error.toString());
    }
    return builder.toString();
  }
}
