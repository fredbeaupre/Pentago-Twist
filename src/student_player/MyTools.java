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
    public static final int MOVE_TIME_LIMIT = 1888;
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
     * Function to find the best move using alpha-beta pruning or negamax, after running
     * MonteCarlo simulations to filter the legalMoves ArrayList so that it contains good
     * moves
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
                break;
            }

            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);
            double ab = alphaBeta(studentTurn, cloneState, DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, isMaxPlayer);
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
     * Function that takes all the legal moves for a given state and removes all those that
     * lead to a direct loss
     * @param playerTurn: (0 = white, 1 = black)
     * @param pbs: board state
     * @return list of moves with direct loss removed
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

    /**
     * Monte Carlo simulations to assign UCT values to states;
     * After simulation, we select topK states, sort them by score, and return to alphabeta
     * @param pbs: board state
     * @param studentTurn: (0 = white, 1 = black)
     * @param moves: possible moves from pbs
     * @return list of k good moves sorted by uct value
     */
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

    /**
     * Sample best k moves from a HashMap
     * @param map: HashMap of (Key, Value) -> (PentagoMove, UCT value of move)
     * @param k: number of moves in the sample
     * @param thresh: lower limit on move strength for it to be part of the sample
     * @return best k moves
     */
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
     * Functions that create an ArrayList containing what we believe are the strongest
     * squares on the board
     * @return ^
     */
    public static ArrayList<PentagoCoord> buildAnchors(){
        ArrayList<PentagoCoord> strongestFour = new ArrayList<>();
        strongestFour.add(topLeft);
        strongestFour.add(topRight);
        strongestFour.add(bottomLeft);
        strongestFour.add(bottomRight);
        Collections.shuffle(strongestFour);
        return strongestFour;
    }


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
        Piece oppColor = playerColor == 0 ? Piece.BLACK : Piece.WHITE;
        ArrayList<PentagoCoord> anchors = buildAnchors();
        PentagoMove move = null;
        PentagoCoord newCoord = null;
        Piece[][] board = pbs.getBoard();
        int checkStreaks = checkHorizontals(board, oppColor) + checkVerticals(board, oppColor) + checkDiagonals(pbs, oppColor);


        if (turnNumber == 0 || turnNumber == 1){
            for (PentagoCoord coord : anchors){
                if (pbs.getPieceAt(coord) == Piece.EMPTY){
                    move = new PentagoMove(coord, randomQuad, randomSwap, playerColor);
                    break;
                }
            }
        }
        else if (turnNumber == 2 && color == Piece.WHITE){
            for (PentagoCoord coord: anchors){
                if (pbs.getPieceAt(coord) == color){
                    newCoord = centreExpansion(pbs,coord, playerColor, color);
                    move = new PentagoMove(newCoord, randomQuad, randomSwap, playerColor);
                    break;
                }
            }
        } else{
            if (checkStreaks > 0){
                move = findBestMove(pbs, playerColor);
            }else{
                for (PentagoCoord coord : anchors){
                    if(pbs.getPieceAt(coord) == color){
                        newCoord = centreExpansion(pbs,coord, playerColor, color);
                        move = new PentagoMove(newCoord, randomQuad, randomSwap, playerColor);
                        break;
                    } else if (pbs.getPieceAt(coord) == Piece.EMPTY){
                        move = new PentagoMove(coord, randomQuad, randomSwap, playerColor);
                        break;
                    }
                } // for-loop
            }

        }
        return move;
    }

    /**
     * Hardcodes function's fourth and fifth moves
     * @param pbs
     * @param playerColor
     * @param turnNumber
     * @return PentagoMove
     */
    public static PentagoMove fourthAndFifthMoves(PentagoBoardState pbs, int playerColor, int turnNumber){
        int randomQuad = getRandomNumberInRange(0,3);
        int randomSwap = getRandomNumberInRange(0,1);
        Piece oppColor = playerColor == 0 ? Piece.BLACK : Piece.WHITE;
        Piece color = playerColor == 0 ? Piece.WHITE : Piece.BLACK;
        ArrayList<PentagoCoord> anchors = buildAnchors();
        Piece[][] board = pbs.getBoard();
        boolean anchorAvailable = false;
        PentagoMove move = null;
        PentagoCoord newCoord = null;
        int checkStreaks = checkHorizontals(board, oppColor) + checkVerticals(board, oppColor) + checkDiagonals(pbs, oppColor);
        if (checkStreaks > 0){
            move = findBestMove(pbs, playerColor);
        }else{
            for (PentagoCoord coord : anchors){
                if (pbs.getPieceAt(coord) == Piece.EMPTY){
                    anchorAvailable = true;
                    move = new PentagoMove(coord, randomQuad, randomSwap, playerColor);
                    break;
                } else if(pbs.getPieceAt(coord) == color && !anchorAvailable){
                    newCoord = centreExpansion(pbs,coord, playerColor, color);
                    move = new PentagoMove(newCoord, randomQuad, randomSwap, playerColor);
                    break;
                }
            } // for-loop
        } // if else

        return move;
    } // fourthAndFifthMoves

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


    ////////////////////////////////////// PRIVATE MONTECARLO CLASS //////////////////////////////////////
    static class MonteCarlo {
        private static final int WIN_SCORE = 10;
        private int depth;
        private int opponent;

        public MonteCarlo(){
            this.depth = 15;
        }

        public int getDepth(){
            return this.depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public int getOpponent(){
            return this.opponent;
        }

        public void setOpponent(int opponent) {
            this.opponent = opponent;
        }

        private int getTimeForThisDepth(){
            return (2000);
        }

        public PentagoMove findBestMCMove(PentagoBoardState pbs, int playerTurn){
            long start = System.currentTimeMillis();
            long end = start + 60 * getTimeForThisDepth();
            PentagoBoardState board = cloneBoard(pbs);
            PentagoMove bestMove = null;
            int gamesFinished = 0;
            int opponent = playerTurn == 0 ? 1 : 0;
            State rootState = new State(board);
            rootState.setLegalMoves(board.getAllLegalMoves());
            rootState.setPbs(board);
            rootState.setPlayerTurn(playerTurn);
            Node rootNode = new Node(rootState);
            rootNode.setState(rootState);
            Tree tree = new Tree(rootNode);
            while(System.currentTimeMillis() < end){

                // Select
                Node goodNode = selectGoodNode(rootNode);
                // Expand
                if (!goodNode.getState().getPbs().gameOver()){
                    expandNode(goodNode);
                }
                // Simulate
                Node nodeToVisit = goodNode;
                if (goodNode.getChildren().size() > 0 ){
                    nodeToVisit = goodNode.getRandomChild();
                }
                int result = simulatePlay(nodeToVisit);
                gamesFinished++;
                // Update
                backProp(nodeToVisit, result);
            }
            System.out.println("Games simulated : " + gamesFinished);
            Node bestNode = rootNode.getBestChild();
            for (PentagoMove move : board.getAllLegalMoves()){
                PentagoBoardState tempBoard = cloneBoard(board);
                tempBoard.processMove(move);
                if (tempBoard.equals(bestNode.getState().getPbs())){
                    bestMove = move;
                }
            }

            tree.setRoot(bestNode);
            return bestMove;

        } // findBestMCMove

        public static Node selectGoodNode(Node rootNode){
            Node node = rootNode;
            while (node.getChildren().size() != 0){
                node = UCT.findBestUCTNode(node);
            }

            return node;
        }

        public static void expandNode(Node node){
            ArrayList<PentagoMove> possibleMoves = node.getState().getLegalMoves();
            for (PentagoMove move : possibleMoves){
                PentagoBoardState newPbs = cloneBoard(node.getState().getPbs());
                newPbs.processMove(move);
                State newState = new State(newPbs);
                Node newNode = new Node(newState);
                newNode.setParent(node);
                newNode.getState().setPlayerTurn(node.getState().getPlayerTurn());
                node.getChildren().add(newNode);
            }
        } // expandNode

        public static void backProp(Node node, int playerTurn){
            Node tempNode = node;
            while (node!= null){
                tempNode.getState().incrementVisits();
                if (tempNode.getState().getPlayerTurn() == playerTurn){
                    tempNode.getState().addScore(WIN_SCORE);
                }
                tempNode = tempNode.getParent();
            }
        } // backProp

        public static int simulatePlay(Node node){
            Node tempNode = new Node(node);
            State tempState = tempNode.getState();
            int winOrLoss = checkGameResult(tempState.getPbs(), tempState.getPlayerTurn());
            int turnNumber = tempState.getPbs().getTurnNumber();
            boolean gameOutcome = tempState.getPbs().gameOver();

            while(!gameOutcome){
                PentagoMove move = (PentagoMove) tempState.getPbs().getRandomMove();
                tempState.getPbs().processMove(move);
                gameOutcome = tempState.getPbs().gameOver();
            }

            int gameResult = checkGameResult(tempState.getPbs(), tempState.getPlayerTurn());
            return gameResult;
        }


    } // MonteCarlo

    ////////////////////////////////////// PRIVATE UCT CLASS //////////////////////////////////////
    static class UCT {
        public static double uctValue(int sims, double winRate, int nodeVisits){
            if (nodeVisits == 0){
                return Integer.MAX_VALUE;
            }

            return (winRate / (double) nodeVisits) * (Math.sqrt(2 * Math.log(sims)/ (double) nodeVisits));
        }

        public static Node findBestUCTNode(Node node){
            int parentVisit = node.getState().getVisits();
            return Collections.max(node.getChildren(), Comparator.comparing(c ->
                uctValue(parentVisit, c.getState().getWinRate(), c.getState().getVisits())));
        }
    } // UCT

    ////////////////////////////////////// PRIVATE TREE CLASS //////////////////////////////////////
    static class Tree{
        Node root;


        public Tree(Node root){
            this.root = root;
        }

        public Node getRoot() {
            return root;
        }

        public void setRoot(Node root) {
            this.root = root;
        }

        public void addChild(Node parent, Node child){
            parent.getChildren().add(child);
        }
    } // Tree

    ////////////////////////////////////// PRIVATE NODE CLASS //////////////////////////////////////
    static class Node {
        State state;
        Node parent;
        ArrayList<Node> children;

        public Node(State state){
            this.state = state;
            children = new ArrayList<>();
        }

        public Node(State state, Node parent, ArrayList<Node> children){
            this.state = state;
            this.parent = parent;
            this.children = children;
        }

        public Node(Node node){
            this.children = new ArrayList<>();
            this.state = new State(node.getState().getPbs());
            if (node.getParent() != null){
                this.parent = node.getParent();
            }
            ArrayList<Node> children = node.getChildren();
            for (Node child : children){
                this.children.add(new Node(child));
            }
        }

        public State getState(){
            return this.state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public Node getParent(){
            return this.parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public ArrayList<Node> getChildren(){
            return this.children;
        }

        public void setChildren(ArrayList<Node> children) {
            this.children = children;
        }

        public Node getRandomChild(){
            int numChildren = this.children.size();
            int randomChild = (int) (Math.random() * numChildren);
            return this.children.get(randomChild);
        }

        public Node getBestChild(){
            return Collections.max(this.children, Comparator.comparing(c -> {
                return c.getState().getVisits();
            }));
        }
    } // Node class

    ////////////////////////////////////// PRIVATE STATE CLASS //////////////////////////////////////
    static class State {
        private PentagoBoardState pbs;
        private int playerTurn;
        private int visits;
        private double winRate;
        private ArrayList<PentagoMove> legalMoves;


        public State(PentagoBoardState pbs){
            this.pbs = pbs;
            this.legalMoves = pbs.getAllLegalMoves();
        }

        public PentagoBoardState getPbs(){
            return this.pbs;
        }

        public void setPbs(PentagoBoardState pbs){
            this.pbs = pbs;
        }

        public int getPlayerTurn(){
            return this.playerTurn;
        }

        public void setPlayerTurn(int playerTurn){
            this.playerTurn = playerTurn;
        }

        public int getVisits(){
            return this.visits;
        }

        public void setVisits(int visits) {
            this.visits = visits;
        }

        public double getWinRate() {
            return winRate;
        }

        public void setWinRate(double winRate) {
            this.winRate = winRate;
        }

        public ArrayList<PentagoMove> getLegalMoves(){
            return this.legalMoves;
        }

        public void setLegalMoves(ArrayList<PentagoMove> legalMoves) {
            this.legalMoves = legalMoves;
        }

        public void incrementVisits(){
            this.visits++;
        }

        public void addScore(double score){
            if (this.winRate != Integer.MIN_VALUE){
                this.winRate += score;
            }
        }
    } // State




} // MyTools