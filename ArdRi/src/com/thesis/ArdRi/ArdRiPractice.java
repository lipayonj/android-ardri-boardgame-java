package com.thesis.ArdRi;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.thesis.ArdRi.board.BoardView;
import com.thesis.ArdRi.models.Coordinate;
import com.thesis.ArdRi.models.GameStatus;
import com.thesis.ArdRi.models.Move;
import com.thesis.ArdRi.models.State;
import com.thesis.ArdRi.rules.GameRules;

import java.util.List;

/**
 * Created by jerwinlipayon on 2/16/15.
 */
public class ArdRiPractice extends Activity {

    // This is actual location where we store state of board. It is here because
    // of save instance state stuff and decoupling between view/controller and model.
    // So this activity is something like model.

    private State[][] positions = new State[BoardView.BOARD_SIZE][BoardView.BOARD_SIZE];
    // Game instance that is used mainly for calculating moves.
    private GameRules game = new GameRules();
    private boolean isFinish = false;
    // Instance of our view.
    private BoardView boardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_board_view);
        boardView = (BoardView) findViewById(R.id.boardView);

        // Initialize positions.
        GameRules.setEmptyValues(positions);
        GameRules.initializeStartPositions(positions);
        game.setPositions(positions);

        // Bind positions table to View values.
        boardView.setPositions(positions);
        boardView.setFocusable(true);
        boardView.setFocusableInTouchMode(true);
        boardView.setMoveStageListener(new CellSelected());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /*
     * OK, the real meat of game-flow control. It uses semaphore-like
     * poor design to control flow and it is crucially important to
     * preserve order of if-s and boolean fields checking.
     * Very, very poor design (dragons live here kind of design!) so:
     * TODO: Please refactor this I get pretty nasty poor design smell here!
     */
    private class CellSelected implements BoardView.MoveStageListener {

        // Semaphore-like boolean fields.
        private boolean isCompMove = false;
        private boolean isCompSelected = false;

        private boolean isHumanBallChange = false;
        private boolean isCompBallsChange = false;

        private boolean isCompVictory = false;
        private boolean isHumanVictory = false;

        public CellSelected(){
            boardView.highlightSquares( game.getMovablePieces(State.ATTACKER));
        }

        /*
         * React on user click on the board. If user clicks on her/his
         * ball then select that ball, of she/he select empty field then
         * move ball, else display error by displaying error animation
         * on that square.
         */
        public void userClick(int i, int j) {

            Coordinate humanSource = game.getMove().sourceMove;

            //Black Piece is selected
            if (!isFinish && positions[i][j] == State.ATTACKER
                    && !isCompMove ) {

                game.getMove().player = State.ATTACKER;

                // If we already selected ball and now we change our mind.
                if (humanSource.isNotEmpty()) {
                    positions[humanSource.i][humanSource.j] =State.ATTACKER;
                }



                boardView.highlightCells(game.getValidMovesForPiece(i, j,State.ATTACKER));
                humanSource.i = i;
                humanSource.j = j;

            } else if (!isFinish
                    && (positions[i][j] == State.DEFENDER  || positions[i][j] == State.KING)
                    && isCompMove ) {

                if(positions[i][j] == State.DEFENDER)
                    game.getMove().player = State.DEFENDER;
                else
                    game.getMove().player = State.KING;
                // If we already selected ball and now we change our mind.
                if (humanSource.isNotEmpty()) {

                    if(positions[humanSource.i][humanSource.j] == State.KING
                            && positions[i][j] == State.DEFENDER)
                        positions[humanSource.i][humanSource.j] = State.KING;
                    else if(positions[humanSource.i][humanSource.j] == State.KING
                            && positions[i][j] == State.KING)
                        positions[humanSource.i][humanSource.j] = State.KING;
                    else if(positions[humanSource.i][humanSource.j] == State.DEFENDER
                            && positions[i][j] == State.KING)
                        positions[humanSource.i][humanSource.j] = State.DEFENDER;
                    else
                        positions[humanSource.i][humanSource.j] = State.DEFENDER;
                }
                //boardView.selectBall(i, j, State.SELECTED);
                boardView.highlightCells( game.getValidMovesForPiece(i,j, game.getMove().player) );
                humanSource.i = i;
                humanSource.j = j;
                Log.d("Human Moves"," Human Piece Selected "+ game.getValidMovesForPiece(i, j,State.ATTACKER));
            }
            else if (!isFinish && humanSource.isNotEmpty() ) {
                game.getMove().destinationMove.i = i;
                game.getMove().destinationMove.j = j;
                if(game.isValidMove(game.getMove())) {
                    boardView.highlightCells(game.getValidMovesForPiece(i,j, game.getMove().player) );
                    //performSlide(game.getMove(), true); // naa pa Error
                    performMove(game.getMove(), true); // animate move

                    if (isCompMove)
                        isCompBallsChange = true;
                    else
                        isHumanBallChange = true;
                    Log.d("Moved", " Move Performed! ");
                }else{
                    boardView.highlightSquares(game.getMovablePieces(game.getMove().player));
                }
            } else {
                boardView.error(i, j);
                if(isCompMove)
                    isCompBallsChange = false;
                else
                    isHumanBallChange = false;
            }
        }

        /*
         * If animation is complete, then it is obvious we need to do something.
         * What will be done is decided by checking various boolean fields.
         */
        public void animationComplete() {
            // TODO: refactor:
            // Excessive conditional logic, must preserve conditions order of
            // conditions...bad, bad design. :(


            if (isCompVictory) {
                isCompVictory = false;
                // stop all activity
                isCompSelected = false;
            }
            if (isHumanVictory) {
                isHumanVictory = false;
                // stop all activity
                isCompSelected = false;
            }

            if (isCompBallsChange) {
                isCompBallsChange = false;
                isCompMove = false;
                List<Coordinate> tobeRemove =  game.performMove(game.getMove());
                performRemove(tobeRemove, game.getOpposingColour(game.getMove().player) , true);

                boardView.highlightSquares( game.getMovablePieces(State.ATTACKER));

                game.deleteMove();
                checkWin();

            }

            if (isHumanBallChange) {
                isHumanBallChange = false;
                isCompMove = true;
                List<Coordinate> tobeRemove =  game.performMove(game.getMove()); //
                performRemove(tobeRemove, game.getOpposingColour(game.getMove().player) , true);

                boardView.highlightSquares( game.getMovablePieces(State.DEFENDER));

                game.deleteMove();
                checkWin();
            }
        }

        private void checkWin() {
            if(game.getGameStatus() != GameStatus.CONTINUE){
                if(game.getGameStatus() == GameStatus.BLACK_WIN) {
                    isCompVictory = true;
                    isHumanVictory = false;
                    isFinish = true;
                    showToast("Attaker Wins");
                }else if(game.getGameStatus() == GameStatus.WHITE_WIN) {
                    isHumanVictory = true;
                    isCompVictory = false;
                    isFinish = true;
                    showToast("Defender Wins");
                }
            }
            if (!game.hasMoreMoves(State.ATTACKER)) {
                isHumanVictory = true;
                isCompVictory = false;
                isFinish = true;
                showToast("Defender Wins");
            }
            if (!game.hasMoreMoves(State.DEFENDER)) {
                isCompVictory = true;
                isHumanVictory = false;
                isFinish = true;
                showToast("Attaker Wins");
            }
        }
    }

    private void performRemove(List<Coordinate> pieces, State who, boolean withAnimation) {
        if (withAnimation)
            boardView.removeBall(pieces, who);
        for(Coordinate c : pieces){
            positions[c.i][c.j] = State.EMPTY;
        }
    }

    private void performMove(Move move, boolean withAnimation) {

        State who = move.player;
        if (withAnimation)
            boardView.moveBall(move, who);
        positions[move.sourceMove.i][move.sourceMove.j] = State.EMPTY;
        positions[move.destinationMove.i][move.destinationMove.j] = who;
    }
}
