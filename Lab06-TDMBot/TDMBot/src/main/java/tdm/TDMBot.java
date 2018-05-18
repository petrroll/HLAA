package tdm;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFMapView;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.impl.PrecomputedPathFuture;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Rotation;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.UT2004Skins;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.levelGeometry.RayCastResult;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.NavMeshClearanceComputer.ClearanceLimit;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.pathfollowing.NavMeshNavigation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.HearNoise;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.IncomingProjectile;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ItemPickedUp;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.TeamScore;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.ExceptionToString;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import math.geom2d.Vector2D;
import tdm.tc.TDMCommItems;
import tdm.tc.TDMCommObjectUpdates;
import tdm.tc.msgs.TCItemPursuing;

import static cz.cuni.amis.utils.collections.MyCollections.getFiltered;

/**
 * TDM BOT TEMPLATE CLASS
 * Version: 0.0.1
 */
@AgentScoped
public class TDMBot extends UT2004BotTCController<UT2004Bot> {

	private static Object CLASS_MUTEX = new Object();
	
	/**
	 * TRUE => draws navmesh and terminates
	 */
	public static final boolean DRAW_NAVMESH = false;
	private static boolean navmeshDrawn = false;
	
	/**
	 * TRUE => rebinds NAVMESH+NAVIGATION GRAPH; useful when you add new map tweak into {@link MapTweaks}.
	 */
	public static final boolean UPDATE_NAVMESH = false;
	
	/**
	 * Whether to draw navigation path; works only if you are running 1 bot...
	 */
	public static final boolean DRAW_NAVIGATION_PATH = false;
	private boolean navigationPathDrawn = false;
	
	/**
	 * If true, all bots will enter RED team... 
	 */
	public static final boolean START_BOTS_IN_SINGLE_TEAM = false;
		
	/**
	 * How many bots we have started so far; used to split bots into teams.
	 */
	private static AtomicInteger BOT_COUNT = new AtomicInteger(0);
	/**
	 * How many bots have entered RED team.
	 */
	private static AtomicInteger BOT_COUNT_RED_TEAM = new AtomicInteger(0);
	/**
	 * How many bots have entered BLUE team.
	 */
	private static AtomicInteger BOT_COUNT_BLUE_TEAM = new AtomicInteger(0);
	
	/**
	 * 0-based; note that during the tournament all your bots will have botInstance == 0!
	 */
	private int botInstance = 0;
	
	/**
	 * 0-based; note that during the tournament all your bots will have botTeamInstance == 0!
	 */
	private int botTeamInstance = 0;
	
	private TDMCommItems<TDMBot> commItems;
	private TDMCommObjectUpdates<TDMBot> commObjectUpdates;


    // =============
    // BOT parameters and variables
    // =============
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

    Map<UnrealId, PursueInfo> someOneIsPursuing = new HashMap<UnrealId, PursueInfo>();

    private final int RUN_TO_COVER_HEALTH_THRESHOLD = 25;
    private final int PREFER_HEALTH_WHEN_PICKUP_STUFF_HEALTH_THRESHOLD = 75;

    private class PursueInfo{
        private double distance;
        private Heatup heatup;

        public PursueInfo(double dist, Heatup heat){
            distance = dist;
            heatup = heat;
        }

        public double getDistance() {
            return distance;
        }

        public Heatup getHeatup() {
            return heatup;
        }
    }

    // =============
    // BOT LIFECYCLE
    // =============

    /**
     * Bot's preparation - called before the bot is connected to GB2004 and launched into UT2004.
     */
    @Override
    public void prepareBot(UT2004Bot bot) {       	
        // DEFINE WEAPON PREFERENCES
        initWeaponPreferences();
        
        // INITIALIZATION OF COMM MODULES
        commItems = new TDMCommItems<TDMBot>(this);
        commObjectUpdates = new TDMCommObjectUpdates<TDMBot>(this);
    }
    
    /**
     * This is a place where you should use map tweaks, i.e., patch original Navigation Graph that comes from UT2004.
     */
    @Override
    public void mapInfoObtained() {
    	// See {@link MapTweaks} for details; add tweaks in there if required.
    	MapTweaks.tweak(navBuilder);    	
    	if (botInstance == 0) navMeshModule.setReloadNavMesh(UPDATE_NAVMESH);    	
    }
    
