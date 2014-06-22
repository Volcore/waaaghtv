/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.http;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // mina
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
        import org.apache.mina.core.service.*;
        import org.apache.mina.transport.socket.nio.*;
    // asyncweb
        import org.apache.asyncweb.common.*;
    // java
        import java.util.*;
        import java.nio.*;
        import java.nio.charset.*;
        import java.util.regex.*;
    // wtvmaster
        import net.volcore.wtvmaster.*;
        import net.volcore.wtvmaster.game.*;
        import net.volcore.wtvmaster.user.*;
        import net.volcore.wtvmaster.upload.*;
        import net.volcore.wtvmaster.relay.*;

/*******************************************************************************
         The HTTP web interface handler.
 *******************************************************************************/
public class HttpIoHandler implements IoHandler
{
    static Logger   logger = LoggerFactory.getLogger( "HttpIoHandler" );

    protected WTVMaster             master;

    protected final CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( );
    protected final CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( );

    protected final Pattern         statsPattern        = Pattern.compile( "^/stats$" );

    protected final Pattern         cacheListPattern    = Pattern.compile( "^/cache/list$" );
    protected final Pattern         cacheDropPattern    = Pattern.compile( "^/cache/drop$" );

    protected final Pattern         gameInfoPattern     = Pattern.compile( "^/game/info$" );
    protected final Pattern         gameListPattern     = Pattern.compile( "^/game/list$" );
    protected final Pattern         gameSincePattern    = Pattern.compile( "^/game/since$" );

    protected final Pattern         currentListPattern  = Pattern.compile( "^/current/list$" );
    protected final Pattern         currentDropPattern  = Pattern.compile( "^/current/drop$" );
    protected final Pattern         currentAddPattern   = Pattern.compile( "^/current/add$" );

    protected final Pattern         sessionListPattern  = Pattern.compile( "^/session/list$" );
    protected final Pattern         sessionDropPattern  = Pattern.compile( "^/session/drop$" );

    protected final Pattern         authPattern         = Pattern.compile( "(?:(\\w+)\\s+)|(?:(\\w+)=(?:([a-zA-Z_0-9/]*)|(?:\"([^\"]*)\"))(?:,|$)\\s*)" );

    protected String                gameCacheSecret = "test";
    protected String                statsSecret     = "test";

    public HttpIoHandler( WTVMaster master )
    {
        this.master = master;
    }

    public void sessionCreated( IoSession session ) throws Exception 
    {
    }

    public void sessionOpened( IoSession session ) throws Exception 
    {
    }

    public void sessionClosed( IoSession session ) throws Exception 
    {
    }

    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception 
    {
        //logger.trace( "Session idle: "+session+" "+status );
    }

    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception 
    {
        logger.trace( "Exception caught: "+session+" "+cause );
        cause.printStackTrace( );
    }

    public void messageReceived( IoSession session, Object message ) throws Exception 
    {
        HttpRequest req = (HttpRequest) message;

        try {

            java.net.URI uri = req.getRequestUri( );
            String path  = uri.getPath( );
            String query = uri.getQuery( );

            if( statsPattern.matcher( path ).matches( ) == true )
                showStats( session, req );
            else
            if( cacheListPattern.matcher( path ).matches( ) == true )
                showCacheList( session, req );
            else
            if( cacheDropPattern.matcher( path ).matches( ) == true )
                cacheDrop( session, req );
            else
            if( gameInfoPattern.matcher( path ).matches( ) == true )
                showGameInfo( session, req );
            else
            if( gameListPattern.matcher( path ).matches( ) == true )
                showGameList( session, req );
            else
            if( gameSincePattern.matcher( path ).matches( ) == true )
                showGameListSince( session, req );
            else
            if( sessionListPattern.matcher( path ).matches( ) == true )
                showSessionList( session, req );
            else
            if( sessionDropPattern.matcher( path ).matches( ) == true )
                dropSession( session, req );
            else
            if( currentListPattern.matcher( path ).matches( ) == true )
                showCurrentList( session, req );
            else
            if( currentDropPattern.matcher( path ).matches( ) == true )
                dropFromCurrent( session, req );
            else
            if( currentAddPattern.matcher( path ).matches( ) == true )
                addToCurrent( session, req );
            else
                show404( session, req );

            if( req.isKeepAlive( ) == false )
                session.closeOnFlush( );
        } catch( Exception e )
        {
            show500( ""+e, session, req );
        }
    }
    
