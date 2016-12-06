package Jarretts_Prototype;

import java.util.*;
import bwapi.*;
import bwta.BWTA;

/**
 * This class tracks and manages all of the worker units 
 * the agent owns.
 * 
 * @author Kenny Trowbridge
 * 
 * Ported to handle Zerg
 * 
 * @author Jarrett Oney
 *
 */
public class WorkerManager{
	
	private Player self = null;
	private List<Unit> neutralUnits = new ArrayList<Unit>();
	private static List<Unit> workerList = new ArrayList<Unit>(10);
	private List<Unit> larvae = new ArrayList<Unit>(10);
	
	/**
	 * constructor
	 * @param self  Player object for bot
	 * @param neutralUnits  List of neutral units in the game (Only used to task workers to gather)
	 */
	public WorkerManager(Player self, List<Unit> neutralUnits)
	{
		this.self = self;
		this.neutralUnits = neutralUnits;
	}
	
	/**
	 * update()
	 * This method maintains the list of worker units by pruning
	 * units that no longer exist. Also assigns idle units tasks
	 */
	public void update()
	{ 
		List<Unit> workersToRemove = new ArrayList<Unit>();
		List<Unit> larvaeToRemove = new ArrayList<Unit>();
		
		for(Unit worker : workerList)
		{			
			if(worker.isIdle() && worker.isCompleted())
			{
				//assign a task
				// protect against not finding any closest minerals. 
				// -- ie. don't pass null to u.gather(); that is a bad thing. 
				Unit closestMineral = findClosestMineral(BWTA.getStartLocation(self).getPosition());
				if(closestMineral != null)
				{
					worker.gather(closestMineral);
				}
			}
			
			//save dead units for deletion	
			if(!worker.exists())
			{
				workersToRemove.add(worker);
			}
		}
		
		for(Unit larva : larvae)
		{
			if(!larva.exists() 
					|| larva.getType() != UnitType.Zerg_Larva)
			{
				larvaeToRemove.add(larva);
			}
		}
		
		//remove dead workers
		for(Unit u : workersToRemove)
		{
			workerList.remove(u);
		}
		
		for(Unit u : larvaeToRemove)
		{
			larvae.remove(u);
		}
		
		//System.out.println("There are "+larvae.size()+" larva in the list");
	}

	/**
	 * getWorker()
	 * Finds an available worker unit
	 * 
	 * @return - a worker unit
	 */
	public Unit getWorker()
	{
		Unit availableWorker = null;
		for(Unit worker : workerList)
		{			
//			//make sure no drones are going to morph at the same time
//			if(worker.getOrder().equals(Order.ZergBuildingMorph))
//			{
//				return null;
//			}		
			
			//find a free worker
			if(!worker.isMorphing() && worker.isInterruptible() 
					&& worker.isCompleted() && !worker.isCarryingMinerals())
			{
				availableWorker = worker;
			}
		}
		workerList.remove(availableWorker);
		return availableWorker;
	}
	
	/**
	 * getLarva()
	 * 
	 * @return a larva
	 */
	public Unit getLarva()
	{
		if(!larvae.isEmpty()){
			Unit larva = larvae.get(0);
			//larvae.remove(larva);
			return larva;
		}
		return null; //no larva available
	}
	
	/**
	 * getDroneCount()
	 * 
	 * @return the number of drones controlled by the player
	 */
	public static int getDroneCount()
	{
		return workerList.size();
	}
	
	/**
	 * getLarvaCount()
	 * 
	 * @return the number of larva controlled by the player
	 */
	public int getLarvaCount(){
		return larvae.size();
	}
	
	/**
	 * addUnit
	 * Adds a unit to the workerList or larvae 
	 * 
	 * @param unit - unit to be added
	 */
	public void addUnit(Unit unit)
	{ 
		//add only worker units
		if (unit != null && unit.getType() == UnitType.Zerg_Drone)
		{
			System.out.println("re-added the worker");
			workerList.add(unit);
		}
		else if(unit != null && unit.getType() == UnitType.Zerg_Larva)
		{
			larvae.add(unit);
		}
	}
	

	/**
	 * findClosestMineral()
	 * Finds the closest mineral to the given position
	 * 
	 * @param pos - position of the unit
	 */
	private Unit findClosestMineral(Position pos) 
	{
		if(pos == null)
		{
			return null;
		}
		//init closest to first in list
		Unit closest = null;
		
		//find closest mineral
		for(Unit neutral : neutralUnits)
		{
			//only check mineral fields
			if(neutral.getType() == UnitType.Resource_Mineral_Field)
			{
				if(closest == null || neutral.getDistance(pos) < closest.getDistance(pos))
				{
					closest = neutral;
				}
			}
		}
		
		return closest;
	}
	
}

