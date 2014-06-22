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
public class GenericError implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "GenericError" );

    public final static byte ERROR_CUSTOM = 0;
    public final static byte ERROR_LOGINDISALLOWED = 1;
    public final static byte ERROR_PASSWORD = 2;
    public final static byte ERROR_PACKETERROR = 3;
    public final static byte ERROR_WRONGVERSION = 4;
    public final static byte ERROR_ALREADYLOGGEDIN = 5;
    public final static byte ERROR_OUTOFORDER = 6;
    public final static byte ERROR_SERVERFULL = 7;
    public final static byte ERROR_SERVERERROR = 8;
    public final static byte ERROR_SEQUENCE = 9;

    public byte    code;
    public String  message;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public GenericError( byte code, String message )
    {
        this.code = code;
        this.message = message;
    }

    public GenericError( IoBuffer in ) throws ParseException
    {
        try {
            this.code = in.get( );
            this.message = in.getString( charsetDecoder );
        } catch( Exception e )
        {
            e.printStackTrace( );
            throw new ParseException( "failed to parse "+this.getClass( ).getName( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+1+message.length( )+1);

            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.GENERIC_ERROR );
            buf.putShort( length );
            buf.put( code );
            buf.putString( message, charsetEncoder );
            buf.put( (byte)0 );
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
        return "("+this.getClass( ).getName( )+": "+code+" '"+message+"')";
    }
}



