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

public class FetchGameSession extends UplinkSession
{
    static Logger   logger = LoggerFactory.getLogger( "FetchGameSession" );

    protected Game  game;

    public FetchGameSession( int serverId, WTVCache wtvCache, Game game )
    {
        super( serverId, wtvCache );
        this.game = game;
    }

    /** Nothing to do on sessionclose for the gamelist fetch. */
        public void sessionClosed( IoSession session )
        {
            // if the game could not be fetched completely, remove it from cache and try again
            if( game.getCacheState( ) != Game.CACHESTATE_FINISHED )
            {
                logger.warn( "Failed to download game "+game+"... setting to finished anyway, reduce the harm. Cache out to retry the download." );
                //logger.error( "Failed to download game... removing from cache." );
                game.setCacheState( Game.CACHESTATE_FINISHED );
                game.setBroken( true );
                //wtvCache.getGameCache( ).removeGame( game.getGameId( ) );
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
                RelaySubscribeGame req = new RelaySubscribeGame( game.getGameId( ), (byte) 1 );
                session.write( req );
            } catch( ParseException e )
            {
                logger.error( "Failed to subscribe game: "+e );
                e.printStackTrace( );
                session.close( );
            }
        }

    /** Process a message received when logged in already. */
        public void processMessage( IoSession session, Object message )
        {
            /** RelaySubscribeGameReply */
                if( message instanceof RelaySubscribeGameReply )
                {
                    RelaySubscribeGameReply reply = (RelaySubscribeGameReply) message;

                    if( reply.status != 1 )
                    {
                        logger.error( "Failed to download game, probably too old or does not exist: "+reply );
                        session.close( );
                        return;
                    }

                    // the game is being downloaded, more messages will follow
                    return;
                } else
            /** RelayGameDetailUpdate */
                if( message instanceof RelayGameDetailUpdate )
                {
                    RelayGameDetailUpdate gamedetail = (RelayGameDetailUpdate) message;

                    if( gamedetail.gameid != game.getGameId( ) )
                    {
                        logger.error( "Detail update for wrong game id!" );
                        session.close( );
                        return;
                    }

                    game.setGameDetails( gamedetail.detail );

                    game.setCacheState( Game.CACHESTATE_GAMEDETAILS );

                    return;
                } else
            /** RelayGameStart */
                if( message instanceof RelayGameStart )
                {
                    RelayGameStart gamestart = (RelayGameStart) message;
                    if( gamestart.gameid != game.getGameId( ) )
                    {
                        logger.error( "Detail start for wrong game id!" );
                        session.close( );
                        return;
                    }

                    game.setLastSeed( gamestart.lastSeed );
                    game.setDelay( gamestart.delay );
                    game.setDate( gamestart.date );
                    if( game.getDelay( ) == 0 
                    &&  game.getCacheState( ) < Game.CACHESTATE_GAMESTART )
                    {
                        game.createGameDataBuffer( );
                        game.setCacheState( Game.CACHESTATE_GAMESTART );
                    }

                    return;
                } else
            /** RelayUpdateGameData */
                if( message instanceof RelayUpdateGameData ) 
                {
                    RelayUpdateGameData data = (RelayUpdateGameData) message;

                    if( data.gameid != game.getGameId( ) )
                    {
                        logger.error( "Game data for wrong game id!" );
                        session.close( );
                        return;
                    }

                    int position = game.addGameData( data.position, data.data );

                    try {
                    RelayUpdateGameDataReply reply = new RelayUpdateGameDataReply( data.gameid, data.blockid, position );
                    session.write( reply );
                    } catch( Exception e ) { }

                    return;
                } else
            /** RelayGameFinish */
                if( message instanceof RelayGameFinish ) 
                {
                    RelayGameFinish finish = (RelayGameFinish) message;

                    if( finish.gameid != game.getGameId( ) )
                    {
                        logger.error( "Game finish for wrong game id!" );
                        session.close( );
                        return;
                    }

                    if( game.getCacheState( ) < Game.CACHESTATE_FINISHED )
                    {
                        game.finishGameData( finish.numpackets );
                        game.setCacheState( Game.CACHESTATE_FINISHED );

                        // perform a forced game info update
                        wtvCache.initiateGameInfoFetch( game, true );
                    }

                    // finished!
                    session.close( );

                    return;
                } else
            /* Default */
                {
                    logger.error( "Failed to parse message in fetch game mode: "+message );
                    session.close( );
                    return;
                }

        }
}


