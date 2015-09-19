package com.pixelgriffin.empires.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.enums.Role;
import com.pixelgriffin.empires.exception.EmpiresJoinableDoesNotExistException;
import com.pixelgriffin.empires.exception.EmpiresJoinableExistsException;
import com.pixelgriffin.empires.exception.EmpiresPlayerExistsException;
import com.pixelgriffin.empires.util.IDUtility;
import com.pixelgriffin.empires.util.IOUtility;

/**
 * 
 * @author Nathan
 *
 */
public class PlayerHandler extends DataHandler {
	
	//the player file without plugin data folder directory
	private final String m_file = "players.dat";
	
	//the default player generation values
	public static final String m_defaultCiv = "wilderness";
	public static final String m_defaultTitle = "";
	public static final Role m_defaultRole = Role.MEMBER;
	public static final int m_defaultPower = 0;
	
	public PlayerHandler() {
		super();
	}
	
	/*
	 * Basic load/save
	 */
	
	public void loadFile(JavaPlugin _inst) {
		createDirectory();//attempt to create a non existent directory
		loadConfigSafe(m_file, _inst);//load the file
	}
	
	public void saveFile() {
		saveConfigSafe(m_file);
	}
	
	public void updateToUUIDs() {
		YamlConfiguration conf = getFileConfiguration();
		
		HashSet<String> players = (HashSet<String>)conf.getKeys(false);
		
		for(String name : players) {
			if(name.equals("data-version"))
				continue;
			
			//convert name to UUID
			UUID id = IDUtility.getUUIDForPlayer(name);
			
			if(id != null) {
				ConfigurationSection sect = conf.getConfigurationSection(name);
				
				//save data old data
				String j = sect.getString("j");
				int p = sect.getInt("p");
				String t = sect.getString("t");
				String r = sect.getString("r");
				long pt = sect.getLong("pt");
				boolean ac = sect.getBoolean("ac");
				int tpid = sect.getInt("tp-id");
				
				//clear old data
				conf.set(name, null);
				
				//create new data
				sect = conf.createSection(id.toString());
				sect.set("j", j);
				sect.set("p", p);
				sect.set("t", t);
				sect.set("r", r);
				sect.set("pt", pt);
				sect.set("ac", ac);
				sect.set("tp-id", tpid);
			} else {
				IOUtility.log("CRITICAL ERROR WHEN CONVERTING player.dat: null UUID for '" + name + "'");
			}
		}
	}
	
	/*
	 * Player specific loading
	 */
	
	/**
	 * "Instantiates" a new player in YML
	 * @param _name
	 */
	private void invokeCreatePlayerSpace(UUID _id) throws EmpiresPlayerExistsException {
		YamlConfiguration conf = getFileConfiguration();
		String idString = _id.toString();
		
		//a player with this name already exists in the YML, creating new space is unadvisable
		if(conf.isConfigurationSection(idString))
			throw new EmpiresPlayerExistsException("A player with the UUID " + idString + " found to exist when attemting to create new space");
		
		//create a default player space
		ConfigurationSection sect = conf.createSection(idString);
		
		sect.set("j", m_defaultCiv);//set civilization name
		sect.set("p", m_defaultPower);//set power value
		sect.set("t", m_defaultTitle);//set title
		sect.set("r", m_defaultRole.toString());//set role
		sect.set("pt", System.currentTimeMillis());
		
		//set autoclaiming
		sect.set("ac", false);
		//set TPID
		sect.set("tp-id", -1);
	}
	
	//methods already create a player if he is found to be non-existent
	//however when you don't want to create a player on accident use this
	public boolean getPlayerExists(UUID _id) {
		return getFileConfiguration().isConfigurationSection(_id.toString());
	}
	
