/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina.messages;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.*;
    // mina
        import org.apache.mina.core.buffer.*;
    // java
        import java.util.*;
        import java.nio.*;
        import java.nio.charset.*;

/*******************************************************************************
         See wtvProtocol.h in docs.
 *******************************************************************************/
public class RecorderServerDetails implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderServerDetails" );

    public int     maxUpload;
    public int     serverTime;

    public RecorderServerDetails( int maxUpload, int serverTime)
    {
        this.maxUpload = maxUpload;
        this.serverTime = serverTime;
    }

    public RecorderServerDetails( IoBuffer in ) throws ParseException
    {
        this.maxUpload = in.getInt( );
        this.serverTime = in.getInt( );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+8);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_SERVERDETAILS );
            buf.putShort( length );
            buf.putInt( maxUpload );
            buf.putInt( serverTime );
            buf.flip( );
            return buf;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( ).getName( )+": "+e );
            e.printStackTrace( );
            return null;
        }
    }

    public String toString( )
    {
        return "("+this.getClass( ).getName( )+": "+maxUpload+" '"+serverTime+"')";
    }
}



