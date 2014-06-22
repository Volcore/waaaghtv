/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmaster.relay;

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
        import org.apache.mina.core.service.*;
    // java
        import java.io.*;
        import java.util.*;
        import java.net.*;
    // wtvmaster
        import  net.volcore.wtvmaster.game.*;
        import  net.volcore.wtvmaster.*;

public class RelayIoHandler extends IoHandlerAdapter
{
    static Logger   logger = LoggerFactory.getLogger( "RelayIoHandler" );

    protected String        serverTitle = "Relaying all day!";
    protected String        serverMOTD  = "WAAAAAAAAAAAAAAAAAAAAAGH! (with a vengeance)";
    protected String        ircServerHost = "irc.quakenet.org";
    protected short         ircServerPort = 6666;
    protected String        ircServerChannel = "#waaaghtv";
    protected String        serverURL = "http://www.waaaghtv.com";

    protected WTVMaster wtvMaster;

    protected Timer         timer;
    // there are not many connections expected for this server ( only the caches ), so the polling timer can be somewhat lower.
    protected int           POLL_NEW_DATA_TIMER = 300;

    public enum SessionAttribute
    {
        LOGGED_IN,
        SUBSCRIBED_GAME,
        SUBSCRIPTION_STATE,
        DATAOFFSET,
        BLOCKID,
        ACKEDBLOCKID,
    }

    public enum SubscriptionState
    {
        NOTHING,
        DETAILS,
        DELAYED,
        STARTED,
        FINISHED
    }

    /** Constructor */
        public RelayIoHandler( WTVMaster wtvMaster )
        {
            this.wtvMaster = wtvMaster;
            timer = new Timer( true );
        }

    /** The session opened event is used to flag every session a server session. */
        public void sessionOpened( IoSession session ) throws Exception 
        {
            session.setAttribute( WTVProtocolDecoder.States.IS_SERVER );
        }

    /** The session closed event is currently just stub. */
        public void sessionClosed( IoSession session ) throws Exception 
        {
        }