    public void messageSent( IoSession session, Object message ) throws Exception 
    {
    }

    /** Authentication. */
        /** Parse the auth string. */
            public HashMap< String, String > parseAuth( String s )
            {
                Matcher matcher = authPattern.matcher( s );

                HashMap<String, String> map = new HashMap< String, String >( );

                {
                    while( matcher.find( ) )
                    {
                        if(  matcher.group( 1 ) != null )
                        {
                            map.put( "method", matcher.group( 1 ) );
                        } else
                        if(  matcher.group( 2 ) != null )
                        {
                            if( matcher.group( 3 ) != null )
                                map.put( matcher.group( 2 ), matcher.group( 3 ) );
                            else
                            if( matcher.group( 4 ) != null )
                                map.put( matcher.group( 2 ), matcher.group( 4 ) );
                        }
                    }
                }

                return map;
            }

        /** Basic */
            public static final int AUTH_OKAY = 0;
            public static final int AUTH_STALE = 1;
            public static final int AUTH_INVALID = 2;

            class AuthResult
            {
                int status = AUTH_INVALID;
                String opaque = null; // only set if AUTH_STALE

                AuthResult( int status ) { this.status = status; }
                AuthResult( int status, String opaque ) { this.status = status; this.opaque = opaque; }
            }

            public AuthResult hasValidAuth( HttpRequest req  )
            {
                if( req.getHeader( "Authorization" ) == null )
                    return new AuthResult( AUTH_INVALID );

                HashMap< String, String > authmap = parseAuth( req.getHeader( "Authorization" ) );

                if( authmap.containsKey( "method" ) == false 
                ||  authmap.get( "method" ).equals( "Digest" ) == false )
                {
                    logger.trace( "Invalid auth reply: "+req.getHeader( "Authorization" ) );
                    return new AuthResult( AUTH_INVALID );
                }

                if( authmap.containsKey( "realm" ) == false 
                ||  authmap.containsKey( "opaque" ) == false 
                ||  authmap.containsKey( "uri" ) == false 
                ||  authmap.containsKey( "response" ) == false
                ||  authmap.containsKey( "nonce" ) == false
                ||  authmap.containsKey( "cnonce" ) == false
                ||  authmap.containsKey( "username" ) == false
                ||  authmap.containsKey( "nc" ) == false )
                {
                    logger.trace( "Invalid auth reply: "+req.getHeader( "Authorization" ) );
                    return new AuthResult( AUTH_INVALID );
                }

                String nonce = authmap.get( "nonce" );
                String opaque = authmap.get( "opaque" );
                int nc = Integer.parseInt( authmap.get( "nc" ), 16 );

                AuthData data = authData.get( opaque );

                if( data == null )
                {
                    logger.trace( "Unknown opaque: "+req.getHeader( "Authorization" ) );
                    return new AuthResult( AUTH_STALE, opaque );
                }

                if( data.nonce.equals( nonce ) == false )
                {
                    logger.trace( "Invalid nonce: "+req.getHeader( "Authorization" )+", expected "+data.nonce );
                    return new AuthResult( AUTH_STALE, opaque );
                }

                if( data.lastnc >= nc )
                {
                    logger.trace( "Old nonce count: "+req.getHeader( "Authorization" )+", expected >"+data.lastnc );
                    return new AuthResult( AUTH_STALE, opaque );
                }

                data.lastnc = nc;

                String username = authmap.get( "username" );
                String correctUsername = "admin";
                String correctPassword = "defaultpassword";

                if( username.equals( correctUsername )==false )
                {
                    logger.trace( "Invalid username: '"+username+"'" );
                    return new AuthResult( AUTH_INVALID );
                }

                String response = authmap.get( "response" );
                String cnonce   = authmap.get( "cnonce" );
                String ncstring = authmap.get( "nc" );
                String realm    = authmap.get( "realm" );
                String uri      = authmap.get( "uri" );

                String ha1 = Checksum.getMD5String( ( correctUsername+":"+realm+":"+correctPassword ).getBytes( )  );
                String ha2 = Checksum.getMD5String( ( req.getMethod( )+":"+uri ).getBytes( )  );
                String resp = Checksum.getMD5String( ( ha1+":"+nonce+":"+ncstring+":"+cnonce+":auth:"+ha2 ).getBytes( )  );

                return new AuthResult( response.equals( resp )==false?AUTH_INVALID:AUTH_OKAY );
            }

