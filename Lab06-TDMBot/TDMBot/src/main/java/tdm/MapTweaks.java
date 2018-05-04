package tdm;

import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.NavigationGraphBuilder;

/**
 * Class containing adjustments for navigation graph of PogamutCup competition maps.
 * 
 * @author Jimmy
 */
public class MapTweaks {

	/**
	 * Called from {@link TDMBot#botInitialized(cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo, cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange, cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage)}.
	 * @param navBuilder
	 */
	public static void tweak(NavigationGraphBuilder navBuilder) {
		if (navBuilder.isMapName("DM-1on1-Roughinery-FPS")) tweakDM1on1RoughineryFPS(navBuilder);
		if (navBuilder.isMapName("DM-DE-Ironic-FE")) tweakDMDEIronicFE(navBuilder);
		if (navBuilder.isMapName("DM-Rankin-FE")) tweakDMRankinFE(navBuilder);
		
	}
	
	// ======================
	// DM-1on1-Roughinery-FPS
	// ======================
	
	private static void tweakDM1on1RoughineryFPS(NavigationGraphBuilder navBuilder) {
	}
	
	// ======================
	// DM-DE-Ironic-FE
	// ======================
	
	private static void tweakDMDEIronicFE(NavigationGraphBuilder navBuilder) {
	}

	// ======================
	// DM-Ranking-FE
	// ======================
	
	private static void tweakDMRankinFE(NavigationGraphBuilder navBuilder) {		
		navBuilder.removeEdge("PathNode122", "JumpSpot1");
	}
	
}
