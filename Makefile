all:				Server.class Client.class ServerThread.class ServerController.class User.class

Server.class:		Server.java
				@javac Server.java

Client.class:		Client.java
				@javac Client.java


ServerThread.class:		ServerThread.java
				@javac ServerThread.java

ServerController.class:     ServerController.java
				@javac ServerController.java


Peer.class:     Peer.java
				@javac Peer.java

ClientController.class:     ClientController.java
				@javac ClientController.java

clean:
				@rm -rf *.class *~
