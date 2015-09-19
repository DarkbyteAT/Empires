package com.pixelgriffin.empires.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.ChannelStorage;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.dthielke.herochat.MessageFormatSupplier;
import com.dthielke.herochat.MessageNotFoundException;
import com.dthielke.herochat.util.Messaging;
import com.pixelgriffin.empires.Empires;
import com.pixelgriffin.empires.enums.Relation;
import com.pixelgriffin.empires.exception.EmpiresJoinableDoesNotExistException;
import com.pixelgriffin.empires.handler.PlayerHandler;

/**
 * 
 * @author Nathan
 *
 */
public class AllyChat implements Channel {
	/*
	 * Thanks to the Factions team for pioneering this code.
	 */

	private static final Pattern msgPattern = Pattern.compile("(.*)<(.*)%1\\$s(.*)> %2\\$s");
	private final ChannelStorage chStore = Herochat.getChannelManager().getStorage();
	private final MessageFormatSupplier fmt = Herochat.getChannelManager();
	
	private Set<Relation> includedRelations = new HashSet<Relation>();
	
	public AllyChat() {
		includedRelations.add(Relation.US);
		includedRelations.add(Relation.E_K);
		includedRelations.add(Relation.ALLY);
	}
	
	@Override
	public boolean addMember(Chatter arg0, boolean arg1, boolean arg2) {
		if(arg0.hasChannel(this))
			return false;
		
		arg0.addChannel(this, arg1, arg2);
		
		return true;
	}
	
	//Removed addWorld() function, unused
	/*
	public void addWorld(String arg0) {
		
	}
	*/

	@Override
	public void announce(String arg0) {
		arg0 = applyFormat(this.fmt.getAnnounceFormat(), "").replace("%2$s", arg0);
		
		for(Chatter c : this.getMembers()) {
			c.getPlayer().sendMessage(arg0);
		}
		
		Herochat.logChat(ChatColor.stripColor(arg0));
	}

	@Override
	public String applyFormat(String arg0, String arg1) {
		arg0 = arg0.replace("{default}", this.fmt.getStandardFormat());
		arg0 = arg0.replace("{name}", this.getName());
		arg0 = arg0.replace("{nick}", this.getNick());
		arg0 = arg0.replace("{color}", this.getColor().toString());
		arg0 = arg0.replace("{msg}", "%2$s");
		
		Matcher m = AllyChat.msgPattern.matcher(arg1);
		if ((m.matches()) && (m.groupCount() == 3))
		{
			arg0 = arg0.replace("{sender}", m.group(1) + m.group(2) + "%1$s" + m.group(3));
		}
		else
		{
			arg0 = arg0.replace("{sender}", "%1$s");
		}
		
		arg0 = arg0.replaceAll("(?i)&([a-fklmno0-9])", "§$1");
		
		return arg0;
	}

	@Override
	public String applyFormat(String format, String originalFormat, Player sender) {
		format = applyFormat(format, originalFormat);
		format = format.replace("{plainsender}", sender.getName());
		format = format.replace("{world}", sender.getWorld().getName());
		Chat chat = Herochat.getChatService();
		if (chat != null)
		{
			try
			{
				String prefix = chat.getPlayerPrefix(sender);
				String suffix = chat.getPlayerSuffix(sender);
				String group = chat.getPrimaryGroup(sender);
				String groupPrefix = group == null ? "" : chat.getGroupPrefix(sender.getWorld(), group);
				String groupSuffix = group == null ? "" : chat.getGroupSuffix(sender.getWorld(), group);
				format = format.replace("{prefix}", prefix == null ? "" : prefix.replace("%", "%%"));
				format = format.replace("{suffix}", suffix == null ? "" : suffix.replace("%", "%%"));
				format = format.replace("{group}", group == null ? "" : group.replace("%", "%%"));
				format = format.replace("{groupprefix}", groupPrefix == null ? "" : groupPrefix.replace("%", "%%"));
				format = format.replace("{groupsuffix}", groupSuffix == null ? "" : groupSuffix.replace("%", "%%"));
			}
			catch (UnsupportedOperationException ignored) {}
		}
		else
		{
			format = format.replace("{prefix}", "");
			format = format.replace("{suffix}", "");
			format = format.replace("{group}", "");
			format = format.replace("{groupprefix}", "");
			format = format.replace("{groupsuffix}", "");
		}
		format = format.replaceAll("(?i)&([a-fklmno0-9])", "§$1");
		return format;
	}

