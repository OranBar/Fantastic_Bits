import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.awt.Point;

/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {
	
	public static SpaghettiDebugger spaghettiDbg = new SpaghettiDebugger();

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        
        Game myGame = new Game();
        Napoleon myNapoleon = new Napoleon(myGame, myTeamId);
        
        myNapoleon.turns = -2;
        
        // game loop
        while (true) {
        	myNapoleon.turns += 2;
        	System.err.println("Turn "+myNapoleon.turns);
            // Inputs
            ///////////////////
            myGame.score[0] = in.nextInt();
            in.nextInt();	//My Mana
            
            myGame.score[1] = in.nextInt();
            myNapoleon.opponentMana = in.nextInt();
            
            myGame.clearEntities();
            
            int entities = in.nextInt(); // number of entities still in game
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
                
                myGame.updateEntity(id, entityType, x, y, vx, vy, state);
            }
            
            ///////////////////
            String[] moves = myNapoleon.think();
            if(myNapoleon.gameStatesHistory.size() >= 2){
            	myNapoleon.gameStatesHistory.remove(0);
            }
            myNapoleon.gameStatesHistory.add((Game) myGame.clone());
            myNapoleon.prevTurnOpponentMana = myNapoleon.opponentMana;
        
            System.err.println(spaghettiDbg.getSummary());
            
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
        private int accioMinDistanceThld = 700;
        private int accioMaxDistanceThd = 5000;
        private int minAccioPower = 300;
        private int flipendoMinDistanceFromGoalThld = 2000;
        private int flipendoMaxDistanceThld = 5000;
        private int playersTooCloseThreshold = 2000;
        private int passingDistanceThld = 2000;
        
        public Game game;
        public int myTeam;
        public int opponentTeam;
        public int turns;
        
        public int myMana = -1;
        public int opponentMana = -1;
        public int prevTurnOpponentMana = -1;
        public int totalSnaffles = -1;
        
        private int[] usingAccio = new int[]{0,0};
        
        private int flipendoedSnaffleId = -1;
        private int flipendoDuration = 0;
        
        private List<Game> gameStatesHistory;
        
        private List<Entity> oneOfThoseMightHaveBeenFlipendoed = new LinkedList<Entity>();
        private int lastFlipendoDetectedTurn = -1;
        
        private int flipendoDetected = 0;
        
          
        public Napoleon(Game game, int team){
            this.game = game;
            this.myTeam = team;
            this.opponentTeam = 1-myTeam;
            gameStatesHistory = new LinkedList<Game>();
        }
        
        public String[] think(){
            String[] result = new String[2];
            
            myMana++;
            flipendoDuration--;
            flipendoDetected--;
            usingAccio[0]--;
            usingAccio[1]--;
           
            
            Entity[] myPlayers = new Entity[2];
            myPlayers[0] = game.getAlliedSnatchers().get(0);
            myPlayers[1] = game.getAlliedSnatchers().get(1);
            
            Entity[] targets = choseTargets(myPlayers);
            
            detectOpponentSpellUse();
            
            for(int i=0; i<2; i++){
            	if(myPlayers[i].state == 0){
                	//TODO: check if doing the right computation on the player acceleration
            		result[i] = "MOVE" +" " + (targets[i].x + targets[i].vx *3 - myPlayers[i].vx) 
                    		+" "+ (targets[i].y + targets[i].vy*3 - myPlayers[i].vy) + " "+ "150";
                }else{
                	usingAccio[i] = 0; //TODO: why?
                    int x = game.getGoal(opponentTeam).x; 
                    int y = game.getGoal(opponentTeam).y;
                    
                    //Snaffles have radius of 150. I'm going to use 300 to be sure I'm not hitting the pole.
                    //I'm also checking if I'm too close to the post. In that case, just throw it to the middle of the goal I guess.
                    
                    /*
                    if(myPlayers[i].y -300 < game.getGoalTop(opponentTeam).y){
                    	//If I'm too close to the post, I'd rather shoot to the center
                    	if(game.getDistance(myPlayers[i].position, game.getGoalTop(opponentTeam)) > 2000){
                    		y = game.getGoalTop(opponentTeam).y + 300;
                    	} else {
                    		y = game.getGoal(opponentTeam).y;
                    	}
                    } else if(myPlayers[i].y +300 > game.getGoalBottom(opponentTeam).y){ 
                    	//If I'm too close to the post, I'd rather shoot to the center
                    	if(game.getDistance(myPlayers[i].position, game.getGoalBottom(opponentTeam)) > 2000){
                    		y = game.getGoalBottom(opponentTeam).y - 300;
                    	} else {
                    		y = game.getGoal(opponentTeam).y;
                    	}
                    } else {
                    	y = myPlayers[i].y ;
                	}
                    */
                    
                    if(shouldThisPlayerPassToTheOther(myPlayers[i], myPlayers[1-i])){
                        Point throwTarget = myPlayers[1-i].futurePosition();
    	            	
    	            	Entity heldSnaffle = findNearestSnuffle(myPlayers[i].position);
    	            	
    	            	x = throwTarget.x;
                        y = throwTarget.y;
                    }
                    
                    //I'm subtracting my velocity from the target position, to make it really go where I want it to go.
                    //System.err.println("my velocity x "+myPlayers[i].vx+"- y "+myPlayers[i].vy);
                    //System.err.println("snaffle velocity x "+targets[i].vx+"- y "+targets[i].vy);
                    
                    //I want my desired velocity to be in the direction of this target.
                    VectorInt desiredVelocity = new VectorInt(x,y).minus(new VectorInt(myPlayers[i].futurePosition()));
                    VectorInt offset = desiredVelocity.minus(new VectorInt(myPlayers[i].vx * 0.75, myPlayers[i].vy * 0.75));
                    
                    
                    x = myPlayers[i].x + (int)offset.x;
                    y = myPlayers[i].y + (int)offset.y;
                    
                    result[i] = "THROW "+x+" "+y+" 500";
                }
            }
            
            if(neddOneMoreGoalToWin()){
            	result = useFlipendoShotAggressive(result, myPlayers, targets);
                result = useAccio(result, myPlayers, targets);
                result = usePetrificus(result, myPlayers);
            } else if(needOneMoreGoalToLoose()) {
            	result = usePetrificus(result, myPlayers);
                result = useFlipendoShot(result, myPlayers, targets);
                result = useAccioDefensive(result, myPlayers, targets);
            } else {
            	result = usePetrificus(result, myPlayers);
            	result = useAccio(result, myPlayers, targets);
                result = useFlipendoShot(result, myPlayers, targets);
            }
            
            /*
            if(turns <= 6 && turns >= 2){
            	Entity incomingBludger = findNearest(game.getGoal(myTeam), game.getBludgers());
            	Entity bludgersTargetPlayer = findNearestFuture(incomingBludger, game.getAlliedSnatchers());
                 
            	//TODO: Use orthogonal vector to flee
            	
            	
                for(int i=0; i<2; i++){
                	 Entity player = myPlayers[i];
                	 if(player.id == bludgersTargetPlayer.id){
                		 
                		 int x = targets[i].x;
                         int y = targets[i].y;
                         
                         Vector bludgerToPlayer = new Vector(myPlayers[i].position).minus(new Vector(incomingBludger.position));
                         Vector playerToTarget = new Vector(x,y).minus(new Vector(myPlayers[i].position));
                         
                         bludgerToPlayer = bludgerToPlayer.multiply(1/bludgerToPlayer.length());
                         playerToTarget = playerToTarget.multiply(1/playerToTarget.length());
                         
                         Vector desiredVelocity = playerToTarget.minus(new Vector(myPlayers[i].futurePosition()));
                         desiredVelocity = desiredVelocity.minus(bludgerToPlayer);
                         
                         x += desiredVelocity.x;
                         y += desiredVelocity.y;
                         
                		 
                        
                		 result[i] = "MOVE "+x+" "+y+" 150";
                	 }
                 }
            	
            } 
            */

            
            //System.err.println("There are "+oneOfThoseMightHaveBeenFlipendoed.size()+" flipendo maybes");
            //System.err.println("I have "+myMana+" mana, while petrificus costs "+petrificusCost);
            
            //I think I know which snaffle he flipendoed. Let's petrify it.
            if(oneOfThoseMightHaveBeenFlipendoed.size() == 1 
            && myMana > petrificusCost){
            	Entity flipendoedSnaffle = oneOfThoseMightHaveBeenFlipendoed.get(0);
            	if(game.getDistance(myPlayers[0], flipendoedSnaffle) < game.getDistance(myPlayers[1], flipendoedSnaffle)){
                    result[1] = "PETRIFICUS "+flipendoedSnaffle.id;
                    myMana -= petrificusCost;
                    
                } else {
                    result[0] = "PETRIFICUS "+flipendoedSnaffle.id;
                    myMana -= petrificusCost;
                }
            }
            
            return result;
             
        }
        
        private void detectOpponentSpellUse(){
        	oneOfThoseMightHaveBeenFlipendoed.clear();
        	if(gameStatesHistory.size()<2){
        		return;
        	}
        	
        	//I'm subtracting the 1 mana point that the player earned at the beginning of his new turn
        	double manaDifference = Math.abs((opponentMana-1) - prevTurnOpponentMana);
        	if(manaDifference == 0){
        		if(turns - lastFlipendoDetectedTurn >= 3){
        			oneOfThoseMightHaveBeenFlipendoed.clear();
        		}
        		return;
        	}
        	
        	Entity candidateCaster = null;
        	double temp = 1000;
        	
        	for(Entity opponent : game.getOpponentSnatchers()){
        		int opponentId = opponent.id;
        		double positionDifferenceFromExpectedIfNotMoved = game.getDistance(game.entitiesDict.get(opponentId).position, gameStatesHistory.get(1).entitiesDict.get(opponentId).futurePosition());
        		
        		if(positionDifferenceFromExpectedIfNotMoved < temp){
        			temp = positionDifferenceFromExpectedIfNotMoved;
        			candidateCaster = opponent;
        		}
        	}
        	
        	System.err.println("Mana Difference "+manaDifference);
        	
        	if(manaDifference == petrificusCost){
    			System.err.println("Petrificus Used by "+candidateCaster.id);
    		}
    		if(manaDifference == accioCost){
    			System.err.println("Accio Used by "+candidateCaster.id);
    		}
    		if(manaDifference == flipendoCost){
    			flipendoDetected = 4; //Until this variable is 0, I can't flipendo. This will give me time to petrify his flipendo, if it was a dangerous one. 
    			
    			System.err.println("Flipendo Used by "+candidateCaster.id);
    			lastFlipendoDetectedTurn = turns;
    			
    			List<Entity> possibleFlipendoTargets = new LinkedList<Entity>();
    			double opponentDistanceFromGoal = game.getDistanceFromGoal(candidateCaster, myTeam); 
    			
    			//If there is only one snaffle on the field, he obviously flipendoed that one. Petrify it!! NOW!
    			if(game.getSnaffles().size() == 1){
    				possibleFlipendoTargets.add(game.getSnaffles().get(0));
    				oneOfThoseMightHaveBeenFlipendoed = possibleFlipendoTargets;
    				return;
    			}
    			
    			for(Entity snaffle : game.getSnaffles()){
    				if(snaffle.state == 0){
    					
    					if(game.getDistanceFromGoal(snaffle, myTeam) < opponentDistanceFromGoal){
    						if(isFlipendoLinedToGoal(candidateCaster, snaffle, opponentTeam)){
    							possibleFlipendoTargets.add(snaffle);
    						} else {
    							System.err.println("I'm thinking "+snaffle.id+" but it isn't lined to the goal, according to my math");
    						}
    					}
    				}
    			}
    			
    			if(possibleFlipendoTargets.size() == 0){
    				//He is probably looking for a bounce shot... For now I'll ignore this case, since I have more time to stop it,
    				//so hopefully, the petrificus code will do that.
    			}
    			if(possibleFlipendoTargets.size() > 1){
    				//This is a really far ball. Let's assume he didn't hit that one
    				List<Entity> snafflesToRemove = new LinkedList<Entity>();
    				
    				for(Entity snaffle : possibleFlipendoTargets){
						if(game.getDistance(snaffle, candidateCaster) > 8000){
							snafflesToRemove.add(snaffle);
						}
    				}
    				possibleFlipendoTargets.removeAll(snafflesToRemove);
    			}
    				
    			possibleFlipendoTargets.stream().forEach(e -> System.err.println("Maybe he is flipendoing "+e.id));
    			
    			if(possibleFlipendoTargets.size() != 0){
    				oneOfThoseMightHaveBeenFlipendoed = possibleFlipendoTargets;
    			}
    		}
        }
        
        private boolean shouldThisPlayerPassToTheOther(Entity player0, Entity player1){
        	double[] distancesFromGoal = new double[2];
        	distancesFromGoal[0] = game.getDistance(player0.position, game.getGoal(opponentTeam)); 
        	distancesFromGoal[1] = game.getDistance(player1.position, game.getGoal(opponentTeam));
        	
        	distancesFromGoal[1] += 200; //I'm faking the other player being further away from the goal, to discourage steep vertical passing.
        	
        	boolean otherPlayerCloserToGoal = distancesFromGoal[1] < distancesFromGoal[0];
        	
        	double distanceBetweenPlayers = game.getDistance(player0, player1);
        	//At first let's try to always target the attacker instead of the goal.
        	if(otherPlayerCloserToGoal 
        	&& distanceBetweenPlayers > 1300 
        	&& Math.abs(player0.x - player1.x) > 1300	//He has to be forward
        	&& Math.abs(player0.y - player1.y) < 4000	//Not too far on the y axis
        	/*&& distanceBetweenPlayers < 3000*/){ 
        		return true;
        	}
        	
        	return false;
        }
        
        private boolean neddOneMoreGoalToWin(){
			return totalSnaffles /2 == game.getScore(myTeam);
		}
        
        private boolean needOneMoreGoalToLoose(){
        	return totalSnaffles /2 == game.getScore(opponentTeam);
        }

        private String[] useFlipendoShot(String[] result, Entity[] myPlayers, Entity[] targets){
        	//TODO: I think it's better to wait for flipendoCost + petrificusShot, this way I can beat a trigger happy flipendo player, 
        	//because I will nearly always block his shot with petrificus, and petrificus is more mana efficient than flipendo, thus giving me an advantage
        	if(myMana < flipendoCost){
				return result;
			}
        	/*
        	if( (petrificusCost - (myMana - flipendoCost) > flipendoCost - opponentMana )
        	|| flipendoDetected > 0){
        		return result;
            }
        	*/
        	
        	for(int i=0; i<2; i++){
        		Entity player = myPlayers[i];
        		
        		List<Entity> snafflesSortedByDistance = new LinkedList<Entity>(game.getSnaffles());
        		
        		snafflesSortedByDistance.sort( (s1, s2) ->  Double.compare(game.getDistance(s1, player), game.getDistance(s2, player)) );
        		
                for(Entity snaffle : snafflesSortedByDistance){
                    if(
                    (isFlipendoLinedToGoal(player, snaffle, myTeam) 
                    //No allied player too close to the snaffle or closest player to snaffle is not me
                    && (game.getDistance(snaffle, player) > 1500 || findNearest(snaffle.position, game.getAllSnatchers()).entityType == "OPPONENT_WIZARD")  	  
                    && game.getDistanceFromGoal(snaffle, opponentTeam) > flipendoMinDistanceFromGoalThld //TODO: take out?
                    && Math.abs(snaffle.vy) < 500	//If snaffle has too much vy, I might miss 
                    && game.getDistance(snaffle, findNearest(snaffle.position, game.getAllEntitiesExcept(snaffle))) > 650 //If something really close to the snaffle, abort
                    && targets[1-i].id != snaffle.id
                    && game.getDistance(snaffle, player) < flipendoMaxDistanceThld 
                    ) ) {
                        
                        if(isThereObstacleBetweenSnaffleAndGoal(snaffle, player)==false){
                        	result[i] = "FLIPENDO "+snaffle.id;
                            flipendoedSnaffleId = snaffle.id;
                            flipendoDuration = 3;
                            myMana -= flipendoCost;
                            
                            return result;
                        }
                        
                    }
                    
                    if(bounceGoalOpportunity(player, snaffle, myTeam) ){
                    	result[i] = "FLIPENDO "+snaffle.id+" Bounce Shot!!!" ;
                        flipendoedSnaffleId = snaffle.id;
                        flipendoDuration = 3;
                        myMana -= flipendoCost;
                        
                        return result;
                    }
                }
             }
            return result;
        }

		private String[] useFlipendoShotAggressive(String[] result, Entity[] myPlayers, Entity[] targets){
			if(myMana < flipendoCost){
				return result;
			}
			
			// A lot of trickery going on here. The target is to consider the closest snaffle to the opponent's goal first, and consider flipendoing
			// with the closest player to that snaffle first.
			List<Entity> snaffles = game.getSnaffles();
			snaffles.sort( (s1, s2) -> ((Double)(game.getDistanceFromGoal(s1, opponentTeam))).compareTo((game.getDistanceFromGoal(s2, opponentTeam))));
			
			for(Entity snaffle : snaffles){
				List<Entity> players = new LinkedList<Entity>();
				
				players.add(myPlayers[0]);
				players.add(myPlayers[1]);
				players.sort( (p1, p2) -> ((Double)game.getDistance(p1, snaffle)).compareTo(game.getDistance(p2, snaffle)) ) ;
				boolean playersSwapped = false;
				
				if( game.getDistance(myPlayers[1], snaffle) < game.getDistance(myPlayers[0], snaffle) ){
					Entity temp = myPlayers[0];
					myPlayers[0] = myPlayers[1];
					myPlayers[1] = temp;
					playersSwapped = true;
				}
				
				
				for(int i=0; i<2; i++){
					if(
                    (isFlipendoLinedToGoal(players.get(i), snaffle, myTeam) 
            		&& game.getDistance(snaffle, players.get(i)) > 300 //I don't want the snaffle to be in my radius
            		&& game.getDistance(snaffle, players.get(i)) < 6000
            		&& game.getDistanceFromGoal(snaffle, opponentTeam) > 1500 //TODO: take out?
            		&& Math.abs(snaffle.vy) < 500
                    && game.getDistance(snaffle, players.get(i)) < flipendoMaxDistanceThld
                    && game.getDistance(snaffle, findNearest(snaffle.position, game.getAllEntitiesExcept(snaffle))) > 650 //If something really close to the snaffle, abort
                    ) ) {
                            
                    	if(isThereObstacleBetweenSnaffleAndGoal(snaffle, players.get(i))==false){
                    		if(playersSwapped){
                    			result[1-i] = "FLIPENDO "+snaffle.id+" SupaShot";
                    		} else {
                    			result[i] = "FLIPENDO "+snaffle.id+" SupaShot";
                    		}
                            System.err.println("SupaShot");
                            flipendoedSnaffleId = snaffle.id;
                            flipendoDuration = 3;
                            myMana -= flipendoCost;
                            return result;
                        }
                    }
                    
					//Should I be trying bounce goals? 
					/*
					if(bounceGoalOpportunity(players.get(i), snaffle, myTeam) ){
                    	result[i] = "FLIPENDO "+snaffle.id+" Bounce Shot!!!" ;
                    	System.err.println("SupaBounce");
                        flipendoedSnaffleId = snaffle.id;
                        flipendoDuration = 3;
                        myMana -= flipendoCost;
                        
                        return result;
                    }
                    */
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
                for(Entity snaffle : game.getSnaffles()){
                     
                    if(
                    (isFlipendoLinedToGoal(myPlayers[i], snaffle, myTeam) 
                    && Math.abs(snaffle.vy) < 500
                    && game.getDistance(snaffle, myPlayers[i]) < flipendoMaxDistanceThld
                    ) ) {
                            
                    	if(isThereObstacleBetweenSnaffleAndGoal(snaffle, myPlayers[i])==false){
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
			spaghettiDbg.trackLineHits("Flipendo: Checking for Obstacles", turns+2);
			Line playerToGoal = new Line(new VectorInt(playerFlipendoing.position), new VectorInt(snaffle.futurePosition()));
			double y = playerToGoal.GetY(game.getGoal(opponentTeam).x);
			VectorD goalHit = new VectorD(game.getGoal(opponentTeam).x, y);
			 
			 /* point 1 = snaffle.position; 
				point 2 = goalHit - ((snaffle -goalHit).ortho().normalize() * checkRadius)
				point 3 = goalHit + ((snaffle -goalHit).ortho() .normalize() * checkRadius)
			  */
			 
			float checkRadius = 600;
			 
			 VectorD snafflePos = new VectorD(snaffle.x, snaffle.y);
			 VectorD trianglePoint1 = goalHit.minus((snafflePos.minus(goalHit).ortho().norm().multiply(checkRadius)));
			 VectorD trianglePoint2 = goalHit.add((snafflePos.minus(goalHit).ortho().norm().multiply(checkRadius)));
			 
			 boolean obstacleFound = false;
			 
			 for(Entity e : game.getAllEntitiesExcept(snaffle)){
				 Point p1 = new Point((int)trianglePoint1.x, (int)trianglePoint1.y);
				 Point p2 = new Point((int)trianglePoint2.x, (int)trianglePoint2.y);
			     if(isLined(snaffle, e, p1, p2)){
			    	 System.err.println("Entity "+e.id+" obstructed");
			    	 spaghettiDbg.trackLineHits("Flipendo: Obstacle Found", turns+2);
			         obstacleFound = true;
			         break;
			     }
			 }
			 
			 
			 Point highest = game.getGoal(opponentTeam);
			 Point lowest = game.getGoal(opponentTeam);
			 
			 Point snafflePosPrediction = new Point((snaffle.x + snaffle.vx), (snaffle.y + snaffle.vy) + (int)(snaffle.vy*0.5));
			 
			 highest.y = snafflePosPrediction.y - 400;
			 lowest.y = snafflePosPrediction.y + 400;
			 
			 for(Entity e : game.entities){
			     if(isLined(snaffle, e, highest, lowest)){
			         spaghettiDbg.trackLineHits("Flipendo OLD: Obstacle Found", turns+2);
			         break;
			     }
			 }
			
			 return obstacleFound;
			 
		}

		private String[] usePetrificus(String[] result, Entity[] myPlayers){
            if(myMana < petrificusCost){
                return result;
            }
            /*
            System.err.println("Now turn "+turns+" Flipendo was detected on turn "+lastFlipendoDetectedTurn
            		+" There are "+oneOfThoseMightHaveBeenFlipendoed.size()+" snaffles in my maybe list");
            //Petrificus a flipendoed snaffle using the detection and guessing tecnique.
            
            for(Entity s : oneOfThoseMightHaveBeenFlipendoed){
            	Entity snaffle = game.entitiesDict.get(s.id);
            	boolean tooLateToStopIt = game.getDistanceFromGoal(snaffle, myTeam) < 2700;
    			
            	//System.err.println("Snaffle distance from goal is "+game.getDistanceFromGoal(snaffle, myTeam));
            	
    			if(tooLateToStopIt){
    				System.err.println("Can't stop "+snaffle.id+" now, it's too close");
    				break;
    			}
    			
            	//This snaffle is moving towards my goal.
    			
            	System.err.println("Checking if I should petrify "+snaffle.id+", since it might have been flipendoed");
            	System.err.println("Its x velocity last turn was "+game.entitiesDict.get(snaffle.id).vx);
            	System.err.println("Its x velocity now is "+snaffle.vx);
            	System.err.println("Its x position last turn was "+gameStatesHistory.get(1).entitiesDict.get(snaffle.id).x);
            	System.err.println("Its x position now is "+snaffle.x);
            	
            	if((opponentTeam == 0 && snaffle.x > gameStatesHistory.get(1).entitiesDict.get(snaffle.id).x)
            	|| (opponentTeam == 1 && snaffle.x < gameStatesHistory.get(1).entitiesDict.get(snaffle.id).x)){
            		if(game.getDistance(myPlayers[0], snaffle) < game.getDistance(myPlayers[1], snaffle)){
                        result[1] = "PETRIFICUS "+snaffle.id+" Guess";
                        myMana -= petrificusCost;
                        return result;
                    } else {
                        result[0] = "PETRIFICUS "+snaffle.id+" Guess";
                        myMana -= petrificusCost;
                        return result;
                    }
            	}
            }
            */
            
            for(Entity e : game.getSnaffles()){
                if(shouldPetrify(e, myTeam)){
                    double distanceToClosestAlly = game.getDistance(e, findNearest(e.position, game.getAlliedSnatchers()));
                    double distanceToClosestOpponent = (game.getDistance(e, findNearest(e.position, game.getOpponentSnatchers())));
                    
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
        	/*
        	int turnWhenBallWillGoIn = 0;
        	Point predictedSnafflePosition = e.position;
        	while(turnWhenBallWillGoIn <=4){
        		predictedSnafflePosition = e.futurePosition();
        	}
        	while(turnWhenBallWillGoIn <=4){
        		predictedSnafflePosition = e.futurePosition();
        	}
        	
        	
        	if(turnWhenBallWillGoIn == 1|| turnWhenBallWillGoIn > 4){
        		return false;
        	} else {
        		return true;
        	}
        	*/
        	
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
                if(flipendoDuration > 0 && targets[i].id == flipendoedSnaffleId){
                    continue;
                }
            	//I have a snaffle. Let's throw it first, then accio later.
            	if(player.state == 1){
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
                if(game.getDistance(player, targets[i]) < accioMinDistanceThld ){
                    //continue; 
                }
                System.err.println("Accio power is "+getAccioPower(myPlayers[i], targets[i]));
                //If accio power is too weak, don't do it.
                if(getAccioPower(myPlayers[i], targets[i]) < minAccioPower){
                	continue; 
                }
                
                if(findNearest(targets[i].position, game.getAllSnatchers()).id != player.id){
                    result[i] = "ACCIO "+targets[i].id;
                    myMana -= accioCost;
                    usingAccio[i] = 6;
                    return result;
                }
            }
            return result;
        }
        
        private String[] useAccioDefensive(String[] result, Entity[] myPlayers, Entity[] targets){
        	return useAccio(result, myPlayers, targets);
        }
        
        private double getAccioPower(Entity player, Entity snaffle){
        	//MIN( 3000 / ( Dist / 1000 )2, 1000 )
        	double distance = game.getDistance(player, snaffle);
        	return Math.min( 3000/ Math.pow((distance)/1000,2), 1000);
        }
        
        private Entity[] choseTargets(Entity[] myPlayers){
            
            List<Entity> snaffles = game.getSnaffles();
            
            //This takes out all the snaffles from the offensive half of the field,
            //to encourage defense when the situation requires the team to defend!
            snaffles = filterSnaffles(snaffles);
            
            Entity[] targets = new Entity[2];
            
            
            targets[0] = findNearestFuture( myPlayers[0], snaffles );
            targets[1] = findNearestFuture( myPlayers[1], snaffles ); 
            
            if(snaffles.size() == 1){
                double[] distanceFromSnaffle = new double[2];
                distanceFromSnaffle[0] = game.getDistance(myPlayers[0], snaffles.get(0));
                distanceFromSnaffle[1] = game.getDistance(myPlayers[1], snaffles.get(0));
                
                for(int i=0; i<2;i++){
                    if(distanceFromSnaffle[i] > distanceFromSnaffle[1-i] ){
                    	//Player i should be the attacker, since it's further away from the snaffle.
                    	VectorInt defenderPos = new VectorInt(myPlayers[1-i].position);
                    	VectorInt goalPos  = new VectorInt(game.getGoal(opponentTeam));
                    	
                    	VectorInt defenderToGoal = goalPos.minus(defenderPos);
                    	defenderToGoal = defenderToGoal.norm();
                    	
                    	
                    	VectorInt targetPosition = new VectorInt(myPlayers[1-i].position).add( (defenderToGoal.multiply(2000) ));
                    	
                    	Entity newTargetPosition = new Entity(snaffles.get(0).id, "PuntoNelVuoto");
                        newTargetPosition.updateInfo( (int)targetPosition.x, (int)targetPosition.y, 0, 0, 0);
                        targets[i] = newTargetPosition;
                        System.err.println("Attacker Split Behaviour");
                    	/*
                    	
                        double distanceFromGoal = game.getDistanceFromGoal(myPlayers[1-i].position, opponentTeam);
                        
                        Point targetPoint = new Point(-1,-1);
                        targetPoint.x = game.getGoal(opponentTeam).x - myPlayers[1-i].position.x;
                        targetPoint.y = game.getGoal(opponentTeam).y - myPlayers[1-i].position.y;
                        
                        
                        targetPoint.x /= distanceFromGoal;
                        targetPoint.y /= distanceFromGoal;
                        
                        if(targetPoint.x==0) return targets;
                        
                        targetPoint.x = myPlayers[1-i].position.x + targetPoint.x * 2000;
                        targetPoint.y = myPlayers[1-i].position.y + targetPoint.y * 2000;
                        
                        Entity newTargetPosition = new Entity(snaffles.get(0).id, "PuntoNelVuoto");
                        newTargetPosition.updateInfo( targetPoint.x, targetPoint.y, 0, 0, 0);
                        targets[i] = newTargetPosition;
                        System.err.println("Attacker Split Behaviour");
                        */
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
            
            //Switching the snaffles between players leads to better results. i.e. they don't cross each other
            if(getDistance(myPlayers[0], targets[1])+getDistance(myPlayers[1], targets[0]) <
                getDistance(myPlayers[0], targets[0])+getDistance(myPlayers[1], targets[1])){
                    Entity temp = targets[0];
                    targets[0] = targets[1];
                    targets[1] = temp;
            }
            
            while(snaffles.size() >= 3 && game.getDistance(targets[0], targets[1]) < playersTooCloseThreshold){
        		int playerCloserToItsTarget = -1;
            	if(getDistance(myPlayers[0], targets[0]) <= getDistance(myPlayers[1], targets[1])){
            		playerCloserToItsTarget = 0;
            	} else {
            	    playerCloserToItsTarget = 1;
            	}
            	Entity removedSnaffle = targets[1-playerCloserToItsTarget];
            	snaffles.remove(targets[playerCloserToItsTarget]);
            	snaffles.remove(targets[1-playerCloserToItsTarget]);
            	targets[1-playerCloserToItsTarget] = findNearestFuture( myPlayers[1-playerCloserToItsTarget], snaffles ); 
            	
            	System.err.println((1-playerCloserToItsTarget)+" wanted to go for "+removedSnaffle.id
            			+". The distance was "+getDistance(myPlayers[1-playerCloserToItsTarget], removedSnaffle)
            			+" It will now go for "+targets[1-playerCloserToItsTarget].id);
        		
            }
            return targets;
        }
        
        private boolean bounceGoalOpportunity(Entity player, Entity snaffle, int team){
        	if(myTeam==0 && player.futurePosition().x > snaffle.x ){
        		return false;
        	}
        	if(myTeam==1 && player.futurePosition().x < snaffle.x ){
        		return false;
        	}
        	VectorInt snaffleVelocity = new VectorInt(snaffle.vx, snaffle.vy);
        	//For the love of god don't hit snaffles that are moving fast
        	if(snaffleVelocity.length() > 400){
        		return false;
        	}
        	//The bounce needs a lot of power to it. You can't do it from too far away
        	if(game.getDistanceFromGoal(player, opponentTeam) > 8000){
        		//Also, the snaffle has to be close enough to you, or it won't travel far...
        		//TODO: add this line back in. It's important.
        		//if(game.getDistance(player, snaffle) > 3700){
        			return false;
        		//}
        	}
        	// We're too close to the goal to be thinking about bounces
        	if(myTeam == 0 && player.x > 13600 || team == 1 && player.x < 16000-13600){
        		return false;
        	}
        	//If I'm extremely close to the snaffle, don't do anything
        	if(game.getDistance(player, snaffle) < 500){
        		return false;
        	}
        	//If I'm looking at a snaffle held by an other player. he's gonna throw it and mess my bounce.
        	if(snaffle.state == 1){
        		return false;
        	}
        	
        	
        	VectorInt playerPos = new VectorInt(player.futurePosition());
        	VectorInt snafflePos = new VectorInt(snaffle.futurePosition());

        	
        	Line line = new Line(playerPos, snafflePos);
        	
        	VectorInt topWallHitPoint = new VectorInt(line.GetX(0f), 0f);
        	VectorInt bottomWallHitPoint = new VectorInt(line.GetX(7000f), 7000f);

        	if(topWallHitPoint.x < 0 || topWallHitPoint.x > 16000){
        		return false;
        	}
        	if(bottomWallHitPoint.x < 0 || bottomWallHitPoint.x > 16000){
        		return false;
        	}
        	
        	Line bounceTrajectory = null;
        	
        	if(myTeam==0){
        		if(topWallHitPoint.x > player.x){
        			bounceTrajectory = new Line(topWallHitPoint, line.getSlope()*-1);
        		} else {
        			bounceTrajectory = new Line(bottomWallHitPoint, line.getSlope()*-1);
            	}
        	} else {
        		if(topWallHitPoint.x < player.x){
        			bounceTrajectory = new Line(bottomWallHitPoint, line.getSlope()*-1);
        		} else {
        			bounceTrajectory = new Line(topWallHitPoint, line.getSlope()*-1);
            	}
        	}
        	
        	Point opponentGoal = game.getGoal(opponentTeam);
        	
        	//Snaffle is 150 of radius. I'm going to subtract/add 400 to the pole, to make sure I'm not hitting it. Vogliamo Solo rete.
        	if(bounceTrajectory.GetY(opponentGoal.x) > (game.goal0_top.y + 1000) && bounceTrajectory.GetY(opponentGoal.x) < (game.goal0_bottom.y - 1000) ){
        		return true;
        	}
        	return false;
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
                    if(s.x < (16000/2)*1 ){
                        snafflesInDefenseZone.add(s);
                    }
                } else {
                    if(s.x > (16000/2)*1 ){
                        snafflesInDefenseZone.add(s);
                    }
                }
            }
            
            return snafflesInDefenseZone;
        }
        
        private List<Entity> filterSnaffles(List<Entity> snaffles){
                        List<Entity> snufflesInDefenseZone = getSnafflesInDefenseZone(snaffles);
            
            if(snufflesInDefenseZone.size() + game.getScore(opponentTeam) >= (totalSnaffles/2) +1 ){
            	System.err.println("Defense");
                return snufflesInDefenseZone;
            } else {
            	return snaffles;
            }
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
        
        private Entity findNearestFuture(Entity me, List<Entity> entities){
            Entity nearest = null;
            double distance = Integer.MAX_VALUE;

            for(Entity e : entities){
                double currDistance = game.getDistanceFuture(me, e);
                if(currDistance < distance){
                    nearest = e;
                    distance = currDistance;
                }
            }
            return nearest;
        }
        
        public Entity findNearestSnuffle(Point position){
            return findNearest(position, game.getSnaffles());
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
        
        //Checks if snaffle is in the triangle with vertices pi, p2, p3.
        public boolean isLined(Entity p1, Entity snaffle, Point p2, Point p3){
            Point snafflePos = new Point((int) snaffle.futurePosition().x, (int) snaffle.futurePosition().y);
            
            double areaBig = getTriangleArea(p1.position, p2, p3);
            double a1 = getTriangleArea(snafflePos, p2, p3) ;
            double a2 = getTriangleArea(p1.position, snafflePos, p3);
            double a3 = getTriangleArea(p1.position, snafflePos, p2);
            
            //TODO: check if tolleranct is too high/low
            return Math.abs( (areaBig - a1) - a2 - a3) <= 0.1;
        }
        
        private boolean isFlipendoLinedToGoal(Entity player, Entity snaffle, int team){
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
        
        private <E> HashMap<E, Integer> rank(List<E> entries, BiFunction<Game,E,Integer> rankingFunction){
        	HashMap<E, Integer> result = new HashMap<E, Integer>();
        	for(E entry : entries){
        		result.put(entry, rankingFunction.apply(game, entry));
        	}
        	return result;
        }
        
    }
    
    
    public static class Entity{
        
    	public static final int BLUDGER_THRUST = 1000, MAX_WIZARD_THRUST = 150;
    	public static final int RADIUS_WIZARD = 400, RADIUS_SNAFFLE = 150, RADIUS_BLUDGER = 200;
    	public static final double FRICTION_WIZARD = 0.75, FRICTION_SNAFFLE = 0.75, FRICTION_BLUDGER = 0.9;
    	public static final double MASS_WIZARD = 1, MASS_SNAFFLE = 0.5, MASS_BLUDGER = 8;
    	
        public int id; // entity identifier
        public String entityType ; // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
        public int vx; // velocity
        public int vy; // velocity
        public int x;
        public int y;
        public Point position;
        
        // 1 if the wizard is holding a Snaffle, 0 otherwise
        // 1 was grabbed by a wizard, 0 otherwise
        public int state; 
        
        
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
        
        public Entity futureTurn(){
        	Point futurePosition = futurePosition();
        	VectorInt futureSpeed = new VectorInt(vx, vy).multiply(getFriction());
        	
        	Entity future = new Entity(id, entityType);
        	//TODO: BUG - I don't really know the future state, and can't know unless I implement collisions and full turn prediction.
        	//I'll put -1, so it will error the program when used.
        	future.updateInfo(futurePosition.x, futurePosition.y, futureSpeed.x, futureSpeed.y, -1);
        	
        	return future;
        }
        
        public Entity futureTurn(VectorInt moveTarget, int thrust){
        	Point futurePosition = futurePosition(moveTarget, thrust);
        	VectorInt futureSpeed = computeSpeed(moveTarget, thrust).multiply(getFriction());
        	
        	Entity future = new Entity(id, entityType);
        	//TODO: BUG - I don't really know the future state, and can't know unless I implement collisions and full turn prediction.
        	//I'll put -1, so it will error the program when used.
        	future.updateInfo(futurePosition.x, futurePosition.y, futureSpeed.x, futureSpeed.y, -1);
        	
        	return future;
        }
        
        public Point futurePosition(){
        	return new Point(x + vx, y + vy);
        }
        
        public Point futurePosition(VectorInt moveTarget, int thrust){
        	VectorInt myPosition = new VectorInt(position);
        	VectorInt newSpeed = computeSpeed(moveTarget, thrust);
        	
        	return new Point(myPosition.x + newSpeed.x, myPosition.y + newSpeed.y);
        	
        }
        
        public VectorInt computeSpeed(VectorInt moveTarget, int thrust){
        	VectorInt myPosition = new VectorInt(position);
        	VectorInt direction = moveTarget.minus(myPosition).norm();
        	
        	VectorInt currSpeed = new VectorInt(vx, vy);
        	VectorInt newSpeed = null;
        	
        	newSpeed = currSpeed.add(direction.multiply( thrust / getMass()));
        	
        	return newSpeed;
        }
        
        public boolean willCollide(Entity other) {
    		VectorInt toOther = new VectorInt(other.position).minus(new VectorInt(position));
    		VectorInt relativeSpeed = new VectorInt(vx, vy).minus(new VectorInt(other.vx, other.vy));
    		if (relativeSpeed.length2() <= 0)// No relative movement
    			return false;
    		if (toOther.dot(relativeSpeed) < 0)// Opposite directions
    			return false;
    		return Math.abs(relativeSpeed.norm().ortho().dot(toOther)) <= this.getRadius() + other.getRadius();
    	}

		private double getFriction(){
			switch(entityType){
		    	case "WIZARD":
		    	case "OPPONENT_WIZARD":
		    		return FRICTION_WIZARD;
		    	case "SNAFFLE":
		    		return FRICTION_SNAFFLE;
		    	case "BLUDGER":
		    		return FRICTION_BLUDGER;
		    		
		    	default:
		    		System.err.println("Error: This doesn't make any sense");
		    		return 1;
			}
		}

		private double getMass(){
			switch(entityType){
		    	case "WIZARD":
		    	case "OPPONENT_WIZARD":
		    		return MASS_WIZARD;
		    	case "SNAFFLE":
		    		return MASS_SNAFFLE;
		    	case "BLUDGER":
		    		return MASS_BLUDGER;
		    		
		    	default:
		    		System.err.println("Error: This doesn't make any sense");
		    		return 1;
			}
		}

		private double getRadius(){
			switch(entityType){
		    	case "WIZARD":
		    	case "OPPONENT_WIZARD":
		    		return RADIUS_WIZARD;
		    	case "SNAFFLE":
		    		return RADIUS_SNAFFLE;
		    	case "BLUDGER":
		    		return RADIUS_WIZARD;
		    		
		    	default:
		    		System.err.println("Error: This doesn't make any sense");
		    		return 1;
			}
		}
        
       
        
        /** Does not take into account friction */
        /*
        public Point futurePosition(){
        	Point newPoint = (Point)this.position.clone();
        	if(entityType.contains("WIZARD")){
        		newPoint.translate((int)(vx*0.75), (int)(vy*0.75));
        	} 
        	if(entityType.equals("SNAFFLE")){
        		newPoint.translate((int)(vx*0.75), (int)(vy*0.75));
        	}
        	if(entityType.equals("BLUDGER")){
        		newPoint.translate((int)(vx*0.9), (int)(vy*0.9));
        	}
        	return newPoint;
        }
        */
    }
    
    public static class Game {
        
    	public static final Point 	goal0_center = new Point(   0  , 3750 );
    	public static final Point	goal0_top    = new Point(   0  , 3750-(2000-300) );
    	public static final Point	goal0_bottom = new Point(   0  , 3750+(2000-300) );
    	public static final Point	goal1_center = new Point( 16000, 3750 );
    	public static final Point	goal1_top    = new Point( 16000, 3750-(2000-300) );
    	public static final Point	goal1_bottom = new Point( 16000, 3750+(2000-300) );
    	
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
        
        public List<Entity> getSnaffles(){
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
            return getSnaffles().size();
        }
        
        public double getDistance(Entity e1, Entity e2){
            return getDistance(e1.position, e2.position);
        }
        
        public double getDistance(VectorInt start, VectorInt end){
            return Math.sqrt(Math.pow(start.x-end.x,2)+Math.pow(start.y-end.y,2));
        }
        
        public double getDistance(Point start, Point end){
           return Math.sqrt(Math.pow(start.x-end.x,2)+Math.pow(start.y-end.y,2));
        }
        
        public double getDistanceFromGoal(Entity e, int team){
            return getDistanceFromGoal(e.position, team);
        }
        
        public double getDistanceFromGoal(Point start, int team){
            return getDistance(start, getGoal(team));
        }
        
        public double getDistanceFuture(Entity e1, Entity e2){
        	Point start = e1.futurePosition();
        	Point end = e2.futurePosition();
        	return Math.sqrt(Math.pow(start.x-end.x,2)+Math.pow(start.y-end.y,2));
        }
        
        public Point getGoal(int team){
            return (Point) ((team==0) ? goal0_center.clone() : goal1_center.clone());
        }
        
        public Point getGoalTop(int team){
            return (Point) ((team==0) ? goal0_top.clone() : goal1_top.clone());
                
        }
        
        public Point getGoalBottom(int team){
            return (Point) ((team==0) ? goal0_bottom.clone() : goal1_bottom.clone());
                
        }
        
        @Override
        protected Object clone() {
        	Game clone = new Game();
        	
        	clone.score = (int[])this.score.clone();       
            clone.entities = new LinkedList<Entity>();
            for(Entity e : this.entities){
            	clone.entities.add(new Entity(e));
            }
        	
            clone.entitiesDict = new HashMap<Integer, Entity>();
            for(Integer key : this.entitiesDict.keySet()){
            	Entity e = entitiesDict.get(key);
            	clone.entitiesDict.put(key, new Entity(e));
            }
        	return clone;
        }
    }
    
    /**
     * @author Manwe
     * 
     * Class representing a vector (x,y) with double precision
     * It contains final fields and will return a new instance on each performed operations
     *
     */
    public static class VectorInt {
        private static String doubleToString(double d) {
            return String.format("%.3f", d);
        }

        public final int x;

        public final int y;

        /**
         * Used in the equals method in order to consider two double are "equals"
         */
        public static double COMPARISON_TOLERANCE = 0.0000001;

        /**
         * Constructor from a given point
         * @param point
         * 	The point from which we will take the x and y
         */
        public VectorInt(Point coord) {
            this(coord.x, coord.y);
        }

        /**
         * Constructor from two double
         * @param x
         * 	the x value of the vector
         * @param y
         *  the y value of the vector
         */
        public VectorInt(double x, double y) {
            this.x = (int) x;
            this.y = (int) y;
        }
        
        public VectorInt(int x, int y) {
            this.x = x;
            this.y = y;
        }

        
        /**
         * Constructor from another vector
         * @param other
         * 		use the x and y values of the given vector
         */
        public VectorInt(VectorInt other) {
            this(other.x, other.y);
        }

        
        /**
         * Add to this vector the given vector
         * @param other
         * @return
         * 	a new instance of vector sum of this and the given vector
         */
        public VectorInt add(VectorInt other) {
            return new VectorInt(x + other.x, y + other.y);
        }

        /**
         * Negates this vector. The vector has the same magnitude as before, but its direction is now opposite.
         * 
         * @return a new vector instance with both x and y negated
         */
        public VectorInt negate() {
            return new VectorInt(-x, -y);
        }

        /**
         * Return a new instance of vector rotated from the given number of degrees.
         * @param degree
         * 		the number of degrees to rotate
         * @return
         * 		a new instance rotated
         */
        public VectorInt rotateInDegree(double degree){
        	return rotateInRadian(Math.toRadians(degree));
        }

        /**
         * Return a new instance of vector rotated from the given number of radians.
         * @param radians
         * the number of radians to rotate
         * @return
         * a new instance rotated
         */
        public VectorInt rotateInRadian(double radians) {
            final double length = length();
            double angle = angleInRadian();
            angle += radians;
            final VectorInt result = new VectorInt(Math.cos(angle), Math.sin(angle));
            return result.multiply(length);
        }

        /**
         * @return
         * 	the angle between this vector and the vector (1,0) in degrees
         */
        public double angleInDegree() {
            return Math.toDegrees(angleInRadian());
        }

    	/**
    	 * @return
         * 	the angle between this vector and the vector (1,0) in radians
    	 */
    	private double angleInRadian() {
    		return Math.atan2(y, x);
    	}

        /**
         * dot product operator
         * two vectors that are perpendicular have a dot product of 0
         * @param other
         * 		the other vector of the dot product
         * @return
         * 		the dot product
         */
        public double dot(VectorInt other) {
            return x * other.x + y * other.y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final VectorInt other = (VectorInt) obj;
            if (Math.abs(x - other.x) > COMPARISON_TOLERANCE) {
                return false;
            }
            if (Math.abs(y - other.y) > COMPARISON_TOLERANCE) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        /**
         * @return the length of the vector
         * Hint: prefer length2 to perform length comparisons
         */
        public double length() {
            return Math.sqrt(x * x + y * y);
        }

        /**
         * @return the square of the length of the vector
         */
        public double length2() {
            return x * x + y * y;
        }

        /**
         * Return the vector resulting in this vector minus the values of the other vector
         * @param other
         * the instance to substract from this
         * @return
         * 
         * a new instance of vector result of the minus operation.
         */
        public VectorInt minus(VectorInt other) {
            return new VectorInt(x - other.x, y - other.y);
        }

        /**
         * multiplication operator
         * @param factor
         * the double coefficient to multiply the vector with
         * @return
         * return a new instance multiplied by the given factor
         */
        public VectorInt multiply(double factor) {
            return new VectorInt(x * factor, y * factor);
        }

        /**
         * @return
         * the new instance normalized from this. A normalized instance has a length of 1
         * If the length of this is 0 returns a (0,0) vector
         */
        public VectorInt norm() {
            final double length = length();
            if (length>0)
            	return new VectorInt(x / length, y / length);
            return new VectorInt(0,0);
        }

        /**
         * Returns the orthogonal vector (-y,x).
         * @return
         *  a new instance of vector perpendicular to this
         */
        public VectorInt ortho() {
            return new VectorInt(-y, x);
        }

        @Override
        public String toString() {
            return "[x=" + doubleToString(x) + ", y=" + doubleToString(y) + "]";
        }
    }
    
    public static class VectorD {
        private static String doubleToString(double d) {
            return String.format("%.3f", d);
        }

        public final double x;

        public final double y;

        /**
         * Used in the equals method in order to consider two double are "equals"
         */
        public static double COMPARISON_TOLERANCE = 0.0000001;

        /**
         * Constructor from a given point
         * @param point
         * 	The point from which we will take the x and y
         */
        public VectorD(Point coord) {
            this(coord.x, coord.y);
        }

        /**
         * Constructor from two double
         * @param x
         * 	the x value of the vector
         * @param y
         *  the y value of the vector
         */
        public VectorD(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public VectorD(int x, int y) {
            this.x = x;
            this.y = y;
        }

        
        /**
         * Constructor from another vector
         * @param other
         * 		use the x and y values of the given vector
         */
        public VectorD(VectorD other) {
            this(other.x, other.y);
        }

        
        /**
         * Add to this vector the given vector
         * @param other
         * @return
         * 	a new instance of vector sum of this and the given vector
         */
        public VectorD add(VectorD other) {
            return new VectorD(x + other.x, y + other.y);
        }

        /**
         * Negates this vector. The vector has the same magnitude as before, but its direction is now opposite.
         * 
         * @return a new vector instance with both x and y negated
         */
        public VectorD negate() {
            return new VectorD(-x, -y);
        }

        /**
         * Return a new instance of vector rotated from the given number of degrees.
         * @param degree
         * 		the number of degrees to rotate
         * @return
         * 		a new instance rotated
         */
        public VectorD rotateInDegree(double degree){
        	return rotateInRadian(Math.toRadians(degree));
        }

        /**
         * Return a new instance of vector rotated from the given number of radians.
         * @param radians
         * the number of radians to rotate
         * @return
         * a new instance rotated
         */
        public VectorD rotateInRadian(double radians) {
            final double length = length();
            double angle = angleInRadian();
            angle += radians;
            final VectorD result = new VectorD(Math.cos(angle), Math.sin(angle));
            return result.multiply(length);
        }

        /**
         * @return
         * 	the angle between this vector and the vector (1,0) in degrees
         */
        public double angleInDegree() {
            return Math.toDegrees(angleInRadian());
        }

    	/**
    	 * @return
         * 	the angle between this vector and the vector (1,0) in radians
    	 */
    	private double angleInRadian() {
    		return Math.atan2(y, x);
    	}

        /**
         * dot product operator
         * two vectors that are perpendicular have a dot product of 0
         * @param other
         * 		the other vector of the dot product
         * @return
         * 		the dot product
         */
        public double dot(VectorD other) {
            return x * other.x + y * other.y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final VectorD other = (VectorD) obj;
            if (Math.abs(x - other.x) > COMPARISON_TOLERANCE) {
                return false;
            }
            if (Math.abs(y - other.y) > COMPARISON_TOLERANCE) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        /**
         * @return the length of the vector
         * Hint: prefer length2 to perform length comparisons
         */
        public double length() {
            return Math.sqrt(x * x + y * y);
        }

        /**
         * @return the square of the length of the vector
         */
        public double length2() {
            return x * x + y * y;
        }

        /**
         * Return the vector resulting in this vector minus the values of the other vector
         * @param other
         * the instance to substract from this
         * @return
         * 
         * a new instance of vector result of the minus operation.
         */
        public VectorD minus(VectorD other) {
            return new VectorD(x - other.x, y - other.y);
        }

        /**
         * multiplication operator
         * @param factor
         * the double coefficient to multiply the vector with
         * @return
         * return a new instance multiplied by the given factor
         */
        public VectorD multiply(double factor) {
            return new VectorD(x * factor, y * factor);
        }

        /**
         * @return
         * the new instance normalized from this. A normalized instance has a length of 1
         * If the length of this is 0 returns a (0,0) vector
         */
        public VectorD norm() {
            final double length = length();
            if (length>0)
            	return new VectorD(x / length, y / length);
            return new VectorD(0,0);
        }

        /**
         * Returns the orthogonal vector (-y,x).
         * @return
         *  a new instance of vector perpendicular to this
         */
        public VectorD ortho() {
            return new VectorD(-y, x);
        }

        @Override
        public String toString() {
            return "[x=" + doubleToString(x) + ", y=" + doubleToString(y) + "]";
        }
    }
    
    public static class Line {
        private double slope;
        private double offset;

        private VectorInt pointOnLine = null;

        public Line(VectorInt point1, VectorInt point2)
        {
            this.slope = (point2.y - point1.y) / (float)(point2.x - point1.x);
            this.pointOnLine = point1;

            this.offset = pointOnLine.y - (slope * pointOnLine.x);
        }

        public Line(VectorInt point1, double slope)
        {
            this.pointOnLine = point1;
            this.slope = slope;
            this.offset = pointOnLine.y - (slope * pointOnLine.x);
        }

        public double GetY(double x)
        {
            return slope * x + offset;
        }
        
        public double GetX(double y){
        	return (y - offset) / slope;
        }

		public double getSlope() {
			return slope;
		}

		public double getOffset() {
			return offset;
		}
    }
    
    public static class InfluenceMap{
    	
    	protected float[][] influenceMap;
    	protected int width, height;
    	public DistanceFunc computeDistanceFunc;
    	
    	
    	public InfluenceMap(int width, int height, DistanceFunc computeDistanceFunc){
    		this.width = width;
    		this.height = height;
    		this.influenceMap = new float[width][];
    		for(int i=0; i<width; i++){
    			this.influenceMap[i] = new float[height];
    		}
    		this.computeDistanceFunc = computeDistanceFunc;
    	}
    	
    	public float get(int x, int y){
    		return influenceMap[x][y];
    	}
    	
    	public int[][] getNeighbours(int x, int y){
    		int noOfNeighbours = 8;
    		if(x == 0 || x == width-1){
    			noOfNeighbours -= 3;
    		}
    		if(y == 0 || y == width-1){
    			if(noOfNeighbours < 8){
    				noOfNeighbours -= 2;
    			}
    			noOfNeighbours -= 3;
    		}
    		
    		int currNeighbours = 0;
    		int[][] neighbours = new int[noOfNeighbours][];
    		for(int i=-1; i<=1; i++){
    			for(int j=-1; j<=1; j++){
    				if(i==0 && j==0){ continue; }
    				
	    			int xNeighbour = x-i, yNeighbour= y-j;
	    			if(xNeighbour >= 0 && xNeighbour <= width-1 
	    			&& yNeighbour >= 0 && yNeighbour <= height-1){
	    				neighbours[currNeighbours] = new int[]{xNeighbour, yNeighbour};
	    				currNeighbours++;
	    			}
    			}
    		}
    		return neighbours;
    	}
    	
    	public void applyInfluence(int x, int y, float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay){
    		applyInfluenceRecursive(x, y, amount, fullAmountDistance, reducedAmountDistance, distanceDecay);
    	}
    	
    	public void applyInfluenceRecursive(int x, int y, float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay){
    		if(fullAmountDistance < 0 && reducedAmountDistance < 0) { System.err.println("Error!"); }
    		
    		if(fullAmountDistance > 0){
    			influenceMap[x][y] = amount;
    			for(int[] neighbour : getNeighbours(x, y)){
    				applyInfluenceRecursive(neighbour[0], neighbour[1], amount, fullAmountDistance-1, reducedAmountDistance, distanceDecay);
    			}
    		}
    		
    		if(reducedAmountDistance > 0){
    			influenceMap[x][y] = amount;
    			for(int[] neighbour : getNeighbours(x, y)){
    				applyInfluenceRecursive(neighbour[0], neighbour[1], amount * distanceDecay, fullAmountDistance, reducedAmountDistance-1, distanceDecay);
    			}
    		}
    	}
    	
    	public void applyInfluence(float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay, int... points){
    		if(points.length%2 == 1){
    			System.err.println("invalid number of points args");
    		}
    		
    		int noOfPoints = points.length/2;
    		
    		amount /= noOfPoints;
    		
    		for(int i=0; i<noOfPoints; i++){
    			int pointX = points[i];
    			int pointY = points[i+1];
    			
    			applyInfluence(pointX, pointY, amount, fullAmountDistance, reducedAmountDistance, distanceDecay);
    		}
    		
    	}
    }
    
    public static interface DistanceFunc{
    	
    	public float apply(int x1, int y1, int x2, int y2);
    }
    
    public static class SpaghettiDebugger {
    	
    	public static int DEFAULT_PRIORITY = -1;
    	
    	private HashMap<String, Integer> labelToHitCount;
    	private HashMap<String, Integer> labelToPriority;
    	private HashMap<String, Integer[]> labelToTurns;
    	
    	private List<LineHitInfo> dataGathered = new ArrayList<LineHitInfo>();
    	
    	private int topPriority;
    	
    	public SpaghettiDebugger(){
    		topPriority = DEFAULT_PRIORITY;
    		labelToHitCount = new HashMap<String, Integer>();
    		labelToPriority = new HashMap<String, Integer>();
    		labelToTurns = new HashMap<String, Integer[]>();		
    	}
    	
    	public void trackLineHits(String label){
    		trackLineHits_Impl(label, -1, DEFAULT_PRIORITY);
    	}
    	
    	public void trackLineHits(String label, int turns){
    		trackLineHits_Impl(label, turns, DEFAULT_PRIORITY);
    	}
    	
    	public void trackLineHits(String label, int turns, int priority){
    		trackLineHits_Impl(label, turns, priority);
    	}
    	
    	private void trackLineHits_Impl(String label, int turns, int priority){
    		//We are skipping one entry in the stacktrace, because to get here we pass through one of the
    		//TrackLineHits methods
    		StackTraceElement[] stackTrace =  Thread.currentThread().getStackTrace();
    		
    		if(priority > topPriority){
    			topPriority = priority;
    		}
    		
    		labelToPriority.put(label, priority); //I think that simply overriding the value every time is more efficient
    		int turnsArrayLength = 0;
    		Integer[] temp = new Integer[1];
    		if(labelToTurns.containsKey(label)){
    			turnsArrayLength = labelToTurns.get(label).length;
    			temp = Arrays.copyOf(labelToTurns.get(label), turnsArrayLength+1);
    		}
    		temp[temp.length-1] = turns;
    		labelToTurns.put(label, temp);
    		
    		int hitsUntilNow = 0;
    		if(labelToHitCount.containsKey(label)){
    			hitsUntilNow = labelToHitCount.get(label);
    		}
    		labelToHitCount.put(label, hitsUntilNow+1);
    		
    		
    		LineHitInfo info = new LineHitInfo(label, turns, hitsUntilNow+1, priority, stackTrace);
            System.err.println(addHeaderAndFooter(info));
    		
    		dataGathered.add(info);
    	}
    	
    	public LineHitInfo getLastEntry(){
    		return dataGathered.get(dataGathered.size()-1);
    	}
    	
    	public String getSummary(){
    		return getSummary(false);
    	}
    	
    	public String getSummary(boolean sortByPriority){
    		if(labelToHitCount.keySet().size() == 0){ return ""; }
    		
    		StringBuilder resultBuilder = new StringBuilder();
            List<String> keys = new ArrayList<String>(labelToHitCount.keySet());
    		
            if(sortByPriority){
            	keys.sort( (label1, label2) -> labelToPriority.get(label1).compareTo(labelToPriority.get(label2) ) );
            }
            
    		for(String label : keys){
    			Integer hitCount = labelToHitCount.get(label);
    			//int turn = labelToTurns.get(label)[labelToTurns.get(label).length-1];
    			resultBuilder.append("Label <"+label+"> Hit count "+hitCount+"\n Turns: {");
    			for(Integer turn : labelToTurns.get(label)){
    				resultBuilder.append(" "+turn+",");
    			}
    			resultBuilder.deleteCharAt(resultBuilder.length()-1);
    			resultBuilder.append(" }\n");
    			
    		}
    		
    		return addHeaderAndFooter(resultBuilder.toString());
    	}
    	
    	public String getFullInfo(){
    		if(dataGathered.size() == 0){ return ""; }
    		
    		String result = "";

    		for(LineHitInfo currInfo : dataGathered){
    			result += currInfo.toString();
    		}

    		return addHeaderAndFooter(result);
    	}
    	
    	public String getFullInfoByLabel(String label){
    		String result = "";
    		for(LineHitInfo info : dataGathered){
    			if(info.getLabel().equals(label)){
    				result += info.toString()+" \n";
    			}
    		}
    		return result;
    	}
    	
    	public String getFullInfoByPriority(int priority){
    		String result = "";
    		for(LineHitInfo info : dataGathered){
    			if(info.getPriority() == priority){
    				result += info.toString()+" \n";
    			}
    		}
    		return result;
    	}
    	
    	public String getFullInfoTopPriority(){
    		int topPriority = getTopPriority();
    		return getFullInfoByPriority(topPriority);
    	}
    	
    	private int getTopPriority(){
    		return topPriority;
    	}
    	
    	private String addHeaderAndFooter(Object o){
    		return "------------Spaghetti Debgugger-------------\n"+
    				o.toString()+
    				"--------------------------------------------\n";
    	}
    }
    
    public static class LineHitInfo{
    	
    	private String label;
    	private int lineNumber;
    	private int hitCount;
    	private int priority;
    	private int turn;
    	private String stackTrace;
    	
		public LineHitInfo(String label, int turn, int hitCount, int priority, StackTraceElement[] stackTrace) {
			this.label = label;
			this.turn = turn;
			this.hitCount = hitCount;
			this.priority = priority;
			
			String temp = stackTrace[3].toString();
    		int startIndex = temp.lastIndexOf(":")+1;
    		int endIndex = temp.lastIndexOf(")");
    		this.lineNumber = Integer.parseInt(temp.substring(startIndex, endIndex));
    		
    		this.stackTrace = "";
    		
    		for (int i = 3; i < stackTrace.length; i++) {
				StackTraceElement e = stackTrace[i];
				this.stackTrace += "  "+e.toString()+"\n";
			}
    	}
		
		public String getLabel(){
			return label;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public int getHitCount() {
			return hitCount;
		}

		public int getPriority() {
			return priority;
		}
		
		@Override
		public String toString(){
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Label <"+label+"> - ");
			//stringBuilder.append("Line Number "+lineNumber+" - ");
			stringBuilder.append("Hit count "+hitCount+"\n");
			stringBuilder.append("Turn "+turn+"\n");
			stringBuilder.append("Stack Trace: \n"+stackTrace+"\n");
			return stringBuilder.toString();
		}
	}

}
