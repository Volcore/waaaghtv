/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.user;

public class RemoteUserBean
{
    protected boolean       valid = false;
    protected boolean       canupload = false;
    protected int           userid = -1;

    /** Getter for valid */
    	public boolean getValid( ) { return valid; }
    /** Setter for valid */
    	public void setValid( boolean valid ) { this.valid = valid; }
    /** Getter for canupload */
    	public boolean getCanupload( ) { return canupload; }
    /** Setter for canupload */
    	public void setCanupload( boolean canupload ) { this.canupload = canupload; }
    /** Getter for userid */
    	public int getUserid( ) { return userid; }
    /** Setter for userid */
    	public void setUserid( int userid ) { this.userid = userid; }
}
