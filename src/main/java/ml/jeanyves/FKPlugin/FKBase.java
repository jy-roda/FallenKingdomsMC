package ml.jeanyves.FKPlugin;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FKBase {

	private DyeColor color;
	private Plugin plugin;
	private Block centerBlock;
	private boolean isAssigned;
	private int radius;
	private Block flag;
	private boolean isGenerated;
	private FKTeam team;

	
	public FKBase(Block centerBlock,int radius,boolean isGenerated, Plugin plugin) {
		
		this.plugin = plugin;
		this.centerBlock = centerBlock;
		this.isAssigned = false;
		this.radius = radius;
		this.isGenerated = isGenerated;
		if (!(this.isGenerated)) {
			this.generateWalls();
			this.generateFlag();
			this.saveInFile();
		}
		this.setFlag();
	}
	
	private void setFlag() {
		World world = this.centerBlock.getWorld();
		this.flag = world.getBlockAt(this.centerBlock.getX(), this.centerBlock.getY() + 5, this.centerBlock.getZ());
	}

	private void saveInFile() {
		try {
			File saveTo = new File("plugins/Fallen_Kingdoms/bases.txt");
			if (!saveTo.exists()) {
				saveTo.createNewFile();
			}
			FileWriter fw = new FileWriter(saveTo, true);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(this.centerBlock.getX() + ";" + this.centerBlock.getY() + ";" + this.centerBlock.getZ() + ";" + this.radius);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void generateFlag() {
		World world = this.centerBlock.getWorld();
		world.getBlockAt(this.centerBlock.getX(), this.centerBlock.getY() + 1, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX(), this.centerBlock.getY() + 2, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() + 1, this.centerBlock.getY() + 2, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() - 1, this.centerBlock.getY() + 2, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() + 1, this.centerBlock.getY() + 3, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() - 1, this.centerBlock.getY() + 3, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() + 1, this.centerBlock.getY() + 4, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX() - 1, this.centerBlock.getY() + 4, this.centerBlock.getZ()).setType(Material.FENCE);
		world.getBlockAt(this.centerBlock.getX(), this.centerBlock.getY() + 4, this.centerBlock.getZ()).setType(Material.FENCE);
	}

	private void generateWalls() {
		for (int x = this.centerBlock.getX() - (radius / 2); x < this.centerBlock.getX() + (radius / 2); x++) {
			getLastFreeBlock(x, this.centerBlock.getZ() - (radius / 2)).setType(this.plugin.getWallMaterial());
			getLastFreeBlock(x, this.centerBlock.getZ() + (radius / 2)).setType(Material.COBBLESTONE);
		}
		for (int z = this.centerBlock.getZ() - (radius / 2); z < this.centerBlock.getZ() + (radius / 2); z++) {
			getLastFreeBlock(this.centerBlock.getX() - (radius / 2), z).setType(Material.COBBLESTONE);
			getLastFreeBlock(this.centerBlock.getX() + (radius / 2), z).setType(Material.COBBLESTONE);
		}
	}

	private Block getLastFreeBlock(int x, int z) {
		int y = 255;
		World world = this.centerBlock.getWorld();
		while (this.plugin.isTransparent(world.getBlockAt(x, y, z)) && this.plugin.isTransparent(world.getBlockAt(x, y - 1, z))) {
			y = y - 1;
		}
		return world.getBlockAt(x, y - 1, z);
	}

	@SuppressWarnings("deprecation")
	public void assignTeam(FKTeam t) {
		this.team = t;
		this.color = t.getColor();
		this.flag.setType(Material.WOOL);
		this.flag.setData(this.color.getData());
		for (Player p : t.getPlayers()) {
			this.plugin.getPlayerForPlayer(p).setBase(this);
		}
	}
	
	public boolean isAssigned() {
		return this.isAssigned;
	}
	
	public FKTeam getTeam() {
		return this.team;
	}
	
	public Block getCenter() {
		return this.centerBlock;
	}
	
	public int getRadius() {
		return this.radius;
	}
	
}
