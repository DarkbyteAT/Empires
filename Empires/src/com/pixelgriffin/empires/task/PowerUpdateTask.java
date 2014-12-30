package com.pixelgriffin.empires.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.EmpiresConfig;

/**
 * 
 * @author Nathan
 *
 */
public class PowerUpdateTask implements Runnable {

	private static final int m_increment = 1;//value to increment power
	
	@Override
	public void run() {
		
		int powerVal;
		for(Player online : Bukkit.getOnlinePlayers()) {
			//gather the power
			powerVal = Empires.m_playerHandler.getPlayerPower(online.getName());
			
			int totalPower = EmpiresConfig.m_powerMax;
			if(online.hasPermission("Empires.power.extra")) {
				totalPower = EmpiresConfig.m_power2Max;
			}
			
			if(powerVal < totalPower) {//should we increase their power?
				//change the value
				Empires.m_playerHandler.setPlayerPower(online.getName(), powerVal + m_increment);//for us
			}
		}
	}

}
