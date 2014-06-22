/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina.messages;

public class WTVHandshake
{
    public byte major;
    public short minor;
    public byte release;

    public WTVHandshake( byte major, short minor, byte release )
    {
        this.major = major;
        this.minor = minor;
        this.release = release;
    }

    public String toString( )
    {
        return "("+this.getClass( ).getName( )+": "+major+"."+minor+"."+release+")";
    }
}
