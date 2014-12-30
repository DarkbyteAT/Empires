package com.pixelgriffin.empires.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.command.SubCommand;
import com.pixelgriffin.empires.enums.GroupPermission;
import com.pixelgriffin.empires.enums.Role;
import com.pixelgriffin.empires.exception.EmpiresJoinableDoesNotExistException;
import com.pixelgriffin.empires.exception.EmpiresJoinableIsEmpireException;
import com.pixelgriffin.empires.handler.PlayerHandler;

/**
 * 
 * @author Nathan
 *
 */
public class SubCommandInvite extends SubCommand {

	@Override
	public boolean run(CommandSender _sender, String[] _args) {
		if(_sender instanceof Player) {
			if(_args.length == 1) {
				//gather info
				Player invoker = (Player)_sender;
				String invokerName = invoker.getName();
				String joinedName = Empires.m_playerHandler.getPlayerJoinedCivilization(invokerName);
				Role invokerRole = Empires.m_playerHandler.getPlayerRole(invokerName);
				
				if(joinedName.equals(PlayerHandler.m_defaultCiv)) {
					setError("You cannot invite people to " + PlayerHandler.m_defaultCiv);
					return false;
				}
				
				try {
					//do we have permission?
					if(Empires.m_joinableHandler.joinableHasPermissionForRole(joinedName, invokerRole, GroupPermission.INVITE)) {
						//is the arg0 a player?
						if(Empires.m_playerHandler.playerExists(_args[0])) {
							//handle player
							//did we request them?
							if(Empires.m_joinableHandler.getJoinableRequestedPlayer(joinedName, _args[0])) {
								//remove them from the request list
								Empires.m_joinableHandler.joinableRemoveRequestedPlayer(joinedName, _args[0]);
								
								//gather display name..
								String displayName = Empires.m_joinableHandler.getJoinableDisplayName(joinedName);
								//inform
								Player invited = Bukkit.getPlayer(_args[0]);
								invited.sendMessage(ChatColor.YELLOW + "You are no longer invited to " + displayName);
								
								Empires.m_joinableHandler.broadcastToJoined(joinedName, ChatColor.YELLOW + invokerName + " uninvited " + invited.getName() + " to the civilization");
								return true;
							} else {
								//add them to request list
								Empires.m_joinableHandler.joinableRequestPlayer(joinedName, _args[0]);
								
								//gather display name..
								String displayName = Empires.m_joinableHandler.getJoinableDisplayName(joinedName);
								//inform
								Player invited = Bukkit.getPlayer(_args[0]);
								invited.sendMessage(ChatColor.YELLOW + "You have been invited to join " + displayName);
								
								Empires.m_joinableHandler.broadcastToJoined(joinedName, ChatColor.YELLOW + invokerName + " invted " + invited.getName() + " to the civilization");
								return true;
							}
						} else if(Empires.m_joinableHandler.joinableExists(_args[0])) {//we're talking a joinable here
							//handle inviting a joinable
							//are we an empire?
							if(Empires.m_joinableHandler.getJoinableEmpireStatus(joinedName)) {
								//are we a leader?
								if(invokerRole.equals(Role.LEADER)) {
									//invite kingdoms to empire
									try {
										//did we request them yet?
										if(!Empires.m_joinableHandler.getEmpireRequestedKingdom(joinedName, _args[0])) {
											//request
											Empires.m_joinableHandler.empireRequestKingdom(joinedName, _args[0]);
											
											String displayName = Empires.m_joinableHandler.getJoinableDisplayName(_args[0]);
											
											//inform
											//us
											Empires.m_joinableHandler.broadcastToEmpireNetwork(joinedName, ChatColor.YELLOW + displayName + " was invited to join the empire");
											//them
											Empires.m_joinableHandler.broadcastToJoined(_args[0], ChatColor.YELLOW + displayName + " was invited to join the " + joinedName + " empire");
											
											return true;
										} else {//guess we did
											//un-request
											Empires.m_joinableHandler.empireRemoveRequestedKingdom(joinedName, _args[0]);
											
											String displayName = Empires.m_joinableHandler.getJoinableDisplayName(_args[0]);
											
											//inform
											//us
											Empires.m_joinableHandler.broadcastToEmpireNetwork(joinedName, ChatColor.YELLOW + displayName + " is no longer invited to the empire");
											//them
											Empires.m_joinableHandler.broadcastToJoined(_args[0], ChatColor.YELLOW + displayName + " is no longer invited to the " + joinedName + "empire");
											
											return true;
										}
									} catch (EmpiresJoinableIsEmpireException e) {
										setError("You cannot invite another empire to join you!");
										return false;
									}
								}
								
								setError("You must be the leader to invite kingdoms to the Empire!");
								return false;
							}
							
							setError("You must be an empire to invite kingdoms!");
							return false;
						}
						//could not find an existing joinable or player
						setError("Could not find a reference for '" + _args[0] + "'");
						return false;
					}
					
					setError("You do not have permission to invite players!");
					return false;
				} catch (EmpiresJoinableDoesNotExistException e) {
					e.printStackTrace();
					
					setError("Something went wrong!");
					return false;
				}
			}
			
			setError("Invalid arguments!");
			return false;
		}
		
		setError("The command 'invite' can only be executed by players");
		return false;
	}

}
