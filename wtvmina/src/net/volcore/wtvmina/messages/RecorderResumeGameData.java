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
public class RecorderResumeGameData implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderResumeGameData" );

    public int      gameId;
    public int      numBytes;
    public int      lastTime;
    public int      lastSize;
    public byte[]   data;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderResumeGameData( int gameId, int numBytes, int lastTime, int lastSize, byte[] data )
    {
        this.gameId = gameId;
        this.numBytes = numBytes;
        this.lastTime = lastTime;
        this.lastSize = lastSize;
        this.data = data;
    }

    public RecorderResumeGameData( IoBuffer in ) throws ParseException
    {
        try {
            this.gameId = in.getInt( );
            this.numBytes = in.getInt( );
            this.lastTime = in.getInt( );
            this.lastSize = in.getInt( );
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
            buf.put( WTVMessageFactory.RECORDER_RESUMEGAMEDATA );
            buf.putShort( length );
            buf.putInt( gameId );
            buf.putInt( numBytes );
            buf.putInt( lastTime );
            buf.putInt( lastSize );
            buf.put( data);
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
        return "("+this.getClass( ).getName( )+": "+gameId+" "+numBytes+" "+lastTime+" "+lastSize+")";
    }
}



