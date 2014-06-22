/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvcache.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // mina
        import org.apache.mina.core.buffer.*;
    // java
        import java.util.concurrent.locks.*;
        import java.nio.*;

public class Game
{
    static Logger   logger = LoggerFactory.getLogger( "Game" );

    /** Caching related data. */
        /** The CACHESTATE describes the current status of the game in the gamecache. 
            It can be used to determine which part of the information is already available for streaming. */
            /** The game does not exist on the primary master server or in the cache. */
                public final static int CACHESTATE_DOESNOTEXIST = -2;
            /** The game is being phased out of the cache (but may still be in the java garbage cache.) */
                public final static int CACHESTATE_CACHEOUT     = -1;
            /** Just the id is in cache, no further information. It may not even exist. */
                public final static int CACHESTATE_NONE         = 0;
            /** This state was removed, as it is no longer a state, but can be queried using getGameInfo( )==null */
                //public final static int CACHESTATE_GAMEINFO     = 1;
            /** The game details have been retrieved as well. */
                public final static int CACHESTATE_GAMEDETAILS  = 2;
            /** The game has started and data is being retrieved. */
                public final static int CACHESTATE_GAMESTART    = 3;
            /** The game is completely in the cache. */
                public final static int CACHESTATE_FINISHED     = 4;

            protected int           cacheState   = CACHESTATE_NONE;
            protected boolean       fetchingInfo = false;
            protected boolean       fetchingGame = false;
            protected boolean       broken       = false;
            protected int           serverId     = 0;
        /** last time the game was accesses in the cache. */
            public long             lastAccess   = System.currentTimeMillis( );

        /** Getter for cacheState */
        	public int getCacheState( ) { return cacheState; }
        /** Setter for cacheState */
        	public void setCacheState( int cacheState ) { this.cacheState = cacheState; }
        /** Getter for fetchingInfo */
        	public boolean getFetchingInfo( ) { return fetchingInfo; }
        /** Setter for fetchingInfo */
        	public void setFetchingInfo( boolean fetchingInfo ) { this.fetchingInfo = fetchingInfo; }
        /** Getter for fetchingGame */
        	public boolean getFetchingGame( ) { return fetchingGame; }
        /** Setter for fetchingGame */
        	public void setFetchingGame( boolean fetchingGame ) { this.fetchingGame = fetchingGame; }
        /** Getter for lastAccess */
        	public long getLastAccess( ) { return lastAccess; }
        /** Setter for lastAccess */
        	public void setLastAccess( long lastAccess ) { this.lastAccess = lastAccess; }
        /** Getter for broken */
        	public boolean getBroken( ) { return broken; }
        /** Setter for broken */
        	public void setBroken( boolean broken ) { this.broken = broken; }

    /** The classical game status. */
        public final static int STATUS_NEW = 1;
        public final static int STATUS_DELAY = 2;
        public final static int STATUS_STARTED = 3;
        public final static int STATUS_COMPLETED = 4;
        public final static int STATUS_BROKEN = 5;

    /** Game information. */
        protected int           gameId;

        // game info packet
            protected byte[]        gameInfo;

        // game details packet
            protected byte[]        gameDetails; 

        // start infos
            protected int           delay;
            protected int           lastSeed;
            protected int           date;

        // ingame data
            protected byte[]        gameData;
            protected int           gameDataSize = 0;
            protected int           numPackets = 0; // computed when the game is finished.

    // Used to get an exclusive write lock by the writer.
        ReentrantReadWriteLock          readWriteLock = new ReentrantReadWriteLock( );

    /** Constructor */
        public Game( int gameId )
        {
            this.gameId = gameId;
        }

    /** Syncronously (writer lock) create the buffer for the game data, with an initial size of 64KiB, growing automatically. */
        public void createGameDataBuffer( )
        {
            readWriteLock.writeLock( ).lock( );
            try {
                gameData = new byte[ 64*1024 ];
                gameDataSize = 0;
            } finally {
                readWriteLock.writeLock( ).unlock( );
            }
        }

    /** Syncronously (writer lock) add data to the game and return the new data size. */
        public int addGameData( int position, byte[] data )
        {
            readWriteLock.writeLock( ).lock( );
            try {
                int req = position+data.length;
                int delta = req-gameData.length;
                if( delta > 0 )
                {
                    // grow
                    byte[] newdata;
                    if( delta > 64*1024 )
                        newdata = new byte[ req ];
                    else
                        newdata = new byte[ gameData.length+64*1024 ];
                    System.arraycopy( gameData, 0, newdata, 0, position );
                    gameData = newdata;
                }

                System.arraycopy( data, 0, gameData, position,  data.length );

                gameDataSize = position+data.length;

                return gameDataSize;
            } finally {
                readWriteLock.writeLock( ).unlock( );
            }
        }

