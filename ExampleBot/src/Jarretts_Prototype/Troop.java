package Jarretts_Prototype;

import java.util.ArrayList;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;


public class Troop {

	public List<Unit> units;
	private Object lastFrameTarget;
	private Object attackTarget;
	private Game game;
	
	private Position meanPos;
	private double unity;
	
	public Troop(Game game){
		this.game = game;
		units = new ArrayList<Unit>();
	}
	
	public void manage()
	{
		
		if(units.size() > 0){
			doStatistics();
			game.drawCircleMap(meanPos, 6, Color.Purple);
			
			ArrayList<Unit> unitsToRemove = new ArrayList<Unit>();
			for(Unit zergling : units)
			{
				//System.out.println("Zergling is managing");
				//save dead units for deletion	
				if(!zergling.exists())
				{
					unitsToRemove.add(zergling);
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
		else
		{
			meanPos = null;
			unity = 0;
		}
	}
	
	public void addUnit(Unit u)
	{
		units.add(u);
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
	
	private void doStatistics()
	{
		int avX = 0;
		int avY = 0;
		
		for(Unit zergling : units)
		{
			avX += zergling.getTilePosition().getX();
			avY += zergling.getTilePosition().getY();
		}
		
		avX /= units.size();
		avY /= units.size();
		
		
		meanPos = new TilePosition(avX, avY).toPosition();
		
		double stdX = 0;
		double stdY = 0;
		
		for(Unit zergling : units)
		{
			stdX += Math.pow(zergling.getTilePosition().getX() - avX, 2);
			stdY += Math.pow(zergling.getTilePosition().getY() - avY, 2);
		}
		
		stdX = Math.sqrt((double) (stdX / units.size()));
		stdY = Math.sqrt((double) (stdY / units.size()));
		
		unity = (stdX + stdY) / 2;
	}
	
	public double getUnity()
	{
		return unity;
	}
	
	public Position getMeanPos()
	{
		return meanPos;
	}
}
