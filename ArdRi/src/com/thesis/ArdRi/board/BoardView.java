package com.thesis.ArdRi.board;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.thesis.ArdRi.R;
import com.thesis.ArdRi.models.Coordinate;
import com.thesis.ArdRi.models.Move;
import com.thesis.ArdRi.models.State;

import java.util.List;

/**
 * Implementation of board view/controller. It draws board, handle user events
 * (touch), rotating screen and animation.
 * @author Lipayon, Jerwin
 */

public class BoardView extends View {

	public static final int BOARD_MARGIN = 10;
	public static final int BOARD_SIZE = 7;
	public static final int GRID_SIZE = 2;

	private static final int MSG_ANIMATE = 0;

	private enum Slide{ VERTICAL, HORIZONTAL };

	private final Handler animationHandler = new Handler(new AnimationMessageHandler());

	private MoveStageListener moveStageListener;

	/**
	 * Listener interface that send messages to Activity. Activity then handle
	 * this messages.
	 */
	public interface MoveStageListener {
		// Fires when user click's somewhere on board.
		void userClick(int i, int j);
		// When animation complete at same current move stage is complete.
		void animationComplete();
	}

	public void setMoveStageListener(MoveStageListener selectionListener) {
		this.moveStageListener = selectionListener;
	}

	/**
	 * Animation interface that control animation handler.
	 */
	public interface Animation {
		// This is called on onDraw method.
		void animate(Canvas canvas);
		// Say if animation should end.
		boolean isFinish();
		// Control which cells will be animated and hence should be
		// ignored when we draw grid.
		boolean skip(int i, int j);
		// How much frames per second we will use for our animation.
		int fps();
		//Skip Animation
		void skipAnimation();
	}

	private Animation animation = new NullAnimation();

	// Here we store animation board state with all players and intermediate
	// states for cells.
	private State[][] positions;

	public void setPositions(State[][] positions) {
		this.positions = positions;
	}

	// Paint for board table line. It is here because onPaint is
	// using it several time per frame.
	private Paint boardLinePaint;

	// Width of board is also calculated dynamically when screen
	// size changes.
	private float boardWidth;

	// Maximum radius of ball - calculated dynamically also...
	private float maxRadius;

	private float borderWidth;

	// Can freely be here because it is calculated every time screen size changes.
	private float cellSize;

	public BoardView(final Context context, AttributeSet attrs) {
		super(context, attrs);
		requestFocus();
		boardLinePaint = new Paint();
	}

	/*
	 * Classic onDraw. It paints table and ball states. When we need to animate
	 * stuff we call it to refresh canvas state (easy as in classic Java 2D
	 * graphics animation).
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		boardLinePaint.setStrokeWidth(borderWidth);

		float offsetBoardWidth = boardWidth - BOARD_MARGIN;

		boardLinePaint.setColor(Color.LTGRAY);
		boardLinePaint.setStyle(Style.STROKE);
		boardLinePaint.setStyle(Style.FILL_AND_STROKE);

		canvas.drawRect(BOARD_MARGIN, BOARD_MARGIN, offsetBoardWidth, offsetBoardWidth, boardLinePaint);

		boardLinePaint.setColor(Color.WHITE);
		for (int i = 0; i <= BOARD_SIZE; i++) {
			float cellStep = BOARD_MARGIN + (i * cellSize);

			if(i == 3){ //highlight corners and king square
				drawKingSquare(canvas,3, 3,255);
			}

			canvas.drawLine(cellStep, BOARD_MARGIN, cellStep, offsetBoardWidth,
					boardLinePaint);
			canvas.drawLine(BOARD_MARGIN, cellStep, offsetBoardWidth, cellStep,
					boardLinePaint);
		}

		setValuesFromDatas(canvas);
		animation.animate(canvas);
	}

	/*
	 * Set values from board state structure and skip animated items.
	 */
	private void setValuesFromDatas(Canvas canvas) {
		for (int i = 1; i < BOARD_SIZE + 1; i++) {
			for (int j = 1; j < BOARD_SIZE + 1; j++) {
				// If this are currently animated squares, do not
				// draw them!
				if (!animation.skip(i - 1, j - 1)) {
					drawBall(i, j, positions[i - 1][j - 1], maxRadius, canvas, 255);
				}
			}
		}
	}
	/*
	* Method for drawing Kings square.
	* It is stupid to create Paint object every time, but it is here
	* for readability and encapsulation reasons.
	*/
	private void drawKingSquare(Canvas canvas, int i, int j, int alpha) {
		Paint paint = new Paint();
		paint.setColor(Color.rgb(210,180,140));
		paint.setStyle(Style.FILL);
		paint.setAlpha(alpha);
		drawCustomRect(i, j, canvas, paint, 0);
	}