    /** Syncronously (reader lock) compute the number of packets. */
        public int computeNumberOfPackets( )
        {
            readWriteLock.readLock( ).lock( );

            int numPackets = 0;
            try {
                IoBuffer buf = IoBuffer.wrap( gameData, 0, gameDataSize );
                buf.order( ByteOrder.LITTLE_ENDIAN );

                while( buf.remaining( ) >= 8 )
                {
                    int time = buf.getInt( );
                    int size = buf.getInt( );

                    if( size < 0 )
                    {
                        logger.error( "Negative packet size! Critical!" );
                        logger.error( time+" "+size+" "+buf.position( ) );
                        return -1;
                    }

                    if( buf.remaining( ) < size )
                        break;

                    buf.skip( size );

                    numPackets++;
                }

                return numPackets;
            } catch( Exception e )
            {
                logger.error( "Failed to get compute nubmer of packets: "+e );
                e.printStackTrace( );
                return -1;
            }
            finally
            {
                readWriteLock.readLock( ).unlock( );
            }
        }

    /** Syncronously (writer lock) finish the game and compute the number of packets, verify. */
        public boolean finishGameData( int numpackets )
        {
            readWriteLock.writeLock( ).lock( );
            try {
                // maybe compact the buffer... but its only about 64k, thats not critical... so not.
                int localNumPackets = computeNumberOfPackets( );
                this.numPackets = localNumPackets;
                if( localNumPackets != numpackets )
                {
                    logger.error( "The number of packets do not match! Download of game failed!" );
                    return false;
                }

                return true;
            } finally {
                readWriteLock.writeLock( ).unlock( );
            }
        }

    /** Syncronously (reader lock) return a piece of data from the offset. Maxsize should be around 30KiB */
        public byte[] getDataBlock( int offset, int maxsize )
        {
            readWriteLock.readLock( ).lock( );

            lastAccess = System.currentTimeMillis( );

            try{
                if( offset > gameDataSize )
                {
                    logger.warn( "Trying to read behind game "+gameId+"'s data size "+offset+"/"+gameDataSize+"!" );
                    return null;
                }

                IoBuffer buf = IoBuffer.wrap( gameData, offset, gameDataSize-offset );
                buf.order( ByteOrder.LITTLE_ENDIAN );

                buf.mark( );
                int length = 0;

                while( length < maxsize
                && buf.remaining( ) >= 8 )
                {
                    int time = buf.getInt( );
                    int size = buf.getInt( );
                    if( size < 0 )
                    {
                        logger.error( "This must not happen, malformed game packet of size < 0!" );
                        // pull the critical hard brake!
                        System.exit( 0 );
                        //throw new Error( "This must not happen, malformed game packet of size < 0!" );
                        return null;
                    }

                    if( buf.remaining( ) < size )
                        break;

                    length += size+8;
                    buf.skip( size );
                }

                if( length == 0 ) 
                    return null;

                buf.reset( );

                byte[] data = new byte[ length ];
                buf.get( data );
                return data;
            } finally
            {
                readWriteLock.readLock( ).unlock( );
            }
        }

    /** Returns the game info and updates the access. */
        public byte[] getGameInfo( )
        {
            lastAccess = System.currentTimeMillis( );
            return gameInfo;
        }

    /** Update the game info */
        public void setGameInfo( byte[] info )
        {
            gameInfo = info;
        }

    /** Update the last time the game got access. */
        public void updateLastAccess( )
        {
            lastAccess = System.currentTimeMillis( );
        }

    /** Returns the game details and updates the access. */
        public byte[] getGameDetails( )
        {
            lastAccess = System.currentTimeMillis( );
            return gameDetails;
        }

    /** Update the game details */
        public void setGameDetails( byte[] details )
        {
            gameDetails = details;
        }

            /** Getter for serverId */
            	public int getServerId( ) { return serverId; }
            /** Setter for serverId */
            	public void setServerId( int serverId ) { this.serverId = serverId; }
        /** Getter for gameId */
        	public int getGameId( ) { return gameId; }
        /** Setter for gameId */
        	public void setGameId( int gameId ) { this.gameId = gameId; }

            /** Getter for gameDataSize */
            	public int getGameDataSize( ) { return gameDataSize; }
            /** Setter for gameDataSize */
            	public void setGameDataSize( int gameDataSize ) { this.gameDataSize = gameDataSize; }
            /** Getter for numPackets */
            	public int getNumPackets( ) { return numPackets; }
            /** Setter for numPackets */
            	public void setNumPackets( int numPackets ) { this.numPackets = numPackets; }
            /** Getter for delay */
            	public int getDelay( ) { return delay; }
            /** Setter for delay */
            	public void setDelay( int delay ) { this.delay = delay; }
            /** Getter for lastSeed */
            	public int getLastSeed( ) { return lastSeed; }
            /** Setter for lastSeed */
            	public void setLastSeed( int lastSeed ) { this.lastSeed = lastSeed; }
            /** Getter for date */
            	public int getDate( ) { return date; }
            /** Setter for date */
            	public void setDate( int date ) { this.date = date; }
    /** Debug function. */
        public String toString( )
        {
            return "(Game: "+gameId+" "+cacheState+")";
        }
}
