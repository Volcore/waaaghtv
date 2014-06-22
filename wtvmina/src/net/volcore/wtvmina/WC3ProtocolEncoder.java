/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.wc3messages.*;
    // mina
        import org.apache.mina.filter.codec.*;
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
    // java
        import java.nio.*;


public class WC3ProtocolEncoder extends ProtocolEncoderAdapter
{
    static Logger   logger = LoggerFactory.getLogger( "WC3ProtocolEncoder" );

    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        WC3Message msg = (WC3Message)message;
        out.write( IoBuffer.wrap( msg.assemble( ) ) );
    }
}

