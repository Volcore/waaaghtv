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
public class RelayGamelist implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGamelist" );

    public int     timestamp;
    public int     numGames;
    public int     totalGames;
    public byte[]  list;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayGamelist( int timestamp, int numGames, int totalGames, byte[] list ) throws ParseException
    {
        this.timestamp = timestamp;
        this.numGames = numGames;
        this.totalGames = totalGames;
        this.list = list;
    }

    public RelayGamelist( IoBuffer in ) throws ParseException
    {
        timestamp = in.getInt( );
        numGames = in.getInt( );
        totalGames = in.getInt( );
        list = new byte[ in.remaining( ) ];
        in.get( list );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4+4+4+list.length);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GAMELIST );
            buf.putShort( length );
            buf.putInt( timestamp );
            buf.putInt( numGames );
            buf.putInt( totalGames );
            buf.put( list );
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
        return "(RelayGamelist: "+timestamp+" "+numGames+" "+totalGames+" [list of size "+list.length+"] )";
    }
}


