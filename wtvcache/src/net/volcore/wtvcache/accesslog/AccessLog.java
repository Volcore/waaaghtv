/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.accesslog;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // Java
        import java.text.*;
        import java.util.*;
        import java.io.*;
        import java.net.*;

public class AccessLog
{
    static Logger   logger = LoggerFactory.getLogger( "AccessLog" );

    protected String                pathname;
    protected PrintWriter           writer;
    protected GregorianCalendar     writerDate;

    protected final static DateFormat fileformat = new SimpleDateFormat("yyyy-M-d");

    // Legend: 
        // %e   epoch
        // %s   string
        // %a   address
        // %l   long
    // Map:
        // %e c %a                      connect: address
        // %e d %a %l %l %l             disconnect: address bytesread byteswrite sessionlength(ns)
        // %e g %a                      gamelist: address
        // %e l %a %s                   login: address clientapp
        // %e i %a %g                   info: address gameid
        // %e s %a %g                   subscribe: address gameid
        // %e f %a %g                   finished: address gameid
        // %e I %g                      cache in: gameid
        // %e O %g                      cache out: gameid
            


    /** The logging functions. */
        /** Log a connect event. */
            public void logConnect( String address )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " c " );
                writer.print( address );
                writer.println( );
                writer.flush( );
            }

        /** Log a disconnect. */
            public void logDisc( String address, long read, long write, long time )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " d " );
                writer.print( address );
                writer.print( " " );
                writer.print( read );
                writer.print( " " );
                writer.print( write );
                writer.print( " " );
                writer.print( time );
                writer.println( );
                writer.flush( );
            }

        /** Log a gamelist request. */
            public void logGamelist( String address, boolean request )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " g " );
                writer.print( address );
                writer.print( " " );
                writer.print( request );
                writer.println( );
                writer.flush( );
            }

        /** Log a login. */
            public void logLogin( String address, String client  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " l " );
                writer.print( address );
                writer.print( " " );
                writer.print( client );
                writer.println( );
                writer.flush( );
            }

        /** Log a game info request. */
            public void logInfo( String address, int gameid  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " i " );
                writer.print( address );
                writer.print( " " );
                writer.print( gameid );
                writer.println( );
                writer.flush( );
            }

        /** Log a game subscribe request. */
            public void logSubscribe( String address, int gameid  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " s " );
                writer.print( address );
                writer.print( " " );
                writer.print( gameid );
                writer.println( );
                writer.flush( );
            }

        /** Log a game finished request. */
            public void logFinished( String address, int gameid  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " f " );
                writer.print( address );
                writer.print( " " );
                writer.print( gameid );
                writer.println( );
                writer.flush( );
            }

        /** Log a cache in. */
            public void logCacheIn( int gameid  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " I " );
                writer.print( gameid );
                writer.println( );
                writer.flush( );
            }

        /** Log a cache out. */
            public void logCacheOut( int gameid  )
            {
                writer.print( System.currentTimeMillis( ) );
                writer.print( " O " );
                writer.print( gameid );
                writer.println( );
                writer.flush( );
            }

    /** Aux. */
        public AccessLog( String pathname )
        {
            this.pathname = pathname;
            new File( pathname ).mkdirs( );

            try {
                writerDate = new GregorianCalendar( );
                writer = new PrintWriter( new FileWriter( pathname+"/"+fileformat.format( writerDate.getTime( ) ), true ) );
                writer.write( "# Started log on "+(writerDate.getTime( ))+"\n" );
                writer.flush( );
            }
            catch( Exception e )
            {
                logger.error( "Failed to create access log: "+e );
                e.printStackTrace( );
            }
        }

        public void checkLogRotate( )
        {
            GregorianCalendar cal = new GregorianCalendar( );
            
            if( cal.get(Calendar.YEAR) == writerDate.get(Calendar.YEAR)
            &&  cal.get(Calendar.MONTH) == writerDate.get(Calendar.MONTH)
            &&  cal.get(Calendar.DAY_OF_MONTH) == writerDate.get(Calendar.DAY_OF_MONTH)
                )
                return;

            writerDate = cal;
            try {
                PrintWriter oldwriter = writer;
                writer = new PrintWriter( new FileWriter( pathname+"/"+fileformat.format( writerDate.getTime( ) ), true ) );
                writer.write( "# Started log on "+(writerDate.getTime( ))+"\n" );
                writer.flush( );

                oldwriter.write( "# Rotated log on "+(cal.getTime( ))+"\n" );
                oldwriter.flush( );
                oldwriter.close( );
            }
            catch( Exception e )
            {
                logger.error( "Failed to rotate access log: "+e );
                e.printStackTrace( );
            }
        }
}


