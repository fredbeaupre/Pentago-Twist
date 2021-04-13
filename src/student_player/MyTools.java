package student_player;
import pentago_twist.PentagoMove;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoCoord;
import pentago_twist.PentagoBoardState.Piece;
import java.util.*;
import java.util.function.UnaryOperator;

public class MyTools {
    //////////////////////////// GLOBAL VARIABLES ////////////////////////////
    public static int CENTRE_MARBLE_WEIGHT = 5;
    public static int TRIPLET_WEIGHT = 100;
    public static int QUADRUPLET_WEIGHT = 1000;
    public static int QUINTUPLET_WEIGHT = 100000;
    public static int WIN_COST = 100000;
    public static final int SIM_TIME_LIMIT = 800;
    public static final int MOVE_TIME_LIMIT = 1920;
    public static int DEPTH = 2;
    public static final int INCREASE_DEPTH = 10;
    private static final UnaryOperator<PentagoCoord> getNextHorizontal = c -> new PentagoCoord(c.getX(), c.getY()+1);
    private static final UnaryOperator<PentagoCoord> getNextVertical = c -> new PentagoCoord(c.getX()+1, c.getY());
    private static final UnaryOperator<PentagoCoord> getNextDiagRight = c -> new PentagoCoord(c.getX()+1, c.getY()+1);
    private static final UnaryOperator<PentagoCoord> getNextDiagLeft = c -> new PentagoCoord(c.getX()+1, c.getY()-1);
    private static final PentagoCoord topLeft = new PentagoCoord(1, 1);
    private static final PentagoCoord topRight = new PentagoCoord(1, 4);
    private static final PentagoCoord bottomLeft = new PentagoCoord(4, 1);
    private static final PentagoCoord bottomRight = new PentagoCoord(4,4);

    //////////////////////////// SEARCH ALGORITHMS ////////////////////////////

    /**
     * Function to find the best move using alpha-beta pruning or negamax, applied on an
     * ArrayList of moves that were filtered from running some MonteCarlo simulations
     * @param pbs: board state
     * @param studentTurn: tells us if student has the white or black pieces
     * @return the best move
     */
    public static PentagoMove findBestMove(PentagoBoardState pbs, int studentTurn){
        boolean isMaxPlayer = studentTurn == 0;
        HashMap<PentagoMove, Double> moveRankings = new HashMap<>();
        PentagoMove bestMove;
        long start = System.currentTimeMillis();

        ArrayList<PentagoMove> bestLegalMoves = removeObviousLosses(studentTurn, pbs);
        bestLegalMoves = monteCarloSimulations(pbs, studentTurn, bestLegalMoves);

        if (bestLegalMoves.size() == 1){
            return bestLegalMoves.get(0);
        }

        for (PentagoMove move: bestLegalMoves){
            if (System.currentTimeMillis() - start > MOVE_TIME_LIMIT){
                System.out.println("Taking too long...");
                break;
            }

            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);
            double ab = negamax(studentTurn, cloneState, DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE);
            moveRankings.put(move, (double)ab);
        }

