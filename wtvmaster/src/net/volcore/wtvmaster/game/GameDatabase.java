/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // hibernate
        import org.hibernate.*;
    // java
        import java.util.*;
        import java.security.*;
        import java.util.concurrent.*;
        import java.nio.charset.*;
        import java.nio.*;
    // mina
        import org.apache.mina.core.buffer.*;
    // wtvmaster
        import net.volcore.wtvmaster.*;
        import net.volcore.wtvmaster.user.*;

/*******************************************************************************
         Manager for all games. Holds a list of all games, a list of the 
        current games (the classical game list), and a list of the games that
        are currently in cache.
 *******************************************************************************/
public class GameDatabase
{
    public static final int         SECONDS = 1;
    public static final int         MINUTES = 60*SECONDS;
    public static final int         HOURS   = 60*MINUTES;
    public static final int         DAYS    = 24*HOURS;

    public static final int         GAMELIST_PRUNE_THRESHOLD = 30;

    static Logger                   logger = LoggerFactory.getLogger( "GameDatabase" );
    public final static int         INVALID_GAMEID = -1;
    protected final CharsetDecoder  charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( );
    protected final CharsetEncoder  charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( );

    /** Data */
        /** The game cache, storing the currently in cache games. If a game is not in here, it may be in the database. */
            protected ConcurrentHashMap< Integer, Game >    gameCache = new ConcurrentHashMap< Integer, Game >( );
        /** The current game list is the list of recent games used to build the static gamelist array. */
            protected LinkedList< Integer >                 currentGameList = new LinkedList< Integer >( );

        /** Game list cache. */
            /** Number of games in the static game list. */
                public int       gameListNumGames = 0;
            /** The static game list, sent to the clients. */
                public byte[]    gameList = new byte[ 0 ];


    /** Current game list related infos. */
        /** Fetch initial list of games from the database, before the server starts. */
            public void fetchInitialList( )
            {
                // populate currentGameList
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                // select all games for which the status is not completed or broken
                    try {
                        tx = session.beginTransaction( );
                        List games = session.createQuery( "from Game where status<4" ).list( );
                        tx.commit( );

                        Iterator git = games.iterator( );

                        while( git.hasNext( ) )
                        {
                            Game game = (Game)git.next( );
                            logger.trace( "Found game "+game+" in database with broken records. Fixing." );

                            if( game.getStatus( )<Game.STATUS_COMPLETED )
                            {
                                game.setStatus( Game.STATUS_BROKEN );
                                game.openFile( );
                                //game.truncate( 0, 0 );
                                game.setNumPackets( game.computeNumberOfPackets( ) );
                                game.closeFile( );
                                storeGame( game );
                            }
                        }
                    } catch( RuntimeException e )
                    {
                        logger.error( "Failed to fetch any unfinished games: "+e );
                    }

                // fetch initial game list
                    try {
                        tx = session.beginTransaction( );
                        List games = session.createQuery( "from Game where certified=true order by id desc" ).setMaxResults( 50 ).list( );
                        tx.commit( );

                        Iterator git = games.iterator( );

                        while( git.hasNext( ) )
                        {
                            Game game = (Game)git.next( );
                            //logger.trace( "Found game "+game );

                            if( game.getStatus( )!=Game.STATUS_COMPLETED )
                                game.setStatus( Game.STATUS_BROKEN );

                            if( game != null )
                                gameCache.put( new Integer( game.getId( ) ), game );
                            game.openFile( );

                            addGameToList( game.getId( ) );
                        }

                        buildGameList( );
                    } catch( RuntimeException e )
                    {
                        logger.error( "Failed to fetch initial game list: "+e );
                        return;
                    } finally
                    {
                        session.close( );
                    }
            }

        /** Add a new game to the recent game list, kicking out old games if the list becomes too long. */
            public void addGameToList( int id )
            {
                synchronized( currentGameList ) 
                {
                    // check for duplicates.
                        Iterator< Integer > git = currentGameList.iterator( );
                        while( git.hasNext( ) )
                            if( id == git.next( ) )
                                return;

                    // add the game to the list
                        currentGameList.add( new Integer( id ) );
                }
            }

        /** Prune the current game list, throwing out old broken and completed games. */
            public void pruneGameList( )
            {
                //logger.trace( "Pruning game list." );
                synchronized( currentGameList )
                {
                    Iterator< Integer > git = currentGameList.iterator( );
                    while( git.hasNext( ) )
                    {
                        Integer id = git.next( );

                        // fetch the game
                            Game game = fetchGame( id );

                            if( game == null )
                            {
                                git.remove( );
                                continue;
                            }

                        // check the game
                            if( game.getStatus( ) == Game.STATUS_BROKEN )
                            {
                                // throw broken games out after a few minutes
                                    if( game.getAge( ) > 60*MINUTES+0*SECONDS )
                                    {
                                        logger.trace( "Removing game "+id+" from the current game list due to broken age." );
                                        git.remove( );
                                        continue;
                                    }
                            } else
                            if( game.getStatus( ) == Game.STATUS_COMPLETED
                            && currentGameList.size( ) > GAMELIST_PRUNE_THRESHOLD )
                            {
                                // throw completed games out after a three hours
                                    if( game.getAge( ) > 3*HOURS+0*MINUTES+0*SECONDS )
                                    {
                                        logger.trace( "Removing game "+id+" from the current game list due to completed age." );
                                        git.remove( );
                                        continue;
                                    }
                            }
                    }
                }
            }

