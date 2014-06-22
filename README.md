Waaagh!TV
=========

After almost 10 years of service, the official Waaagh!TV server was shut down due to a database server hardware failure.

I've decided to release all the server code to the public, so that anyone interested can go in and set up a new server.


Subprojects
-----------
The Server infrastructure consists of a number of different services:

* _database_ This is a database server (currently only MySQL is supported right away, through hibernate)
* _wtvmaster_ Is the master server. This is where the games are uploaded. Uploaders are authenticated against a database. The master server stores all games to disk.
* _wtvcache_ Is a frontend server that serves the games to the viewers. It connects to the master server, forwards and caches all the games found there. All games are kept in memory, it does not use any disk space (other than logging).

wtvmaster does not require wtvcaches to function, viewers can connect directly to the master server. This is, however, not recommended, as a crash or issue on the master server will bring down the entire system. The master server upload system runs on port 10384, while the relay system runs on port 11383.

wtvcaches usually run on port 10383, which is the Waaagh!TV client default port.

Simple Setup
------------
The simplest setup would be to just run a wtvmaster server on port 10384 (uploaders) and 10383 (clients), and connect it to a small local MySQL database. I've added a sample scheme for the database that should be sufficient to create the necessary tables.

Tutorial (How to set up a simple master server)
-----------------------------------------------
1. Install a clean Ubuntu machine (in this case 14.04). In this case, the machine name is wtv, as well as the username.
2. 
