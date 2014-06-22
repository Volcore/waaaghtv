/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.io.*;
        import java.util.*;

public class LogFile extends Thread
{
    static Logger   logger = LoggerFactory.getLogger( "LogFile" );
    FileWriter                  writer;

    public LogFile( String filename, int maxsize, int numfiles )
    {
        this.logfileName = filename;
        this.rotationFileSize = maxsize; 
        this.rotationNumFiles = numfiles;
        this.start( );
    }

    LinkedList< String >     scheduled = new LinkedList< String > ( );
    LinkedList< String >     buffer    = new LinkedList< String > ( );


    /** Synchronously add a string to the list */
        public void write( String str )
        {
            synchronized( scheduled )
            {
                scheduled.add( str );
            }
        }


    /** Perform a logrotation - if its time */
        public void logrotate( int maxsize, int numfiles )
        {
            File file = new File( logfileName );

            long length = file.length( );

            if( length < maxsize )
                return;

            try {
                writer.close( );
            } catch( IOException e )
            {
                logger.error( "Failed to close accesslog during rotate: "+e );
            }

            File targetFile = new File( logfileName+"."+(numfiles-1) );

            for( int i=numfiles-1; i>1; --i )
            {
                File sourceFile = new File( logfileName+"."+(i-1) );

                sourceFile.renameTo( targetFile );

                targetFile = sourceFile;
            }

            file.renameTo( new File( logfileName+".1" ) );

            try {
                writer = new FileWriter( logfileName, true );
            } catch( IOException e )
            {
                logger.error( "Failed to open accesslog after rotate: "+e );
            }
        }

    public String logfileName;
    public int rotationFileSize = 1*1024;
    public int rotationNumFiles   = 10;

    public void run( )
    {
        /** Open the log file */
            try {
                writer = new FileWriter( logfileName, true );
            } catch( IOException e )
            {
                logger.error( "Failed to open accesslog: "+e );
            }

        /** Mainloop */
            while( true )
            {
                /** Swap the double-buffers */
                    synchronized( scheduled )
                    {
                        LinkedList< String > tmp = buffer;
                        buffer = scheduled;
                        scheduled = tmp;
                    }

                /** Process all scheduled log messages */
                    Iterator< String > sit = buffer.iterator( );

                    while( sit.hasNext( ) )
                    {
                        String str = sit.next( );
                        sit.remove( );

                        try {
                            writer.write( str );
                        } catch( IOException e )
                        {
                            logger.error( "Failed to write to accesslog: "+e );
                        }
                    }

                /** Flush the writer so that it does not buffer. Some stuff may not be written otherwise. */
                    try {
                        writer.flush( );
                    } catch( IOException e )
                    {
                        logger.error( "Failed to flush the data to file: "+e );
                    }

                /** check age and size of the file, possibly doing a logrotation */
                    logrotate( rotationFileSize, rotationNumFiles );
                

                /** Sleep 5 seconds until next log flush. */
                    try { 
                        Thread.sleep( 5000 );
                    } catch( InterruptedException e )
                    {
                        logger.info( "got interrupted!" );
                        break;
                    }
            }

        /** Don't forget to close the writer. */
            try { 
                writer.close( );
            } catch( IOException e )
            {
                logger.error( "Failed to close accesslog: "+e );
            }
    }
}
