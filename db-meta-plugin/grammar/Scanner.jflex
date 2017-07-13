package ch.ivyteam.db.meta.parser.internal;

import java_cup.runtime.*;

import java.math.BigDecimal;
import static ch.ivyteam.db.meta.parser.internal.Symbols.*;

/** 
 * This class is a scanner for IvyScript.
 */
@SuppressWarnings("all")

%%
%class Scanner
%public
%unicode
%cupsym Symbols
%cup
%char
%eofval{
  return symbol(EOF, END_OF_FILE);
%eofval}
%{

  /** A string builder for scanning String-literals. */
  private StringBuilder string = new StringBuilder();
  /** The start-position of the String. */
  private int stringStart;
  
  /** Message */ 
  private static final String END_OF_FILE="End of File";
  
  /** Message */
  private static final String ILLEGAL_CHARACTER="Illegal Character";
  
  /** Message */
  private static final String ILLEGAL_STRING_CONSTANTS="Illegal String Constants";
  
  /**
   * Creates a symbol without value.
   * @param type The type of the symbol.
   * @return A Symbol.
   */
  private Symbol symbol(int type, String name) 
  {
    return new TerminalSymbol(type, name, yyline+1, yycolumn);
  }
  
  /**
   * Creates a symbol with value.
   * @param type The type of the symbol.
   * @param value The value of the symbol.
   * @return A Symbol.
   */
  private Symbol symbol(int type, String name, Object value, int length) 
  {
    return new TerminalSymbol(type, name, yyline+1, yycolumn, value);
  }
    
  /** 
   * This method is called if the scanner cannot scan the input
   */
  private Symbol fail()
  {
    if (yystate() == READ_STRING)
    {
      return symbol(ILLEGAL, ILLEGAL_STRING_CONSTANTS, yytext(), 0);
    } 
    else 
    {
      return symbol(ILLEGAL, ILLEGAL_CHARACTER, yytext(), yytext().length());
    }
  }
  

%}
LineTerminator   = \r|\n|\r\n
WhiteSpace       = {LineTerminator} | [ \t\f]

/* comments */
Comment     = "--" [^\r\n]*

Identifier = [:jletter:] [:jletterdigit:]*

IntegerLiteral     =  [0-9]+
NumberLiteral      =  [0-9]+ "." [0-9]* | "." [0-9]+

%state READ_STRING

%%

