/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmaster.upload;

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
        import java.util.concurrent.*;
        import java.util.*;
        import java.util.regex.*;
    // wtvmaster
        import net.volcore.wtvmaster.user.*;
        import net.volcore.wtvmaster.game.*;
        import net.volcore.wtvmaster.*;

public class RecorderIoHandler extends IoHandlerAdapter
{
    static Logger   logger = LoggerFactory.getLogger( "RecorderIoHandler" );

    /** Constants. */
        /** Generic. */
            public static final int     RECORDER_UPLOADLIMIT = 0;
            public static final int     NUM_THREADPOOLWORKER = 4;

            public final static Pattern         orgaPattern  = Pattern.compile( "^\\[([^\\]]+)\\] .*$" );
        
            protected int               SET_GAME_BROKEN_TIMER = 5*60*1000;


        /** Session attributes. */
            public enum SessionAttribute
            {
                LOGGEDIN,           // contains Integer userid
                USERNAME,           // contains String username
                NUMFAILEDLOGINS,
                ISUPLOADINGGAME,    // contains Integer gameid
                ORGANISATIONLIST,   // contains LinkedList< Organization >
            }

    /** Member variables */
        /** Threadpool for async  */
            protected ScheduledThreadPoolExecutor       workerThreadPool = new ScheduledThreadPoolExecutor( NUM_THREADPOOLWORKER );
        /** Reference to daddy. */
            protected WTVMaster wtvMaster;
        /** The asyncronous homepage notificator */
            protected HomepageDispatcher                homepageDispatcher;
        /** A timer used for broken games. */
            protected Timer         timer;

    /** Auxiliary. */
        /** Constructor */
            public RecorderIoHandler( WTVMaster wtvMaster )
            {
                this.wtvMaster = wtvMaster;
                homepageDispatcher = new HomepageDispatcher( wtvMaster );
                timer = new Timer( true );
            }

    /** IoHandlerAdapter callbacks. */
        /** The session opened event is used to flag every session a server session. */
            public void sessionOpened( IoSession session ) throws Exception 
            {
                session.setAttribute( WTVProtocolDecoder.States.IS_SERVER );
                session.setAttribute( SessionAttribute.NUMFAILEDLOGINS, new Integer( 0 ) );
            }

