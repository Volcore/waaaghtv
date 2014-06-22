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
public class RelayGameFinish implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGameFinish" );

    public int     gameid;
    public int     numpackets;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public RelayGameFinish( int gameid, int numpackets )
    {
        this.gameid = gameid;
        this.numpackets = numpackets;
    }

    public RelayGameFinish( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
        numpackets = in.getInt( );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+8);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GAMEFINISH );
            buf.putShort( length );
            buf.putInt( gameid );
            buf.putInt( numpackets);
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
        return "("+this.getClass( ).getName( )+": "+gameid+" "+numpackets+")";
    }
}


