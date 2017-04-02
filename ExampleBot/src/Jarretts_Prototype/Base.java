package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class Base {

	private UPStarcraft controller;
	private Player self;
	private Game game;
	private Unit hatchery;
	private WorkerManager workerManager;
	private Troop troop;
	private boolean buildingOverlord;

	private boolean gettingPoolWorker;
	private boolean buildingPool;
	private Unit poolMorpherDrone;
	private TilePosition poolPos;
	private int workersExpected;
	private TilePosition baseTarget;
	private TilePosition altTarget;

	public Unit gasMorpher;
	private boolean doExtractor;
	private boolean gasMorphing2;
	private boolean gasMorphingStarted;
	private int frames;
	private Unit baseWorker;
	private int baseFrames;
	private boolean buildingBase;
	private boolean builtInitDrone;
	private int baseID;
	private int baseDist;
	private int baseCounter;

	private List<Unit> eggs;	//eggs being morphed by base
	private List<Unit> orderedLarvae; //larvae that have been ordered

	private Random RNGesus;

	private static int WORKER_AMOUNT;

	private final boolean debug = false;

	//If doExtractor is true, it is the first base
	public Base(UPStarcraft controller, Player self, Game game, Unit hatchery, boolean doExtractor, int baseID){
		this.controller = controller;
		this.hatchery = hatchery;
		this.self = self;
		this.game = game;
		this.doExtractor = doExtractor;
		this.baseID = baseID;
		buildingPool = false;
		gettingPoolWorker = false;
		baseCounter = 0;
		baseDist = 1;

		if(doExtractor)
			WORKER_AMOUNT = 4;

		eggs = new ArrayList<Unit>();
		orderedLarvae = new ArrayList<Unit>();
		builtInitDrone = false;
		RNGesus = new Random();
		buildingBase = false;
		baseWorker = null;
		baseTarget = null;
		baseFrames = 0;
		frames = 0;
		buildingOverlord = false;
		workerManager = new WorkerManager(self, game, doExtractor);
		troop = new Troop(game, hatchery.getPosition());
		controller.newTroop(troop);

		//This is set to account for the starting drones we get
		if(doExtractor)
			workersExpected = 4;
		else
			workersExpected = 0;

		gasMorphingStarted = false;
		gasMorphing2 = false;
		gasMorpher = null;
	}

	public int manage(int minerals)
	{	

		if(baseWorker != null && baseWorker.exists() && true)
		{
			int x = Math.max(baseWorker.getPosition().getX() - 325, 0);
			int y = Math.max(baseWorker.getPosition().getY() - 200, 0);
			game.setScreenPosition(new Position(x, y));
			//game.setLocalSpeed(20);
		}


		if(!controller.rushing)
		{
			workersExpected = 0;
			buildingOverlord = false;
			List<Unit> badEggs = new ArrayList<Unit>();

			for(Unit egg : eggs)
				if(egg.getType() != UnitType.Zerg_Egg)
					badEggs.add(egg);
				else if(egg.getBuildType() == UnitType.Zerg_Drone)
					workersExpected++;
				else if(egg.getBuildType() == UnitType.Zerg_Overlord) 
				{
					buildingOverlord = true;
				}

			eggs.removeAll(badEggs);
		}


		List<Unit> larvae = hatchery.getLarva();

		//		//check for ordered larvae
		//		for(Unit used : orderedLarvae)
		//			if(larvae.contains(used))
		//				larvae.remove(used);

		game.drawTextMap(hatchery.getPosition(), "allocatedMin: " + minerals + "\nnumWorkers : " + workerManager.getNumWorkers() + "\nzergs: " + troop.getSize() + 
				"\nnumLarva: " + larvae.size() + "\nexpectedWorkers: " + workersExpected + "\nworkerCountStat: " + WORKER_AMOUNT + "\nEggs: " + eggs.size());


		if(poolPos != null && !controller.spawnPoolExists)
			game.drawCircleMap(poolPos.toPosition(), 10, Color.Yellow, true);

		workerManager.manage();

		if(!controller.spawnPoolExists && (controller.poolAssigned == baseID || controller.poolAssigned == -1))
			makeSpawnPool(minerals);

		if(doExtractor)
			extractorTrick(minerals);

		if(!controller.rushing)
			minerals = expand(minerals);



		//Initial drone building
		if(doExtractor && !builtInitDrone && minerals >= 50)
		{
			Unit larva = larvae.get(0);
			if(larva.morph(UnitType.Zerg_Drone))
				orderedLarvae.add(larva); //morph and add to list if successful
			workersExpected++;

			larvae.remove(larva);
			minerals -= 50;

			builtInitDrone = true;
		}

		//OVERLORDS
		if((self.supplyTotal() == 2 && !buildingOverlord) || (minerals >= 100 && larvae.size() == 3 && self.supplyUsed()>=self.supplyTotal()-1 && !buildingOverlord))
		{
			if(larvae.size()>0)
			{
				Unit larva = larvae.get(0); //get a larvae
				if(larva.morph(UnitType.Zerg_Overlord))
					orderedLarvae.add(larva); //morph and add to list if successful

				if(debug)
				{
					System.out.println("Tried to morph overlord at base " + baseID);
					System.out.println(" on frame: " + game.getFrameCount());
				}

				larvae.remove(0); //remove from array
				minerals -= 100;
				buildingOverlord = true;
			}
		}
		//else if ()

		//build workers to get to 4?
		if(workerManager.getNumWorkers() + workersExpected < WORKER_AMOUNT && minerals  >= 50 && !larvae.isEmpty() && self.supplyUsed() < self.supplyTotal()-1)
		{
			if(debug)
				System.out.println("Larvae Size: " + larvae.size());

			Unit larva = larvae.get(0);

			if(debug)
			{
				System.out.print("Trying to morph drone ID: " + larva.getID());
				System.out.println(" on frame: " + game.getFrameCount());
			}

			larva.morph(UnitType.Zerg_Drone);
			workersExpected++;
			larvae.remove(larva);
			minerals =- 50;

			if(debug)
				System.out.println("Tried to morph worker, base " + baseID);
		}

		//ZERGLINGS
		while(controller.spawnPoolExists && minerals >= 50 && self.supplyUsed() <= self.supplyTotal()-2 && !larvae.isEmpty() && self.supplyUsed() < self.supplyTotal()-1/*&& troop.getSize() < 16*/)
		{
			Unit larva = larvae.get(0); //get a larvae
			larvae.remove(0); //remove from array
			larva.morph(UnitType.Zerg_Zergling);
			minerals =- 50;
		}

		return minerals;
	}

	private void makeSpawnPool(int minerals)
	{
		//Declare to controller that this base is making the pool
		if(controller.poolAssigned == -1)
		{
			controller.poolAssigned = baseID;
			System.out.println("Base " + baseID + " is building the pool");
		}

		// build a spawning pool when we can
		if ( !buildingPool && minerals >= 180 && workerManager.getNumWorkers()>0) 
		{
			if(minerals <200 && (!gettingPoolWorker || (poolMorpherDrone != null && !poolMorpherDrone.exists()))) //between 150 and 200 then
			{
				poolMorpherDrone = workerManager.getWorker();

				if(poolMorpherDrone != null)
				{		
					poolPos = findPoolPos();
					if(poolPos != null)
						poolMorpherDrone.move(poolPos.toPosition());
				}
				gettingPoolWorker = true;
			}
			else if(minerals >=200 && poolMorpherDrone != null) //anything over 200
			{
				if(!buildingPool && poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
				{
					poolMorpherDrone.build(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
					minerals -= 200;

					if(self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0)
					{
						buildingPool = true;
						System.out.println("Set building pool to true");
					}
				}
				else if(!buildingPool && !poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
				{
					frames++;
					if(frames > 50)
					{
						poolMorpherDrone.move(poolPos.toPosition());
						frames = 0;
					}
				}
				else if(!buildingPool && poolMorpherDrone.getTilePosition().getDistance(poolPos) > 2)
					//&& !poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition())
				{
					//maybe still moving, let it keep moving to the spot we found works
					poolMorpherDrone.move(poolPos.toPosition());
					//if you command it to move, it gets mad and won't obey. too many commands
				}
			}						
		}
	}

	private void extractorTrick(int minerals)
	{
		if ((controller.gasMorphing && self.supplyUsed() == 16 && !controller.cheesed) || gasMorphing2) 
		{
			gasMorphing2 = true;
			if(gasMorpher.canCancelMorph() && self.supplyUsed() == 18)
			{
				gasMorpher.cancelMorph();
				workersExpected--;
				gasMorphingStarted = false;
				controller.cheesed = true;
			}
			else
			{
				controller.cheesed = true;
			}
		}
		else if (self.allUnitCount(UnitType.Zerg_Extractor) < 1 && minerals >= 50 
				&& self.supplyUsed() == 18 && !gasMorphingStarted && !controller.cheesed) 
		{
			if(workerManager.getNumWorkers() > 0)
			{
				makeGas();
			}
			minerals -= 50;

			gasMorphingStarted = true;
		}
	}

	private void makeGas()
	{
		Unit builder = workerManager.getWorker();
		// make sure the builder is not null
		if(builder != null)
		{	
			Unit closestGeyser = null;
			Position startArea = BWTA.getStartLocation(self).getPosition();

			//loop through game's geysers, get closest to our base
			for(Unit geyser : game.getGeysers() ){
				if(game.canBuildHere(geyser.getTilePosition(), UnitType.Zerg_Extractor, builder, true)){

					//calculate distance, if less then is closes
					if(closestGeyser == null || closestGeyser.getDistance(startArea) > geyser.getDistance(startArea)){
						closestGeyser = geyser;
					}					
				}
			}

			builder.build(UnitType.Zerg_Extractor, closestGeyser.getTilePosition());
			gasMorphingStarted = true;
			workersExpected++;
		}
	}

	public void addUnit(Unit unit)
	{
		if(unit.getType() == UnitType.Zerg_Drone)
		{
			workerManager.addWorker(unit);
			workersExpected--;
		}
		else if (unit.getType() == UnitType.Zerg_Zergling)
			troop.addUnit(unit);
	}

	public TilePosition findPoolPos()
	{
		boolean minLeft = false;

		Unit closestMineral = null;
		int x = hatchery.getTilePosition().getX();
		int y = hatchery.getTilePosition().getY() + 2;

		Unit closestGeyser = null;

		//loop through game's geysers, get closest to our base
		for(Unit geyser : game.getGeysers() ){

			//calculate distance, if less then is closes

			if(closestGeyser == null || closestGeyser.getDistance(hatchery.getPosition()) > geyser.getDistance(hatchery.getPosition())){
				closestGeyser = geyser;
			}					
		}

		for(Unit mineral : game.getMinerals() ){

			//calculate distance, if less then is closes
			if(closestMineral == null || closestMineral.getDistance(hatchery.getPosition()) > mineral.getDistance(hatchery.getPosition())){
				closestMineral = mineral;
			}					
		}

		if(hatchery.getTilePosition().getX() > closestMineral.getTilePosition().getX())
			//minerals are to the left
			minLeft = true;

		if(minLeft)
		{
			x += 6;

			if(hatchery.getTilePosition().getX() - closestGeyser.getTilePosition().getX() < -4)
			{
				//geyser is in the way
				y += 3;
				x -= 2;
			}
		}
		else
		{
			x -= 3;
			if(hatchery.getTilePosition().getX() - closestGeyser.getTilePosition().getX() > 4)
			{
				//geyser is in the way
				y += 3;
				x += 1;
			}
		}

		return new TilePosition(x, y);

	}

	private int expand(int minerals)
	{
		if(baseTarget == null)
			return minerals;

		game.drawCircleMap(baseTarget.toPosition(), 10, Color.Green, true);

		if(altTarget != null)
			game.drawCircleMap(altTarget.toPosition(), 10, Color.Orange, true);

		//get the worker
		if((baseWorker == null || !baseWorker.exists()) && minerals > 270)
		{
			baseWorker = workerManager.getWorker();
		}
		//run to the base
		if(baseWorker != null && baseWorker.exists() && baseWorker.getTilePosition().getDistance(baseTarget) > 3)
		{
			baseWorker.move(baseTarget.toPosition());
		}

		//build the base
		else if(baseWorker != null && baseWorker.exists() && minerals >= 300 && baseWorker.canBuild(UnitType.Zerg_Hatchery))
		{
			baseFrames++;
			if(baseFrames == 50)
			{
				altTarget = findAltTarget();

				baseFrames = 0;

				if(altTarget != null)
				{
					baseWorker.move(altTarget.toPosition());
					baseWorker.build(UnitType.Zerg_Hatchery, baseWorker.getTilePosition());
				}
				minerals -= 300;
			}
		}
		else if(baseWorker != null && baseWorker.exists() && baseWorker.isMorphing())
			buildingBase = true;
		return minerals;
	}

	private TilePosition findAltTarget()
	{
		baseCounter++;
		if(baseCounter == 8)
		{
			baseCounter = 0;
			baseDist++;
		}


		switch(baseCounter)
		{
		case 0:
			return new TilePosition(baseTarget.getX(), baseTarget.getY() - baseDist);

		case 1:
			return new TilePosition(baseTarget.getX() + baseDist, baseTarget.getY() - baseDist);

		case 2:
			return new TilePosition(baseTarget.getX() + baseDist, baseTarget.getY());

		case 3:
			return new TilePosition(baseTarget.getX() + baseDist, baseTarget.getY() + baseDist);

		case 4:
			return new TilePosition(baseTarget.getX(), baseTarget.getY() + baseDist);

		case 5:
			return new TilePosition(baseTarget.getX() - baseDist, baseTarget.getY() + baseDist);

		case 6:
			return new TilePosition(baseTarget.getX() - baseDist, baseTarget.getY());

		case 7:
			return new TilePosition(baseTarget.getX() - baseDist, baseTarget.getY() - baseDist);

		}
		return null;
	}

	/*

	private TilePosition findAltTarget()
	{
//		int minLeft = 0;
//		int minRight = 0;
//		int minUp = 0;
//		int minDown = 0;

		int x = baseTarget.getX();
		int y = baseTarget.getY();

		Unit closestGeyser = null;

		//loop through game's geysers, get closest to our base
		for(Unit geyser : game.getGeysers() ){

			//calculate distance, if less then is closes

			if(closestGeyser == null || closestGeyser.getTilePosition().getDistance(baseTarget) > geyser.getTilePosition().getDistance(baseTarget)){
				closestGeyser = geyser;
			}					
		}

		int avgX = 0;
		int avgY = 0;
		int amtMin = 0;

		for(Unit mineral : game.getMinerals() ){

			//determines position relative to base

			if(mineral.getTilePosition().getDistance(baseTarget) > 15)
				//irrelevant mineral
				continue;

			avgX += mineral.getTilePosition().getX();
			avgY += mineral.getTilePosition().getY();
			amtMin++;

//			if(baseTarget.getX() > mineral.getTilePosition().getX())
//				//minerals are to the left
//				minLeft++;
//			else
//				minRight++;
//
//			if(baseTarget.getY() > mineral.getTilePosition().getY())
//				//minerals are above
//				minUp++;
//			else
//				minDown++;
		}

		//stupid case with the stupid map with the stupid spots
		if(amtMin == 0)
			return baseTarget;

		avgX /= amtMin;
		avgY /= amtMin;

		System.out.println("Base target is  : " + baseTarget);
		System.out.println("Average minerals: " + new TilePosition(avgX, avgY));
		System.out.println("Closest getseyr : " + closestGeyser.getTilePosition());

		//Base is to left of average minerals
		if(baseTarget.getX() > avgX) //alt need to be to the right
		{
			boolean moveRight = true;

			//check for dominantly horizontal geyser
			if(Math.abs(baseTarget.getX() - closestGeyser.getTilePosition().getX()) 
					> Math.abs(baseTarget.getY() - closestGeyser.getTilePosition().getY()))
				if(baseTarget.getX() < closestGeyser.getTilePosition().getX()) 
					moveRight = false; //geyser is to the right, cannot move right

			if(moveRight)
				x += 1;
		}
		else if(baseTarget.getX() < avgX) //Base is to right of average minerals
		{
			boolean moveLeft = true;

			//check for dominantly horizontal geyser
			if(Math.abs(baseTarget.getX() - closestGeyser.getTilePosition().getX()) 
					> Math.abs(baseTarget.getY() - closestGeyser.getTilePosition().getY()))
				if(baseTarget.getX() > closestGeyser.getTilePosition().getX()) 
					moveLeft = false; //geyser is to the left, cannot move left

			if(moveLeft)
				x -= 1;
		}

		if(baseTarget.getY() > avgY) //alt need to be to down
		{
			boolean moveDown = true;

			//check for dominantly vertical geyser
			if(Math.abs(baseTarget.getX() - closestGeyser.getTilePosition().getX()) 
					< Math.abs(baseTarget.getY() - closestGeyser.getTilePosition().getY()))
				if(baseTarget.getY() < closestGeyser.getTilePosition().getY()) 
					moveDown = false; //geyser is to the down, cannot move down

			if(moveDown)
				y += 1;			
		}
		else if(baseTarget.getY() < avgY)
		{
			boolean moveUp = true;

			//check for dominantly vartical geyser
			if(Math.abs(baseTarget.getX() - closestGeyser.getTilePosition().getX()) 
					< Math.abs(baseTarget.getY() - closestGeyser.getTilePosition().getY()))
				if(baseTarget.getY() > closestGeyser.getTilePosition().getY()) 
					moveUp = false; //geyser is to the up, cannot move up

			if(moveUp)
				y -= 1;		
		}

		/*
		if(0>(baseTarget.getX() - avgX)*(baseTarget.getX() - closestGeyser.getTilePosition().getX())){
			//presumably this means that the geyser is on the left/right and crystals on opposite
			if(Math.abs(baseTarget.getY()-avgY)<Math.abs(baseTarget.getY()-closestGeyser.getTilePosition().getY())){
				//step away from avgY
			}
			else if(){
				//step away from closestGeyser
			}
		}
		else if(0>(baseTarget.getY() - avgY)*(baseTarget.getY() - closestGeyser.getTilePosition().getY())){
			//presumably this means that the geyser is on the left/right and crystals on opposite
		}
	 */

	//return new TilePosition(x, y);
	//}

	public static void setWorkerAmount(int amt)
	{
		WORKER_AMOUNT = amt;
	}

	public Unit getWorker()
	{
		return workerManager.getWorker();
	}

	public void decrementWorkers(){
		workersExpected--;
	}

	public Unit getHatchery()
	{
		return hatchery;
	}

	public TilePosition getTarget()
	{
		return baseTarget;
	}

	public void setTarget(TilePosition pos)
	{
		baseTarget = pos;
	}

	public void nullify()
	{
		baseWorker = null;
		baseTarget = null;
		altTarget = null;
		buildingBase = false;
	}

	public boolean expandable()
	{
		if(workerManager.getNumWorkers() >= WORKER_AMOUNT)
			return true;
		return false;
	}

	public List<Unit> getWorkers()
	{
		return workerManager.getWorkerList();
	}

	public void inheritWorkers(List<Unit> workers)
	{
		for(Unit worker : workers)
			workerManager.addWorker(worker);
	}

	public boolean exists()
	{
		return hatchery.exists();
	}

	public void addEgg(Unit egg)
	{
		eggs.add(egg);
	}
}
