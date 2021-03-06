package org.bajnarola.game.view;

public class RelativeSizes {
	public enum Resolutions {
		R_800x600,
		R_1280x720,
		R_1920x1080,
		R_FULLSCREEN
	}
	
	private static RelativeSizes instance = null;
	private Resolutions activeResolution = Resolutions.R_1280x720;
	
	private RelativeSizes(){}
	
	public static RelativeSizes getInstance(){
		if(instance == null)
			instance = new RelativeSizes();
		return instance;
	}
	
	public void setResolution(Resolutions resolution, int width, int height){
		if(resolution == Resolutions.R_FULLSCREEN){
			int wDelta = Math.abs(width - 800), hDelta = Math.abs(height - 600), wReso = 800, hReso = 600;
			if(Math.abs(width - 1280) < wDelta){
				wDelta = width - 1280;
				wReso = 1280;
			}
			if(Math.abs(width - 1920) < wDelta){
				wDelta = width - 1920;
				wReso = 1920;
			}
			
			if(Math.abs(height - 720) < hDelta){
				hDelta = height - 720;
				hReso = 720;
			}
			if(Math.abs(height - 1080) < hDelta){
				hDelta = height - 1080;
				hReso = 1080;
			}
			
			wReso = Math.min(wReso, hReso);
			switch(wReso){
				case 800:
				case 600:
					activeResolution = Resolutions.R_800x600;
					break;
				case 1280:
				case 720:
					activeResolution = Resolutions.R_1280x720;
					break;
				case 1920:
				case 1080:
					activeResolution = Resolutions.R_1920x1080;
					break;
			}
		} else
			activeResolution = resolution;
	}
	
	public float mainFontSize(){
		switch(activeResolution){
			case R_800x600:
				return 22f;
			case R_1920x1080:
				return 35f;
			case R_1280x720:
			default:
				return 25f;
		}
	}
	
	public float buttonsFontSize(){
		switch(activeResolution){
			case R_800x600:
				return 40f;
			case R_1920x1080:
				return 65f;
			case R_1280x720:
			default:
				return 50f;
		}
	}
	
	public float bannerYHeightDisplacement(){
		switch(activeResolution){
			case R_800x600:
				return 3f/5f;
			case R_1920x1080:
				return 3f/5f;
			case R_1280x720:
			default:
				return 3f/5f;
		}
	}
	
	public int handMeepleXOffset(){
		switch(activeResolution){
			case R_800x600:
				return 8;
			case R_1920x1080:
				return 20;
			case R_1280x720:
			default:
				return 15;
		}
	}
	
	public int handMeepleYOffset(){
		switch(activeResolution){
			case R_800x600:
				return 10;
			case R_1920x1080:
				return 20;
			case R_1280x720:
			default:
				return 15;
		}
	}
		
	public int scoresXOffset(){
		switch(activeResolution){
			case R_800x600:
				return 10;
			case R_1920x1080:
				return 30;
			case R_1280x720:
			default:
				return 20;
		}
	}
	
	public int scoresYOffset(){
		switch(activeResolution){
			case R_800x600:
				return 10;
			case R_1920x1080:
				return 30;
			case R_1280x720:
			default:
				return 20;
		}
	}
	
	public int scoreOverHeight(){
		switch(activeResolution){
			case R_800x600:
				return 5;
			case R_1920x1080:
				return 10;
			case R_1280x720:
			default:
				return 8;
		}
	}
	
	//TODO: fix these sizes
	
	public float scrollAreaRateo(){
		switch(activeResolution){
			case R_800x600:
				return 0.05f;
			case R_1920x1080:
				return 0.1f;
			case R_1280x720:
			default:
				return 0.07f;
		}
	}
	
	public int viewShiftOffset(){
		switch(activeResolution){
			case R_800x600:
				return 2;
			case R_1920x1080:
				return 10;
			case R_1280x720:
			default:
				return 4;
		}
	}
}
