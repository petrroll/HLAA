package tdm.starter;

import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;

public class Starter_TCServer {
	
	public static void main(String[] args) {
		// Start TC (~ TeamCommunication) Server first... before you start your bots.
		UT2004TCServer tcServer = UT2004TCServer.startTCServer();
	}
	
}
