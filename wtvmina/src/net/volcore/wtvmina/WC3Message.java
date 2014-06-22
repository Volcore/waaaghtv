/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

/*******************************************************************************
         Basis for the Warcraft 3 protocol messages.
 *******************************************************************************/
public interface WC3Message
{
    /** Sent by the server to the users to notify about a running, idling server. */

        /** UDP Messages. */
            public final static byte        UDPINFOREQUEST          = (byte) 0x2F;
            public final static byte        UDPSERVERINFO           = (byte) 0x30;
            public final static byte        UDPNEWSERVERBROADCAST   = (byte) 0x31;
            public final static byte        UDPIDLESERVERBROADCAST  = (byte) 0x32;
            public final static byte        UDPCLOSESERVERBROADCAST = (byte) 0x33;

    /** The message primer. */
    public final static byte        PRIMER = (byte) 0xf7;

    public abstract byte[] assemble( );
}

