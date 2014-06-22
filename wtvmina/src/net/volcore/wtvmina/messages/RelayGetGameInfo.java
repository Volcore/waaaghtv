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
        import java.nio.*;
        import java.nio.charset.*;

/*******************************************************************************
         See wtvProtocol.h in docs.
 *******************************************************************************/
public class RelayGetGameInfo implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "RelayGetGameInfo" );

    public int     gameid;

    public RelayGetGameInfo( int gameid ) throws ParseException
    {
        this.gameid = gameid;
    }

    public RelayGetGameInfo( IoBuffer in ) throws ParseException
    {
        gameid = in.getInt( );
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+4);
            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.RELAY_GETGAMEINFO );
            buf.putShort( length );
            buf.putInt( gameid );
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
        return "(RelayGetGameInfo: "+gameid+" )";
    }
}


