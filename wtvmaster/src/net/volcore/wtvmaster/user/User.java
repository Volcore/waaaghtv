/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.user;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java persistance
        import javax.persistence.*;


//@Entity
public class User
{
    private int     id;
    private String  name;
    private String  password;

    /** Getter for id */
        //@Id
        //@GeneratedValue(strategy=GenerationType.AUTO)
    	public int getId( ) { return id; }
    /** Setter for id */
    	public void setId( int id ) { this.id = id; }


    /** Getter for name */
    	public String getName( ) { return name; }
    /** Setter for name */
    	public void setName( String name ) { this.name = name; }
    /** Getter for password */
    	public String getPassword( ) { return password; }
    /** Setter for password */
    	public void setPassword( String password ) { this.password = password; }
}

