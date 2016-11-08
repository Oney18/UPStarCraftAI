package Jarretts_Prototype;
//package StarcraftAI;
import java.util.*;

import javax.sound.midi.ControllerEventListener;

import bwapi.*;
import bwta.BWTA;
/**
 * The production manager is responsible for building units that the strategy manager requests. 
 * ProductionManager uses the WorkerManager and the BuildingManager to handle build and research 
 * orders passed to it from the StrategyManager.
 * 
 * @author Kenny Trowbridge
 * @author Alex Bowns
 * @author Max Robinson
 * @author Casey Sigelmann
 * 
 * This is ported to Zerg
 * @author Jarrett Oney
 */
public class ProductionManager {
	
	private Game game;
	private Player self; 
	
	private ArrayList<UnitType> productionQueue = new ArrayList<UnitType>(); 
	private ArrayList<UnitType> goals = new ArrayList<UnitType>();
	private ArrayList<UnitType> newGoal = new ArrayList<UnitType>();
	
	private WorkerManager workerManager;
	
	private Unit gasMorpher = null;
	
	/**
	 * Ctor
	 * Sets up the needed instance variables for the class and sets up the game and player objects needed 
	 * to reference the game.
	 * 
	 * @param game
	 * @param self
	 */
	public ProductionManager(Game game, Player self){
		this.game = game;
		this.self = self;
		
		this.workerManager = new WorkerManager(self, game.getNeutralUnits());
		
		this.productionQueue = new ArrayList<UnitType>();
		this.goals = new ArrayList<UnitType>();
		this.newGoal = new ArrayList<UnitType>();
		
		//add starting workers to worker list
		for(Unit u : game.self().getUnits())
		{
			if(u.getType() == UnitType.Zerg_Drone
					|| u.getType() == UnitType.Zerg_Larva)
			{
				workerManager.addUnit(u);
			}
		}
		
			
	}

	/**
	 * addUnit()
	 * This checks the type of the given unit and then calls the appropriate 
	 * addUnit method in either the BuildingManager or WorkerManager.
	 * 
	 * @param unit - the specific unit that we are checking
	 */
	public void addUnit(Unit unit)
	{
		//dont add null or units that do not belong to the agent		if (unit == null || unit.getPlayer() != self)
			return;
		
		if(unit.getType() == UnitType.Zerg_Drone
				|| unit.getType() == UnitType.Zerg_Larva)
		{
			workerManager.addUnit(unit);
		}
		
	}
	
	/**
	 * setGoal()
	 * This method sets the newGoal instance variable to the specified parameter.
	 * 
	 * @param newGoal - the new goal instance variable
	 */
	public void setGoal(ArrayList<UnitType> newGoal)
	{
		this.newGoal = newGoal;
	}
	
	/** 
	 * buildBuilding()
	 * This method will retrieve a worker from the Worker Manager,
	 * then using that worker, issue a build command to the Building Manager to construct 
	 * the building type specified.
	 *  
	 * @param unitType - type of building to build
	 * @return true if order is sent
	 */
	private void buildBuilding(UnitType buildingType)
	{
		if(buildingType.isBuilding())
		{
			Unit builder = workerManager.getWorker();
			System.out.println("It recognizes the building?");
			// make sure the builder is not null
			if(builder != null && game.canMake(buildingType, builder))
			{
				if(buildingType == UnitType.Zerg_Extractor){
					System.out.println("builder is not null, tries to look for geyser");
					Unit closestGeyser = null;
					Position startArea = BWTA.getStartLocation(self).getPosition();
					
					//loop through game's geysers, get closest to our base
					for(Unit geyser : game.getGeysers() ){
						if(game.canBuildHere(geyser.getTilePosition(), buildingType, builder, true)){
							
							//calculate distance, if less then is closes
							if(closestGeyser == null || closestGeyser.getDistance(startArea) > geyser.getDistance(startArea)){
								closestGeyser = geyser;
							}					
						}
					}
					gasMorpher = builder;
					builder.build(buildingType, closestGeyser.getTilePosition());
					System.out.println("Sent the builder?");
				}
				//average building, need not geyser
				else{					
					TilePosition buildSpot = getBuildTile(builder, buildingType, builder.getTilePosition());
					
					if(buildSpot != null){
						builder.build(buildingType, buildSpot);
					}
				}
			}
		}
	}
	
	/**
	 * cancelGas()
	 * Cancels the extractor morph to create cheese
	 */
	public void cancelGas()
	{
		if(gasMorpher != null)
			gasMorpher.cancelMorph();
	}
	
	/**
	 * update()
	 * This method is responsible for calling the update methods in the WorkerManager.It checks if goal and newGoal are not the same and if so,
	 * it sets goal to newGoal and updates the productionQueue. 
	 * It then calls processQueue to initiate building construction, unit training, 
	 * and technology research using the productionQueue.
	 * 
	 */
	public void update()
	{
		try
		{
			workerManager.update();
			
			//if goal and new goal are the same, 
			if(!Arrays.deepEquals(goals.toArray(), newGoal.toArray()))
			{
				goals = newGoal;
				
				productionQueue.clear();
				
				//find paths for all of the goals
				//update production queue
				for(UnitType u : goals)
				{
					productionQueue.add(u);
				}
				
			}
			

			
			//issue builder orders
			processQueue();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	

	
	/**
	 * processQueue()
	 * The process queue method is responsible for sending out build orders based on the 
	 * contents of the priority queue. 
	 * 
	 */
	private void processQueue()
	{
		for(UnitType goal : productionQueue)
		{

			
			if(goal.isBuilding())
			{
				buildBuilding(goal);
			}
			else
			{
				//is unit, get a larva
				Unit larva = workerManager.getLarva();
				//System.out.println("Goal is " + goal.toString());
				if(larva != null)
				{
					//TODO if we create different units, we will need to add logic checking here
					
					if(!larva.morph(goal)){
						//System.out.println("The larva tried to morph but BWAPI said no!");
					}
					
				}
			}
		}
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