	@Override
	public void attachStorage(ChannelStorage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean banMember(Chatter arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void emote(Chatter arg0, String arg1) {
		arg1 = applyFormat(this.fmt.getEmoteFormat(), "").replace("%2$s", arg1);
		Set<Player> rec = new HashSet<Player>();
		
		for(Chatter c : this.getMembers()) {
			rec.add(c.getPlayer());
		}
		
		trim(rec, arg0);
		
		final Player p = arg0.getPlayer();
		
		if(!isMessageHeard(rec, arg0)) {
			Bukkit.getScheduler().runTaskLater(Herochat.getPlugin(), new Runnable() {

				@Override
				public void run() {
					try {
						Messaging.send(p, Herochat.getMessage("channel_alone"));
					} catch(MessageNotFoundException e) {
						Herochat.severe("Missing channel_alone");
					}
				}
				
			}, 1L);
		} else {
			for(Player ip : rec) {
				ip.sendMessage(arg1);
			}
		}
		
	}
	
	public boolean isMessageHeard(Set<Player> rec, Chatter sen) {
		if(!isLocal())
			return true;
		
		Player sp = sen.getPlayer();
		for(Player ip : rec) {
			if(ip.equals(sp))
				continue;
			
			if(ip.hasPermission("herochat.admin.stealth"))
				return true;
		}
		
		return false;
		
	}
	
	public void trim(Set<Player> rec, Chatter sen) {
		World w = sen.getPlayer().getWorld();
		
		Set<Chatter> mem = this.getMembers();
		Iterator<Player> it = rec.iterator();
		while(it.hasNext()) {
			Chatter r = Herochat.getChatterManager().getChatter(it.next());
			if(r == null)
				continue;
			
			World rw = r.getPlayer().getWorld();
			
			if(!mem.contains(r))
				it.remove();
			else if(isLocal() && (!sen.isInRange(r, this.getDistance())))
				it.remove();
			else if(!hasWorld(rw))
				it.remove();
			else if(r.isIgnoring(sen))
				it.remove();
			else if((!this.isCrossWorld()) && (!w.equals(rw)))
				it.remove();
		}
	}

	@Override
	public Set<String> getBans() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public ChatColor getColor() {
		return ChatColor.DARK_PURPLE;
	}

	@Override
	public int getDistance() {
		return 0;
	}

	@Override
	public String getFormat() {
		return "{color}[&l&f{nick} {color}{sender}{color}] &f{msg}";
		//return "{color}[&l{nick}&r{color} &l{civ_role}&r{civ_rel}{civ_name}{sender}{color}] &f{msg}";
	}

	@Override
	public MessageFormatSupplier getFormatSupplier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Chatter> getMembers() {
		Set<Chatter> ret = new HashSet<Chatter>();
		
		for(Chatter c : Herochat.getChatterManager().getChatters()) {
			if(c.hasChannel(this))
				ret.add(c);
		}
		
		return ret;
	}

	@Override
	public Set<String> getModerators() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getMutes() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return "Ally";
	}

	@Override
	public String getNick() {
		return "A";
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public ChannelStorage getStorage() {
		return this.chStore;
	}

	@Override
	public Set<String> getWorlds() {
		Set<String> ret = new HashSet<String>();
		for(World w : Bukkit.getWorlds()) {
			ret.add(w.getName());
		}
		
		return ret;
	}

	@Override
	public boolean hasWorld(String arg0) {
		return true;
	}

	@Override
	public boolean hasWorld(World arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isBanned(String arg0) {
		return false;
	}

	@Override
	public boolean isCrossWorld() {
		return true;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isMember(Chatter arg0) {
		return this.getMembers().contains(arg0);
	}

	@Override
	public boolean isModerator(String arg0) {
		return this.getModerators().contains(arg0.toLowerCase());
	}

	@Override
	public boolean isMuted() {
		return false;
	}

	@Override
	public boolean isMuted(String arg0) {
		if(this.isMuted())
			return true;
		
		return this.getMutes().contains(arg0.toLowerCase());	// TODO Auto-generated method stub
	}

	@Override
	public boolean isShortcutAllowed() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isTransient() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVerbose() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean kickMember(Chatter arg0, boolean arg1) {
		if(!arg0.hasChannel(this))
			return false;
		
		this.removeMember(arg0, true, false);
		
		if(arg1) {
			try {
				announce(Herochat.getMessage("channel_kick").replace("$1", arg0.getPlayer().getDisplayName()));
			} catch (MessageNotFoundException e) {
				Herochat.severe("Missing channel_kick");
			}
		}
		
		return true;
	}

	@Override
	public void onFocusGain(Chatter arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFocusLoss(Chatter arg0) {
	}

	@Override
	public void processChat(ChannelChatEvent arg0) {
		final Player p = arg0.getSender().getPlayer();
		
		String format = applyFormat(arg0.getFormat(), arg0.getBukkitFormat(), p);
		Chatter sen = Herochat.getChatterManager().getChatter(p);
		Set<Player> rec = this.getRecipients(p);//null
		
		trim(rec, sen);
		
		String msg = String.format(format, p.getDisplayName(), arg0.getMessage());
		if(!isMessageHeard(rec, sen)) {
			Bukkit.getServer().getScheduler().runTaskLater(Herochat.getPlugin(), new Runnable() {

				@Override
				public void run() {
					try {
						Messaging.send(p, Herochat.getMessage("channel_alone"));
					} catch(MessageNotFoundException e) {
						Herochat.severe("Missing channel_alone");
					}
				}
				
			}, 1L);
		}
		
		for(Player r : rec) {
			r.sendMessage(msg);
		}
		
		Herochat.logChat(msg);
	}
	
	public Set<Player> getRecipients(Player sen) {
		Set<Player> ret = new HashSet<Player>();
		
		//gather joined name
		String joinedName = Empires.m_playerHandler.getPlayerJoinedCivilization(sen.getUniqueId());
		
		//is it wilderness?
		if(!joinedName.equalsIgnoreCase(PlayerHandler.m_defaultCiv)) {
			try {
				//gather our relations
				Set<String> relations = Empires.m_joinableHandler.getJoinableRelationNameSet(joinedName);
				ArrayList<UUID> players = new ArrayList<UUID>();
				
				for(String relation : relations) {
					//if the relation is one of the recipient types
					if(includedRelations.contains(Empires.m_joinableHandler.getJoinableRelationTo(joinedName, relation))) {
						//gather players from relation
						players.addAll(Empires.m_joinableHandler.getJoinableJoinedPlayers(relation));
						
						Player p;
						for(UUID player : players) {
							//are they online?
							p = Bukkit.getPlayer(player);
							
							if(p != null) {
								ret.add(p);//they are a recipient
							}
						}
					}
				}
				
				//add us
				players.clear();
				players.addAll(Empires.m_joinableHandler.getJoinableJoinedPlayers(joinedName));
				
				Player p;
				for(UUID player : players) {
					p = Bukkit.getPlayer(player);
					
					if(p != null) {
						ret.add(p);
					}
				}
				
			} catch (EmpiresJoinableDoesNotExistException e) {
				e.printStackTrace();
				
				sen.sendMessage(ChatColor.RED + "Something went wrong!");
			}
			
		}
		
		//return a set of recipients
		return ret;
	}

	@Override
	public boolean removeMember(Chatter arg0, boolean arg1, boolean arg2) {
		if(!arg0.hasChannel(this))
			return false;
		
		arg0.removeChannel(this, arg1, arg2);
		return true;
	}

	//Removed functions
	/*
	@Override
	public void removeWorld(String arg0) {
	}

	@Override
	public void sendRawMessage(String arg0) {
	}

	@Override
	public void setBanned(String arg0, boolean arg1) {
	}

	@Override
	public void setBans(Set<String> arg0) {
	}

	@Override
	public void setColor(ChatColor arg0) {
	}

	@Override
	public void setCrossWorld(boolean arg0) {
	}

	@Override
	public void setDistance(int arg0) {
	}

	@Override
	public void setFormat(String arg0) {
	}
	@Override
	public void setModerator(String arg0, boolean arg1) {
	}

	@Override
	public void setModerators(Set<String> arg0) {
	}

	@Override
	public void setMuted(boolean arg0) {
	}

	@Override
	public void setMuted(String arg0, boolean arg1) {
	}

	@Override
	public void setMutes(Set<String> arg0) {
	}

	@Override
	public void setNick(String arg0) {
	}

	@Override
	public void setPassword(String arg0) {
	}

	@Override
	public void setShortcutAllowed(boolean arg0) {
	}

	@Override
	public void setVerbose(boolean arg0) {
	}

	@Override
	public void setWorlds(Set<String> arg0) {
	}
    */

}
