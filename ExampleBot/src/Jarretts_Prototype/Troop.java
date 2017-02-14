package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.List;

import bwapi.Position;
import bwapi.Unit;


public class Troop {

	public List<Unit> units;
	private Object lastFrameTarget;
	private Object attackTarget;
	
	public Troop(){
		units = new ArrayList<Unit>();
	}
	
	public void manage()
	{

		if(units.size() > 0){
			ArrayList<Unit> unitsToRemove = new ArrayList<Unit>();
			for(Unit zergling : units)
			{
				//System.out.println("Zergling is managing");
				//save dead units for deletion	
				if(!zergling.exists())
				{
					unitsToRemove.add(zergling);
					//units.remove(zergling);
				}
				else if(attackTarget instanceof Position)
				{
					zergling.move((Position) attackTarget);
				}
				else if(lastFrameTarget != attackTarget || zergling.isIdle())
				{
					//this will still tell all 'stuck" zergs to do a command every frame
					//which we believe to be the cause of the "stuck bug"(hence does nothing)
					if(attackTarget != null){
						if(attackTarget instanceof Unit)
							zergling.attack((Unit) attackTarget);
						////System.out.println("attack order");
					}
				}
			}
			for(Unit zergling : unitsToRemove)
			{
				units.remove(zergling);
			}
		}
	}
	
	public void addUnit(Unit u)
	{
		units.add(u);
		System.out.println("This troop has "+units.size());
	}
	
	public void move(Position p)
	{
		for(Unit unit : units)
			unit.move(p);
	}
	
	public void attackMove(Position p)
	{
		for(Unit unit : units)
			unit.attack(p);
	}

	public void setAttackTarget(Object target)
	{
		if((target instanceof Position || target instanceof Unit) && units.size() > 0)
		{
			lastFrameTarget = attackTarget;
			attackTarget = target;
		}
		
		//for(Unit unit : units)
		//	unit.attack(target);
		
		
	}
	
}