	/*
	 * Method for drawing filled square (when user touch inappropriate section
	 * of table). It is stupid to create Paint object every time, but it is here
	 * for readability and encapsulation reasons.
	 */
	private void drawWhiteSquare(Canvas canvas, int i, int j, int alpha) {
		Paint paint = new Paint();
		paint.setColor(Color.rgb(235, 235, 235));
		paint.setStyle(Style.FILL);
		paint.setAlpha(alpha);
		drawCustomRect(i, j, canvas, paint, 0);
	}

	private void drawErrorSquare(Canvas canvas, int i, int j, int alpha) {
		Paint paint = new Paint();
		paint.setColor(Color.LTGRAY);
		paint.setStyle(Style.FILL);
		paint.setAlpha(alpha);
		drawCustomRect(i, j, canvas, paint, 0);
	}

	private void drawCustomRect(int i, int j, Canvas canvas, Paint paint,
			float shrink) {
		canvas.drawRect(i * cellSize + GRID_SIZE + BOARD_MARGIN + shrink, j
				* cellSize + GRID_SIZE + BOARD_MARGIN + shrink, (i + 1)
				* cellSize - GRID_SIZE + BOARD_MARGIN - shrink, (j + 1)
				* cellSize + BOARD_MARGIN - GRID_SIZE - shrink, paint);
	}

	/*
	 * Draw custom balls. We can change balls alpha and radius in animation.
	 */
	private void drawCustomPosBall(float i, float j, State who, Canvas canvas) {

		// Calculate where we will put ball in our grid based on coordinates in
		// grid.
		float x = cellSize * i + cellSize / 2 + BOARD_MARGIN;
		float y = cellSize * j + cellSize / 2 + BOARD_MARGIN;
		// Skip empty every time.
		if (who != State.EMPTY) {


			Paint smallBall = new Paint();

			int color = Color.BLACK;

			if (who == State.SELECTED)
				color = Color.rgb(255,128,0);
			else if (who == State.DEFENDER)
				color = Color.WHITE;
			else if(who == State.KING) {
				color = Color.rgb (169,169,169);
			}

			smallBall.setColor(color);
			smallBall.setStyle(Style.FILL);
			smallBall.setAlpha(255);

			Paint bigBall = new Paint();
			bigBall.setColor(Color.GRAY);
			bigBall.setStyle(Style.FILL);
			bigBall.setAlpha(255);

			// Smaller ball is 15% smaller than bigger.
			canvas.drawCircle(x, y, maxRadius * 1.15f, bigBall);
			canvas.drawCircle(x, y, maxRadius, smallBall);

		}
	}

	/*
	 * Draw custom balls. We can change balls alpha and radius in animation.
	 */
	private void drawBall(int i, int j, State who, float radius, Canvas canvas,
			int alpha) {

		// Calculate where we will put ball in our grid based on coordinates in
		// grid.
		float x = cellSize * (i - 1) + cellSize / 2 + BOARD_MARGIN;
		float y = cellSize * (j - 1) + cellSize / 2 + BOARD_MARGIN;
		// Skip empty every time.
		if (who != State.EMPTY) {
			Paint smallBall = new Paint();

			int color = Color.BLACK;

			if (who == State.SELECTED)
				color = Color.rgb(255,128,0);
			else if (who == State.DEFENDER)
				color = Color.WHITE;
			else if(who == State.KING)
				color = Color.rgb (169,169,169);

			smallBall.setColor(color);
			smallBall.setStyle(Style.FILL);
			smallBall.setAlpha(alpha);

			Paint bigBall = new Paint();
			bigBall.setColor(Color.GRAY);
			bigBall.setStyle(Style.FILL);
			bigBall.setAlpha(alpha);

			// Smaller ball is 15% smaller than bigger.
			canvas.drawCircle(x, y, radius * 1.15f, bigBall);
			canvas.drawCircle(x, y, radius, smallBall);

		}
	}

