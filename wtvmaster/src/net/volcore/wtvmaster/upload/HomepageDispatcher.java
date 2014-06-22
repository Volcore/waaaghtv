/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.upload;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // wtvmaster
        import net.volcore.wtvmaster.game.*;
        import net.volcore.wtvmaster.*;
    // java
        import java.util.*;
        import java.net.*;
        import java.io.*;
        import java.util.concurrent.*;
        import java.nio.charset.*;
    // apache httpclient
        import org.apache.http.*;
        import org.apache.http.entity.*;
        import org.apache.http.client.*;
        import org.apache.http.client.methods.*;
        import org.apache.http.impl.client.*;
    // gson
        import com.google.gson.*;

public class HomepageDispatcher
{
    static Logger   logger = LoggerFactory.getLogger( "HomepageDispatcher" );

    /** Constants. */
        /** Generic. */
            public static final int     NUM_THREADPOOLWORKER = 4;

    /** Member variables */
        /** Threadpool for async  */
            protected ScheduledThreadPoolExecutor       workerThreadPool = new ScheduledThreadPoolExecutor( NUM_THREADPOOLWORKER );
        /** Reference to daddy. */
            protected WTVMaster wtvMaster;
            protected String    url  = "http://www.waaaghtv.com/en/games/master/update/";

    /** Auxiliary. */
        /** Constructor */
            public HomepageDispatcher( WTVMaster wtvMaster )
            {
                this.wtvMaster = wtvMaster;
            }

            public String escape( String str )
            {
                // replace \ and "
                return str.replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
            }

    /** Public functionality */
        public void dispatch( final int gameid )
        {
            workerThreadPool.submit( new Runnable( )
            {
                public void run( )
                {
                    String str = "";
                    try {
                        HttpClient httpclient = new DefaultHttpClient();
                        Game game = wtvMaster.gameDB.fetchGame( gameid );

                        GameInfo gi = game.getParsedGameInfo( );

                        String version = "";

                        if( gi.gameTag == 1462982736 )
                            version = "W3XP 1."+gi.gameVersion;
                        else
                            version = "WAR3 1."+gi.gameVersion;

                        HttpPost httppost = new HttpPost( url );
                        str =   
                            "{"
                                +"\"id\":"+gameid+","
                                +"\"s\":"+( (gameid*5039)%2311 )+","
                                +"\"status\":"+game.getHpStatus( )+","
                                +"\"date\":"+game.getDate( )+","
                                +"\"name\":\""+escape(game.getName( ))+"\","
                                +"\"players\":\""+escape(game.getComment( ))+"\","
                                +"\"streamer\":\""+escape(game.getStreamer( ))+"\","
                                +"\"length\":"+game.getGameLength( )+","
                                +"\"version\":\""+version+"\","
                                +"\"mapname\":\""+escape(gi.mapName)+"\","
                                +"\"mapcheck\":"+gi.mapCheck+","
                                +"\"certified\": "+game.getCertified( )+","
                                +"\"organisation\": \""+escape(game.getOrganisation( ))+"\""
                            +"}";
                        httppost.setEntity( new ByteArrayEntity( str.getBytes( ) ) );
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        String responseBody = httpclient.execute( httppost, responseHandler);
                    } catch( Exception e )
                    {
                        logger.error( "Failed to update hp: "+e );
                        logger.error( "Request was: "+str );
                        e.printStackTrace( );
                    }

                }
            } );
        }
}

