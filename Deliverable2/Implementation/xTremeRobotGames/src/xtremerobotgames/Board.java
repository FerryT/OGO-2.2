/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package xtremerobotgames;

import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;

/**
 *
 * @author s102231
 */
public class Board {

    private static int width = 10;
    private static int height = 10;
    private Tile[][] tiles;
    public Controller controller;
    private RobotCoord robots;
    private HashMap<Robot, Tile> home;
    private HashMap<Robot, Rotation> robotRotation;

    //TO DO constructor
    public Board(){
        tiles = new Tile[width][height];
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                tiles[i][j] = new NormalTile();
            }
        }
        robots = new RobotCoord();
        home = new HashMap<Robot, Tile>();
        robotRotation = new HashMap<Robot, Rotation>();
        controller = new Controller();
    }

    public void addRobot(Robot r, AbsoluteCoord abs, AbsoluteCoord hometile, Rotation rot){
        robots.addRobot(r, abs);
        tiles[hometile.getX()][hometile.getY()] = new HomeTile(r);
        home.put(r, tiles[hometile.getX()][hometile.getY()]);
        robotRotation.put(r, rot);
    }

    public void addConveyorTile(AbsoluteCoord abs, Rotation rot){
        tiles[abs.getX()][abs.getY()] = new ConveyorTile(rot);
    }

    public void addBrokenRobotTile(AbsoluteCoord abs){
        tiles[abs.getX()][abs.getY()] = new BrokenRobotTile();
    }

    public void addHintTile(AbsoluteCoord abs){
        tiles[abs.getX()][abs.getY()] = new HintTile();
    }
    
    //might be a better way to solve this
    public boolean canReset(){
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                if(tiles[i][j].getClass() == HomeTile.class){
                    HomeTile ht = (HomeTile) tiles[i][j];
                    if(ht.homeRobot == ht.occupier){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //DONE for the move..
    public BoardResponse moveRequest(RelativeCoord loc, Robot r, Rotation rot){
       /**

        if(rot != Rotation.R0DEG){                  //rulecheck
            if(r.r.possibleRotations.contains(rot)){
                robotRotation.remove(r);
                robotRotation.put(r, rot);
                return BoardResponse.SUCCESS;
            } else {
                return BoardResponse.FAILED;
            }
        } else if(!r.r.possibleMoves.contains(loc)){    //rulecheck
            return BoardResponse.FAILED;
        } else {
        *
        */

            AbsoluteCoord position = addAbstoRel(robots.getAbsoluteCoord(r), loc);
            if(position == null){
                return BoardResponse.FAILED;
            } else if (tiles[position.getX()][position.getY()].getClass() == HomeTile.class){
                HomeTile ht = (HomeTile) tiles[position.getX()][position.getY()];
                    if(ht.homeRobot == r){
                        return BoardResponse.WIN;
                    } else {
                    saveLocation(position, r);
                    return BoardResponse.SUCCESS;
                    }
            } else {
                /*
                 if(tiles[position.getX()][position.getY()].getClass() == ConveyorTile.class){
                    AbsoluteCoord nposition = conveyorMove(position, r);
                    if(nposition != position){
                        position = nposition;
                        r.notifyAutoMovement();
                    }
                }
                 * 
                 */
                saveLocation(position, r);
                return BoardResponse.SUCCESS;
            }
        
    }

    //DONE
    public BoardSnapshot requestSnapshot(){
        Tile[][] tiles2 = new Tile[width][height];
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                tiles2[i][j] = tiles[i][j].clone();
            }
        }
        BoardSnapshot snapshot = new BoardSnapshot(tiles2);
        return snapshot;
    }

    //should be done
    public RobotPair requestTilesExchange(){
        TilePair switchTiles = getValidTiles();
        AbsoluteCoord coord1 = null;
        AbsoluteCoord coord2 = null;

        //get coords of the tiles
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                if(tiles[i][j].equals(switchTiles.tile1)){
                    coord1 = new AbsoluteCoord(i,j);
                }
                if(tiles[i][j].equals(switchTiles.tile2)){
                    coord2 = new AbsoluteCoord(i,j);
                }
            }
        }

        //switch the tiles
        tiles[coord1.getX()][coord1.getY()] = switchTiles.tile2;
        tiles[coord2.getX()][coord2.getY()] = switchTiles.tile1;

        //calculate new positions
        if (switchTiles.tile1.occupier != null){
            saveLocation(calculateLocation(coord2, switchTiles.tile1.occupier), switchTiles.tile1.occupier);
            controller.notifyAutoMovement(switchTiles.tile1.occupier);                  //always notify of automovement
        } else {
            checkAround(coord2);    //check if conveyortiles pointing towards this tile might move, since it is (now) empty
        }
        if (switchTiles.tile2.occupier != null){
            saveLocation(calculateLocation(coord1, switchTiles.tile2.occupier), switchTiles.tile2.occupier);
            controller.notifyAutoMovement(switchTiles.tile2.occupier);
        } else {
           checkAround(coord1);  
        }



        return null;
    }

    //get new position of robot
    public AbsoluteCoord calculateLocation(AbsoluteCoord absCoord, Robot r){
        if(tiles[absCoord.getX()][absCoord.getY()].getClass() == ConveyorTile.class ){
            return conveyorMove(absCoord, r);
        } else {
            return absCoord;
        }
    }

    //pre: robot is on a conveyorBelt
    public AbsoluteCoord conveyorMove(AbsoluteCoord absCoord, Robot r){
        AbsoluteCoord destination;
        ConveyorTile ct = (ConveyorTile) tiles[absCoord.getX()][absCoord.getY()];
        destination = addAbstoRel(absCoord, ct.getRelativeCoord());
        //if he cant move, do not move, else do move and check environment
        if(destination == null || tiles[destination.getX()][destination.getY()].getClass() == BrokenRobotTile.class || tiles[destination.getX()][destination.getY()].occupier != null){
            return absCoord;
        } else {
            destination = conveyorMove(destination, r);
            saveLocation(destination, r);
            controller.viewer.notifyStateChange();
            checkAround(absCoord);
            return destination;
        }
    }

    //check if conveyor tile is there and it is possible to move
    public void checkConveyor(AbsoluteCoord absCoord){
        AbsoluteCoord destination;
        if(tiles[absCoord.getX()][absCoord.getY()].getClass() == ConveyorTile.class && tiles[absCoord.getX()][absCoord.getY()].occupier != null && absCoord != null){
            Robot r = robots.getRobot(absCoord);
            destination = conveyorMove(absCoord, r);
            if(destination != absCoord){
                controller.notifyAutoMovement(r);
                saveLocation(destination, r);
            }
        }
    }

    public void checkAround(AbsoluteCoord absCoord){
        RelativeCoord checkConveyor = new RelativeCoord(0, -1);
        checkConveyor(addAbstoRel(absCoord, checkConveyor));
        checkConveyor = new RelativeCoord(0,1);
        checkConveyor(addAbstoRel(absCoord, checkConveyor));
        checkConveyor = new RelativeCoord(-1,0);
        checkConveyor(addAbstoRel(absCoord, checkConveyor));
        checkConveyor = new RelativeCoord(1,0);
        checkConveyor(addAbstoRel(absCoord, checkConveyor));
    }

    //need to take rotation of robot in account....
    public Hint getHint(Robot r){
        Hint hint;
        AbsoluteCoord coordRobot;
        AbsoluteCoord coordHome = null;
        coordRobot = robots.getAbsoluteCoord(r);
        ArrayList<Hint> hints = new ArrayList<Hint>();

        //check where the hometile lies
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                if( tiles[i][j].equals(home.get(r))){
                    coordHome = new AbsoluteCoord(i,j);
                    break;
                }
            }
        }

        //determine hint, robot to the right of hometile, robot.x > hometile.x
        if(coordRobot.getX() > coordHome.getX()){
            if(coordRobot.getY() == coordHome.getY()){
                hint = Hint.WEST;
            } else if(coordRobot.getY() < coordHome.getY()){
                hints.add(Hint.WEST);
                hints.add(Hint.SOUTH);
                hints.add(Hint.SOUTH_WEST);
                hint = pickHint(hints);
            } else {
                hints.add(Hint.WEST);
                hints.add(Hint.NORTH);
                hints.add(Hint.NORTH_WEST);
                hint = pickHint(hints);
            }

        //determine hint, robot to the left of hometile, robot.x < hometile.x
        } else if(coordRobot.getX() < coordHome.getX()){
            if(coordRobot.getY() == coordHome.getY()){
                hint = Hint.EAST;
            } else if(coordRobot.getY() < coordHome.getY()){
                hints.add(Hint.EAST);
                hints.add(Hint.SOUTH);
                hints.add(Hint.SOUTH_EAST);
                hint = pickHint(hints);
            } else {
                hints.add(Hint.EAST);
                hints.add(Hint.NORTH);
                hints.add(Hint.NORTH_EAST);
                hint = pickHint(hints);
            }

        //robot north / south of hometile, same x
        } else {
            if(coordRobot.getY() > coordHome.getY()){
                hint = Hint.SOUTH;
            } else {
                hint = Hint.NORTH;
            }
        }
        return calculateHint(hint, robotRotation.get(r));
    }

    //sub function to randomly pick a hint from a list
    private Hint pickHint(ArrayList<Hint> hints){
        int pick = new Random().nextInt(hints.size());
        return hints.get(pick);
    }

    private Hint calculateHint(Hint hint, Rotation r){
        int hintnumber;
        if(r == Rotation.R0DEG){
            hintnumber = 0;
        } else if (r == Rotation.R90DEG){
            hintnumber = 3;
        } else if (r == Rotation.R180DEG){
            hintnumber = 2;
        } else{
            hintnumber = 1;
        }

        if(getInt(hint) > 3){
            return getHint(((getInt(hint) + hintnumber) % 4) + 4);
        } else {
            return getHint(getInt(hint) + hintnumber % 4);
        }


    }

    //maping from ints to hints
    public Hint getHint(int i){
        if(i==0){
            return Hint.NORTH;
        } else if(i == 1){
            return Hint.EAST;
        } else if(i == 2){
            return Hint.SOUTH;
        } else if(i == 3){
            return Hint.WEST;
        } else if(i == 4){
            return Hint.NORTH_EAST;
        } else if(i == 5){
            return Hint.SOUTH_EAST;
        } else if(i == 6){
            return Hint.SOUTH_WEST;
        } else if(i == 7){
            return Hint.NORTH_WEST;
        } else {
            return null;
        }
    }

    //mapping from hints to ints
    public int getInt(Hint h){
        if(h == Hint.NORTH){
            return 0;
        } else if(h == Hint.EAST){
            return 1;
        } else if(h == Hint.SOUTH){
            return 2;
        } else if(h == Hint.WEST){
            return 3;
        } else if(h == Hint.NORTH_EAST){
            return 4;
        } else if(h == Hint.SOUTH_EAST){
            return 5;
        } else if(h == Hint.SOUTH_WEST){
            return 6;
        } else if(h == Hint.NORTH_WEST){
            return 7;
        } else {
            return -1;
        }
    }

    //TO DO
    private AbsoluteCoord calculateNewLocation(RelativeCoord loc, Robot r){
        AbsoluteCoord position = robots.getAbsoluteCoord(r);
        AbsoluteCoord destination = addAbstoRel(position, loc);
        int x = destination.getX() - position.getX();
        int y = destination.getY() - position.getY();
        if( destination == null){
            return null;
        } else {
           if(x > 0 || y > 0){ //otherwise smaller than 0 for sure
               if(x == 0){
                   for(int i = 0; i <= y; i++){
                       if(tiles[position.getX()][(position.getY()+i)].getClass() == BrokenRobotTile.class){
                           return null;
                       } else if(tiles[position.getX()][(position.getY()+i)].getClass() == ConveyorTile.class){
                           RelativeCoord help = new RelativeCoord(0,i);
                           return addAbstoRel(position, help);
                       }
                   }
               } else {
                   for(int i = 0; i <= x; i++){
                       if(tiles[(position.getX()+i)][position.getY()].getClass() == BrokenRobotTile.class){
                           return null;
                       } else if(tiles[position.getX()+i][position.getY()].getClass() == ConveyorTile.class){
                           RelativeCoord help = new RelativeCoord(i,0);
                           return addAbstoRel(position, help);
                       }
                   }
               }
           } else {
               if(x == 0){
                   for(int i = 0; i >= y; i--){
                       if(tiles[position.getX()][(position.getY()+i)].getClass() == BrokenRobotTile.class){
                           return null;
                       } else if(tiles[position.getX()][(position.getY()+i)].getClass() == ConveyorTile.class){
                           RelativeCoord help = new RelativeCoord(0,i);
                           return addAbstoRel(position, help);
                       }
                   }
               } else {
                   for(int i = 0; i >= x; i--){
                       if(tiles[(position.getX()+i)][position.getY()].getClass() == BrokenRobotTile.class){
                           return null;
                       } else if(tiles[position.getX()+i][position.getY()].getClass() == ConveyorTile.class){
                           RelativeCoord help = new RelativeCoord(i,0);
                           return addAbstoRel(position, help);
                       }
                   }
               }
           }
        }
        return null; // just to be sure
    }

    //TO DO
    private TilePair getValidTiles(){
        return null;
    }

    //TO DO
    private void reset(){
        if(canReset()){
           //reset.
        }

    }


    //function to save robot location, done
    private void saveLocation(AbsoluteCoord abs, Robot r){
        AbsoluteCoord coord = robots.getAbsoluteCoord(r);
        tiles[coord.getX()][coord.getY()].occupier = null;
        tiles[abs.getX()][abs.getY()].occupier = r;
        robots.changePosition(r, abs);
        System.out.println("Savend");
    }

    //function to add relative to absolute Coordinate, returns null if absCoord is not on the board
    private AbsoluteCoord addAbstoRel(AbsoluteCoord abs, RelativeCoord rel){
        AbsoluteCoord absCoord = new AbsoluteCoord(abs.getX() + rel.getX(), abs.getY() + rel.getY());
        if(absCoord.getX() >= width || absCoord.getY() >= height || absCoord.getX() < 0 || absCoord.getY() < 0){
            return null;
        } else {
            return absCoord;
        }
    }

    public Rotation getRotation(Robot r){
        return this.robotRotation.get(r);
    }
}
