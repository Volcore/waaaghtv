/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvcache.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // wtvcache
        import net.volcore.wtvcache.*;
    // java
        import java.util.concurrent.*;
        import java.util.*;
        import java.nio.*;
        import java.nio.charset.*;
    // mina
        import org.apache.mina.core.buffer.*;

/*******************************************************************************
         This is a bridge between the uplink part and the cache frontend. It
        contains the list of cached games. The cache has a limited size.
 *******************************************************************************/
public class GameCache
{
    static Logger   logger = LoggerFactory.getLogger( "GameCache" );

    public ConcurrentHashMap< Integer, Game >               cache = new ConcurrentHashMap< Integer, Game >( );
    public final int                                        MAX_NUM_GAMES = 150;

    protected GameListEntry[][]                             gameLists;
    protected byte[]                                        compiledGameList = new byte[0];
    protected byte[]                                        compiledLiveGames = new byte[0];
    protected int                                           numGames = 0;
    protected int                                           numLiveGames = 0;

    protected WTVCache                                      wtvCache;


    public final static long                                MILLISECONDS = 1;
    public final static long                                     SECONDS = 1000*MILLISECONDS;
    public final static long                                     MINUTES = 60*SECONDS;
    public final static long                                       HOURS = 60*MINUTES;
    public final static long                                        DAYS = 24*HOURS;
    public final static long                                       WEEKS = 7*DAYS;

    protected long                                          MAX_CACHE_AGE = 5*MINUTES;
    protected long                                          MAX_BROKEN_CACHE_AGE = 30*SECONDS;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    /** public access. */
        /** Getter for compiledGameList */
        	public byte[] getCompiledGameList( ) { return compiledGameList; }
        /** Setter for compiledGameList */
        	public void setCompiledGameList( byte[] compiledGameList ) { this.compiledGameList = compiledGameList; }
        /** Getter for compiledLiveGames */
        	public byte[] getCompiledLiveGames( ) { return compiledLiveGames; }
        /** Setter for compiledLiveGames */
        	public void setCompiledLiveGames( byte[] compiledLiveGames ) { this.compiledLiveGames = compiledLiveGames; }
        /** Getter for numGames */
        	public int getNumGames( ) { return numGames; }
        /** Setter for numGames */
        	public void setNumGames( int numGames ) { this.numGames = numGames; }
        /** Getter for numLiveGames */
        	public int getNumLiveGames( ) { return numLiveGames; }
        /** Setter for numLiveGames */
        	public void setNumLiveGames( int numLiveGames ) { this.numLiveGames = numLiveGames; }

    /** Create the game cache. */
        public GameCache( int numServers, WTVCache wtvCache )
        {
            gameLists = new GameListEntry[ numServers ][];
            this.wtvCache = wtvCache;
        }

    /** GameList related... */
        /** Update the game list of a server. */
            synchronized public void setGameList( int serverid, GameListEntry[] list )
            {
                gameLists[serverid] = list;
            }

        /** Compile the game list */
            synchronized public void compileGameList( )
            {
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.clear( );
                buf.order( ByteOrder.LITTLE_ENDIAN ); 
                buf.setAutoExpand( true );

                IoBuffer buf2 = IoBuffer.allocate( 1 );
                buf2.clear( );
                buf2.order( ByteOrder.LITTLE_ENDIAN ); 
                buf2.setAutoExpand( true );

                int numgames = 0;
                int numlivegames = 0;

                for( GameListEntry[] list : gameLists )
                {
                    if( list == null )
                        continue;

                    for( GameListEntry entry : list )
                    {
                        entry.write( buf );
                        numgames++;
                        if( entry.status < Game.STATUS_COMPLETED )
                        {
                            entry.write( buf2 );
                            numlivegames++;
                        }
                    }
                }

                int length = buf.position( );
                byte[] data = new byte[ length ];
                buf.flip( );
                buf.get( data );
                this.compiledGameList = data;
                this.numGames = numgames;

                int length2 = buf2.position( );
                byte[] data2 = new byte[ length2 ];
                buf2.flip( );
                buf2.get( data2 );
                this.compiledLiveGames = data2;
                this.numLiveGames = numlivegames;

                //logger.trace( "Compiled game list with "+numgames+" games, "+numlivegames+" of them are live." );
            }

    /** Cache administration. */
        public void pruneCache( )
        {
            Enumeration< Game > git = cache.elements( );

            long now = System.currentTimeMillis( );

            while( git.hasMoreElements( ) )
            {
                Game game = git.nextElement( );

                if( game.getCacheState( ) < Game.CACHESTATE_NONE )
                {
                    logger.info( "Caching out "+game+" because its in an bogus state..." );
                    wtvCache.getAccessLog( ).logCacheOut( game.getGameId( ) );
                    removeGame( game.getGameId( ) );
                    continue;
                }

                long delta = now - game.getLastAccess( );

                if( delta > MAX_CACHE_AGE 
                || ( game.getBroken( ) == true
                    && delta > MAX_BROKEN_CACHE_AGE ) )
                {
                    logger.info( "Caching out "+game+" because its unused"+(game.getBroken()==true?" and broken.":".") );
                    wtvCache.getAccessLog( ).logCacheOut( game.getGameId( ) );
                    removeGame( game.getGameId( ) );
                    continue;
                }
            }
        }

        /** Returns a list of all cache elements. */
            public Enumeration< Game > getCacheElements( ) { return cache.elements( ); }

    /** Game related. */
        /** Returns the game for a certain id or null if the game is not in the cache. */
            public Game getGameById( int gameid )
            {
                Game game = cache.get( new Integer( gameid ) );

                if( game == null )
                {
                    game = new Game( gameid );
                    cache.put( new Integer( gameid ), game );
                }

                return game;
            }

            public Game getGameByIdOnly( int gameid )
            {
                Game game = cache.get( new Integer( gameid ) );
                return game;
            }

        /** Remove a game from the cache. */
            public void removeGame( int gameid )
            {
                Game game = cache.remove( new Integer( gameid ) );

                if( game != null )
                    game.setCacheState( Game.CACHESTATE_CACHEOUT );
            }
}

