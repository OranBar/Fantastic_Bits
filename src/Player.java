import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;
import java.awt.Point;

/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        System.err.println("teamid "+myTeamId);
                
        
        Game myGame = new Game();
        Napoleon myNapoleon = new Napoleon(myGame, myTeamId);
        
        myNapoleon.turns = -1;
        
        // game loop
        while (true) {
        	myNapoleon.turns++;
            // Inputs
            ///////////////////
            myGame.score[0] = in.nextInt();
            int myMagic = in.nextInt();
            
            myGame.score[1] = in.nextInt();
            myNapoleon.opponentMana = in.nextInt();
            
            myGame.clearEntities();
            
            int entities = in.nextInt(); // number of entities still in game
            System.err.println("entities "+entities);
            if(myNapoleon.turns == 0){
            	int noOfPlayers = 4;
            	int noOfBludgers = 2;
                myNapoleon.totalSnaffles = entities - noOfPlayers - noOfBludgers;
            }
            for (int i = 0; i < entities; i++) {
                int id = in.nextInt(); // entity identifier
                String entityType = in.next(); // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
                int x = in.nextInt(); // position
                int y = in.nextInt(); // position
                int vx = in.nextInt(); // velocity
                int vy = in.nextInt(); // velocity
                int state = in.nextInt(); // 1 if the wizard is holding a Snaffle, 0 otherwise
                
                Entity e = myGame.updateEntity(id, entityType, x, y, vx, vy, state);
            }
            
            ///////////////////
            
            String[] moves = myNapoleon.think();
            
            //Output - Movement
            ///////////////////
            System.out.println(moves[0]);
            System.out.println(moves[1]);
            ///////////////////
        }
    }
    
    public static class Napoleon {
        
    	//Constants
    	private final int obliviateCost = 5, petrificusCost = 10, accioCost = 15, flipendoCost = 20;
    	
        //Parameters
        private int accioMinDistanceThld = 2500;
        private int accioMaxDistanceThd = 5000;
        private int flipendoMinDistanceFromGoalThld = 2000;
        private int flipendoMaxDistanceThld = 5000;
        private int passingDistanceThld = 2000;
        
        public Game game;
        public int myTeam;
        public int turns;
        
        public int myMana = -1;
        public int opponentMana = -1;
        public int totalSnaffles = -1;
        
        private int[] usingAccio = new int[]{0,0};
        
        private int flipendoedSnaffleId = -1;
        private int flipendoDuration = 0;
        
          
        public Napoleon(Game game, int team){
            this.game = game;
            this.myTeam = team;
        }
        
        public String[] think(){
            String[] result = new String[2];
            
            myMana++;
            flipendoDuration--;
            usingAccio[0]--;
            usingAccio[1]--;
           
            
            Entity[] myPlayers = new Entity[2];
            myPlayers[0] = game.getAlliedSnatchers().get(0);
            myPlayers[1] = game.getAlliedSnatchers().get(1);
            
            Entity[] targets = choseTargets(myPlayers);
            
            for(int i=0; i<2; i++){
            	if(myPlayers[i].state == 0){
                	//TODO: check if doing the right computation on the player acceleration
                    result[i] = "MOVE" +" " + (targets[i].x + targets[i].vx *3 -myPlayers[i].vx/2) 
                    		+" "+ (targets[i].y + targets[i].vy*3 - myPlayers[i].vy/2) + " "+ "150";
                }else{
                	usingAccio[i] = 0; //TODO: why?
                    int x = game.getGoal(1-myTeam).x; 
                    int y = game.getGoal(1-myTeam).y;
                   
                    if(shouldThisPlayerPassToTheOther(myPlayers[i], myPlayers[1-i])){
                        System.err.println("Passing to the other player");
    	            	Point throwTarget = myPlayers[1-i].futurePosition();
    	            	
    	            	Entity heldSnaffle = findNearestSnuffle(myPlayers[i].position);
    	            	System.err.println("distance to held snaffle "+game.getDistance(myPlayers[i], heldSnaffle)
    	            		+"held snaffle "+heldSnaffle.id);
    	            	//throwTarget.translate(-heldSnaffle.vx, -heldSnaffle.vy);
    	            	//throwTarget.translate(-myPlayers[i].vx, -myPlayers[i].vy);
    	            	
                    	x = throwTarget.x;
                        y = throwTarget.y;
                    }
                    
                    //I'm subtracting my velocity from the target position, to make it really go where I want it to go.
                    x += myPlayers[i].vx * -1;
                    y += myPlayers[i].vy * -1;
                    
                    result[i] = "THROW "+x+" "+y+" 500";
                }
            }
            
            if(neddOneMoreGoalToWin()){
                System.err.println("Aggressive mode ON");
                result = useFlipendoShotAggressive(result, myPlayers, targets);
                result = useAccio(result, myPlayers, targets);
                result = usePetrificus(result, myPlayers);
            } else {
                result = useAccio(result, myPlayers, targets);
                result = useFlipendoShot(result, myPlayers, targets);
                result = usePetrificus(result, myPlayers);
            }
            
            if(turns == 5){
            	Entity bludger = game.getBludgers().get(0);
           		Entity closestSnatcher = findNearest(bludger.position, game.getAllSnatchers());
           		
           		if(closestSnatcher.entityType.equals("WIZARD")){
           			result[0] = "OBLIVIATE "+bludger.id;
           		} else {
           			result[0] = "OBLIVIATE "+game.getBludgers().get(1).id;
           		}
           		myMana -= obliviateCost;
            }
            
            return result;
             
            /*
            //Player 1
            if(myPlayers[0].state == 0){
            	result[0] = "MOVE" +" " + (targets[0].x + targets[0].vx *3 -myPlayers[0].vx/2) +" "+ (targets[0].y + targets[0].vy*3 - myPlayers[0].vy/2) + " "+ "150";
            }else{
            	usingAccio[0] = 0;
                int x = game.getGoal(1-myTeam).x, y = game.getGoal(1-myTeam).y;
               
                if(areAlliedPlayersWithinPassingDistance(myPlayers)){
	            	x = myPlayers[1].x;
                    y = myPlayers[1].y;
                }
                
                result[0] = "THROW "+x+" "+y+" 500";
            }
            //Player 2
            if(myPlayers[1].state == 0){
                result[1] = "MOVE" +" " + (targets[1].x + targets[1].vx*2) +" "+ (targets[1].y + targets[1].vy*2) + " "+ "150";
            }else{
            	usingAccio[1] = 0;
            	int x = game.getGoal(1-myTeam).x, y = game.getGoal(1-myTeam).y;
                
                if(areAlliedPlayersWithinPassingDistance(myPlayers)){
	            	x = myPlayers[0].x;
	                y = myPlayers[0].y;
                }

//                LinkedList<Entity> opponentsAndBludgers = game.getOpponentSnatchers();
//                game.getOpponentSnatchers().addAll(game.getBludgers());
//                
//                for(Entity e : opponentsAndBludgers){
//                    if(game.getDistance(myPlayers[0], e) < 1900){
//                        x = x - e.x;
//                        y = y - e.y;
//                    }
//                }

                result[1] = "THROW "+x+" "+y+" 500";
            }
            */
            
           
        }
        
        private boolean shouldThisPlayerPassToTheOther(Entity player0, Entity player1){
        	double[] distancesFromGoal = new double[2];
        	distancesFromGoal[0] = game.getDistance(player0.position, game.getGoal(1-myTeam)); 
        	distancesFromGoal[1] = game.getDistance(player1.position, game.getGoal(1-myTeam));
        	
        	distancesFromGoal[1] += 200; //I'm faking the other player being further away from the goal, to discourage steep vertical passing.
        	
        	boolean otherPlayerCloserToGoal = distancesFromGoal[1] < distancesFromGoal[0];
        	
        	double distanceBetweenPlayers = game.getDistance(player0, player1);
        	//At first let's try to always target the attacker instead of the goal.
        	if(otherPlayerCloserToGoal && distanceBetweenPlayers > 1300 && Math.abs(player0.x - player1.x) > 1300 
        			/*&& distanceBetweenPlayers < 3000*/){ 
        		return true;
        	}
        	
        	return false;
        }
        
        private boolean neddOneMoreGoalToWin(){
			return totalSnaffles /2 == game.getScore(myTeam);
		}

        private String[] useFlipendoShot(String[] result, Entity[] myPlayers, Entity[] targets){
        	//TODO: I think it's better to wait for flipendoCost + petrificusShot, this way I can beat a trigger happy flipendo player, 
        	//because I will nearly always block his shot with petrificus, and petrificus is more mana efficient than flipendo, thus giving me an advantage
        	if(myMana < flipendoCost){
				return result;
			}
        	
        	for(int i=0; i<2; i++){
                for(Entity snaffle : game.getSnuffles()){
                    if(
                    (isFlipendoLinedToGoal(myPlayers[i], snaffle, myTeam) 
                    && game.getDistance(snaffle, myPlayers[i]) > 1500	//No allied player too close to the snaffle  
                    && game.getDistanceFromGoal(snaffle, myTeam) > flipendoMinDistanceFromGoalThld //TODO: take out?
                    && Math.abs(snaffle.vy) < 500	//If snaffle has too much vy, I might miss 
                    && game.getDistance(snaffle, findNearest(snaffle.position, game.getAllEntitiesExcept(snaffle))) > 650 //If something really close to the snaffle, abort
                    && targets[1-i].id != snaffle.id
                    && game.getDistance(snaffle, myPlayers[i]) < flipendoMaxDistanceThld 
                    ) ) {
                        
                        if(isThereObstacleBetweenSnaffleAndGoal(snaffle, myPlayers[i])){
                            result[i] = "FLIPENDO "+snaffle.id;
                            flipendoedSnaffleId = snaffle.id;
                            flipendoDuration = 3;
                            myMana -= flipendoCost;
                            
                            return result;
                        }
                        
                    }
                }
            }
            return result;
        }

		private String[] useFlipendoShotAggressive(String[] result, Entity[] myPlayers, Entity[] targets){
			if(myMana < flipendoCost){
				return result;
			}
			
			for(int i=0; i<2; i++){
                for(Entity snaffle : game.getSnuffles()){
                     
                    if(
                    (isFlipendoLinedToGoal(myPlayers[i], snaffle, myTeam) 
                    && Math.abs(snaffle.vy) < 500
                    && game.getDistance(snaffle, myPlayers[i]) < flipendoMaxDistanceThld
                    ) ) {
                            
                    	if(isThereObstacleBetweenSnaffleAndGoal(snaffle, myPlayers[i])){
                            result[i] = "FLIPENDO "+snaffle.id;
                            flipendoedSnaffleId = snaffle.id;
                            flipendoDuration = 3;
                            myMana -= flipendoCost;
                            return result;
                        }
                        
                    }
                }
            }
            return result;
        }
		
		//The idea here, is that I want to give priority to accio and petrificus. In particular, I want to give a lot of importance
		//to the petrificus + accio combo, and never use flipendo unless I'm sure I can do that combo. Becuase there is no point in attacking
		//if I'm gonna get scored and loose
		private String[] useFlipendoShotDefensive(String[] result, Entity[] myPlayers, Entity[] targets){
			if(myMana < flipendoCost + accioCost + petrificusCost){
				return result;
			}
			
			for(int i=0; i<2; i++){
                for(Entity snaffle : game.getSnuffles()){
                     
                    if(
                    (isFlipendoLinedToGoal(myPlayers[i], snaffle, myTeam) 
                    && Math.abs(snaffle.vy) < 500
                    && game.getDistance(snaffle, myPlayers[i]) < flipendoMaxDistanceThld
                    ) ) {
                            
                    	if(isThereObstacleBetweenSnaffleAndGoal(snaffle, myPlayers[i])){
                            result[i] = "FLIPENDO "+snaffle.id;
                            flipendoedSnaffleId = snaffle.id;
                            flipendoDuration = 3;
                            myMana -= flipendoCost;
                            return result;
                        }
                        
                    }
                }
            }
            return result;
        }
		
		private boolean isThereObstacleBetweenSnaffleAndGoal(Entity snaffle, Entity playerFlipendoing){
			 System.err.println(playerFlipendoing.id+" Flipendo Lined up "+snaffle.id);
             
             Point highest = game.getGoal(1-myTeam);
             Point lowest = game.getGoal(1-myTeam);
             
             Point snafflePosPrediction = new Point((snaffle.x + snaffle.vx), (snaffle.y + snaffle.vy) + (int)(snaffle.vy*0.5));
             
             System.err.println("snaffle.vy " +snaffle.vy);
             System.err.println("snaffle.vy*4 " +snaffle.vy*4);
             
             highest.y = snafflePosPrediction.y - 400;
             lowest.y = snafflePosPrediction.y + 400;

             
             boolean obstacleFound = true;
             
             for(Entity e : game.entities){
                 if(isLined(snaffle, e, highest, lowest)){
                     System.err.println("playerFlipendoing "+playerFlipendoing.id
                     +"snaffle "+snaffle.id
                     +" obstruction "+e.id
                     +" obstacleFound"+obstacleFound);
                     obstacleFound = false;
                     break;
                 }
             }
            
             return obstacleFound;
		}

		private String[] usePetrificus(String[] result, Entity[] myPlayers){
            if(myMana < petrificusCost){
                return result;
            }
            
            for(Entity e : game.getSnuffles()){
                if(shouldPetrify(e, myTeam)){
                    double distanceToClosestAlly = game.getDistance(e, findNearest(e.position, game.getAlliedSnatchers()));
                    double distanceToClosestOpponent = (game.getDistance(e, findNearest(e.position, game.getOpponentSnatchers())));
                    
                    
                    System.err.println("distanceToClosestAlly "+distanceToClosestAlly+"\n distanceToClosestOpponent "+distanceToClosestOpponent);
                    
                    //Petrificus + Accio is an incredible defensive combo, although it is very expensive.
                    if(myMana >= accioCost){
                    	if(game.getDistance(myPlayers[0], e) < game.getDistance(myPlayers[1], e)){
                            result[1] = "PETRIFICUS "+e.id;
                            myMana -= petrificusCost;
                            /*
                            result[0] = "ACCIO "+e.id;
                            myMana -= accioCost;
                            */
                        } else {
                            result[0] = "PETRIFICUS "+e.id;
                            myMana -= petrificusCost;
                            /*
                            result[1] = "ACCIO "+e.id;
                            myMana -= accioCost;
                            */
                        }
                    } else
                    //If it's too close to me, it's useless. 
                    //If it's too close to an opponent, and I'm not closer than him, it's useless. He's gonna score later. Keep the mana hope Accio saves our ass
                    if( (distanceToClosestAlly > 750 &&  distanceToClosestOpponent > 2500)
                    || ((distanceToClosestOpponent >= distanceToClosestAlly) || myMana >= petrificusCost + accioCost) 
                    ){
                        if(game.getDistance(myPlayers[0], e) < game.getDistance(myPlayers[1], e)){
                            result[1] = "PETRIFICUS "+e.id;
                            myMana -= petrificusCost;
                        } else {
                            result[0] = "PETRIFICUS "+e.id;
                            myMana -= petrificusCost;
                        }
                    }
                }
            }
            return result;
        }
     	
        private boolean shouldPetrify(Entity e, int team){
        	
			boolean xOk = false;
			boolean yOk = (e.y + e.vy) < 3750 + 1900;
			yOk = yOk && (e.y + e.vy) > 3750 - 1900;
			if (team == 1) {
				xOk = e.x + e.vx > 16000;
			} else {
				xOk = e.x + e.vx < 0;
			}
			boolean tooLateToStopIt = xOk && yOk;

			xOk = false;
			//I want to change the 3s to 4s, and the 2000s in at least 1900s, but it works....
			yOk = (e.y + e.vy * 3) < 3750 + 2000;
			yOk = yOk && (e.y + e.vy * 3) > 3750 - 2000;
			if (team == 1) {
				xOk = e.x + e.vx * 4 > 16000;
			} else {
				xOk = e.x + e.vx * 4 < 0;
			}
            boolean iThinkItWillGoalInMax4Turns = xOk && yOk;
            
            return tooLateToStopIt==false && iThinkItWillGoalInMax4Turns;
        }
        
        private String[] useAccio(String[] result, Entity[] myPlayers, Entity[] targets){
        	//TODO: should I increase the mana needed for accio?
            if(myMana < accioCost){
                return result;
            }
            
            for(int i=0; i<2; i++){
            	Entity player = myPlayers[i];
                if(flipendoDuration > 0 && targets[i].id ==flipendoedSnaffleId){
                    continue;
                }
                if(usingAccio[i] > 0 ){
                    continue;
                }
                //Don't accio things in front of you, only backwards
                if(myTeam == 0 && player.x < targets[i].x ){
                    continue;                    
                }
                if(myTeam == 1 && player.x > targets[i].x ){
                    continue;
                }
                //If it's too close, then there is no point -- wrong comment
                //If it's too far, it won't do anything
                if(game.getDistance(player, targets[i]) > accioMinDistanceThld ){
                    continue;
                }
                
                if(findNearest(targets[i].position, game.getAllSnatchers()).id != player.id){
                    
                    System.err.println("Using Accio with "+player.id+" on "+targets[i].id);
                    System.err.println("Distance between me and snaffle is "+
                        game.getDistance(player, targets[i]));
                    System.err.println("Closest player to target is "+
                        findNearest(targets[i].position, game.getAllSnatchers()).id
                        +" and it is distant "+game.getDistance(player, targets[i]) );
                        
                    
                    
                    result[i] = "ACCIO "+targets[i].id;
                    myMana -= accioCost;
                    usingAccio[i] = 6;
                    return result;
                }
            }
            return result;
        }
        
        private Entity[] choseTargets(Entity[] myPlayers){
            
            List<Entity> snaffles = game.getSnuffles();
            
            Entity[] targets = new Entity[2];
            
            targets[0] = findNearest( myPlayers[0].position, snaffles );
            targets[1] = findNearest( myPlayers[1].position, snaffles ); 
            
            if(snaffles.size() == 1){
                double[] distanceFromSnaffle = new double[2];
                distanceFromSnaffle[0] = game.getDistance(myPlayers[0], snaffles.get(0));
                distanceFromSnaffle[1] = game.getDistance(myPlayers[1], snaffles.get(0));
                
                for(int i=0; i<2;i++){
                    if(distanceFromSnaffle[i] > distanceFromSnaffle[1-i] ){
                        double distanceFromGoal = game.getDistanceFromGoal(myPlayers[1-i].position, 1-myTeam);
                        
                        
                        
                        Point targetPoint = new Point(-1,-1);
                        targetPoint.x = game.getGoal(1-myTeam).x - myPlayers[1-i].position.x;
                        targetPoint.y = game.getGoal(1-myTeam).y - myPlayers[1-i].position.y;
                        
                        
                        targetPoint.x /= distanceFromGoal;
                        targetPoint.y /= distanceFromGoal;
                        
                        if(targetPoint.x==0) return targets;
                        
                        targetPoint.x = myPlayers[i-1].position.x + targetPoint.x * 2000;
                        targetPoint.y = myPlayers[i-1].position.y + targetPoint.y * 2000;
                        
                        Entity temp = new Entity(snaffles.get(0).id, "PuntoNelVuoto");
                        temp.updateInfo( targetPoint.x, targetPoint.y, 0, 0, 0);
                        targets[i] = temp;
                        System.err.println("Attacker Split Behaviour");
                    }
                }
                
                return targets;                
            }
            
            //Don't let the players go for the same snaffle if there are more on the field.
            //Might want to change this, if I implement passing to attacker. Probably not
            if(targets[0]==targets[1]){
                if(getDistance(myPlayers[0], targets[0])<=getDistance(myPlayers[1], targets[1])){
                    snaffles.remove(targets[0]);
                    targets[1] = findNearest( myPlayers[1].position, snaffles );
                } else {
                    snaffles.remove(targets[1]);
                    targets[0] = findNearest( myPlayers[0].position, snaffles );
                }
            }
            
            //Switching the snaffles between players sometimes leads to better results. i.e. they don't cross each other
            if(getDistance(myPlayers[0], targets[1])+getDistance(myPlayers[1], targets[0]) <
                getDistance(myPlayers[0], targets[0])+getDistance(myPlayers[1], targets[1])){
                    Entity temp = targets[0];
                    targets[0] = targets[1];
                    targets[1] = temp;
            }
            
                
            return targets;
          
        }
        
        public List<Entity> getSnafflesInAttackZone(List<Entity> snaffles){
        	List<Entity> snafflesInAttackZone = new LinkedList<Entity>();
            
            for(Entity s : snaffles){
                if(myTeam == 0){
                    if(s.x >= (16000/3)*2 ){
                        snafflesInAttackZone.add(s);
                    } 
                } else {
                    if(s.x <= (16000/3)*1 ){
                        snafflesInAttackZone.add(s);
                    } 
                }
            }
            
            return snafflesInAttackZone;
        }
        
        public List<Entity> getSnafflesInDefenseZone(List<Entity> snaffles){
        	List<Entity> snafflesInDefenseZone = new LinkedList<Entity>();
            
            for(Entity s : snaffles){
                if(myTeam == 0){
                    if(s.x < (16000/3)*2 ){
                        snafflesInDefenseZone.add(s);
                    }
                } else {
                    if(s.x > (16000/3)*1 ){
                        snafflesInDefenseZone.add(s);
                    }
                }
            }
            
            return snafflesInDefenseZone;
        }
            
        
        private Entity findNearest(Point start, List<Entity> entities){
            Entity nearest = null;
            double distance = Integer.MAX_VALUE;

            for(Entity e : entities){
                double currDistance = getDistance(start, e.position);
                if(currDistance < distance){
                    nearest = e;
                    distance = currDistance;
                }
            }
            return nearest;
        }
        
        public Entity findNearestSnuffle(Point position){
            return findNearest(position, game.getSnuffles());
        }
        
        public Entity findNearestSnatcher(Point position){
            return findNearest(position, game.getAllSnatchers());
        }
        
        public Entity findNearestOpponentSnatcher(Point position){
            return findNearest(position, game.getOpponentSnatchers());
        }
        
        public Entity findNearestAlliedSnatcher(Point position){
            return findNearest(position, game.getAlliedSnatchers());
        }

        public double getDistance(Entity e1, Entity e2){
            return game.getDistance(e1.position, e2.position);
        }
        
        public double getDistance(Point start, Point end){
           return game.getDistance(start, end);
        }
        
        public boolean isLined(Entity player, Entity snaffle, Point goal1, Point goal2){
            Point snafflePos = new Point((int) snaffle.x + snaffle.vx, (int) snaffle.y + snaffle.vy);
            
            double areaBig = getTriangleArea(player.position, goal1, goal2);
            double a1 = getTriangleArea(snafflePos, goal1, goal2) ;
            double a2 = getTriangleArea(player.position, snafflePos, goal2);
            double a3 = getTriangleArea(player.position, snafflePos, goal1);
            
            //TODO: check if tolleranct is too high/low
            return Math.abs( (areaBig - a1) - a2 - a3) <= 0.1;
        }
        
        public boolean isFlipendoLinedToGoal(Entity player, Entity snaffle, int team){
            Point goal1 = (team==0) ? new Point(16000, 3750-1400) : new Point(0, 3750-1400);
            Point goal2 = (team==0) ? new Point(16000, 3750+1400) : new Point(0, 3750+1400);
            
            return isLined(player, snaffle, goal1, goal2);
        }
        
        private double crossProduct(Point v1, Point v2, Point v3){
            Point a = new Point(v1.x + v2.x, v1.y + v2.y);
            Point b = new Point(v2.x + v3.x, v2.y + v3.y);
            Point c = new Point(v3.x + v1.x, v3.y + v1.y);
            
            double areaBig = (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x) ;
            return areaBig;
        }
        
        private double getTriangleArea(Point v1, Point v2, Point v3){
            double v1v2 = Math.sqrt( Math.pow(v1.x - v2.x, 2) + Math.pow(v1.y - v2.y, 2) );
            double v2v3 = Math.sqrt( Math.pow(v2.x - v3.x, 2) + Math.pow(v2.y - v3.y, 2) );
            double v3v1 = Math.sqrt( Math.pow(v3.x - v1.x, 2) + Math.pow(v3.y - v1.y, 2) );
            
            double perimeter = v1v2 + v2v3 + v3v1;
            double sp = perimeter/2;
        
            return Math.sqrt(sp * (sp - v1v2)*(sp - v2v3)*(sp - v3v1));
        }
    }
    
    
    public static class Entity{
        
        public int id; // entity identifier
        public String entityType ; // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
        public int vx; // velocity
        public int vy; // velocity
        public int state; // 1 if the wizard is holding a Snaffle, 0 otherwise
        public int x;
        public int y;
        public Point position;
        
        public Entity(Entity entity){
            this.id = entity.id;
            this.entityType = entity.entityType;
            this.vx = entity.vx;
            this.vy = entity.vy;
            this.state = entity.state;
            this.x = entity.x;
            this.y = entity.y;
            this.position = new Point(entity.position);
        }
        
        public Entity(int id, String entityType){
            this.id = id;
            this.entityType = entityType;
        }
        
        public void updateInfo( int x, int y, int vx, int vy, int state){
            this.vx = vx;
            this.vy = vy;
            this.state = state;
            this.x = x;
            this.y = y;
            this.position = new Point(x,y);
        }
        
        @Override
        public boolean equals(Object obj) {
        	if(obj instanceof Entity){
        		return equals((Entity)obj);
        	} else {
        		return false;
        	}
        }
        
        public boolean equals(Entity entity) {
        	return id == entity.id
        		&& entityType.equals(entity.entityType)
				&& vx == entity.vx
				&& vy == entity.vy
				&& state == entity.state
				&& x == entity.x
				&& y == entity.y;
	    }
        
        public Point futurePosition(){
        	Point newPoint = (Point)this.position.clone(); 
        	newPoint.translate(vx, vy);
        	return newPoint;
        }
    }
    
    public static class Game {
        
    	public final Point 	goal0_center = new Point(0, 3750), 
    						goal1_center =  new Point(16000, 3750);
    
    	
        public int[] score = new int[2];       
        
        public LinkedList<Entity> entities = new LinkedList<Entity>();
        public HashMap<Integer, Entity> entitiesDict = new HashMap<Integer, Entity>();
        
        public Entity updateEntity(int id, String entityType, int x, int y, int vx, int vy, int state){
            Entity myEntity = entitiesDict.get(id);
            if(myEntity == null){
                myEntity = initializeEntity(id, entityType, x, y, vx, vy, state);
            } else {
                myEntity.updateInfo(x, y, vx, vy, state);
            }
            return myEntity;
        }
        
        public void updateScore(List<Entity> entitiesInGame){
            for(Entity s : entitiesInGame){
                if(s.entityType.equals("SNAFFLE")==false){
                    System.err.println("ERROR: I found an entity that" + 
                    "disappeared from turn to turn, and is not a snuffle. It is a "+s.entityType);
                    continue;
                }
                if(s.x <= 2000){
                    score[1]++;
                } else {
                    score[0]++;
                }
            }
        }
        
        public int getScore(int team){
            return score[team];
        }
        
        private Entity initializeEntity( int id, String entityType, int x, int y, int vx, int vy, int state){
            Entity myEntity = new Entity(id, entityType);
            myEntity.updateInfo(x, y, vx, vy, state);
            entitiesDict.put(id, myEntity);
            entities.add(myEntity);
            return myEntity;
        }
        
        public void clearEntities(){
            entities = new LinkedList<Entity>();
            entitiesDict = new HashMap<Integer, Entity>();
        }
        
        public List<Entity> getSnuffles(){
            return entities.stream()
            		.filter(e -> e.entityType.equals("SNAFFLE"))
            		.collect(Collectors.toList());
        }
        
        
        public List<Entity> getAlliedSnatchers(){
            return entities.stream()
            		.filter(e -> e.entityType.equals("WIZARD"))
            		.collect(Collectors.toList());
        }
        
        public List<Entity> getOpponentSnatchers(){
            return entities.stream()
            		.filter(e -> e.entityType.equals("OPPONENT_WIZARD"))
            		.collect(Collectors.toList());
        }
        
        public List<Entity> getBludgers(){
        	return entities.stream()
            		.filter(e -> e.entityType.equals("BLUDGER"))
            		.collect(Collectors.toList());
        }
        
        public List<Entity> getAllSnatchers(){
        	return entities.stream()
            		.filter(e -> e.entityType.equals("OPPONENT_WIZARD") || e.entityType.equals("WIZARD"))
            		.collect(Collectors.toList()); 
        }
        
        public List<Entity> getAllEntitiesExcept(Entity arg){
        	return entities.stream()
            		.filter(e -> e.equals(arg)==false)
            		.collect(Collectors.toList()); 
        }
        
        public int getCountRemainingSnuffles(){
            return getSnuffles().size();
        }
        
        public double getDistance(Entity e1, Entity e2){
            return getDistance(e1.position, e2.position);
        }
        
        public double getDistance(Point start, Point end){
           return Math.sqrt(Math.pow(start.x-end.x,2)+Math.pow(start.y-end.y,2));
        }
        
        public double getDistanceFromGoal(Entity e, int team){
            return getDistanceFromGoal(e.position, team);
        }
        
        public double getDistanceFromGoal(Point start, int team){
            if(team==0){
            } else {
            }
        }
        
        public Point getGoal(int team){
            return (Point) ((team==0) ? goal0_center.clone() : goal1_center.clone());
                
        }
    }
}