    /** The session idle event sends ping messages to the client and eventually disconnects them. */
        public void sessionIdle( IoSession session, IdleStatus status ) throws Exception 
        {
            switch( session.getReaderIdleCount( ) )
            {
            case 1:
                {
                    // send a ping message... actually one on the first tic would be enough
                    RelayPing ping = new RelayPing( (int) System.currentTimeMillis( ));
                    session.write( ping );
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

    /** If an exception is caught, print it. Except its a trivial exception (reset by peer) */
        public void exceptionCaught( IoSession session, Throwable cause ) throws Exception 
        {
            if( session == null
            ||  cause == null)
            {
                logger.error( "Caught exception with session="+session+" and cause="+cause );
                return;
            }

            if( cause instanceof IOException 
            &&  cause.getMessage( )!= null 
            &&  cause.getMessage( ).equals( "Connection reset by peer" ) )
            {
                session.close( );
                return;
            }
            logger.error( "Caught exception "+session+": "+cause );
            cause.printStackTrace( );
            session.close( );
        }

    /** The heartpiece of the handler, the message received function. */
        public void messageReceived( IoSession session, Object message ) throws Exception 
        {
            /** Is it a handshake? Just pingpong if it has the right version */
                if( message instanceof WTVHandshake )
                {
                    WTVHandshake handshake = (WTVHandshake) message;
                    if( ( handshake.major != 1
                    ||  ( handshake.minor != 2 )
                      &&  handshake.minor != 3 ) )
                    {
                        session.close( );
                        return;
                    }
                    session.write( handshake );
                    return;
                }

            /** The ping messages are just to keep the idle time away. so ignore them */
                if( message instanceof RelayPong )
                    return;

            /** If the session is not logged in yet, process related messages. */
                if( session.getAttribute( SessionAttribute.LOGGED_IN ) == null )
                {
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
                            LogonAuthorizeReply reply = new LogonAuthorizeReply( "Invalid version!" );
                            session.write( reply );
                            session.closeOnFlush( );
                        } else
                        {
                            LogonAuthorizeReply reply = new LogonAuthorizeReply( "wtvMaster", wtvMaster.versionMajor, wtvMaster.versionMinor, wtvMaster.versionRelease, serverTitle, serverMOTD, ircServerHost, ircServerPort, ircServerChannel, serverURL );
                            session.write( reply );
                            session.setAttribute( SessionAttribute.LOGGED_IN );
                        }
                    } else
                    {
                        logger.error( "Unknown "+message+" during logon!" );
                        session.close( );
                    }

                    return;
                }

            /** It is logged in, so handle Relay protocol messages. */
                /** RelayGetGamelist - Send the current game list. */
                    if( message instanceof RelayGetGamelist )
                    {
                        RelayGetGamelist request = (RelayGetGamelist) message;

                        RelayGamelist    reply = new RelayGamelist( (int)(System.currentTimeMillis( )/10000), wtvMaster.gameDB.gameListNumGames, wtvMaster.gameDB.gameListNumGames, wtvMaster.gameDB.gameList );
                        session.write( reply );
                        return;
                    }
                    else 
                /** RelayGetGameInfo - Send the info for the requested game, if it exists. */
                    if( message instanceof RelayGetGameInfo )
                    {
                        RelayGetGameInfo request = (RelayGetGameInfo) message;
                        Game game = wtvMaster.gameDB.fetchGame( request.gameid );

                        if( game == null )
                        {
                            logger.warn( "Client requests info for unknown game, ignoring: "+request );
                            return;
                        }

                        byte[] info = game.getGameInfo( );

                        if( info == null )
                        {
                            logger.error( "Failed to fetch game info from file!" );
                            GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (1000). Please retry the download later." );
                            session.write( error );
                            return;
                        }

                        RelayGameInfo   reply = new RelayGameInfo( request.gameid, game.getComment( ), game.getStreamer( ), game.getGameLength( ), "", "", 0, "", info );
                        session.write( reply );
                    } else
                /** RelaySubscribeGame - Start streaming the game to the user. */
                    if( message instanceof RelaySubscribeGame )
                    {
                        RelaySubscribeGame sub = (RelaySubscribeGame) message;

                        /** fetch the game. */
                            Game game = wtvMaster.gameDB.fetchGame( sub.gameid );

                            if( game == null )
                            {
                                logger.warn( "Client subscribes unknown game, ignoring: "+sub );
                                RelaySubscribeGameReply reply = new RelaySubscribeGameReply( sub.gameid, (byte)0 );
                                session.write( reply );
                                return;
                            }

                        /** prepare streaming. */
                            RelaySubscribeGameReply reply = new RelaySubscribeGameReply( sub.gameid, (byte)1 );
                            session.write( reply );

                            session.setAttribute( SessionAttribute.SUBSCRIBED_GAME, new Integer( sub.gameid ) );
                            session.setAttribute( SessionAttribute.SUBSCRIPTION_STATE, SubscriptionState.NOTHING );
                            session.setAttribute( SessionAttribute.DATAOFFSET, new Integer( 0 ) );
                            session.setAttribute( SessionAttribute.BLOCKID, new Integer( 1 ) );
                            session.setAttribute( SessionAttribute.ACKEDBLOCKID, new Integer( 0 ) );
                            logger.trace( session+" game "+sub.gameid+" subscribed." );
                            updateSessionSubscription( session );

                        return;
                    } else
                /** RelayUpdateGameDataReply - Update the session subscription, possibly sending more data. */
                    if( message instanceof RelayUpdateGameDataReply )
                    {
                        RelayUpdateGameDataReply rep = (RelayUpdateGameDataReply) message;

                        // verify that the session is indeed still downloading this
                            Object obj = session.getAttribute( SessionAttribute.SUBSCRIBED_GAME );
                            if( obj == null 
                            ||  (Integer)obj != rep.gameid )
                            {
                                logger.trace( "UpdateGameData for unsubscribed game, ignoring." );
                                return;
                            }

                        int shouldblock = -1+(Integer)session.getAttribute( SessionAttribute.BLOCKID );

                        // Sanity check for correct order.
                            if( shouldblock != rep.blockid )
                            {
                                logger.error( "Invalid block id sequence! "+shouldblock+" vs "+message );
                                GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Network protocol problem (2000)! "+message );
                                session.write( error );
                                session.closeOnFlush( );
                                return;
                            }

                        session.setAttribute( SessionAttribute.ACKEDBLOCKID, new Integer( rep.blockid ) );

                        updateSessionSubscription( session );
                        return;
                    } else
                /** RelayUnsubscribeGame - Remove the subscription and cancle the timer task. */
                    if( message instanceof RelayUnsubscribeGame )
                    {
                        RelayUnsubscribeGame sub = (RelayUnsubscribeGame) message;

                        Integer g = (Integer) session.getAttribute( SessionAttribute.SUBSCRIBED_GAME );

                        /** check if its the right game... if not, just ignore. */
                            if( g == null || g != sub.gameid )
                                return;

                        // handle the unsubscription
                            RelayUnsubscribeGameReply reply = new RelayUnsubscribeGameReply( sub.gameid );
                            session.write( reply );

                            logger.trace( "Unsubscribe!" );

                            session.removeAttribute( SessionAttribute.SUBSCRIBED_GAME );
                            session.removeAttribute( SessionAttribute.SUBSCRIPTION_STATE );
                            session.removeAttribute( SessionAttribute.DATAOFFSET );
                            session.removeAttribute( SessionAttribute.BLOCKID );
                            session.removeAttribute( SessionAttribute.ACKEDBLOCKID );
                        return;
                    } else
                /** Error: unknown packet, just print dump and kick. */
                    {
                        logger.error( "Unhandled message! "+session+": "+message );
                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Not yet implemented: "+message );
                        session.write( error );
                        session.closeOnFlush( );
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
            // Find out what game id we are talking about
                Integer gameid = (Integer) session.getAttribute( SessionAttribute.SUBSCRIBED_GAME );

                if( gameid == null )
                    // no longer subscribed.
                    return;

            /** fetch the game. */
                Game game = wtvMaster.gameDB.fetchGame( gameid );

                if( game == null )
                    // game no longer exists.
                    return;

            /** Fetch more session attributes. */
                SubscriptionState state = (SubscriptionState) session.getAttribute( SessionAttribute.SUBSCRIPTION_STATE );
                Integer dataOffset = (Integer) session.getAttribute( SessionAttribute.DATAOFFSET );
                Integer blockId = (Integer) session.getAttribute( SessionAttribute.BLOCKID );
                Integer ackedBlockId = (Integer) session.getAttribute( SessionAttribute.ACKEDBLOCKID );

            /** Run the state machine. */
                switch( state )
                {
                case NOTHING:
                    // check if the game has details now.
                    if( game.getStatus( ) >= Game.STATUS_DELAY )
                    {
                        byte[] details = game.getGameDetails( );
                        if( details == null )
                        {
                            logger.error( "Failed to fetch game details for game "+gameid+"! Disconnecting." );
                            GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (1001)!" );
                            session.write( error );
                            session.closeOnFlush( );
                            return;
                        }
                        session.write( new RelayGameDetailUpdate( gameid, details ) );
                        session.setAttribute( SessionAttribute.SUBSCRIPTION_STATE, SubscriptionState.DETAILS );
                    } else
                    {
                        letSessionPollDelayed( session );
                        return;
                    }
                    // no break intentionally, after successfull send the next step can immediately be executed
                case DETAILS:
                    // check if the game has started now.
                    if( game.getStatus( ) >= Game.STATUS_DELAY )
                    {
                        session.write( new RelayGameStart( gameid, game.getLastSeed( ), game.getDelay( ), game.getDate( ) ) );

                        if( game.getDelay( )> 0 )
                        {
                            // if the delay isn't over, send the delay message and wait a little.
                            letSessionPollDelayed( session );
                            return;
                        }

                        session.setAttribute( SessionAttribute.SUBSCRIPTION_STATE, SubscriptionState.STARTED );
                    } else
                    {
                        letSessionPollDelayed( session );
                        return;
                    }
                    // no break intentionally, after successfull send the next step can immediately be executed
                case STARTED:
                    if( blockId > ackedBlockId+5 )
                        // there is more to come anyway.
                        return;

                    byte[] data = game.getDataBlock( dataOffset );

                    if( data != null )
                    {
                        RelayUpdateGameData gdata = new RelayUpdateGameData( gameid, blockId, data.length, dataOffset, 0, data );
                        session.setAttribute( SessionAttribute.DATAOFFSET, new Integer( dataOffset+data.length ) );
                        session.setAttribute( SessionAttribute.BLOCKID, new Integer( blockId+1 ) );

                        session.write( gdata );
                    } else
                    {
                        if( game.isFinished( ) )
                        {
                            if( game.getGameDataSize( ) != dataOffset )
                            {
                                letSessionPollDelayed( session );
                                return;
                            }

                            RelayGameFinish finish = new RelayGameFinish( gameid, game.getNumPackets( ));
                            session.write( finish );
                            return;
                        }

                        letSessionPollDelayed( session );
                    }

                    return;
                    
                default:
                }
        }
}

