/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.user;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.util.*;
        import java.net.*;
        import java.io.*;
        import java.nio.charset.*;
    // apache httpclient
        import org.apache.http.*;
        import org.apache.http.client.*;
        import org.apache.http.client.methods.*;
        import org.apache.http.impl.client.*;
    // gson
        import com.google.gson.*;


public class RemoteUserDatabase extends UserDatabase
{
    static Logger       logger = LoggerFactory.getLogger( "RemoteUserDatabase" );
    protected String    url;
    protected String    url2;

    public RemoteUserDatabase( String url, String url2 )
    {
        this.url = url;
        this.url2 = url2;
    }

        public int isValidUploadAccount( String username, String password )
        {
            try {
                HttpClient httpclient = new DefaultHttpClient();
                logger.info( "Checking user credentials for "+username+"...");
                //logger.info( "TMP with pass "+password+"...");

                HttpGet httpget = new HttpGet( url+"?u="+username+"&p="+password );
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                //logger.info( "response "+responseBody+"...");

                Gson gson = new Gson();
                RemoteUserBean userbean = gson.fromJson( responseBody, RemoteUserBean.class);   

                if( userbean.getValid( ) == false )
                {
                    logger.info( "Invalid login!" );
                    return INVALID_USERID;
                }

                if( userbean.getCanupload( ) == false )
                {
                    logger.info( "Not a recorder login!" );
                    return INVALID_USERID;
                }

                logger.trace( "Authed user "+username+" ("+userbean.getUserid( )+")" );

                return userbean.getUserid( );
            } catch( Exception e )
            {
                logger.error( "Exception in isValidUploadAccount: "+e );
                e.printStackTrace( );
            }

            return INVALID_USERID;
        }


        public int validateOrganisation( String username, String organisation )
        {
            try {
                HttpClient httpclient = new DefaultHttpClient();
                logger.trace( "Checking organisation affiliation of "+username+" to "+organisation+"...");

                HttpGet httpget = new HttpGet( url2+"?u="+username+"&o="+organisation );
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);

                Gson gson = new Gson();
                OrganisationBean orgbean = gson.fromJson( responseBody, OrganisationBean.class );

                if( orgbean.getValid( ) == false )
                {
                    logger.info( "Invalid affiliation!" );
                    return 0;
                }

                int mask = VALID_ORGANISATION_MASK;

                if( orgbean.getCertified( ) == true )
                {
                    //logger.trace( "Certified!" );
                    mask |= CERTIFIED_ORGANISATION_MASK;
                }

                return mask;
            } catch( Exception e )
            {
                logger.error( "Exception in validateOrganisation: "+e );
                e.printStackTrace( );
            }

            return 0;
        }
}

