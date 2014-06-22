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

public class FetchGameInfoSession extends UplinkSession
{
    static Logger   logger = LoggerFactory.getLogger( "FetchGameInfoSession" );

    protected Game  game;

    public FetchGameInfoSession( int server, Game game, WTVCache wtvCache )
    {
        super( server, wtvCache );
        this.game = game;
    }

    /** Nothing to do on sessionclose for the gamelist fetch. */
        public void sessionClosed( IoSession session )
        {
            // check if the info fetch failed...
            if( game.getGameInfo( )==null )
            {
                game.setCacheState( Game.CACHESTATE_DOESNOTEXIST );
            } 
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
                RelayGetGameInfo gi = new RelayGetGameInfo( game.getGameId( ) );
                session.write( gi );
            } catch( ParseException e )
            {
                logger.error( "Failed to request game info: "+e );
                e.printStackTrace( );
                session.close( );
            }
        }

    /** Process a message received when logged in already. */
        public void processMessage( IoSession session, Object message )
        {
            if( message instanceof RelayGameInfo )
            {
                RelayGameInfo gameinfo = (RelayGameInfo) message;
    
                if( game.getGameId( ) != gameinfo.gameid )
                {
                    logger.error( "Game info for wrong game? "+game+" "+gameinfo );
                    session.close( );
                    return;
                }

                //logger.trace( "Got game infos for game "+game );
                game.setGameInfo( gameinfo.gameInfo );
                // success, finished.
                session.close( );
                return;
            } else
            {
                logger.error( "Failed to parse message in gameinfo mode: "+message );
                session.close( );
                return;
            }
        }
}


