package org.bajnarola.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public abstract class LandscapeElement {
	boolean completed;
	Map<Player, Integer> owners;
	List<Tile> tiles;
	Tile elementRoot;
	
	public LandscapeElement(Tile elementRoot, short tileSide) {
		completed = false;
		this.owners = new Hashtable<Player, Integer>();
		this.elementRoot = elementRoot;
		this.tiles = new ArrayList<Tile>();
		
		/* Link this landscape to the elementRoot */
		this.elementRoot.putLSElement(tileSide, this);
		tiles.add(elementRoot);
	}
	
	public abstract void merge(LandscapeElement el);
	public abstract void addTile(Tile t, short tileSide);
		
	public Tile getElementRoot() {
		return elementRoot;
	}
	
	public boolean isMeepleDeployable(Player player) {
		if (owners.isEmpty())
			return true;
		
		int max = Collections.max(owners.values());
		
		if (owners.get(player) != null && owners.get(player) == max)
			return true;
		
		return false;
	}
	
	public abstract int getValue(boolean endGame);
	
	public List<Tile> getTiles(){
		return this.tiles;
	}
	
	protected void complete() {
		completed = true;
	}

	public boolean isCompleted() {
		return completed;
	}
	
	/* Unlink the passed landscape from all its tiles which
	 * must link to the current landscape instead. 
	 * Add those tiles to the current landscape tiles list. */
	protected void relink(LandscapeElement el) {
		List<Tile> elTiles  = el.getTiles();
		
		Tile tileTmp;
		
		while (!elTiles.isEmpty()) {
			tileTmp = elTiles.remove(0);	

			for (short i = 0; i < Tile.SIDE_COUNT; i++) {
				if (tileTmp.getLSElement(i) != null && tileTmp.getLSElement(i).equals(el)) {
					tileTmp.popLSElement(i);
					tileTmp.putLSElement(i, this);
					if(tileTmp.hasMeeple() && tileTmp.getMeeple().getTileSide() == i)
						addMeeple(tileTmp.getMeeple());
				}
				
			}
			
			addTileInt(tileTmp);
			
		}
	}
	
	public List<Player> getScoreOwners(){
		List<Player> actOwners = new ArrayList<Player>();
		
		for (Player player : owners.keySet()) {
			if (isMeepleDeployable(player))
				actOwners.add(player);
		}
		
		return actOwners;
	}
	
	public Map<Player, Integer> getOwners(){
		return owners;
	}
	
	public void clear(){
		for (Tile t : tiles){
			t.unlinkLSElement(this);
		}
	}
	
	public void addMeeple(Meeple meeple) {
		
		int value = 1;
		if (owners.get(meeple.getOwner()) != null) {
			value = owners.remove(meeple.getOwner()) + 1;
		}
		owners.put(meeple.getOwner(), value);
	}
	
	public void removeOwner(Player owner){
		owners.remove(owner);
	}
	
	protected void addTileInt(Tile t){
		if(!tiles.contains(t))
			tiles.add(t);
	}
}
