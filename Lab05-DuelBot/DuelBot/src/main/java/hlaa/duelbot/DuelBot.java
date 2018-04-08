package hlaa.duelbot;

import java.util.Collection;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import java.util.function.Predicate;


@AgentScoped
public class DuelBot extends UT2004BotModuleController {

	private long   lastLogicTime        = -1;
    private long   logicIterationNumber = 0;    

    /**
     * Here we can modify initializing command for our bot, e.g., sets its name or skin.
     *
     * @return instance of {@link Initialize}
     */
    @Override
    public Initialize getInitializeCommand() {  
    	return new Initialize().setName("DuelBot").setDesiredSkill(6);
    }

    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	bot.getLogger().getCategory("Yylex").setLevel(Level.OFF);
    }
    
    @Override
    public void botFirstSpawn(GameInfo gameInfo, ConfigChange config, InitedMessage init, Self self) {
        navigation.addStrongNavigationListener(new FlagListener<NavigationState>() {
			@Override
			public void flagChanged(NavigationState changedValue) {
				navigationStateChanged(changedValue);
			}
        });
    }
    
    private void navigationStateChanged(NavigationState changedValue) {
    	switch(changedValue) {
    	case TARGET_REACHED:
    		break;
		case PATH_COMPUTATION_FAILED:
			break;
		case STUCK:
			break;
		}
    }
    
    @Override
    public void beforeFirstLogic() {
    }

    private int getHealthAmount(Item itm){
        if(itm.getType().equals(UT2004ItemType.HEALTH_PACK)) return 25;
        else if(itm.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)) return 5;
        else if(itm.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)) return 100;

        return 0;
    }
        
    @Override
    public void logic() throws PogamutException {
    	if (lastLogicTime < 0) {
    		lastLogicTime = System.currentTimeMillis();
    		return;
    	}

    	log.info("---LOGIC: " + (++logicIterationNumber) + " / D=" + (System.currentTimeMillis() - lastLogicTime) + "ms ---");
    	lastLogicTime = System.currentTimeMillis();

        Collection<Item> items = MyCollections.getFiltered(this.items.getSpawnedItems().values(), (IFilter<Item>) arg0 -> arg0.getType().getCategory() == ItemType.Category.HEALTH);

        Item nearestItemLocation = DistanceUtils.getNearest(items, info.getLocation(), (DistanceUtils.IGetDistance<Item>) (item, myLocation) -> {
            return navMeshModule.getAStarPathPlanner().getDistance(item, myLocation);
        });


    	// FOLLOWS THE BOT'S LOGIC
    	
    }

    // ==============
    // EVENT HANDLERS
    // ==============
    
    @EventListener(eventClass=ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
    	if (info.getSelf() == null) return; // ignore the first equipment...
    }
    
    @EventListener(eventClass=BotDamaged.class)
    public void botDamaged(BotDamaged event) {
    }

    @Override
    public void botKilled(BotKilled event) {
        sayGlobal("I was KILLED!");
    }

    // =========
    // UTILITIES
    // =========
    
    private void sayGlobal(String msg) {
    	// Simple way to send msg into the UT2004 chat
    	body.getCommunication().sendGlobalTextMessage(msg);
    	// And user log as well
    	log.info(msg);
    }
    
    // ===========
    // MAIN METHOD
    // ===========
    
    public static void main(String args[]) throws PogamutException {
        new UT2004BotRunner(     // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                DuelBot.class,   // which UT2004BotController it should instantiate
                "DuelBot"        // what name the runner should be using
        ).setMain(true)          // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed
         .startAgents(1);        // tells the runner to start 2 agent
    }
}
