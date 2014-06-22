/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

// imports
    // mina
        import org.apache.mina.core.buffer.*;

/*******************************************************************************
         This is a message sent or received by the cache. Protocol version 1.
 *******************************************************************************/
public interface WTVMessage
{
    public IoBuffer assemble( );
}