        /** Build a new game list. */
            public void buildGameList( )
            {
                pruneGameList( );
                //logger.trace( "Building game list." );
                
                synchronized( currentGameList )
                {
                    IoBuffer buf = IoBuffer.allocate( 1 );
                    buf.clear( );
                    buf.order( ByteOrder.LITTLE_ENDIAN ); 
                    buf.setAutoExpand( true );

                    Iterator< Integer > git = currentGameList.iterator( );
                    int numGames = 0;
                    while( git.hasNext( ) )
                    {
                        Game game = fetchGame( git.next( ) );

                        try{
                            buf.putInt( game.getId( ) );
                            buf.put( game.getStatus( ) );
                            buf.putInt( game.getDate( ) );
                            buf.putString( game.getName( ), charsetEncoder );
                            buf.put( (byte) 0 );
                            numGames++;
                        } catch( Exception e )
                        {
                            logger.error( "Failed to write game name: "+e );
                            e.printStackTrace( );
                        }
                    }

                    int length = buf.position( );
                    byte[] data = new byte[ length ];
                    buf.flip( );
                    buf.get( data );
                    this.gameList = data;
                    this.gameListNumGames = numGames;
                }
            }

        /** Returns the current game lust. */
            public LinkedList< Integer > getCurrentGameList( )
            {
                return currentGameList;
            }

        /** Remove a game from the current game list. */
            public boolean removeFromCurrentGameList( int id )
            {
                boolean found = false;

                synchronized( currentGameList )
                {
                    Iterator< Integer > lit = currentGameList.iterator( );
                    
                    while( lit.hasNext( ) )
                    {
                        Integer i = lit.next( );

                        if( id == i )
                        {
                            lit.remove( );
                            found = true;
                        }
                    }
                }

                if( found )
                    buildGameList( );

                return found;
            }

    /** Cache administration. */
        /** Prune the cache of any unused items */
            public void pruneCache( )
            {
                logger.info( "Pruning cache..." );
                Enumeration< Game > git = gameCache.elements( );
                long now = System.currentTimeMillis( );
                
                while( git.hasMoreElements( ) )
                {
                    Game g = git.nextElement( );

                    if( g.getUploaderId( ) == UserDatabase.INVALID_USERID
                    && now - g.getLastAccessTime( ) > (5*MINUTES + 0*SECONDS)*1000 )
                    {
                        logger.trace( "Removing game "+g+" from cache, because it is not used at the moment." );
                        gameCache.remove( new Integer( g.getId( ) ) );
                        continue;
                    }
                }
            }

        /** Get current cache elements. */
            public Enumeration< Game > getCacheElements( )
            { 
                return gameCache.elements( );
            }

        /** Drop cache item. */
            public boolean dropCacheItem( int i )
            {
                return gameCache.remove( new Integer( i ) )!=null;
            }

    /** Operations. */

    /** Database game I/O */
        /** Atomically (at least in the sense of write access) check if a game is 
            a duplicate and return that game, otherwise create a new game. */
            synchronized public Game createGame( byte[] gameInfo, String name, String comment, int date, String streamer )
            {
                Session session = HibernateUtil.getSessionFactory( ).openSession( );

                try {
                    Transaction tx = null;
                    Game game = null;

                    String checksum = Game.computeGameInfoChecksum( gameInfo );
                    logger.trace( "Game checksum: "+checksum );


                    // check if the game is a duplicate
                        try {
                            tx = session.beginTransaction( );
                            List games = session.createQuery( "from Game where checksum=? and streamer=?" ).setString( 0, checksum ).setString( 1, streamer).list( );
                            tx.commit( );

                            Iterator git = games.iterator( );

                            while( git.hasNext( ) )
                            {
                                Game g = (Game) git.next( );
                                     g = fetchGame( g.getId( ) );

                                if( g.isSameGame( gameInfo ) )
                                {
                                    logger.info( "Game "+g.getId( )+" is duplicate, resuming..." );
                                    return g;
                                }
                            }
                        } catch( RuntimeException e )
                        {
                            logger.error( "Failed to check for duplicate games: "+e );
                            return null;
                        }

                    // no duplicate found, so create a new game.
                        try 
                        {
                            tx = session.beginTransaction( );

                            game = new Game( );

                            game.setName( name );
                            game.setComment( comment );
                            game.setStreamer( streamer );
                            //game.setDate( (int)(System.currentTimeMillis( )/1000) );
                            game.setDate( date );
                            game.setChecksum( checksum );

                            session.save( game );

                            session.getTransaction( ).commit( );
                        } catch( RuntimeException e )
                        {
                            if( tx != null ) 
                                tx.rollback( );
                            logger.error( "Failed to create new game: "+ e );
                            e.printStackTrace( );
                            return null;
                        }

                        try 
                        {
                            // now with the id secured, compute the path
                            tx = session.beginTransaction( );
                            game.setPath( Repository.computePath( game.getId( ) ) );
                            game.openFile( );
                            boolean success = game.createOnDisk( gameInfo );
                            game.setStatus( Game.STATUS_NEW );
                            gameCache.put( new Integer( game.getId( ) ), game );
                            session.save( game );
                            session.getTransaction( ).commit( );

                            // create the game on disk and write the game infos

                            if( success == false )
                            {
                                logger.error( "Failed to create game on disk!" );
                                return null;
                            }

                            return game;
                        } catch( RuntimeException e )
                        {
                            if( tx != null ) 
                                tx.rollback( );
                            logger.error( "Failed to update new game: "+ e );
                            e.printStackTrace( );
                            return null;
                        }
                } finally
                {
                    session.close( );
                }
            }

