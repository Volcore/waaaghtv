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
public class RecorderUpdateGameData implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderUpdateGameData" );

    public int      gameId;
    public int      lastPacket;
    public int      time;
    public int      size;
    public byte[]   data;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderUpdateGameData( int gameId, int lastPacket, int time, int size, byte[] data )
    {
        this.gameId = gameId;
        this.lastPacket = lastPacket;
        this.time = time;
        this.size = size;
        this.data = data;
    }

    public RecorderUpdateGameData( IoBuffer in ) throws ParseException
    {
        try {
            this.gameId = in.getInt( );
            this.lastPacket = in.getInt( );
            this.time = in.getInt( );
            this.size = in.getInt( );
            this.data = new byte[ in.remaining( ) ];
            in.get( this.data );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+16+data.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_UPDATEGAMEDATA );
            buf.putShort( length );
            buf.putInt( gameId );
            buf.putInt( lastPacket );
            buf.putInt( time );
            buf.putInt( size );
            buf.put( data );
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
        return "("+this.getClass( ).getName( )+": "+gameId+" "+lastPacket+" "+time+" "+size+")";
    }
}



