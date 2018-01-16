package org.bajnarola.game.controller;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.bajnarola.game.BajnarolaServer;
import org.bajnarola.game.MainClass;
import org.bajnarola.game.model.Board;
import org.bajnarola.game.model.City;
import org.bajnarola.game.model.Cloister;
import org.bajnarola.game.model.LandscapeElement;
import org.bajnarola.game.model.Meeple;
import org.bajnarola.game.model.Player;
import org.bajnarola.game.model.Street;
import org.bajnarola.game.model.Tile;
import org.newdawn.slick.SlickException;

import sun.misc.Lock;

public class BotController extends UnicastRemoteObject implements
		GameControllerRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public enum endGameCause {
		notEnded,
		deckEmpty,
		lastPlayer
	}
	
	endGameCause endCause;
	Map<String, Integer> finalScores;
	boolean winner;
	BajnarolaServer myServer;
	
	Board board;
	Lock diceLock;
	public Lock reinitLock;
	ReentrantLock playLock;
	Integer diceValue;
	Random randomGenerator;
	Condition waitCondition;
	TurnDiff myTurnDiff = null;
	
	public String gameId = null;
		
	boolean reinit = false;
	
	public int myPlayedTurn = 0;

	private void throwDice() {
		this.diceValue = this.randomGenerator.nextInt();
		this.diceLock.unlock();
	}

	public BotController() throws RemoteException {
		this.randomGenerator = new Random();
		
		this.finalScores = new Hashtable<String, Integer>();
		
		reinitLock = new Lock();
		
		init();
		
	}

	private void init() {
		this.board = new Board();
		this.playLock = new ReentrantLock();
		this.diceLock = new Lock();
		try {
			this.diceLock.lock();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.waitCondition = this.playLock.newCondition();

		this.winner = false;
		this.gameId = null;
		this.endCause = endGameCause.notEnded;
		this.myPlayedTurn = -1;
		this.diceValue = null;
		this.throwDice();
	}
	
	public void reinit() throws SlickException {
		if (reinit) {
			init();
			finalScores.clear();
			this.reinit = false;
		}
	}
	
	public void setMyServer(BajnarolaServer s) {
		this.myServer = s;
	}
	
	@Override
	public Integer getDiceValue() throws RemoteException {
		if (this.diceValue == null) {
			try {
				this.diceLock.lock();
				this.diceLock.unlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return this.diceValue;
	}

	@SuppressWarnings("unused")
	public boolean isDeckEmpty() {
		if(MainClass.debugPlay && board.debugPlayTiles <= 0)
			return true;
		return board.getDeck().isEmpty();
	}

	public endGameCause getEndCause() {
		return endCause;
	}

	public Map<String, Integer> getFinalScores() {
		return finalScores;
	}

	public boolean amIWinner() {
		return winner;
	}
	

	@Override
	public TurnDiff play(Integer turn, String gameId) throws RemoteException {
		if (this.gameId == null || !this.gameId.equals(gameId))
			throw new RemoteException("unmatched GameID");
			
		this.playLock.lock();
		try {
			if (this.myPlayedTurn < turn) {
				System.out.println("Asking for turn " + turn);
				this.waitCondition.await();
			} else if (this.myPlayedTurn > turn) {
				System.err.println("Wrong turn request: myTurn="
						+ this.myPlayedTurn + " turn: " + turn);
				/* throw new Exception("Wrong turn"); */
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.playLock.unlock();
		}
		if(isReinitRequested())
			throw new RemoteException("Player left the game");
		return this.myTurnDiff;
	}

	public void removePlayer(String playerName){
		board.removePlayer(playerName);
	}
	
	public void requestReinit(){
		this.reinit = true;
		reinitLock.unlock();
		try{
			playLock.lock();
			waitCondition.signalAll();
			playLock.unlock();
		} catch (Exception e){}
	}
	
	public boolean isReinitRequested() {
		return this.reinit;
	}
	
	/*
	 * Update the internal board status after a turn has been 
	 * played by another player. */
	public void updateBoard(TurnDiff diff) throws Exception {
		Player p;
		Tile tile;
		Meeple meeple;

		tile = board.beginTurn();
		for(int i = 0; i < diff.tileDirection; i++)
			tile.rotate(true);

		p = board.getPlayerByName(diff.playerName);
		if (p == null)
			throw new Exception("Error: missing player");

		if (!board.probe(diff.x, diff.y, tile))
			throw new Exception("Error: illegal tile placement");

		board.place(diff.x, diff.y, tile);

		if (diff.meepleTileSide > -1) {
			meeple = p.getMeeple();
			meeple.setTileSide(diff.meepleTileSide);
			if (!board.probeMeeple(tile, meeple))
				throw new Exception("Error: illegal meeple placement");

			board.placeMeeple(tile, meeple);
		}

		board.endTurn(tile);
	}
	
	private void createTileLandscape(Tile tile, short x, short y, short side) {
		short citiesCount, streetsCount;
		citiesCount = tile.countElement(Tile.ELTYPE_CITY);
		streetsCount = tile.countElement(Tile.ELTYPE_STREET);
		
		Tile tmpTile;
		/* Create a new landscape if it does not exist yet.*/
		switch(tile.getElements()[side]){
			case Tile.ELTYPE_CITY:
				if (citiesCount <= 2)
					new City(tile, side);
				else if (tile.getLSElement(side) == null) {
					LandscapeElement nc = new City(tile, side);
					for (short j = 0; j < Tile.SIDE_COUNT; j++)
						if (tile.getElements()[j] == Tile.ELTYPE_CITY && j != side)
							tile.putLSElement(j, nc);
				}	
				break;
			case Tile.ELTYPE_STREET:
				if (streetsCount != 2)
					new Street(tile, side);
				else if (tile.getLSElement(side) == null) {
					/* If there is not a landscape for this side yet, 
					 * create it and spalmate it*/
					LandscapeElement ns = new Street(tile, side);
					for (short j = 0; j < Tile.SIDE_COUNT; j++)
						if (tile.getElements()[j] == Tile.ELTYPE_STREET && j != side)
							tile.putLSElement(j, ns);
				}	
				break;
			case Tile.ELTYPE_CLOISTER:
				Cloister c = new Cloister(tile, side);
				for (short j = -1; j <= 1; j++) {
					for (short k = -1; k <= 1; k++) {
						if (j != k || j != 0) {
							tmpTile = board.getTile((short)(x+j), (short)(y+k));
							if (tmpTile != null)
								c.addTile(tmpTile, (short)-1);
						}
					}
				}
				break;					
		}
	}
	
	private boolean checkPlacement(short x, short y, int meepleCount, short meeplePos, Tile tile, Meeple meeple){
		if(meeplePos > -1){
			if(meeple == null)
				return false;
			if(tile.getElements()[meeplePos] == Tile.ELTYPE_GRASS)
				return false;
			//if meeple can be placed upon this landscape
			LandscapeElement ls = tile.getLSElement(meeplePos);
			Map<Player, Integer> owners = new Hashtable<Player, Integer>(), to;
			for(short i = 0; i < Tile.SIDE_COUNT; i++){
				if(i != Tile.SIDE_CENTER && tile.getLSElement(i) != null && tile.getLSElement(i).equals(ls)){
					if(board.getNeighbourTile(x, y, i) != null && board.getNeighbourTile(x, y, i).getLSElement(Board.getNeighbourSide(i)) != null){
						to = board.getNeighbourTile(x, y, i).getLSElement(Board.getNeighbourSide(i)).getOwners();
						for(Player p : to.keySet()){
							if(owners.containsKey(p)){
								owners.put(p, owners.remove(p)+to.get(p));
							} else {
								owners.put(p, to.get(p));
							}
						}
					}
				}
			}
			int max = 0;
			for(Integer i : owners.values()){
				if(i > max)
					max = i;
			}
			int maxcount = 0;
			for(Integer i : owners.values()){
				if(i == max)
					maxcount++;
			}
			if(!owners.isEmpty() && owners.get(meeple.getOwner()) != null && (owners.get(meeple.getOwner()) < max || (owners.get(meeple.getOwner()) == max && maxcount == 1)))
				return false;
			
		}
		return true;
	}
	
	private boolean checkStreetClose(short x, short y, Tile tile, short meeplePos){
		Street ls = (Street) tile.getLSElement(meeplePos), tls;
		LandscapeElement nls;
		Tile nb;
		
		int streetEnds = ls.getStreetEnds();
		
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			if(i != Tile.SIDE_CENTER && (nls = tile.getLSElement(i)) != null && nls.equals(ls)){
				nb = board.getNeighbourTile(x, y, i);
				if(nb == null)
					continue;
				tls = (Street) nb.getLSElement(Board.getNeighbourSide(i));
				if(tls != null)
					streetEnds += tls.getStreetEnds();
			}
		}
		
		if(streetEnds == 2)
			return true;
		return false;
	}
	
	private boolean checkCityClose(short x, short y, Tile tile, short meeplePos){
		Tile nb;
		LandscapeElement nls;
		City ls = (City) tile.getLSElement(meeplePos), tls;
		
		int openSides = ls.getOpenSides();
		
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			if(i != Tile.SIDE_CENTER && (nls = tile.getLSElement(i)) != null && nls.equals(ls)){
				nb = board.getNeighbourTile(x, y, i);
				if(nb == null)
					continue;
				tls = (City) nb.getLSElement(Board.getNeighbourSide(i));
				if(tls != null)
					openSides += tls.getOpenSides()-2;
			}
		}
		
		if(openSides == 0)
			return true;
		return false;
	}

	private boolean checkCloisterClose(short x, short y, Tile tile){
		int tcount = 1;
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
				if(i!=0 && j!=0)
					if(board.getTile((short)(x+i), (short)(y+j)) != null)
						tcount++;
			}
		}
		if(tcount == 9)
			return true;
		return false;
	}
	
	private boolean probeCloisterClose(int x, int y){
		int tcount = 2;
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
				if(i!=0 && j!=0)
					if(board.getTile((short)(x+i), (short)(y+j)) != null)
						tcount++;
			}
		}
		if(tcount >= 9)
			return true;
		return false;
	}
	
	private boolean checkLandscapeClose(short x, short y, Tile tile, short meeplePos, short lsType){
		switch(lsType){
			case Tile.ELTYPE_CITY:
				return checkCityClose(x, y, tile, meeplePos);
			case Tile.ELTYPE_CLOISTER:
				return checkCloisterClose(x, y, tile);
			case Tile.ELTYPE_STREET:
				return checkStreetClose(x, y, tile, meeplePos);
		}
		return false;
	}

	private List<LandscapeElement> getMyClosedLandscapes(short x, short y, Tile tile, short meeplePos, Player owner){
		List<LandscapeElement> myls = new ArrayList<LandscapeElement>();		
		List<LandscapeElement> checked = new ArrayList<LandscapeElement>();
		LandscapeElement ls, ols;
		Map<Player, Integer> owners;
		int maxval;
		
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			if((ls = tile.getLSElement(i)) != null && !checked.contains(ls)){
				checked.add(ls);
				if(checkLandscapeClose(x, y, tile, i, tile.getElements()[i])){
					ols = board.getNeighbourTile(x, y, i).getLSElement(Board.getNeighbourSide(i));
					owners = ols.getOwners();
					maxval = 0;
					for(Integer val : owners.values()){
						if(val > maxval)
							maxval = val;
					}
					if(owners.get(owner) != null && (owners.get(owner)+(meeplePos == i ? 1 : 0)) >= maxval){
						myls.add(ols);
						if(!myls.contains(ls))
							myls.add(ls);
					}
				}
			}
		}
		
		Tile t;
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
				if(i!=0 && j!=0 && (t = board.getTile((short)(x+i), (short)(y+j))) != null 
						&& t.getElements()[Tile.SIDE_CENTER] == Tile.ELTYPE_CLOISTER 
						&& t.getMeeple() != null && t.getMeeple().getOwner().equals(owner) 
						&& t.getMeeple().getTileSide() == Tile.SIDE_CENTER && probeCloisterClose(x+i, y+j)){
					myls.add(t.getLSElement(Tile.SIDE_CENTER));
				}
			}
		}
		
		return myls;
	}
	
	private int getOtherCompletedLandscapes(short x, short y, Tile tile, short meeplePos, Player owner, List<LandscapeElement> mine){
		List<LandscapeElement> checked = new ArrayList<LandscapeElement>();
		LandscapeElement ls, ols;

		int value = 0;
		
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			if((ls = tile.getLSElement(i)) != null && !checked.contains(ls) && !mine.contains(ls)){
				checked.add(ls);
				if(checkLandscapeClose(x, y, tile, i, tile.getElements()[i])){
					ols = board.getNeighbourTile(x, y, i).getLSElement(Board.getNeighbourSide(i));
					value += ols.getValue(false) + ls.getValue(false);
				}
			}
		}
		
		Tile t;
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
				if(i!=0 && j!=0 && (t = board.getTile((short)(x+i), (short)(y+j))) != null 
						&& t.getElements()[Tile.SIDE_CENTER] == Tile.ELTYPE_CLOISTER 
						&& t.getMeeple() != null && !t.getMeeple().getOwner().equals(owner) 
						&& t.getMeeple().getTileSide() == Tile.SIDE_CENTER && probeCloisterClose(x+i, y+j)){
					value += 9;
				}
			}
		}
		
		return value;
	}
	
	private int getFreedMeeples(short x, short y, Tile tile, Player owner){
		int mcount = 0;
		Integer tval;
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			if(i != Tile.SIDE_CENTER && checkLandscapeClose(x, y, tile, i, tile.getElements()[i])){
				if((tval = board.getNeighbourTile(x, y, i).getLSElement(Board.getNeighbourSide(i)).getOwners().get(owner)) != null)
					mcount += tval;
			}
		}
		Tile t;
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
				if(i!=0 && j!=0 && (t = board.getTile((short)(x+i), (short)(y+j))) != null 
						&& t.getElements()[Tile.SIDE_CENTER] == Tile.ELTYPE_CLOISTER && (t.getMeeple()) != null 
						&& t.getMeeple().getOwner().equals(owner) && t.getMeeple().getTileSide() == Tile.SIDE_CENTER && probeCloisterClose(x+i, y+j)){
					mcount ++;
				}
			}
		}
		
		return mcount;
	}
	
	private Placement evaluateSinglePlacement(short x, short y, int meepleCount, short meeplePos, Tile tile, Meeple meeple, Player owner){
		Tile t = tile.clone();
		
		for(short i = 0; i < Tile.SIDE_COUNT; i++){
			createTileLandscape(t, x, y, i);
		}
		
		if(!checkPlacement(x, y, meepleCount, meeplePos, t, meeple))
			return null;
		
		Placement pl = new Placement(x, y, tile.getDirection(), meeplePos);
		float meepleFactor = (float)(((meeplePos == -1 ? Player.PLAYER_N_MEEPLE : 0) - meepleCount)/(float)Player.PLAYER_N_MEEPLE);
		
		int myMeeplesRemoved = getFreedMeeples(x, y, t, owner); 
		List<LandscapeElement> myClosedLandscapes = getMyClosedLandscapes(x, y, t, meeplePos, owner);
		int lsValues = 0;
		for(LandscapeElement ls : myClosedLandscapes){
			lsValues += ls.getValue(false);
		}
		int myOpenedLandscapes = 0;
		for(LandscapeElement ls : t.getLSElements()){
			if(!myClosedLandscapes.contains(ls))
				myOpenedLandscapes++;
		}
		float openLsFactor = (float)myOpenedLandscapes * meepleFactor;
		float meepleRemovedValue = (float)myMeeplesRemoved * (1/meepleFactor);
		int othersCompletedLandscapes = getOtherCompletedLandscapes(x, y, t, meeplePos, owner, myClosedLandscapes);
		
		if(tile.getElements()[Tile.SIDE_CENTER] == Tile.ELTYPE_CLOISTER && meeplePos == Tile.SIDE_CENTER)
			pl.setValue(1+lsValues+meepleRemovedValue-othersCompletedLandscapes);
		else
			pl.setValue(((1+lsValues)*openLsFactor*meepleFactor)+meepleRemovedValue-othersCompletedLandscapes);
			
		return pl;
	}
	
	private List<Placement> evaluate(short x, short y, int meepleCount, Tile tile, Meeple meeple, Player owner){
		List<Placement> tilepl = new ArrayList<BotController.Placement>();
		Placement pl;
		
		pl = evaluateSinglePlacement(x, y, meepleCount, (short)-1, tile, meeple, owner);
		if(pl != null)
			tilepl.add(pl);
		
		if(meepleCount > 0)
			for(short i = 0; i < Tile.SIDE_COUNT; i++){
				pl = evaluateSinglePlacement(x, y, meepleCount, i, tile, meeple, owner);
				if(pl != null)
					tilepl.add(pl);
			}
		
		return tilepl;
	}
	
	public void localPlay(String me) {
		this.playLock.lock();

		System.out.print("Playing...");

		Tile tile = board.beginTurn();
		Player botPlayer = board.getPlayerByName(me);
		Meeple m = botPlayer.getMeeple();
		int meeples = botPlayer.getMeepleCount(); 
		List<String> holes = board.getHoles();
		List<Placement> possibilities = new ArrayList<Placement>();
		
		Placement pl = null;
		float maxv = Float.NEGATIVE_INFINITY;
		
		for(int i = 0; i < 4; i++){
			for(String p : holes){
				if(board.probe(getX(p), getY(p), tile))
					possibilities.addAll(evaluate(getX(p), getY(p), meeples, tile, m, botPlayer));
			}
			tile.rotate(true);
		}
		
		if(!possibilities.isEmpty()){
			for(Placement p : possibilities){
				if(p.value > maxv){
					maxv = p.value;
					pl = p;
				}
			}
		}
		
		while(tile.getDirection() != pl.direction)
			tile.rotate(true);
		
		board.place(pl.x, pl.y, tile);
		
		if(pl.meepleSide != -1 && m != null){
			m.setTileSide(pl.meepleSide);
			if(board.probeMeeple(tile, m))
				board.placeMeeple(tile, m);
		} else if(m != null){
			m.getOwner().giveMeepleBack(m);
		}
		
		board.endTurn(tile);

		short meepleTileSide = -1;
		if (tile.getMeeple() != null)
			meepleTileSide = tile.getMeeple().getTileSide();

		myTurnDiff = new TurnDiff(tile.getX(), tile.getY(),
				(short) tile.getDirection(), meepleTileSide, me, (short)myPlayedTurn);

		System.out.println("OK");

		this.myPlayedTurn++;
		this.waitCondition.signalAll();

		this.playLock.unlock();
	}

	public Map<String,Integer> finalCheckScore() {
		Map<String,Integer> scores = new Hashtable<String, Integer>();
		board.finalCheckScores();
		
		for (Player p : board.getPlayers())
			scores.put(p.getName(), (int)p.getScore());
		
		return scores;
	}

	
	/* This is called when the game ends */
	public void endGame(endGameCause cause, Map<String, Integer> scores, boolean winner) {
		this.endCause = cause;
		this.finalScores = scores;
		this.winner = winner;
		
		System.out.println((winner ? "I won!" : "I have lost"));
		System.out.println("Scores:");
		for(String s : scores.keySet()){
			System.out.println(s+ ": "+scores.get(s));
		}
	}


	public void initBoard(String playerName, List<String> playerNames, int seed) {
		board.initBoard(playerNames, seed);
	}
	
	public void cleanRegistry() {
		if (this.myServer != null)
			this.myServer.cleanRegistry();
	}
	
	private static short getX(String coords){
		return (short)Integer.parseInt(coords.split(";")[0]);
	}
	
	private static short getY(String coords){
		return (short)Integer.parseInt(coords.split(";")[1]);
	}
	
	private class Placement{
		public short x, y, meepleSide;
		public float value;
		public int direction;
		
		public Placement(short x, short y, int direction, short meepleSide, float value){
			this.x = x;
			this.y = y;
			this.direction = direction;
			this.meepleSide = meepleSide;
			this.value = value;
		}
		
		public Placement(short x, short y, int direction, short meepleSide){
			this(x, y, direction, meepleSide, (short)0);
		}
		
		public void setValue(float value){
			this.value = value;
		}
	}
	
}
