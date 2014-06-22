/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.messages.*;
    // mina
        import org.apache.mina.filter.codec.*;
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
    // java
        import java.nio.*;


public class WTVProtocolEncoder extends ProtocolEncoderAdapter
{
    static Logger   logger = LoggerFactory.getLogger( "WTVProtocolEncoder" );

    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        if( message instanceof WTVHandshake )
        {
            WTVHandshake handshake = (WTVHandshake) message;
            IoBuffer buf = IoBuffer.allocate( 4 );
            buf.order( ByteOrder.LITTLE_ENDIAN );
            buf.put( handshake.release );
            buf.putShort( handshake.minor);
            buf.put( handshake.major );
            buf.flip( );
            out.write( buf );
        } else
        {
            WTVMessage msg = (WTVMessage)message;
            out.write( msg.assemble( ) );
        }
    }
}
