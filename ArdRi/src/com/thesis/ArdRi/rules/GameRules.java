package com.thesis.ArdRi.rules;

import android.net.sip.SipSession;
import android.util.Log;
import com.thesis.ArdRi.board.BoardView;
import com.thesis.ArdRi.models.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Game specific methods and utilities for move calculation and others stuff. It
 * have several static methods, but there is also one important field "move"
 * that represent current move.
 */

public class GameRules {

	private Move move = new Move();
	private GameStatus gameStatus = GameStatus.CONTINUE;
	private State[][] positions;

	/** Direcions in which to check for captured pieces on after a piece is moved. */
	protected static final int[] CAPTURE_CHECK_DR = {1, 0, -1, 0};
	protected static final int[] CAPTURE_CHECK_DC = {0, 1, 0, -1};

	public void setPositions(State[][] positions) {
		gameStatus = GameStatus.CONTINUE;
		this.positions = positions;
	}

	public Move getMove() {
		return move;
	}

	public void setMove(Move move) {
		this.move = move;
	}

	public GameStatus getGameStatus() {
		return gameStatus;
	}

	public void deleteMove() {
		move.delete();
	}

	public static void initializeStartPositions(State[][] positions) {
		// set up the pieces
		for (int i = 1; i < BoardCfg.ARD_RI.length; i += 3) {
			if(BoardCfg.ARD_RI[i+2] == BoardCfg.KING)
				positions[ BoardCfg.ARD_RI[i]][ BoardCfg.ARD_RI[i+1]] = State.KING;
			else
				positions[ BoardCfg.ARD_RI[i]][ BoardCfg.ARD_RI[i+1]]
					= ( BoardCfg.ARD_RI[i+2] == BoardCfg.BLACK)?State.ATTACKER:State.DEFENDER;
		}
	}

	public static void setEmptyValues(State[][] positions) {
		for (int i = 0; i < BoardView.BOARD_SIZE; i++) {
			for (int j = 0; j < BoardView.BOARD_SIZE; j++) {
				positions[i][j] = State.EMPTY;
			}
		}
	}

	/**
	 * Returns true if the given colour has valid moves available, false if not.
	 */
	public boolean hasMoreMoves(State piece) {
		return getMovablePieces(piece).size() > 0;
	}

	/**
	 * Returns true if the given colour has valid moves available, false if not.
	 */
	public List<Coordinate> getMovablePieces(State piece) {
		List<Coordinate> validMoves = new ArrayList<Coordinate>();
		// check every piece of the given colour
		for(int i = 0; i < BoardView.BOARD_SIZE; i++){
			for(int j = 0; j < BoardView.BOARD_SIZE; j++){
				if(piece == State.DEFENDER && positions[i][j] == State.KING && isPieceMovable(i, j, State.KING)){
					validMoves.add(new Coordinate(i,j));
					continue;
				}
				if(positions[i][j] == piece && isPieceMovable(i, j, piece)){
					validMoves.add(new Coordinate(i,j));
				}
			}
		}
		Log.d("MOVABLE_PIECE", validMoves.toString());
		return validMoves;
	}


	public boolean isPieceMovable(int i, int j, State who){
		for (int dir = 0; dir < 4; dir++) {
			int r = i + CAPTURE_CHECK_DR[dir];
			int c = j + CAPTURE_CHECK_DC[dir];
			// find the neighbouring piece in this direction
			State neighbour = getStateAt(r, c);
			if(who == State.KING){
				if (neighbour == State.EMPTY && neighbour != State.INVALID ){
					return true;
				}
			}else if(who == State.ATTACKER || who == State.DEFENDER){
				if (neighbour == State.EMPTY && neighbour != State.INVALID && !isThrone(r, c)){
					return true;
				}
			}
		}
		return false;
	}

	public List<Coordinate> getValidMovesForPiece(int i, int j, State who){
		List<Coordinate> validMoves = new ArrayList<Coordinate>();

		// look for captured pieces
		for (int dir = 0; dir < 4; dir++) {
			int r = i + CAPTURE_CHECK_DR[dir];
			int c = j + CAPTURE_CHECK_DC[dir];

			// find the neighbouring piece in this direction
			State neighbour = getStateAt(r, c);

			if(who == State.KING){
				if (neighbour == State.EMPTY && neighbour != State.INVALID ){
					validMoves.add(new Coordinate(r,c));
				}
			}else{
				if (neighbour == State.EMPTY && neighbour != State.INVALID && !isThrone(r, c)){
					validMoves.add(new Coordinate(r,c));
				}
			}
		}
		return validMoves;
	}

