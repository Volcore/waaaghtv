/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.uplink;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.*;
        import net.volcore.wtvmina.messages.*;
    // mina
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
        import org.apache.mina.core.future.*;
        import org.apache.mina.core.service.*;
        import org.apache.mina.transport.socket.nio.*;
    // java
        import java.net.*;
        import java.util.*;
        import java.nio.*;
        import java.nio.charset.*;
    // wtvcache
        import net.volcore.wtvcache.game.*;
        import net.volcore.wtvcache.*;

public abstract class UplinkSession
{
    protected int serverId;
    protected WTVCache wtvCache;
    protected boolean loggedin = false;

    /** Getter for loggedin */
    	public boolean getLoggedin( ) { return loggedin; }
    /** Setter for loggedin */
    	public void setLoggedin( boolean loggedin ) { this.loggedin = loggedin; }

    public UplinkSession( int serverId, WTVCache wtvCache )
    {
        this.serverId = serverId;
        this.wtvCache = wtvCache;
    }

    public abstract void sessionClosed( IoSession session );
    public abstract void sessionIdle( IoSession session, IdleStatus status );
    public abstract void onLoggedIn( IoSession session );
    public abstract void processMessage( IoSession session, Object message);
}


