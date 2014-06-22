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
        import java.nio.*;
        import java.nio.charset.*;

/*******************************************************************************
         See wtvProtocol.h in docs.
 *******************************************************************************/
public class RelayGetGamelist implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGetGamelist" );

    public int     timestamp;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayGetGamelist( int timestamp ) throws ParseException
    {
        this.timestamp = timestamp;
    }

    public RelayGetGamelist( IoBuffer in ) throws ParseException
    {
        timestamp = in.getInt( );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4);
            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GETGAMELIST );
            buf.putShort( length );
            buf.putInt( timestamp );
            buf.flip( );
            return buf;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            return null;
        }
    }

    public String toString( )
    {
        return "(RelayGetGamelist: "+timestamp+" )";
    }
}

