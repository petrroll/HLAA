package ut2004.exercises.e03.comm;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.IToken;
import cz.cuni.amis.utils.token.Tokens;

public class TCItemPursuing extends TCMessageData {

	/**
	 * Auto-generated.
	 */
	private static final long serialVersionUID = 7866323423491233L;

	public static final IToken MESSAGE_TYPE = Tokens.get("TCItemPursuing");

	private UnrealId who;

	private UnrealId what;

	private double dist;

	public TCItemPursuing(UnrealId who, UnrealId what, double currentDistanceTo) {
		this.who = who;
		this.what = what;
		this.dist = dist;
	}

	public UnrealId getWho() {
		return who;
	}

	public void setWho(UnrealId who) {
		this.who = who;
	}

	public UnrealId getWhat() {
		return what;
	}

	public void setWhat(UnrealId what) {
		this.what = what;
	}

	public double getDistance(){ return dist; }

}
