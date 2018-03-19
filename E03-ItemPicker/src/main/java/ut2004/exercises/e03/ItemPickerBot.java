package ut2004.exercises.e03;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils.IGetDistance;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.UT2004Skins;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GlobalChat;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ItemPickedUp;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import ut2004.exercises.e03.comm.TCItemPicked;
import ut2004.exercises.e03.comm.TCItemPursuing;

/**
 * EXERCISE 03
 * -----------
 *
 * Your task is to pick all interesting items.
 *
 * Interesting items are:
 * -- weapons
 * -- shields
 * -- armors
 *
 * Target maps, where to test your squad are:
 * -- DM-1on1-Albatross
 * -- DM-1on1-Roughinery-FPS
 * -- DM-Rankin-FE
 *
 * To start scenario:
 * 1. start either of startGamebotsTDMServer-DM-1on1-Albatross.bat, startGamebotsTDMServer-DM-1on1-Roughinery-FPS.bat, startGamebotsTDMServer-DM-Rankin-FE.bat
 * 2. start team communication view running {@link TCServerStarter#main(String[])}.
 * 3. start your squad
 * 4. use ItemPickerChecker methods to query the state of your run
 *
 * Behavior tips:
 * 1. be sure not to repick item you have already picked
 * 2. be sure not to repick item some other bot has already picked (use {@link #tcClient} for that)
 * 3. do not try to pick items you are unable to, check by {@link Items#isPickable(Item)}
 * 4. be sure not to start before {@link ItemPickerChecker#isRunning()}
 * 5. you may terminate your bot as soon as {@link ItemPickerChecker#isVictory()}.
 *
 * WATCH OUT!
 * 1. All your bots must be run from the same JVM, but they must not communicate via STATICs!
 *
 * @author Jakub Gemrot aka Jimmy aka Kefik
 */
@AgentScoped
public class ItemPickerBot extends UT2004BotTCController {

    private static AtomicInteger INSTANCE = new AtomicInteger(1);
    private static Object MUTEX = new Object();
    private int instance = 0;

    private int logicIterationNumber;
    private long lastLogicTime = -1;

    private Item currentlyPursuedItem = null;

    Set<UnrealId> pickedItems = new HashSet<UnrealId>();
    Set<UnrealId> someOneIsPursuing = new HashSet<UnrealId>();

    State currState = State.ChoosingItem;
    enum State {
        ChoosingItem,
        RunningToItem,
        RepickingItem,
    }

