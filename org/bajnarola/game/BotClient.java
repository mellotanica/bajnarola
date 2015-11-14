/*****************************************************************************/
/* Client package for Bajnarola distributed game                             */
/*                                                                           */
/* Copyright (C) 2015                                                        */
/* Marco Melletti, Davide Berardi, Matteo Martelli                           */
/*                                                                           */
/* This program is free software; you can redistribute it and/or             */
/* modify it under the terms of the GNU General Public License               */
/* as published by the Free Software Foundation; either version 2            */
/* of the License, or any later version.                                     */
/*                                                                           */
/* This program is distributed in the hope that it will be useful,           */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of            */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             */
/* GNU General Public License for more details.                              */
/*                                                                           */
/* You should have received a copy of the GNU General Public License         */
/* along with this program; if not, write to the Free Software               */
/* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,*/
/* USA.                                                                      */
/*****************************************************************************/

package org.bajnarola.game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.reflect.Method;

import org.bajnarola.game.controller.BotController;
import org.bajnarola.game.controller.BotController.endGameCause;
import org.bajnarola.game.controller.GameControllerRemote;
import org.bajnarola.game.controller.TurnDiff;
import org.bajnarola.networking.NetPlayer;
import org.bajnarola.utils.BajnarolaRegistry;

import java.util.Collections;
import java.util.List;

public class BotClient {
	Map<String,GameControllerRemote> players;

	public BotClient() {
		this.players = new LinkedHashMap<String,GameControllerRemote>();
	}
	
	public void getPlayers(Map<String,NetPlayer> playersStrings) {
		NetPlayer p;
		for (String user : playersStrings.keySet()) {
			GameControllerRemote bc;
			try {
				p = playersStrings.get(user);
				/* Get the IP from the NetPlayer (filled by the lobby server 
				 * at the join invocation) and get the RemoteRegistry by that IP.*/ 
				
				bc = (GameControllerRemote) BajnarolaRegistry.getRegistry(p.host, p.bindPort).lookup(p.rmiUriBoard);

				System.out.println("user: " + user + " host: " + p.host + " bindPort: " + p.bindPort);
				
				this.players.put(user, bc);
			} catch (NotBoundException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				/* crash! */
				System.out.println("No answer from " + user);
			}
		}
	}
	
	public void sortPlayerOnDiceThrow(Map<String, Integer> dices) {

		Map<String,GameControllerRemote> tmpplayers;
		List<Entry<String,Integer>> tmpl;
		
		tmpl = new LinkedList <Entry<String,Integer>>(dices.entrySet());		
		tmpplayers = new LinkedHashMap<String,GameControllerRemote>();

		
		Collections.sort(tmpl,
			new Comparator<Entry<String,Integer>>() {
				public int compare(Entry<String,Integer> e1, Entry<String,Integer> e2) {
					return e1.getValue().compareTo(e2.getValue());
				}
			}
		);
		
		tmpplayers.putAll(this.players);
		this.players.clear();
		
		for (Entry<String,Integer> tmpe : tmpl) {
			String player = tmpe.getKey();
			this.players.put(player, tmpplayers.get(player));
		}
		
		tmpplayers.clear();
	}
	
	@SuppressWarnings("unchecked")
	public <T> Map<String,T> multicastInvoke(Method m) {
		GameControllerRemote cGameBoard;
		Map <String,T> retMap = new Hashtable<String,T>();
		T retVal;
		
		for (String user : this.players.keySet()) {
			cGameBoard = (GameControllerRemote) this.players.get(user);
			
			try {
				 retVal = (T) m.invoke(cGameBoard);
				 retMap.put(user, retVal);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return retMap;
	}
	
	/* Game main loop */
	public void mainLoop(String myUsername, BotController myBc) {
		Boolean gameEnded = false;
		endGameCause cause = null;
		TurnDiff dState = null;
		GameControllerRemote othBc = null;
		List<String> deadPlayers = new ArrayList<String>();

		/* While the game is running and there are 2 or more players. */
		while (!gameEnded && !myBc.isReinitRequested()) {
			/* For Every player */
			for (String cPlayer : this.players.keySet()) {
				
				if (cPlayer.equals(myUsername)) {
					/* It's the turn of this player */
					System.out.println("My turn! " + myBc.myPlayedTurn);
					
					if (!(gameEnded = myBc.isDeckEmpty()))
						myBc.localPlay(myUsername);
					
				} else {
					/* Call the other players and kindly ask them to play */
					othBc = this.players.get(cPlayer);

					System.out.println("Turn of " + cPlayer + " waiting for a response...");
					
					try {
						if (!(gameEnded = myBc.isDeckEmpty())) {
							dState = othBc.play(myBc.myPlayedTurn + 1, myBc.gameId);
							if(!dState.playerName.equals(cPlayer) || dState.turn != myBc.myPlayedTurn)
								throw new Exception("invalid state diff");
							myBc.myPlayedTurn++;
							myBc.updateBoard(dState);
						}
					} catch(Exception e) {
						/* CRASH! */
						System.err.println("Node Crash! (" + cPlayer + "): "+e.getMessage());
						deadPlayers.add(cPlayer);
						myBc.removePlayer(cPlayer);
					}
				}
				
				/* If this player is the only remaining one, end the game */
				if((players.size() - deadPlayers.size()) <= 1){
					gameEnded = true;
					cause = endGameCause.lastPlayer;
					break;
				}
			}
			
			/* Garbage collecting the crashed players */
			for (String dPlayer : deadPlayers)
				this.players.remove(dPlayer);
			deadPlayers.clear();
			
			if(players.size() <= 1){
				gameEnded = true;
				cause = endGameCause.lastPlayer;
			}
			
			if (gameEnded && !myBc.isReinitRequested()) {
				
				Map<String, Integer> scores = myBc.finalCheckScore();
				boolean winner = false;
				
				if (cause == endGameCause.lastPlayer) {
					System.out.println("Game ended as I am the last player");
					winner = true;
				} else {
					cause = endGameCause.deckEmpty;
					System.out.println("Game ended for empty deck");
					
					int max = Collections.max(scores.values());
					if (scores.get(myUsername) == max)
						winner = true;
				}
				
				/* Notify the view */
				myBc.endGame(cause, scores, winner);
			}
		}
	}
}