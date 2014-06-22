Waaagh!TV
=========

After almost 10 years of service, the official Waaagh!TV server was shut down due to a database server hardware failure.

I've decided to release all the server code to the public, so that anyone interested can go in and set up a new server.

Cheers,
Volker
volker@limbic.com


Subprojects
-----------
The Server infrastructure consists of a number of different services:

* _database_ This is a database server (currently only MySQL is supported right away, through hibernate)
* _wtvmaster_ Is the master server. This is where the games are uploaded. Uploaders are authenticated against a database. The master server stores all games to disk.
* _wtvcache_ Is a frontend server that serves the games to the viewers. It connects to the master server, forwards and caches all the games found there. All games are kept in memory, it does not use any disk space (other than logging).

wtvmaster does not require wtvcaches to function, viewers can connect directly to the master server. This is, however, not recommended, as a crash or issue on the master server will bring down the entire system. The master server upload system runs on port 10384, while the relay system runs on port 8888.

wtvcaches usually run on port 10383, which is the Waaagh!TV client default port.

Simple Setup
------------
The simplest setup would be to just run a wtvmaster server on port 10384 (uploaders) and 10383 (clients), and connect it to a small local MySQL database. I've added a sample scheme for the database that should be sufficient to create the necessary tables.

Tutorial (How to set up a simple master server)
-----------------------------------------------
1. Install a clean Ubuntu server with OpenSSH (in this case Ubuntu 14.04 64 bit). In this case, the machine name is wtv, as well as the username.
2. Install Java via "sudo apt-get install default-jre"
3. Install mysql via "sudo apt-get install mysql-server"
4. On your local machine, run ./build in wtvmaster to build net-volcore-wtvmaster.jar
5. Make new folder wtvmaster on the server
6. Copy everything from wtvmaster/lib into ~/wtvmaster/lib on the server
7. Copy net-volcore-wtvmaster.jar into ~/wtvmaster on the server
8. Copy the "wtvmaster" script into ~/wtvmaster on the server. Uncomment the CP=net-volcore-wtvmaster.jar line in the script on the server
9. At this point, running `./wtvmaster -p 10384 -P 8888` should work. The recorder should be able to connect to the server on port 10384, and a client should be able to connect on port 8888.
10. Run `mysql -u root` to set up the database:

    CREATE DATABASE `wtvmaster`;
    USE `wtvmaster`;

    CREATE USER 'wtvmaster'@'localhost' IDENTIFIED BY 'defaultpass';
    GRANT ALL PRIVILEGES ON wtvmaster.* TO 'wtvmaster'@'%';
    FLUSH PRIVILEGES;

    DROP TABLE IF EXISTS `Game`;
    CREATE TABLE `Game` (
      `id` int(11) NOT NULL,
      `status` tinyint(4) default NULL,
      `date` int(11) default NULL,
      `name` varchar(255) default NULL,
      `organisation` varchar(255) default NULL,
      `certified` bit(1) default NULL,
      `comment` varchar(255) default NULL,
      `streamer` varchar(255) default NULL,
      `gameLength` int(11) default NULL,
      `numPackets` int(11) default NULL,
      `delay` int(11) default NULL,
      `lastSeed` int(11) default NULL,
      `path` varchar(255) default NULL,
      `checksum` varchar(255) default NULL,
      PRIMARY KEY  (`id`)
    ) ENGINE=MyISAM DEFAULT CHARSET=latin1;

    DROP TABLE IF EXISTS `Users`;
    CREATE TABLE `Users` (
      `id` int(11) NOT NULL,
      `name` varchar(255) default NULL,
      `password` varchar(255) default NULL,
      PRIMARY KEY  (`id`),
      UNIQUE KEY `name` (`name`)
    ) ENGINE=MyISAM DEFAULT CHARSET=latin1;

    INSERT INTO Users(id, name, password) VALUES (0, 'testuser', 'testpass');
  
At this point, the recorder should be able to connect to the server on port 10384 and log in with the login testuser:testpass.