	public Role getPlayerRole(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return m_defaultRole;
		} else {
			ConfigurationSection sect = conf.getConfigurationSection(idString);
			
			try {
				return Role.valueOf(sect.getString("r"));
			} catch(Exception e) {//corrupted role value found
				e.printStackTrace();
				return Role.MEMBER;
			}
		}
	}
	
	/**
	 * Gather player's joined civilization name
	 * @param _name - the name of the player in question
	 * @return the name of the civilization the player is joined with (m_defaultCiv if none)
	 */
	public String getPlayerJoinedCivilization(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//if this player does not exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return m_defaultCiv;//will have set the civ to this by default creation
		} else {
			//he does exist, return his joinable
			return conf.getConfigurationSection(idString).getString("j");//grab the j (joinable) YML stored string
		}
	}
	
	public String getPlayerTitle(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//if this player does not exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return m_defaultTitle;//return default just set
		} else {
			//he does exist, return his joinable
			return conf.getConfigurationSection(idString).getString("t");//grab the j (joinable) YML stored string
		}
	}
	
	public int getPlayerPower(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist??
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return m_defaultPower;//return default just set
		} else {
			//he exists
			return conf.getConfigurationSection(idString).getInt("p");
		}
	}
	
	public boolean getPlayerAutoClaiming(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist??
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return false;//return default just set
		} else {
			//he exists
			return conf.getConfigurationSection(idString).getBoolean("ac");
		}
	}
	
	public void setPlayerAutoClaiming(UUID _id, boolean _val) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);

		//finally set our auto claiming value
		sect.set("ac", _val);
	}
	
	public void setPlayerTPID(UUID _id, int _val) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);

		//finally set our auto claiming value
		sect.set("tp-id", _val);
	}
	
	public int getPlayerTPID(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist??
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return -1;//return default just set
		} else {
			//he exists
			return conf.getConfigurationSection(idString).getInt("tp-id");
		}
	}
	
	/**
	 * Removes a player from a joined civilization. Reset both the player's pointer and the joinable's reference
	 * @param _name
	 * @throws EmpiresJoinableDoesNotExistException
	 */
	@SuppressWarnings("deprecation")
	public void invokeRemovePlayerFromJoinedJoinable(UUID _id) throws EmpiresJoinableDoesNotExistException {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
			
			//no reason to do anything in here since he had no joinable set to begin with
			return;
		}
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		String joined = sect.getString("j");
		
		
		//we cannot remove ourselves from the default civ (wilderness)
		if(joined.equals(m_defaultCiv))
			return;
		
		//since we are in a joinable and we exist..
		//we will change our pointer to the default (wilderness)
		sect.set("j", m_defaultCiv);
		
		//and remove ourselves from the previously joined player list
		Empires.m_joinableHandler.invokeJoinableRemovePlayer(joined, _id);
		
		//gather the old role
		Role oldRole = Role.valueOf(sect.getString("r").toUpperCase());
		
		//reset our role as well
		sect.set("r", m_defaultRole.toString());
		//reset our title
		sect.set("t", m_defaultTitle);
		
		//if we haven't disbanded then do some extra stuff
		if(!Empires.m_joinableHandler.getJoinableExists(joined))
			return;
		
		//now that we're out of the previously joined civilization
		//remove our power value
		//gather
		int powerValue = getPlayerPower(_id);
		
		//remove
		Empires.m_joinableHandler.setJoinablePowerValue(joined, -powerValue, true);
		
		//was the removed player the leader?
		if(oldRole.equals(Role.LEADER)) {
			//find a new leader.
			try {
				Empires.m_joinableHandler.invokeJoinableFindNewLeader(joined, true);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();//shouldn't happen.. but you never know!
			}
		}
	}
	
	/**
	 * Adds a player to a civilization.
	 * @param _playerID Player UUID
	 * @param _joinableID Joinable id
	 * @throws EmpiresJoinableExistsException the player in question already has a joinable set
	 * @throws EmpiresJoinableDoesNotExistException the joinable in question does not exists
	 */
	public void setPlayerJoinedCivlization(UUID _playerID, String _joinableID) throws EmpiresJoinableExistsException, EmpiresJoinableDoesNotExistException {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _playerID.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_playerID);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		if(!sect.getString("j").equals(m_defaultCiv))
			throw new EmpiresJoinableExistsException("The player '" + idString + "' already has a joinable set. Use removePlayerFromJoinedCivilization to remove him from one");
		
		//since we don't have a joinable and we exist
		//we set our pointer to the joinable in question
		sect.set("j", _joinableID);
		//and add ourselves to the newly joined player list
		Empires.m_joinableHandler.invokeJoinableAddPlayer(_joinableID, _playerID);
		
		//now that we've added the player, change the power value yo
		//gather power value
		int powerValue = getPlayerPower(_playerID);
		
		//add the power!
		Empires.m_joinableHandler.setJoinablePowerValue(_joinableID, powerValue, true);
	}
	
	public void setPlayerRole(UUID _id, Role _role) throws EmpiresJoinableDoesNotExistException {
		YamlConfiguration conf = getFileConfiguration();
		
		//in the case that someone tries to change the name of nobody..?
		if(_id == null)
			return;
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		//make sure we're in a joinable so we can set the role
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		
		if(sect.getString("j").equals(m_defaultCiv))
			throw new EmpiresJoinableDoesNotExistException("Attempted to set a role for " + idString + " who is not involved with a joinable");
		
		//finally set our role
		sect.set("r", _role.toString());
	}
	
	public void setPlayerTitle(UUID _id, String _title) throws EmpiresJoinableDoesNotExistException {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		//make sure we're in a joinable so we can set the role
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		if(sect.getString("j").equals(m_defaultCiv))
			throw new EmpiresJoinableDoesNotExistException("Attempted to set a role for " + idString + " who is not involved with a joinable");
		
		//finally set our role
		sect.set("t", _title);
	}
	
	/**
	 * Changes player's power and absolutely affects the player's joinable
	 * @param _name
	 * @param _val
	 */
	public void setPlayerPower(UUID _id, int _val) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		//gather sect
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		
		//gather old power
		int oldPower = sect.getInt("p");
		
		//set our new power
		sect.set("p", _val);
		
		//update our joined total power
		//gather joined
		String joined = sect.getString("j");
		
		//do not act on wilderness
		if(joined.equals(m_defaultCiv))
			return;
		
		try {
			//change the power relatively based on the old value and the new one
			//for example: we're at 10 we die so we set our power to 0
			//the difference is -10 and therefore 10 power is removed from the joinable
			Empires.m_joinableHandler.setJoinablePowerValue(joined, _val - oldPower, true);//for our joinable
		} catch (EmpiresJoinableDoesNotExistException e) {
			e.printStackTrace();//something went wrong! no joinable was found.. boohoo
		}
	}

	public void overridePlayerJoinedCivilization(UUID _id, String _name) throws EmpiresJoinableDoesNotExistException {
		YamlConfiguration conf = getFileConfiguration();
		
		//lower case the name for proper lookup
		_name = _name.toLowerCase();
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		if(!Empires.m_joinableHandler.getJoinableExists(_name))
			throw new EmpiresJoinableDoesNotExistException("Tried to override player civilization to a non-existent joinable '" + _name + "'");
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		sect.set("j", _name);
	}
	
	public void setPlayerLastPlayedTime(UUID _id, long _time) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist in YML
		if(!conf.isConfigurationSection(idString)) {
			try {
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {
				e.printStackTrace();
			}
		}
		
		ConfigurationSection sect = conf.getConfigurationSection(idString);
		
		sect.set("pt", _time);
	}
	
	public long getPlayerLastPlayedTime(UUID _id) {
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		//player doesn't exist??
		if(!conf.isConfigurationSection(idString)) {
			try {
				//attempt to create him
				invokeCreatePlayerSpace(_id);
			} catch (EmpiresPlayerExistsException e) {//unlikely - but that's programming!
				e.printStackTrace();
			}
			
			return System.currentTimeMillis();//return default just set
		} else {
			//he exists
			return conf.getConfigurationSection(idString).getLong("pt");
		}
	}
	
	public void removeDormantPlayer(UUID _id) throws EmpiresJoinableDoesNotExistException {
		//remove us from any joinable we were in
		invokeRemovePlayerFromJoinedJoinable(_id);
		
		//remove our data
		YamlConfiguration conf = getFileConfiguration();
		
		//id to string for lookup
		String idString = _id.toString();
		
		conf.set(idString, null);
	}
	
	/**
	 * Not async safe
	 * @param _currentTime
	 */
	public void purgeDormantPlayers(long _currentTime) {
		YamlConfiguration conf = getFileConfiguration();
		
		for(String player : conf.getKeys(false)) {
			if(player.equals("data-version"))
				continue;
			
			if(conf.isConfigurationSection(player)) {
				long playTime = getPlayerLastPlayedTime(UUID.fromString(player));
				long timeDiff = (System.currentTimeMillis() - playTime)/(86400000);
				
				if(timeDiff >= 28) {//28 days
					//they're inactive
					try {
						removeDormantPlayer(UUID.fromString(player));
					} catch (EmpiresJoinableDoesNotExistException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
