/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.util;

// imports
    // java 
        import java.nio.*;
        import java.text.*;

public class Text
{
        public static boolean isPrintable( char c ) 
    	{
            if( c >= 32 && c<=126 ) 
                return true;
    
            return false;
        }

    /** Reads an ASCII line from a ByteBuffer. Note: this does not work well with encodings! */
        public static String readLine( ByteBuffer buf )
        {
            StringBuilder s = new StringBuilder( );
            char c = (char)buf.get( );
            while( c!='\n' && buf.hasRemaining( ) )
            {
                s.append( c );
                c = (char)buf.get( );
            }

            return s.toString( );
        }
}
