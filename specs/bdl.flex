package de.hopp.generator.frontend;

import java_cup.runtime.*;

%%

%class BDLFileScanner
%unicode
%cup
%line
%column

%{
  StringBuffer string = new StringBuffer();

  private Symbol symbol(int type) {
    return new Symbol(type, yyline+1, yycolumn+1);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline+1, yycolumn+1, value);
  }
%}

%xstates code
%xstates medium
%xstates path

/* whitespaces */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

/* comments */
SingleLineComment = "//" {InputCharacter}* {LineTerminator}
 MultiLineComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
Comment = {SingleLineComment} | {MultiLineComment}

/* numbers and identifiers */
DecNumber       = 0 | [1-9][0-9]*
HexNumber       = 0 [x] [0-9a-f]*
VerPart         = 0 [0-9]+

Identifier      = [:jletter:] [[:jletterdigit:]\-_]*
%%

/* ignore whitespaces and comments */
{WhiteSpace}    { /* ignore */ }
{Comment}       { /* ignore */ }

","             { return symbol(sym.COMMA); }

/* Blocks */
"{"             { return symbol(sym.BEGIN); }
"}"             { return symbol(sym.END); }
"{:"            { yybegin(code);      // state switch to code block
                  return symbol(sym.CBEGIN); }
<code> {
  ":}"     { yybegin(YYINITIAL); // state switch back to default
                  return symbol(sym.CEND); }
  [.]*     { return symbol(sym.CODE, yytext()); }
}

"medium"        { yybegin(medium); // state switch to medium blcok
                  return symbol(sym.MEDIUM); }
<medium> {
  "{"           { string.setLength(0); }
  "}"           { yybegin(YYINITIAL); // state switch back to default
                  return symbol(sym.STR, string.toString()); }
  [^\n\r\"\\]+  { string.append(yytext()); }
  \\t           { string.append('\t'); }
  \\n           { string.append('\n'); }

  \\r           { string.append('\r'); }
  \\\"          { string.append('\"'); }
  \\            { string.append('\\'); }
}

<path> {
  [.]*          { yybegin(YYINITIAL); // state switch back to default
                  return symbol(sym.PATH, yytext()); }
}

/* Keywords */
"import"        { return symbol(sym.IMPORT); }

/* selected backends */
"host"          { return symbol(sym.HOST); }
"board"         { return symbol(sym.BOARD); }
"project"       { return symbol(sym.PROJECT); }

/* global options */
"swqueue"       { return symbol(sym.SWQUEUE); }
"hwqueue"       { return symbol(sym.HWQUEUE); }

/* medium related */
//"mac"         { return symbol(sym.MAC); }
//"ip"          { return symbol(sym.IP); }
//"mask"        { return symbol(sym.MASK); }
//"gate"        { return symbol(sym.GATE); }

/* core related */
"core"          { return symbol(sym.CORE); }
"source"        { yybegin(path); // state switch to path
                  return symbol(sym.SOURCE); }

/* port related */
"width"         { return symbol(sym.WIDTH); }
"in"            { return symbol(sym.IN); }
"out"           { return symbol(sym.OUT); }
"dual"          { return symbol(sym.DUAL); }
"poll"          { return symbol(sym.POLL); }

/* instance related */
"gpio"          { return symbol(sym.GPIO); }
"instance"      { return symbol(sym.INSTANCE); }
"bind"          { return symbol(sym.BIND); }
"cpu"           { return symbol(sym.CPU); }

"scheduler"     { return symbol(sym.SCHEDULER); }

/* identifiers */
{Identifier}    { return symbol(sym.ID, yytext()); }

/* literals */
{DecNumber}     { return symbol(sym.DEC, Integer.valueOf(yytext())); }
{HexNumber}     { return symbol(sym.HEX, yytext()); }
{VerPart}       { return symbol(sym.VER, yytext()); }
