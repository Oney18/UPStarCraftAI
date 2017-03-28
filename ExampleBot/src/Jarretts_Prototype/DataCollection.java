package Jarretts_Prototype;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Race;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class DataCollection extends DefaultBWListener{
	private Mirror mirror = new Mirror();
	private Game game;
	private Player self;
	private Player enemy;
	private int games = 0;

	private Hashtable<Race, Integer> winsRace = new Hashtable<Race, Integer>();
	private Hashtable<Race, Integer> totalRace = new Hashtable<Race, Integer>();

	private Hashtable<String, Integer> winsMap = new Hashtable<String, Integer>();
	private Hashtable<String, Integer> totalMap = new Hashtable<String, Integer>();




	public static void main(String[] args) {
		new DataCollection().run();
	}

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onStart() {
		game = mirror.getGame();
//		BWTA.readMap();
//		BWTA.analyze();	

		for(Player p : game.getPlayers())
		{
			if(p.getName().equals("UPStarCraftAI 2016"))
				self = p;
			else if(!p.getName().equals("Neutral"))
				enemy = p;
		}

		game.setVision(self);
		game.setLocalSpeed(0);
	}	

	@Override
	public void onEnd(boolean isWinner)
	{
		games++;

		boolean won = false;

		for(Unit u : self.getUnits())
			if(u.getType() == UnitType.Zerg_Hatchery)
			{
				won = true;
				break;
			}

		System.out.println("We have " +won);
		System.out.println("Game says " + isWinner);

		if(won)
		{
			Integer victoriesRace = winsRace.get(enemy.getRace());
			if(victoriesRace == null)
				victoriesRace = 0;
			winsRace.put(enemy.getRace(), victoriesRace + 1);

			Integer victoriesMap = winsMap.get(game.mapFileName());
			if(victoriesMap == null)
				victoriesMap = 0;
			winsMap.put(game.mapName(), victoriesMap + 1);
		}

		Integer amtRace = totalRace.get(enemy.getRace());
		if(amtRace == null)
			amtRace = 0;
		totalRace.put(enemy.getRace(), amtRace + 1);

		Integer amtMap = totalMap.get(game.mapFileName());
		if(amtMap == null)
			amtMap = 0;
		totalMap.put(game.mapName(), amtMap + 1);


		if(games == 75)
		{
			try(PrintWriter out = new PrintWriter("data.txt"))
			{

				out.println("By Race:");
				for(Race r : totalRace.keySet())
					out.println("Fought against " + r.toString() + " " + totalRace.get(r) + " times and won " + winsRace.get(r));

				out.println("\nBy map:");
				for(String s : totalMap.keySet())
					out.println("Fought on" + s + " " + totalMap.get(s) + " times and won " + winsMap.get(s));

			}
			catch(Exception e)
			{
				System.out.println("We fucked up");
			}

			System.exit(0);
		}




	}



}
