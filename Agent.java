/*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2017
*/

import java.util.*;
import java.io.*;
import java.net.*;

class Coordinate {
   private int x;
   private int y;

   public Coordinate(int x, int y){
      this.x = x;
      this.y = y;
   }

   public int get_x(){
      return this.x;
   }

   public int get_y(){
      return this.y;
   }

   public void set_x(int x){
      this.x = x;
   }

   public void set_y(int y){
      this.y = y;
   }

}

public class Agent {

   private Map<Coordinate, Character> map = new HashMap<Coordinate, Character>();
   private char lastMove = 'Z';
   private Coordinate currentLocation = new Coordinate(0,0);
   private int direction = 1;

   public void updateMapAndDirection(char view[][]) {
      if(lastMove == 'Z'){
         int x = -2;
         for(int counter = 0; counter < 5; counter++){
            int y = 2;
            for(int counter2 = 0; counter2 < 5; counter2++){
               if(counter == 0 && counter2 == 0) continue;
               Coordinate coord = new Coordinate(x,y);
               map.put(coord, view[counter2][counter]);
               y--;
            }
            x++;
         }
      } else if(lastMove == 'l'){
         direction++;
         direction = direction%4;
      } else if(lastMove == 'r'){
         direction--;
         direction = direction%4;
      } else if(lastMove == 'f'){
         if(direction == 0){
            currentLocation.set_x(currentLocation.get_x()+1);
            int viewCounter = 0;
            for(int counter = 2; counter <= -2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+2, currentLocation.get_y()+counter);
               map.put(discovery, view[viewCounter][0]);
               viewCounter++;
            }
         } else if(direction == 1){
            currentLocation.set_y(currentLocation.get_y()+1);
            int viewCounter = 0;
            for(int counter = -2; counter <= 2; counter++){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()+2);
               map.put(discovery, view[viewCounter][0]);
               viewCounter++;
            }
         } else if(direction == 2) {
            currentLocation.set_x(currentLocation.get_x()-1);
            int viewCounter = 0;
            for(int counter = 2; counter <= -2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()-2, currentLocation.get_y()+counter);
               map.put(discovery, view[viewCounter][0]);
               viewCounter++;
            }
         } else if(direction == 3) {
            currentLocation.set_y(currentLocation.get_y()-1);
            int viewCounter = 0;
            for(int counter = -2; counter <= 2; counter--){
               Coordinate discovery = new Coordinate(currentLocation.get_x()+counter, currentLocation.get_y()-2);
               map.put(discovery, view[viewCounter][0]);
               viewCounter++;
            }
         }
      }
   }
   public char get_action( char view[][] ) {

      updateMapAndDirection(view);
      print_view(view);
      if(view[1][2] == 'T' || view[1][2] == '-' || view[1][2] == '*' || view[1][2] == '~'){
         return 'r';
      } else {
         return 'f';
      }

      /*int ch=0;

      System.out.print("Enter Action(s): ");

      try {
         while ( ch != -1 ) {
            // read character from keyboard
            ch  = System.in.read();

            switch( ch ) { // if character is a valid action, return it
            case 'F': case 'L': case 'R': case 'C': case 'U': case 'B':
            case 'f': case 'l': case 'r': case 'c': case 'u': case 'b':
               return((char) ch );
            }
         }
      }
      catch (IOException e) {
         System.out.println ("IO error:" + e );
      }*/

      //return 0;
   }

   void print_view( char view[][] )
   {
      int i,j;

      System.out.println("\n+-----+");
      for( i=0; i < 5; i++ ) {
         System.out.print("|");
         for( j=0; j < 5; j++ ) {
            if(( i == 2 )&&( j == 2 )) {
               System.out.print('^');
            }
            else {
               System.out.print( view[i][j] );
            }
         }
         System.out.println("|");
      }
      System.out.println("+-----+");
   }

   public static void main( String[] args )
   {
      InputStream in  = null;
      OutputStream out= null;
      Socket socket   = null;
      Agent  agent    = new Agent();
      char   view[][] = new char[5][5];
      char   action   = 'F';
      int port;
      int ch;
      int i,j;

      if( args.length < 2 ) {
         System.out.println("Usage: java Agent -p <port>\n");
         System.exit(-1);
      }

      port = Integer.parseInt( args[1] );

      try { // open socket to Game Engine
         socket = new Socket( "localhost", port );
         in  = socket.getInputStream();
         out = socket.getOutputStream();
      }
      catch( IOException e ) {
         System.out.println("Could not bind to port: "+port);
         System.exit(-1);
      }

      try { // scan 5-by-5 wintow around current location
         while( true ) {
            for( i=0; i < 5; i++ ) {
               for( j=0; j < 5; j++ ) {
                  if( !(( i == 2 )&&( j == 2 ))) {
                     ch = in.read();
                     if( ch == -1 ) {
                        System.exit(-1);
                     }
                     view[i][j] = (char) ch;
                  }
               }
            }
            //agent.print_view( view ); // COMMENT THIS OUT BEFORE SUBMISSION
            action = agent.get_action( view );
            out.write( action );
         }
      }
      catch( IOException e ) {
         System.out.println("Lost connection to port: "+ port );
         System.exit(-1);
      }
      finally {
         try {
            socket.close();
         }
         catch( IOException e ) {}
      }
   }
}
