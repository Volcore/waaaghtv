/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util.config;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.lang.annotation.*; 


/*******************************************************************************
         Use this to annotated config String, order them by groups.
 *******************************************************************************/
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigString
{
    String name( );
    String defaultValue( ) default "";
    String description( ) default "";
    String group( ) default "";
}
