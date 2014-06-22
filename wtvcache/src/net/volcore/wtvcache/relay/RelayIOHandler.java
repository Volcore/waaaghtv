/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvcache.relay;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.*;
        import net.volcore.wtvmina.messages.*;
    // mina
        import org.apache.mina.core.session.*;
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.service.*;
        import org.apache.mina.core.future.*;
    // java
        import java.util.*;
        import java.net.*;
        import java.io.*;
    // wtvcache
        import net.volcore.wtvcache.game.*;
        import net.volcore.wtvcache.*;


/*******************************************************************************
         This is the IO Handler, which processes messages and queues the results.
 *******************************************************************************/
public class RelayIOHandler extends IoHandlerAdapter
{
    static Logger   logger = LoggerFactory.getLogger( "RelayIOHandler" );

    /** Data */
        /** Server configuration information. */
            protected String        serverTitle = "It's about time!";
            protected String        serverMOTD  = "WAAAAAAAAAAAAAAAAAAAAAGH! (with a vengeance)";
            protected String        ircServerHost = "irc.quakenet.org";
            protected short         ircServerPort = 6666;
            protected String        ircServerChannel = "#waaaghtv";
            protected String        serverURL = "http://www.waaaghtv.com";

        /** Cache information. */
            protected WTVCache      wtvCache;
            protected Timer         timer;

        /** Time until more data is polled. */
            protected int           POLL_NEW_DATA_TIMER = 650;
            protected int           POLL_GAME_INFOS     = 1000;
            protected int           POLL_GAME_DETAILS   = 1000;

    /** Constructor. */
        public RelayIOHandler( WTVCache wtvCache )
        {
            this.wtvCache = wtvCache;
            timer = new Timer( true );
        }

    /** Unused overrides. */
        public void sessionCreated( IoSession session ) throws Exception 
        {
            RelaySessionData s = new RelaySessionData( );
            s.hostname = ((InetSocketAddress)session.getRemoteAddress( )).getAddress( ).getHostAddress( );
            session.setAttachment( s );
            
            //wtvCache.getAccessLog( ).logConnect( ((InetSocketAddress)session.getRemoteAddress( )).getAddress( ).getHostAddress( ) );
        }

        public void messageSent( IoSession session, Object message ) throws Exception 
        {
        }

        public void sessionClosed( IoSession session ) throws Exception 
        {
            //logger.trace( "Session closed: "+session );
            RelaySessionData s = (RelaySessionData) session.getAttachment( );
            wtvCache.getAccessLog( ).logDisc( s.hostname, session.getReadBytes( ), session.getWrittenBytes( ), System.currentTimeMillis( ) - session.getCreationTime( ) );
        }

    /** Called whenever a new session is opened. */
        public void sessionOpened( IoSession session ) throws Exception 
        {
            session.setAttribute( WTVProtocolDecoder.States.IS_SERVER );
        }

    /** Called when a sesison is idle for a while */
        public void sessionIdle( IoSession session, IdleStatus status ) throws Exception 
        {
            RelaySessionData s = (RelaySessionData) session.getAttachment( );

            switch( session.getReaderIdleCount( ) )
            {
            case 1:
                {
                    // send a ping message...
                    RelayPing ping = new RelayPing( (int) System.currentTimeMillis( ));
                    session.write( ping );

                    GameCache gameCache = wtvCache.getGameCache( );

                    if( s.loggedIn && s.isWebClient==false )
                    {
                        RelayGamelist    reply = new RelayGamelist( (int)(System.currentTimeMillis( )/10000), gameCache.getNumGames( ), gameCache.getNumGames( ), gameCache.getCompiledGameList( ) );
                        session.write( reply );
                        //wtvCache.getAccessLog( ).logGamelist( ((InetSocketAddress)session.getRemoteAddress( )).getAddress( ).getHostAddress( ), false );
                    } 
                    return;
                }
            case 2:
            case 3:
            case 4:
                break;

            case 5:
            default:
                session.close( );
                return;
            }
        }

    /** Called when an exception is caught.. clean up, etc. */
        public void exceptionCaught( IoSession session, Throwable cause ) throws Exception 
        {
            if( session == null
            ||  cause == null
            ||  cause.getMessage( ) == null )
            {
                logger.error( "Caught exception with session="+session+" and cause="+cause );
                return;
            }

            if( cause instanceof IOException 
            &&  cause.getMessage( ).equals( "Connection reset by peer" ) )
            {
                session.close( );
                return;
            }
            logger.error( "Caught exception "+session+": "+cause );
            cause.printStackTrace( );
            session.close( );
        }

