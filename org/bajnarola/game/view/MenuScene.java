package org.bajnarola.game.view;

import java.util.List;

import org.bajnarola.game.view.Gui.bg_type;
import org.bajnarola.game.view.Gui.scene_type;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;

public class MenuScene extends IScene {
	
	Button startButton, optionButton, exitButton;
	
	public MenuScene(Gui guiManager, Image background, bg_type backgroundType, List<Music> soundtrack) throws SlickException{
		super(guiManager, background, backgroundType, soundtrack);
		sceneType = scene_type.SCENE_MENU;
		
		startButton = new Button(guiManager.windowWidth/3, guiManager.windowHeight/9, guiManager.windowWidth/2, guiManager.windowHeight/4, 
				"Start");
		
		optionButton = new Button(guiManager.windowWidth/3, guiManager.windowHeight/9, guiManager.windowWidth/2, guiManager.windowHeight/4*2,
				"Options");
		
		exitButton = new Button(guiManager.windowWidth/3, guiManager.windowHeight/9, guiManager.windowWidth/2, guiManager.windowHeight/4*3,
				"Exit");
	}

	@Override
	public void render(GameContainer gc, Graphics g) {
		guiManager.drawBackground(background, backgroundType);
		startButton.draw();
		optionButton.draw();
		exitButton.draw();
	}

	@Override
	public void leftClick(int x, int y) {
		if(startButton.isClicked(x, y))
			guiManager.switchScene(scene_type.SCENE_LOBBY);
		else if(optionButton.isClicked(x, y))
			guiManager.switchScene(scene_type.SCENE_OPTIONS);
		else if(exitButton.isClicked(x, y))
			guiManager.exit();
	}

	@Override
	public void rightClick(int x, int y) {
	}

	@Override
	public void wheelMoved(boolean up) {
	}

	@Override
	public void enterPressed() {
	}

	@Override
	public void escPressed() {
	}

	@Override
	public void backspacePressed() {
	}

	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy) {
		startButton.isClicked(newx, newy);
		optionButton.isClicked(newx, newy);
		exitButton.isClicked(newx, newy);
	}

	@Override
	public void leftRelease(int x, int y) {
	}

	@Override
	public void rightRelease(int x, int y) {
	}

	@Override
	public void keyPressed(int key, char c) {
	}

	@Override
	public void keyReleased(int key, char c) {
	}

}
