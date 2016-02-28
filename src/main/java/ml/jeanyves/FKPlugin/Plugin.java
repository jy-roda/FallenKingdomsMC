package ml.jeanyves.FKPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;




public class Plugin extends JavaPlugin implements Listener {
	
	private ArrayList<FKTeam> teams = new ArrayList<FKTeam>();
	private ArrayList<FKBase> bases = new ArrayList<FKBase>();
	private Random randomGenerator;
	private String sbObjName;
	private Scoreboard sb = null;
	private int jour;
	private ArrayList<Material> transparentBlocks = new ArrayList<Material>();
	private ArrayList<Material> outsideBlocks = new ArrayList<Material>();
	private ArrayList<FKPlayer> alivePlayers = new ArrayList<FKPlayer>();
	private ArrayList<Player> deadPlayers = new ArrayList<Player>();
	private NumberFormat formatter = new DecimalFormat("0000");
	private NumberFormat minuteFormatter = new DecimalFormat("00");
	private Material wallMaterial;
	private boolean isGameRunning = false;
	private boolean isDamageOn = false;
	private boolean isPVPOn = false;
	private long pauseTime;
	private int deathLimit;
	private boolean isDeathLimit;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable () {
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		this.saveDefaultConfig();
		this.jour = 0;
		
		Bukkit.getServer().getWorlds().get(0).setGameRuleValue("doDaylightCycle", "true");
		Bukkit.getServer().getWorlds().get(0).setTime(6000L);
		
		sb = Bukkit.getScoreboardManager().getNewScoreboard();
		Random r = new Random();
		sbObjName = "KTP" + r.nextInt(10000000);
		Objective obj = sb.registerNewObjective(sbObjName, "dummy");
		obj.setDisplayName(getScoreboardName());
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "Jour " + this.jour)).setScore(4);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + get24time())).setScore(3);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "" + Bukkit.getOnlinePlayers().size() + " joueurs")).setScore(2);

		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "" + teams.size() + " équipes")).setScore(1);
		
		if (this.getConfig().getInt("deathLimit") == 0) {
			this.isDeathLimit = false;
			Bukkit.getLogger().info("Limite des morts désactivée.");
		} else {
			this.isDeathLimit = true;
			this.deathLimit = this.getConfig().getInt("deathLimit");
			Bukkit.getLogger().info("Limite des morts réglée à " + this.getConfig().getInt("deathLimit") + " morts.");
		}
		
		List<String> blocks = this.getConfig().getStringList("ignoredBlocks");
		for (String s : blocks) {
			Material m;
			try {
				m = Material.valueOf(s);
				Bukkit.getLogger().info("Ajout de " + s + " comme bloc transparent.") ;
				this.transparentBlocks.add(m);
			} catch (IllegalArgumentException e) {
				Bukkit.getLogger().warning("Le block \"" + s + "\" est invalide.");
			}
		}
		
		List<String> outBlocks = this.getConfig().getStringList("outsideBlocks");
		for (String s : outBlocks) {
			Material m;
			try {
				m = Material.valueOf(s);
				Bukkit.getLogger().info("Ajout de " + s + " comme bloc autorisé a l'éxterieur.") ;
				this.outsideBlocks.add(m);
			} catch (IllegalArgumentException e) {
				Bukkit.getLogger().warning("Le block \"" + s + "\" est invalide.");
			}
		}
		
		String sm = this.getConfig().getString("wallBlock");
		try {
			Material wm = Material.valueOf(sm);
			Bukkit.getLogger().info("Le bloc des murailles est " + sm);
			this.wallMaterial = wm;
		} catch (IllegalArgumentException ex) {
			Bukkit.getLogger().severe("Le bloc de la muraille est invalide !");
		}
		
		File bases = new File("plugins/Fallen_Kingdoms/bases.txt");
		if (bases.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(bases));
				String line;
				while ((line = br.readLine()) != null) {
					String[] l = line.split(";");
					Bukkit.getLogger().info("Adding base at " + Integer.parseInt(l[0]) + ", " + 
									Integer.parseInt(l[1]) + ", " + Integer.parseInt(l[2]) + 
									" of radius " + Integer.parseInt(l[3]) + " from bases.txt");
					this.bases.add(new FKBase(Bukkit.getWorlds().get(0).getBlockAt(Integer.parseInt(l[0]), Integer.parseInt(l[1]), Integer.parseInt(l[2])), Integer.parseInt(l[3]), true, this));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null) 
						br.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		}
		
		
		Bukkit.getLogger().info("Plugin Fallen Kingdoms activé.");
		
	}
	
	public ArrayList<Material> getIgnoredBlocks() {
		return this.transparentBlocks;
	}
	
	public String getScoreboardName() {
		return getConfig().getString("scoreboard");
	}

	public String get24time() {
		int time;
		long timestamp = Bukkit.getServer().getWorlds().get(0).getTime();
		if (timestamp >= 18000) {
			time = (int) (timestamp - 18000) / 10;
		} else {
			time = (int) (timestamp + 6000) / 10;
		}
		String timeString = formatter.format(time);
		char h1c = timeString.charAt(0);
		char h2c = timeString.charAt(1);
		char m1c = timeString.charAt(2);
		char m2c = timeString.charAt(3);
		String h1 = Character.toString(h1c);
		String h2 = Character.toString(h2c);
		String m1 = Character.toString(m1c);
		String m2 = Character.toString(m2c);
		int minute = Integer.parseInt(m1) * 10 + Integer.parseInt(m2);
		int finalMinute = (minute * 60) / 100;
		String finalTime = h1 + h2 + ":" + minuteFormatter.format(finalMinute);
		return finalTime;
	}

	public void wrongCommand(CommandSender s) {
		s.sendMessage("Mauvaise commande et/ou valeur. Utilisez /fk help pour la liste des commandes.");
	}
	
	public void notOp(Player p) {
		p.sendMessage(ChatColor.RED + "Vous devez être op !");
	}
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender s, Command c, String l,
			String[] a) {
		if (c.getName().equalsIgnoreCase("fk")) {
			if (a == null) {
				wrongCommand(s);
				return true;
			}
			if (a[0].equalsIgnoreCase("team") || a[0].equalsIgnoreCase("teams")) {
				if (a[1] == null) {
					wrongCommand(s);
					return true;
				}
				if (a[1].equalsIgnoreCase("create")) {
					if (s instanceof Player) {
						Player ps = (Player) s;
						if (!(ps.isOp())) {
							notOp(ps);
							return true;
						}
					}
					if (a[2] == null) {
						wrongCommand(s);
						return true;
					}
					if (Integer.parseInt(a[2]) > 0 || Integer.parseInt(a[2]) < 13) {
						int nbTeam = Integer.parseInt(a[2]);
						int nbPlayer = Bukkit.getServer().getOnlinePlayers().size();
						ArrayList<Player> playersToAdd = new ArrayList<Player>(Bukkit.getOnlinePlayers());
						int PlayerByTeam = nbPlayer / nbTeam;
						int playersLeft = nbPlayer % nbTeam;
						if (nbTeam == 12 && playersLeft > 0) {
							nbTeam = 11;
							PlayerByTeam = nbPlayer / nbTeam;
							playersLeft = nbPlayer % nbTeam;
						}
						for (Player p : Bukkit.getServer().getOnlinePlayers()) {
							if (p.getItemInHand().getType() == Material.WOOL) {
								byte Bcolor = (byte) p.getItemInHand().getDurability();
								DyeColor color = DyeColor.getByWoolData(Bcolor);
								if (getTeamByColor(color) == null) {
									teams.add(new FKTeam(color));
									getTeamByColor(color).addPlayer(p);
									playersToAdd.remove(p);
								} else { if (getTeamByColor(color).getPlayers().size() < PlayerByTeam) {
										getTeamByColor(color).addPlayer(p);
										playersToAdd.remove(p);
									}
								}
							}
						}
						if (playersToAdd.size() != 0) {
							s.sendMessage(ChatColor.RED + "Les joueurs ne tiennent pas leur laine de couleur, ou alors ils tiennent une mauvaise couleur.");
							ArrayList<FKTeam> removeTeams = teams;
							for (FKTeam t : removeTeams) {
								teams.remove(t);
							}
							return true;
						}
						s.sendMessage("Équipes crées et équilibrées. Tapez /fk team list pour avoir la liste des teams.");
						return true;
					}
				}
				if (a[1].equalsIgnoreCase("list")) {
					s.sendMessage("Liste des teams :");
					for (FKTeam t : teams) {
						StringBuilder players = new StringBuilder();
						players.append("[");
						for (Player p : t.getPlayers()) {
							players.append(p.getName() + " ");
						}
						players.append(" ]");
						s.sendMessage(" - Team " + t.getColor().toString() + " - " + players.toString());
					}
					return true;
				}
				if (a[1].equalsIgnoreCase("clear")) {
					ArrayList<FKTeam> removeTeams = teams;
					for (FKTeam t : removeTeams) {
						teams.remove(t);
					}
					s.sendMessage("Toutes les teams ont été remises à zéro.");
					return true;
				}
				wrongCommand(s);
				return true;
			}
			if (a[0].equalsIgnoreCase("base")) {
				if (a[1].equalsIgnoreCase("new")) {
					if (a[2] != null) {
						if (Integer.parseInt(a[2]) > 9 && Integer.parseInt(a[2]) < 51) {
							if (!(s instanceof Player)) {
								s.sendMessage("Vous devez être un joueur !");
								return true;
							}
							Player p = (Player) s;
							if (!(p.isOp())) {
								notOp(p);
								return true;
							}
							int radius = Integer.parseInt(a[2]);
							HashSet<Material> hash = null;
							Block center = p.getTargetBlock(hash, 0);
							bases.add(new FKBase(center,radius,false,this));
							s.sendMessage("Base créée !");
							return true;
						}
						s.sendMessage("La taille doit être comprise entre 10 et 50 blocs !");
						return true;
					}
					wrongCommand(s);
					return true;
				}
				wrongCommand(s);
				return true;
			}
			if (a[0].equalsIgnoreCase("start")) {
				if(s instanceof Player) {
					Player coms = (Player) s;
					if (!(coms.isOp())) {
						notOp(coms);
						return true;
					}
				}
				setBaseForTeams();
				Bukkit.getScheduler().runTaskTimer(this, new BukkitRunnable() {
					@Override
					public void run() {
						updateScoreboard();
						checkTime();
					}
				}, 1L, 1L);
				Bukkit.broadcastMessage("Téléportation dans 10 secondes ...");
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
					@Override
					public void run() {
						tpToBases();
						Bukkit.broadcastMessage("Préparez-vous ! La partie commence dans une minute.");
					}

				}, 200L);
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.broadcastMessage("La partie commence dans 10 secondes !");
					}
				}, 1200L);
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.getServer().getWorlds().get(0).setTime(0L);
						isGameRunning = true;
						isDamageOn = true;
						Bukkit.broadcastMessage("La partie commence ! Bonne chance !");
						if (isDeathLimit) {
							Bukkit.broadcastMessage(ChatColor.BLUE + "[RAPPEL]" + ChatColor.WHITE + " Vous disposez de " + deathLimit + " vies.");
						}
					}
				}, 1400L);
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.broadcastMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 1. " + getConfig().getString("obj1"));
						Bukkit.broadcastMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 2. " + getConfig().getString("obj2"));
						Bukkit.broadcastMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 3. " + getConfig().getString("obj3"));
					}
				}, 2400L);
				return true;
			}
			if (a[0].equalsIgnoreCase("pause")) {
				if(s instanceof Player) {
					Player coms = (Player) s;
					if (!(coms.isOp())) {
						notOp(coms);
						return true;
					}
				}
				if (!(isGameRunning)) {
					s.sendMessage("Le jeu n'est pas en cours !");
					return true;
				}
				this.pauseTime = Bukkit.getServer().getWorlds().get(0).getTime();
				this.isGameRunning = false;
				this.isDamageOn = false;
				this.isPVPOn = false;
				Bukkit.getScheduler().runTaskTimer(this, new BukkitRunnable() {
					@Override
					public void run() {
						if(!(isGameRunning)) {
							Bukkit.getServer().getWorlds().get(0).setTime(pauseTime);
						} else {
							this.cancel();
							return;
						}
					}
				}, 20L, 20L);
				Bukkit.broadcastMessage("Jeu mis en pause.");
				return true;
			}
			if (a[0].equalsIgnoreCase("resume") || a[0].equalsIgnoreCase("reprendre")) {
				if(s instanceof Player) {
					Player coms = (Player) s;
					if (!(coms.isOp())) {
						notOp(coms);
						return true;
					}
				}
				if (isGameRunning) {
					s.sendMessage("Le jeu est déjà en cours !");
					return true;
				}
				this.isGameRunning = true;
				this.isDamageOn = true;
				if (this.getConfig().getInt("jourDePVP") <= this.jour) {
					this.isPVPOn = true;
				}
				Bukkit.broadcastMessage("Le jeu a repris !");
				return true;
			}
			if (a[0].equalsIgnoreCase("vie") || a[0].equalsIgnoreCase("vies")) {
				if (!(s instanceof Player)) {
					s.sendMessage("Vous devez être un joueur !");
					return true;
				}
				if (!(this.isDeathLimit)) {
					s.sendMessage("Vous n'avez pas de limite de vie,");
					return true;
				}
				s.sendMessage("Il vous reste encore " + (deathLimit - getPlayerForPlayer((Player) s).getDeaths()) + " vies." );
				return true;
			}
			if (a[0].equalsIgnoreCase("objectifs")) {
				s.sendMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 1. " + getConfig().getString("obj1"));
				s.sendMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 2. " + getConfig().getString("obj2"));
				s.sendMessage(ChatColor.GREEN + "[OBJECTIFS]" + ChatColor.WHITE + " 3. " + getConfig().getString("obj3"));
			}
			if (a[0].equalsIgnoreCase("help")) {
				s.sendMessage(ChatColor.YELLOW + "Commandes du plugin Fallen Kingdoms:");
				s.sendMessage(ChatColor.YELLOW + "/fk teams list " + ChatColor.WHITE + ": Donne la liste des équipes ainsi que les joueurs de chaque équipe.");
				s.sendMessage(ChatColor.YELLOW + "/fk objectifs " + ChatColor.WHITE + ": Affiche les objectifs de la partie.");
				if (this.isDeathLimit && s instanceof Player) {
					s.sendMessage("/fk vie : Affiche vos vies restantes.");
				}
				if (!(s instanceof Player)) {
					s.sendMessage(ChatColor.YELLOW + "/fk teams create <nb teams> " + ChatColor.WHITE + ": Crée le nombre de teams correpondant.");
					s.sendMessage(ChatColor.YELLOW + "/fk teams clear " + ChatColor.WHITE + ": Supprime toute les teams.");
					s.sendMessage(ChatColor.YELLOW + "/fk start " + ChatColor.WHITE + ": Démarre le Fallen Kingdoms.");
					s.sendMessage(ChatColor.YELLOW + "/fk pause " + ChatColor.WHITE + ": met en pause le jeu.");
					s.sendMessage(ChatColor.YELLOW + "/fk resume " + ChatColor.WHITE + ": Reprend le jeu.");
					return true;
				}
				Player p = (Player) s;
				if (p.isOp()) {
					s.sendMessage(ChatColor.YELLOW + "/fk teams create <nb teams> " + ChatColor.WHITE + ": Crée le nombre de teams correpondant.");
					s.sendMessage(ChatColor.YELLOW + "/fk teams clear " + ChatColor.WHITE + ": Supprime toute les teams.");
					s.sendMessage(ChatColor.YELLOW + "/fk base new <rayon> " + ChatColor.WHITE + ": Crée une base du rayon spécifié (10-50 blocs)");
					s.sendMessage(ChatColor.YELLOW + "/fk start " + ChatColor.WHITE + ": Démarre le Fallen Kingdoms.");
					s.sendMessage(ChatColor.YELLOW + "/fk pause " + ChatColor.WHITE + ": met en pause le jeu.");
					s.sendMessage(ChatColor.YELLOW + "/fk resume " + ChatColor.WHITE + ": Reprend le jeu.");
					return true;
				}
				return true;
			}
			wrongCommand(s);
			return true;
		}
		return false;
	}

	public void checkTime() {
		if (Bukkit.getServer().getWorlds().get(0).getTime() == 12000L) {
			Bukkit.broadcastMessage("La nuit tombe !");
		}
		if (Bukkit.getServer().getWorlds().get(0).getTime() == 0L) {
			this.jour ++;
			Bukkit.broadcastMessage("Le jour se lève ! Nous sommes au jour " + this.jour);
			checkDay();
		}
	}

	public void checkDay() {
		if(this.jour == this.getConfig().getInt("jourDePVP")) {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Le PVP est mainetant activé !");
			isPVPOn = true;
			Bukkit.broadcastMessage(ChatColor.BLUE + "[RAPPEL]" + ChatColor.WHITE + " Vous ne pouvez pas attaquer avant d'avoir rempli vos objectifs.");
			Bukkit.broadcastMessage(ChatColor.YELLOW + "[AIDE]" + ChatColor.WHITE + " Pour voir les objectifs, tapez /fk objectifs.");
		} 
	}

	@SuppressWarnings("deprecation")
	public void updateScoreboard() {
		Objective obj = null;
		try {
			obj = sb.getObjective(sbObjName);
			obj.setDisplaySlot(null);
			obj.unregister();
		} catch (Exception e) {
			
		}
		Random r = new Random();
		sbObjName = "KTP" + r.nextInt(10000000);
		obj = sb.registerNewObjective(sbObjName, "dummy");
		obj.setDisplayName(getScoreboardName());
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "Jour " + this.jour)).setScore(4);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + get24time())).setScore(3);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "" + Bukkit.getOnlinePlayers().size()  + " joueurs")).setScore(2);
		obj.getScore(Bukkit.getOfflinePlayer(ChatColor.GRAY + "" + teams.size() + " équipes")).setScore(1);
	}

	public void tpToBases() {
		for (FKTeam t : teams) {
			for (Player p : t.getPlayers()) {
				tpInsideBase(p, t.getBase());
			}
		}
	}
	
	public void tpInsideBase(Player p, FKBase base) {
		Random ran = new Random();
		int xMin = base.getCenter().getX() - (base.getRadius() / 2);
		int xMax = base.getCenter().getX() + (base.getRadius() / 2);
		int x = ran.nextInt(xMax-xMin) + xMin;
		int zMin = base.getCenter().getZ() - (base.getRadius() / 2);
		int zMax = base.getCenter().getZ() + (base.getRadius() / 2);
		int z = ran.nextInt(zMax-zMin) + zMin;
		Location tpLoc = new Location(Bukkit.getServer().getWorlds().get(0),x , base.getCenter().getY() + 30, z);
		p.teleport(tpLoc);
	}

	public void setBaseForTeams() {
		Random ran = new Random();
		ArrayList<FKBase> unusedBases = bases;
		for (final FKTeam t : teams) {
			final FKBase b = unusedBases.get(ran.nextInt(unusedBases.size()));
			t.setBase(b);
			b.assignTeam(t);
			unusedBases.remove(b);
		}
	}

	@EventHandler
	public void onPlayerItemConsumeEvent(final PlayerItemConsumeEvent ev) {
		Bukkit.getLogger().info("Player " + ev.getPlayer().getDisplayName() + " consumed an item. Running potions check ...");
		if (ev.getItem().getType() == Material.POTION) {
			Bukkit.getLogger().info("Item is potion.");
			Potion po = Potion.fromItemStack(ev.getItem());
			if (po.getLevel() >= 2) {
				Bukkit.getLogger().info("Potion is of type " + po.getEffects().toString() + " and is of level 2 or above.");
				Bukkit.getLogger().info("Cancelling event and removong potion(s) from inventory...");
				ev.setCancelled(true);
				ev.getItem().setType(Material.AIR);
				ev.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas utiliser de potions de niveau 2 !");
				Bukkit.getLogger().info("Potion and effects succesfully removed.");
			}  
		}
	}
	
	@EventHandler
	public void onEntityDamage(final EntityDamageEvent e) {
		if(!(isDamageOn)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && e.getDamager() instanceof Player && !(isPVPOn)) {
			e.setCancelled(true);
			Player p = (Player) e.getDamager();
			p.sendMessage(ChatColor.RED + "Vous ne pouvez pas encore attaquer ! Le PVP sera activé à partir du jour " + this.getConfig().getInt("jourDePVP"));
		}
	}
	
	@EventHandler
	public boolean onBlockPlace(BlockPlaceEvent e) {
		if (!(isGameRunning)) {
			e.setCancelled(true);
			return true;
		}
		if (!(isBlockInBase(e.getBlock(), getBaseForPlayer(e.getPlayer()), e.getPlayer())) && !(outsideBlocks.contains(e.getBlock().getType()))) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas placer ce bloc en dehors de votre base !");
			return true;
		}
		return false;
	}
	
	@EventHandler
	public boolean onBlockBreak(BlockBreakEvent e) {
		if (!(isGameRunning)) {
			e.setCancelled(true);
			return true;
		}
		if (isBlockInAnyBase(e.getBlock())) {
			if (this.isBlockInBase(e.getBlock(), this.getPlayerForPlayer(e.getPlayer()).getBase(), e.getPlayer())) {
				// Do Nothing
			} else {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas casser de blocs dans une base ennemie !");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (isDeathLimit && deadPlayers.contains(e.getPlayer())) {
			e.getPlayer().kickPlayer("Il ne vous reste plus aucune vie !");
		}
	}
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayerForPlayer(e.getPlayer()) == null) {
			alivePlayers.add(new FKPlayer(e.getPlayer()));
		}
		e.getPlayer().setScoreboard(sb);
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		if (isDeathLimit) {
			e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "[INFO] " + ChatColor.WHITE + "Vous êtes morts " + getPlayerForPlayer(e.getPlayer()).getDeaths() 
					+ " fois : il ne vous reste plus que " + (deathLimit - getPlayerForPlayer(e.getPlayer()).getDeaths()) + " vies.");
		}
		e.getPlayer().setScoreboard(sb);
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent e) {
		if (isDeathLimit) {
			getPlayerForPlayer(e.getEntity()).addDeath();
			if (getPlayerForPlayer(e.getEntity()).getDeaths() == this.deathLimit) {
				Collection<? extends Player> pps = Bukkit.getOnlinePlayers();
				Player[] ps = pps.toArray (new Player[pps.size ()]);
				for (Player pp : ps) {
					pp.playSound(pp.getLocation(), Sound.WITHER_SPAWN, 1F, 1F);
				}
				alivePlayers.remove(getPlayerForPlayer(e.getEntity()));
				try {
					ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
					SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
					skullMeta.setOwner(((Player)e.getEntity()).getName());
					skullMeta.setDisplayName(ChatColor.RESET + ((Player)e.getEntity()).getName());
					skull.setItemMeta(skullMeta);
					e.getEntity().getLocation().getWorld().dropItem(e.getEntity().getLocation(), skull);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
					@Override
					public void run() {
						e.getEntity().kickPlayer("Merci d'avoir participé !");
					}
				}, 20L * 30);
			}
		}
	}
	
	public FKPlayer getPlayerForPlayer(Player p) {
		for (FKPlayer fkp : alivePlayers) {
			if (fkp.getPlayer() == p) {
				return fkp;
			}
		}
		return null;
	}

	public FKTeam getTeamByColor(DyeColor c) {
		for (FKTeam t : teams) {
			if (t.getColor().toString() == c.toString()) {
				return t;
			}
		}
		return null;
	}
	
	public boolean isTransparent(Block b) {
		if (transparentBlocks.contains(b.getType())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public ArrayList<FKTeam> getFreeTeams(int maxPlayers) {
		ArrayList<FKTeam> freeTeams = new ArrayList<FKTeam>();
		for (FKTeam t : teams) {
			if (t.getPlayers().size() < maxPlayers) {
				freeTeams.add(t);
			}
		}
		return freeTeams;
	}
	
	public DyeColor getRandomUnusedColor() {
		while (true) {
			DyeColor color = DyeColor.values()[randomGenerator.nextInt(DyeColor.values().length)];
			if (getTeamByColor(color) == null) {
				return color;
			}
		}
	}
	
	public FKTeam getTeamForPlayer(Player p) {
		for (FKTeam t : teams) {
			if (t.getPlayers().contains(p)) {
				return t;
			}
		}
		return null;
	}
	
	public FKBase getBaseForPlayer(Player p) {
		return getPlayerForPlayer(p).getBase();
	}
	
	public boolean isPlayerInHomeBase(Player p) {
		if (isPlayerInBase(p, getBaseForPlayer(p))) {
			return true;
		}
		return false;
	}
	
	public boolean isPlayerInBase(Player p, FKBase b) {
		if (p.getLocation().getBlockX() >= b.getCenter().getX() - (b.getRadius() / 2) && p.getLocation().getBlockX() <= b.getCenter().getX() + (b.getRadius() / 2)
						&& p.getLocation().getBlockZ() >= b.getCenter().getZ() - (b.getRadius() / 2) && p.getLocation().getBlockZ() <= b.getCenter().getZ() + (b.getRadius() / 2)
						&& p.getLocation().getBlockY() >= b.getCenter().getY() - getConfig().getInt("blocksUnderBase")) {
			return true;
		}
		return false;
	}
	
	public boolean isBlockInBase(Block p, FKBase b, Player pl) {
		if (pl.getLocation().getWorld() != Bukkit.getServer().getWorlds().get(0)) {
			return true;
		} else 
		if (p.getX() >= b.getCenter().getX() - (b.getRadius() / 2) && p.getX() <= b.getCenter().getX() + (b.getRadius() / 2)
						&& p.getZ() >= b.getCenter().getZ() - (b.getRadius() / 2) && p.getZ() <= b.getCenter().getZ() + (b.getRadius() / 2)
						&& p.getY() >= b.getCenter().getY() - getConfig().getInt("blocksUnderBase")) {
			return true;
		}
		return false;
	}
	
	public boolean isBlockInAnyBase(Block b) {
		for (FKPlayer p : alivePlayers) {
			if (isBlockInBase(b, p.getBase(), p.getPlayer())) {
				return true;
			}
		}
		return false;
	}

	public boolean teamsReady() {
		if (teams.size() > 1) {
			return true;
		} else {
			return false;
		}
	}
	
	public FKTeam getRandomTeam() {
		return teams.get(randomGenerator.nextInt(teams.size()));
	}
	
	@Override
	public void onDisable() {
		Bukkit.getLogger().info("Plugin Fallen Kingdoms désactivé.");
	}

	public Material getWallMaterial() {
		return this.wallMaterial;
	}
	
}  