    /** Game info related functionality. */
        /** Try to send the game info, or request it if its not cached. */
            public void trySendGameInfo( final IoSession session, final Game game )
            {
                switch( game.getCacheState( ) )
                {
                case Game.CACHESTATE_DOESNOTEXIST:
                    {
                        logger.warn( "Client requested info for non-existant game "+game );
                        return;
                    }

                default:
                    if( game.getGameInfo( ) == null )
                    {
                        wtvCache.initiateGameInfoFetch( game, false );

                        timer.schedule( new TimerTask( ) 
                            {
                                public void run( )
                                {
                                    try {
                                        if( session.isClosing( ) ) 
                                            return;

                                        trySendGameInfo( session, game );
                                    } catch( Throwable e )
                                    // if not catched here the timer thread will be canceled, leading to havok
                                    {
                                        logger.warn( "Catched "+e+" in timer thread!" );
                                        e.printStackTrace( );
                                        return;
                                    }
                                }
                            }, POLL_GAME_INFOS );
                        return;
                    } 

                    // gameinfos available, send
                    try {
                    RelayGameInfo   reply = new RelayGameInfo( game.getGameId( ), game.getGameInfo( ) );
                    session.write( reply );
                    } catch( ParseException e ) { } // just a bug, doesn't ever trigger.
                    break;
                }
            }

    /** Game details related functionality. */
        /** Try to send the game details, or request them if they are not cached. */
            public void trySendGameDetails( final IoSession session, final Game game )
            {
                RelaySessionData s = (RelaySessionData) session.getAttachment( );
                switch( game.getCacheState( ) )
                {
                case Game.CACHESTATE_DOESNOTEXIST:
                    {
                        logger.warn( "Client requested details for non-existant game "+game );
                        try {
                        RelaySubscribeGameReply reply = new RelaySubscribeGameReply( game.getGameId( ), (byte)0 );
                        session.write( reply );
                        } catch( ParseException e ) { }
                        return;
                    }

                case Game.CACHESTATE_NONE:
                    {
                        // also try to fetch game infos in case they are not available
                        wtvCache.initiateGameInfoFetch( game, false );
                        wtvCache.initiateGameFetch( game );

                        timer.schedule( new TimerTask( ) 
                            {
                                public void run( )
                                {
                                    try {
                                        if( session.isClosing( ) ) 
                                            return;

                                        trySendGameDetails( session, game );
                                    } catch( Throwable e )
                                    // if not catched here the timer thread will be canceled, leading to havok
                                    {
                                        logger.warn( "Catched "+e+" in timer thread!" );
                                        e.printStackTrace( );
                                        return;
                                    }
                                }
                            }, POLL_GAME_DETAILS );
                    } break;

                default:
                    // gamedetails available, send
                    try {
                        RelaySubscribeGameReply reply = new RelaySubscribeGameReply( game.getGameId( ), (byte)1 );
                        session.write( reply );

                        //wtvCache.increaseGameStats( game.getGameId( ), GAMESTATS_NUMSUBSCRIBED );

                        s.subscribedGame = game.getGameId( );
                        s.dataOffset = 0;
                        s.state = RelaySessionData.SubscriptionState.NOTHING;
                        s.blockId = 1;
                        s.ackedBlockId = 0;

                        updateSessionSubscription( session );
                    } catch( ParseException e ) { } // just a bug, doesn't ever trigger.
                    break;
                }
            }

