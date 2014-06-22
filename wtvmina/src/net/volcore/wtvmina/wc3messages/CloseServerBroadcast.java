/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmina.wc3messages;

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
         See warProtocol.h in docs.
 *******************************************************************************/
public class CloseServerBroadcast implements WC3Message
{
    static Logger   logger = LoggerFactory.getLogger( "CloseServerBroadcast" );

    public int     gameid;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public CloseServerBroadcast( int gameid )
    {
        this.gameid = gameid;
    }

    public CloseServerBroadcast( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
    }

    public byte[] assemble( )
    {
        try {
            short length = (short)(4+4);

            byte[] assembled = new byte[ length ];

            IoBuffer buf = IoBuffer.wrap( assembled );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.put( WC3Message.PRIMER );
            buf.put( WC3Message.UDPCLOSESERVERBROADCAST );
            buf.putShort( length );
            buf.putInt( gameid );
            buf.flip( );
            return assembled;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            e.printStackTrace( );
            return null;
        }
    }

    public String toString( )
    {
        return "("+this.getClass( ).getName( )+": "+gameid+")";
    }
}


