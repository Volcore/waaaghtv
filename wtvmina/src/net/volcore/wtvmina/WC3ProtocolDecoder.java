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

public class WC3ProtocolDecoder extends CumulativeProtocolDecoder
{
    static Logger   logger = LoggerFactory.getLogger( "WC3ProtocolDecoder" );
    
    protected boolean doDecode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        in.order( ByteOrder.LITTLE_ENDIAN );

        // It's a packet! Read size from header to estimate packet size.
            if( in.remaining( ) < 4 ) 
                return false;

            int   oldpos = in.position( );
            byte  primer = in.get( );
            byte  type   = in.get( );
            short size   = in.getShort( );

            if( in.remaining( ) < size-5 )
            {
                // FIXME: verify that the buffer size is large enough to hold this packet!

                // not enough data yet, wait a little and reset the buffer.
                in.position( oldpos );
                return false;
            }

            if( primer != WC3Message.PRIMER )
            {
                in.position( oldpos );
                logger.trace( "Invalid packet primer: "+HexDump.bytebufferToHexString( in.buf( ) ) );
                session.close( );
                return false;
            }

            // more data may be available, but only the next packet should be readable.
            int oldlimit = in.limit( );
            in.limit( in.position( )+size-5 );
            WC3Message message = WC3MessageFactory.parse( type, in );

            // construct settings of a successfully read message.
            in.limit( oldlimit );
            in.position( oldpos+size );

            if( message == null )
            {
                in.position( oldpos );
                logger.trace( "failed parsing packetdump: "+HexDump.bytebufferToHexString( in.buf( ) ) );
            
                session.close( );
                return false;
            }

            out.write( message );

        return in.hasRemaining( );
    }
}