        /** Fetches the game from the database. Needs to be synchronized to avoid 2 threads fetching the same thing from the database. */
            synchronized public Game fetchGame( int gameid )
            {
                Game game = gameCache.get( new Integer( gameid ) );

                if( game != null )
                {
                    game.setLastAccessTime( );
                    return game;
                }

                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                try {
                    tx = session.beginTransaction( );
                    game = (Game) session.createQuery( "from Game where id=?" ).setInteger( 0, gameid ).uniqueResult( );
                    tx.commit( );

                    if( game == null )
                        return null;

                    // the game is not compelted, but not in cache -> has been broken before!
                        if( game.getStatus( )!=Game.STATUS_COMPLETED )
                        {
                            game.setStatus( Game.STATUS_BROKEN );
                            storeGame( game );
                        }

                    gameCache.put( new Integer( gameid ), game );
                    game.openFile( );
                    game.setLastAccessTime( );

                    return game;
                } catch( RuntimeException e )
                {
                    logger.error( "Failed to fetch game: "+e );
                    return null;
                } finally
                {
                    session.close( );
                }
            }

        /** Write a game back to the database. */
            public void storeGame( Game game )
            {
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                try {
                    tx = session.beginTransaction( );
                    session.update( game );
                    tx.commit( );
                } catch( RuntimeException e )
                {
                    tx.rollback( );
                    logger.error( "Failed to store game: "+e );
                    return;
                } finally
                {
                    session.close( );
                }
            }

        /** Get the total number of games */
            public long getNumGames( )
            {
                // populate currentGameList
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                // select all games for which the status is not completed or broken
                try {
                    tx = session.beginTransaction( );
                    long num = (Long) session.createQuery( "select count(*) from Game" ).uniqueResult( );
                    tx.commit( );

                    return num;
                } catch( RuntimeException e )
                {
                    logger.error( "Failed to fetch number of games: "+e );
                    return -1;
                }
            }

        /** Get the total number of games */
            public long getNumGamesSince( int date )
            {
                // populate currentGameList
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                // select all games for which the status is not completed or broken
                try {
                    tx = session.beginTransaction( );
                    long num = (Long) session.createQuery( "select count(*) from Game where date>=?" ).setInteger( 0, date ).uniqueResult( );
                    tx.commit( );

                    return num;
                } catch( RuntimeException e )
                {
                    logger.error( "Failed to fetch number of games since date: "+e );
                    return -1;
                }
            }

        /** fetch a list of games with offset and page number. */
            public LinkedList< Game > fetchList( int page, int num )
            {
                // populate currentGameList
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                // select all games for which the status is not completed or broken
                try {
                    tx = session.beginTransaction( );
                    List games = session.createQuery( "from Game order by date" ).setFirstResult( page ).setMaxResults( num ).list( );
                    tx.commit( );

                    Iterator git = games.iterator( );

                    LinkedList< Game > list = new LinkedList< Game >( );

                    while( git.hasNext( ) )
                    {
                        Game game = (Game)git.next( );
                        list.add( game );
                    }
                    return list;
                } catch( RuntimeException e )
                {
                    logger.error( "Failed to fetch game list: "+e );
                    return null;
                }
            }

        /** fetch a list of games with offset and page number, since a specific time stamp. */
            public LinkedList< Game > fetchListSince( int page, int num, int date )
            {
                // populate currentGameList
                Session session = HibernateUtil.getSessionFactory( ).openSession( );
                Transaction tx = null;

                // select all games for which the status is not completed or broken
                try {
                    tx = session.beginTransaction( );
                    List games = session.createQuery( "from Game where date>=? order by date " ).setInteger( 0, date ).setFirstResult( page ).setMaxResults( num ).list( );
                    tx.commit( );

                    Iterator git = games.iterator( );

                    LinkedList< Game > list = new LinkedList< Game >( );

                    while( git.hasNext( ) )
                    {
                        Game game = (Game)git.next( );
                        list.add( game );
                    }
                    return list;
                } catch( RuntimeException e )
                {
                    logger.error( "Failed to fetch game list: "+e );
                    return null;
                }
            }
}
