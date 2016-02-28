package ml.jeanyves.FKPlugin;

import org.bukkit.entity.Player;

public class FKPlayer {
	
	private Player player;
	private int deaths = 0;
	private FKBase b = null;
	
	public FKPlayer(Player p) {
		this.player = p;
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public int getDeaths() {
		return this.deaths;
	}
	
	public FKBase getBase(){
		return this.b;
	}
	
	public void addDeath() {
		this.deaths ++;
	}
	
	public void setBase(FKBase base){
		this.b = base;
	}
	
}
