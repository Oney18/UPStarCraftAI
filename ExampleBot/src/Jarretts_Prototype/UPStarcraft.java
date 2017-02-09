package Jarretts_Prototype;

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
	
	public boolean spawnPoolExists;
	private List<Base> baseList;
	private List<Unit> basesMade; //used for sorting
	private Army army;
	private boolean init;
	
	public static void main(String[] args) {
		new UPStarcraft().run();
	}
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}
	
	@Override
	public void onStart() {
		init = true;
		game = mirror.getGame();
		self = game.self();
		BWTA.readMap();
		BWTA.analyze();
		
		game.setLocalSpeed(4);
		
		army = new Army(self, game);
		baseList = new ArrayList<Base>();
		basesMade = new ArrayList<Unit>();
		List<Unit> myUnits = self.getUnits();
		Base initBase = null;
		spawnPoolExists = false;
		
		//search once for the base
		for(Unit u : myUnits)
			if(u.getType() == UnitType.Zerg_Hatchery)
			{
				initBase = new Base(this, self, game, u);
				basesMade.add(u);
				break;
			}
		
		//search again to get the initial workers
		for(Unit u : myUnits)
			if(u.getType() == UnitType.Zerg_Drone)
				initBase.addUnit(u);
			else if(u.getType() == UnitType.Zerg_Overlord)
				army.setScout(u);
		
		baseList.add(initBase);
				
		
	}
	
	@Override
	public void onFrame() {
		if(game.isPaused())
			return;

		try{
			army.manage();
			for(Base base: baseList)
				base.manage();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		init = false; //should mean no duplicates in first frame are done
	}
	
	@Override
	public void onUnitCreate(Unit unit) {
		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Extractor)//called even when morphing not done yet
		{ 
			//gasMorphing = true; //set flag to check cancel conditions
			//gasMorpher = unit;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			//buildingPool = true;
		}
		else if (unit.getType() == UnitType.Zerg_Drone ||
				 unit.getType() == UnitType.Zerg_Zergling) 
		{
			baseList.get(basesMade.indexOf(findClosest(basesMade, unit))).addUnit(unit);
		} 
		else if(unit.getType() == UnitType.Zerg_Overlord)
		{
			army.setScout(unit);
		}
	}
	
	@Override
	public void onUnitMorph(Unit unit){
		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Extractor)//called even when morphing not done yet
		{ 
			//gasMorphing = true; //set flag to check cancel conditions
			//gasMorpher = unit;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			//buildingPool = true;
		}
		else if (unit.getType() == UnitType.Zerg_Drone ||
				 unit.getType() == UnitType.Zerg_Zergling) 
		{
			baseList.get(basesMade.indexOf(findClosest(basesMade, unit))).addUnit(unit);
		} 
		else if(unit.getType() == UnitType.Zerg_Overlord)
		{
			army.setScout(unit);
		}
	}
	
	@Override
	public void onUnitComplete(Unit unit){
		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool)
		{
			spawnPoolExists = true;
		}
		else if(unit.getType() == UnitType.Zerg_Extractor)
		{
			//cheesed = true;
		}
		else if(unit.getType() == UnitType.Zerg_Hatchery)
		{
			Base b = new Base(this, self, game, unit);
			baseList.add(b);
		}
	}
	@Override
	public void onUnitDestroy(Unit unit){
		
	}
	
	public void newTroop(Troop troop)
	{
		army.addTroop(troop);
	}
	
	private static Unit findClosest(List<Unit> list, Unit u)
	{
		Unit returnUnit = null;
		int distance = Integer.MAX_VALUE;
		for(Unit unit : list)
		{
			if(unit.getPosition().getApproxDistance(u.getPosition()) < distance)
			{
				distance = unit.getPosition().getApproxDistance(u.getPosition());
				returnUnit = unit;
			}
		}
		return returnUnit;
	}
}

