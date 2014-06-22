/** Copyright (C) 2008 Volker SchÃ¶nefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.wc3messages.*;
    // mina
        import org.apache.mina.core.buffer.*;


/*******************************************************************************
         This factory can be used to parse WC3 messages.
 *******************************************************************************/
public class WC3MessageFactory
{
    static Logger   logger = LoggerFactory.getLogger( "WC3MessageFactory" );

    public static WC3Message parse( int type, IoBuffer in )
    {
        try {
            switch( type )
            {
            case WC3Message.UDPINFOREQUEST:                 return new InfoRequest( in );
            case WC3Message.UDPSERVERINFO:                  return new ServerInfo( in );
            case WC3Message.UDPNEWSERVERBROADCAST:          return new NewServerBroadcast( in );
            case WC3Message.UDPIDLESERVERBROADCAST:         return new IdleServerBroadcast( in );
            case WC3Message.UDPCLOSESERVERBROADCAST:        return new CloseServerBroadcast( in );
            }

            throw new ParseException( "Unknown packet type: "+type );
        }
        catch( ParseException e )
        {
            logger.error( "Failed to parse wc3 message: "+e );
            e.printStackTrace( );
            return null;
        }
    }
}


