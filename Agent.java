<<<<<<< Updated upstream
/*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2017
*/

import java.util.*;
import java.io.*;
import java.net.*;

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
	// 
	private Map<Coordinate, Integer> explored = new HashMap<Coordinate, Integer>();
	// Store last move
	private char lastMove = 'Z';
	// Store current character coordinate
	private Coordinate currentLocation = new Coordinate(0, 0);
	// Store character direction
	private int direction = 1;


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
            	   explored.put(coord, 1);
            	   Coordinate origin = new Coordinate(0,0);
            	   y--;
            	   continue;
               }
               map.put(coord, view[counter2][counter]);
               explored.put(coord, 0);
               y--;
            }
            x++;
         }
//         for(Coordinate coord:map.keySet()) {
//        	   System.out.println(coord);
//        	   System.out.println(map.get(coord));
//         }
//         

      // Update direction of character if rotated
      } else if(lastMove == 'l'){
         direction++;
         direction = direction%4;
      } else if(lastMove == 'r'){
         direction--;
         direction = direction%4;
      // Update map if last move was to move forward
      } else if(lastMove == 'f'){
         // Move East
         if(direction == 0){
            currentLocation.set_x(currentLocation.get_x()+1);
            int viewCounter = 0;
            for(int counter = 2; counter <= -2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+2, currentLocation.get_y()+counter);
               map.put(discovery, view[0][viewCounter]);
               viewCounter++;
            }
         // Move North
         } else if(direction == 1){
            currentLocation.set_y(currentLocation.get_y()+1);
            int viewCounter = 0;
            for(int counter = -2; counter <= 2; counter++){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()+2);
               map.put(discovery, view[0][viewCounter]);
               viewCounter++;
            }
         // Move West
         } else if(direction == 2) {
            currentLocation.set_x(currentLocation.get_x()-1);
            int viewCounter = 0;
            for(int counter = 2; counter <= -2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()-2, currentLocation.get_y()+counter);
               map.put(discovery, view[0][viewCounter]);
               viewCounter++;
            }
         // Move South
         } else if(direction == 3) {
            currentLocation.set_y(currentLocation.get_y()-1);
            int viewCounter = 0;
            for(int counter = -2; counter <= 2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()-2);
               map.put(discovery, view[0][viewCounter]);
               viewCounter++;
            }
         }
      }
   }

	public char get_action(char view[][]) {

		// At each move update map and direction
		updateMapAndDirection(view);
		print_view(view);
		if (view[1][2] == 'T' || view[1][2] == '-' || view[1][2] == '*' || view[1][2] == '~') {
			lastMove = 'r';
			return 'r';
		} else {
			lastMove = 'f';
			return 'f';
		}

		/*
		 * int ch=0;
		 * 
		 * System.out.print("Enter Action(s): ");
		 * 
		 * try { while ( ch != -1 ) { // read character from keyboard ch =
		 * System.in.read();
		 * 
		 * switch( ch ) { // if character is a valid action, return it case 'F':
		 * case 'L': case 'R': case 'C': case 'U': case 'B': case 'f': case 'l':
		 * case 'r': case 'c': case 'u': case 'b': return((char) ch ); } } }
		 * catch (IOException e) { System.out.println ("IO error:" + e ); }
		 */

		// return 0;
	}

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