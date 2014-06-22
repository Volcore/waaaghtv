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

public class WTVProtocolDecoder extends CumulativeProtocolDecoder
{
    static Logger   logger = LoggerFactory.getLogger( "WTVProtocolDecoder" );
    
    public static final short PRIMER = -16657;

    //public static final int PROTOCOL_VERSION_MAJOR = 1;
    //public static final int PROTOCOL_VERSION_MINOR = 3;

    public enum States
    {
        HANDSHAKE_OK,
        IS_SERVER
    }

    protected boolean doDecode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        boolean isClient = session.getAttribute( States.IS_SERVER )==null;
        in.order( ByteOrder.LITTLE_ENDIAN );

        // If this session is new, verify the handshake.
            if( session.getAttribute( States.HANDSHAKE_OK ) == null )
            {
                if( in.remaining( ) < 4 ) 
                    return false;

                byte  release = in.get( );
                short minor = in.getShort( );
                byte  major = in.get( );

                // only protocol 1.3 or less
                if( major != -1
                && ( major != 1
                || ( minor != 2
                &&   minor != 3 )))
                {
                    session.close( );
                    return false;
                }

                // need to set this in here and not the handler, since its modifing a protocol parser state! more packets may follow.
                session.setAttribute( States.HANDSHAKE_OK );

                // send the handshake so the server/client can decide what to do
                out.write( new WTVHandshake( major, minor, release ) );

                return in.hasRemaining( ); 
            }

        // It's a packet! Read size from header to estimate packet size.
            if( in.remaining( ) < 5 ) 
                return false;

            int   oldpos = in.position( );
            short primer = in.getShort( );
            byte  type   = in.get( );
            short size   = in.getShort( );

            if( in.remaining( ) < size-5 )
            {
                // FIXME: verify that the buffer size is large enough to hold this packet!

                // not enough data yet, wait a little and reset the buffer.
                in.position( oldpos );
                return false;
            }

            if( primer != PRIMER )
            {
                in.position( oldpos );
                logger.trace( "Invalid packet primer: "+HexDump.bytebufferToHexString( in.buf( ) ) );
                session.close( );
                return false;
            }

            // more data may be available, but only the next packet should be readable.
            int oldlimit = in.limit( );
            in.limit( in.position( )+size-5 );
            WTVMessage message = WTVMessageFactory.parse( type, in, isClient );

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