	/*
	 * perform move, returning a list of captured pieces
	 */
	public List<Coordinate> performMove(Move move){

		List<Coordinate> capturedPieces = new ArrayList<Coordinate>();

		// white wins if the King successfully escapes to a corner tile
		if (move.player == State.KING && isEdgeOfBoard(move.destinationMove)) {
			gameStatus = GameStatus.WHITE_WIN;
			return capturedPieces;
		}

		// look for captured pieces
		for (int dir = 0; dir < 4; dir++) {

			int r = move.destinationMove.i + CAPTURE_CHECK_DR[dir];
			int c = move.destinationMove.j + CAPTURE_CHECK_DC[dir];

			// find the neighbouring piece in this direction
			State neighbour = getStateAt(r, c);
			Coordinate ncoor = new Coordinate(r,c);

			if (neighbour != State.EMPTY && neighbour != State.INVALID ) {
				if (neighbour == move.player) {
					// no sense comparing with ourselves; this space will be empty
					continue;

				} else if (neighbour == State.KING && move.player == State.ATTACKER) {
					// Kings require far more elaborate capturing moves
					if (isKingCapturable(ncoor, move.destinationMove)) { // check if the king is trapped or surrounded
						capturedPieces.add(ncoor);
						gameStatus = GameStatus.BLACK_WIN;
						break;
					}

					int rr = move.destinationMove.i - CAPTURE_CHECK_DR[dir];
					int rc = move.destinationMove.j - CAPTURE_CHECK_DC[dir];

					State neighbour2 = getStateAt(rr, rc);

					if(neighbour2 == State.DEFENDER){
						capturedPieces.add(move.destinationMove);
					}
					//check if the the king is checked

				} else if (neighbour == getOpposingColour(move.player)) {

					int farr = move.destinationMove.i + 2 * CAPTURE_CHECK_DR[dir];
					int farc = move.destinationMove.j + 2 * CAPTURE_CHECK_DC[dir];

					// we are beside an opposing piece
					State farneighbour = getStateAt(farr, farc);

					if(move.player == State.KING && farneighbour == State.DEFENDER){
						capturedPieces.add(ncoor);
						Log.d("KING"," KING KONG ");
					}

					if(farneighbour == move.player){
						capturedPieces.add(ncoor);
					}

					if( move.player == State.DEFENDER && farneighbour == State.KING ){
						capturedPieces.add(ncoor);
					} else if ( move.player == State.ATTACKER && farneighbour == move.player ) {
						capturedPieces.add(ncoor);
					}
				}
			}
		}
		return capturedPieces;
	}

	/**
	 * Returns true if the given king piece is in a capturable position when
	 * a black piece is moved to the given blocker position.
	 * <p>
	 * A king may only be captured by being surrounded. For the purposes of
	 * capture, we consider the throne to contribute in
	 * the same way as an opposing piece.
	 */
	protected boolean isKingCapturable(Coordinate kingCoor,  Coordinate blocker) {
		for (int dir = 0; dir < 4; dir++) {
			int r = kingCoor.i + CAPTURE_CHECK_DR[dir];
			int c = kingCoor.j + CAPTURE_CHECK_DC[dir];

			if (!isThrone(r, c) &&
					!(blocker.i == r && blocker.j == c) &&
					getStateAt(r, c) != State.ATTACKER &&
					getStateAt(r, c) != State.INVALID )
			{
				// the king is free on this side, and so is not capturable
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the piece of the opponent to the colour given.
	 */
	public State getOpposingColour(State player) {
		return (player == State.DEFENDER || player == State.KING) ? State.ATTACKER : State.DEFENDER;
	}

	/*
	 * Returns true if a board location is a valid move for the given piece.
	 */
	public boolean isValidMove(Move move){
		//check if selected destination move is in list of valid moves
		for(Coordinate c : getValidMovesForPiece(move.sourceMove.i, move.sourceMove.j, move.player)){
			if(move.destinationMove.i == c.i && move.destinationMove.j == c.j){
				Log.d("VALIDATE MOVE"," INVALID COOR: "+ c + " to " + move.destinationMove);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the given board position is on edge.
	 */
	public boolean isEdgeOfBoard(Coordinate loc) {
		return loc.j == 0 || loc.i == 0 ||
				loc.j == BoardView.BOARD_SIZE - 1
				|| loc.i == BoardView.BOARD_SIZE - 1;
	}

	public boolean isThrone(int i, int j) {
		return (i == BoardView.BOARD_SIZE / 2) &&
				(j == BoardView.BOARD_SIZE / 2);
	}

	/**
	 * Returns the piece at the given row and column location,
	 * State.EMPTY if the space is unoccupied, or State.INVALID if the location is invalid.
	 */
	protected State getStateAt(int i, int j) {
		if (i < 0 || i >= BoardView.BOARD_SIZE
				|| j < 0 || j >= BoardView.BOARD_SIZE) {
			return State.INVALID;
		} else {
			return positions[i][j];
		}
	}

}
