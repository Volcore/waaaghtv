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
public class RelayUpdateGameDataReply implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayUpdateGameDataReply" );

    public int     gameid;
    public int     blockid;
    public int     totalsize;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayUpdateGameDataReply( int gameid, int blockid, int totalsize ) throws ParseException
    {
        this.gameid = gameid;
        this.blockid = blockid;
        this.totalsize = totalsize;
    }

    public RelayUpdateGameDataReply( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
        blockid = in.getInt( );
        totalsize = in.getInt( );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+12);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_UPDATEGAMEDATAREPLY );
            buf.putShort( length );
            buf.putInt( gameid );
            buf.putInt( blockid );
            buf.putInt( totalsize );
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
        return "("+this.getClass( ).getName( )+": "+gameid+" "+blockid+" "+totalsize+")";
    }
}