        moveRankings = sortByScore(moveRankings);
        int bestIndex = Math.max(moveRankings.keySet().size() - 1, 0);
        bestMove = (PentagoMove) moveRankings.keySet().toArray()[bestIndex];
        return bestMove;


    } // findBestMove

    /**
     * Alpha-Beta pruning algorithm
     * @param studentTurn: white or black pieces
     * @param pbs: board state
     * @param depth: to what depth alpha-beta will search before return cost
     * @param alpha: alpha value
     * @param beta: beta value
     * @param isMaxPlayer: if student is maxPlayer
     * @return alpha-beta bestValue
     */
    public static int alphaBeta(int studentTurn, PentagoBoardState pbs, int depth, int alpha, int beta, boolean isMaxPlayer){
        if (depth == 0 || pbs.gameOver()){
            return getEvaluation(pbs);
        }

        int eval;
        ArrayList<PentagoMove> legalMoves = pbs.getAllLegalMoves();

        if (isMaxPlayer){
            int maxEval = Integer.MIN_VALUE;
            for (PentagoMove move : legalMoves){
                PentagoBoardState cloneState = cloneBoard(pbs);
                cloneState.processMove(move);
                eval = alphaBeta(studentTurn, cloneState, depth -1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, maxEval);
                if (beta <= alpha){
                    break;
                }
            }
            return maxEval;
        } else{
            int minEval = Integer.MAX_VALUE;
            for (PentagoMove move : legalMoves){
                PentagoBoardState cloneState = cloneBoard(pbs);
                cloneState.processMove(move);
                eval = alphaBeta(studentTurn, cloneState, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, minEval);
                if (beta <= alpha){
                    break;
                }
            }
            return minEval;
        }
    } // alphaBeta

    /**
     * Negamax Search algorithm
     * @param currentTurn: white or black pieces
     * @param pbs: board state
     * @param depth: to what depth negamax will search
     * @param alpha: alpha value
     * @param beta: beta value
     * @return negamax best value found
     */
    public static int negamax(int currentTurn, PentagoBoardState pbs, int depth, int alpha, int beta){
        int currentColor = currentTurn % 2 == 0 ? 1: -1;
        if (depth == 0 || pbs.gameOver()){
            return currentColor * getEvaluation(pbs);
        }
        int bestValue = Integer.MIN_VALUE;
        ArrayList<PentagoMove> legalMoves = pbs.getAllLegalMoves();

        for (PentagoMove move : legalMoves){
            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);
            int value = -1*negamax(currentTurn+1, cloneState, depth - 1, -1*beta, -1*alpha);
            bestValue = Math.max(value, bestValue);
            if (bestValue >= beta){
                return beta;
            }
            if(value > alpha){
                alpha = bestValue;
            }
        }
        return bestValue;
    } // negamax

    /**
     *
     * @param playerTurn
     * @param pbs
     * @return
     */
    public static ArrayList<PentagoMove> removeObviousLosses(int playerTurn, PentagoBoardState pbs){
        ArrayList<PentagoMove> legalMoves = pbs.getAllLegalMoves();
        Collections.shuffle(legalMoves);
        ArrayList<PentagoMove> bestLegalMoves = new ArrayList<>();

        for (PentagoMove move : legalMoves){
            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);
            boolean isWin = cloneState.gameOver() && checkGameResult(cloneState, playerTurn) > 0;
            boolean isLoss = cloneState.gameOver() && checkGameResult(cloneState, playerTurn) < 0;

            if (isWin){ // return right away if we have a win
                bestLegalMoves.add(move);
                return bestLegalMoves;
            }
            if (isLoss){ // skip move if it leads directly to a loss
                continue;
            }

            bestLegalMoves.add(move); // add moves that are neither a direct loss nor a direct win
        }

        return bestLegalMoves;
    } // removeObviousLosses

    public static ArrayList<PentagoMove> monteCarloSimulations(PentagoBoardState pbs, int studentTurn, ArrayList<PentagoMove> moves){
        long start = System.currentTimeMillis();
        HashMap<PentagoMove, Tuple> moveRankings = new HashMap<>();
        boolean simDone = false;
        double simCounter = 1.0;

        for (PentagoMove move: moves){
            if (!moveRankings.containsKey(move)){ // move has never yet been performed
                moveRankings.put(move, new Tuple<>(0.0, 0.0));
            }

            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);

            // simulate with random moves until we reach end of game
            // i.e., default policy
            while(!cloneState.gameOver()){
                cloneState.processMove((PentagoMove) cloneState.getRandomMove());
            }
            boolean isWin = checkGameResult(cloneState, studentTurn) > 0;
            double wins = (double) moveRankings.get(move).x;
            double visits = (double) moveRankings.get(move).y;

            if (isWin){
                moveRankings.put(move, new Tuple<>(wins+1, visits+1));
            } else{
                moveRankings.put(move, new Tuple<>(wins, visits+1));
            }
        } // for-loop
        // before, we had random simulations, from here we investigate which simulations would
        // be beneficial
        // First we compute the upper confidence tree of each state:

        double bestUCT = -1;
        PentagoMove chosenMove = null;
        while(true){
            simCounter++;
            for (PentagoMove move: moveRankings.keySet()){
                if (System.currentTimeMillis() - start > SIM_TIME_LIMIT){
                    simDone = true;
                    break;
                }

                Tuple moveStats = moveRankings.get(move);
                double uct = (  ((double) moveStats.x) / ((double) moveStats.y) ) +
                        (Math.sqrt(2 * Math.log(simCounter)) / ((double) moveStats.y));

                if (uct > bestUCT){
                    bestUCT = uct;
                    chosenMove = move;
                }
            } // for-loop

            if (simDone){
                break;
            }
            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(chosenMove);
            // simulate with random moves until we reach game's end
            while(!cloneState.gameOver()){
                cloneState.processMove((PentagoMove) cloneState.getRandomMove());
            }

            boolean isWin = checkGameResult(cloneState, studentTurn) > 0;
            double wins = (double) moveRankings.get(chosenMove).x;
            double visits = (double) moveRankings.get(chosenMove).y;

            if (isWin){
                moveRankings.put(chosenMove, new Tuple<>(wins+1, visits+1));
            }else{
                moveRankings.put(chosenMove, new Tuple<>(wins, visits+1));
            }
            // reset before next iteration of while loop
            chosenMove = null;
            bestUCT = -1;
        } // while loop

        HashMap<PentagoMove, Double> movesAndStats = new HashMap<>();
        for (PentagoMove move: moveRankings.keySet()){
            double wins = (double) moveRankings.get(move).x;
            double visits = (double) moveRankings.get(move).y;
            movesAndStats.put(move, wins/visits);
        }
        movesAndStats = sortByScore(movesAndStats);
        ArrayList<PentagoMove> topKMoves = topKSample(movesAndStats,50, Integer.MIN_VALUE/2);
        return topKMoves;
    } // monteCarloSimulations

    //////////////////////////// SAMPLING METHODS ////////////////////////////
    private static ArrayList<PentagoMove> topKSample(HashMap<PentagoMove, Double> map, int k, double thresh){
        ArrayList<PentagoMove> topK = new ArrayList<>();
        Object[] moves = map.keySet().toArray(); // list of moves
        if (moves.length < k){ // if size is smaller than k, put all moves in the list
            for (Object move : moves) {
                topK.add((PentagoMove) move);
            }
        }else{ // number of moves is larger than k
            for(int i = moves.length - 1; i > moves.length - 1 - k; i--){
                PentagoMove move = (PentagoMove) moves[i];
                if (map.get(move) > thresh){
                    topK.add((PentagoMove) moves[i]);
                }
            }
        }
        return topK;
    }

    //////////////////////////// EVALUATION METHODS ////////////////////////////


    /**
     * Checks the number of marbles the current player has in the centre of the board, and
     * mulitplies that quantity by the weight of centre marbles
     * @param board: board state
     * @param color: this player's piece color
     * @return partial cost of board associated with centre marbles
     */
    public static int checkCentreMarbles(Piece[][] board, Piece color){
        int counter = 0;
        for (int i = 1; i< PentagoBoardState.BOARD_SIZE - 1; i++){
            for (int j = 1; j < PentagoBoardState.BOARD_SIZE -1; j++){
                if (board[i][j] == color){
                    counter++;
                }
            }
        }
        return (counter*CENTRE_MARBLE_WEIGHT);
    }

    /**
     *
     * @param pbs
     * @param color
     * @return
     */
    public static int checkDiagonals(PentagoBoardState pbs, Piece color){
        int totalCost = 0;
        PentagoCoord topLeftDiag = new PentagoCoord(0,1);
        PentagoCoord midLeftDiag = new PentagoCoord(0, 0);
        PentagoCoord bottomLeftDiag = new PentagoCoord(1, 0);
        PentagoCoord topRightDiag = new PentagoCoord(0,4);
        PentagoCoord midRightDiag = new PentagoCoord(0,5);
        PentagoCoord bottomRightDiag = new PentagoCoord(1,5);

        PentagoCoord[] leftRightDiags = {topLeftDiag, midLeftDiag, bottomLeftDiag};
        PentagoCoord[] rightLeftDiags = {topRightDiag, midRightDiag, bottomRightDiag};

        for (PentagoCoord coord: leftRightDiags){
            totalCost += traverseDiagonal(pbs, coord, getNextDiagRight, color);
        }

        for (PentagoCoord coord: rightLeftDiags){
            totalCost += traverseDiagonal(pbs, coord, getNextDiagLeft, color);
        }
        return totalCost;
    }

    /**
     * Traverse a diagonal and look for streaks
     * @param pbs
     * @param startSquare
     * @param direction
     * @param color
     * @return
     */
    public static int traverseDiagonal(PentagoBoardState pbs, PentagoCoord startSquare, UnaryOperator<PentagoCoord> direction, Piece color){
        int streak = 1;
        int triplets = 0;
        int quadruplets = 0;
        int quintuplets = 0;

        PentagoCoord current = startSquare;
        while (true){
            try{
                if (pbs.getPieceAt(current) == color){
                    streak++;
                    current = direction.apply(current);
                }else{
                    switch(streak){
                        case 1: ;
                        case 2: ;
                        case 3: triplets++;
                        case 4: quadruplets++;
                        case 5: quintuplets++;
                    }
                    streak = 1;
                    current = direction.apply(current);
                }
            } catch(IllegalArgumentException e){
                switch(streak){
                    case 1: ;
                    case 2: ;
                    case 3: triplets++;
                    case 4: quadruplets++;
                    case 5: quintuplets++;
                }
                streak = 1;
                break;
            } // end of try block
        } // while loop
        return (triplets * TRIPLET_WEIGHT) + (quadruplets * QUADRUPLET_WEIGHT) + (quintuplets*QUINTUPLET_WEIGHT);
    }

    /**
     *
     * @param board
     * @param color
     * @return
     */
    public static int checkVerticals(Piece[][] board, Piece color){
        int triplets = 0;
        int quadruplets = 0;
        int quintuplets = 0;
        int streak = 1;
        for (int j = 0; j< PentagoBoardState.BOARD_SIZE; j++){
            for (int i= 0; i < PentagoBoardState.BOARD_SIZE-1;i++){
                Piece piece = board[i][j];
                Piece nextPiece = board[i+1][j];
                if (piece == nextPiece && piece == color){
                    streak++;
                }else{
                    switch(streak){
                        case 1: ;
                        case 2: ;
                        case 3: triplets++;
                        case 4: quadruplets++;
                        case 5: quintuplets++;
                    }
                    streak = 1;
                }
            } // inner for-loop
        } // outer for-loop;
        return (triplets * TRIPLET_WEIGHT) + (quadruplets * QUADRUPLET_WEIGHT) + (quintuplets * QUINTUPLET_WEIGHT);
    } //checkVerticals

    /**
     * Checks horizontals for three, four or five in a row of the given color
     * @param board: board state
     * @param color: player's pieces color
     * @return partial cost associated with horizontal streaks
     */
    public static int checkHorizontals(Piece[][] board, Piece color){
        int triplets = 0;
        int quadruplets = 0;
        int quintuplets = 0;
        int streak = 1;
        for (int i = 0; i < PentagoBoardState.BOARD_SIZE; i++){
            for (int j = 0 ; j < PentagoBoardState.BOARD_SIZE - 1; j++){
                Piece piece = board[i][j];
                Piece nextPiece = board[i][j+1];
                if (piece == nextPiece && piece == color){
                    streak++;
                } else{
                    switch(streak){
                        case 1: ;
                        case 2: ;
                        case 3: triplets++;
                        case 4: quadruplets++;
                        case 5: quintuplets++;
                    }
                    streak = 1;
                }
            } // inner for-loop
        } // outer for-loop
        return (triplets * TRIPLET_WEIGHT) + (quadruplets * QUADRUPLET_WEIGHT) + (quintuplets * QUINTUPLET_WEIGHT);
    } // checkHorizontals

    /**
     * Gets the cost of a board state
     * Note that the evaluation always returns a value from the point of view of white, i.e.,
     * it will return large value for white having an advantage and small values for a disadvantage
     * This is done so that it's easier to use the same eval function for both alpha-beta and negamax
     * @param pbs: board state
     * @return cost
     */
    public static int getEvaluation(PentagoBoardState pbs){
        int whiteScore = 0;
        int blackScore = 0;
        int cost;
        Piece[][] board = pbs.getBoard();

        Piece whitePlayer = Piece.WHITE;
        Piece blackPlayer = Piece.BLACK;


        whiteScore = checkHorizontals(board, whitePlayer)+
                checkVerticals(board, whitePlayer)+
                checkCentreMarbles(board, whitePlayer)+
                checkDiagonals(pbs, whitePlayer);


        blackScore = checkHorizontals(board, blackPlayer)+
                checkVerticals(board, blackPlayer)+
                checkCentreMarbles(board, blackPlayer)+
                checkDiagonals(pbs, blackPlayer);



        cost = whiteScore - blackScore;
        return cost;
    } //getEvaluation

    //////////////////////////// "THEORY" METHODS ////////////////////////////

    /**
     * Function to hardcode the agent's first three moves (only 2 if black) according to
     * our own opening "theory"
     * @param pbs
     * @param playerColor
     * @param turnNumber
     * @return
     */
    public static PentagoMove firstThreeMoves(PentagoBoardState pbs, int playerColor, int turnNumber){
        int randomQuad = getRandomNumberInRange(0,3);
        int randomSwap = getRandomNumberInRange(0,1);
        Piece color = playerColor == 0 ? Piece.WHITE : Piece.BLACK;
        ArrayList<PentagoCoord> strongestFour = new ArrayList<>();
        strongestFour.add(topLeft);
        strongestFour.add(topRight);
        strongestFour.add(bottomLeft);
        strongestFour.add(bottomRight);
        Collections.shuffle(strongestFour);
        PentagoMove move = null;
        PentagoCoord newCoord = null;

        if (turnNumber == 0 || turnNumber == 1){
            for (PentagoCoord coord : strongestFour){
                if (pbs.getPieceAt(coord) == Piece.EMPTY){
                    move = new PentagoMove(coord, randomQuad, randomSwap, playerColor);
                }
            }
        }
        else if (turnNumber == 2 && color == Piece.WHITE){
            for (PentagoCoord coord: strongestFour){
                if (pbs.getPieceAt(coord) == color){
                    newCoord = centreExpansion(pbs,coord, playerColor, color);
                    move = new PentagoMove(newCoord, randomQuad, randomSwap, playerColor);
                }
            }
        } else{
            findBestMove(pbs,playerColor);
        }
        return move;
    }

    /**
     * Hardcode the 3rd move so that the agent tries to make a move towards the center
     * that also gives him a marble pair
     * @param pbs
     * @param coord
     * @param playerColor
     * @param color
     * @return
     */
    public static PentagoCoord centreExpansion(PentagoBoardState pbs, PentagoCoord coord, int playerColor, Piece color){
        int x = coord.getX();
        int y = coord.getY();
        PentagoCoord newCoord = null;

        if (x == 1 && y == 1){
            if (pbs.getPieceAt(x, y+1) == Piece.EMPTY){
                newCoord = new PentagoCoord(x, y+1);
            } else if(pbs.getPieceAt(x+1, y) == Piece.EMPTY){
                newCoord = new PentagoCoord(x+1, y);
            }
            else if(pbs.getPieceAt(x+1, y+1 ) == Piece.EMPTY){
                newCoord = new PentagoCoord(x+1, y+1);
            }
        } // topLeft case
        else if (x == 1 && y == 4){
            if (pbs.getPieceAt(x, y-1) == Piece.EMPTY){
                newCoord = new PentagoCoord(x, y-1);
            } else if(pbs.getPieceAt(x+1, y) == Piece.EMPTY){
                newCoord = new PentagoCoord(x+1, y);
            }
            else if(pbs.getPieceAt(x+1, y-1 ) == Piece.EMPTY){
                newCoord = new PentagoCoord(x+1, y-1);
            }
        } // topRight case
        else if (x == 4 && y == 1){
            if (pbs.getPieceAt(x-1, y) == Piece.EMPTY){
                newCoord = new PentagoCoord(x-1, y);
            } else if(pbs.getPieceAt(x, y+1) == Piece.EMPTY){
                newCoord = new PentagoCoord(x, y+1);
            }
            else if(pbs.getPieceAt(x-1, y+1 ) == Piece.EMPTY){
                newCoord = new PentagoCoord(x-1, y+1);
            }
        } // bottomLeft case
        else {
            if (pbs.getPieceAt(x-1, y) == Piece.EMPTY){
                newCoord = new PentagoCoord(x-1, y);
            } else if(pbs.getPieceAt(x, y-1) == Piece.EMPTY){
                newCoord = new PentagoCoord(x, y-1);
            }
            else if(pbs.getPieceAt(x-1, y-1 ) == Piece.EMPTY){
                newCoord = new PentagoCoord(x-1, y-1);
            }
        } // topLeft case
        return newCoord;
    }


    //////////////////////////// HELPER METHODS ////////////////////////////

    /**
     *
     * @param pbs
     * @param playerTurn
     * @return
     */
    public static int checkGameResult(PentagoBoardState pbs, int playerTurn){
        int winner = pbs.getWinner();
        int cost = winner == playerTurn ? WIN_COST : -1*WIN_COST;
        return cost;
    } // checkGameResult

    /**
     * Sorts a hashmap of moves in order of utility values
     * @param map: Hashmap of keys = moves and values = utilities
     * @return sorted HashMap of moves
     */
    public static HashMap<PentagoMove, Double> sortByScore(HashMap<PentagoMove, Double> map){
        List<Map.Entry<PentagoMove, Double>> moveList = new LinkedList<Map.Entry<PentagoMove, Double>> (map.entrySet());

        Collections.sort(moveList, new Comparator<Map.Entry<PentagoMove, Double>>() {
            @Override
            public int compare(Map.Entry<PentagoMove, Double> o1, Map.Entry<PentagoMove, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        HashMap<PentagoMove, Double> sortedMoves = new LinkedHashMap<PentagoMove, Double>();
        for (Map.Entry<PentagoMove, Double> temp : moveList){
            sortedMoves.put(temp.getKey(), temp.getValue());
        }
        return sortedMoves;
    } // sortByScore

    public static PentagoBoardState cloneBoard(PentagoBoardState pbs){
        return (PentagoBoardState) pbs.clone();
    } // sortByScore

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }


////////////////////////////////////// PRIVATE TUPLE CLASS //////////////////////////////////////

    /**
     * Tuple class to hold deconstructed utility
     * @param <X> Number of wins for a state
     * @param <Y> Number of visits of a state
     */
    private static class Tuple<X, Y>{
        public final X x;
        public final Y y;

        public Tuple(X x, Y y){
            this.x = x;
            this.y = y;
        }
    } // Tuple



} // MyTools