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

/*******************************************************************************
         See wtvProtocol.h in docs.
 *******************************************************************************/
public class LogonRequest implements WTVMessage
{
    public final static byte CLIENTTYPE_UNKNOWN  = 0;
    public final static byte CLIENTTYPE_CLIENT   = 1;
    public final static byte CLIENTTYPE_RECORDER = 2;
    public final static byte CLIENTTYPE_RELAY    = 3;

    public byte clientType;
    public byte protoRelease;
    public short protoMinor;
    public byte protoMajor;

    public LogonRequest( byte clientType, byte protoMajor, short protoMinor, byte protoRelease )
    {
        this.clientType = clientType;
        this.protoRelease = protoRelease;
        this.protoMinor = protoMinor;
        this.protoMajor = protoMajor;
    }

    public LogonRequest( IoBuffer in )
    {
        clientType = in.get( );
        protoRelease = in.get( );
        protoMinor = in.getShort( );
        protoMajor = in.get( );
    }

    public IoBuffer assemble( )
    {
        IoBuffer buf = IoBuffer.allocate( 10 );
        buf.order( ByteOrder.LITTLE_ENDIAN );
        buf.putShort( WTVProtocolDecoder.PRIMER );
        buf.put( WTVMessageFactory.LOGON_VERSIONREQUEST );
        buf.putShort( (short) 10 );
        buf.put( clientType );
        buf.put( protoRelease );
        buf.putShort( protoMinor);
        buf.put( protoMajor );
        buf.flip( );
        return buf;
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
        return "(LogonRequest: "+clientTypeToString( clientType )+" "+protoMajor+"."+protoMinor+"."+protoRelease+")";
    }
}
