/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvcache;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.*;
        import net.volcore.wtvmina.messages.*;
    // java
        import java.util.concurrent.atomic.*;
        import java.net.*;
        import javax.net.ssl.*;
    // Apache commons CLI
        import org.apache.commons.cli.*;
    // Mina
        import org.apache.mina.transport.socket.nio.*;
        import org.apache.mina.filter.codec.*;
        import org.apache.mina.filter.ssl.*;
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
        import org.apache.mina.core.future.*;
    // asyncweb
        import org.apache.asyncweb.common.*;
        import org.apache.asyncweb.common.codec.*;
    // wtvcache
        import net.volcore.wtvcache.game.*;
        import net.volcore.wtvcache.uplink.*;
        import net.volcore.wtvcache.http.*;
        import net.volcore.wtvcache.relay.*;
        import net.volcore.wtvcache.accesslog.*;


/*******************************************************************************
         Main class
 *******************************************************************************/
public class WTVCache
{
    static Logger           logger = LoggerFactory.getLogger( "WTVCache" );
    static Options          options;
    static HelpFormatter    formatter = new HelpFormatter();

    public static final String   applicationName = "WTVCache";

    /** Protected members. */
        /** The provided config. */
            protected Config        config;
        /** Shutdown trigger flag. */
            protected boolean       doShutdown = false;
        /** The web interface acceptor. */
            protected NioSocketAcceptor         webAcceptor;
        /** The WTVCache server acceptor. */
            protected NioSocketAcceptor         acceptor;
        /** The uplink connector. */
            protected NioSocketConnector        connector;
        /** The GameCache holds all the game infos returned from the server. */
            public    GameCache                 gameCache;
        /** The access log. */
            protected AccessLog                 accessLog;
        /** Last time the game list was acquired. */
            protected long                      lastGameListRequest = 0;
            protected long                      lastCachePrune = 0;
        /** Uptime counter. */
            protected long                      timeStarted = System.currentTimeMillis( );

    /** Public access. */
        /** Getter for gameCache */
        	public GameCache getGameCache( ) { return gameCache; }
        /** Setter for gameCache */
        	public void setGameCache( GameCache gameCache ) { this.gameCache = gameCache; }
        /** Getter for accessLog */
        	public AccessLog getAccessLog( ) { return accessLog; }
        /** Setter for accessLog */
        	public void setAccessLog( AccessLog accessLog ) { this.accessLog = accessLog; }

    /** Statistics. */
        public long getUptime( )                        { return System.currentTimeMillis( ) - timeStarted; }
        public String getVersionString( )               { return VersionInfo.major+"."+VersionInfo.minor+"."+VersionInfo.release+"-"+VersionInfo.special.trim( ); }
        public long getNumSessions( )                   { return acceptor.getManagedSessionCount( ); }
        public long getTotalSessionCount( )             { return acceptor.getStatistics( ).getCumulativeManagedSessionCount( ); }
        public long getTotalBytesRead( )                { return acceptor.getStatistics( ).getReadBytes( ); }
        public long getTotalBytesWritten( )             { return acceptor.getStatistics( ).getWrittenBytes( ); }
        public double getBytesReadPerSecond( )          { return acceptor.getStatistics( ).getReadBytesThroughput( ); }
        public double getBytesWrittenPerSecond( )       { return acceptor.getStatistics( ).getWrittenBytesThroughput( ); }
        public long getPeakSessionCount( )              { return acceptor.getStatistics( ).getLargestManagedSessionCount( ); }
        public double getPeakBytesReadPerSecond( )      { return acceptor.getStatistics( ).getLargestReadBytesThroughput( ); }
        public double getPeakBytesWrittenPerSecond( )   { return acceptor.getStatistics( ).getLargestWrittenBytesThroughput( ); }

            /** Getter for connector */
            	public NioSocketConnector getConnector( ) { return connector; }
            /** Setter for connector */
            	public void setConnector( NioSocketConnector connector ) { this.connector = connector; }
            /** Getter for acceptor */
            	public NioSocketAcceptor getAcceptor( ) { return acceptor; }
            /** Setter for acceptor */
            	public void setAcceptor( NioSocketAcceptor acceptor ) { this.acceptor = acceptor; }
            /** Getter for webAcceptor */
            	public NioSocketAcceptor getWebAcceptor( ) { return webAcceptor; }
            /** Setter for webAcceptor */
            	public void setWebAcceptor( NioSocketAcceptor webAcceptor ) { this.webAcceptor = webAcceptor; }

    /** Constructor */
        protected WTVCache( Config config ) throws ConfigException
        {
            this.config = config;

            /** Sanitycheck the config. */
                if( config == null ) 
                    throw new ConfigException( "No config provided!" );

                if( config.listenPort == 0 )
                    throw new ConfigException( "Please specify a port!" );

                if( config.master == null )
                    throw new ConfigException( "Please specify at least one master server!" );
        }