            public void addAuthData( String opaque, String nonce )
            {
                AuthData ad = new AuthData( );
                ad.nonce = nonce;

                authData.put( opaque, ad );
            }

            HashMap< String, AuthData > authData = new HashMap< String, AuthData >( );

            class AuthData
            {
                String nonce;
                int  lastnc = -1;
            }

    /** The handlers. */
        /** /stats */
            public void showStats( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( 
                        "{ "
                    +       "\"info\":"
                    +       "{"
                    +           "\"version\": \""+master.getVersionString( )+"\", "
                    +           "\"uptime\": "+master.getUptime( )+"  "
                    +       "},"
                    +       "\"relay\":"
                    +       "{"
                    +           "\"current\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRelayNumSessions( )+", "
                    +               "\"bytesReadPerSecond\": "+master.getRelayBytesReadPerSecond( )+", "
                    +               "\"bytesWrittenPerSecond\": "+master.getRelayBytesWrittenPerSecond( )+" "
                    +           "}, "
                    +           "\"total\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRelayTotalSessionCount( )+", "
                    +               "\"bytesRead\": "+master.getRelayTotalBytesRead( )+", "
                    +               "\"bytesWritten\": "+master.getRelayTotalBytesWritten( )+" "
                    +           "}, "
                    +           "\"peak\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRelayPeakSessionCount( )+", "
                    +               "\"bytesReadPerSecond\": "+master.getRelayPeakBytesReadPerSecond( )+", "
                    +               "\"bytesWrittenPerSecond\": "+master.getRelayPeakBytesWrittenPerSecond( )+" "
                    +           "} "
                    +       "},"
                    +       "\"recorder\":"
                    +       "{"
                    +           "\"current\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRecorderNumSessions( )+", "
                    +               "\"bytesReadPerSecond\": "+master.getRecorderBytesReadPerSecond( )+", "
                    +               "\"bytesWrittenPerSecond\": "+master.getRecorderBytesWrittenPerSecond( )+" "
                    +           "}, "
                    +           "\"total\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRecorderTotalSessionCount( )+", "
                    +               "\"bytesRead\": "+master.getRecorderTotalBytesRead( )+", "
                    +               "\"bytesWritten\": "+master.getRecorderTotalBytesWritten( )+" "
                    +           "}, "
                    +           "\"peak\": "
                    +           "{ "
                    +               "\"sessions\": "+master.getRecorderPeakSessionCount( )+", "
                    +               "\"bytesReadPerSecond\": "+master.getRecorderPeakBytesReadPerSecond( )+", "
                    +               "\"bytesWrittenPerSecond\": "+master.getRecorderPeakBytesWrittenPerSecond( )+" "
                    +           "} "
                    +       "}"
                    +   "}"
                    , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /cache/list */
            public void showCacheList( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "[", charsetEncoder );
                Enumeration< Game > git = master.gameDB.getCacheElements( );
                while( git.hasMoreElements( ) )
                {
                    Game game = git.nextElement( );
                    buf.putString( "{", charsetEncoder );
                    buf.putString( "\"id\": "+game.getId( )+", ", charsetEncoder );
                    buf.putString( "\"at\": "+game.getLastAccessTime( ), charsetEncoder );
                    buf.putString( "}", charsetEncoder );
                    if( git.hasMoreElements( ) )
                        buf.putString( ", ", charsetEncoder );
                }
                buf.putString( "]", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /cache/drop */
            public void cacheDrop( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String id = req.getParameter( "i" );

                if( id == null )
                {
                    show400( "Please specify the game to be deleted!", session, req );
                    return;
                }

                int gid = Integer.parseInt( id );

                boolean success = master.gameDB.dropCacheItem( gid );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{ \"success\": "+success+" }", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /game/info */
            public void showGameInfo( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String id = req.getParameter( "i" );

                if( id == null )
                {
                    show400( "Please specify the game for info!", session, req );
                    return;
                }

                int gid = Integer.parseInt( id );

                Game game = master.gameDB.fetchGame( gid );

                if( game == null )
                {
                    show404( session, req );
                    return;
                }

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{", charsetEncoder );
                buf.putString( "\"id\": "+game.getId( )+", ", charsetEncoder );
                buf.putString( "\"status\": "+game.getStatus( )+", ", charsetEncoder );
                buf.putString( "\"date\": "+game.getDate( )+", ", charsetEncoder );
                buf.putString( "\"name\": \""+game.getName( ).replace("\"","\\\"")+"\", ", charsetEncoder );
                buf.putString( "\"comment\": \""+game.getComment( ).replace("\"","\\\"")+"\", ", charsetEncoder );
                buf.putString( "\"streamer\": \""+game.getStreamer( ).replace("\"","\\\"")+"\", ", charsetEncoder );
                buf.putString( "\"length\": "+game.getGameLength( )+", ", charsetEncoder );
                buf.putString( "\"numpackets\": "+game.getNumPackets( )+", ", charsetEncoder );
                buf.putString( "\"delay\": "+game.getDelay( )+", ", charsetEncoder );
                buf.putString( "\"path\": \""+game.getPath( )+"\", ", charsetEncoder );
                buf.putString( "\"checksum\": \""+game.getChecksum( )+"\", ", charsetEncoder );
                buf.putString( "\"streamerid\": "+game.getUploaderId( )+", ", charsetEncoder );
                buf.putString( "\"datasize\": "+game.getGameDataSize( )+", ", charsetEncoder );
                buf.putString( "\"at\": "+game.getLastAccessTime( )+", ", charsetEncoder );
                buf.putString( "\"organisation\": \""+game.getOrganisation( )+"\", ", charsetEncoder );
                buf.putString( "\"certified\": "+game.getCertified( ), charsetEncoder );
                buf.putString( "}", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /game/list */
            public void showGameList( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String page = req.getParameter( "o" );

                int pid = 0;
                if( page != null )
                    pid = Integer.parseInt( page );

                LinkedList< Game > games = master.gameDB.fetchList( pid, 50 );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{ \"num\":"+master.gameDB.getNumGames( )+", \"offset\":"+pid+", \"page\":[", charsetEncoder );
                Iterator< Game > git = games.iterator( );
                while( git.hasNext( ) )
                {
                    Game game = git.next( );
                    buf.putString( ""+game.getId( ), charsetEncoder );
                    if( git.hasNext( ) )
                        buf.putString( ", ", charsetEncoder );
                }
                buf.putString( "]}", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /game/since */
            public void showGameListSince( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String page = req.getParameter( "o" );
                String since = req.getParameter( "t" );

                int pid = 0;
                if( page != null )
                    pid = Integer.parseInt( page );

                int t = 0;
                if( since != null )
                    t = Integer.parseInt( since );

                LinkedList< Game > games = master.gameDB.fetchListSince( pid, 50, t );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                //res.setContentType( "text/x-json" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{ \"num\":"+master.gameDB.getNumGamesSince( t )+", \"offset\":"+pid+", \"page\":[", charsetEncoder );
                Iterator< Game > git = games.iterator( );
                while( git.hasNext( ) )
                {
                    Game game = git.next( );
                    buf.putString( ""+game.getId( ), charsetEncoder );
                    if( git.hasNext( ) )
                        buf.putString( ", ", charsetEncoder );
                }
                buf.putString( "]}", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /session/list */
            public void showSessionList( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                Map<Long,IoSession> relaylist       = master.getRelayAcceptor( ).getManagedSessions( );
                Map<Long,IoSession> recorderlist    = master.getRecorderAcceptor( ).getManagedSessions( );
                Map<Long,IoSession> httplist        = master.getWebAcceptor( ).getManagedSessions( );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{ ", charsetEncoder );
            
                // relay
                    buf.putString( "\"relay\":[", charsetEncoder );
                    Iterator< IoSession > sit = relaylist.values( ).iterator( );
                    while( sit.hasNext( ) )
                    {
                        IoSession sess = sit.next( );
                        buf.putString( "{", charsetEncoder );
                        buf.putString( "\"id\":"+sess.getId( )+",", charsetEncoder );
                        buf.putString( "\"lastio\":"+sess.getLastIoTime( )+",", charsetEncoder );
                        buf.putString( "\"lastread\":"+sess.getLastReadTime( )+",", charsetEncoder );
                        buf.putString( "\"lastwrite\":"+sess.getLastWriteTime( )+",", charsetEncoder );
                        buf.putString( "\"readbytes\":"+sess.getReadBytes( )+",", charsetEncoder );
                        buf.putString( "\"readbytetp\":"+sess.getReadBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"readmsgs\":"+sess.getReadMessages( )+",", charsetEncoder );
                        buf.putString( "\"readmsgstp\":"+sess.getReadMessagesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"address\":"+sess.getRemoteAddress( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritebyte\":"+sess.getScheduledWriteBytes( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritemsgs\":"+sess.getScheduledWriteMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytes\":"+sess.getWrittenBytes( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytestp\":"+sess.getWrittenBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgs\":"+sess.getWrittenMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgstp\":"+sess.getWrittenMessagesThroughput( ), charsetEncoder );
                        buf.putString( "}", charsetEncoder );

                        if( sit.hasNext( ) )
                            buf.putString( ",", charsetEncoder );

                    }
                    buf.putString( "],", charsetEncoder );

                // recorder
                    buf.putString( "\"recorder\":[", charsetEncoder );
                    sit = recorderlist.values( ).iterator( );
                    while( sit.hasNext( ) )
                    {
                        IoSession sess = sit.next( );
                        buf.putString( "{", charsetEncoder );
                        buf.putString( "\"id\":"+sess.getId( )+",", charsetEncoder );
                        buf.putString( "\"lastio\":"+sess.getLastIoTime( )+",", charsetEncoder );
                        buf.putString( "\"lastread\":"+sess.getLastReadTime( )+",", charsetEncoder );
                        buf.putString( "\"lastwrite\":"+sess.getLastWriteTime( )+",", charsetEncoder );
                        buf.putString( "\"readbytes\":"+sess.getReadBytes( )+",", charsetEncoder );
                        buf.putString( "\"readbytetp\":"+sess.getReadBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"readmsgs\":"+sess.getReadMessages( )+",", charsetEncoder );
                        buf.putString( "\"readmsgstp\":"+sess.getReadMessagesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"address\":"+sess.getRemoteAddress( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritebyte\":"+sess.getScheduledWriteBytes( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritemsgs\":"+sess.getScheduledWriteMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytes\":"+sess.getWrittenBytes( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytestp\":"+sess.getWrittenBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgs\":"+sess.getWrittenMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgstp\":"+sess.getWrittenMessagesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"username\": \""+(String)sess.getAttribute( RecorderIoHandler.SessionAttribute.USERNAME )+"\"", charsetEncoder );
                        buf.putString( "}", charsetEncoder );

                        if( sit.hasNext( ) )
                            buf.putString( ",", charsetEncoder );

                    }

                    buf.putString( "],", charsetEncoder );

                // web
                    buf.putString( "\"web\":[", charsetEncoder );
                    sit = httplist.values( ).iterator( );
                    while( sit.hasNext( ) )
                    {
                        IoSession sess = sit.next( );
                        buf.putString( "{", charsetEncoder );
                        buf.putString( "\"id\":"+sess.getId( )+",", charsetEncoder );
                        buf.putString( "\"lastio\":"+sess.getLastIoTime( )+",", charsetEncoder );
                        buf.putString( "\"lastread\":"+sess.getLastReadTime( )+",", charsetEncoder );
                        buf.putString( "\"lastwrite\":"+sess.getLastWriteTime( )+",", charsetEncoder );
                        buf.putString( "\"readbytes\":"+sess.getReadBytes( )+",", charsetEncoder );
                        buf.putString( "\"readbytetp\":"+sess.getReadBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"readmsgs\":"+sess.getReadMessages( )+",", charsetEncoder );
                        buf.putString( "\"readmsgstp\":"+sess.getReadMessagesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"address\":"+sess.getRemoteAddress( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritebyte\":"+sess.getScheduledWriteBytes( )+",", charsetEncoder );
                        buf.putString( "\"scheduledwritemsgs\":"+sess.getScheduledWriteMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytes\":"+sess.getWrittenBytes( )+",", charsetEncoder );
                        buf.putString( "\"writtenbytestp\":"+sess.getWrittenBytesThroughput( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgs\":"+sess.getWrittenMessages( )+",", charsetEncoder );
                        buf.putString( "\"writtenmsgstp\":"+sess.getWrittenMessagesThroughput( ), charsetEncoder );
                        buf.putString( "}", charsetEncoder );

                        if( sit.hasNext( ) )
                            buf.putString( ",", charsetEncoder );

                    }
                    buf.putString( "]", charsetEncoder );

                buf.putString( "}", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /session/drop */
            public void dropSession( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String id = req.getParameter( "i" );

                if( id == null )
                {
                    show400( "Please specify the id of the session!", session, req );
                    return;
                }

                long sid = Long.parseLong( id );

                Map<Long,IoSession> relaylist       = master.getRelayAcceptor( ).getManagedSessions( );
                Map<Long,IoSession> recorderlist    = master.getRecorderAcceptor( ).getManagedSessions( );
                Map<Long,IoSession> httplist        = master.getWebAcceptor( ).getManagedSessions( );


                boolean success = false;

                IoSession sess = relaylist.get( new Long( sid ) );
                if( sess != null )
                {
                    success |= true;
                    sess.close( );
                }
                sess = recorderlist.get( new Long( sid ) );
                if( sess != null )
                {
                    success |= true;
                    sess.close( );
                }

                sess = httplist.get( new Long( sid ) );
                if( sess != null )
                {
                    success |= true;
                    sess.close( );
                }

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );

                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( "{ \"success\": "+success+" }", charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /current/list */
            public void showCurrentList( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                LinkedList<Integer> list = master.gameDB.getCurrentGameList( );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );

                buf.putString( "[", charsetEncoder );

                synchronized( list )
                {
                    Iterator< Integer > iit = list.iterator( );

                    while( iit.hasNext( ) )
                    {
                        Integer i = iit.next( );
                        buf.putString( ""+i, charsetEncoder );
                        if( iit.hasNext( ) )
                            buf.putString( ",", charsetEncoder );
                    }
                }

                buf.putString( "]", charsetEncoder );

                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /current/drop */
            public void dropFromCurrent( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String id = req.getParameter( "i" );

                if( id == null )
                {
                    show400( "Please specify the id of the game!", session, req );
                    return;
                }

                long sid = Long.parseLong( id );

                boolean success = master.gameDB.removeFromCurrentGameList( (int)sid );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );

                buf.putString( "{ \"success\": "+success+"}", charsetEncoder );

                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** /current/add */
            public void addToCurrent( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                AuthResult auth = hasValidAuth( req );
                if( auth.status != AUTH_OKAY )
                {
                    show401( session, req, auth );
                    return;
                }

                String id = req.getParameter( "i" );

                if( id == null )
                {
                    show400( "Please specify the id of the game!", session, req );
                    return;
                }

                long sid = Long.parseLong( id );

                master.gameDB.addGameToList( (int)sid );
                master.gameDB.buildGameList( );

                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.OK );
                res.setContentType( "text/plain" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );

                buf.putString( "{ \"success\": true}", charsetEncoder );

                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** 400 */
            public void show400( String message, IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.NOT_FOUND );
                res.setContentType( "text/html" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( 
                        "<html><body><center><h1>400</h1>"+req.getRequestUri( )+"<h2>"+message+"</h2></center></body></html>"
                                , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** 401 */
            public void show401( IoSession session, HttpRequest req, AuthResult prevAuth) throws CharacterCodingException
            {
                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.UNAUTHORIZED );
                res.setContentType( "text/html" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );

                String nonce = HexDump.generateRandomHexString( 8 );
                String opaque = HexDump.generateRandomHexString( 8 );
                String stale ="";

                if( prevAuth.status == AUTH_STALE )
                {
                    stale = ", stale=true"; 
                    opaque = prevAuth.opaque;
                }

                addAuthData( opaque, nonce );

                String authString = "Digest realm=\"WTVMaster\", qop=\"auth\", nonce=\""+nonce+"\", opaque=\""+opaque+"\""+stale;
                res.setHeader( "WWW-Authenticate", authString );

                buf.putString( 
                        "<html><body><center><h1>401</h1>"+req.getRequestUri( )+"<br/></center></body></html>"
                                , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** 403 */
            public void show403( String message, IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.FORBIDDEN );
                res.setContentType( "text/html" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( 
                        "<html><body><center><h1>403</h1>"+req.getRequestUri( )+"<br/><h2>"+message+"</h2></center></body></html>"
                                , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** 404 */
            public void show404( IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.NOT_FOUND );
                res.setContentType( "text/html" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( 
                        "<html><body><center><h1>404</h1>"+req.getRequestUri( )+"</center></body></html>"
                                , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }

        /** 500 */
            public void show500( String message, IoSession session, HttpRequest req ) throws CharacterCodingException
            {
                MutableHttpResponse res = new DefaultHttpResponse( );
                res.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
                res.setContentType( "text/html" );
                IoBuffer buf = IoBuffer.allocate( 1 );
                buf.setAutoExpand( true );
                buf.putString( 
                        "<html><body><center><h1>500</h1>"+req.getRequestUri( )+"</center><h2>"+message+"</h2></body></html>"
                                , charsetEncoder );
                buf.flip( );
                res.setContent( buf );
                res.normalize( req );
                session.write( res );
            }
}


