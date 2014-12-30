package com.pixelgriffin.empires.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.command.SubCommand;
import com.pixelgriffin.empires.enums.GroupPermission;
import com.pixelgriffin.empires.enums.Role;
import com.pixelgriffin.empires.exception.EmpiresJoinableDoesNotExistException;
import com.pixelgriffin.empires.exception.EmpiresNoFundsException;
import com.pixelgriffin.empires.handler.PlayerHandler;

/**
 * 
 * @author Nathan
 *
 */
public class SubCommandWithdraw extends SubCommand {

	@Override
	public boolean run(CommandSender _sender, String[] _args) {
		if(_sender instanceof Player) {
			if(_args.length == 1) {
				Player invoker = (Player)_sender;
				String invokerName = invoker.getName();
				String joinedName = Empires.m_playerHandler.getPlayerJoinedCivilization(invokerName);
				
				if(joinedName.equals(PlayerHandler.m_defaultCiv)) {
					setError("You cannot withdraw money from " + PlayerHandler.m_defaultCiv);
					return false;
				}
				
				try {
					Role invokerRole = Empires.m_playerHandler.getPlayerRole(invokerName);
					
					if(!Empires.m_joinableHandler.joinableHasPermissionForRole(joinedName, invokerRole, GroupPermission.WITHDRAW)) {
						setError("You do not have permission to withdraw money!");
						return false;
					}
				} catch (EmpiresJoinableDoesNotExistException e1) {
					e1.printStackTrace();
					
					setError("Something went wrong!");
					return false;
				}
				
				double amount = 0D;
				
				try {
					amount = Double.valueOf(_args[0]); 
				} catch(NumberFormatException e) {
					setError(_args[0] + " is not a valid amount to withdraw!");
					return false;
				}
				
				try {
					Empires.m_joinableHandler.joinableWithdrawMoney(joinedName, invokerName, amount);
					
					//inform of a successful deposit!
					Empires.m_joinableHandler.broadcastToJoined(joinedName, ChatColor.YELLOW + invokerName + " withdrew " + Empires.m_economy.format(amount) + " from the civilization bank!");
					
					return true;//success
				} catch (EmpiresNoFundsException e) {
					setError(joinedName + " does not have the funds to withdraw " + Empires.m_economy.format(amount) + "!");
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
		
		setError("Only players can invoke the 'deposit' command");
		return false;
	}

}
