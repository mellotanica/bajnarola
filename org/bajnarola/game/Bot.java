package org.bajnarola.game;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bajnarola.game.controller.BotController;
import org.bajnarola.game.controller.GameControllerRemote;
import org.bajnarola.lobby.LobbyClient;
import org.bajnarola.networking.NetPlayer;
import org.bajnarola.utils.BajnarolaRegistry;
import org.newdawn.slick.SlickException;

//TODO: if run from jar, game client's registry is unreachable from the lobby

public class Bot {
	
	private static int seed; 
	
	public static final boolean debugPlay = false;
	
	public static void main(String[] argv) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		LobbyClient     iLobby  = null;
		BajnarolaServer iServer = null;
		BotClient iClient = null;
		
		boolean okLobby = false;
		
		String username = "";
		String lobbyHost;
		int lobbyPort;
		
		
		if(argv.length < 1){
			System.err.println("At least Bot name is needed");
			System.exit(1);
		}
		username = argv[0];
		if(argv.length >= 2)
			lobbyHost = argv[1];
		else
			lobbyHost = "localhost";
		if(argv.length >= 3)
			lobbyPort = Integer.parseInt(argv[2]);
		else
			lobbyPort = BajnarolaRegistry.DEFAULT_LOBBY_PORT;
		
		Map<String, NetPlayer> players = null;


		System.out.println("Bot \""+username+"\" starting up.");
		
		BotController gBoard = null;
		try {
			gBoard = new BotController();
		} catch (RemoteException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.print("Personal board set up...");
		System.out.println("OK!");
		
		
		while (true) {
			try {
				seed = -1;
				okLobby = false;
				
				gBoard.reinitLock.lock();
				gBoard.reinit();
				
				while (!okLobby) {
					
					System.out.print("Server start up:");
					if (!username.isEmpty())
						iServer = new BajnarolaServer(username, gBoard);
					else
						iServer = new BajnarolaServer(gBoard);
					
					gBoard.setMyServer(iServer);
					
					System.out.println("OK!");
								
					System.out.print("Client module initilization:");
					iClient = new BotClient();
					System.out.println("OK!");
					
					System.out.println("Registering to lobby at " + lobbyHost + "...");
					try {
						iLobby = new LobbyClient(lobbyHost, lobbyPort);
					} catch (Exception e1) {
						iLobby = null;
						iServer = null;
						System.err.println("Can't reach the lobby server");
						//e1.printStackTrace();
						Thread.sleep(5000);
						continue;
					}
					System.out.println("OK!");
					
					System.out.print("Joining the default room...");
					try {
						/* Join the lobby and set neighbours list.
						 * If there is any error, try again. */
						players = iLobby.join(iServer.getPlayer());
						okLobby = true;
					} catch (RemoteException e) {
						iServer = null;
						iLobby = null;
						iClient = null;
						
						if (e.getMessage().contains("User")) {
							System.err.println("User already exists");
						} else if (e.getMessage().contains("Game")){
							System.err.println("Game already started");
						} else {
							System.err.println("Can't connect to the lobby");
							//e.printStackTrace();
						}
						Thread.sleep(5000);
					}
				}
				
				
				iClient.getPlayers(players);
				
				System.out.println("OK!");
				
				System.out.println("Beginning game with " + iClient.players.size() + " players.");
				
				/* Get others dice throws */
				Map<String,Integer> dices;
				
				dices = iClient.multicastInvoke(GameControllerRemote.class.getMethod("getDiceValue"));
				/* Sorting players based on dice throws */
				iClient.sortPlayerOnDiceThrow(dices);
				
				System.out.println("Got players:");
				for (String k : iClient.players.keySet()) {
					System.out.println("\t" + k + " with dice throw: " + dices.get(k));
					if (seed == -1) {
						seed = dices.get(k);
						gBoard.gameId = Integer.toString(seed) + k;
						System.out.println(gBoard.gameId);
					}
				}
				
				System.out.print("Initializing the board...");
				
				List<String> playerNames = new ArrayList<>(iClient.players.keySet());
				
				gBoard.initBoard(iServer.getPlayer().username, playerNames, seed);
				System.out.println("OK");
				
				iClient.mainLoop(iServer.getPlayer().username, gBoard);
			} catch (SlickException | NoSuchMethodException | InterruptedException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
		}
	}
}
