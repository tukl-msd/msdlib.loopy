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

%states CODE, STRING

/* whitespaces */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]
StringCharacter = [^\r\n\"\\]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment}// | {DocumentationComment}
TraditionalComment = "/*" ~"*/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}
//DocumentationComment = "/**" ~"*/"

/* numbers and identifiers */
DecNumber       = 0 | [1-9][0-9]*
HexNumber       = [0-9a-f]*
VerPart         = "." [:jletterdigit:]+

Identifier      = [:jletter:] [[:jletterdigit:]\-_]*
%%

/* ignore whitespaces and comments */
<YYINITIAL> {WhiteSpace}    { /* ignore */ }
<YYINITIAL> {Comment}       { /* ignore */ }

/* Blocks */
<YYINITIAL> {
  "{"  { return symbol(sym.BEGIN); }
  "}"  { return symbol(sym.END); }
  "{:" { yybegin(CODE);      // state switch to code block
                   string.setLength(0);
                   return symbol(sym.CBEGIN); }
  ":}" { }
}

<CODE> {
  {Comment} {string.append(yytext()); } // append comments
  ":}"      { yybegin(YYINITIAL); return symbol(sym.CEND, string.toString()); } // switch state back, if :} out of comment occurs
  ({InputCharacter} | {WhiteSpace}) { string.append(yytext()); } // otherwise append
}

<STRING> {
  \"            { yybegin(YYINITIAL); return symbol(sym.STRING_LITERAL, string.toString()); }
  {StringCharacter}+ { string.append( yytext() ); }
}

<YYINITIAL> {
\"              { yybegin(STRING); string.setLength(0); }

","             { return symbol(sym.COMMA); }

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
"medium"        { return symbol(sym.MEDIUM); }
"mac"           { return symbol(sym.MAC);  }
"ip"            { return symbol(sym.IP);   }
"mask"          { return symbol(sym.MASK); }
"gate"          { return symbol(sym.GATE); }
"port"          { return symbol(sym.PORT); }

/* core related */
"core"          { return symbol(sym.CORE); }
"source"        { return symbol(sym.SOURCE); }

/* port related */
"port"          { return symbol(sym.PORT); }
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

//{HexNumber} ":" {HexNumber} ":" {HexNumber} { return symbol(sym.MACADDR, yytext()); }


/* identifiers */
{Identifier}    { return symbol(sym.ID, yytext()); }

/* literals */
{DecNumber}     { return symbol(sym.DEC, Integer.valueOf(yytext())); }
//{HexNumber}     { return symbol(sym.HEX, yytext()); }
{VerPart}       { return symbol(sym.VER, yytext()); }
. { System.err.println("Illegal character: "+yytext()+" in line "+(yyline+1)+" , column "+(yycolumn+1)); }
}
