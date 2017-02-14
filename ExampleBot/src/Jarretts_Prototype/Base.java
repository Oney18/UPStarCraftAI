package Jarretts_Prototype;

import java.util.List;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;


public class Base {

	private UPStarcraft controller;
	private Player self;
	private Game game;
	private Position basePos;
	private Unit hatchery;
	private WorkerManager workerManager;
	private Troop troop;
	private boolean buildingOverlord;
	
	private boolean gettingPoolWorker = false;
	private boolean buildingPool = false;
	private Unit poolMorpherDrone;
	private TilePosition poolPos;
	int workersExpected;
	
	
		public Base(UPStarcraft controller, Player self, Game game, Unit hatchery){
			this.controller = controller;
			this.hatchery = hatchery;
			this.self = self;
			this.game = game;
			buildingOverlord = false;
			workerManager = new WorkerManager(self, game);
			troop = new Troop();
			controller.newTroop(troop);
			basePos = hatchery.getPosition();
			workersExpected = 0;
		}
		
		public void manage()
		{
			workerManager.manage();
			
			if(!controller.spawnPoolExists)
				makeSpawnPool(self.minerals());
			
			int minerals = self.minerals();
			List<Unit> larvae = hatchery.getLarva();
			//System.out.println("Larva Size: " + larvae.size());
			
			//build workers to get to 4?
			if(!larvae.isEmpty() && workerManager.getNumWorkers() + workersExpected < 3)
			{
				Unit larva = larvae.get(0);
				larva.morph(UnitType.Zerg_Drone);
				larvae.remove(larva);
				minerals =- 50;
				workersExpected++;
			}
			
			//ZERGLINGS
			while(controller.spawnPoolExists && minerals >= 50 && self.supplyUsed() <= self.supplyTotal()-2 && larvae.size() > 0)
			{
				//System.out.println("building zerg");
				Unit larva = larvae.get(0); //get a larvae
				larvae.remove(0); //remove from array
				//System.out.println("removing one");
				larva.morph(UnitType.Zerg_Zergling); //morph
				minerals =- 50;
			}
			
			//TODO: Need to edit, do we need to have more overlords?
			if((self.supplyTotal() == 2 && !buildingOverlord) || (minerals >= 100+50*larvae.size() && larvae.size()==3 && self.supplyUsed()>=self.supplyTotal()-1))
			{
				if(larvae.size()>0)
				{
					Unit larva = larvae.get(0); //get a larvae
					larvae.remove(0); //remove from array
					//System.out.println("removing one1");
					larva.morph(UnitType.Zerg_Overlord); //morph
					minerals -= 100;
					buildingOverlord=true;
				}
			}else if(self.supplyTotal() > 2)
			{
				buildingOverlord = false;
			}
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
						poolPos = getBuildTile(poolMorpherDrone, UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
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
					if(poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
					{
						//System.out.println("trying to build");
						poolMorpherDrone.build(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition());
						if(self.allUnitCount(UnitType.Zerg_Spawning_Pool) > 0)
						{
							buildingPool = true;
						}
					}
					else if(poolMorpherDrone.getPosition().equals(poolPos.toPosition()) && !poolMorpherDrone.canBuild(UnitType.Zerg_Spawning_Pool, poolMorpherDrone.getTilePosition()))
					{
						//maybe still moving, let it keep moving to the spot we found works
						poolMorpherDrone.move(basePos);
						//System.out.println("move home");
						//if you command it to move, it gets mad and won't obey. too many commands
					}
				}						
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
		
		/** 
		 * getBuildTile
		 * 
		 * Returns a suitable TilePosition to build a given building type near 
		 * Highly inefficient, ripped from SSCAIT tutorial
		 * 
		 * @param builder
		 * @param buildingType
		 * @param aroundTile
		 * @return
		 */
		 private TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		 	TilePosition ret = null;
		 	int maxDist = 3;
		 	int stopDist = 40;
		 	
		 	while ((maxDist < stopDist) && (ret == null)) {
		 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
		 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
		 				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
		 					// units that are blocking the tile
		 					boolean unitsInWay = false;
		 					for (Unit u : game.getAllUnits()) {
		 						if (u.getID() == builder.getID()) continue;
		 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
		 					}
		 					if (!unitsInWay) {
		 						return new TilePosition(i, j);
		 					}
		 					// creep for Zerg
		 					if (buildingType.requiresCreep()) {
		 						boolean creepMissing = false;
		 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
		 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
		 								if (!game.hasCreep(k, l)) creepMissing = true;
		 								break;
		 							}
		 						}
		 						if (creepMissing) continue; 
		 					}
		 				}
		 			}
		 		}
		 		maxDist += 2;
		 	}
		 	
		 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
		 	return ret;
		 }
}