    /** Called whenever a message has been received. */
        public void messageReceived( IoSession session, Object message ) throws Exception 
        {
            RelaySessionData s = (RelaySessionData) session.getAttachment( );

            /** Hardcore message tracing. */
                //logger.trace( "Message received "+session+": "+message );

            /** Is it a handshake? */
                if( message instanceof WTVHandshake )
                {
                    WTVHandshake handshake = (WTVHandshake) message;

                    if( handshake.major == -1 )
                        s.expectCacheHack = true;
                    else
                    {
                        if( handshake.major != 1
                        ||  ( handshake.minor != 2
                          &&  handshake.minor != 3 ) )
                        {
                            session.close( );
                            return;
                        }
                        session.write( handshake );
                    }
                    return;
                }

            /** The ping messages are just to keep the idle time away. so ignore them */
                if( message instanceof RelayPong )
                    return;

            /** If the session is not logged in yet, process related messages. */
                if( s.loggedIn == false )
                {
                    if( s.expectCacheHack )
                    {
                        if( message instanceof CacheHack )
                        {
                            CacheHack ch = (CacheHack) message;

                            s.hostname = ch.cacheInfo;

                            if( ch.major != 1
                            ||  ( ch.minor != 2
                              &&  ch.minor != 3 ) )
                            {
                                session.close( );
                                return;
                            }

                            session.write( new WTVHandshake( ch.major, ch.minor, ch.release ) );

                            s.expectCacheHack = false;
                            return;
                        } else
                        {
                            logger.error( "Invalid Cache hack protocol!" );
                            session.close( );
                            return;
                        }
                    }

                    if( message instanceof LogonRequest )
                    {
                        LogonRequest request = (LogonRequest) message;
                        if( request.protoMajor != 1 
                        ||  request.protoMinor != 5
                        ||  request.clientType != LogonRequest.CLIENTTYPE_CLIENT )
                        {
                            logger.error( "Invalid "+message+". Version or client type not okay!" );
                            session.close( );
                            return;
                        }

                        LogonRequestReply reply = new LogonRequestReply( request.protoMajor, request.protoMinor, request.protoRelease );
                        session.write( reply );
                    } else
                    if( message instanceof LogonAuthorize )
                    {
                        LogonAuthorize authorize = (LogonAuthorize) message;
                        if( authorize.protoMajor != 1 
                        ||  authorize.protoMinor != 5 )
                        {
                            logger.trace( "Rejected request due to invalid version: "+authorize );
                            LogonAuthorizeReply reply = new LogonAuthorizeReply( "All your base are belong to us!" ); // FIXME: proper reason
                            session.write( reply );
                            session.closeOnFlush( );
                        } else
                        {
                            LogonAuthorizeReply reply = new LogonAuthorizeReply( WTVCache.applicationName, (byte)net.volcore.wtvcache.VersionInfo.major, (short)net.volcore.wtvcache.VersionInfo.minor, (byte)net.volcore.wtvcache.VersionInfo.release, serverTitle, serverMOTD, ircServerHost, ircServerPort, ircServerChannel, serverURL );
                            session.write( reply );
                            s.loggedIn = true;

                            if( authorize.appName.equals( "Waaagh!TV Java Client" ) )
                                s.isWebClient = true;

                            wtvCache.getAccessLog( ).logLogin( s.hostname, authorize.appName+"-"+authorize.appMajor+"."+authorize.appMinor+"."+authorize.appRelease );
                        }
                    } else
                    {
                        logger.error( "Unknown "+message+" during logon!" );
                        session.close( );
                    }

                    return;
                }

            /** So the user is logged in, process the request. */
                /** RelayGetGamelist */
                    if( message instanceof RelayGetGamelist )
                    {
                        RelayGetGamelist request = (RelayGetGamelist) message;

                        GameCache gameCache = wtvCache.getGameCache( );

                        // Gamelist is generated 10 seconds. therefore timestamps can use that precision.
                        if( s.isWebClient == false )
                        {
                            RelayGamelist    reply = new RelayGamelist( (int)(System.currentTimeMillis( )/10000), gameCache.getNumGames( ), gameCache.getNumGames( ), gameCache.getCompiledGameList( ) );
                            session.write( reply );
                        } else
                        {
                            RelayGamelist    reply = new RelayGamelist( (int)(System.currentTimeMillis( )/10000), gameCache.getNumLiveGames( ), gameCache.getNumLiveGames( ), gameCache.getCompiledLiveGames( ) );
                            session.write( reply );
                        }

                        //wtvCache.getAccessLog( ).logGamelist( s.hostname, true );
                    } else
                /** RelayGetGameInfo */
                    if( message instanceof RelayGetGameInfo )
                    {
                        RelayGetGameInfo request = (RelayGetGameInfo) message;

                        //wtvCache.getAccessLog( ).logInfo( s.hostname, request.gameid );

                        Game game = wtvCache.getGameCache( ).getGameById( request.gameid );

                        trySendGameInfo( session, game );
                    } else
                /** RelaySubscribeGame */
                    if( message instanceof RelaySubscribeGame )
                    {
                        RelaySubscribeGame sub = (RelaySubscribeGame) message;

                        Game game = wtvCache.getGameCache( ).getGameById( sub.gameid );

                        wtvCache.getAccessLog( ).logSubscribe( s.hostname, sub.gameid );

                        trySendGameDetails( session, game );
                        return;
                    } else
                /** RelayUpdateGameDataReply */
                    if( message instanceof RelayUpdateGameDataReply )
                    {
                        RelayUpdateGameDataReply rep = (RelayUpdateGameDataReply) message;

                        // ignore old acks
                        if( s.subscribedGame == null || rep.gameid != s.subscribedGame )
                            return;

                        // this my actually happen if someone re-subscribes a game.
                        if( rep.blockid != s.ackedBlockId+1 )
                        {
                            // so spit out a warning and do nothing, to filter out the acks for the old subscription
                            logger.warn( "Invalid block id ack! "+rep+" "+s+".... Trying to continue..." );
                            return;
                        }

                        s.ackedBlockId = rep.blockid;

                        updateSessionSubscription( session );
                        return;
                    } else
                /** RelayUnsubscribeGame */
                    if( message instanceof RelayUnsubscribeGame )
                    {
                        RelayUnsubscribeGame sub = (RelayUnsubscribeGame) message;

                        /** check if its the right game... if not, just ignore. */
                            if( s.subscribedGame == null || s.subscribedGame != sub.gameid )
                            {
                                logger.trace( "Invalid unsubscribe... "+s+" "+sub );
                                return; 
                            }

                        // the game is here
                        RelayUnsubscribeGameReply reply = new RelayUnsubscribeGameReply( sub.gameid );
                        session.write( reply );

                        s.subscribedGame    = null;
                        s.dataOffset        = 0;
                        s.state             = RelaySessionData.SubscriptionState.NOTHING;
                        s.blockId           = 1;
                        s.ackedBlockId      = 0;

                        return;
                    } else
                // default
                    {
                        logger.error( "Unhandled message! "+session+": "+message );
                        session.close( );
                    }
        }

