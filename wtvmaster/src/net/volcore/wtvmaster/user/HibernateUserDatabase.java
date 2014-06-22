/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.user;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // hibernate
        import org.hibernate.*;
    // java
        import java.util.*;
    // wtvmaster
        import net.volcore.wtvmaster.*;

public class HibernateUserDatabase extends UserDatabase
{
    static Logger   logger = LoggerFactory.getLogger( "HibernateUserDatabase" );

        public void createUser( String username, String password )
        {
            Session session = HibernateUtil.getSessionFactory( ).openSession( );

            try 
                {
                    session.beginTransaction( );

                    User user = new User( );
                    user.setName( username );
                    user.setPassword( password );

                    session.save( user );

                    session.getTransaction( ).commit( );
                } 
            finally
                {
                    session.close( );
                }
        }

        public int isValidUploadAccount( String username, String password )
        {
            Session session = HibernateUtil.getSessionFactory( ).openSession( );

            try 
            {
                    Transaction tx = session.beginTransaction( );
                    List users = session.createQuery( "from User where name=?" ).setString( 0, username ).list( );
                    tx.commit( );

                    if( users.size( ) == 0 )
                        return INVALID_USERID;

                    User user = (User)users.get( 0 );

                    if( user.getPassword( ).equals( password ) == false )
                        return INVALID_USERID;

                    return user.getId( );
            } 
            finally
            {
                session.close( );
            }
        }

        public int validateOrganisation( String username, String organisation )
        {
            return 0;
        }
}

