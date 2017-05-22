/*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2017
*/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

// Coordinate system for map
class Coordinate {
	private int x;
	private int y;

	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(final Object o){
		if (this == o){
			return true;
		}
		if (!(o instanceof Coordinate)){
			return false;
		}
		
		final Coordinate coordinate = (Coordinate) o;
		
		if (x != coordinate.x){
			return false;
		}
		
		if (y != coordinate.y){
			return false;
		}
		
		return true;
	}
	
	// Create unique hashcode for each coordinate
	@Override
	public int hashCode(){
		int result = x;
		result = 31 * result + y;
		return result;
	}
	
	public int get_x() {
		return this.x;
	}

	public int get_y() {
		return this.y;
	}

	public void set_x(int x) {
		this.x = x;
	}

	public void set_y(int y) {
		this.y = y;
	}
	
	

}

public class Agent {

	// Create map
	private Map<Coordinate, Character> map = new HashMap<Coordinate, Character>();
	// Map of explored coordinates, stores number of times player has explored coordinate
	private Map<Coordinate, Integer> explored = new HashMap<Coordinate, Integer>();
	// Store last move
	private char lastMove = 'Z';
	// Store current character coordinate
	private Coordinate currentLocation = new Coordinate(0, 0);
	// Store character direction
	private int direction = 1;
	// Following a wall 
	private boolean following = false;
	// Store inventory
	private Map<String, Boolean> inventory = new HashMap<String, Boolean>();

	private Queue<Character> moveQueue = new LinkedList<Character>();
	
	
	// Checks if a provided character is an obstacle
	private boolean isObstacle(char spaceToCheck) {
		if (spaceToCheck == 'T' || spaceToCheck == '-' || spaceToCheck == '*' || spaceToCheck == '~'){
			return true;
		}
		return false;
		
	}

	// Keep wall/obstacles to the left and follow, move forward otherwise 
	public void wallFollow(char view[][]) {
		char move = 'Z';

		char frontView = view[1][2];
		char backView = view[3][2];
		char rightView = view[2][3];
		char leftView = view[2][1];
		
		
		// If any obstacle to the left, we are following
		if (isObstacle(leftView) == true){
			following = true;
		}
		
		// If no obstacles in front then move forward or rotate right if obstacle directly ahead
		if (isObstacle(frontView) && !following) {
			move = 'r';
		} else if (isObstacle(frontView) && following) {
			move = 'r';
		} else {
         move = 'f';
		}
		
		// If previously following a wall, turn to ensure we keep it on the left
		if (following == true && isObstacle(leftView) == false && lastMove != 'l'){
			move = 'l';
		}
		// Prevents following infinitely along an obstacle, move on if we arrive at same spot more than twice
		if (explored.get(currentLocation) > 2) {
			following = false;
		}
		

		moveQueue.add(move);
   }