	/*
	 * Highlight Valid Moves
	 */
	public void highlightCells(List<Coordinate> list) {
		animation = new DrawSquareAnimation();
		DrawSquareAnimation highlightCellsAnimation = (DrawSquareAnimation) animation;
		highlightCellsAnimation.coordinates = list;
		highlightCellsAnimation.alpha = 80;
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	/*
	 * Paint square in white block operation (along with alpha animation) when
	 * user perform illegal move.
	 */
	public void error(int i, int j) {
		animation = new FillSquareAnimation();
		FillSquareAnimation fillSquareAnimation = (FillSquareAnimation) animation;
		fillSquareAnimation.i = i;
		fillSquareAnimation.j = j;
		fillSquareAnimation.alpha = 255;
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	/*
	 * Move ball from one place to another operation (with animation also).
	 */
	public void moveBall(Move move, State who) {
		animation = new MoveBallsAnimation();
		MoveBallsAnimation createBallAnimation = (MoveBallsAnimation) animation;
		createBallAnimation.radius = maxRadius;
		createBallAnimation.move = move;
		createBallAnimation.who = who;
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	public void noAction() {
		animation = new NullAnimation();
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	/*
	 * Remove ball when captured.
	 */
	public void removeBall(List<Coordinate> pieces, State who) {
		animation = new RemoveBallAnimation();
		RemoveBallAnimation removeBallAnimation = (RemoveBallAnimation) animation;
		removeBallAnimation.coordinates = pieces;
		removeBallAnimation.radius = maxRadius;
		removeBallAnimation.who = who;
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	/*
	 * Highlight Cells
	 */
	public void highlightSquares(List<Coordinate> pieces) {
		animation = new DrawSquareAndBallAnimation();
		DrawSquareAndBallAnimation squareAndBallAnimation = (DrawSquareAndBallAnimation) animation;
		squareAndBallAnimation.coordinates = pieces;
		squareAndBallAnimation.alpha = 240;
		animationHandler.sendEmptyMessage(MSG_ANIMATE);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		animation.skipAnimation();
		if (animation.isFinish()) {
			int action = event.getAction();

			int i = (int) ((event.getX() - BOARD_MARGIN) / cellSize);
			int j = (int) ((event.getY() - BOARD_MARGIN) / cellSize);

			if (i >= 0 && i <= (BOARD_SIZE - 1) && j >= 0 && j <= (BOARD_SIZE - 1)) {

				// If user just click, then we will show painted square.
				if (action == MotionEvent.ACTION_DOWN) {
					moveStageListener.userClick(i, j);
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Recalculate fields based on current screen size.
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		boardWidth = w < h ? w : h;
		cellSize = (boardWidth - GRID_SIZE * BOARD_MARGIN) / BOARD_SIZE;
		maxRadius = cellSize * 0.68f / 2;
		borderWidth = cellSize * 0.05f;
	}

	/*
	 * Set dimension of current view.
	 */
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
		setMeasuredDimension(d, d);
	}

	/**
	 * Inner animation handler. This handler call itself several times during
	 * animation and in every pass invalidates current view (calls onDraw method
	 * of View). It is controlled by Animation interface and hence concrete
	 * implementation of Animation interface. This implementation "tells" it
	 * when to stop.
	 */
	private class AnimationMessageHandler implements Callback {
		public boolean handleMessage(Message msg) {
			if (msg.what == MSG_ANIMATE) {
				BoardView.this.invalidate();
				if (!animationHandler.hasMessages(MSG_ANIMATE)) {
					if (animation.isFinish()) {
						animationHandler.removeMessages(MSG_ANIMATE);
						moveStageListener.animationComplete();
					} else {
						animationHandler.sendEmptyMessageDelayed(MSG_ANIMATE,
								animation.fps());
					}
				}
				return true;
			}
			return false;
		}
	}

	/**
	 * This animation doesn't do anything - null animation.
	 */
	private class NullAnimation implements Animation {
		public void animate(Canvas canvas) {
			// do nothing
		}

		public boolean isFinish() {
			return true;
		}

		public boolean skip(int i, int j) {
			return false;
		}

		public int fps() {
			return 1000 / 1;
		}

		@Override
		public void skipAnimation() {}
	}

	/**
	 * Remove ball animation (balls pops-up up in empty square).
	 */
	private class ShowBallAnimation implements Animation {

		public State who;
		public float radius;

		public List<Coordinate> coordinates = null;

		public void animate(Canvas canvas) {

			for(Coordinate c: coordinates){
				drawBall(c.i + 1 , c.j + 1, positions[c.i][c.j], radius, canvas, 255);
			}
			radius += 8;
			if (radius >= maxRadius) {
				radius = maxRadius;
			}
		}

		public boolean isFinish() {
			return radius >= maxRadius;
		}

		public boolean skip(int i, int j) {
			return false;//this.i == i && this.j == j;
		}

		public int fps() {
			return 1000 / 40;
		}

		@Override
		public void skipAnimation() {
			radius = maxRadius;
		}
	}

	/**
	 * Remove ball animation (balls pops-up up in empty square).
	 */
	private class RemoveBallAnimation implements Animation {
		
		public State who;
		public float radius;

		public List<Coordinate> coordinates = null;

		public void animate(Canvas canvas) {

			for(Coordinate c: coordinates){
				//i = c.i;
				//j = c.j;
				drawBall(c.i + 1 , c.j + 1, who, radius, canvas, 255);
			}
			radius -= 8;
			if (radius <= 0) {
				radius = 0;
			}
		}

		public boolean isFinish() {
			return radius <= 0;
		}

		public boolean skip(int i, int j) {
			return false;//this.i == i && this.j == j;
		}

		public int fps() {
			return 1000 / 40;
		}

		@Override
		public void skipAnimation() {
			radius = 0;
		}
	}

	/**
	 * Move ball animation that moves current ball from one square to another
	 * altogether with pop-ing-up effect. :) It can be use for one ball or ball
	 * set (represented by coordinate matrix).
	 */
	private class MoveBallsAnimation implements Animation {

		public Move move;
		public State who;
		public float radius;

		public boolean firstPhaseFinish;
		public boolean secondPhaseFinish;

		public void animate(Canvas canvas) {
			if (!firstPhaseFinish) {
				drawBall(move.sourceMove.i + 1, move.sourceMove.j + 1, who, radius, canvas, 255);
				radius -= 8;
				if (radius <= 0) {
					radius = 0;
					firstPhaseFinish = true;
				}
			} else {
				drawBall(move.destinationMove.i + 1, move.destinationMove.j + 1, who, radius, canvas, 255);
				radius += 8;
				if (radius >= maxRadius) {
					radius = maxRadius;
					secondPhaseFinish = true;
				}
			}
		}

		public boolean isFinish() {
			return firstPhaseFinish && secondPhaseFinish;
		}

		public boolean skip(int i, int j) {
			return (this.move.sourceMove.i == i && this.move.sourceMove.j == j)
					|| (this.move.destinationMove.i == i && this.move.destinationMove.j == j);
		}

		public int fps() {
			return 1000 / 32;
		}

		@Override
		public void skipAnimation() {
			if(firstPhaseFinish){
				radius = 0;
			}else{
				radius = maxRadius;
			}
		}
	}


	/**
	 * Paint square with white gradually disappeared white inner square.
	 */
	private class FillSquareAnimation implements Animation {
		public int i;
		public int j;
		public int alpha;
		public void animate(Canvas canvas) {
			drawErrorSquare(canvas, i, j, alpha);
			alpha -= 10;
			if (alpha <= 0)
				alpha = 0;
		}
		public boolean isFinish() {
			return alpha <= 0;
		}

		public boolean skip(int i, int j) { return false; }

		public int fps() {
			return 1000 / 16;
		}
		@Override
		public void skipAnimation() {
			alpha = 0;
		}
	}

	/**
	 * Paint square and Piece.
	 */
	private class DrawSquareAndBallAnimation implements Animation {

		public List<Coordinate> coordinates = null;
		public int alpha;

		public void animate(Canvas canvas) {
			for(Coordinate c: coordinates){
				drawWhiteSquare(canvas, c.i, c.j, alpha);
				drawCustomPosBall(c.i, c.j, positions[c.i][c.j], canvas);
			}
		}

		public boolean isFinish() {
			return true;
		}

		public boolean skip(int i, int j) {
			return false;
		}

		public int fps() {
			return 1000 / 1;
		}

		@Override
		public void skipAnimation() { }
	}

	/**
	 * Paint square with white gradually disappeared white inner square.
	 */
	private class DrawSquareAnimation implements Animation {

		public List<Coordinate> coordinates = null;
		public int alpha;

		public void animate(Canvas canvas) {

			for(Coordinate c: coordinates){
				drawWhiteSquare(canvas, c.i, c.j, alpha);
			}
			alpha += 8;
			if (alpha >= 255) {
				alpha = 255;
			}
		}

		public boolean isFinish() {
			return alpha >= 255;
		}

		public boolean skip(int i, int j) {
			return false;
		}

		public int fps() {
			return 1000 / 50;
		}

		@Override
		public void skipAnimation() {
			alpha = 255;
		}
	}
}