/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.atlas.json.io;


import static org.openjena.atlas.lib.Chars.CH_QUOTE1 ;
import static org.openjena.atlas.lib.Chars.CH_QUOTE2 ;

import java.io.OutputStream ;
import java.util.Stack ;

import org.openjena.atlas.io.IndentedLineBuffer ;
import org.openjena.atlas.io.IndentedWriter ;
import org.openjena.atlas.lib.BitsInt ;
import org.openjena.atlas.lib.Ref ;


/** A low level streaming JSON writer - assumes correct sequence of calls (e.g. keys in objects).
 * Useful when writing JSON directly from some other structure 
 */

public class JSWriter
{
    private IndentedWriter out = IndentedWriter.stdout ;
    
    public JSWriter() { this(IndentedWriter.stdout) ; }
    public JSWriter(OutputStream ps) { this(new IndentedWriter(ps)) ; }
    public JSWriter(IndentedWriter ps) { out = ps ; }
    
    public void startOutput() {}
    public void finishOutput() { out.flush(); } 
    
    // Remember whether we are in the first element of a compound (object or array). 
    Stack<Ref<Boolean>> stack = new Stack<Ref<Boolean>>() ;
    
    public void startObject()
    {
        startCompound() ;
        out.print("{ ") ;
        out.incIndent() ;
    }
    
    public void finishObject()
    {
        out.decIndent() ;
        if ( isFirst() )
            out.print("}") ;
        else
        {
            out.ensureStartOfLine() ;
            out.println("}") ;
        }
        finishCompound() ;
    }
    
    public void key(String key)
    {
        if ( isFirst() )
        {
            out.println();
            setNotFirst() ;
        }
        else
            out.println(" ,") ;
        value(key) ;
        out.print(" : ") ;
        // Ready to start the pair value.
    }
    
    // "Pair" is the name used in the JSON spec. 
    public void pair(String key, String value)
    {
        key(key) ;
        value(value) ;
    }
    
     
    public void pair(String key, boolean val)
    {
        key(key) ;
        value(val) ;
    }

    public void pair(String key, long val)
    {
        key(key) ;
        value(val) ;
    }


    public void startArray()
    {
        startCompound() ;
        out.print("[ ") ;
        // Messy with objects out.incIndent() ;
    }
    
    public void finishArray()
    {

//        out.decIndent() ;
        out.print(" ]") ;       // Leave on same line.
        finishCompound() ;
    }

    public void arrayElement(String str)
    {
        arrayElementprocess() ;
        value(str) ;
    }

    private void arrayElementprocess()
    {
        if ( isFirst() )
            setNotFirst() ;
        else
            out.print(", ") ;
    }
    
    public void arrayElement(boolean b)
    {
        arrayElementprocess() ;
        value(b) ;
    }
    public void arrayElement(long integer)
    {
        arrayElementprocess() ;
        value(integer) ;
    }
    
    public static String outputQuotedString(String string)
    {
        IndentedLineBuffer b = new IndentedLineBuffer() ;
        outputQuotedString(b, string) ;
        return b.asString() ;
    }
    
    static private boolean writeJavaScript = false ;
    
