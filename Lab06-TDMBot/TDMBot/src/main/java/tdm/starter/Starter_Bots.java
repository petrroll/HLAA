package tdm.starter;

import java.util.logging.Level;

import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;
import tdm.TDMBot;

public class Starter_Bots {

	 /**
     * Main execute method of the program.
     * 
     * @param args
     * @throws PogamutException
     */
    public static void main(String args[]) throws PogamutException {
    	// Starts N agents of the same type at once
    	new UT2004BotRunner(TDMBot.class, "TDMBot").setMain(true).setLogLevel(Level.INFO).startAgents(4);
    }
	
}
