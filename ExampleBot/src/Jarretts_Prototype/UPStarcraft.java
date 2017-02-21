package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class UPStarcraft extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;

	public boolean spawnPoolExists;
	public boolean gasMorphing;
	public boolean cheesed;

	private List<Base> baseList;
	private List<Unit> basesMade; //used for sorting
	private Army army;
	private boolean init;
	
	public boolean rushing;
	private boolean zergDeath;
	private int zergsKilled;
	private int frameCount;
	private int enemiesSeen;
	private int enemiesKilled;
	private List<Unit> enemiesWitnessed;
	
	//Static frame limit for how long we rush, most useful for 4-base maps
	//Dummied right now
	private final int RUSH_FRAME_COUNT = Integer.MAX_VALUE;
	private final double RUSH_FAIL_HEURISTIC = 1;

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
		rushing = true;
		zergsKilled = 0;
		frameCount = 0;
		enemiesSeen = 0;
		enemiesKilled = 0;
		zergDeath = false;
		enemiesWitnessed = new ArrayList<Unit>();
		game = mirror.getGame();
		self = game.self();
		BWTA.readMap();
		BWTA.analyze();

		game.setLocalSpeed(20);

		army = new Army(self, game, this);
		//System.out.println("DOES THIS WORK");

		System.out.println("Map name is " + game.mapFileName());
		baseList = new ArrayList<Base>();
		basesMade = new ArrayList<Unit>();
		List<Unit> myUnits = self.getUnits();
		Base initBase = null;
		spawnPoolExists = false;
		gasMorphing = false;
		cheesed = false;

		//search once for the base
		for(Unit u : myUnits)
			if(u.getType() == UnitType.Zerg_Hatchery)
			{
				initBase = new Base(this, self, game, u, true);
				basesMade.add(u);
				break;
			}

		//search again to get the initial workers
		for(Unit u : myUnits)
			if(u.getType() == UnitType.Zerg_Drone)
				initBase.addUnit(u);
			else if(u.getType() == UnitType.Zerg_Overlord)
				army.addScout(u);

		baseList.add(initBase);
		//System.out.println("Added Base to LIst");
	}	


	@Override
	public void onFrame() {
		if(game.isPaused())
			return;

		try{
			army.manage();
			for(Base base: baseList)
				base.manage();
		

		if(init) init = false; //should mean no duplicates in first frame are done
		if(rushing)
			rushing = calculateRush();
		frameCount++;
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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
			army.addScout(unit);
		}
	}

	@Override
	public void onUnitMorph(Unit unit){
		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Extractor)//called even when morphing not done yet
		{ 
			gasMorphing = true; //set flag to check cancel conditions
			baseList.get(0).gasMorpher = unit;
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
			army.addScout(unit);
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
			cheesed = true;
		}
		else if(unit.getType() == UnitType.Zerg_Hatchery)
		{
			Base b = new Base(this, self, game, unit, false);
			baseList.add(b);
		}
	}
	@Override
	public void onUnitDestroy(Unit unit){
		if(unit.getPlayer() == game.enemy())
			enemiesKilled++;
		else if(unit.getType() == UnitType.Zerg_Zergling && unit.getPlayer() == self)
		{
			zergsKilled++;
			if(!zergDeath && zergsKilled > 7)
				zergDeath = true;
		}
	}
	
	
	private int games = 0;
	private int wins = 0;
	@Override
    public void onEnd(boolean isWinner)
    {
		games++;
    	if(isWinner) wins++;
    	else System.out.println("We lost this one.");
    	System.out.println("Game ended: " + wins + "/" + games);
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

	private boolean calculateRush()
	{
		List<Unit> enemies = game.enemy().getUnits();

		//System.out.println("Num enemies visible is " + game.enemy().visibleUnitCount() + " and our seen counter is " + enemiesSeen+ " and we killed " + enemiesKilled);
		
		if(!enemies.isEmpty() && game.enemy().visibleUnitCount() != 0)
		{
			for(Unit enemy : enemies)
				if(!enemiesWitnessed.contains(enemy))
				{
					enemiesWitnessed.add(enemy);
					enemiesSeen++;
				}
			return true;
		}
		else if(enemiesSeen != 0 && zergDeath && zergsKilled > 0)//enemies is empty cause we see none; initial push failed
		{
			double killRatio = ((double) enemiesKilled) / ((double) zergsKilled);
			
			if(killRatio < RUSH_FAIL_HEURISTIC)
			{
				System.out.println("THE RUSH HAS FAILED");
				System.out.println("The KD ratio is " + killRatio);
				baseList.get(0).setWorkerAmount(11);
				baseList.get(0).decrementWorkers();
				return false;
			}
			
			if(game.enemy().visibleUnitCount() == 0)
			{
				enemiesKilled = 0;
				zergsKilled = 0;
			}
		}
		
		return true; //haven't seen enemies yet
	}
	
	public Unit getWorker()
	{
		for(Base b : baseList)
		{
			Unit worker = b.getWorker();
			if(worker != null)
				return worker;
		}
		return null;
	}
}

