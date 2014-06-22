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
public class RecorderCreateGame implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderCreateGame" );

    public int      replyId;
    public int      gameId;
    public String   gameName;
    public String   gameComment;
    public int      date;
    public byte[]   gameInfo;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderCreateGame( int replyId, int gameId, String gameName, String gameComment, int date, byte[] gameInfo )
    {
        this.replyId = replyId;
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameComment = gameComment;
        this.date = date;
        this.gameInfo = gameInfo;
    }

    public RecorderCreateGame( IoBuffer in ) throws ParseException
    {
        try {
            this.replyId = in.getInt( );
            this.gameId = in.getInt( );
            this.gameName = in.getString( charsetDecoder );
            this.gameComment = in.getString( charsetDecoder );
            this.date = in.getInt( );
            this.gameInfo = new byte[ in.remaining( ) ];
            in.get( this.gameInfo );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+8+gameName.length( )+1+gameComment.length( )+1+4+gameInfo.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_CREATEGAME );
            buf.putShort( length );
            buf.putInt( replyId );
            buf.putInt( gameId );
            buf.putString( gameName, charsetEncoder );
            buf.put( (byte)0 );
            buf.putString( gameComment, charsetEncoder );
            buf.put( (byte)0 );
            buf.putInt( date );
            buf.put( gameInfo );
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
        return "("+this.getClass( ).getName( )+": "+replyId+" "+gameId+" "+gameName+" "+gameComment+" "+date+")";
    }
}



