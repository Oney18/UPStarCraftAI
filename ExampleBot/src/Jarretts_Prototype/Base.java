package Jarretts_Prototype;

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

	private boolean gettingPoolWorker = false;
	private boolean buildingPool = false;
	private Unit poolMorpherDrone;
	private TilePosition poolPos;
	private int workersExpected;
	private TilePosition baseTarget;

	public Unit gasMorpher;
	private boolean doExtractor;
	private boolean gasMorphing2;
	private boolean gasMorphingStarted;
	private int frames;
	private Unit baseWorker;
	private int baseFrames;
	private boolean buildingBase;
	private boolean builtInitDrone;
	
	private Random RNGesus;
	
	private static int WORKER_AMOUNT = 4;

	public Base(UPStarcraft controller, Player self, Game game, Unit hatchery, boolean doExtractor){
		this.controller = controller;
		this.hatchery = hatchery;
		this.self = self;
		this.game = game;
		this.doExtractor = doExtractor;
		builtInitDrone = false;
		RNGesus = new Random();
		buildingBase = false;
		baseWorker = null;
		baseTarget = null;
		baseFrames = 0;
		frames = 0;
		buildingOverlord = false;
		workerManager = new WorkerManager(self, game, doExtractor);
		troop = new Troop(game);
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

	public void manage(int minerals)
	{	
		List<Unit> larvae = hatchery.getLarva();
		game.drawTextMap(hatchery.getPosition(), "allocatedMin: " + minerals + "\nnumWorkers : " + workerManager.getNumWorkers() + "\nzergs: " + troop.getSize() + 
				"\nnumLarva: " + larvae.size() + "\nexpectedWorkers: " + workersExpected + "\nworkerCountStat: " + WORKER_AMOUNT);

		
		if(poolPos != null && !controller.spawnPoolExists)
			game.drawCircleMap(poolPos.toPosition(), 10, Color.Yellow, true);
		
		workerManager.manage();

		if(!controller.spawnPoolExists)
			makeSpawnPool(minerals);
		if(doExtractor)
			extractorTrick(minerals);

		

		//Initial drone building
		if(doExtractor && !builtInitDrone && minerals >= 50)
		{
			Unit larva = larvae.get(0);
			if(larva.morph(UnitType.Zerg_Drone))
			{
				workersExpected++;
			}
			
			larvae.remove(larva);
			minerals -= 50;
			
			builtInitDrone = true;
		}
		
		//build workers to get to 4?
		while(workerManager.getNumWorkers() + workersExpected < WORKER_AMOUNT && minerals >= 50 && !larvae.isEmpty())
		{
			//System.out.println("Called to morph another larvae, list length is at " + workerManager.getNumWorkers() + " and expected is " + workersExpected);
			Unit larva = larvae.get(0);
			if(larva.morph(UnitType.Zerg_Drone))
			{
				workersExpected++;
			}
			larvae.remove(larva);
			minerals -= 50;
		}

		//ZERGLINGS
		while(controller.spawnPoolExists && minerals >= 50 && self.supplyUsed() <= self.supplyTotal()-2 && !larvae.isEmpty() && troop.getSize() < 16)
		{
			//System.out.println("building zerg");
			Unit larva = larvae.get(0); //get a larvae
			larvae.remove(0); //remove from array
			//System.out.println("removing one");
			larva.morph(UnitType.Zerg_Zergling); //morph
			minerals =- 50;
		}

		if((self.supplyTotal() == 2 && !buildingOverlord) || (minerals >= 100 && larvae.size()==3 && self.supplyUsed()>=self.supplyTotal()-1 && !buildingOverlord))
		{
			if(larvae.size()>0)
			{
				Unit larva = larvae.get(0); //get a larvae
				larvae.remove(0); //remove from array
				//System.out.println("removing one1");
				larva.morph(UnitType.Zerg_Overlord); //morph
				minerals -= 100;
				buildingOverlord = true;
			}
		}else if(self.supplyTotal() > 2)
		{
			buildingOverlord = false;
		}
		
		if(!controller.rushing)
			expand(minerals);
		
	}

	private void makeSpawnPool(int minerals)
	{
		// build a spawning pool when we can
		if ( !buildingPool && minerals >= 180 && workerManager.getNumWorkers()>0) 
		{
			if(minerals <200 && !gettingPoolWorker) //between 150 and 200 then
			{
				//System.out.println("Starting to find a dude");
				poolMorpherDrone = workerManager.getWorker();
				
				
				//System.out.println("Got Worker: " + poolMorpherDrone.toString());
				if(poolMorpherDrone != null)
				{		
					//System.out.println("Got to 1");
					poolPos = findPoolPos();
					//System.out.println("Got position at: " + poolPos.toPosition().toString());
					if(poolPos != null)
					{
						poolMorpherDrone.move(poolPos.toPosition());
						//System.out.println("mmoving");
					}
				}
				//System.out.println("won't do this again");
				gettingPoolWorker = true;
			}
			else if(minerals >=200) //anything over 200
			{
				if(!buildingPool && poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
				{
					//System.out.println("trying to build");
					poolMorpherDrone.build(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
					
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
					//poolMorpherDrone.move(basePos);
					//System.out.println(poolMorpherDrone.getTilePosition().getDistance(poolPos));
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
				//System.out.println("trying to cancel");
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
			//System.out.println("starting extractor");
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
		workersExpected++;
		// make sure the builder is not null
		if(builder != null)
		{	
			//System.out.println("builder is not null, tries to look for geyser");
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
			//System.out.println("Sent the builder?");
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

	public void buildOverlord()
	{
		List<Unit> larvae = hatchery.getLarva();
		if(larvae.size() > 0 && self.minerals() > 200)
			larvae.get(0).morph(UnitType.Zerg_Overlord);
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
	
	private void expand(int minerals)
	{
		if(baseTarget == null)
			return;
		
		//get the worker
		if((baseWorker == null || !baseWorker.exists()) && minerals > 270)
		{
			baseWorker = workerManager.getWorker();
		}
		//run to the base
		if(baseWorker != null && baseWorker.exists() && baseWorker.getTilePosition().getDistance(baseTarget) > 10)
		{
			baseWorker.move(baseTarget.toPosition());
		}
		
		//build the base
		else if(baseWorker != null && baseWorker.exists() && minerals >= 300 && baseWorker.canBuild(UnitType.Zerg_Hatchery))
		{
			baseFrames++;
			//TODO find closest mineral, move away from, right now it just randomly twitches about
			if(baseFrames == 50)
			{
				baseFrames = 0;
				int deltaX = RNGesus.nextInt(2);
				int deltaY = RNGesus.nextInt(2);
				
				if(RNGesus.nextBoolean())
					deltaX *= -1;
				if(RNGesus.nextBoolean())
					deltaY *= -1;
				
				
				int x = baseTarget.getX() + deltaX;
				int y = baseTarget.getY() + deltaY;
				baseWorker.move(new TilePosition(x, y).toPosition());
				baseWorker.build(UnitType.Zerg_Hatchery, baseWorker.getTilePosition());
			}
		}
		else if(baseWorker != null && baseWorker.exists() && baseWorker.isMorphing())
			buildingBase = true;
	}
	
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
		buildingBase = false;
	}
}
