/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.uplink;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.*;
        import net.volcore.wtvmina.messages.*;
    // mina
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
        import org.apache.mina.core.future.*;
        import org.apache.mina.core.service.*;
        import org.apache.mina.transport.socket.nio.*;
    // java
        import java.net.*;
        import java.util.*;
        import java.nio.*;
        import java.nio.charset.*;
    // wtvcache
        import net.volcore.wtvcache.game.*;
        import net.volcore.wtvcache.*;

public class FetchGameListSession extends UplinkSession
{
    static Logger   logger = LoggerFactory.getLogger( "FetchGameListSession" );

    public FetchGameListSession( int serverId, WTVCache wtvCache )
    {
        super( serverId, wtvCache );
    }

    /** Nothing to do on sessionclose for the gamelist fetch. */
        public void sessionClosed( IoSession session )
        {
        }

    /** Disconnect on idle. */
        public void sessionIdle( IoSession session, IdleStatus status )
        {
            session.close( );
        }

    /** When logged in send a game list request. */
        public void onLoggedIn( IoSession session )
        {
            try {
                RelayGetGamelist req = new RelayGetGamelist( 0 );
                session.write( req );
            } catch( ParseException e )
            {
                logger.error( "Failed to request game list: "+e );
                e.printStackTrace( );
                session.close( );
            }
        }

    /** Process a message received when logged in already. */
        public void processMessage( IoSession session, Object message )
        {
            if( message instanceof RelayGamelist )
            {
                RelayGamelist gamelist = (RelayGamelist) message;

                byte[] data = gamelist.list;
                IoBuffer rbuf = IoBuffer.wrap( data );
                rbuf.order( ByteOrder.LITTLE_ENDIAN );

                GameListEntry[] list = new GameListEntry[ gamelist.numGames ];

                for( int i=0; i<gamelist.numGames; ++i )
                {
                    list[ i ] = new GameListEntry( rbuf );

                    // fetch the game and try to download info. 
                        // Note: these functions will not do anything if the game is already in the cache
                        //       and the infos are cached.
                    Game game = wtvCache.getGameCache( ).getGameById( list[ i ].gameId );
                    game.updateLastAccess( );
                    game.setServerId( serverId );
                    wtvCache.initiateGameInfoFetch( game, false );
                }

                wtvCache.getGameCache( ).setGameList( serverId, list );
                wtvCache.getGameCache( ).compileGameList( );
                session.close( );
                return;
            } else
            {
                logger.error( "Failed to parse message in gamelist mode: "+message );
                session.close( );
                return;
            }
        }
}

