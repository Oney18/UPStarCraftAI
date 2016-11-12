package Jarretts_Prototype;

import java.util.*;
import bwapi.*;

/**
 * MilitaryManager
 * Maintains and manages all military units under the
 * agent's control
 * 
 * @author Kenny Trowbridge
 * 
 * converted to zerg
 * @author Jarrett Oney
 *
 */
public class MilitaryManager{
	private Game game;
	private Player self;
	
	protected List<Unit> militaryUnits;
	protected HashMap<SquadType, Squad> squads;
	private ArmyManager armyManager;
	private BattleManager battleManager;
	private int armyCount;


	/**
	 * MilitaryManager()
	 * Constructor for the MilitaryManager class.
	 */
	public MilitaryManager(Game game, Player self)
	{
		this.game = game;
		this.self = self;
		
		militaryUnits = new ArrayList<Unit>();
		squads = new HashMap<SquadType, Squad>();
		armyCount = 0;
		
		initSquads();
		
		armyManager = new ArmyManager(squads, self, game);
		battleManager = new BattleManager();
	}
	
	/**
	 * initSquads()
	 * Initialize all Terran squads.
	 */
	private void initSquads()
	{
		for(SquadType type : SquadType.values())
		{
			squads.put(type, new Squad(type));
		}
	}
	
	/**
	 * addUnit()
	 * Adds a unit to the militaryUnits list and adds it to a squad.
	 * If the unit already exists, it is not added again to either 
	 * to either list.
	 * 
	 * @param unit - unit to add
	 */
	public void addUnit(Unit unit)
	{
		// put unit in a squad. Default is Offense. 
		if(squads.get(SquadType.Scout).isEmpty())
		{
			//add only the first unit to the scout squad
			squads.get(SquadType.Scout).addUnit(unit);
		}
		else
		{
			//default, add to offense
			squads.get(SquadType.Offense).addUnit(unit);
		}
	}
	
	/**
	 * hasScout()
	 * Check if there is a unit within the scout squad. 
	 * 
	 * @return true if there is a scout, false if not
	 */
	public boolean hasScout()
	{
		if (squads.get(SquadType.Scout).isEmpty()){
			return false;
		}
		return true;
	}
	
	/**
	 * update()
	 * Examines all the military units and ensures that they 
	 * are in both the squads list and the militaryUnits list.
	 * It also prunes units that no longer exist from both lists.
	 */
	public void update()
	{
		try
		{
			updateArmyCount();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * command()
	 * Given a command from the StrategyManager this method will
	 * interpret and execute that command.
	 * 
	 * @param command - command from the StrategyManager
	 * @param percentCommit - percentage of units to commit to command
	 * @param position - the position of the commanded
	 */
	public void command(Command command, Position position)
	{
		switch(command)
		{
			case Attack:
				armyManager.engage(position);
				break;
			case Defend:
				armyManager.defend(position);
				break;
			case Scout:
				if(armyCount > 20)
				{
					armyManager.scoutMap();
				}
				else
				{
					armyManager.scoutBases();
				}
				break;
		}
	}
    
    /**
     * updateArmyCount()
     * 
     * Update the number of military units the agent controls
     */
    private void updateArmyCount()
    {
    	//TODO if we build more than zerglings then we need to update this
    	armyCount = self.completedUnitCount(UnitType.Zerg_Zergling);
    }
    
    /**
     * getArmyCount()
     * @return size of army
     */
    public int getArmyCount()
    {
    	return armyCount;
    }
    
}
