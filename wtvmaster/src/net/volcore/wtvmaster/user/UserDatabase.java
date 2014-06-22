/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.user;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.util.*;

abstract public class UserDatabase
{
    public final static int         INVALID_USERID = -1;
    abstract public int isValidUploadAccount( String username, String password );
    
    public final static int         VALID_ORGANISATION_MASK = 1;
    public final static int         CERTIFIED_ORGANISATION_MASK  = 2;
    abstract public int validateOrganisation( String username, String organisation );
}

