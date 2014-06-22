/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // java
        import java.security.*;

public class Checksum
{
    static Logger   logger = LoggerFactory.getLogger( "Checksum" );
    public static String getMD5String( byte[] b )
    {
        try{
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update( b, 0, b.length );
    
            byte[] d = m.digest( );
            StringBuilder sb = new StringBuilder( );
    
            for( int i=0; i<d.length; ++i )
                sb.append( HexDump.printHex( d[i] ) );
    
            return sb.toString( );
        } catch( Exception e ) {
            logger.error( "Failed to compute checksum: "+e ); 
            return null; 
        }
    }
}





