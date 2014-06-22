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
public class LogonAuthorize implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "LogonAuthorize" );

    public final static byte CLIENTTYPE_UNKNOWN  = 0;
    public final static byte CLIENTTYPE_CLIENT   = 1;
    public final static byte CLIENTTYPE_RECORDER = 2;
    public final static byte CLIENTTYPE_RELAY    = 3;

    public String  appName;
    public byte    appRelease;
    public short   appMinor;
    public byte    appMajor;
    public byte    clientType;
    public byte    protoRelease;
    public short   protoMinor;
    public byte    protoMajor;
    public String  userName;
    public String  password;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public LogonAuthorize( String appName, byte appMajor, short appMinor, byte appRelease, byte clientType, byte protoMajor, short protoMinor, byte protoRelease, String userName, String password ) throws ParseException
    {
        this.appName = appName;
        this.appRelease = appRelease;
        this.appMinor = appMinor;
        this.appMajor = appMajor;
        this.clientType = clientType;
        this.protoRelease = protoRelease;
        this.protoMinor = protoMinor;
        this.protoMajor = protoMajor;
        this.userName = userName;
        this.password = password;
    }

    public LogonAuthorize( IoBuffer in ) throws ParseException
    {
        try {
            appName = in.getString( charsetDecoder );
            appRelease = in.get( );
            appMinor = in.getShort( );
            appMajor = in.get( );
            clientType = in.get( );
            protoRelease = in.get( );
            protoMinor = in.getShort( );
            protoMajor = in.get( );
            userName = "John Doe";
            password = "nopass";
        } catch( CharacterCodingException e )
        {
            throw new ParseException( "Failed to parse "+this.getClass( )+": "+e );
        }

        try {
            userName = in.getString( charsetDecoder );
            password = in.getString( charsetDecoder );
        } catch( CharacterCodingException e )
        {
            logger.warn( "Failed to parse username with charset decoder, assuming John Doe." );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            short length = (short)(5+appName.length( )+1+4+1+4+userName.length( )+1+password.length( )+1);
            IoBuffer buf = IoBuffer.allocate( length );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.putShort( WTVProtocolDecoder.PRIMER );
            buf.put( WTVMessageFactory.LOGON_AUTHORIZE );
            buf.putShort( length );
            buf.putString( appName, charsetEncoder );
            buf.put( (byte) 0 );
            buf.put( appRelease );
            buf.putShort( appMinor );
            buf.put( appMajor );
            buf.put( clientType );
            buf.put( protoRelease );
            buf.putShort( protoMinor );
            buf.put( protoMajor );
            buf.putString( userName, charsetEncoder );
            buf.put( (byte) 0 );
            buf.putString( password, charsetEncoder );
            buf.put( (byte) 0 );
            buf.flip( );
            return buf;
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            return null;
        }
    }

    public String clientTypeToString( byte type )
    {
        switch( type )
        {
        case 1: return "Client";
        case 2: return "Recorder";
        case 3: return "Relay";
        default: return "Unknown";
        }
    }

    public String toString( )
    {
        return "(LogonAuthorize: "+appName+" "+appMajor+"."+appMinor+"."+appRelease+" "+clientTypeToString( clientType )+" "+protoMajor+"."+protoMinor+"."+protoRelease+" "+" "+userName+" "+password+" )";
    }
}