    /* \"  \\ \/ \b \f \n \r \t
     * control characters (def?) 
     * \ u four-hex-digits (if
     *  you don't know why the comment writes "\ u", 
     *  and not without space then ... */
    public static void outputQuotedString(IndentedWriter out, String string)
    { 
        final boolean allowBareWords = writeJavaScript ;
        
        char quoteChar = CH_QUOTE2 ;
        int len = string.length() ;
        
        if ( allowBareWords )
        {
            boolean safeBareWord = true ;
            if ( len != 0 )
                safeBareWord = isA2Z(string.charAt(0)) ;

            if ( safeBareWord )
            {
                for (int i = 1; i < len; i++)
                {
                    char ch = string.charAt(i);
                    if ( isA2ZN(ch) ) continue ;
                    safeBareWord = false ;
                    break ;
                }
            }
            if ( safeBareWord )
            {
                // It's safe as a bare word in JavaScript.
                out.print(string) ;
                return ;
            }
        }

        if ( allowBareWords )
            quoteChar = CH_QUOTE1 ;
        
        out.print(quoteChar) ;
        for (int i = 0; i < len; i++)
        {
            char ch = string.charAt(i);
            if ( ch == quoteChar )
            {
                esc(out, quoteChar) ;
                continue ;
            }
            
            switch (ch)
            {
                case '"':   esc(out, '"') ; break ;
                case '\'':   esc(out, '\'') ; break ;
                case '\\':  esc(out, '\\') ; break ;
                case '/':
                    // Avoid </ which confuses if it's in HTML (this is from json.org)
                    if ( i > 0 && string.charAt(i-1) == '<' )
                        esc(out, '/') ;
                    else
                        out.print(ch) ;
                    break ;
                case '\b':  esc(out, 'b') ; break ;
                case '\f':  esc(out, 'f') ; break ;
                case '\n':  esc(out, 'n') ; break ;
                case '\r':  esc(out, 'r') ; break ;
                case '\t':  esc(out, 't') ; break ;
                default:
                    
                    //Character.isISOControl(ch) ; //00-1F, 7F-9F
                    // This is more than Character.isISOControl
                    
                    if (ch < ' ' || 
                        (ch >= '\u007F' && ch <= '\u009F') ||
                        (ch >= '\u2000' && ch < '\u2100'))
                    {
                        out.print("\\u") ;
                        int x = ch ;
                        x = oneHex(out, x, 3) ;
                        x = oneHex(out, x, 2) ;
                        x = oneHex(out, x, 1) ;
                        x = oneHex(out, x, 0) ;
                        break ;
                    }
                        
                    out.print(ch) ;
                    break ;
            }
        }
        out.print(quoteChar) ;
    }
    
    

    
    private void startCompound()    { stack.push(new Ref<Boolean>(true)) ; }
    private void finishCompound()   { stack.pop(); }
    private boolean isFirst()   { return stack.peek().getValue() ; }
    private void setNotFirst()  { stack.peek().setValue(false) ; }
    
    // Can only write a value in some context.
    private void value(String x) { out.print("\"") ; out.print(x) ; out.print("\"") ; }
    
    private void value(boolean b) { out.print(Boolean.toString(b)) ; }
    
    private void value(long integer) { out.print(Long.toString(integer)) ; }
    
//    void valueString(String image) {}
//    void valueInteger(String image) {}
//    void valueDouble(String image) {}
//    void valueBoolean(boolean b) {}
//    void valueNull() {}
//    void valueDecimal(String image) {}

    // Library-ize.
    
    private static boolean isA2Z(int ch)
    {
        return range(ch, 'a', 'z') || range(ch, 'A', 'Z') ;
    }

    private static boolean isA2ZN(int ch)
    {
        return range(ch, 'a', 'z') || range(ch, 'A', 'Z') || range(ch, '0', '9') ;
    }

    private static boolean isNumeric(int ch)
    {
        return range(ch, '0', '9') ;
    }
    
    private static boolean isWhitespace(int ch)
    {
        return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '\f' ;    
    }
    
    private static boolean isNewlineChar(int ch)
    {
        return ch == '\r' || ch == '\n' ;
    }
    
    private static boolean range(int ch, char a, char b)
    {
        return ( ch >= a && ch <= b ) ;
    }
    
    private static void esc(IndentedWriter out, char ch)
    {
        out.print('\\') ; out.print(ch) ; 
    }
    
    private static int oneHex(IndentedWriter out, int x, int i)
    {
        int y = BitsInt.unpack(x, 4*i, 4*i+4) ;
        char charHex = org.openjena.atlas.lib.Chars.hexDigits[y] ;
        out.print(charHex) ; 
        return BitsInt.clear(x, 4*i, 4*i+4) ;
    }
    

}

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */