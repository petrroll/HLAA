package ut2004.exercises.e02;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.UT2004Skins;
import cz.cuni.amis.pogamut.ut2004.bot.command.AdvancedLocomotion;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;

import javax.vecmath.Vector3d;

/**
 * EXERCISE 02
 * -----------
 * 
 * Implement a WolfBot(s) that will be able to catch all the sheeps as fast as possible!
 * 
 * No shooting allowed, no speed reconfiguration allowed.
 * 
 * Just use {@link AdvancedLocomotion#moveTo(cz.cuni.amis.pogamut.base3d.worldview.object.ILocated)}, 
 * {@link AdvancedLocomotion#strafeTo(cz.cuni.amis.pogamut.base3d.worldview.object.ILocated, cz.cuni.amis.pogamut.base3d.worldview.object.ILocated)}
 * {@link AdvancedLocomotion#jump()} and {@link AdvancedLocomotion#dodge(cz.cuni.amis.pogamut.base3d.worldview.object.Location, boolean)} 
 * and alikes to move your bot!
 * 
 * To start scenario:
 * 1. blend directory ut2004 with you UT2004 installation
 * 2. start DM-TagMap using startGamebotsDMServer-DM-TagMap.bat
 * 3. start SheepBot
 * -- it will launch 12 agents (Sheeps) into the game
 * 4. start WolfBot
 * 5. one of your WolfBot has to say "start" to start the match or "restart" to re/start the match
 * 
 * Behavior tips:
 * 1. in this exercise, you can implement the communication using statics, i.e., both your Wolfs are running
 *    within the same JVM - make use of that - and watch out for race-conditions (synchronized(MUTEX){ ... } your critical stuff)
 * 2. first, you have to check that both your wolfs are kicking and you should issue "start" message
 * 3. do not start playing before that ;) ... check {@link Utils#gameRunning} whether the game is running
 * 4. you catch the sheep by bumping to it (getting near to it...)
 * 5. count how many sheeps are still alive (via implementing PlayerKilled listener correctly) to know when to restart the match!
 *    -- how fast can you take them out all?
 * 
 * 
 * @author Jakub Gemrot aka Jimmy aka Kefik
 */
@AgentScoped
public class WolfBot extends UT2004BotModuleController {
    
	private static AtomicInteger INSTANCE = new AtomicInteger(1);
	private static int aliveSheepies; // only wolfie 2 changes, both can read

    private static WolfBot[] wolfies = new WolfBot[2];

	private static Object MUTEX = new Object();
	
	private int instance = 0;
    private int logicIterationNumber;

    private Player currentTarget; // expect reference assignment is atomic
    private WolfBot buddyWolfie; // assume there's only one other wolf

    private boolean ready = false;

	/**
     * Here we can modify initializing command for our bot, e.g., sets its name or skin.
     *
     * @return instance of {@link Initialize}
     */
    @Override
    public Initialize getInitializeCommand() {  
    	instance = INSTANCE.getAndIncrement();
    	return new Initialize().setName("WolfBot-" + instance).setSkin(UT2004Skins.getSkin());
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
        wolfies[instance - 1] = this;
    }
    
    /**
     * Say something through the global channel + log it into the console...    
     * @param msg
     */
    private void sayGlobal(String msg) {
    	// Simple way to send msg into the UT2004 chat
    	body.getCommunication().sendGlobalTextMessage(msg);
    	// And user log as well
    	log.info(msg);
    }
    
    @EventListener(eventClass=GlobalChat.class)
    public void chatReceived(GlobalChat msg) {

        if (instance == 1) Utils.handleMessage(msg);
        if (instance == 2) {
            if (msg.getText().toLowerCase().contains("restart")) {
                aliveSheepies = 12;
            }
        }
    }
    
    /**
     * Some other player has been killed.
     * @param event
     */
    @EventListener(eventClass=PlayerKilled.class)
    public synchronized void playerKilled(PlayerKilled event) {
    	if(instance == 2){
    	    aliveSheepies -= 1;

    	    if(aliveSheepies <= 0) {
    	        sayGlobal("restart");
                aliveSheepies = 12;
            }
        }


    }
    
