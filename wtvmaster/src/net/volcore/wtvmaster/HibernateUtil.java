/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // hibernate
        import org.hibernate.*;
        import org.hibernate.cfg.*;



public class HibernateUtil
{
    private static final SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory( )
    {
        return sessionFactory;
    }

    static 
    {
        try 
        {
            sessionFactory = new Configuration( ).configure( "hibernate/hibernate.properties" ).buildSessionFactory( );
        }
        catch( Throwable ex ) 
        {
            System.err.println( "Failed to initialize SessionFactory: "+ex );
            ex.printStackTrace( );
            throw new ExceptionInInitializerError( ex );
        }
    }

}

