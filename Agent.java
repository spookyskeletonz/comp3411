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
import java.util.ArrayList;
import java.util.Comparator;
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
	private int gCost = 0;
	private int hCost = 0;
	private Coordinate prevCoord;

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

	public int get_fCost() {
		int fcost = gCost + hCost;
		return fcost;
	}

	public void set_hCost(int h2cost, Coordinate goal) {
		this.hCost = h2cost + (Math.abs(x - goal.get_x()) + Math.abs(y - goal.get_y()));
		//our heuristic is a combination of logic based on what is at that coordinate(the value for this is calculated in the a* method)
		//and manhattan distance
	}

	public int get_hCost(){
		return hCost;
	}

	public int get_gCost() {
		return gCost;
	}

	public void set_gCost(int gCost) {
		this.gCost = gCost;
	}
	
	public Coordinate get_prevCoord(){
		return prevCoord;
	}
	
	public void set_prevCoord(Coordinate prevCoord) {
		this.prevCoord = prevCoord;
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
	// Queue of moves to pick up an item
	private Queue<Character> itemMoveQueue = new LinkedList<Character>();

	private Queue<Character> treeMoveQueue = new LinkedList<Character>();
	
	private Queue<Character> returnMoveQueue = new LinkedList<Character>();
	
	// Indicator for if the agent has seen an item on its' adventure
	Boolean foundItem = false;
	// flag and coord for if an item appears in the 5x5
	Coordinate itemCoord = new Coordinate(-90, -90);

	// Indicator for tree to cut down
	Boolean foundTree = false;
	//flag and coord for if a tree appears in the 5x5
	Coordinate treeCoord = new Coordinate(-90, -90);
	
	
	// Checks if a provided character is an obstacle
	private boolean isObstacle(char spaceToCheck) {
		if (spaceToCheck == 'T' || spaceToCheck == '-' || spaceToCheck == '*' || spaceToCheck == '~'){
			return true;
		}
		return false;
		
	}

	// input 2 adjacent coordinates and this function will queue up the required moves to move there
	private int moveDirection(Coordinate from, Coordinate to, int prevDirection, Queue<Character> moveQueue) {
		int nextDirection = 0;
		// DEBUG
		// System.out.format("moveDirection: from (%d, %d), to (%d, %d) \n", from.get_x(), from.get_y(), to.get_x(), to.get_y());
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
			System.out.println("moveDirection: wrong input\n");
			return -99;
		}
		
		int rotates = prevDirection - nextDirection;
		// DEBUG
		// System.out.format("moveDirection: rotates = %d - %d = %d \n", prevDirection, nextDirection, rotates);
		//rotate to face right direction
		if (rotates < 0){
			while(rotates < 0) {
				// System.out.print("ERROR HERE?\n");
				moveQueue.add('l');
				// DEBUG
				// System.out.format("moveDirection added move l to queue\n");
				rotates++;
			}
		} else if (rotates > 0) {
			while (rotates > 0) {
				// DEBUG
				// System.out.format("moveDirection added move r to queue\n");
				moveQueue.add('r');
				rotates--;
			}
		}
		//if item use is needed, queue up its use and flag it as false in hashmap
		if (map.get(to) == '-' && inventory.get("key") == 1) {
			moveQueue.add('u');
		} else if (map.get(to) == 'T' && inventory.get("axe") == 1) {
			moveQueue.add('c');
			//inventory.put("raft", 1);
		} else if (map.get(to) == '*' && inventory.get("dynamite") != 0) {
			moveQueue.add('b');
			int currentDynamite = inventory.get("dynamite");
			inventory.put("dynamite", currentDynamite-1);
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
		
		
		// // If any obstacle to the left, we are following
		// if (isObstacle(leftView) == true || explored.get(getAdjacent(currentLocation).get((direction + 1) % 4)) > 0) {
		// following = true;
		// }
		//
		// // If no obstacles in front then move forward or rotate right if obstacle directly ahead
		// if (isObstacle(frontView) || explored.get(getAdjacent(currentLocation).get(direction)) > 2) {
		// move = 'r';
		// } else {
		// move = 'f';
		// }
		//
		// // If previously following a wall, turn to ensure we keep it on the left
		// if (following == true && isObstacle(leftView) == false && lastMove != 'l'
		// || explored.get(getAdjacent(currentLocation).get((direction + 1) % 4)) > 2 && lastMove != 'l' && following == true) {
		// move = 'l';
		// }
		
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
		if (explored.get(currentLocation) > 2)
			following = false;
		/*
		 * t
		 * 
		 * /* the following code should now be redundant // Prevents following infinitely along an obstacle, move on if we arrive at same spot
		 * more than twice if (explored.get(currentLocation) > 2) { following = false; }
		 */
		
		System.out.format("wall follow added move %c \n", move);
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
             	if(view[counter2][counter] == 'T'){
             		foundTree = true;
             		itemCoord = coord;
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
             	if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd' || discoveredChar == '$'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
             	if(discoveredChar == 'T'){
             		foundTree = true;
             		itemCoord = discovery;
             	}
					System.out.print("put into map" + discoveredChar + "\n");
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
					if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd' || discoveredChar == '$'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
             	if(discoveredChar == 'T'){
             		foundTree = true;
             		itemCoord = discovery;
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
					// DEBUG
					// System.out.print("DISCOVERY IS " + discovery.get_x() + "," + discovery.get_y() + "\n");
					// System.out.print("ViewCounter = " + viewCounter + "\n");
					char discoveredChar = view[0][viewCounter];
					map.put(discovery, discoveredChar);
					explored.put(discovery, 0);
					if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd' || discoveredChar == '$'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
             	if(discoveredChar == 'T'){
             		foundTree = true;
             		itemCoord = discovery;
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
        		 if(discoveredChar == 'a' || discoveredChar == 'k' || discoveredChar == 'd' || discoveredChar == '$'){
             		foundItem = true;
             		itemCoord = discovery;
             	}
             	if(discoveredChar == 'T'){
             		foundTree = true;
             		itemCoord = discovery;
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
				inventory.put("treasure", 1);
				map.put(currentLocation, ' ');
				foundItem = false;
			}

      }
   }
	
	public class coordinateComparator implements Comparator<Coordinate> {
		
		@Override
		public int compare(Coordinate a, Coordinate b) {
			if (a.get_fCost() < b.get_fCost()) {
				return -1;
			} else if (a.get_fCost() > b.get_fCost()) {
				return 1;
			}
			return 0;
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
	public int calculateH2Cost(Coordinate current) {
		int h2cost = 0;
		if (map.get(current) == '.') {
			h2cost = 100000;
		} else if (map.get(current) == '~' && inventory.get("raft") == 0){
			h2cost = 90000;
		} else if (map.get(current) == 'T' && inventory.get("axe") == 0) {
			h2cost = 90000;
		} else if (map.get(current) == '-' && inventory.get("key") == 0){
			h2cost = 90000;
		} else if (map.get(current) == '*' && inventory.get("dynamite") == 0){
			h2cost = 90000;
		}
		return h2cost;
	}
	
	// A* Search for path-finding between two coordinates
	public Stack<Coordinate> aStar(Coordinate start, Coordinate goal) {
		Stack<Coordinate> path = new Stack<Coordinate>();
		Comparator<Coordinate> coordComparator = new coordinateComparator();
		PriorityQueue<Coordinate> open = new PriorityQueue<Coordinate>(100, coordComparator);
		ArrayList<Coordinate> closed = new ArrayList<Coordinate>();
		// Heuristic cost of a coordinate based on what value the coordinate holds
		int h2Cost = 0;
		start.set_gCost(0);
		start.set_hCost(0, goal);
		// Set fCost for start
		open.add(start);
		
		while (!open.isEmpty()) {
			Coordinate currCoord = open.poll();
			// DEBUG
			System.out.print("processing coordinate (" + currCoord.get_x() + "," + currCoord.get_y() + ")" + " fCost = "
					+ currCoord.get_fCost() + "\n\n");
			//
			// If current coordinate is goal, we have completed search
			if (currCoord.equals(goal)) {
				path.push(goal);
				while (!currCoord.get_prevCoord().equals(start)) {
					path.push(currCoord.get_prevCoord());
					currCoord = currCoord.get_prevCoord();
					// DEBUG
					// System.out.print("GOAL (" + currCoord.get_x() + "," + currCoord.get_y() + ")" + " prev = "
					// + currCoord.get_prevCoord().get_x() + "," + currCoord.get_prevCoord().get_y() + "\n\n");
				}
				return path;
			}
			
			ArrayList<Coordinate> adjacentCoords = getAdjacent(currCoord);
			
			closed.add(currCoord);
			for (Coordinate nextCoord : adjacentCoords) {

				// All path movements are of "cost" 1, gCost of a coordinate is gCost of previous coordinate + 1
				nextCoord.set_gCost(currCoord.get_gCost() + 1);
				h2Cost = calculateH2Cost(nextCoord);
				nextCoord.set_hCost(h2Cost, goal);
				nextCoord.set_prevCoord(currCoord);
				// If closed set contains adjacent coordinate move onto next coordinate
				if (closed.contains(nextCoord)) {
					continue;
				}
				// System.out
				// .print("next coord (" + nextCoord.get_x() + "," + nextCoord.get_y() + ")" + " fCost = " + nextCoord.get_fCost() + "\n\n");
				
				// If open set does not contain adjacent coordinate,
				if (!open.contains(nextCoord)) {
					open.add(nextCoord);
				}
				// Calculate heuristic costs
				// Set heuristic cost of coordinate
				
				// DEBUG
				// System.out.print("NEXT ELEMENT IN OPEN IS " + open.element().get_x() + "," + open.element().get_y());
				// DEBUG
				// System.out.print(" with fCost = " + open.element().get_fCost() + "\n");
				// DEBUG
				// System.out.print("prev = " + nextCoord.get_prevCoord().get_x() + "," + nextCoord.get_prevCoord().get_y() + "\n\n");
				
			}

			// When pulling adjacent coordinates, ensure that the coordinate to be expanded is contained in map
			
		}
		
		return path;
	}

	// Returns the pathCost to retrieve an item it sees
	public boolean retrieveItem() {
		// Keep track of the total path cost to check for obstacles
		int pathCost = 0;
		int pathDirection = direction;
		Stack<Coordinate> makeMovesToItem = aStar(currentLocation, itemCoord);
		// Create a path to the item from current location
		Coordinate currCoord = currentLocation;
		while (!makeMovesToItem.isEmpty()) {
			pathCost += currCoord.get_fCost();
			// DEBUG
			Coordinate nextCoord = makeMovesToItem.pop();
			
			// System.out.print("I see the item\n");
			pathDirection = moveDirection(currCoord, nextCoord, pathDirection, itemMoveQueue);
			currCoord = nextCoord;
		}
		// System.out.print("move Queue head is " + moveQueue.element());
		// DEBUG
		// System.out.format("next move is %c\n", itemMoveQueue.element());
		if (pathCost < 6400) {
			// // Return to where we started moving toward item
			// makeMovesToItem = aStar(itemCoord, currentLocation);
			// currCoord = itemCoord;
			// while (!makeMovesToItem.isEmpty()) {
			// Coordinate nextCoord = makeMovesToItem.pop();
			// moveDirection(currCoord, nextCoord);
			// currCoord = nextCoord;
			// }
			
		}			
		if (pathCost >= 90000) {
			itemMoveQueue.clear();
			return false;
		} else {
			return true;
		}
	}

	public boolean cutTree() {
		// Keep track of the total path cost to check for obstacles
		int pathCost = 0;
		int pathDirection = direction;
		Stack<Coordinate> makeMovesToTree = aStar(currentLocation, itemCoord);
		// Create a path to the item from current location
		Coordinate currCoord = currentLocation;
		while (!makeMovesToTree.isEmpty()) {
			pathCost += currCoord.get_fCost();
			// DEBUG
			Coordinate nextCoord = makeMovesToTree.pop();
			
			// System.out.print("I see the item\n");
			pathDirection = moveDirection(currCoord, nextCoord, pathDirection, treeMoveQueue);
			currCoord = nextCoord;
		}
		// System.out.print("move Queue head is " + moveQueue.element());
		// DEBUG
		// System.out.format("next move is %c\n", itemMoveQueue.element());
		if (pathCost < 6400) {
			// // Return to where we started moving toward item
			// makeMovesToItem = aStar(itemCoord, currentLocation);
			// currCoord = itemCoord;
			// while (!makeMovesToItem.isEmpty()) {
			// Coordinate nextCoord = makeMovesToItem.pop();
			// moveDirection(currCoord, nextCoord);
			// currCoord = nextCoord;
			// }
			
		}			
		if (pathCost >= 90000) {
			treeMoveQueue.clear();
			return false;
		} else {
			return true;
		}
	}
	
	public boolean returnToStart() {
		int pathCost = 0;
		int pathDirection = direction;
		Stack<Coordinate> returnPath = new Stack<Coordinate>();
		// Coordinate tempCoord = new Coordinate()
		// DEBUG //System.out.print("I HAVE THE TREASURE \n\n");
		// Plan a path back to start
		returnPath = aStar(currentLocation, new Coordinate(0, 0));
		
		// Add moves along the path to the move queue
		Coordinate currCoord = currentLocation;
		while (!returnPath.empty()) {
			pathCost += currCoord.get_fCost();
			Coordinate nextCoord = returnPath.pop();
			pathDirection = moveDirection(currCoord, nextCoord, pathDirection, returnMoveQueue);
			currCoord = nextCoord;
			
			// DEBUG
			System.out.print("Path back (" + nextCoord.get_x() + "," + nextCoord.get_y() + ")" + '\n');
			
		}
		if (pathCost >= 90000) {
			returnMoveQueue.clear();
			return false;
		} else {
			return true;
		}
	}
	
	public char get_action(char view[][]) {



		// At each move update map and direction
		updateMapAndDirection(view);
		// DEBUG
		System.out.print("last move was " + lastMove + " current direction = " + direction + "\n");
		System.out.print("x coordinate = " + currentLocation.get_x() + " y coordinate = " + currentLocation.get_y() + "\n");
		System.out.print("current location = " + map.get(currentLocation));
		// Print current view of map
		print_view(view);
		
		
		// Initialise nextMove
		char nextMove = ' ';
		
		// +++++ TEMP +++++
		// if (view[1][2] == '$') {
		// nextMove = 'f';
		// }
		// Plan path to item if we aren't already moving toward an item
		if (foundItem == true && itemMoveQueue.isEmpty()) {
			// Check that we are able to retrieve item with items we already have
			retrieveItem();
		}
		// If there are moves to be executed to retrieve the item, execute them
		if (!itemMoveQueue.isEmpty()) {
			nextMove = itemMoveQueue.poll();
			lastMove = nextMove;
			return nextMove;
		}

		if (foundTree == true && inventory.get("axe") == 1 && treeMoveQueue.isEmpty()){
			cutTree();
		}

		if (!treeMoveQueue.isEmpty()){
			nextMove = treeMoveQueue.poll();
			lastMove = nextMove;
			return nextMove;
		}

		
		// Plan a path back if the agent has picked up the treasure
		if (inventory.containsKey("treasure") && inventory.get("treasure") == 1 && returnMoveQueue.isEmpty()) {
			returnToStart();
		}
		if (!returnMoveQueue.isEmpty()) {
			lastMove = returnMoveQueue.peek();
			// DEBUG
			// System.out.print("RETURNING NOW \n");
			return nextMove = returnMoveQueue.poll();
		}
		

		wallFollow(view);
		nextMove = moveQueue.poll();
		// DEBUG
		System.out.print("NO ITEMS \n");
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
