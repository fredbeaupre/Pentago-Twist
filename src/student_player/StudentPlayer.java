package student_player;

import boardgame.Move;

import pentago_twist.PentagoPlayer;
import pentago_twist.PentagoBoardState;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("AlphaBeta");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {

        Move myMove;
        PentagoBoardState pbs = MyTools.cloneBoard(boardState);
        int studentTurn = pbs.getTurnPlayer();
        int turnNumber = pbs.getTurnNumber();

        if (turnNumber < 3){
            myMove = MyTools.firstThreeMoves(pbs, studentTurn, turnNumber);
        } else {
            MyTools.DEPTH = 3;

            if (turnNumber > MyTools.INCREASE_DEPTH){
                MyTools.DEPTH = 4;
            }

            if (turnNumber > 2*MyTools.INCREASE_DEPTH){
                MyTools.DEPTH = 5;
            }

            myMove = MyTools.findBestMove(pbs, studentTurn);

        }


        // Return your move to be processed by the server.
        return myMove;
    }
}