    /**
     * Here we can modify initializing command for our bot, e.g., sets its name or skin.
     *
     * @return instance of {@link Initialize}
     */
    @Override
    public Initialize getInitializeCommand() {
        instance = INSTANCE.getAndIncrement();
        return new Initialize().setName("PickerBot-" + instance).setSkin(UT2004Skins.getSkin());
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
     * At this point you have {@link Self} i.e., this.info fully initialized.
     */
    @Override
    public void beforeFirstLogic() {
        // REGISTER TO ITEM PICKER CHECKER
        ItemPickerChecker.register(info.getId());
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
    }


    /**
     * THIS BOT has picked an item!
     * @param event
     */
    @EventListener(eventClass=ItemPickedUp.class)
    public void itemPickedUp(ItemPickedUp event) {
        Item item = items.getItem(event.getId());
        if (!isInteresting(item)){
            return;
        }

        notifyCheckerAboutInterestingItem(item);
    }

    /**
     * Someone else picked an item!
     * @param event
     */
    @EventListener(eventClass = TCItemPicked.class)
    public void tcItemPicked(TCItemPicked event) {
        UnrealId itemPickedId = event.getWhat();
        UnrealId whoPickedTheItemId = event.getWho();

        logAsMe("Received info about item picked:" + whoPickedTheItemId + ":" + itemPickedId
                + "|" + "wasPursuing:" + ((currentlyPursuedItem != null) ? currentlyPursuedItem.getId() : "Null"));

        this.pickedItems.add(itemPickedId);
        if (isCurrentlyPursuedItemEqualTo(itemPickedId)) {
            logAsMe("Somebody took my item: " + whoPickedTheItemId + ":" + currentlyPursuedItem.getId());
            transitionToChoosingNewItem();
        }
    }



    /**
     * Someone else picked an item!
     * @param event
     */
    @EventListener(eventClass = TCItemPursuing.class)
    public void tcItemBeingPursued(TCItemPursuing event) {
        logAsMe("Received info about someone PursuingItem:" + event.getWho() + ":" + event.getWhat()
                + "|" + "wasPursuing:" + ((currentlyPursuedItem != null) ? currentlyPursuedItem.getId() : "Null"));


        if(isCurrentlyPursuedItemEqualTo(event.getWhat())){
            // we are both pursuing the same item
            double distanceToCurrentlyPursued = navMeshModule.getAStarPathPlanner().getDistance(info.getLocation(), currentlyPursuedItem);

            logAsMe("Somebody is going after my item: " + event.getWho() + ":" + currentlyPursuedItem.getId());
            //transitionToChoosingNewItem();

        } else {
            this.someOneIsPursuing.add(event.getWhat());
        }
    }


    /**
     * Main method called 4 times / second. Do your action-selection here.
     */
    @Override
    public void logic() throws PogamutException {
        ++logicIterationNumber;
        lastLogicTime = System.currentTimeMillis();

        if (isSomethingNotInitializedOrGameNotRunning()) return;

        switch (currState) {
            case ChoosingItem:
                chooseNewItem();
                break;
            case RunningToItem:
                notifyOthersAboutRunningToAnItem();
                break;
            case RepickingItem:
                notifyCheckerAboutInterestingItem(currentlyPursuedItem);
                break;
        }
    }


    private void transitionToChoosingNewItem() {
        logAsMe("Transitioning to choosing a new item.");

        currState = State.ChoosingItem;
        currentlyPursuedItem = null;
    }

    private void notifyOthersAboutRunningToAnItem() {
        assert currentlyPursuedItem != null;
        double distanceToCurrentlyPursued = navMeshModule.getAStarPathPlanner().getDistance(info.getLocation(), currentlyPursuedItem);
        this.tcClient.sendToTeamOthers(new TCItemPursuing(info.getId(), currentlyPursuedItem.getId(), distanceToCurrentlyPursued));
    }

    private void chooseNewItem() {
        assert currentlyPursuedItem == null;
        currentlyPursuedItem = getNearestInterestingItem();
        logAsMe("Currently pursued item is: " + info.getId() + ":" + currentlyPursuedItem.getId());

        if (currentlyPursuedItem != null) {
            navigation.navigate(currentlyPursuedItem);
            currState = State.RunningToItem;
        } else if (!navigation.isNavigating()) {
            navigation.navigate(navPoints.getRandomNavPoint());
        }
    }

    private Item getNearestInterestingItem() {

        Collection<Item> items = MyCollections.getFiltered(
                this.items.getSpawnedItems().values(),
                new IFilter<Item>() {
                    @Override
                    public boolean isAccepted(Item arg0){
                        return isInteresting(arg0);
                    }
                });

        Item nearestItemLocation = DistanceUtils.getNearest(items, info.getLocation(), new IGetDistance<Item>(){
            @Override
            public double getDistance(Item item, ILocated arg1){
                return navMeshModule.getAStarPathPlanner().getDistance(item,  arg1);

            }
        });
        return nearestItemLocation;
    }

    private void notifyCheckerAboutInterestingItem(Item item) {
        if (ItemPickerChecker.itemPicked(info.getId(), item)) {
            logAsMe("I picked up: " + info.getId() + ":" + item.getId());
            this.tcClient.sendToTeam(new TCItemPicked(info.getId(), item.getId()));
        } else {
            // should not happen... but if you encounter this, just wait with the bot a cycle and report item picked again
            log.severe("SHOULD NOT BE HAPPNEINING! ItemPickerChecker refused our item!");
            transitionToRepickingAnItem(item);
        }
    }

    private void transitionToRepickingAnItem(Item item) {
        logAsMe("I'm starting to repick: " + info.getId() + ":" + item.getId());

        navigation.navigate(item);
        currentlyPursuedItem = item;
        currState = State.RepickingItem;
    }

    private boolean isCurrentlyPursuedItemEqualTo(UnrealId itemId) {
        return currentlyPursuedItem != null && itemId.equals(currentlyPursuedItem.getId());
    }

    public boolean isInteresting(Item item){
        if(item == null)
            return false;

        // not already picked items
        if (pickedItems.contains(item.getId()))
            return false;

        // trash from other players etc
        if (item.isDropped())
            return false;

        // only selected item categories
        ItemType.Category category = item.getType().getCategory();
        return (category == ItemType.Category.WEAPON ||
                category == ItemType.Category.ARMOR ||
                category == ItemType.Category.SHIELD);
    }

    private void logAsMe(String s){
        log.info( instance+ ": " + logicIterationNumber + ":" + s);
    }


    private boolean isSomethingNotInitializedOrGameNotRunning() {
        if (!tcClient.isConnected()) {
            log.warning("TeamComm not running!");
            return true;
        }


        if (!ItemPickerChecker.isRunning()) return true;
        if (!ItemPickerChecker.isInited()) return true;
        if (ItemPickerChecker.isVictory()) return true;

        return false;
    }

    /**
     * This method is called when the bot is started either from IDE or from command line.
     *
     * @param args
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String args[]) throws PogamutException {
        new UT2004BotRunner(      // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                ItemPickerBot.class,  // which UT2004BotController it should instantiate
                "PickerBot"       // what name the runner should be using
        ).setMain(true)           // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed
                .startAgents(ItemPickerChecker.BOTS_COUNT); // tells the runner to start N agent
    }
}