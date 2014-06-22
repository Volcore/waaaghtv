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
public class RecorderGameStart implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderGameStart" );

    public int      gameId;
    public int      lastSeed;
    public int      delay;
    public int      date;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderGameStart( int gameId, int lastSeed, int delay, int date )
    {
        this.gameId = gameId;
        this.lastSeed = lastSeed;
        this.delay = delay;
        this.date = date;
    }

    public RecorderGameStart( IoBuffer in ) throws ParseException
    {
        try {
            this.gameId = in.getInt( );
            this.lastSeed = in.getInt( );
            this.delay = in.getInt( );
            this.date = in.getInt( );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+16);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_CREATEGAME );
            buf.putShort( length );
            buf.putInt( gameId );
            buf.putInt( lastSeed );
            buf.putInt( delay );
            buf.putInt( date );
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
        return "("+this.getClass( ).getName( )+": "+gameId+" "+lastSeed+" "+delay+" "+date+")";
    }
}