    /** Small subclass that takes care of the Control-C termination. */
        protected class ShutdownThread extends Thread
        {
            public void run( )
            {
                logger.info( "Shutdown command received, terminating." );
                doShutdown = true;
                final int wait = 3000;
                try{
                    // wait at most 3 seconds, then forcefully terminate.
                    Thread.sleep( wait );
                    logger.error( "Timed out waiting "+wait+"ms for main thread to shut down, forcefully terminating." );
                }catch( Exception e ){ }
            }
        }

    /** should be called once in the beginning, sets up all the required logging. */
        protected static void initializeLogging( )
        {
            // Initialize logging
                String name = "wtvcache-log4j.properties";
                URL log4jfile = ClassLoader.getSystemClassLoader( ).getResource( name );
                if( log4jfile == null )
                    throw new Error( "Could not find "+name+"!" );
                org.apache.log4j.PropertyConfigurator.configure( log4jfile );
        }

    /** Small subclass that contains all the parameters parsed at the command line. */
        static protected class Config
        {
            public int  httpPort = 8081;
            public int  listenPort = 0;
            public SocketAddress[] master = null;
            public String accessLogDir = "accesslog";
        }

        static protected class ConfigException extends Exception
        {
            public ConfigException( String message )
            {
                super( message );
            }
        }

    /** Use apache CLI to parse the command line */
        protected static Config parseOptions( String[] args )
        {
            // Build options
                options = new Options( );

                options.addOption( "h", "help", false, "print this message" );
                options.addOption( "m", "master", true, "master server, format is <addr>:<port>, can be set 16 times , for multiple master servers" );
                options.getOption( "m" ).setArgs( 16 );
                options.addOption( "p", "port", true, "port to run the cache on" );
                options.addOption( "l", "accesslog", true, "Directory of accesslog" );

            // Parse the command line options
                CommandLineParser parser = new PosixParser();

                CommandLine line;
                try {
                    // parse the command line arguments
                    line = parser.parse( options, args );
                }
                catch( org.apache.commons.cli.ParseException exp ) {
                    // oops, something went wrong
                    logger.error( "Parsing failed.  Reason: " + exp.getMessage() );
                    return null;
                }

            // Process the parsed options
                if( line.hasOption( "help" ) )
                {
                    formatter.printHelp( "wtvcache", options );
                    return null;
                } 

                Config config = new Config( );

                if( line.hasOption( 'p' ) )
                    config.listenPort = Integer.parseInt( line.getOptionValue( 'p' ) );

                if( line.hasOption( 'l' ) )
                    config.accessLogDir = line.getOptionValue( 'l' );

                if( line.hasOption( 'm' ) )
                {
                    String[] v = line.getOptionValues( 'm' );

                    config.master = new SocketAddress[ v.length ];

                    for( int i=0; i<v.length; ++i )
                    {
                        String[] spl = v[i].split( ":" );
                        String addr = spl[0];
                        int    port = Integer.parseInt( spl[1] );
                        config.master[i] = new InetSocketAddress( addr, port );
                    }
                }

            return config;
        }

    /** Main entry point */
        public static void main( String[] args )
        {
            initializeLogging( );

            Config config = parseOptions( args );

            // start the wtvcache.
                WTVCache wtvCache;
                ShutdownThread shutdownThread;

                try {
                    wtvCache = new WTVCache( config );
                    shutdownThread = wtvCache.new ShutdownThread( );
                    Runtime.getRuntime().addShutdownHook( shutdownThread );
                    wtvCache.run( );
                } catch( ConfigException e )
                {
                    formatter.printHelp( "wtvcache", options );
                    logger.error( e.getMessage( ) );
                    return;
                }

            // wake the shutdown thread
                shutdownThread.interrupt( );
                logger.info( "WTVCache terminated." );

            // remove it in case its still there.
                try {
                    Runtime.getRuntime( ).removeShutdownHook( shutdownThread );
                    System.exit( 0 );
                } catch( Exception e ) { }
        }

    /** Game related tasks. */
        /** Contact the master server to query existential information about a game. */
            public void initiateGameInfoFetch( final Game game, boolean force )
            {
                if( force == false )
                {
                    if( game.getGameInfo( ) != null  )
                        return;

                    if( game.getFetchingInfo( ) == true )
                        return;
                }

                game.setFetchingInfo( true );

                final SocketAddress addr = config.master[ game.getServerId( ) ];
                connector.connect( addr ).addListener( 
                    new IoFutureListener< ConnectFuture >( )
                        {
                            public void operationComplete( ConnectFuture future )
                            {
                                if( future.isConnected( ) == true )
                                    future.getSession( ).setAttachment( new FetchGameInfoSession( game.getServerId( ), game, WTVCache.this ) );
                                else
                                {
                                    logger.warn( "Failed to connect to master server "+addr+" to fetch info for "+game+"!" );

                                    // remove the game from cache so next time someone requests info a new attempt is made.
                                    // but only if this is not a forced, secondary attempt to update the game infos
                                    if( game.getGameInfo( ) == null )
                                        gameCache.removeGame( game.getGameId( ) );
                                }
                            }
                        } );
            }

