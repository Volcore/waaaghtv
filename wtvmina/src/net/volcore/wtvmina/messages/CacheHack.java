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
public class CacheHack implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "CacheHack" );

    public String   cacheInfo;
    public byte     major;
    public short    minor;
    public byte     release;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public CacheHack( String cacheInfo, byte major, short minor, byte release )
    {
        this.cacheInfo = cacheInfo;
        this.major = major;
        this.minor = minor;
        this.release = release;
    }

    public CacheHack( IoBuffer in ) throws ParseException
    {
        try {
            this.cacheInfo = in.getString( charsetDecoder );
            this.release = in.get( );
            this.minor = in.getShort( );
            this.major = in.get( );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+cacheInfo.length( )+1+4);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.CACHE_HACK );
            buf.putShort( length );
            buf.putString( cacheInfo, charsetEncoder );
            buf.put( (byte)0 );
            buf.put( release );
            buf.putShort( minor );
            buf.put( major);
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
        return "("+this.getClass( ).getName( )+": '"+cacheInfo+"' "+major+"."+minor+"."+release+")";
    }
}