        /** The session closed event is currently just stub. */
            public void sessionClosed( IoSession session ) throws Exception 
            {
                // uploading a game? broken!
                    if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) != null )
                    {
                        final Game game = wtvMaster.gameDB.fetchGame((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME ));

                        if( game == null )
                        {
                            logger.error( "Sanity check failed: uploader disconnected, was uploading null game! "+session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) );
                            return;
                        }
                        
                        synchronized( game )
                        {
                            logger.trace( "Uploader disconnected!" );
                            game.uploaderId = UserDatabase.INVALID_USERID;
                            game.uploadPosition = 0;
                        }

                            timer.schedule( new TimerTask( ) 
                                {
                                    public void run( )
                                    {
                                        synchronized( game )
                                        {
                                            if( game.getStatus( ) != Game.STATUS_COMPLETED 
                                            &&  game.uploaderId == UserDatabase.INVALID_USERID )
                                            {
                                                logger.trace( "Uploader disconnected, was still uploading game "+game+" and timer run of! Set to broken." );
                                                game.setStatus( Game.STATUS_BROKEN );
                                                wtvMaster.gameDB.storeGame( game );
                                                if( game.getCertified( ) )
                                                    wtvMaster.gameDB.buildGameList( );
                                                homepageDispatcher.dispatch( game.getId( ) );
                                            }
                                        }
                                    }
                                }, SET_GAME_BROKEN_TIMER );
                    }
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

        /** The sent message function does virtually nothing. */
            public void messageSent( final IoSession session, Object message ) throws Exception
            {
            }

        /** The heartpiece of the handler, the message received function. */
            public void messageReceived( final IoSession session, Object message ) throws Exception 
            {
                //logger.trace( "received: "+message );
                /** Process the handshake. */
                    if( message instanceof WTVHandshake )
                    {
                        WTVHandshake handshake = (WTVHandshake) message;

                        if( handshake.major != 1
                        &&  handshake.minor != 3 )
                        {
                            logger.error( "Invalid login version!" );
                            session.write( new WTVHandshake( (byte) 0, (short) 0, (byte)0 ) );
                            session.close( );
                            return;
                        }

                        // handshake was okay.
                        session.write( new WTVHandshake( (byte) 1, (short) 3, (byte) 0 ) );

                        return;
                    }

                /** Process ping messages. */
                    if( message instanceof RelayPong )
                    {
                        //logger.trace(" tmp*pong*." );
                        // just ignore. these are only used to reset the writer idle timer
                        return;
                    }

                /** Process login data. */
                    if( session.getAttribute( SessionAttribute.LOGGEDIN ) == null )
                    {
                        /** LogonRequest is used to negotiate the protocol. */
                            if( message instanceof LogonRequest )
                            {
                                LogonRequest logonRequest = (LogonRequest) message;

                                if( logonRequest.clientType != LogonRequest.CLIENTTYPE_RECORDER 
                                ||  logonRequest.protoMajor != 1
                                ||  logonRequest.protoMinor != 6)
                                {
                                    GenericError error = new GenericError( GenericError.ERROR_LOGINDISALLOWED, "" );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                LogonRequestReply reply = new LogonRequestReply( logonRequest.protoMajor, logonRequest.protoMinor, logonRequest.protoRelease );
                                session.write( reply );
                                return;
                            }
                        /** LogonAuthorize is used to handle login details. */
                            else if( message instanceof LogonAuthorize ) {
                                final LogonAuthorize auth = (LogonAuthorize) message;

                                if( auth.clientType != LogonRequest.CLIENTTYPE_RECORDER 
                                ||  auth.protoMajor != 1
                                ||  auth.protoMinor != 6

                                // added to fix 1.23 issues
                                ||  (auth.appMinor <= 51 
                                    && auth.appMajor == 0 )
                                
                                )
                                {
                                    GenericError error = new GenericError( GenericError.ERROR_LOGINDISALLOWED, "" );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                workerThreadPool.submit( new Runnable( )
                                    {
                                        public void run( )
                                        {
                                            int userid = wtvMaster.userDB.isValidUploadAccount( auth.userName, auth.password );
                                            if( userid == UserDatabase.INVALID_USERID )
                                            {
                                                if( 3 <= (Integer) session.getAttribute( SessionAttribute.NUMFAILEDLOGINS ) )
                                                {
                                                    GenericError error = new GenericError( GenericError.ERROR_PASSWORD, "" );
                                                    session.write( error );
                                                    session.closeOnFlush( );
                                                    return;
                                                } else
                                                {
                                                    session.setAttribute( SessionAttribute.NUMFAILEDLOGINS, 1+ (Integer) session.getAttribute( SessionAttribute.NUMFAILEDLOGINS ) );
                                                    LogonAuthorizeReply reply = new LogonAuthorizeReply( "Invalid username or password." );
                                                    session.write( reply );
                                                }
                                                return;
                                            }

                                            session.setAttribute( SessionAttribute.LOGGEDIN, new Integer( userid ) );
                                            session.setAttribute( SessionAttribute.USERNAME, auth.userName );
                                            LogonAuthorizeReply reply = new LogonAuthorizeReply( wtvMaster.appName, wtvMaster.versionMajor, wtvMaster.versionMinor, wtvMaster.versionRelease, wtvMaster.title, wtvMaster.motd, wtvMaster.ircServer, wtvMaster.ircPort, wtvMaster.ircChannel, wtvMaster.url );
                                            session.write( reply );
                                        }
                                    } );

                                return;
                            }
                        logger.trace( "Unknown message during logon received "+session+": "+message );
                        session.close( );
                        return;
                    }

                /** Process recorder messages. */
                    /** RecorderGetServerDetails - return the server settings. */
                        if( message instanceof RecorderGetServerDetails )
                        {
                            RecorderServerDetails details = new RecorderServerDetails( RECORDER_UPLOADLIMIT, (int)(System.currentTimeMillis( )/1000) );
                            session.write( details );
                            return;
                        }
                    /** RecorderCreateGame - Check if the game exists and create it if it does not exist. */
                        else if( message instanceof RecorderCreateGame )
                        {
                            final RecorderCreateGame create = (RecorderCreateGame) message;

                            // check that the recorder is not currently uploading a game already.
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) != null )
                                {
                                    logger.error( "Uploader is already uploading a game! Uploading multiple games at the same time not allowed!" );
                                    RecorderGameCreated reply = new RecorderGameCreated( create.replyId, GameDatabase.INVALID_GAMEID, RecorderGameCreated.RESULT_FAILED, 0, 0, 0 );
                                    session.write( reply );
                                    return;
                                }

                                logger.info( "New game '"+create.gameName+"' with '"+create.gameComment+"' by '"+session.getAttribute( SessionAttribute.USERNAME )+"'" );

                            // check duplicate game or create if not dupe (atomic).
                                final String username = (String)session.getAttribute( SessionAttribute.USERNAME );
                                final Game game = wtvMaster.gameDB.createGame( create.gameInfo, create.gameName, create.gameComment, create.date, username );

                                if( game == null )
                                {
                                    logger.info( "Failed to create game!" );
                                    RecorderGameCreated reply = new RecorderGameCreated( create.replyId, GameDatabase.INVALID_GAMEID, RecorderGameCreated.RESULT_FAILED, 0, 0, 0 );
                                    session.write( reply );
                                    return;
                                }

                            synchronized( game )
                            {
                                // is someone else uploading?
                                    if( game.uploaderId != UserDatabase.INVALID_USERID )
                                    {
                                        logger.warn( "Someone else is already uploading this game!" );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Someone else is already uploading this game." );
                                        session.write( error );
                                        return;
                                    }

                                // is it a finished, old game?
                                    if( game.getStatus( ) == Game.STATUS_COMPLETED )
                                    {
                                        logger.trace( "Game is already complete, no need to upload." );
                                        RecorderGameCreated reply = new RecorderGameCreated( create.replyId, game.getId( ), RecorderGameCreated.RESULT_FINISHED, game.getLastSeed( ), game.getGameDataSize( ), game.getLastPacketSize( ) );
                                        session.write( reply );
                                        return;
                                    }
                                
                                // this creategame is valid.
                                    game.uploaderId = (Integer) session.getAttribute( SessionAttribute.LOGGEDIN );
                                    session.setAttribute( SessionAttribute.ISUPLOADINGGAME, new Integer( game.getId( ) ) );

                                // create or resume?
                                    if( game.getStatus( ) <= Game.STATUS_NEW )
                                    {
                                        // its a new game!
                                        logger.trace( session.getAttribute( SessionAttribute.USERNAME )+" is uploading game "+game.getId( ) );
                                        game.uploadPosition = 0;
                                        RecorderGameCreated reply = new RecorderGameCreated( create.replyId, game.getId( ), RecorderGameCreated.RESULT_CREATED, 0, 0, 0 );
                                        session.write( reply );
                                    } else
                                    {
                                        // its an old game.
                                        // it is not finished (either broken or new)
                                        logger.trace( session.getAttribute( SessionAttribute.USERNAME )+" resuming an unfinished game "+game.getId( ) );
                                        RecorderGameCreated reply = new RecorderGameCreated( create.replyId, game.getId( ), RecorderGameCreated.RESULT_INCOMPLETE, game.getLastSeed( ), game.getGameDataSize( ), game.getLastPacketSize( ) );
                                        session.write( reply );
                                    }
                            }

                            // Extract the organisation name and verify it
                                // do in seperate thread. 
                                workerThreadPool.submit( new Runnable( )
                                    {
                                        public void run( )
                                        {
                                            // extract organisation name from game name
                                                String name = game.getName( );
                                                String organisation = "";

                                                Matcher m = orgaPattern.matcher( name );

                                                if( m.find( ) )
                                                {
                                                    if( m.group( 1 ) != null )
                                                    {
                                                        organisation = m.group( 1 );
                                                    }
                                                }


                                            // check the organisation
                                                boolean certified = false;
                                                boolean valid = false;
                                                if( organisation.length( )>0 )
                                                {
                                                    int v = wtvMaster.userDB.validateOrganisation( username, organisation );
                                                    certified = (v&UserDatabase.CERTIFIED_ORGANISATION_MASK)==UserDatabase.CERTIFIED_ORGANISATION_MASK;
                                                    valid = (v&UserDatabase.VALID_ORGANISATION_MASK)==UserDatabase.VALID_ORGANISATION_MASK;
                                                }

                                                if( valid == false )
                                                    organisation = "";

                                                if( certified )
                                                {
                                                    // save information.
                                                        synchronized( game )
                                                        {
                                                            game.setCertified( true );
                                                            game.setOrganisation( organisation );
                                                            wtvMaster.gameDB.storeGame( game );
                                                        }

                                                    // add it to the current game list.
                                                        wtvMaster.gameDB.addGameToList( game.getId( ) );
                                                        wtvMaster.gameDB.buildGameList( );
                                                } else
                                                {
                                                    synchronized( game )
                                                    {
                                                        game.setCertified( false );
                                                        game.setOrganisation( organisation );
                                                        wtvMaster.gameDB.storeGame( game );
                                                    }
                                                }

                                            // notify the homepage
                                                homepageDispatcher.dispatch( game.getId( ) );
                                        }
                                    } );

                            return;
                        } 
                    /** RecorderResumeGameData - Sanity check and reply with acceptance status  */
                        else if( message instanceof RecorderResumeGameData )
                        {
                            final RecorderResumeGameData resume = (RecorderResumeGameData) message;

                            // check that the recorder is uploading this game
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) == null 
                                || ((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME )) != resume.gameId )
                                {
                                    logger.error( "Uploader sent resume for a game he is not uploading! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (13). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                logger.info( "Received resume game "+resume.gameId );

                            // fetch the game 
                                Game game = wtvMaster.gameDB.fetchGame( resume.gameId );

                                if( game == null )
                                {
                                    logger.error( "Sanity check failed: uploader sending resume for null game! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (14). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                            synchronized( game )
                            {
                                // more sanity checks
                                    if( game.uploaderId != (Integer) session.getAttribute( SessionAttribute.LOGGEDIN ) )
                                    {
                                        logger.error( "Sanity check failed: uploaderId not current user! "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (15). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                    //int status = game.getStatus( );
                                    //if( status != Game.STATUS_BROKEN )
                                    //{
                                    //    logger.error( "Sanity check failed: Game resume for game in wrong state: "+message+" "+game );
                                    //    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (16). Please retry the upload." );
                                    //    session.write( error );
                                    //    session.closeOnFlush( );
                                    //    return;
                                    //}

                                // verify the provided data
                                    if( game.verifyResumeInfo( resume.numBytes, resume.lastTime, resume.lastSize, resume.data ) == false )
                                    {
                                        logger.error( "Sanity check failed: Resume game with invalid information! "+message+" "+game );

                                        RecorderResumingGameData reply = new RecorderResumingGameData( resume.gameId, 0 );
                                        session.write( reply );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                // write the new information
                                    game.setStatus( Game.STATUS_STARTED );
                                    game.uploadPosition = resume.numBytes;
                                    game.setGameLength( resume.lastTime );
                                    game.truncate( resume.numBytes, resume.lastSize );
                                    wtvMaster.gameDB.storeGame( game );

                                    if( game.getCertified( ) )
                                        wtvMaster.gameDB.buildGameList( );
                            }
                            
                            // send the reply.
                                RecorderResumingGameData reply = new RecorderResumingGameData( resume.gameId, 1 );
                                session.write( reply );
                                
                                return;
                        }
                    /** RecorderDetailUpdate - Replace the current game details with new data */
                        else if( message instanceof RecorderDetailUpdate )
                        {
                            final RecorderDetailUpdate detail = (RecorderDetailUpdate) message;

                            // check that the recorder is uploading this game
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) == null 
                                || ((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME )) != detail.gameId )
                                {
                                    logger.error( "Uploader sent detail update for a game he is not uploading! "+detail );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (0). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                logger.info( "Received game details for game "+detail.gameId );

                            // fetch the game 
                                Game game = wtvMaster.gameDB.fetchGame( detail.gameId );

                                if( game == null )
                                {
                                    logger.error( "Sanity check failed: uploader sending details for null game! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (1). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                            synchronized( game )
                            {
                                // more sanity checks
                                    if( game.uploaderId != (Integer) session.getAttribute( SessionAttribute.LOGGEDIN ) )
                                    {
                                        logger.error( "Sanity check failed: uploaderId not current user! "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (2). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                    int status = game.getStatus( );
                                    if( status != Game.STATUS_NEW )
                                    {
                                        logger.error( "Sanity check failed: Game details for game in wrong state: "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (3). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                // finally write the game
                                    game.writeGameDetails( detail.details );
                            }
                            
                            // there is no reply sent to the uploader for this messsage
                                return;
                        }
                    /** RecorderGameStart - Update the game start information */
                        else if( message instanceof RecorderGameStart )
                        {
                            final RecorderGameStart start = (RecorderGameStart) message;

                            // check that the recorder is uploading this game
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) == null 
                                || ((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME )) != start.gameId )
                                {
                                    logger.error( "Uploader sent start for a game he is not uploading! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (4). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                logger.info( "Received game start for game "+message );

                            // fetch the game 
                                Game game = wtvMaster.gameDB.fetchGame( start.gameId );

                                if( game == null )
                                {
                                    logger.error( "Sanity check failed: uploader sending start for null game! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (5). Please retry the upload." );
                                    session.write( error );
                                    session.close( );
                                    return;
                                }

                                synchronized( game )
                                {
                                // more sanity checks
                                    if( game.uploaderId != (Integer) session.getAttribute( SessionAttribute.LOGGEDIN ) )
                                    {
                                        logger.error( "Sanity check failed: uploaderId not current user! "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (6). Please retry the upload." );
                                        session.write( error );
                                        session.close( );
                                        return;
                                    }

                                    int status = game.getStatus( );
                                    if( status != Game.STATUS_NEW 
                                    &&  status != Game.STATUS_DELAY )
                                    {
                                        logger.error( "Sanity check failed: Game start for game in wrong state: "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (7). Please retry the upload." );
                                        session.write( error );
                                        session.close( );
                                        return;
                                    }

                                // finally write the info
                                    game.setLastSeed( start.lastSeed );
                                    game.setDelay( start.delay );
                                    game.setDate( start.date );

                                    if( start.delay == 0 )
                                    {
                                        game.setStatus( Game.STATUS_STARTED );

                                        // also prepare the upload
                                        game.uploadPosition = 0;

                                        // write back to database in case its a start message. 
                                        wtvMaster.gameDB.storeGame( game );
                                        if( game.getCertified( ) )
                                            wtvMaster.gameDB.buildGameList( );
                                    } else
                                    {
                                        game.setStatus( Game.STATUS_DELAY );
                                        if( game.getCertified( ) )
                                            wtvMaster.gameDB.buildGameList( );
                                        // don't write delay messages to database
                                    }
                                }
                            
                            // there is no reply sent to the uploader for this messsage
                                return;
                        }
                    /** RecorderUpdateGameData - Add a game data packet. */
                        else if( message instanceof RecorderUpdateGameData )
                        {
                            final RecorderUpdateGameData data = (RecorderUpdateGameData) message;

                            // sanity check
                                    if( data.lastPacket != 1 )
                                    {
                                        logger.error( message+" invalid, not exactly one packet payload!" );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (12). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                            // check that the recorder is uploading this game
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) == null 
                                || ((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME )) != data.gameId )
                                {
                                    logger.error( "Uploader sent data for a game he is not uploading! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (8). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                            // fetch the game 
                                Game game = wtvMaster.gameDB.fetchGame( data.gameId );

                                if( game == null )
                                {
                                    logger.error( "Sanity check failed: uploader sending data for null game! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (9). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                synchronized( game )
                                {
                                // more sanity checks
                                    if( game.uploaderId != (Integer) session.getAttribute( SessionAttribute.LOGGEDIN ) )
                                    {
                                        logger.error( "Sanity check failed: uploaderId not current user! "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (10). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                    int status = game.getStatus( );
                                    if( status != Game.STATUS_STARTED )
                                    {
                                        logger.error( "Sanity check failed: Game data for game in wrong state: "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (11). Please retry the upload." );
                                        session.write( error );
                                        session.closeOnFlush( );
                                        return;
                                    }

                                // finally write the data
                                    game.addPacket( data.time, data.size, data.data );
                                }
                            
                            // there is no reply sent to the uploader for this messsage
                                return;
                        } 
                    /** RecorderGameFinish - Verify the correct number of packets and release the upload lock */
                        else if( message instanceof RecorderGameFinish )
                        {
                            final RecorderGameFinish finish = (RecorderGameFinish) message;

                            // check that the recorder is uploading this game
                                if( session.getAttribute( SessionAttribute.ISUPLOADINGGAME ) == null 
                                || ((Integer)session.getAttribute( SessionAttribute.ISUPLOADINGGAME )) != finish.gameId )
                                {
                                    logger.error( "Uploader sent finish for a game he is not uploading! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (17). Please retry the upload." );
                                    session.write( error );
                                    session.closeOnFlush( );
                                    return;
                                }

                                logger.info( "Received game finish for game "+message );

                            // fetch the game 
                                Game game = wtvMaster.gameDB.fetchGame( finish.gameId );

                                if( game == null )
                                {
                                    logger.error( "Sanity check failed: uploader sending finish for null game! "+message );
                                    GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (18). Please retry the upload." );
                                    session.write( error );
                                    session.close( );
                                    return;
                                }

                                synchronized( game )
                                {
                                // more sanity checks
                                    if( game.uploaderId != (Integer) session.getAttribute( SessionAttribute.LOGGEDIN ) )
                                    {
                                        logger.error( "Sanity check failed: uploaderId not current user! "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (19). Please retry the upload." );
                                        session.write( error );
                                        session.close( );
                                        return;
                                    }

                                    int status = game.getStatus( );
                                    if( status != Game.STATUS_STARTED )
                                    {
                                        logger.error( "Sanity check failed: Game finish for game in wrong state: "+message+" "+game );
                                        GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Internal server error (20). Please retry the upload." );
                                        session.write( error );
                                        session.close( );
                                        return;
                                    }

                                // verify the number of packets
                                    if( game.getNumPackets( ) != finish.result )
                                    {
                                        logger.error( "Sanity check failed: Wrong number of packets for game: "+message+" "+game );
                                        RecorderGameFinishReply finished = new RecorderGameFinishReply( finish.gameId, 1/** 1 means failure */ );
                                        session.write( finished );

                                        game.setStatus( Game.STATUS_BROKEN );
                                        game.uploaderId = UserDatabase.INVALID_USERID;
                                        session.setAttribute( SessionAttribute.ISUPLOADINGGAME, null );
                                        wtvMaster.gameDB.storeGame( game );
                                        homepageDispatcher.dispatch( game.getId( ) );
                                        return;
                                    }

                                // finally write the info
                                    game.setStatus( Game.STATUS_COMPLETED );
                                    game.uploaderId = UserDatabase.INVALID_USERID;
                                    session.setAttribute( SessionAttribute.ISUPLOADINGGAME, null );
                                    wtvMaster.gameDB.storeGame( game );
                                }

                                RecorderGameFinishReply finished = new RecorderGameFinishReply( finish.gameId, 0/** 0 means success*/ );
                                session.write( finished );

                            // rebuild the game list
                                if( game.getCertified( ) )
                                    wtvMaster.gameDB.buildGameList( );

                            // notify the homepage
                                homepageDispatcher.dispatch( game.getId( ) );
                            
                            // there is no reply sent to the uploader for this messsage
                                return;
                        }
                        // else default: 
                            logger.trace( "Unknown message received "+session+": "+message );
                            GenericError error = new GenericError( GenericError.ERROR_CUSTOM, "Not yet implemented!" );
                            session.write( error );
                            session.closeOnFlush( );
            }
}

