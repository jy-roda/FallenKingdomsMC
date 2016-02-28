package ml.jeanyves.FKPlugin;

import java.util.ArrayList;

import org.bukkit.DyeColor;
import org.bukkit.entity.Player;

public class FKTeam {
	
	private DyeColor color;
	private ArrayList<Player> players = new ArrayList<Player>();
	private FKBase base;
	
	public FKTeam(DyeColor color) {
		this.color = color;
	}
	
	public ArrayList<Player> getPlayers() {
		return players;
	}
	
	public DyeColor getColor() {
		return this.color;
	}
	
	public FKBase getBase() {
		return this.base;
	}
	
	public void setBase(FKBase base) {
		this.base = base;
	}
	
	public void addPlayer(Player p) {
		players.add(p);
	}
	
	public void removePlayer(Player p) {
		players.remove(p);
	}
	
	public void clearPlayers() {
		players.clear();
	}
	
}