    /**
     * Main method called 4 times / second. Do your action-selection here.
     */
    @Override
    public void logic() throws PogamutException {
    	log.info("---LOGIC(" + instance + ":" + aliveSheepies+ "): " + (++logicIterationNumber) + "---");
    	if(!ready) {
            tryToReadyUpAndSetUpConnectionToOtherWolf();
            return;
        }

        if(!isGameRunningStartIfAllConnected()){
            return;
        }

        Player closestSheep = getClosestSheep();
        Player viableSheepChasedByBuddy = (instance == 1) ? getViableBuddysTarget(closestSheep) : null;

        currentTarget = (viableSheepChasedByBuddy != null) ? viableSheepChasedByBuddy : closestSheep;

        if(currentTarget == null) {
            move.turnHorizontal(90);
            return;
        }


        { // catch target sheep

            log.info("---Target(" + instance +"): " + currentTarget.getId());
            double distanceToTarget = info.getDistance(currentTarget.getLocation());

            boolean doubleJump = distanceToTarget < 300;
            double predictionScale = (distanceToTarget < 300)
                    ? (distanceToTarget < 150 ? 0.2 : 0.5)
                    : 1;

            // when both chasing the same sheep -> move to further future position with  instance1
            predictionScale = (instance == 1 && this.currentTarget == buddyWolfie.currentTarget) ? 1 : predictionScale;

            Location futureLocation = currentTarget.getLocation().add(currentTarget.getVelocity().scale(predictionScale));
            Location directionToFutureLocation = futureLocation.sub(info.getLocation());


            move.moveTo(futureLocation);
            move.dodge(directionToFutureLocation, doubleJump);
        }
    }

    private void tryToReadyUpAndSetUpConnectionToOtherWolf() {
        buddyWolfie = wolfies[instance % 2]; // can be null if not connected yet
        ready = (buddyWolfie != null);
    }

    private Player getViableBuddysTarget(Player closestSheep) {
        Player sheepChasedByBuddy = buddyWolfie.currentTarget;

        if (sheepChasedByBuddy == null || sheepChasedByBuddy.getLocation() == null) {
            return null;
        }

        closestSheep = closestSheep != null ? closestSheep : sheepChasedByBuddy;

        Vector3d meToBuddysSheepVector = sheepChasedByBuddy.getLocation().sub(info.getLocation()).getNormalized().asVector3d();
        double directionVectorSimilarity = sheepChasedByBuddy.getVelocity().normalize().asVector3d().dot(meToBuddysSheepVector);
        double distanceRatio = info.getDistance(sheepChasedByBuddy) / info.getDistance(closestSheep);

        // runs directly towards me & not extremely far
        if (directionVectorSimilarity < -0.8 && (closestSheep == null || distanceRatio < 5)){
            log.info("---BuddySheep(" + instance +"): " + sheepChasedByBuddy.getId() + " directly towards me.");
            return sheepChasedByBuddy;
        }

        // doesn't run directly towards me but is close enough
        if (directionVectorSimilarity < 0 && (closestSheep == null || distanceRatio < 2)) {
            log.info("---BuddySheep(" + instance +"): " + sheepChasedByBuddy.getId() + " reasonably towards me & close.");
            return sheepChasedByBuddy;
        }

        log.info("---NoBuddySheep(" + instance +"): " + sheepChasedByBuddy.getId() + ":" + directionVectorSimilarity + ":" + distanceRatio);
        return null;
    }

    private Player getClosestSheep() {
        Collection<Player> visibleSheep = players.getVisibleEnemies().values().stream().filter(p -> Utils.isSheep(p) && p.getLocation() != null).collect(Collectors.toList());
        return DistanceUtils.getNearest(visibleSheep, info.getLocation());
    }

    private boolean isGameRunningStartIfAllConnected() {
        if (Utils.gameRunning) {
            return true;
        }

        Collection<Player> bots = players.getPlayers().values();
        if (bots.size() == 14 && this.ready && this.buddyWolfie.ready) {

            if (this.instance == 2) {
                sayGlobal("start");
                aliveSheepies = 12;
            }
        }

        return  false;

    }

    /**
     * This method is called when the bot is started either from IDE or from command line.
     *
     * @param args
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String args[]) throws PogamutException {
        new UT2004BotRunner(      // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                WolfBot.class,  // which UT2004BotController it should instantiate
                "WolfBot"       // what name the runner should be using
        ).setMain(true)           // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed
         .startAgents(2);         // tells the runner to start 1 agent
    }
}