    /**
     * Define your weapon preferences here (if you are going to use weaponPrefs).
     * 
     * For more info, see slides (page 8): http://diana.ms.mff.cuni.cz/pogamut_files/lectures/2010-2011/Pogamut3_Lecture_03.pdf
     */
    private void initWeaponPreferences() {
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
    public Initialize getInitializeCommand() {
    	// IT IS FORBIDDEN BY COMPETITION RULES TO CHANGE DESIRED SKILL TO DIFFERENT NUMBER THAN 6
    	// IT IS FORBIDDEN BY COMPETITION RULES TO ALTER ANYTHING EXCEPT NAME & SKIN VIA INITIALIZE COMMAND
		// Jakub Gemrot -> targetName = "JakubGemrot"
		String targetName = "MyBot";
		botInstance = BOT_COUNT.getAndIncrement();
		
		int targetTeam = AgentInfo.TEAM_RED;
		if (!START_BOTS_IN_SINGLE_TEAM) {
			targetTeam = botInstance % 2 == 0 ? AgentInfo.TEAM_RED : AgentInfo.TEAM_BLUE;
		}
		switch (targetTeam) {
		case AgentInfo.TEAM_RED: 
			botTeamInstance = BOT_COUNT_RED_TEAM.getAndIncrement();  
			targetName += "-RED-" + botTeamInstance; 
			break;
		case AgentInfo.TEAM_BLUE: 
			botTeamInstance = BOT_COUNT_BLUE_TEAM.getAndIncrement(); 
			targetName += "-BLUE-" + botTeamInstance;
			break;
		}		
        return new Initialize().setName(targetName).setSkin(targetTeam == AgentInfo.TEAM_RED ? UT2004Skins.SKINS[0] : UT2004Skins.SKINS[UT2004Skins.SKINS.length-1]).setTeam(targetTeam).setDesiredSkill(6);
    }

    /**
     * Bot has been initialized inside GameBots2004 (Unreal Tournament 2004) and is about to enter the play
     * (it does not have the body materialized yet).
     *  
     * @param gameInfo
     * @param currentConfig
     * @param init
     */
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	// INITIALIZE TABOO SETS, if you have them, HERE
    }

    // ==========================
    // EVENT LISTENERS / HANDLERS
    // ==========================
	
    /**
     * {@link PlayerDamaged} listener that senses that "some other bot was hurt".
     *
     * @param event
     */
    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
    	UnrealId botHurtId = event.getId();
    	if (botHurtId == null) return;
    	
    	int damage = event.getDamage();
    	Player botHurt = (Player)world.get(botHurtId); // MAY BE NULL!
    	
