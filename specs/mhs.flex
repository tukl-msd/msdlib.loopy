package de.hopp.generator.parser;

import java_cup.runtime.*;

%%

%class Lexer
%unicode
%cup
%line
%column

%{

  StringBuffer string = new StringBuffer();

  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}

/* comments */

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment        = "#" {InputCharacter}* {LineTerminator}
//Comment = "#" .* {LineTerminator}

DecNumber      = 0 | [1-9][0-9]*
HexNumber      = 0 [x] [0-9a-f]*
VerPart        = 0 [0-9]+

Identifier     = [:jletter:] [[:jletterdigit:]-]*
%%

/* ignore whitespaces and comments */
{WhiteSpace}    { /* ignore */ }
{Comment}       { /* ignore */ }

/* Block */
"BEGIN"         { return symbol(sym.BEGIN); }
"END"           { return symbol(sym.END); }

/* attributes */
"BUS_INTERFACE" { return symbol(sym.BUS_INTERFACE); }
"PARAMETER"     { return symbol(sym.PARAMETER); }
"PORT"          { return symbol(sym.PORT); }

/* operators */
"="             { return symbol(sym.EQ); }
"&"             { return symbol(sym.AND); }
"."             { return symbol(sym.DOT); }
"["             { return symbol(sym.OBR); }
"]"             { return symbol(sym.CBR); }
":"             { return symbol(sym.COL); }
","             { return symbol(sym.COM); }

/* identifiers */
{Identifier}    { return symbol(sym.ID, yytext()); }

/* literals */
{DecNumber}     { return symbol(sym.DEC, Integer.valueOf(yytext())); }
{HexNumber}     { return symbol(sym.HEX, yytext()); }
{VerPart}       { return symbol(sym.VER, yytext()); }

