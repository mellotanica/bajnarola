package org.bajnarola.game.model;

import java.util.ArrayList;
import java.util.List;

public class Player {

	public static final short PLAYER_N_MEEPLE = 7;
	Meeple meepleList[];
	List<Meeple> hand;
	short score, id;
	boolean scoreChanged;
	String name;
	
	public Player(String name, short id) {
		meepleList = new Meeple[PLAYER_N_MEEPLE];
		hand = new ArrayList<Meeple>();
		this.id = id;
		
		for (int i = 0; i < meepleList.length ; i++) {
			meepleList[i] = new Meeple(this);
			hand.add(meepleList[i]);
		}
		
		score = 0;
		scoreChanged = false;
		
		this.name = name;
	}
	
	public short getScore() {
		return score;
	}
	
	public String getName() {
		return name;
	}
	
	public short getId() {
		return id;
	}
	
	public short getUpdateScore() {
		scoreChanged = false;
		return score;
	}

	public void setScore(short score) {
		scoreChanged = true;
		this.score = score;
	}

	public boolean isScoreChanged() {
		return scoreChanged;
	}
	
	public int getMeepleCount() {
		return hand.size();
	}
	
	public boolean hasMeeples() {
		return (hand.size() > 0);
	}

	public Meeple getMeeple() {
		if(hand.size() > 0)
			return hand.remove(0);
		else
			return null;
	}
	
	public void giveMeepleBack(Meeple meeple){
		hand.add(meeple);
		meeple.setTileSide((short)-1);
		meeple.setTile(null);
	}
	
	public List<String> removeAllMeeple() {
		Tile t;
		List<String> coords = new ArrayList<String>();
		for (int i = 0; i < meepleList.length ; i++) {
			if ((t = meepleList[i].getTile()) != null) {
				coords.add(Board.getKey(t.getX(), t.getY()));
				t.getLSElement(meepleList[i].getTileSide()).removeOwner(this);
				t.removeMeeple();
			}
		}
		
		hand.clear();
		
		for (int i = 0; i < meepleList.length ; i++) {
			meepleList[i] = null;
		}
		
		meepleList = null;
		return coords;
	}
}
