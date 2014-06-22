/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmaster;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;

public class ShutdownThread extends Thread
{
    static Logger   logger = LoggerFactory.getLogger( "ShutdownThread" );
    Stoppable trigger;

    ShutdownThread( Stoppable trigger )
    {
        this.trigger = trigger;
    }

    public void run( )
    {
        logger.info( "Shutdown command received, terminating." );
        trigger.stop( );
        //doShutdown = true;
        final int wait = 3000;
        try{
            // wait at most 3 seconds, then forcefully terminate.
            Thread.sleep( wait );
            logger.error( "Timed out waiting "+wait+"ms for main thread to shut down, forcefully terminating." );
        }catch( Exception e ){ }
    }
}
