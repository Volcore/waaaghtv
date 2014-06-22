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
public class RelayGameDetailUpdate implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGameDetailUpdate" );

    public int     gameid;
    public byte[]  detail;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayGameDetailUpdate( int gameid, byte[] detail )
    {
        this.gameid = gameid;
        this.detail = detail;
    }

    public RelayGameDetailUpdate( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
        detail = new byte[ in.remaining( ) ];
        in.get( detail );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4+detail.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GAMEDETAILUPDATE );
            buf.putShort( length );
            buf.putInt( gameid );
            buf.put( detail );
            buf.flip( );
            return buf;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            e.printStackTrace( );
            return null;
        }
    }

    public String toString( )
    {
        return "("+this.getClass( )+": "+gameid+" "+detail.length+")";
    }
}