<YYINITIAL> {

  /* keywords */
  "CREATE"     { return symbol(CREATE, yytext()); }
  "INSERT"     { return symbol(INSERT, yytext()); }
  "TABLE"      { return symbol(TABLE, yytext()); }
  "VIEW"       { return symbol(VIEW, yytext()); }
  "INDEX"      { return symbol(INDEX, yytext()); }
  "INTEGER"    { return symbol(INTEGER, yytext()); }
  "BIGINT"     { return symbol(BIGINT, yytext()); }
  "VARCHAR"    { return symbol(VARCHAR, yytext()); }
  "BIT"        { return symbol(BIT, yytext()); }
  "BLOB"       { return symbol(BLOB, yytext()); }
  "CLOB"       { return symbol(CLOB, yytext()); }
  "CHAR"       { return symbol(CHAR, yytext()); }
  "DATETIME"   { return symbol(DATETIME, yytext()); }
  "DATE"       { return symbol(DATE, yytext()); }
  "TIME"       { return symbol(TIME, yytext()); }
  "NUMBER"     { return symbol(NUMBER, yytext()); }
  "DECIMAL"    { return symbol(DECIMAL, yytext()); }
  "FLOAT"      { return symbol(FLOAT, yytext()); }
  "PRIMARY"    { return symbol(PRIMARY, yytext()); }
  "FOREIGN"    { return symbol(FOREIGN, yytext()); }
  "KEY"        { return symbol(KEY, yytext()); }
  "REFERENCES" { return symbol(REFERENCES, yytext()); }
  "ON"         { return symbol(ON, yytext()); }
  "DELETE"     { return symbol(DELETE, yytext()); }
  "SET"        { return symbol(SET, yytext()); }
  "CASCADE"    { return symbol(CASCADE, yytext()); }
  "NULL"       { return symbol(NULL, yytext()); }
  "NOT"        { return symbol(NOT, yytext()); }
  "DEFAULT"    { return symbol(DEFAULT, yytext()); }
  "UNIQUE"     { return symbol(UNIQUE, yytext()); }
  "INTO"       { return symbol(INTO, yytext()); }
  "VALUES"     { return symbol(VALUES, yytext()); }
  "FOR"        { return symbol(FOR, yytext()); }
  "USE"        { return symbol(USE, yytext()); }
  "AS"         { return symbol(AS, yytext()); }
  "SELECT"     { return symbol(SELECT, yytext()); }
  "FROM"       { return symbol(FROM, yytext()); }
  "WHERE"      { return symbol(WHERE, yytext()); }
  "AND"        { return symbol(AND, yytext()); }
  "OR"         { return symbol(OR, yytext()); }
  "CASE"       { return symbol(CASE, yytext()); }
  "WHEN"       { return symbol(WHEN, yytext()); }
  "THEN"       { return symbol(THEN, yytext()); }
  "END"        { return symbol(END, yytext()); }
  "ELSE"       { return symbol(ELSE, yytext()); }
  "THIS"       { return symbol(THIS, yytext()); }
  "UPDATE"     { return symbol(UPDATE, yytext()); }
  "AFTER"      { return symbol(AFTER, yytext()); }
  "BEGIN"      { return symbol(BEGIN, yytext()); }
  "TRIGGER"    { return symbol(TRIGGER, yytext()); }
  "IS"         { return symbol(IS, yytext()); }
  "EXECUTE"    { return symbol(EXECUTE, yytext()); }
  "EACH"       { return symbol(EACH, yytext()); }
  "ROW"        { return symbol(ROW, yytext()); }
  "STATEMENT"  { return symbol(STATEMENT, yytext()); }
  "UNION"      { return symbol(UNION, yytext()); }
  "ALL"        { return symbol(ALL, yytext()); }
  "JOIN"       { return symbol(JOIN, yytext()); }
  "INNER"      { return symbol(INNER, yytext()); }
  "OUTER"      { return symbol(OUTER, yytext()); }
  "FULL"       { return symbol(FULL, yytext()); }
  "LEFT"       { return symbol(LEFT, yytext()); }
  "RIGHT"      { return symbol(RIGHT, yytext()); }
  
  /* identifiers */ 
  {Identifier}        { return symbol(IDENTIFIER, "Identifier", yytext(), yytext().length()); }
 
  /* literals */
  {IntegerLiteral}    { return symbol(INTEGER_LITERAL, "Integer", Integer.parseInt(yytext()), yytext().length()); }
  {NumberLiteral}     { return symbol(NUMBER_LITERAL, "Number", new BigDecimal(yytext()), yytext().length()); }
  
  /* parenthesis */
  "("                 { return symbol(L_PARENT, yytext()); }
  ")"                 { return symbol(R_PARENT, yytext()); }
  
  /* operators */ 
  ";"                { return symbol(SEMICOLON, yytext()); }
  ","                { return symbol(COMMA, yytext());     }
  "="                { return symbol(EQUAL, yytext());     }
  "<>"               { return symbol(NOT_EQUAL, yytext());     }
  "<"                { return symbol(LESS, yytext());     }
  "<="               { return symbol(LESS_EQUAL, yytext()); }
  ">"                { return symbol(GREATER, yytext()); }
  ">="               { return symbol(GREATER_EQUAL, yytext()); }
  "."                { return symbol(DOT, yytext()); }
 
  /* comments */
  {Comment}          { 
                    String comment;
                    comment = yytext().substring(2);
                    return symbol(COMMENT, "Comment", comment, comment.length()); }
 
  /* whitespace */
  {LineTerminator}   { yyline++; }
  {WhiteSpace}       { /* ignore */ }

  \'                 { string.setLength(0); yybegin(READ_STRING); stringStart = yychar; }
}

<READ_STRING> {
  \'                 { yybegin(YYINITIAL); 
                       return symbol(STRING_LITERAL, "String", string.toString(), string.length());
                     }
  [^\']+             { string.append( yytext() ); }
 
}

/* error fallback */
.                    { return fail(); }
  
    
