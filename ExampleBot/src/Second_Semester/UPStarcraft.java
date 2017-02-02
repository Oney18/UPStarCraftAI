import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class UPStarcraft extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;
	
	private int minerals;
	private boolean spawnPoolExists = false;
	private ArrayList<Base> baseList = new ArrayList<Base>();
	private Unit scout;
	private Enemy enemy;
	private Army army;
	
	public static void main(String[] args) {
		new UPStarcraft().run();
	}
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}
	
	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		BWTA.readMap();
		BWTA.analyze();
	}
	
	@Override
	public void onFrame() {
		if(game.isPaused())
			return;
	}
	
	@Override
	public void onUnitCreate(Unit unit) {
		
	}
	
	@Override
	public void onUnitMorph(Unit unit){
		
	}
	
	@Override
	public void onUnitComplete(Unit unit){
		
	}
	@Override
	public void onUnitDestroy(Unit unit){
		
	}
}

