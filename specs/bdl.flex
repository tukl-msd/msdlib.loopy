package de.hopp.generator.frontend;

import java_cup.runtime.*;

%%

%class BDLFileScanner
%cupsym BDLFileSymbols
%cup
%unicode
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

%states CODE, STRING, BACKEND_START, BACKEND

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
//HexNumber       = [0-9a-f]*
VerPart         = "." [:jletterdigit:]+

Identifier      = [:jletter:] [[:jletterdigit:]\-_]*
%%

/* ignore whitespaces and comments */
<YYINITIAL> {WhiteSpace}    { /* ignore */ }
<YYINITIAL> {Comment}       { /* ignore */ }

/* Blocks */
<YYINITIAL> {
  "{"  { return symbol(BDLFileSymbols.BEGIN); }
  "}"  { return symbol(BDLFileSymbols.END); }
  "{:" { yybegin(CODE);      // state switch to code block
                   string.setLength(0);
                   return symbol(BDLFileSymbols.CBEGIN); }
  ":}" { }
}

<BACKEND_START> {
  {Comment} { /* ignore comments */ }
  "{" { yybegin(BACKEND); string.setLength(0); }
}

<BACKEND> {
  {Comment} { /* ignore comments */ }
  "}" { yybegin(YYINITIAL); return symbol(BDLFileSymbols.STRING_LITERAL, string.toString()); }
   ({InputCharacter} | {WhiteSpace}) { string.append(yytext()); } // otherwise append
}

<CODE> {
  {Comment} {string.append(yytext()); } // append comments. They may also include :} symbols
  ":}"      { yybegin(YYINITIAL); return symbol(BDLFileSymbols.CEND, string.toString()); } // switch state back, if :} out of comment occurs
  ({InputCharacter} | {WhiteSpace}) { string.append(yytext()); } // otherwise append
}

<STRING> {
  \"            { yybegin(YYINITIAL); return symbol(BDLFileSymbols.STRING_LITERAL, string.toString()); }
  {StringCharacter}+ { string.append( yytext() ); }
 {LineTerminator} { System.err.println("Unterminated String: "+yytext()+" in line "+(yyline+1)+" , column "+(yycolumn+1)); }
}

<YYINITIAL> {
\"              { yybegin(STRING); string.setLength(0); }
","             { return symbol(BDLFileSymbols.COMMA); }

/* Keywords */
"import"        { return symbol(BDLFileSymbols.IMPORT); }

/* selected backends */
"backend"       { yybegin(BACKEND_START); }

/* global options */
"log"           { return symbol(BDLFileSymbols.LOG);     }
"swqueue"       { return symbol(BDLFileSymbols.SWQUEUE); }
"hwqueue"       { return symbol(BDLFileSymbols.HWQUEUE); }

/* medium related */
"medium"        { return symbol(BDLFileSymbols.MEDIUM); }
"mac"           { return symbol(BDLFileSymbols.MAC);  }
"ip"            { return symbol(BDLFileSymbols.IP);   }
"mask"          { return symbol(BDLFileSymbols.MASK); }
"gate"          { return symbol(BDLFileSymbols.GATE); }
"port"          { return symbol(BDLFileSymbols.PORT); }
"timeout"       { return symbol(BDLFileSymbols.TIMEOUT); }
"dhcp"          { return symbol(BDLFileSymbols.DHCP); }

/* core related */
"core"          { return symbol(BDLFileSymbols.CORE); }
"source"        { return symbol(BDLFileSymbols.SOURCE); }

/* port related */
"port"          { return symbol(BDLFileSymbols.PORT); }
"clk"           { return symbol(BDLFileSymbols.CLK); }
"rst"           { return symbol(BDLFileSymbols.RST); }
"width"         { return symbol(BDLFileSymbols.WIDTH); }
"in"            { return symbol(BDLFileSymbols.IN); }
"out"           { return symbol(BDLFileSymbols.OUT); }
"dual"          { return symbol(BDLFileSymbols.DUAL); }
"poll"          { return symbol(BDLFileSymbols.POLL); }

/* instance related */
"gpio"          { return symbol(BDLFileSymbols.GPIO); }
"instance"      { return symbol(BDLFileSymbols.INSTANCE); }
"bind"          { return symbol(BDLFileSymbols.BIND); }
"cpu"           { return symbol(BDLFileSymbols.CPU); }

"scheduler"     { return symbol(BDLFileSymbols.SCHEDULER); }

//{HexNumber} ":" {HexNumber} ":" {HexNumber} { return symbol(BDLFileSymbols.MACADDR, yytext()); }


/* identifiers */
{Identifier}    { return symbol(BDLFileSymbols.ID, yytext()); }

/* literals */
{DecNumber}     { return symbol(BDLFileSymbols.DEC, Integer.valueOf(yytext())); }
//{HexNumber}     { return symbol(BDLFileSymbols.HEX, yytext()); }
{VerPart}       { return symbol(BDLFileSymbols.VER, yytext()); }
. { System.err.println("Illegal character: "+yytext()+" in line "+(yyline+1)+" , column "+(yycolumn+1)); }
}
