/*********************************************
 *  Agent.java 
 *  by Xu Wu z5061319 and Arunav N Sarkar z5061286
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2017
*/
/*
Briefly describe how your program works, including any algorithms and data structures employed, and explain any design decisions you made along the way:
	We decided to implement a wall following approach to exploration. When exploring the map our agent will store
	any new areas at the border of its view into a hashmap, fittingly called map. This hashmap used a Coordinate class
	we created as a key, and the characters at those coordinates as values. We used another hashmap for our inventory
	stock. When coming across items, treasure or trees we utilised an A* search algorithm using a heuristic based
	on the Manhattan heuristic and large values assigned to non traversable values such as walls and sometimes water,
	upon finding an item that allows the player to deal with the obstacle, heuristics are reduced.

	In our A* search we used the Stack data structure for pathfinding. When queuing up moves for our agent to make,
	we used a Queue data structure. We designed our agent to have multiple queues for separate objectives, so as
	to remember paths to objectives. Our design also included the creation of a Coordinate class, and a CoordState
	class. our CoordState class was used when finding optimal paths in A*, to keep track of item use along the path.


*/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

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
		
		if (this.x != coordinate.get_x()) {
			return false;
		}
		
		if (this.y != coordinate.get_y()) {
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

// States to use in conjunction with astar
class CoordState implements Comparable<CoordState> {
	private Coordinate coordinate;
	private int numDynamite;
	private int numRaft;
	private int gCost;
	private int hCost = 0;
	private CoordState prevState;
	
	public CoordState(Coordinate coordinate, int gCost, CoordState prevState, int numDynamite, int numRaft) {
		this.coordinate = coordinate;
		this.gCost = gCost;
		this.numDynamite = numDynamite;
		this.numRaft = numRaft;
		this.prevState = prevState;
	}
	
	@Override
	public int compareTo(CoordState state) {
		return this.get_fCost() - state.get_fCost();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CoordState)) {
			return false;
		}
		
		final CoordState state = (CoordState) o;
		
		if (this.get_coordinate().get_x() != state.get_coordinate().get_x()) {
			return false;
		}
		
		if (this.get_coordinate().get_y() != state.get_coordinate().get_y()) {
			return false;
		}
		
		if (this.get_numDynamite() != state.get_numDynamite()) {
			return false;
		}
		
		if (this.get_numRaft() != state.get_numRaft()) {
			return false;
		}
		
		return true;
	}

	// Create unique hashcode for each coordinate
	@Override
	public int hashCode() {
		int result = this.coordinate.get_x();
		result = 17 * result + this.coordinate.get_y();
		return result;
	}
	
	public Coordinate get_coordinate() {
		return coordinate;
	}

	public void set_oordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public int get_numDynamite() {
		return numDynamite;
	}

	public void set_numDynamite(int numDynamite) {
		this.numDynamite = numDynamite;
	}

	public int get_numRaft() {
		return numRaft;
	}

	public void set_numRaft(int numRaft) {
		this.numRaft = numRaft;
	}

	public CoordState get_prevState() {
		return prevState;
	}

	public void set_prevState(CoordState prevState) {
		this.prevState = prevState;
	}
	
	public int get_gCost() {
		return gCost;
	}
	
	public void set_gCost(int gCost) {
		this.gCost = gCost;
	}
	
	public int get_hCost() {
		return hCost;
	}
	
	public void set_hCost(int h2cost, Coordinate goal) {
		this.hCost = h2cost + (Math.abs(coordinate.get_x() - goal.get_x()) + Math.abs(coordinate.get_y() - goal.get_y()));
		// our heuristic is a combination of logic based on what is at that coordinate(the value for this is calculated in the a* method)
		// and manhattan distance
	}
	
	public int get_fCost() {
		return gCost + hCost;
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
	private Map<String, Integer> inventory = new HashMap<String, Integer>();
	// Maintains a queue of moves to execute
	private Queue<Character> moveQueue = new LinkedList<Character>();
	// Various move queues to follow
	private Queue<Character> itemMoveQueue = new LinkedList<Character>();
	private Queue<Character> treeMoveQueue = new LinkedList<Character>();
	private Queue<Character> returnMoveQueue = new LinkedList<Character>();
	private Queue<Character> treasureMoveQueue = new LinkedList<Character>();
	
	// Indicator for if the agent has seen an item on its' adventure
	Boolean foundItem = false;
	// flag and coord for if an item appears in the 5x5
	Coordinate itemCoord = new Coordinate(-90, -90);
	Coordinate treasureCoord = new Coordinate(-90, -90);
	// Indicator for tree to cut down
	Boolean foundTree = false;
	//flag and coord for if a tree appears in the 5x5
	Coordinate treeCoord = new Coordinate(-90, -90);
	
	Boolean pathForTreasure = true;
	
	Boolean useRaftExplore = false;
	Boolean onWater = false;
	
	// Checks if a provided character is an obstacle
	private boolean isObstacle(char spaceToCheck) {

		if (spaceToCheck == 'T' || spaceToCheck == '-' || spaceToCheck == '*' || spaceToCheck == '~' || spaceToCheck == '.') {
			if (spaceToCheck == '~' && useRaftExplore == true) {
				return false;
			}
			return true;
		}
		return false;
	}

	// input 2 adjacent coordinates and this function will queue up the required moves to move there
	private int moveDirection(Coordinate from, Coordinate to, int prevDirection, Queue<Character> moveQueue) {
		int nextDirection = 0;
		// Change player direction to face goal location
		if (from.get_x() == to.get_x() - 1) {
			nextDirection = 0;
		} else if (from.get_x() == to.get_x() + 1) {
			nextDirection = 2;
		} else if (from.get_y() == to.get_y() - 1) {
			nextDirection = 1;
		} else if (from.get_y() == to.get_y() + 1) {
			nextDirection = 3;
		} else {
			// System.out.println("moveDirection: wrong input\n");
			return -99;
		}
		
		int rotates = prevDirection - nextDirection;
		//rotate to face right direction
		if (rotates < 0){
			while(rotates < 0) {
				moveQueue.add('l');
				rotates++;
			}
		} else if (rotates > 0) {
			while (rotates > 0) {
				moveQueue.add('r');
				rotates--;
			}
		}
		//if item use is needed, queue up its use and flag it as false in hashmap
		if (map.get(to) == '-' && inventory.get("key") == 1) {
			moveQueue.add('u');
			map.put(to, ' ');
			pathForTreasure = true;
		} else if (map.get(to) == 'T' && inventory.get("axe") == 1) {
			moveQueue.add('c');
			map.put(to, ' ');
			inventory.put("raft", 1);
			pathForTreasure = true;
		} else if (map.get(to) == '*' && inventory.get("dynamite") != 0) {
			moveQueue.add('b');
			int currentDynamite = inventory.get("dynamite");
			inventory.put("dynamite", currentDynamite-1);
			pathForTreasure = true;
			map.put(to, ' ');
		}

		moveQueue.add('f');
		return nextDirection;
	}
	
	// Keep wall/obstacles to the left and follow, move forward otherwise 
	public void wallFollow(char view[][]) {
		char move = 'Z';

		char frontView = view[1][2];
		char backView = view[3][2];
		char rightView = view[2][3];
		char leftView = view[2][1];
		
		
		if (isObstacle(leftView) == true) {
			following = true;
		}
		
		// If no obstacles in front then move forward or rotate right if obstacle directly ahead
		if (isObstacle(frontView)) {
			move = 'r';
		} else {
         move = 'f';
		}
		
		// If previously following a wall, turn to ensure we keep it on the left
		if (following == true && isObstacle(leftView) == false && lastMove != 'l') {
			move = 'l';
		}
		if (explored.get(currentLocation) > 2) {
			following = false;
			if (inventory.get("raft") == 1 || onWater == true) {
				useRaftExplore = true;
			}
		}
		/*
		 * t
		 * 
		 * /* the following code should now be redundant // Prevents following infinitely along an obstacle, move on if we arrive at same spot
		 * more than twice if (explored.get(currentLocation) > 2) { following = false; }
		 */
		
		// System.out.format("wall follow added move %c \n", move);
		moveQueue.add(move);
   }

	public void updateMapAndDirection(char view[][]) {



      // Start of game,  map starting view
      if(lastMove == 'Z'){
      	//initialising inventory storing
      	inventory.put("raft", 0);
      	inventory.put("key", 0);
      	inventory.put("dynamite", 0);
      	inventory.put("axe", 0);
      	inventory.put("treasure", 0);
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
					// if item appears trigger flag and store coord
             	if(view[counter2][counter] == 'a' || view[counter2][counter] == 'k' || view[counter2][counter] == 'd' || view[counter2][counter] == '$'){
             		foundItem = true;
             		itemCoord = coord;
             	}
             	// if tree appears trigger flag and store coord
					if (view[counter2][counter] == 'T' && inventory.get("axe") > 0) {
             		foundTree = true;
						treeCoord = coord;
             	}
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
					char discoveredChar = view[0][viewCounter];
					map.put(discovery, discoveredChar);
					explored.put(discovery, 0);
					// if item appears trigger flag and store coord
					if (discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd') {
             		foundItem = true;
             		itemCoord = discovery;
             	}
					if (discoveredChar == '$') {
						treasureCoord = discovery;
					}
             	if(discoveredChar == 'T'){
             		foundTree = true;
						treeCoord = discovery;
             	}
					System.out.format("discovery = (%d,%d) ", discovery.get_x(), discovery.get_y());
					System.out.print("put into map = |" + map.get(discovery) + "|" + "\n");
				

					viewCounter++;
				}
         // Move North
         } else if(direction == 1){
				currentLocation.set_y(currentLocation.get_y() + 1);
				int viewCounter = 0;
				for (int counter = -2; counter <= 2; counter++) {
					Coordinate discovery = new Coordinate(currentLocation.get_x() + counter, currentLocation.get_y() + 2);
					char discoveredChar = view[0][viewCounter];
					map.put(discovery, discoveredChar);
					explored.put(discovery, 0);
					if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
					if (discoveredChar == '$') {
						treasureCoord = discovery;
					}
             	if(discoveredChar == 'T'){
             		foundTree = true;
						treeCoord = discovery;
             	}
					System.out.print("put into map = |" + discoveredChar + "|" + "\n");
               viewCounter++;
            }
         // Move West
         } else if(direction == 2) {
				currentLocation.set_x(currentLocation.get_x() - 1);
				int viewCounter = 0;
				for (int counter = -2; counter <= 2; counter++) {
					Coordinate discovery = new Coordinate(currentLocation.get_x() - 2, currentLocation.get_y() + counter);
					char discoveredChar = view[0][viewCounter];
					map.put(discovery, discoveredChar);
					explored.put(discovery, 0);
					if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
					if (discoveredChar == '$') {
						treasureCoord = discovery;
					}
             	if(discoveredChar == 'T'){
             		foundTree = true;
						treeCoord = discovery;
             	}
					System.out.print("put into map = |" + discoveredChar + "|" + "\n");
					viewCounter++;
            }
         // Move South
         } else if(direction == 3) {
        	 currentLocation.set_y(currentLocation.get_y()-1);
        	 int viewCounter = 0;
        	 for(int counter = 2; counter >= -2; counter--){
        		 Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()-2);
        		 char discoveredChar = view[0][viewCounter];
        		 map.put(discovery, discoveredChar);
        		 explored.put(discovery, 0);
        		 if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
					if (discoveredChar == '$') {
						treasureCoord = discovery;
					}
             	if(discoveredChar == 'T'){
             		foundTree = true;
						treeCoord = discovery;
             	}
					System.out.print("put into map = |" + discoveredChar + "|" + "\n");
        		 viewCounter++;
            }
         }
         //if current location had item then add to inventory hash
         if(map.get(currentLocation) == 'a') {
				inventory.put("axe", 1);
				map.put(currentLocation, ' ');
				foundItem = false;
         }
         int currentDynamite = inventory.get("dynamite");
			if (map.get(currentLocation) == 'd') {
				inventory.put("dynamite", currentDynamite + 1);
				map.put(currentLocation, ' ');
				foundItem = false;
			}
			if (map.get(currentLocation) == 'k') {
				inventory.put("key", 1);
				map.put(currentLocation, ' ');
				foundItem = false;
			}
			if (map.get(currentLocation) == '$') {
				treasureCoord = new Coordinate(-90, -90);
				inventory.put("treasure", 1);
				map.put(currentLocation, ' ');
				foundItem = false;
			}

      }
   }
	
	// Gets all documented adjacent coordinates for a given coordinate
	private ArrayList<Coordinate> getAdjacent(Coordinate currCoord) {
		ArrayList<Coordinate> adjacentCoords = new ArrayList<Coordinate>();
		Coordinate adjNorth = new Coordinate(currCoord.get_x(), currCoord.get_y() + 1);
		Coordinate adjEast = new Coordinate(currCoord.get_x() + 1, currCoord.get_y());
		Coordinate adjSouth = new Coordinate(currCoord.get_x(), currCoord.get_y() - 1);
		Coordinate adjWest = new Coordinate(currCoord.get_x() - 1, currCoord.get_y());
		
		if (map.containsKey(adjEast)) {
			adjacentCoords.add(adjEast);
		}
		if (map.containsKey(adjNorth)) {
			adjacentCoords.add(adjNorth);
		}
		if (map.containsKey(adjWest)) {
			adjacentCoords.add(adjWest);
		}
		if (map.containsKey(adjSouth)) {
			adjacentCoords.add(adjSouth);
		}
		
		return adjacentCoords;
	}
	
	// Calculates heuristics of a certain coordinate, heuristic is very high for coordinates with obstacles or normal if we have an item to
	// clear obstacle
	public int calculateH2Cost(CoordState currentState) {
		int h2cost = 0;
		Coordinate current = currentState.get_coordinate();
		if (map.get(current) == '.') {
			h2cost = 100000;
		} else if (map.get(current) == 'T' && inventory.get("axe") == 0) {
			h2cost = 90000;
		} else if (map.get(current) == '-' && inventory.get("key") == 0){
			h2cost = 90000;
		} else if (map.get(current) == '*' && currentState.get_numDynamite() <= 0) {
			h2cost = 90000;
		} else if (map.get(current) == '*' && currentState.get_numDynamite() > 0) {
			h2cost = 2000;
		}
		if (map.get(current) == '~' && currentState.get_numRaft() <= 0) {
			h2cost = 90000;
		} else if (map.get(current) == '~' && currentState.get_numRaft() > 0) {
			h2cost = 1000;
		}
		

		return h2cost;
	}
	
	// A* Search for path-finding between two coordinates
	public Stack<CoordState> aStar(CoordState startState, Coordinate goal) {
		Stack<CoordState> statePath = new Stack<CoordState>();
		PriorityQueue<CoordState> open = new PriorityQueue<CoordState>();
		ArrayList<CoordState> closed = new ArrayList<CoordState>();
		
		
		// Heuristic cost of a coordinate based on what value the coordinate holds
		int h2Cost = 0;
		int gCost = 0;
		
		startState.set_hCost(0, goal);
		// Set fCost for start
		open.add(startState);
		
		while (!open.isEmpty()) {
			CoordState currState = open.poll();
			
			if (startState.get_coordinate().equals(goal)) {
				return statePath;
			}
			// If current coordinate is goal, we have completed search	
			if (currState.get_coordinate().equals(goal)) {
				int pathCost = 0;
				statePath.push(currState);
				while (!currState.get_prevState().get_coordinate().equals(startState.get_coordinate())) {
					pathCost += currState.get_prevState().get_fCost();
					// DEBUG
					statePath.push(currState.get_prevState());
					currState = currState.get_prevState();
				}
				if (pathCost >= 90000) {
					statePath.clear();
					inventory.put("dynamite", startState.get_numDynamite());
					return statePath;
				} else {
					return statePath;
				}
			}
			
			ArrayList<Coordinate> adjacentCoords = getAdjacent(currState.get_coordinate());
			
			closed.add(currState);
			for (Coordinate nextCoord : adjacentCoords) {
				CoordState nextState;
				// All path movements are of "cost" 1, gCost of a coordinate is gCost of previous coordinate + 1
				gCost = currState.get_gCost() + 1;
				
				nextState = new CoordState(nextCoord, gCost, currState, currState.get_numDynamite(), currState.get_numRaft());
				// If the path contains a wall and we have dynamite then we can use it
				
				if (map.get(currState.get_coordinate()) == '~' && map.get(nextCoord) != '~') {
					nextState.set_numRaft(0);
				}
				
				// Calculate heuristic costs
				h2Cost = calculateH2Cost(nextState);
				
				if (map.get(nextCoord) == '*' && h2Cost < 90000) {
					nextState.set_numDynamite(nextState.get_numDynamite() - 1);
				}
				
				
				nextState.set_hCost(h2Cost, goal);
				
				
				// If closed set contains adjacent coordinate move onto next coordinate
				if (closed.contains(nextState)) {
					continue;
				}
				
				// If open set does not contain adjacent coordinate,
				if (!open.contains(nextState)) {
					open.add(nextState);
				}
				
			}
		}
		
		return statePath;
	}

	// Returns the pathCost to retrieve an item it sees
	public boolean retrieveItem(Coordinate itemCoord) {
		// Keep track of the total path cost to check for obstacles

		int pathDirection = direction;
		CoordState startState = new CoordState(currentLocation, 0, null, inventory.get("dynamite"), inventory.get("raft"));
		Stack<CoordState> makeMovesToItem = aStar(startState, itemCoord);
		// Create a path to the item from current location
		Coordinate currCoord = currentLocation;
		if (!makeMovesToItem.isEmpty()) {
			while (!makeMovesToItem.isEmpty()) {
				CoordState nextState = makeMovesToItem.pop();
				Coordinate nextCoord = nextState.get_coordinate();
				
				pathDirection = moveDirection(currCoord, nextCoord, pathDirection, itemMoveQueue);
				currCoord = nextCoord;
				
			}
				
			return true;
		}
		
		foundItem = false;
		return false;
	}

	public boolean retrieveTreasure() {
		// Keep track of the total path cost to check for obstacles
		int pathDirection = direction;
		CoordState startState = new CoordState(currentLocation, 0, null, inventory.get("dynamite"), inventory.get("raft"));
		
		Stack<CoordState> makeMovesToItem = aStar(startState, treasureCoord);
		
		// Create a path to the item from current location
		Coordinate currCoord = currentLocation;
		CoordState currState = startState;
		
		if (!makeMovesToItem.isEmpty()) {
			while (!makeMovesToItem.isEmpty()) {
				// DEBUG
				CoordState nextState = makeMovesToItem.pop();
				Coordinate nextCoord = nextState.get_coordinate();
				
				
				pathDirection = moveDirection(currCoord, nextCoord, pathDirection, treasureMoveQueue);
				
				currState = nextState;
				currCoord = nextCoord;
				
			}
			
			// Check that a valid path exists back to the start from the treasure before retrieving it
			Stack<CoordState> returnToStart = aStar(currState, new Coordinate(0, 0));
			// Put path back into return queue;
			if (!returnToStart.isEmpty()) {
				currCoord = currState.get_coordinate();
				while (!returnToStart.isEmpty()) {
					CoordState nextState = returnToStart.pop();
					Coordinate nextCoord = nextState.get_coordinate();
					
					pathDirection = moveDirection(currCoord, nextCoord, pathDirection, returnMoveQueue);
					currState = nextState;
					currCoord = nextCoord;
					
				}
				return true;
			
			} else {
				treasureMoveQueue.clear();
				pathForTreasure = false;
				return false;
			}
		}
		return false;
	}
	
	public boolean cutTree() {
		// Keep track of the total path cost to check for obstacles
		int pathDirection = direction;
		CoordState startState = new CoordState(currentLocation, 0, null, inventory.get("dynamite"), inventory.get("raft"));
		Stack<CoordState> makeMovesToTree = aStar(startState, treeCoord);
		
		// Create a path to the item from current location
		if (!makeMovesToTree.isEmpty()) {
			Coordinate currCoord = currentLocation;
			while (!makeMovesToTree.isEmpty()) {
				// DEBUG
				CoordState nextState = makeMovesToTree.pop();
				Coordinate nextCoord = nextState.get_coordinate();
				
				// System.out.print("I see the item\n");
				pathDirection = moveDirection(currCoord, nextCoord, pathDirection, treeMoveQueue);
				currCoord = nextCoord;
				
			}
			
			return true;
		}
		foundTree = false;
		return false;
	}
	
	public char get_action(char view[][]) {

		// At each move update map and direction
		updateMapAndDirection(view);
		
		// Print current view of map
		print_view(view);
		
		// Initialise nextMove
		char nextMove = ' ';
		
		// If treasure has been found, make a path to treasure
		if (treasureCoord.get_x() > -90 && treasureMoveQueue.isEmpty() && pathForTreasure && treeMoveQueue.isEmpty()) {
			retrieveTreasure();
		}
		// Run path to treasure
		if (!treasureMoveQueue.isEmpty()) {
			lastMove = treasureMoveQueue.peek();
			return nextMove = treasureMoveQueue.poll();
		}
		
		// Execute path back if it exists;
		if (!returnMoveQueue.isEmpty() && inventory.get("treasure") == 1) {
			lastMove = returnMoveQueue.peek();
			return nextMove = returnMoveQueue.poll();
		}
		
		// Plan path to item if we aren't already moving toward an item
		if (foundItem == true && itemMoveQueue.isEmpty()) {
			// Check that we are able to retrieve item with items we already have
			retrieveItem(itemCoord);
		}
		// If there are moves to be executed to retrieve the item, execute them
		if (!itemMoveQueue.isEmpty()) {
			nextMove = itemMoveQueue.poll();
			lastMove = nextMove;
			return nextMove;
		}
		
		// If there are trees to cut, cut 'em
		if (foundTree == true && inventory.get("axe") == 1 && treeMoveQueue.isEmpty()) {
			cutTree();
		}
		
		if (!treeMoveQueue.isEmpty()) {
			nextMove = treeMoveQueue.poll();
			lastMove = nextMove;
			return nextMove;
		}
		



		wallFollow(view);
		nextMove = moveQueue.poll();
		// Update last move
		lastMove = nextMove;
		return nextMove;
		
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
