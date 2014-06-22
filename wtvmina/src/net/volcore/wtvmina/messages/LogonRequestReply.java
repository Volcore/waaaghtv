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
public class LogonRequestReply implements WTVMessage
{
    public byte protoRelease;
    public short protoMinor;
    public byte protoMajor;

    public LogonRequestReply( IoBuffer in )
    {
        this.protoRelease = in.get( );
        this.protoMinor = in.getShort( );
        this.protoMajor = in.get( );
    }

    public LogonRequestReply( byte protoMajor, short protoMinor, byte protoRelease )
    {
        this.protoMajor = protoMajor;
        this.protoMinor = protoMinor;
        this.protoRelease = protoRelease;
    }

    public IoBuffer assemble( )
    {
        IoBuffer buf = IoBuffer.allocate( 9 );
        buf.order( ByteOrder.LITTLE_ENDIAN );
        buf.putShort( WTVProtocolDecoder.PRIMER );
        buf.put( WTVMessageFactory.LOGON_VERSIONREQUESTREPLY );
        buf.putShort( (short) 9 );
        buf.put( protoRelease );
        buf.putShort( protoMinor );
        buf.put( protoMajor );
        buf.flip( );
        return buf;
    }

    public String toString( )
    {
        return "(LogonRequestReply: "+protoMajor+"."+protoMinor+"."+protoRelease+")";
    }
}

