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
    public static final int SIM_TIME_LIMIT = 800;
    public static final int MOVE_TIME_LIMIT = 1920;
    public static int DEPTH = 2;
    public static final int INCREASE_DEPTH = 10;
    private static final UnaryOperator<PentagoCoord> getNextHorizontal = c -> new PentagoCoord(c.getX(), c.getY()+1);
    private static final UnaryOperator<PentagoCoord> getNextVertical = c -> new PentagoCoord(c.getX()+1, c.getY());
    private static final UnaryOperator<PentagoCoord> getNextDiagRight = c -> new PentagoCoord(c.getX()+1, c.getY()+1);
    private static final UnaryOperator<PentagoCoord> getNextDiagLeft = c -> new PentagoCoord(c.getX()+1, c.getY()-1);

    //////////////////////////// SEARCH ALGORITHMS ////////////////////////////

    /**
     * Function to find the best move using alpha-beta pruning or negamax, applied on an
     * ArrayList of moves that were filtered from running some MonteCarlo simulations
     * @param pbs: board state
     * @param studentTurn: tells us if student has the white or black pieces
     * @return the best move
     */
    public static PentagoMove findBestMove(PentagoBoardState pbs, int studentTurn){
        boolean isMaxPlayer = studentTurn == 0 ? true : false;
        HashMap<PentagoMove, Double> moveRankings = new HashMap<>();
        PentagoMove bestMove;
        long start = System.currentTimeMillis();

        ArrayList<PentagoMove> bestLegalMoves = pbs.getAllLegalMoves(); // TODO: Filter for best moves (MCTS)

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

    //////////////////////////// SAMPLING METHODS ////////////////////////////

    //////////////////////////// EVALUATION METHODS ////////////////////////////

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
    } // check Horizontal

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


        whiteScore = checkHorizontals(board, whitePlayer);
        blackScore = checkHorizontals(board, blackPlayer);
        cost = whiteScore - blackScore;
        return cost;
    } //getEvaluation


    //////////////////////////// HELPER METHODS ////////////////////////////

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