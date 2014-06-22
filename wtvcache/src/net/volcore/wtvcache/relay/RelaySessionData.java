/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.relay;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;

public class RelaySessionData
{
    public enum SubscriptionState
    {
        NOTHING,
        DETAILS,
        DELAYED,
        STARTED,
        FINISHED
    }

    public boolean                  loggedIn = false;
    public Integer                  subscribedGame = null;
    public int                      dataOffset = 0;
    public int                      blockId = 1;
    public int                      ackedBlockId = 0;
    public boolean                  isWebClient = false;
    public boolean                  expectCacheHack = false;
    public SubscriptionState        state = SubscriptionState.NOTHING;
    public String                   hostname;
    
    public String toString( )
    {
        return  "{"
            +   "\"loggedin\": "+loggedIn
            +   ",\"subscribedGame\": "+subscribedGame
            +   ",\"dataOffset\": "+dataOffset
            +   ",\"blockId\": "+blockId
            +   ",\"ackedBlockId\": "+ackedBlockId
            +   ",\"isWebClient\": "+isWebClient
            +   ",\"state\": "+state
            +   ",\"hostname\": \""+hostname+"\""
            +   "}";
    }
}
