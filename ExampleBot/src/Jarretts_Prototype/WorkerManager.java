package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.List;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;


public class WorkerManager {
	
	private List<Unit> workers;
	private Player self;
	private Game game;
	private int mineralsMined;
	
	public WorkerManager(Player self, Game game, boolean init){
		this.self = self;
		this.game = game;
		workers = new ArrayList<Unit>();
		
		if(init)
			mineralsMined = 50; //initial base
		else
			mineralsMined = 200; //built from existing base
	}
	
	public void addWorker(Unit worker){
		workers.add(worker);
	}
	
	public Unit getWorker(){
		for(Unit worker : workers)
		{
			if(!worker.isMorphing() && worker.isInterruptible() && worker.isCompleted() && 
					!worker.isCarryingMinerals() && !worker.isCarryingGas())
			{
				workers.remove(worker);
				return worker;
			}
		}
		return null;
	}
	
	public List<Unit> getWorkerList(){
		return workers;
	}
	
//	public void removeWorker(Unit worker){
//		workers.remove(worker);
//	}
	
	public int getNumWorkers(){
		//System.out.println(workers.size());
		return workers.size();
	}
	
	public void manage(){
		
		List<Unit> workersToRemove = new ArrayList<Unit>();
		for(Unit drone : workers)
		{			
			if(drone.isIdle() && drone.isCompleted())
			{
				Unit closestMineral = findClosestMineral(drone.getPosition());
				if(closestMineral != null)
				{
					drone.gather(closestMineral);
				}
			}

			//save dead units for deletion	
			if(!drone.exists())
			{
				workersToRemove.add(drone);
			}
		}
		for(Unit worker : workersToRemove)
		{
			workers.remove(worker);
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
		for(Unit neutral : game.getNeutralUnits())
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
	
	public int getMineralsMined()
	{
		return mineralsMined;
	}
	
}
