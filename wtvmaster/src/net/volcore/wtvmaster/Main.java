/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmaster;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // Apache commons CLI
        import org.apache.commons.cli.*;
    // java
        import java.net.*;

public class Main
{
    static Logger           logger = LoggerFactory.getLogger( "Main" );
    static Options          options;
    static HelpFormatter    formatter = new HelpFormatter();

    /** should be called once in the beginning, sets up all the required logging. */
        protected static void initializeLogging( )
        {
            // Initialize logging
                String name = "wtvmaster-log4j.properties";
                URL log4jfile = ClassLoader.getSystemClassLoader( ).getResource( name );
                if( log4jfile == null )
                    throw new Error( "Could not find "+name+"!" );
                org.apache.log4j.PropertyConfigurator.configure( log4jfile );
        }
    /** Use apache CLI to parse the command line */
        protected static Config parseOptions( String[] args )
        {
            // Build options
                options = new Options( );

                options.addOption( "h", "help", false, "print this message" );
                options.addOption( "p", "recorderport", true, "port for the recorder to connect" );
                options.addOption( "P", "relayport", true, "port for the caches to connect" );
                options.addOption( "h", "httpport", true, "port for the web interface" );

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
                    formatter.printHelp( "wtvmaster", options );
                    return null;
                } 

                Config config = new Config( );

                if( line.hasOption( 'p' ) )
                    config.recorderPort = Integer.parseInt( line.getOptionValue( 'p' ) );
 
                if( line.hasOption( 'P' ) )
                    config.relayPort = Integer.parseInt( line.getOptionValue( 'P' ) );

                if( line.hasOption( 'h' ) )
                    config.httpPort = Integer.parseInt( line.getOptionValue( 'h' ) );

            return config;
        }

    /** Main entry point */
        public static void main( String[] args )
        {
            initializeLogging( );

            Config config = parseOptions( args );

            // start the wtvMaster.
            WTVMaster wtvMaster;
            ShutdownThread shutdownThread;

            try {
                wtvMaster = new WTVMaster( config );
                shutdownThread = new ShutdownThread( wtvMaster );
                Runtime.getRuntime().addShutdownHook( shutdownThread );
                wtvMaster.run( );
            } catch( ConfigException e )
            {
                formatter.printHelp( "wtvmaster", options );
                logger.error( e.getMessage( ) );
                return;
            }

            HibernateUtil.getSessionFactory( ).close( );

            // wake the shutdown thread
            shutdownThread.interrupt( );
            logger.info( "WTVMaster terminated." );

            // remove it in case its still there.
            try {
                Runtime.getRuntime( ).removeShutdownHook( shutdownThread );
                System.exit( 0 );
            } catch( Exception e ) { }
        }
}
