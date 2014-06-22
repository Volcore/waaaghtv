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
public class RecorderGameCreated implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderGameCreated" );

    public final static int         RESULT_CREATED      = 0;
    public final static int         RESULT_INCOMPLETE   = 1;
    public final static int         RESULT_FINISHED     = 2;
    public final static int         RESULT_NAMEEXISTS   = 3;
    public final static int         RESULT_FAILED       = 4;

    public int      replyId;
    public int      gameId;
    public int      result;
    public int      lastSeed;
    public int      allSize;
    public int      lastSize;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderGameCreated( int replyId, int gameId, int result, int lastSeed, int allSize, int lastSize )
    {
        this.replyId = replyId;
        this.gameId = gameId;
        this.result = result;
        this.lastSeed = lastSeed;
        this.allSize = allSize;
        this.lastSize = lastSize;
    }

    public RecorderGameCreated( IoBuffer in ) throws ParseException
    {
        try {
            this.replyId    = in.getInt( );
            this.gameId     = in.getInt( );
            this.result     = in.getInt( );
            this.lastSeed   = in.getInt( );
            this.allSize    = in.getInt( );
            this.lastSize   = in.getInt( );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+6*4);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_GAMECREATED );
            buf.putShort( length );
            buf.putInt( replyId );
            buf.putInt( gameId );
            buf.putInt( result );
            buf.putInt( lastSeed );
            buf.putInt( allSize );
            buf.putInt( lastSize );
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
        return "("+this.getClass( ).getName( )+": "+replyId+" "+gameId+" "+result+" "+lastSeed+" "+allSize+" "+lastSize+")";
    }
}



