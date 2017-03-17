package Jarretts_Prototype;

import static Jarretts_Prototype.ReverseOrder.reversed;

import java.util.ArrayList;
import java.util.List;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class UPStarcraft extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;

	public boolean spawnPoolExists;
	public boolean gasMorphing;
	public boolean cheesed;

	private List<Base> baseList;
	private List<Base> basesToRemove;
	private List<Unit> basesMade; //used for sorting
	
	private Army army;
	private boolean init;
	private int nextBaseID;

	public boolean rushing;
	public boolean zergDeath;

	private int expectedBases;
	private int zergsKilled;
	private int enemiesSeen;
	private int enemiesKilled;
	private List<Unit> enemiesWitnessed;
	private List<TilePosition> basePositions;
	private List<TilePosition> enemyBuiltPositions;

	private boolean notCapped;

	public Position enemyBase;

	int frames;

	//Static frame limit for how long we rush, most useful for 4-base maps
	//Dummied right now
	private final double RUSH_FAIL_HEURISTIC = 1;
	private final int AMT_BASES = Integer.MAX_VALUE;
	private final int SPEED = 20;
	private final int DEFENDERS_PER_BASE = 12;
	private final int EXPAND_WORKER_COUNT = 10;

	public static void main(String[] args) {
		new UPStarcraft().run();
	}

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onStart() {
		frames = 0;
		notCapped = true;
		expectedBases = 0;
		nextBaseID = 0;
		init = true;
		enemyBase = null;
		rushing = true;
		zergsKilled = 0;
		enemiesSeen = 0;
		enemiesKilled = 0;
		zergDeath = false;
		enemiesWitnessed = new ArrayList<Unit>();
		game = mirror.getGame();
		self = game.self();
		BWTA.readMap();
		BWTA.analyze();

		game.setLocalSpeed(SPEED);

		army = new Army(self, game, this);

		basePositions = new ArrayList<TilePosition>();
		for(Position pos : army.basePoss)
		{
			basePositions.add(pos.toTilePosition());
		}
		enemyBuiltPositions = new ArrayList<TilePosition>();

		System.out.println("Map name is " + game.mapFileName());
		baseList = new ArrayList<Base>();
		basesMade = new ArrayList<Unit>();
		basesToRemove = new ArrayList<Base>();
		List<Unit> myUnits = self.getUnits();
		Base initBase = null;
		spawnPoolExists = false;
		gasMorphing = false;
		cheesed = false;

		//search once for the base
		for(Unit u : myUnits)
			if(u.getType() == UnitType.Zerg_Hatchery)
			{
				initBase = new Base(this, self, game, u, true, nextBaseID);
				basesMade.add(u);
				nextBaseID++;
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

		frames++;

		if(frames > 100)//90000)
		{
			rushing = false;
			Base.setWorkerAmount(EXPAND_WORKER_COUNT);
		}

		if(self.supplyUsed() == 400 && notCapped)
		{
			System.out.println("Reached cap at frames " + frames);
			notCapped = false;
		}

		try{
			army.manage();

			if(!rushing && expectedBases + baseList.size() < AMT_BASES)
				assignBases();

			int allocatedMinerals = self.minerals();

			//Remove enemy bases from our list of places
			removeEnemyBases();
			
			//Each manage function returns the minerals it did not use for the next to use
			for(Base base : reversed(baseList))
			{
				if(base.exists())
					allocatedMinerals = base.manage(allocatedMinerals);
				else
					basesToRemove.add(base);
			}

			for(Base base : basesToRemove)
			{
				if(baseList.size() > 1)
				{
					basesMade.remove(base.getHatchery());
					baseList.get(basesMade.indexOf(findClosest(basesMade, base.getHatchery()))).inheritWorkers(base.getWorkers());
					baseList.remove(base);
				}
			}


			if(init) init = false; //should mean no duplicates in first frame are done
			if(rushing)
				rushing = calculateRush();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public int getSupplyUsed(){
		return self.supplyUsed();
	}

	public Unit getClosestBase(TilePosition tp){
		Unit ret = null;
		for(Unit b : basesMade){
			if(ret == null || ret.getTilePosition().getDistance(tp) > b.getTilePosition().getDistance(tp))
				ret = b;
		}
		return ret;
	}

	@Override
	public void onUnitCreate(Unit unit) {
		//only care about my stuff
		boolean mine = unit.getPlayer() == self;

		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Extractor && mine)//called even when morphing not done yet
		{ 
			//gasMorphing = true; //set flag to check cancel conditions
			//gasMorpher = unit;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool && mine)
		{
			//buildingPool = true;
		}
		else if ((unit.getType() == UnitType.Zerg_Drone ||
				unit.getType() == UnitType.Zerg_Zergling) && mine)
		{
			baseList.get(basesMade.indexOf(findClosest(basesMade, unit))).addUnit(unit);

		} 
		else if(unit.getType() == UnitType.Zerg_Overlord && mine)
		{
			army.addScout(unit);
		}
	}

	@Override
	public void onUnitMorph(Unit unit){
		//only care about my stuff
		boolean mine = unit.getPlayer() == self;

		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Extractor && mine)//called even when morphing not done yet
		{ 
			gasMorphing = true; //set flag to check cancel conditions
			baseList.get(0).gasMorpher = unit;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool && mine)
		{
			//buildingPool = true;
		}
		else if ((unit.getType() == UnitType.Zerg_Drone ||
				unit.getType() == UnitType.Zerg_Zergling) && mine) 
		{
			baseList.get(basesMade.indexOf(findClosest(basesMade, unit))).addUnit(unit);
		} 
		else if(unit.getType() == UnitType.Zerg_Overlord && mine)
		{
			army.addScout(unit);
		}
		else if(unit.getType() == UnitType.Zerg_Egg && mine)
		{
			if(unit.getBuildType() == UnitType.Zerg_Overlord);
						
			baseList.get(basesMade.indexOf(findClosest(basesMade, unit))).addEgg(unit);
		}
	}

	@Override
	public void onUnitComplete(Unit unit){
		if(init)
			; //Need to set up initial data structures
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool && unit.getPlayer() == self)
		{
			spawnPoolExists = true;
		}
		else if(unit.getType() == UnitType.Zerg_Extractor && unit.getPlayer() == self)
		{
			cheesed = true;
		}
		else if(unit.getType() == UnitType.Zerg_Hatchery && unit.getPlayer() == self)
		{
			for(Base base : baseList)
			{
				TilePosition target = base.getTarget();
				if(target != null)
					if(target.getDistance(unit.getTilePosition()) < 10 )
					{
						base.setTarget(null);
						base.nullify();
						//System.out.println("Nulled worker");
						break;
					}
			}
			Base b = new Base(this, self, game, unit, false, nextBaseID);
			baseList.add(b);
			basesMade.add(unit);
			expectedBases--;
			nextBaseID++;
		}
	}

	@Override
	public void onUnitDestroy(Unit unit){
		if(unit.getPlayer() == game.enemy())
		{
			enemiesKilled++;
			if(unit.getPosition().equals(enemyBase))
			{
				army.killedBase = true;
				System.out.println("Killed the base");
			}
		}
		else if(unit.getType() == UnitType.Zerg_Zergling && unit.getPlayer() == self)
		{
			zergsKilled++;
			if(!zergDeath && zergsKilled > 7)
				zergDeath = true;
		}
		else if(unit.getType() == UnitType.Zerg_Spawning_Pool && unit.getPlayer() == self)
			spawnPoolExists = false;
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
		troop.setController(this);		
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

	private static TilePosition findClosestPosition(List<TilePosition> list, Unit u)
	{
		TilePosition returnPos = null;
		int distance = Integer.MAX_VALUE;
		for(TilePosition pos : list)
		{
			if(pos.getDistance(u.getTilePosition()) < distance)
			{
				distance = (int) pos.getDistance(u.getTilePosition());
				returnPos = pos;
			}
		}
		return returnPos;
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
				baseList.get(0).setWorkerAmount(EXPAND_WORKER_COUNT);
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


	private void assignBases()
	{
		boolean expand = true;
		for(Base base : baseList){
			if(!base.expandable())
				expand = false;
		}

		Base base = baseList.get(baseList.size()-1);

//		System.out.println("Army size: " + army.size());
//		System.out.println("Base size: " + baseList.size());
		if(base.getTarget() == null && expand && army.size() >= baseList.size()*DEFENDERS_PER_BASE)
		{
			TilePosition basePos = findClosestPosition(basePositions, base.getHatchery());
			if(basePos != null)
			{
				base.setTarget(basePos);
				basePositions.remove(basePos);
				expectedBases++;
			}

			if(expectedBases + baseList.size() >= AMT_BASES)
				return;
		}
		else if(enemyBuiltPositions.contains(base.getTarget()))
		{
			base.nullify();
			expectedBases--;
		}
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

	private void removeEnemyBases()
	{
		for(Unit enemy : game.enemy().getUnits())
			if(basePositions.contains(enemy.getTilePosition()))
			{
				basePositions.remove(enemy.getTilePosition());
				enemyBuiltPositions.add(enemy.getTilePosition());
			}
	}
}

