/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.util.concurrent.atomic.*;


/*******************************************************************************
         Statistics for a game.
 *******************************************************************************/
public class GameStats
{
    /** protected data. */
        protected int           gameId;        

        protected AtomicLong    timesSubscribed     = new AtomicLong( );
        protected AtomicLong    timesFinished       = new AtomicLong( );

    /** public members. */
        /** Getter for gameId */
        	public int getGameId( ) { return gameId; }
        /** Setter for gameId */
        	public void setGameId( int gameId ) { this.gameId = gameId; }
        /** Getter for timesSubscribed */
        	public AtomicLong getTimesSubscribed( ) { return timesSubscribed; }
        /** Setter for timesSubscribed */
        	public void setTimesSubscribed( AtomicLong timesSubscribed ) { this.timesSubscribed = timesSubscribed; }
        /** Getter for timesFinished */
        	public AtomicLong getTimesFinished( ) { return timesFinished; }
        /** Setter for timesFinished */
        	public void setTimesFinished( AtomicLong timesFinished ) { this.timesFinished = timesFinished; }

        /** Update functions. */
            public void increaseTimesFinished( )
            {
                timesFinished.getAndIncrement( );
            }

            public void increaseTimesSubscribed( )
            {
                timesSubscribed.getAndIncrement( );
            }
}


