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
    public static PentagoMove findBestMove(PentagoBoardState pbs, int studentTurn){
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

    public static int negamax(int studentTurn, PentagoBoardState pbs, int depth, int alpha, int beta){
        if (depth == 0 || pbs.gameOver()){
            return getEvaluation(pbs);
        }
        int bestValue = Integer.MIN_VALUE;
        ArrayList<PentagoMove> legalMoves = pbs.getAllLegalMoves();

        for (PentagoMove move : legalMoves){
            PentagoBoardState cloneState = cloneBoard(pbs);
            cloneState.processMove(move);
            int value = negamax(studentTurn, cloneState, depth - 1, -1*beta, -1*alpha);
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
    public static int checkHorizontals(Piece[][] board, Piece color){
        int pairs = 0;
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
    }

    public static int getEvaluation(PentagoBoardState pbs){
        return 0;
    }


    //////////////////////////// HELPER METHODS ////////////////////////////

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
    }






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