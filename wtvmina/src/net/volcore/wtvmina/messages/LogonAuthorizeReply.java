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
public class LogonAuthorizeReply implements WTVMessage
{
    static Logger   logger = LoggerFactory.getLogger( "LogonAuthorizeReply" );
    public byte    code;

    public String  reason;

    public String  appName;
    public byte    appRelease;
    public short   appMinor;
    public byte    appMajor;
    public String  title;
    public String  motd;
    public String  ircServer;
    public short   ircPort;
    public String  ircChannel;
    public String  url;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public LogonAuthorizeReply( String reason )
    {
        code = 0;
        this.reason = reason;
    }

    public LogonAuthorizeReply( String appName, byte appMajor, short appMinor, byte appRelease, String title, String motd, String ircServer, short ircPort, String ircChannel, String url )
    {
        code = 1;
        this.appName = appName;
        this.appMajor = appMajor;
        this.appMinor = appMinor;
        this.appRelease = appRelease;
        this.title = title;
        this.motd = motd;
        this.ircServer = ircServer;
        this.ircPort = ircPort;
        this.ircChannel = ircChannel;
        this.url = url;
    }

    public LogonAuthorizeReply( IoBuffer in ) throws ParseException
    {
        try {
            code = in.get( );
            if( code == 0 )
                reason = in.getString( charsetDecoder );
            else
            {
                appName = in.getString( charsetDecoder );
                appRelease = in.get( );
                appMinor = in.getShort( );
                appMajor = in.get( );
                title = in.getString( charsetDecoder );
                motd = in.getString( charsetDecoder );
                ircServer = in.getString( charsetDecoder );
                ircPort = in.getShort( );
                ircChannel = in.getString( charsetDecoder );
                url = in.getString( charsetDecoder );
            }
        } catch( CharacterCodingException e )
        {
            throw new ParseException( "Failed to parse "+this.getClass( )+": "+e );
        }
    }

    public IoBuffer assemble( )
    {
        try {
            if( code == 0 )
            {
                short length = (short)(5+1+reason.length( )+1);
                IoBuffer buf = IoBuffer.allocate( length );
                buf.order( ByteOrder.LITTLE_ENDIAN );
                buf.putShort( WTVProtocolDecoder.PRIMER );
                buf.put( WTVMessageFactory.LOGON_AUTHORIZEREPLY );
                buf.putShort( length );
                buf.put( code );
                buf.putString( reason, charsetEncoder );
                buf.put( (byte) 0 );
                buf.flip( );
                return buf;
            } else
            {
                short length = (short)(5+1+appName.length( )+1+4+title.length( )+1+motd.length( )+1+ircServer.length( )+1+2+ircChannel.length( )+1+url.length( )+1);
                IoBuffer buf = IoBuffer.allocate( length );
                buf.order( ByteOrder.LITTLE_ENDIAN );
                buf.putShort( WTVProtocolDecoder.PRIMER );
                buf.put( WTVMessageFactory.LOGON_AUTHORIZEREPLY );
                buf.putShort( length );
                buf.put( code );
                buf.putString( appName, charsetEncoder );
                buf.put( (byte) 0 );
                buf.put( appRelease );
                buf.putShort( appMinor );
                buf.put( appMajor );
                buf.putString( title, charsetEncoder );
                buf.put( (byte) 0 );
                buf.putString( motd, charsetEncoder );
                buf.put( (byte) 0 );
                buf.putString( ircServer, charsetEncoder );
                buf.put( (byte) 0 );
                buf.putShort( ircPort );
                buf.putString( ircChannel, charsetEncoder );
                buf.put( (byte) 0 );
                buf.putString( url, charsetEncoder );
                buf.put( (byte) 0 );
                buf.flip( );
                return buf;
            }
        } catch( Exception e )
        {
            logger.error( "Failed to assemble "+this.getClass( )+": "+e );
            return null;
        }
    }

    public String toString( )
    {
        if( code == 0 )
            return "(LogonAuthorizeReply: failed "+reason+" )";
        else
            return "(LogonAuthorizeReply: succeeded '"+appName+"' "+appMajor+"."+appMinor+"."+appRelease+" '"+title+"' '"+motd+"' '"+ircServer+"' "+ircPort+" '"+ircChannel+"' '"+url+"' )";
    }
}