    /** Activates a timer for a session. */
        public void letSessionPollDelayed( final IoSession session )
        {
            timer.schedule( new TimerTask( ) 
                {
                    public void run( )
                    {
                        try {
                            if( session.isClosing( ) ) 
                                return;

                            updateSessionSubscription( session );
                        } catch( Throwable e )
                        // if not catched here the timer thread will be canceled, leading to havok
                        {
                            logger.warn( "Catched "+e+" in timer thread!" );
                            return;
                        }
                    }
                }, POLL_NEW_DATA_TIMER );
        }

    /** Update the subscription status of a session. */
        protected void updateSessionSubscription( final IoSession session )
        {
            // sanity check: check if session is still alive. 
                if( session.isClosing( ) )
                    return;

            // fetch session data
                RelaySessionData s = (RelaySessionData) session.getAttachment( );

            // chceck if still loading a game
                if( s.subscribedGame == null )
                    return;

            // fetch game
                Game game = wtvCache.getGameCache( ).getGameById( s.subscribedGame );

                if( game.getCacheState( ) < Game.CACHESTATE_NONE )
                {
                    logger.warn( "Game cached out while user was still downloading!" );
                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (2000). Please retry the download later." );
                    session.write( error );
                    return;
                }

            
            //  main distributor. 
                switch( s.state )
                {
                case NOTHING:
                    // check if the game has details now.
                    if( game.getCacheState( ) >= Game.CACHESTATE_GAMEDETAILS )
                    {
                        session.write( new RelayGameDetailUpdate( game.getGameId( ), game.getGameDetails( ) ) );
                        s.state = RelaySessionData.SubscriptionState.DETAILS;
                    } else
                    {
                        letSessionPollDelayed( session );
                        return;
                    }
                    // no break intentionally, after successfull send the next step can immediately be executed
                case DETAILS:
                    // check if the game has started now.
                    if( game.getCacheState( ) >= Game.CACHESTATE_GAMESTART )
                    {
                        if( game.getDelay( ) > 0 )
                        {
                            // if the delay isn't over, send the delay message and wait a little.
                            session.write( new RelayGameStart( game.getGameId( ), game.getLastSeed( ), game.getDelay( ), game.getDate( ) ) );
                            letSessionPollDelayed( session );
                            return;
                        }

                        session.write( new RelayGameStart( game.getGameId( ), game.getLastSeed( ), game.getDelay( ), game.getDate( ) ) );
                        s.state = RelaySessionData.SubscriptionState.STARTED;
                    } else
                    {
                        letSessionPollDelayed( session );
                        return;
                    }
                    // no break intentionally, after successfull send the next step can immediately be executed
                case STARTED:
                    if( s.blockId > s.ackedBlockId+5 )
                        // there is more to come anyway.
                        return;

                    byte[] data = game.getDataBlock( s.dataOffset, 15*1024 );

                    if( data != null )
                    {
                        RelayUpdateGameData gdata = new RelayUpdateGameData( game.getGameId( ), s.blockId, data.length, s.dataOffset, 0, data );
                        s.dataOffset += data.length;
                        s.blockId ++;
                        session.write( gdata );
                    } else
                    {
                        if( game.getCacheState( ) == Game.CACHESTATE_FINISHED )
                        {
                            if( game.getGameDataSize( ) != s.dataOffset )
                            {
                                letSessionPollDelayed( session );
                                return;
                            }

                            RelayGameFinish finish = new RelayGameFinish( game.getGameId( ), game.getNumPackets( ) );
                            session.write( finish );
                            wtvCache.getAccessLog( ).logFinished( ((InetSocketAddress)session.getRemoteAddress( )).getAddress( ).getHostAddress( ), game.getGameId( ) );
                            return;
                        }

                        letSessionPollDelayed( session );
                    }

                    return;
                    
                default:
                }
        }
}

