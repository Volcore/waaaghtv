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
public class ServerInfo implements WC3Message
{
    static Logger   logger = LoggerFactory.getLogger( "ServerInfo" );

    public int      gameTag;
    public int      gameVersion;
    public int      gameid;
    public int      timestamp;
    public String   gameName;
    public byte[]   rules;
    public int      mapCheck;
    public String   mapName;
    public String   mapCreator;
    public int      numSlots;
    public int      gameType;
    public int      slotsUsed;
    public int      slotsTotal;
    public int      uptime;
    public short    port;

    public final static int         GAMETYPE_CUSTOM = 1;
    public final static int         GAMETYPE_NORMAL = 9;
    public final static int         GAMETYPE_SAVED  = 0x200;

    protected final static CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final static CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public ServerInfo( int gameTag, int gameVersion, int gameid, int timestamp, String gameName, 
                    byte[] rules, int mapCheck, String mapName, String mapCreator, int numSlots, 
                    int gameType, int slotsUsed, int slotsTotal, int uptime, short port )
    {
        this.gameTag = gameTag;
        this.gameVersion = gameVersion;
        this.gameid = gameid;
        this.timestamp = timestamp;
        this.gameName = gameName;
        this.rules = rules;
        if( rules.length != 12 )
            throw new Error( "Invalid rule length! Should be 12!" );
        this.mapCheck = mapCheck;
        this.mapName = mapName;
        this.mapCreator = mapCreator;
        this.numSlots = numSlots;
        this.gameType = gameType;
        this.slotsUsed = slotsUsed;
        this.slotsTotal = slotsTotal;
        this.uptime = uptime;
        this.port = port;
    }

    public static final void encodeStrings( String string1, String string2, IoBuffer out ) throws Exception
    {
        // Prepare the original unencoded buffer.
            IoBuffer buf2 = IoBuffer.allocate( string1.length( ) + string2.length( ) +8 );
            buf2.setAutoExpand( true );

            buf2.putString( string1, charsetEncoder );
            buf2.put( (byte) 0 );
            buf2.putString( string2, charsetEncoder );
            buf2.put( (byte) 0 );
            buf2.put( (byte) 0 );

            buf2.flip( );

        // Write to the encoded buffer.
            int len = buf2.remaining( );

            if( len > 0 )
                out.put( buf2.get( ) );

            int pos = 0;
            for( int i=1; i<len; ++i )
            {
                if( (pos&7) == 0 )
                {
                    // compute the bitmask
                    byte mask = 1;
                    pos = 1;
                    buf2.mark( );

                    for( int j=0; j<7; ++j )
                        if( buf2.hasRemaining( ) )
                            if( ( buf2.get( )&1 ) == 1 )
                                mask |= 1<<(j+1);

                    out.put( mask );

                    buf2.reset( );
                }

                byte next = buf2.get( );

                if( (next&1)==1 )
                    out.put( next );
                else
                    out.put( (byte) (next+1) );
                pos++;
            }

            out.put( (byte) 0 );
    }

    public static final String[] decodeStrings( IoBuffer in )
    {
        logger.error( "decodeStrings not implemented yet!" );
        return null;
    }

    public ServerInfo( IoBuffer in ) throws ParseException
    {
        try {
            this.gameTag        = in.getInt( );
            this.gameVersion    = in.getInt( );
            this.gameid         = in.getInt( );
            this.timestamp      = in.getInt( );
            this.gameName       = in.getString( charsetDecoder );
            this.rules          = new byte[12];
            in.get( this.rules );
            this.mapCheck       = in.getInt( );
            String[] str = decodeStrings( in );

            this.mapName = str[0];
            this.mapCreator = str[1];

            this.numSlots       = in.getInt( );
            this.gameType       = in.getInt( );
            this.slotsUsed      = in.getInt( );
            this.slotsTotal     = in.getInt( );
            this.uptime         = in.getInt( );
            this.port           = in.getShort( );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "Failed to parse ServerInfo: "+e );
        }
    }

    public byte[] assemble( )
    {
        try {
            IoBuffer buf = IoBuffer.allocate( 512 );
            buf.setAutoExpand( true );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.put( WC3Message.PRIMER );
            buf.put( WC3Message.UDPSERVERINFO );
            buf.putShort( (short) -1 );
            buf.putInt( gameTag );
            buf.putInt( gameVersion );
            buf.putInt( gameid );
            buf.putInt( timestamp );
            buf.putString( gameName, charsetEncoder );
            buf.put( (byte) 0 );
            buf.put( rules );
            buf.putInt( mapCheck );
            encodeStrings( mapName, mapCreator, buf );
            buf.putInt( numSlots );
            buf.putInt( gameType );
            buf.putInt( slotsUsed );
            buf.putInt( slotsTotal );
            buf.putInt( uptime );
            buf.putShort( port );

            short length = (short) buf.position( );
            buf.putShort( 2, length );
            buf.flip( );

            byte[] assembled = new byte[ buf.remaining( ) ];
            buf.get( assembled );

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
        return "("+this.getClass( ).getName( )+": "+gameTag+" "+gameVersion+" "+gameid+" "+timestamp+" "+gameName+" "+mapCheck+" "+mapName+" "+mapCreator+" "+numSlots+" "+gameType+" "+slotsUsed+" "+slotsTotal+" "+uptime+" "+port+")";
    }
}