    	log.info("OTHER HURT: " + damage + " DMG to " + botHurtId.getStringId() + " [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "]");
    }
    
    /**
     * {@link BotDamaged} listener that senses that "I was hurt".
     *
     * @param event
     */
    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
    	int damage = event.getDamage();

        shotAtWith = event.getWeaponName();
        shotAt.heat();

    	if (event.getInstigator() == null) {
    		log.info("HURT: " + damage + " DMG done to ME [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "] by UNKNOWN");
    	} else {
    		UnrealId whoCauseDmgId = event.getInstigator();
    		Player player = (Player) world.get(whoCauseDmgId); // MAY BE NULL!
    		log.info("HURT: " + damage + " DMG done to ME [type=" + event.getDamageType() + ", weapon=" + event.getWeaponName() + "] by " + whoCauseDmgId.getStringId());
    	}
    }
    
    /**
     * {@link PlayerKilled} listener that senses that "some other bot has died".
     *
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
    	UnrealId botDiedId = event.getId();
    	if (botDiedId == null) return;
    	
    	Player botDied = (Player) world.get(botDiedId);
    	
    	if (event.getKiller() == null) {
    		log.info("OTHER DIED: " + botDiedId.getStringId() + ", UNKNOWN killer");
    	} else {
    		UnrealId killerId = event.getKiller();
    		if (killerId.equals(info.getId())) {
    			log.info("OTHER KILLED: " + botDiedId.getStringId() + " by ME");
    		} else {
    			Player killer = (Player) world.get(killerId);
    			if (botDiedId.equals(killerId)) {
    				log.info("OTHER WAS KILLED: " + botDiedId.getStringId() + " comitted suicide");
    			} else {
    				log.info("OTHER WAS KILLED: " + botDiedId.getStringId() + " by " + killerId.getStringId());
    			}
    		}
    	}
    }
    
    /**
     * {@link BotKilled} listener that senses that "your bot has died".
     */
	@Override
	public void botKilled(BotKilled event) {
		if (event.getKiller() == null) {
			log.info("DEAD");
		} else {
			UnrealId killerId = event.getKiller();
			Player killer = (Player) world.get(killerId);
			log.info("KILLED by" + killerId.getStringId());
		} 
		reset();
	}
	
    /**
     * {@link HearNoise} listener that senses that "some noise was heard by the bot".
     *
     * @param event
     */
    @EventListener(eventClass = HearNoise.class)
    public void hearNoise(HearNoise event) {
    	double noiseDistance = event.getDistance();   // 100 ~ 1 meter
    	Rotation faceRotation = event.getRotation();  // rotate bot to this if you want to face the location of the noise
    	log.info("HEAR NOISE: distance = " + noiseDistance);
    }
    
    /**
     * {@link ItemPickedUp} listener that senses that "your bot has picked up some item".
     * 
     * See sources for {@link ItemType} for details about item types / categories / groups.
     *
     * @param event
     */
    @EventListener(eventClass = ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
        if (info.getSelf() == null) return; // ignore the first equipment...

        ItemType itemType = event.getType();
    	ItemType.Group itemGroup = itemType.getGroup();
    	ItemType.Category itemCategory = itemType.getCategory();
    	Item item = items.getItem(event.getId());
    	log.info("PICKED " + itemCategory.name + ": " + itemType.getName() + " [group=" + itemGroup.getName() + "]");

        if (item == null) {return; }

        if (event.getType().getCategory() == ItemType.Category.WEAPON){
            currentLongRangeWeaponLevel = Math.max(getLongRangeWeaponScore(item), currentLongRangeWeaponLevel);
            currentShortRangeWeaponLevel = Math.max(getShortRangeWeaponScore(item), currentShortRangeWeaponLevel);
        }

    }
    
    /**
     * {@link IncomingProjectile} listener that senses that "some projectile has appeared OR moved OR disappeared".
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = IncomingProjectile.class, eventClass = WorldObjectUpdatedEvent.class)
    public void incomingProjectileUpdated(WorldObjectUpdatedEvent<IncomingProjectile> event) {
    	IncomingProjectile projectile = event.getObject();
    	// DO NOT SPAM... uncomment for debug
    	//log.info("PROJECTILE UPDATED: " + projectile);
    }
    
    /**
     * {@link Player} listener that senses that "some other bot has appeared OR moved OR disappeared"
     *
     * WARNING: this method will also be called during handshaking GB2004.
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = Player.class, eventClass = WorldObjectUpdatedEvent.class)
    public void playerUpdated(WorldObjectUpdatedEvent<Player> event) {
    	if (info.getLocation() == null) {
    		// HANDSHAKING GB2004
    		return;
    	}
    	Player player = event.getObject();    	
    	// DO NOT SPAM... uncomment for debug
    	//log.info("PLAYER UPDATED: " + player.getId().getStringId());
    }
        
    
    /**
     * {@link TeamScore} listener that senses changes within scoring.
     *
     * @param event
     */
    @ObjectClassEventListener(objectClass = TeamScore.class, eventClass = WorldObjectUpdatedEvent.class)
    public void teamScoreUpdated(WorldObjectUpdatedEvent<TeamScore> event) {
    	switch (event.getObject().getTeam()) {
    	case AgentInfo.TEAM_RED: 
    		log.info("RED TEAM SCORE UPDATED: " + event.getObject());
    		break;
    	case AgentInfo.TEAM_BLUE:
    		log.info("BLUE TEAM SCORE UPDATED: " + event.getObject());
    		break;
    	}
    }
    
    
    private long selfLastUpdateStartMillis = 0;
    private long selfTimeDelta = 0;
    
    /**
     * {@link Self} object has been updated. This update is received about every 50ms. You can use this update
     * to fine-time some of your behavior like "weapon switching". I.e. SELF is updated every 50ms while LOGIC is invoked every 250ms.
     * 
     * Note that during "SELF UPDATE" only information about your bot location/rotation ({@link Self}) is updated. All other visibilities 
     * remains the same as during last {@link #logic()}.
     * 
     * Note that new {@link NavMeshNavigation} is using SELF UPDATES to fine-control the bot's navigation.
     * 
     * @param event
     */
    @ObjectClassEventListener(objectClass = Self.class, eventClass = WorldObjectUpdatedEvent.class)
    public void selfUpdated(WorldObjectUpdatedEvent<Self> event) {
    	if (lastLogicStartMillis == 0) {
    		// IGNORE ... logic has not been executed yet...
    		return;
    	}
    	if (selfLastUpdateStartMillis == 0) {
    		selfLastUpdateStartMillis = System.currentTimeMillis();
    		return;
    	}
    	long selfUpdateStartMillis = System.currentTimeMillis(); 
    	selfTimeDelta = selfUpdateStartMillis  - selfLastUpdateStartMillis;
    	selfLastUpdateStartMillis = selfUpdateStartMillis;
    	log.info("---[ SELF UPDATE | D: " + (selfTimeDelta) + "ms ]---");
    	
    	try {
    		
    		// YOUR CODE HERE
    		
    	} catch (Exception e) {
    		// MAKE SURE THAT YOUR BOT WON'T FAIL!
    		log.info(ExceptionToString.process(e));
    	} finally {
    		//log.info("---[ SELF UPDATE END ]---");
    	}
    	
    }

    /**
     * Someone else picked an item!
     * @param event
     */
    @EventListener(eventClass = TCItemPursuing.class)
    public void tcItemBeingPursued(TCItemPursuing event) {
        Heatup htp = new Heatup((long)(event.getDistance() * 10));
        htp.heat();
        this.someOneIsPursuing.put(event.getWhat(), new PursueInfo(event.getDistance(), htp));
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

    // ==============
    // MAIN BOT LOGIC
    // ==============
    
    /**
     * Method that is executed only once before the first {@link TDMBot#logic()} 
     */
    @Override
    public void beforeFirstLogic() {
    	lastLogicStartMillis = System.currentTimeMillis();
    	if (DRAW_NAVMESH && botInstance == 0) {
    		boolean drawNavmesh = false;
    		synchronized(CLASS_MUTEX) {
    			if (!navmeshDrawn) {
    				drawNavmesh = true;
    				navmeshDrawn = true;
    			}
    		}
    		if (drawNavmesh) {
    			log.warning("!!! DRAWING NAVMESH !!!");
    			navMeshModule.getNavMeshDraw().draw(true, true);
    			navmeshDrawn  = true;
    			log.warning("NavMesh drawn, waiting a bit to finish the drawing...");
    		}    		
    	}
    }
    
    private long lastLogicStartMillis = 0;
    private long lastLogicEndMillis = 0;
    private long timeDelta = 0;
    
    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second.
     * 
     * This is a typical place from where you start coding your bot. Even though bot
     * can be completely EVENT-DRIVEN, the reactive aproach via "ticking" logic()
     * method is more simple / straight-forward.
     */
    @Override
    public void logic() {
    	long logicStartTime = System.currentTimeMillis();
    	try {
	    	// LOG VARIOUS INTERESTING VALUES
    		logLogicStart();
	    	logMind();
	    	
	    	// UPDATE TEAM COMM
	    	commItems.update();
	    	commObjectUpdates.update();
	    	
	    	// MAIN BOT LOGIC
	    	botLogic();
	    	
    	} catch (Exception e) {
    		// MAKE SURE THAT YOUR BOT WON'T FAIL!
    		log.info(ExceptionToString.process(e));
    		// At this point, it is a good idea to reset all state variables you have...
    		reset();
    	} finally {
    		// MAKE SURE THAT YOUR LOGIC DOES NOT TAKE MORE THAN 250 MS (Honestly, we have never seen anybody reaching even 150 ms per logic cycle...)
    		// Note that it is perfectly OK, for instance, to count all path-distances between you and all possible pickup-points / items in the game
    		// sort it and do some inference based on that.
    		long timeSpentInLogic = System.currentTimeMillis() - logicStartTime;
    		log.info("Logic time:         " + timeSpentInLogic + " ms");
    		if (timeSpentInLogic >= 245) {
    			log.warning("!!! LOGIC TOO DEMANDING !!!");
    		}
    		log.info("===[ LOGIC END ]===");
    		lastLogicEndMillis = System.currentTimeMillis();
    	}    	
    }
    
    public void botLogic() {
    	// RANDOM NAVIGATION
    	if (navigation.isNavigating()) {
    		if (DRAW_NAVIGATION_PATH) {
    			if (!navigationPathDrawn) {
    				drawNavigationPath(true);
    				navigationPathDrawn = true;
    			}
    		}
    		return;
    	}

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
    
    public void reset() {
    	navigationPathDrawn = false;

        currentLongRangeWeaponLevel = 1;
        currentShortRangeWeaponLevel = 1;

        fleeFromEnemy.clear();
        pursueEnemy.clear();
        pursuedEnemy = null;
        cdLightningShoot.clear();
        shotAt.clear();
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
            pursueToPickup(bestPickup);
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
                pursueToPickup(bestWeapon);
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
            pursueToPickup(closestHealth);
            return true;
        }
        return false;
    }


    private Item getBestInterestingItem(IFilter<Item> interestingFilter, ToDoubleBiFunction<Item, Double> scoreItem) {
        Collection<Item> items = getFiltered(this.items.getSpawnedItems().values(), interestingFilter);

        Item nearestItemLocation = DistanceUtils.getNearest(items, info.getLocation(), (DistanceUtils.IGetDistance<Item>) (item, myLocation) -> {
            double myDistance = Double.MAX_VALUE;
            try{ // AStarPlanner sometimes throws NullPointerException for no reason.
                myDistance = navMeshModule.getAStarPathPlanner().getDistance(item.getLocation(), myLocation);
            } catch (NullPointerException ex) {}

            // If someone is pursuing and I'm further make my distance 10-times further to discourage pursuing
            if (someOneIsPursuing.containsKey(item.getId())){
                PursueInfo nfo = someOneIsPursuing.get(item.getId());
                if (nfo.getHeatup().isHot()){
                    if(nfo.distance < myDistance){
                        myDistance *= 10;
                    }
                }
                else {
                    someOneIsPursuing.remove(item.getId());
                }
            }

            return scoreItem.applyAsDouble(item, myDistance);
        });

        return nearestItemLocation;
    }

    private void pursueToPickup(Item item){

        // I know I'm recomputing a value I should be able to have here -> temporal solution
        double distanceToCurrentlyPursued = navMeshModule.getAStarPathPlanner().getDistance(info.getLocation(), item);
        this.tcClient.sendToTeamOthers(new TCItemPursuing(info.getId(), item.getId(), distanceToCurrentlyPursued));

        navigation.navigate(item);

    }
    
    // ===========
    // MIND LOGGER
    // ===========
    
    /**
     * It is good to log that the logic has started so you can then retrospectively check the batches.
     */
    public void logLogicStart() {
    	long logicStartTime = System.currentTimeMillis();
    	timeDelta = logicStartTime - lastLogicStartMillis;
    	log.info("===[ LOGIC ITERATION | Delta: " + (timeDelta) + "ms | Since last: " + (logicStartTime - lastLogicEndMillis) + "ms]===");    		
    	lastLogicStartMillis = logicStartTime;    	
    }
    
    /**
     * It is good in-general to periodically log anything that relates to your's {@link TDMBot#logic()} decision making.
     * 
     * You might consider exporting these values to some custom Swing window (you crete for yourself) that will be more readable.
     */
    public void logMind() {
    	log.info("My health/armor:   " + info.getHealth() + " / " + info.getArmor() + " (low:" + info.getLowArmor() + " / high:" + info.getHighArmor() + ")");
    	log.info("My weapon:         " + weaponry.getCurrentWeapon());
    }
    
    // =====================================
    // UT2004 DEATH-MATCH INTERESTING GETTERS
    // ======================================
    
    /**
     * Returns path-nearest {@link NavPoint} that is covered from 'enemy'. Uses {@link UT2004BotModuleController#getVisibility()}.
     * @param enemy
     * @return
     */
    public NavPoint getNearestCoverPoint(Player enemy) {
    	if (!visibility.isInitialized()) {
    		log.warning("VISIBILITY NOT INITIALIZED: returning random navpoint");    		
    		return MyCollections.getRandom(navPoints.getNavPoints().values());
    	}
    	List<NavPoint> coverPoints = new ArrayList<NavPoint>(visibility.getCoverNavPointsFrom(enemy.getLocation()));
    	return fwMap.getNearestNavPoint(coverPoints, info.getNearestNavPoint());
    }
    
    /**
     * Returns whether 'item' is possibly spawned (to your current knowledge).
     * @param item
     * @return
     */
    public boolean isPossiblySpawned(Item item) {
    	return items.isPickupSpawned(item);
    }
    
    /**
     * Returns whether you can actually pick this 'item', based on "isSpawned" and "isPickable" in your current state and knowledge.
     */
    public boolean isCurrentlyPickable(Item item) {
    	return isPossiblySpawned(item) && items.isPickable(item);
    }
        
    // ==========
    // RAYCASTING
    // ==========
    
    /**
     * Performs a client-side raycast against UT2004 map geometry.
     * 
     * It is not sensible to perform more than 1000 raycasts per logic() per bot.
     *  
     * NOTE THAT IN ORDER TO USE THIS, you have to rename "map_" folder into "map" ... so it would load the level geometry.
     * Note that loading a level geometry up takes quite a lot of time (>60MB large BSP tree...). 
     *  
     * @param from
     * @param to
     * @return
     */
    public RayCastResult raycast(ILocated from, ILocated to) {
    	if (!levelGeometryModule.isInitialized()) {
    		throw new RuntimeException("Level Geometry not initialized! Cannot RAYCAST!");
    	}
    	return levelGeometryModule.getLevelGeometry().rayCast(from.getLocation(), to.getLocation());
    }
    
    /**
     * Performs a client-side raycast against NavMesh in 'direction'. Returns distance of the edge in given 'direction' sending the ray 'from'.
     * @param from
     * @param direction
     * @return
     */
    public double raycastNavMesh(ILocated from, Vector2D direction) {
    	if (!navMeshModule.isInitialized()) {
    		log.severe("NavMesh not initialized! Cannot RAYCAST-NAVMESH!");
    		return 0;
    	}
    	ClearanceLimit limit = navMeshModule.getClearanceComputer().findEdge(from.getLocation(), direction);
    	if (limit == null) return Double.POSITIVE_INFINITY;
    	return from.getLocation().getDistance(limit.getLocation());
    }
    
    // =======
    // DRAWING
    // =======
    
    public void drawNavigationPath(boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	List<ILocated> path = navigation.getCurrentPathCopy();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(path.get(i-1), path.get(i));
    	}
    }
    
    public void drawPath(IPathFuture<? extends ILocated> pathFuture, boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	List<? extends ILocated> path = pathFuture.get();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(path.get(i-1), path.get(i));
    	}
    }
    
    public void drawPath(IPathFuture<? extends ILocated> pathFuture, Color color, boolean clearAll) {
    	if (clearAll) {
    		draw.clearAll();
    	}
    	if (color == null) color = Color.WHITE;
    	List<? extends ILocated> path = pathFuture.get();
    	for (int i = 1; i < path.size(); ++i) {
    		draw.drawLine(color, path.get(i-1), path.get(i));
    	}
    }
    
    // =====
    // AStar
    // =====
    
    private NavPoint lastAStarTarget = null;
    
    public boolean navigateAStarPath(NavPoint targetNavPoint) {
        if (lastAStarTarget == targetNavPoint) {
            if (navigation.isNavigating()) return true;
        }
        PrecomputedPathFuture<ILocated> path = getAStarPath(targetNavPoint);
        if (path == null) {
            navigation.stopNavigation();
            return false;
        }
        lastAStarTarget = targetNavPoint;
        navigation.navigate(path);
        return true;
    }
    
    private IPFMapView<NavPoint> mapView = new IPFMapView<NavPoint>() {

        @Override
        public Collection<NavPoint> getExtraNeighbors(NavPoint node, Collection<NavPoint> mapNeighbors) {
            return null;
        }

        @Override
        public int getNodeExtraCost(NavPoint node, int mapCost) {
            return 0;
        }

        @Override
        public int getArcExtraCost(NavPoint nodeFrom, NavPoint nodeTo, int mapCost) {
            return 0;
        }

        @Override
        public boolean isNodeOpened(NavPoint node) {
            return true;
        }

        @Override
        public boolean isArcOpened(NavPoint nodeFrom, NavPoint nodeTo) {
            return true;
        }
    };
    
    private PrecomputedPathFuture<ILocated> getAStarPath(NavPoint targetNavPoint) {
        NavPoint startNavPoint = info.getNearestNavPoint();
        AStarResult<NavPoint> result = aStar.findPath(startNavPoint, targetNavPoint, mapView);
        if (result == null || !result.isSuccess()) return null;
        PrecomputedPathFuture path = new PrecomputedPathFuture(startNavPoint, targetNavPoint, result.getPath());
        return path;
    }
    
    // ===========
    // MAIN METHOD
    // ===========
    
    /**
     * Main execute method of the program.
     * 
     * @param args
     * @throws PogamutException
     */
    public static void main(String args[]) throws PogamutException {
    	// Starts N agents of the same type at once
    	// WHEN YOU WILL BE SUBMITTING YOUR CODE, MAKE SURE THAT YOU RESET NUMBER OF STARTED AGENTS TO '1' !!!
    	// => during the development, please use {@link Starter_Bots} instead to ensure you will leave "1" in here
    	new UT2004BotRunner(TDMBot.class, "TDMBot").setMain(true).setLogLevel(Level.INFO).startAgent();
    }
    
}
