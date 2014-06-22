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
public class RelayGameInfo implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGameInfo" );

    public int     gameid;
    public byte[]  gameInfo;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayGameInfo( int gameid, byte[] gameInfo ) throws ParseException
    {
        this.gameid = gameid ;
        this.gameInfo = gameInfo;
    }

    public RelayGameInfo( int gameid, String comment, String streamer, int length, String ad1, String ad2, int addelay, String additional, byte[] gameInfo ) throws ParseException
    {
        this.gameid = gameid;

        try {
            short l = (short)(comment.length( )+1+streamer.length( )+1+4+ad1.length( )+1+ad2.length( )+1+4+additional.length( )+1+gameInfo.length);

            this.gameInfo = new byte[ l ];

            IoBuffer buf = IoBuffer.wrap( this.gameInfo );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putString( comment, charsetEncoder );
            buf.put( (byte) 0 );
            buf.putString( streamer, charsetEncoder );
            buf.put( (byte) 0 );
            buf.putInt( length );
            buf.putString( ad1, charsetEncoder );
            buf.put( (byte) 0 );
            buf.putString( ad2, charsetEncoder );
            buf.put( (byte) 0 );
            buf.putInt( addelay );
            buf.putString( additional, charsetEncoder );
            buf.put( (byte) 0 );
            buf.put( gameInfo );
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
        }

    }

    public RelayGameInfo( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
        gameInfo = new byte[ in.remaining( ) ];
        in.get( gameInfo );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4+gameInfo.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GAMEINFO );
            buf.putShort( length );
            buf.putInt( gameid );
            buf.put( gameInfo );
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
        return "(RelayGameInfo: "+gameid+" "+" [info of size "+gameInfo.length+"] )";
    }
}