	public void updateMapAndDirection(char view[][]) {

      // Start of game,  map starting view
      if(lastMove == 'Z'){
         int x = -2;
         for(int counter = 0; counter < 5; counter++){
            int y = 2;
            for(int counter2 = 0; counter2 < 5; counter2++){
               Coordinate coord = new Coordinate(x,y);
               
               // At spawned coordinate
               if(x == 0 && y == 0){            	   
            	   map.put(coord, '!');
            	   explored.put(coord, 0);
            	   y--;
            	   continue;
               }
					// Otherwise insert view's value
               map.put(coord, view[counter2][counter]);
               explored.put(coord, 0);
               y--;
            }
            x++;
         }

      // Update direction of character if rotated
      } else if(lastMove == 'l'){
			direction++;
			direction = direction % 4;
      } else if(lastMove == 'r'){
			direction--;
			direction = direction % 4;
			// Ensure that direction doesn't go negative (strange mod behaviour in java)
			if (direction < 0) direction += 4;
      // Update map if last move was to move forward
      } else if(lastMove == 'f'){
         // Add current coordinate into explored
			explored.put(currentLocation, (explored.get(currentLocation)) + 1);
			
         // Move East
			if (direction == 0) {
				currentLocation.set_x(currentLocation.get_x() + 1);
				int viewCounter = 0;
				for (int counter = 2; counter >= -2; counter--) {
					Coordinate discovery = new Coordinate(currentLocation.get_x() + 2, currentLocation.get_y() + counter);
					map.put(discovery, view[0][viewCounter]);
					explored.put(discovery, 0);
					System.out.print("put into map" + view[0][viewCounter]);
					viewCounter++;
				}
         // Move North
         } else if(direction == 1){
				currentLocation.set_y(currentLocation.get_y() + 1);
				int viewCounter = 0;
				for (int counter = -2; counter <= 2; counter++) {
					Coordinate discovery = new Coordinate(currentLocation.get_x() + counter, currentLocation.get_y() + 2);
					map.put(discovery, view[0][viewCounter]);
					explored.put(discovery, 0);
					System.out.print("put into map = |" + view[0][viewCounter] + "|");

               viewCounter++;
            }
         // Move West
         } else if(direction == 2) {
				currentLocation.set_x(currentLocation.get_x() - 1);
				int viewCounter = 0;
				for (int counter = -2; counter <= 2; counter++) {
					Coordinate discovery = new Coordinate(currentLocation.get_x() - 2, currentLocation.get_y() + counter);
					
					System.out.print("DISCOVERY IS " + discovery.get_x() + "," + discovery.get_y() + "\n");
					System.out.print("ViewCounter = " + viewCounter + "\n");
					
					map.put(discovery, view[0][viewCounter]);
					explored.put(discovery, 0);
					System.out.print("put into map = |" + view[0][viewCounter] + "|" + "\n");
					viewCounter++;
            }
         // Move South
         } else if(direction == 3) {
        	 currentLocation.set_y(currentLocation.get_y()-1);
        	 int viewCounter = 0;
        	 for(int counter = 2; counter >= -2; counter--){
        		 Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()-2);
        		 map.put(discovery, view[0][viewCounter]);
        		 explored.put(discovery, 0);
        		 System.out.print("put into map = |" +view[0][viewCounter] + "|");
        		 viewCounter++;
            }
         }
         //if current location had item then add to inventory hash
         if(map.get(currentLocation) == 'a') inventory.put("axe", true);
         if(map.get(currentLocation) == 'd') inventory.put("dynamite", true);
         if(map.get(currentLocation) == 'k') inventory.put("key", true);
         if(map.get(currentLocation) == '$') inventory.put("treasure", true);
      }
   }

	public char get_action(char view[][]) {

		// At each move update map and direction
		updateMapAndDirection(view);
		System.out.print("line 140 last move was " + lastMove + " current direction = " + direction + "\n");
		System.out.print("x coordinate = " + currentLocation.get_x() + " y coordinate = " + currentLocation.get_y() + "\n");
		System.out.print("current location = " + map.get(currentLocation));
		
		print_view(view);
		
		char nextMove = wallFollow(view);
		
		if (view[0][2] == '$'){
			nextMove = 'f';
		}
		
		// Update last move
		lastMove = moveQueue.element();
		return moveQueue.poll();
		
	}


// ==================================================================================================================================================
	
	void print_view(char view[][]) {
		int i, j;

		System.out.println("\n+-----+");
		for (i = 0; i < 5; i++) {
			System.out.print("|");
			for (j = 0; j < 5; j++) {
				if ((i == 2) && (j == 2)) {
					System.out.print('^');
				} else {
					System.out.print(view[i][j]);
				}
			}
			System.out.println("|");
		}
		System.out.println("+-----+");
	}

	public static void main(String[] args) {
		InputStream in = null;
		OutputStream out = null;
		Socket socket = null;
		Agent agent = new Agent();
		char view[][] = new char[5][5];
		char action = 'F';
		int port;
		int ch;
		int i, j;

		if (args.length < 2) {
			System.out.println("Usage: java Agent -p <port>\n");
			System.exit(-1);
		}

		port = Integer.parseInt(args[1]);

		try { // open socket to Game Engine
			socket = new Socket("localhost", port);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			System.out.println("Could not bind to port: " + port);
			System.exit(-1);
		}

		try { // scan 5-by-5 wintow around current location
			while (true) {
				for (i = 0; i < 5; i++) {
					for (j = 0; j < 5; j++) {
						if (!((i == 2) && (j == 2))) {
							ch = in.read();
							if (ch == -1) {
								System.exit(-1);
							}
							view[i][j] = (char) ch;
						}
					}
				}
				// agent.print_view( view ); // COMMENT THIS OUT BEFORE
				// SUBMISSION
				action = agent.get_action(view);
				out.write(action);
			}
		} catch (IOException e) {
			System.out.println("Lost connection to port: " + port);
			System.exit(-1);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
}
