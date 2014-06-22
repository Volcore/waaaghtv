/** Copyright (C) 2008 Volker SchÃ¶nefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmina.wc3messages;

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
         See warProtocol.h in docs.
 *******************************************************************************/
public class NewServerBroadcast implements WC3Message
{
    static Logger   logger = LoggerFactory.getLogger( "NewServerBroadcast" );

    public int     gameTag;
    public int     gameVersion;
    public int     gameid;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public NewServerBroadcast( int gameTag, int gameVersion, int gameid )
    {
        this.gameTag = gameTag;
        this.gameVersion = gameVersion;
        this.gameid = gameid;
    }

    public NewServerBroadcast( IoBuffer in ) throws ParseException
    {
        gameTag = in.getInt( );
        gameVersion = in.getInt( );
        gameid = in.getInt( );
    }

    public byte[] assemble( )
    {
        try {
            short length = (short)(4+12);

            byte[] assembled = new byte[ length ];

            IoBuffer buf = IoBuffer.wrap( assembled );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.put( WC3Message.PRIMER );
            buf.put( WC3Message.UDPNEWSERVERBROADCAST );
            buf.putShort( length );
            buf.putInt( gameTag );
            buf.putInt( gameVersion );
            buf.putInt( gameid );
            buf.flip( );
            return assembled;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            e.printStackTrace( );
            return null;
        }
    }

    public String toString( )
    {
        return "("+this.getClass( ).getName( )+": "+gameTag+" "+gameVersion+" "+gameid+")";
    }
}



