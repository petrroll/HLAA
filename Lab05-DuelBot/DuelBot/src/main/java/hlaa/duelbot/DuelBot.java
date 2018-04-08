package hlaa.duelbot;

import java.util.Collection;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;


@AgentScoped
public class DuelBot extends UT2004BotModuleController {

	private long   lastLogicTime        = -1;
    private long   logicIterationNumber = 0;

    private int currentLongRangeWeaponLevel = 1;
    private int currentShortRangeWeaponLevel = 1;

    Cooldown cdLightningShoot = new Cooldown(3000);

    Heatup pursueEnemy = new Heatup(7000);
    Player pursuedEnemy = null;
    Heatup fleeFromEnemy = new Heatup(1500);

    Heatup shotAt = new Heatup(500);
    String shotAtWith = "";


    private final int RUN_TO_COVER_HEALTH_THRESHOLD = 25;
    private final int PREFER_HEALTH_WHEN_PICKUP_STUFF_HEALTH_THRESHOLD = 75;


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
        // FIRST we DEFINE GENERAL WEAPON PREFERENCES
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);

        weaponPrefs.newPrefsRange(80)
                .add(UT2004ItemType.SHIELD_GUN, true);

        weaponPrefs.newPrefsRange(1000)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.ASSAULT_RIFLE, true);

        weaponPrefs.newPrefsRange(4000)
                .add(UT2004ItemType.SHOCK_RIFLE, true)
                .add(UT2004ItemType.MINIGUN, false);

        weaponPrefs.newPrefsRange(100000)
                .add(UT2004ItemType.SNIPER_RIFLE, true)
                .add(UT2004ItemType.LIGHTNING_GUN, true)
                .add(UT2004ItemType.SHOCK_RIFLE, true);
    }


        
    @Override
    public void logic() throws PogamutException {
    	if (lastLogicTime < 0) {
    		lastLogicTime = System.currentTimeMillis();
    		return;
    	}

    	log.info("---LOGIC: " + (++logicIterationNumber) + " / D=" + (System.currentTimeMillis() - lastLogicTime) + "ms ---");
    	lastLogicTime = System.currentTimeMillis();

        //
        // Visible enemy -> remember it
        // -> if seriously injured -> flee to cover & get health-pack (after some time in cover).
        // -> decide whether to fight based on current equipment
        //    |- run towards the enemy & pursue him for some time even if lost sight
        //    |- run to the cover
        // No enemy lately -> pickup stuff
        // -> if injured prefer health-packs
        // -> pickup a good weapon for long and short range
        // -> pick pickups such as armor & additional health
        //

        if (tryEngageVisibleEnemy()) return;
        if (tryToStockpileOnStuff()) return;

        wanderRandomly();

    }

    private boolean tryEngageVisibleEnemy() {
        tryToSenseAndUpdateEnemy();
        if (pursueEnemy.isCool()) return false;

        if (runToCoverIfSeriouslyInjured()) return true;

        log.info("Pursuing enemy:" + pursuedEnemy);

        if(doIWantToTakeAFight()){
            navigation.navigate(pursuedEnemy);
        } else {
            fleeFromEnemy();
        }


        if (pursuedEnemy.isVisible()) {
            shootEnemy(pursuedEnemy);
        }

        return true;
    }

    private boolean doIWantToTakeAFight() {
        boolean noEquipmentNotCompletelyHealthy = info.getHealth() < 80 && currentLongRangeWeaponLevel <= 3 && currentShortRangeWeaponLevel <= 3;
        boolean noEquipmentTheyShootingBack = currentLongRangeWeaponLevel <= 3 && currentShortRangeWeaponLevel <= 3 && shotAt.isHot() && shotAtWith.equals(UT2004ItemType.ASSAULT_RIFLE.getName());

        return noEquipmentTheyShootingBack || noEquipmentNotCompletelyHealthy;
    }

    private boolean runToCoverIfSeriouslyInjured() {
        if (info.getHealth() < RUN_TO_COVER_HEALTH_THRESHOLD) {
            if(pursuedEnemy.isVisible()) fleeFromEnemy.heat();

            if(fleeFromEnemy.isHot()){
                fleeFromEnemy();
                return true;
            } else {
                log.info("Going for health pack.");
                moveToBestHealthPack(true);
                return true;
            }
        }
        return false;
    }

    private void fleeFromEnemy() {
        NavPoint nearestCover = visibility.getNearestCoverNavPointFrom(pursuedEnemy);
        log.info("Covering:" + nearestCover);
        navigation.navigate(nearestCover);
    }

    private boolean tryToSenseAndUpdateEnemy() {
        Player veryRecentEnemy = players.getNearestEnemy(300);
        if(veryRecentEnemy == null) return false;

        pursuedEnemy = veryRecentEnemy;
        pursueEnemy.heat();
        return true;
    }

    private void shootEnemy(Player veryRecentEnemy) {
        if (cdLightningShoot.tryUse()) { shoot.shoot(weaponPrefs, veryRecentEnemy); cdLightningShoot.use(); }
        else { shoot.shoot(weaponPrefs, veryRecentEnemy, UT2004ItemType.LIGHTNING_GUN); }

        shoot.shoot(weaponPrefs, veryRecentEnemy);
    }

    private void wanderRandomly() {
        if(!navigation.isNavigating()){
            navigation.navigate(navPoints.getRandomNavPoint());
        }
    }

    private boolean tryToStockpileOnStuff() {
        if (findHealthPackIfModeratelyInjured()) return true;

        if (findBetterWeapons()) return true;
        if (findGoodPickup()) return true;

        return false;
    }

    private boolean findGoodPickup() {
        Item bestPickup = getBestInterestingItem(
                (IFilter<Item>) arg0 -> arg0.getType().getCategory() != ItemType.Category.WEAPON,
                (ToDoubleBiFunction<Item, Double>) (item, dist) -> dist / scoreGeneralPickup(item));

        if (bestPickup != null){
            log.info("Moving for pickup:" + bestPickup);
            navigation.navigate(bestPickup);
            return  true;
        }
        return false;
    }

    private boolean findBetterWeapons() {

        if (findBetterWeapon(currentLongRangeWeaponLevel, 3, arg -> getLongRangeWeaponScore(arg))) return true;
        if (findBetterWeapon(currentShortRangeWeaponLevel, 3, arg -> getShortRangeWeaponScore(arg))) return true;

        return false;
    }

    private boolean findBetterWeapon(int currentLevel, int tresholdLevel, ToIntFunction<Item> weaponScore){
        if(currentLevel <= tresholdLevel) {

            Item bestWeapon = getBestInterestingItem(
                    (IFilter<Item>) arg0 -> arg0.getType().getCategory() == ItemType.Category.WEAPON,
                    (ToDoubleBiFunction<Item, Double>) (item, dist) -> dist / weaponScore.applyAsInt(item));

            if (bestWeapon != null && weaponScore.applyAsInt(bestWeapon) > currentLevel){
                navigation.navigate(bestWeapon);
                log.info("Moving for weapons:" + bestWeapon);
                return true;
            }
        }
        return false;
    }


    private boolean findHealthPackIfModeratelyInjured() {
        if (info.getHealth() < PREFER_HEALTH_WHEN_PICKUP_STUFF_HEALTH_THRESHOLD) {
            return moveToBestHealthPack();
        }
        return  false;
    }

    private boolean moveToBestHealthPack(){ return moveToBestHealthPack(false); }
    private boolean moveToBestHealthPack(boolean moveInCoverFromLastSeenEnemy) {
        int missingHealth = Math.max(100 - info.getHealth(), 0);
        Item closestHealth = getBestInterestingItem(
                (IFilter<Item>) arg0 -> arg0.getType().getCategory() == ItemType.Category.HEALTH,
                (ToDoubleBiFunction<Item, Double>) (item, dist) -> (moveInCoverFromLastSeenEnemy && visibility.isVisible(pursuedEnemy, item)) ? Double.MAX_VALUE : dist / Math.min(getHealthAmount(item), missingHealth));

        if (closestHealth != null){
            log.info("Moving for medpack:" + closestHealth);
            navigation.navigate(closestHealth);
            return true;
        }
        return false;
    }


    private Item getBestInterestingItem(IFilter<Item> interestingFilter, ToDoubleBiFunction<Item, Double> scoreItem) {
        Collection<Item> items = MyCollections.getFiltered(this.items.getSpawnedItems().values(), interestingFilter);

        Item nearestItemLocation = DistanceUtils.getNearest(items, info.getLocation(), (DistanceUtils.IGetDistance<Item>) (item, myLocation) -> {
            double myDistance = Double.MAX_VALUE;
            try{ // AStarPlanner sometimes throws NullPointerException for no reason.
                myDistance = navMeshModule.getAStarPathPlanner().getDistance(item.getLocation(), myLocation);
            } catch (NullPointerException ex) {}

            return scoreItem.applyAsDouble(item, myDistance);
        });

        return nearestItemLocation;
    }
    
    // ==============
    // EVENT HANDLERS
    // ==============
    
    @EventListener(eventClass=ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
    	if (info.getSelf() == null) return; // ignore the first equipment...
        Item item = items.getItem(event.getId());
        if (item == null) {return; }

        if (event.getType().getCategory() == ItemType.Category.WEAPON){
            currentLongRangeWeaponLevel = Math.max(getLongRangeWeaponScore(item), currentLongRangeWeaponLevel);
            currentShortRangeWeaponLevel = Math.max(getShortRangeWeaponScore(item), currentShortRangeWeaponLevel);
        }
    }
    
    @EventListener(eventClass=BotDamaged.class)
    public void botDamaged(BotDamaged event) {
        shotAtWith = event.getWeaponName();
        shotAt.heat();
    }

    @Override
    public void botKilled(BotKilled event) {

        sayGlobal("I was KILLED!");

        currentLongRangeWeaponLevel = 1;
        currentShortRangeWeaponLevel = 1;

        fleeFromEnemy.clear();
        pursueEnemy.clear();
        pursuedEnemy = null;
        cdLightningShoot.clear();
        shotAt.clear();
    }


    // ========
    // CONSTANTS COUNTING
    // ========

    private int getHealthAmount(Item itm){
        if(itm.getType().equals(UT2004ItemType.HEALTH_PACK)) return 25;
        else if(itm.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)) return 5;
        else if(itm.getType().equals(UT2004ItemType.SUPER_HEALTH_PACK)) return 100;

        return -1; // Should never go here.

    }

    private int getShortRangeWeaponScore(Item itm){
        if (itm.getType().equals(UT2004ItemType.FLAK_CANNON)) {return 6;}
        else if (itm.getType().equals(UT2004ItemType.MINIGUN)) {return 4;}
        else if (itm.getType().equals(UT2004ItemType.ROCKET_LAUNCHER)) {return 4;}
        else if (itm.getType().equals(UT2004ItemType.LINK_GUN)) {return 4;}

        else if (itm.getType().getCategory() == ItemType.Category.WEAPON){

            if (!itm.getType().equals(UT2004ItemType.SNIPER_RIFLE)
                    && !itm.getType().equals(UT2004ItemType.SHIELD_GUN)) {return 2;}
            return 0;
        }

        return 0; // Should never go here.
    }

    private int scoreGeneralPickup(Item itm){
        if(itm.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)) {return Math.min(5, 200 - info.getHealth());}
        else if(itm.getType().equals(UT2004ItemType.SUPER_HEALTH_PACK)) {return Math.min(100, 200 - info.getHealth());}
        else if(itm.getType().equals(UT2004ItemType.SHIELD_PACK)) {return Math.min(40, 150 - info.getArmor());}
        else if(itm.getType().equals(UT2004ItemType.SUPER_SHIELD_PACK)) {return Math.min(80, 150 - info.getArmor());}
        else if(itm.getType().equals(UT2004ItemType.U_DAMAGE_PACK)) {return 150;}

        return 0;
    }

    private int getLongRangeWeaponScore(Item itm){
        if (itm == null)

        if (itm.getType().equals(UT2004ItemType.SNIPER_RIFLE)) {return 6;}
        else if (itm.getType().equals(UT2004ItemType.LIGHTNING_GUN)) {return 5;}
        else if (itm.getType().equals(UT2004ItemType.SHOCK_RIFLE)) {return 5;}
        else if (itm.getType().equals(UT2004ItemType.ROCKET_LAUNCHER)) {return 5;}
        else if (itm.getType().equals(UT2004ItemType.MINIGUN)) {return 4;}
        else if (itm.getType().getCategory() == ItemType.Category.WEAPON){

            if (!itm.getType().equals(UT2004ItemType.FLAK_CANNON)
                    && !itm.getType().equals(UT2004ItemType.BIO_RIFLE)) {return 2;}
            return 0;
        }

        return 0; // Should never go here.

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
