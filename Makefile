all:				Server.class Client.class ServerThread.class ServerController.class Peer.class ClientController.class Main.class

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

Main.class:     Main.java
				@javac Main.java

clean:
				@rm -rf *.class *~
