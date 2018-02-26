package ut2004.exercises.e01;

import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.UT2004Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.UT2004Skins;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.IUT2004Navigation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;
import ut2004.exercises.e01.checker.CheckerBot;

/**
 * EXERCISE 01
 * -----------
 * 
 * Implement a SearchBot that will be able to find another bot {@link CheckerBot} within the environment and chat with it.
 * 
 * Step:
 * 1. find the bot and approach him (get near him ... distance < 200)
 * 2. greet him by saying "Hello!"
 * 3. upon receiving reply "Hello, my friend!"
 * 4. answer "I'm not your friend."
 * 5. and fire a bit at CheckerBot (do not kill him, just a few bullets)
 * 6. then CheckerBot should tell you "COOL!"
 * 7. then CheckerBot respawns itself
 * 8. repeat 1-6 until CheckerBot replies with "EXERCISE FINISHED"
 * 
 * If you break the protocol, {@link CheckerBot} will respawn at another location saying "RESET".
 * 
 * @author Jakub Gemrot aka Jimmy aka Kefik
 */
@AgentScoped
public class SearchBot extends UT2004BotModuleController {

    private enum SearchBotStates {
        SEARCHING,
        GOING_TO_PLAYER,
        GREETING,
        WAITING_FOR_GREETING_RESPONSE,
        PREPARING_FOR_FIRE,
        FIRING,
        WAITING_FOR_FIRE_RESPONSE,
        WAITING_FOR_NEXT_ROUND,
    }
    private SearchBotStates currentState;
    private int logicIterationNumber;

	/**
     * Here we can modify initializing command for our bot, e.g., sets its name or skin.
     *
     * @return instance of {@link Initialize}
     */
    @Override
    public Initialize getInitializeCommand() {  
    	return new Initialize().setName("SearchBot").setSkin(UT2004Skins.getSkin());
    }

    /**
     * Bot is ready to be spawned into the game; configure last minute stuff in here
     *
     * @param gameInfo information about the game type
     * @param config information about configuration
     * @param init information about configuration
     */
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	// ignore any Yylex whining...
    	bot.getLogger().getCategory("Yylex").setLevel(Level.OFF);
    }
    
    /**
     * This method is called only once, right before actual logic() method is called for the first time.
     */
    @Override
    public void beforeFirstLogic() {
        currentState = SearchBotStates.SEARCHING;
    }
    
    /**
     * Say something through the global channel + log it into the console...    
     * @param msg
     */
    private void sayGlobal(String msg) {
    	// Simple way to send msg into the UT2004 chat
    	body.getCommunication().sendGlobalTextMessage(msg);
    	// And user log as well
    	log.info("MSG: " + msg);
    }
    
    @EventListener(eventClass=GlobalChat.class)
    public void chatReceived(GlobalChat msg) {
        String receivedText = msg.getText();

    	if (receivedText.toLowerCase().equals("reset")) {
    		currentState = SearchBotStates.SEARCHING;
    		return;
    	}

    	if(currentState == SearchBotStates.WAITING_FOR_GREETING_RESPONSE){
            if(receivedText.equals("Hello, my friend!")){
                currentState = SearchBotStates.PREPARING_FOR_FIRE;
                log.info("Switched to: " + currentState.name());

                return;
            }
        }
        else if(currentState == SearchBotStates.WAITING_FOR_FIRE_RESPONSE){
            if(receivedText.equals("COOL!")){

                currentState = SearchBotStates.WAITING_FOR_NEXT_ROUND;
                log.info("Switched to: " + currentState.name());

                navigation.navigate(navPoints.getRandomNavPoint());
                return;
            }
        }

    }
    
    /**
     * Some other player/bot has taken damage.
     * @param event
     */
    @EventListener(eventClass=PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {

        if(currentState == SearchBotStates.FIRING){

            shoot.stopShooting();
            currentState = SearchBotStates.WAITING_FOR_FIRE_RESPONSE;
            log.info("Switched to: " + currentState.name());

            return;
        }

    }

    /**
     * Main method called 4 times / second. Do your action-selection here.
     */
    @Override
    public void logic() throws PogamutException {
        log.info("---LOGIC: " + (++logicIterationNumber) + "---");
        Player nearestVisiblePlayer = players.getNearestVisiblePlayer();

        if (currentState == SearchBotStates.SEARCHING) {

            if (players.canSeePlayers()) {
                currentState = SearchBotStates.GOING_TO_PLAYER;
                log.info("Switched to: " + currentState.name());

                navigation.navigate(nearestVisiblePlayer);
            }

            if (!navigation.isNavigating()) {
                navigation.navigate(navPoints.getRandomNavPoint());
                return;
            }

        }

        if (currentState == SearchBotStates.GOING_TO_PLAYER) {

            if (nearestVisiblePlayer != null && info.getDistance(nearestVisiblePlayer) < 150) {
                currentState = SearchBotStates.GREETING;
                log.info("Switched to: " + currentState.name());

            } else if (!navigation.isNavigating()) {
                currentState = SearchBotStates.SEARCHING;
                log.info("Switched to: " + currentState.name());
                return;

            }
        }

        if (currentState == SearchBotStates.GREETING) {
            sayGlobal("Hello!");
            currentState = SearchBotStates.WAITING_FOR_GREETING_RESPONSE;
            log.info("Switched to: " + currentState.name());

            return;
        }

        if (currentState == SearchBotStates.PREPARING_FOR_FIRE) {
            sayGlobal("I'm not your friend.");
            weaponry.changeWeapon(UT2004ItemType.ASSAULT_RIFLE);

            currentState = SearchBotStates.FIRING;
            return; // This state and return is here just to create a delay between the response above and shooting. Without it the checkerBot fails to register the response sometimes.
        }

        if (currentState == SearchBotStates.FIRING) {
            if (nearestVisiblePlayer == null) {
                move.turnHorizontal(30);
            }

            shoot.shoot(nearestVisiblePlayer);
            return;
        }

        if (currentState == SearchBotStates.WAITING_FOR_NEXT_ROUND) {
            if (!navigation.isNavigating()) {
                currentState = SearchBotStates.SEARCHING;
                log.info("Switched to: " + currentState.name());

                return;
            }
        }
    }

    /**
     * This method is called when the bot is started either from IDE or from command line.
     *
     * @param args
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String args[]) throws PogamutException {
        new UT2004BotRunner(      // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                SearchBot.class,  // which UT2004BotController it should instantiate
                "SearchBot"       // what name the runner should be using
        ).setMain(true)           // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed
         .startAgents(1);         // tells the runner to start 1 agent
    }
}
