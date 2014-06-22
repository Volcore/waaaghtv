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
public class RecorderDetailUpdate implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderDetailUpdate" );

    public int      gameId;
    public byte[]   details;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RecorderDetailUpdate( int gameId, byte[] details )
    {
        this.gameId = gameId;
        this.details = details;
    }

    public RecorderDetailUpdate( IoBuffer in ) throws ParseException
    {
        try {
            this.gameId = in.getInt( );
            this.details = new byte[ in.remaining( ) ];
            in.get( this.details );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4+details.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RECORDER_DETAILUPDATE );
            buf.putShort( length );
            buf.putInt( gameId );
            buf.put( details );
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
        return "("+this.getClass( ).getName( )+": "+gameId+")";
    }
}



