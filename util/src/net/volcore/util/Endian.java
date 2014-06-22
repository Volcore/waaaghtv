/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // java
        import java.security.*;

public class Endian
{
    static Logger   logger = LoggerFactory.getLogger( "Endian" );

    public static int swapInt( int v )
    {
        int b1 = (v >> 0  ) & 0xff;
        int b2 = (v >> 8  ) & 0xff;
        int b3 = (v >> 16 ) & 0xff;
        int b4 = (v >> 24 ) & 0xff;

        return b1 << 24 
             | b2 << 16 
             | b3 << 8
             | b4 << 0;
    }

    public static short swapShort( short v )
    {
        int b1 = (v >> 0  ) & 0xff;
        int b2 = (v >> 8  ) & 0xff;

        return (short)(b1 <<  8 
             | b2 <<  0);
    }
}


