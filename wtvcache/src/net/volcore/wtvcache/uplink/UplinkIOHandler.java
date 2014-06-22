/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
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

/*******************************************************************************
         This is the IO Handler for the uplink, which takes care of the 
        communication to the master server.
 *******************************************************************************/
public class UplinkIOHandler implements IoHandler
{
    static Logger   logger = LoggerFactory.getLogger( "UplinkIOHandler" );

    /** Protected members. */
        protected WTVCache              wtvCache;
        protected NioSocketConnector    connector;
        protected Timer                 timer;
        protected long                  GAME_DOWNLOAD_RETRY_TIMER = 10000;

    /** Schedule a game download retry. */
        //public void scheduleGameDownloadRetry( final SocketAddress remoteAddress, final int gameid, final int numRetries )
        //{
        //    //timer.schedule( new TimerTask( ) 
        //    //    {
        //    //        public void run( )
        //    //        {
        //    //            connector.connect( remoteAddress ).addListener( 
        //    //                new IoFutureListener< ConnectFuture >( )
        //    //                    {
        //    //                        public void operationComplete( ConnectFuture future )
        //    //                        {
        //    //                            if( future.isConnected( ) == true )
        //    //                            {
        //    //                                future.getSession( ).setAttribute( UplinkIOHandler.SessionAttribute.GETGAME, new Integer( gameid ) );
        //    //                                future.getSession( ).setAttribute( UplinkIOHandler.SessionAttribute.NUMRETRIES, new Integer( numRetries ) );
        //    //                            }
        //    //                            else
        //    //                            {
        //    //                                scheduleGameDownloadRetry( remoteAddress, gameid, numRetries+1 );
        //    //                                logger.warn( "Failed to connect to master server to retry fetch game "+gameid );
        //    //                            }
        //    //                        }
        //    //                    } );
    
        //    //        }
        //    //    }, numRetries*GAME_DOWNLOAD_RETRY_TIMER );
        //}

    /** Constructor */
        public UplinkIOHandler( WTVCache wtvCache, NioSocketConnector connector )
        {
            this.wtvCache = wtvCache;
            this.connector = connector;
            timer = new Timer( );
        }

    /** Unused interface overrides */
        public void sessionCreated( IoSession session ) throws Exception 
        {
        }

        public void messageSent( IoSession session, Object message ) throws Exception 
        {
        }

    /** Called when a new session is opened, sending the initial message. */
        public void sessionOpened( IoSession session ) throws Exception 
        {
            // send a handshake.
            session.write( new WTVHandshake( (byte)1, (short)3, (byte)0 ) );
        }

    /** Called when a session is closed. takes care of the cleanup. */
        public void sessionClosed( IoSession session ) throws Exception 
        {
            UplinkSession s = (UplinkSession) session.getAttachment( );
            s.sessionClosed( session );

            //Object o = session.getAttribute( UplinkIOHandler.SessionAttribute.GETGAME );

            //// gamelist fetches don't matter
            //if( o == null )
            //    return;

            //Integer gid = (Integer) o;
            //Game game = gameCache.getGameById( gid );

            ////logger.trace( "Session closed: "+session+" ("+gid+")" );

            //// ignore unknown games
            //if( game == null )
            //    return;

            //// is the game finished or gone? all okay then.
            //if( game.state >= Game.STATE_FINISHED )
            //    return;

            //// game is not finished -> retry to download
            //Integer numret = (Integer) session.getAttribute( UplinkIOHandler.SessionAttribute.NUMRETRIES );
            //scheduleGameDownloadRetry( session.getRemoteAddress( ), gid, numret++ );
            //logger.trace( "Retrying game: "+session+" ("+gid+") " );
        }

    /** Called when a session is idle. */
        public void sessionIdle( IoSession session, IdleStatus status ) throws Exception 
        {
            UplinkSession s = (UplinkSession) session.getAttachment( );
            s.sessionIdle( session, status );
            
            //// Is it a broken game?
            //
            //Object o = session.getAttribute( UplinkIOHandler.SessionAttribute.GETGAME );

            //// gamelist fetches must not idle.
            //if( o == null )
            //{
            //    logger.warn( "Disconnecting gamelist fetch due to idling." );
            //    session.close( );
            //    return;
            //}

            //Integer gid = (Integer) o;
            //Game game = gameCache.getGameById( gid );

            ////logger.trace( "Session idle: "+session+" ("+gid+") "+status );

            //// the game is unknown? disconnect
            //if( game == null )
            //{
            //    session.close( );
            //    return;
            //}

            //if( game.status == Game.STATUS_BROKEN )
            //{
            //    logger.trace( "Finished broken game, heuristically" );
            //    // It is broken, we idle for a while -> probably have all data!
            //    game.state = Game.STATE_FINISHED;
            //    session.close( );
            //    return;
            //}
        }

    /** Handle an caught exception. */
        public void exceptionCaught( IoSession session, Throwable cause ) throws Exception 
        {
            logger.error( "Caught exception "+session+": "+cause );
            cause.printStackTrace( );
            session.close( );
        }

    /** Handle received message. */
        public void messageReceived( IoSession session, Object message ) throws Exception 
        {
            /** Fetch the session object. */
                UplinkSession s = (UplinkSession) session.getAttachment( );

            /** Hardcore message tracing. */
                //logger.trace( "Message received "+session+": "+message );
                
            /** Security check that removes sessions which are way too old. */
                if( System.currentTimeMillis( ) - session.getCreationTime( ) > 12*60*60*1000  )
                {
                    logger.warn( "Forcefully closing session which is older than 12 hours!" );
                    session.close( );
                    return;
                }

            /** If this is the handshake reply start the auth. */
                if( message instanceof WTVHandshake )
                {
                    LogonRequest request = new LogonRequest( LogonRequest.CLIENTTYPE_CLIENT, (byte) 1, (short) 5, (byte) 0 );
                    session.write( request );
                    return;
                }

            /** If the session is not logged in yet, process related messages. */
                if( s.getLoggedin( ) == false )
                {
                    if( message instanceof LogonRequestReply )
                    {
                        LogonRequestReply request = (LogonRequestReply) message;
                        if( request.protoMajor != 1 
                        ||  request.protoMinor != 5 )
                        {
                            logger.error( request+" has invalid version, can't proceed!" );
                            session.close( );
                            return;
                        }

                        LogonAuthorize reply = new LogonAuthorize( WTVCache.applicationName, (byte)net.volcore.wtvcache.VersionInfo.major, (short)net.volcore.wtvcache.VersionInfo.minor, (byte)net.volcore.wtvcache.VersionInfo.release, LogonAuthorize.CLIENTTYPE_CLIENT, request.protoMajor, request.protoMinor, request.protoRelease, "WTVCache", "nopass"  );
                        session.write( reply );
                    } else
                    if( message instanceof LogonAuthorizeReply )
                    {
                        LogonAuthorizeReply authorize = (LogonAuthorizeReply) message;

                        if( authorize.code == 0 )
                        {
                            logger.error( "Login rejected: "+authorize.reason );
                            session.closeOnFlush( );
                            return;
                        } else
                        {
                            s.setLoggedin( true );
                            s.onLoggedIn( session );
                        }
                    } else
                    {
                        logger.error( "Unknown "+message+" during logon!" );
                        session.close( );
                    }

                    return;
                }

            /** Always handle ping packets. */
                if( message instanceof RelayPing )
                {
                    RelayPing ping = (RelayPing) message;
                    RelayPong pong = new RelayPong( ping.timestamp );
                    session.write( pong );
                    return;
                }

            /** Let the handlers process the message. */
                s.processMessage( session, message );
        }
}
