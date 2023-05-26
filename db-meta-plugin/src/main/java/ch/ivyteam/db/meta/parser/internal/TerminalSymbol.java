package ch.ivyteam.db.meta.parser.internal;

import java_cup.runtime.Symbol;

/**
 * @author rwei
 * @since 15.09.2006
 */
@SuppressWarnings("all")
public class TerminalSymbol extends Symbol {

  /** The name of the symbol */
  private String name;

  /**
   * Constructor
   * @param _id
   * @param _name the name of the symbol
   * @param leftPosition
   * @param rightPosition
   * @param _value
   */
  public TerminalSymbol(int _id, String _name, int leftPosition, int rightPosition, Object _value) {
    super(_id, leftPosition, rightPosition, _value);
    name = _name;
  }

  /**
   * Constructor
   * @param _id
   * @param _name the name of the symbol
   * @param leftPosition
   * @param rightPosition
   */
  public TerminalSymbol(int _id, String _name, int leftPosition, int rightPosition) {
    super(_id, leftPosition, rightPosition);
    name = _name;
  }

  /**
   * Constructor
   * @param _id
   * @param _name the name of the symbol
   * @param _value
   */
  public TerminalSymbol(int _id, String _name, Object _value) {
    super(_id, _value);
    name = _name;
  }

  /**
   * Constructor
   * @param arg0
   * @param _name the name of the symbol
   */
  public TerminalSymbol(int arg0, String _name) {
    super(arg0);
    name = _name;
  }

  /**
   * Gets the name of the symbol
   * @return symbol name
   */
  public String getName() {
    return name;
  }
}
