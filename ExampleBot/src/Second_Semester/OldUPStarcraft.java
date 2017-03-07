package Second_Semester;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class OldUPStarcraft extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;
	
	private int minerals;
	private boolean spawnPoolExists = false;
	private ArrayList<Base> baseList = new ArrayList<Base>();
	private Unit scout;
	private Enemy enemy;
	private Position enemyBase;
	private Army army;
	
	public static void main(String[] args) {
		new OldUPStarcraft().run();
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
		
		//create lists of visible enemy units/buildings
		List<Unit> enemyBlds = new ArrayList<Unit>();
		List<Unit> enemyWorkers = new ArrayList<Unit>();
		List<Unit> enemyCores = new ArrayList<Unit>();
		List<Unit> enemyProblems = new ArrayList<Unit>();
		
		for(Unit unit : game.enemy().getUnits())
		{
			if(unit.exists() && unit.isVisible() && unit.isDetected())
			{
				if(unit.getType().isWorker())
				{
					enemyWorkers.add(unit);
				}
				else if(unit.getType().canAttack())
				{
					enemyProblems.add(unit);
				}
				else if(unit.getType().isBuilding())
				{
					if(unit.getType() == UnitType.Zerg_Hatchery 
							|| unit.getType() == UnitType.Terran_Command_Center
							|| unit.getType() == UnitType.Protoss_Nexus)
					{
						enemyCores.add(unit);
						if(enemyBase == null)
							enemyBase = unit.getPosition();
					}
					else
						//TODO make a way to sort buildings based on priority. those that can attack back should be higher.
						enemyBlds.add(unit);
					}
					
				}
		}
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