        /** Contact the associated server to fetch a game. */
            public void initiateGameFetch( final Game game )
            {
                if( game.getCacheState( ) != Game.CACHESTATE_NONE )
                    return;

                if( game.getFetchingGame( ) == true )
                    return;


                accessLog.logCacheIn( game.getGameId( ) );
                game.setFetchingGame( true );

                final SocketAddress addr = config.master[ game.getServerId( ) ];
                connector.connect( addr ).addListener( 
                    new IoFutureListener< ConnectFuture >( )
                        {
                            public void operationComplete( ConnectFuture future )
                            {
                                if( future.isConnected( ) == true )
                                    future.getSession( ).setAttachment( new FetchGameSession( game.getServerId( ), WTVCache.this, game ) );
                                else
                                {
                                    logger.warn( "Failed to connect to master server "+addr+" to fetch game "+game+"!" );

                                    // remove the game from cache so next time someone requests info a new attempt is made.
                                    gameCache.removeGame( game.getGameId( ) );
                                }
                            }
                        } );
            }

        /** Check for a game list event and initiates it. */
            protected void updateGamelist( )
            {
                long now = System.currentTimeMillis( );
                // check every minute
                final long interval = 60*1000;
                if( now - lastGameListRequest > interval )
                {
                    //logger.trace( "Initiating a new game list request." );
                    lastGameListRequest = now;

                    // initiate a new request.
                    for( int i=0; i<config.master.length; ++i )
                    {
                        final int index = i;
                        final SocketAddress addr = config.master[i];
                        connector.connect( addr ).addListener( 
                            new IoFutureListener< ConnectFuture >( )
                                {
                                    public void operationComplete( ConnectFuture future )
                                    {
                                        if( future.isConnected( ) == true )
                                            future.getSession( ).setAttachment( new FetchGameListSession( index, WTVCache.this ) );
                                        else
                                            logger.warn( "Failed to connect to master server "+addr+"!" );
                                    }
                                } );
                    }
                }
            }

        /** Update the cache */
            protected void updateCache( )
            {
                long now = System.currentTimeMillis( );

                final long interval = 60*1000;
                if( now - lastCachePrune > interval )
                {
                    lastCachePrune = now;
                    gameCache.pruneCache( );
                }

            }

    /** Main loop of the WTVCache. */
        protected void run( )
        {
            try {
                /** Startup */
                    logger.info( "Starting up WTVCache version "+getVersionString( )+"..." );

                    /** Create the game cache. */
                        gameCache = new GameCache( config.master.length, this );

                    /** Create the access log. */
                        accessLog = new AccessLog( config.accessLogDir );

                    /** Disable direct memory. */
                        IoBuffer.setUseDirectBuffer( false );
                        IoBuffer.setAllocator( new SimpleBufferAllocator() );

                    /** Create the web acceptor */
                        webAcceptor = new NioSocketAcceptor();
                        webAcceptor.setReuseAddress( true );
                        webAcceptor.setDefaultLocalAddress( new InetSocketAddress( config.httpPort ) );
                        webAcceptor.setHandler( new HttpIoHandler( this ) );
                        webAcceptor.getFilterChain( ).addLast( "httpCodec", new ProtocolCodecFilter( new HttpCodecFactory( ) ) );
                        webAcceptor.bind();

                    /** Create the relay acceptor */
                        acceptor = new NioSocketAcceptor();
                        acceptor.setReuseAddress( true );
                        acceptor.setDefaultLocalAddress( new InetSocketAddress( config.listenPort ) );
                        acceptor.setHandler( new RelayIOHandler( this ) );
                        acceptor.getSessionConfig().setIdleTime( IdleStatus.READER_IDLE, 60 );
                        acceptor.getFilterChain( ).addLast( "wtvCodec", new ProtocolCodecFilter( new WTVCodecFactory( ) ) );
                        acceptor.bind();

                    /** Create the uplink connector */
                        connector = new NioSocketConnector( );
                        connector.getFilterChain( ).addLast( "wtvCodec", new ProtocolCodecFilter( new WTVCodecFactory( ) ) );
                        connector.getSessionConfig().setIdleTime( IdleStatus.READER_IDLE, 120 );
                        connector.setHandler( new UplinkIOHandler( this, connector ) );

                /** Main loop */
                    logger.info( "Entering main loop..." );

                    while( doShutdown == false )
                    {
                        updateGamelist( );
                        updateCache( );
                        accessLog.checkLogRotate( );

                        try{
                            Thread.sleep( 1000 );
                        } catch( Exception e )
                        {   
                            break; 
                        }
                    }

                /** Shutdown */
                    logger.info( "Shutting down..." );

                    /** Shut down mina. */
                        acceptor.unbind();
                        webAcceptor.unbind( );

                        /** Close any remaining sessions. */
                        for( IoSession session : acceptor.getManagedSessions( ).values( ) )
                            session.close( true );

                        /** Close any remaining sessions. */
                        for( IoSession session : webAcceptor.getManagedSessions( ).values( ) )
                            session.close( true );

            } catch( Exception e )
            {
                logger.error( "Startup failed: "+e );
                doShutdown = true;
            }

        }
}

