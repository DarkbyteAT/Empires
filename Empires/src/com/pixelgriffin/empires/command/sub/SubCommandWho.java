package com.pixelgriffin.empires.command.sub;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.command.SubCommand;
import com.pixelgriffin.empires.enums.Relation;
import com.pixelgriffin.empires.exception.EmpiresJoinableDoesNotExistException;
import com.pixelgriffin.empires.handler.PlayerHandler;
import com.pixelgriffin.empires.util.IDUtility;
import com.pixelgriffin.empires.util.IOUtility;

/**
 * 
 * @author Nathan
 *
 */
public class SubCommandWho extends SubCommand {

	@Override
	public boolean run(CommandSender _sender, String[] _args) {
		if(_sender instanceof Player) {
			Player player = (Player)_sender;
			
			String joinedName = "noname";
			
			//here we determine whether or not we're talking about our own joined status or someone/something else
			if(_args.length == 0) {
				//gather the player's joined name
				joinedName = Empires.m_playerHandler.getPlayerJoinedCivilization(player.getUniqueId());
				
				//no data avilable for default civilization
				if(joinedName.equals(PlayerHandler.m_defaultCiv)) {
					setError("You belong to the " + PlayerHandler.m_defaultCiv);
					return false;
				}
			} else if(_args.length == 1) {
				joinedName = _args[0];
				UUID otherID = IDUtility.getUUIDForPlayer(_args[0]);
				
				//gather the joinedName
				//the user could be talking about a player OR a joinable
				//this determines what they're refering to
				if(Empires.m_joinableHandler.getJoinableExists(_args[0])) {
					joinedName = _args[0];
				} else if(otherID != null) {
						if(Empires.m_playerHandler.getPlayerExists(otherID)) {
						joinedName = Empires.m_playerHandler.getPlayerJoinedCivilization(otherID);
						
						//if the user belongs to the wilderness we cannot print anything
						if(joinedName.equals(PlayerHandler.m_defaultCiv)) {
							setError("That person belongs to the wilderness");
							return false;
						}
					}
				} else {
					//if there was no player or joinable, tell them we couldn't find anything
					setError("Could not find a civilization or person associated with " + _args[0]);
					return false;
				}
			} else {
				setError("Too many arguments!");
				return false;
			}
			
			//gather data based on joined name
			try {
				String displayName = Empires.m_joinableHandler.getJoinableDisplayName(joinedName);
				String description = Empires.m_joinableHandler.getJoinableDescription(joinedName);
				
				double bankValue = Empires.m_joinableHandler.getJoinableBankBalance(joinedName);
				
				int claimSize = Empires.m_joinableHandler.getJoinableClaimSize(joinedName);
				int powerValue = Empires.m_joinableHandler.getJoinablePowerValue(joinedName);
				
				//gather players
				ArrayList<String> onlinePlayers = new ArrayList<String>();
				ArrayList<String> offlinePlayers = new ArrayList<String>();
				
				{
					//separate joined players into online/offline status
					ArrayList<UUID> joinedPlayers = Empires.m_joinableHandler.getJoinableJoinedPlayers(joinedName);
					
					String role, title;
					for(UUID playerID : joinedPlayers) {
						//store real name
						OfflinePlayer jp = null;
						jp = Bukkit.getPlayer(playerID);
						
						if(jp == null)
							jp = Bukkit.getOfflinePlayer(playerID);
						if(jp == null)
							continue;
						
						String playerName = jp.getName();
						if(playerName == null)
							continue;
						
						//gather role & title data
						role = Empires.m_playerHandler.getPlayerRole(playerID).getPrefix();
						title = Empires.m_playerHandler.getPlayerTitle(playerID);
						
						//add space
						if(!title.equals("")) {
							title = title + " ";
						}
						
						//add role & title data
						playerName = role + title + playerName;
						
						//if bukkit finds the player in the server
						if(Bukkit.getPlayer(jp.getName()) != null) {
							onlinePlayers.add(playerName);//they're online
						} else {//otherwise
							offlinePlayers.add(playerName);//they're offline
						}
					}
				}
				
				//gather relations
				ArrayList<String> alliedJoinables = new ArrayList<String>();
				ArrayList<String> enemiedJoinables = new ArrayList<String>();
				
				{
					Set<String> relationNames = Empires.m_joinableHandler.getJoinableRelationNameSet(joinedName);
					
					Relation rel;
					for(String name : relationNames) {
						rel = Empires.m_joinableHandler.getJoinableRelationTo(joinedName, name);
						
						if(rel.equals(Relation.ALLY)) {
							alliedJoinables.add(name);
						} else if(rel.equals(Relation.ENEMY)) {
							enemiedJoinables.add(name);
						}
					}
				}
				
				//type color denotes whether or not something is an empire
				ChatColor typeColor = ChatColor.GRAY;
				String empireName = "";
				ArrayList<String> kingdomList = new ArrayList<String>();
				
				boolean isEmpire = Empires.m_joinableHandler.getJoinableEmpireStatus(joinedName);
				if(isEmpire) {
					//load empire specific data
					kingdomList = Empires.m_joinableHandler.getEmpireKingdomList(joinedName);
					
					//set type color
					typeColor = ChatColor.GOLD;
				} else {
					//load kingdom specific data
					empireName = Empires.m_joinableHandler.getKingdomJoinedEmpire(joinedName);
				}
				
				//print message
				player.sendMessage(typeColor + "__________["+ChatColor.GREEN+displayName+typeColor+"]__________");
				if(Empires.m_vaultActive)
					player.sendMessage(ChatColor.GRAY + "Bank: " + Empires.m_economy.format(bankValue));
				if(!isEmpire) {//is a kingdom
					//we have an empire
					if(!empireName.equals(""))
						player.sendMessage(ChatColor.GRAY + "Empire: " + ChatColor.GOLD + empireName);
				}
				player.sendMessage(ChatColor.GRAY + "Land/Power: " + claimSize + "/" + powerValue);
				player.sendMessage(ChatColor.GRAY + "Description: " + "'" + description + "'");
				player.sendMessage(ChatColor.GRAY + "Allies: " + ChatColor.DARK_PURPLE + alliedJoinables.toString());
				player.sendMessage(ChatColor.GRAY + "Enemies: " + ChatColor.RED + enemiedJoinables.toString());
				if(isEmpire) {
					player.sendMessage(ChatColor.GRAY + "Kingdoms: " + ChatColor.GOLD + kingdomList);
				}
				player.sendMessage(ChatColor.GREEN + "Online Players: ");
				player.sendMessage(ChatColor.GREEN + onlinePlayers.toString());
				player.sendMessage(ChatColor.GRAY + "Offline Players: ");
				player.sendMessage(ChatColor.GRAY + offlinePlayers.toString());
				
			} catch (EmpiresJoinableDoesNotExistException e) {//shouldn't happen, but hey
				setError("Could not find the civilization '" + joinedName + "'");
				return false;
			}
			
			//printed successfully
			return true;
		}
		
		setError("The command 'who' can only be executed by a player");
		return false;
	}
}
