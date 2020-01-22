package com.thesis.ArdRi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.thesis.ArdRi.bluetooth.*;
import com.thesis.ArdRi.bluetooth.messages.*;
import com.thesis.ArdRi.board.BoardView;
import com.thesis.ArdRi.models.Coordinate;
import com.thesis.ArdRi.models.GameStatus;
import com.thesis.ArdRi.models.Move;
import com.thesis.ArdRi.models.State;
import com.thesis.ArdRi.rules.GameRules;

import java.util.List;

/**
 * Created by jerwinlipayon on 2/17/15.
 */
public class ArdRiBluetooth extends Activity {

    private boolean server = false;

    private static final String TAG = "ArdRiBluetoothActivity";

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    public final static String PLAY_MODE = "isServerOrNot";

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Name of the device connecting to
     */
    private String mConnectingDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mService = null;

    private Handler mHandler = null;

    // This is actual location where we store state of board. It is here because
    // of save instance state stuff and decoupling between view/controller and model.
    // So this activity is something like model.

    private State[][] positions = new State[BoardView.BOARD_SIZE][BoardView.BOARD_SIZE];

    // Game instance that is used mainly for calculating moves.
    private GameRules game = new GameRules();

    // Instance of our view.
    private BoardView boardView;

    private TextView time;

    private int timeLimit = 30; // in seconds

    private Handler timerHandler = new Handler();

    private Dialog chatDialog = null;

    private ArrayAdapter<String> mConversationArrayAdapter = null;

    private EditText txtMessage = null;

    private boolean gameOver = true;

    private ViewGroup notificationView;

    private ViewGroup boardFrame;
    private ViewGroup selectPiece;
    private View mLoadingView;
    private View mContentView;

    private State yourPiece = null;

    /**
     * The system "short" animation time duration, in milliseconds. This duration is ideal for
     * subtle animations or animations that occur very frequently.
     */
    private int mShortAnimationDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        server = getIntent().getExtras().getBoolean(PLAY_MODE);

        setContentView(R.layout.multiplayer_board_view);
        boardView = (BoardView) findViewById(R.id.boardView);
        boardView.setMoveStageListener(new CellSelected());

        time = (TextView) findViewById(R.id.time);
        notificationView = (ViewGroup) findViewById(R.id.notification_container);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        setupGame();

        findViewById(R.id.btnRestart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOver = false;
                sendMessage(new RestartGame());
            }
        });

        boardFrame = (ViewGroup) findViewById(R.id.board_frame);
        selectPiece = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.select_piece_layout, boardFrame, false);

        serverSelectPiece();
        setupChat();

        if(server){
            findViewById(R.id.btnMakeDisco).setVisibility(View.VISIBLE);
            findViewById(R.id.btnMakeDisco).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ensureDiscoverable();
                }
            });
        }

    }

    private void serverSelectPiece() {

        updateTurnIndicator("Select A Piece");

        mLoadingView = selectPiece.findViewById(R.id.select_loading_llayout);
        mContentView = selectPiece.findViewById(R.id.select_piece_view);

        selectPiece.findViewById(R.id.select_attacker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new ServerSelectPiece(State.ATTACKER));
            }
        });
        selectPiece.findViewById(R.id.select_defender).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new ServerSelectPiece(State.DEFENDER));
            }
        });
        showSelectPieceLayout();
    }

    private void showSelectPieceLayout() {
        boardFrame.addView(selectPiece);
    }

    private void removeSelectPieceLayout() {
        boardFrame.removeView(selectPiece);
    }

    private void showLoadingOrSelectPiece(boolean contentLoaded) {

        Log.d(TAG, "showLoadingOrSelectPiece(boolean contentLoaded)");
        // Decide which view to hide and which to show.
        final View showView = contentLoaded ? mContentView : mLoadingView;
        final View hideView = contentLoaded ? mLoadingView : mContentView;

        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);
        showView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        hideView.animate().alpha(0f).setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    private void showDrawtBtn(boolean contentLoaded) {

        Log.d(TAG, "showDraworRestartBtn(boolean contentLoaded)");

        // Decide which view to hide and which to show.

        Button draw = (Button) findViewById(R.id.btnDraw);
        Button restart = (Button) findViewById(R.id.btnRestart);

        final View showView = contentLoaded ? draw : restart;
        final View hideView = contentLoaded ? restart : draw;

        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);
        showView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        hideView.animate().alpha(0f).setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    public void updateTurnIndicator(String msg){
        ((TextView) findViewById(R.id.turn_label)).setText(msg);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupGame() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mService == null) {
            setUpBluetoothService();
            if (!server) {
                connectDevice(getIntent(), true);
            }
        }
    }

    @Override
    protected void onPause() {
        if (!gameOver) {
            sendMessage(new IdleMode());
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!gameOver) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(ArdRiBluetooth.this);
            builder.setTitle("Quit Game");
            builder.setMessage("Do you want To Quit the Game? ");
            // Add the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    gameOver = true;
                    finish();
                    System.exit(0);
                    ArdRiBluetooth.super.onBackPressed();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    return;
                }
            });
            builder.show();
        } else {
            //gameOver = true;
            finish();
            System.exit(0);
            ArdRiBluetooth.super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {

        if (mService != null) {
            mService.stop();
        }
        mService = null;
        chatDialog.dismiss();

        //android.os.Process.killProcess(android.os.Process.myPid());
        //finish();
        //System.exit(0);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mService != null && server) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mService.start();
            }
        }
        if (!gameOver) {
            sendMessage(new ResumeGame());
        }
        super.onResume();
    }

    private void setupGame() {
        // Initialize positions.
        GameRules.setEmptyValues(positions);
        boardView.setPositions(positions);
    }

    private void setUpBluetoothService() {
        // Initialize the BluetoothService to perform bluetooth connection
        mService = new BluetoothService(ArdRiBluetooth.this, mHandler);
    }

    private void updateLoadingLabel(CharSequence msg) {
        ((TextView) selectPiece.findViewById(R.id.select_piece_loading_label)).setText(msg);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        chatDialog = new Dialog(ArdRiBluetooth.this, R.style.Theme_CustomTranslucent);
        chatDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        chatDialog.setContentView(R.layout.chat_view_layout);

        findViewById(R.id.btnChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatDialog.show();
            }
        });

        chatDialog.findViewById(R.id.closeChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatDialog.hide();
            }
        });

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(ArdRiBluetooth.this, R.layout.message);

        ((ListView) chatDialog.findViewById(R.id.chatConversation)).setAdapter(mConversationArrayAdapter);

        txtMessage = (EditText) chatDialog.findViewById(R.id.chatMsg);

        // Initialize the send button with a listener that for click events
        (chatDialog.findViewById(R.id.chatSendMsg)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = new String(txtMessage.getText().toString());
                sendMessage(message);
            }
        });

        chatDialog.findViewById(R.id.chat_linear_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatDialog.isShowing())
                    chatDialog.hide();
            }
        });
    }

    private void notifyMessage(CharSequence ntype, CharSequence msg) {
        final ViewGroup notification = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.notification_layout, notificationView, false);
        ((TextView) notification.findViewById(R.id.notification_type)).setText(ntype);
        ((TextView) notification.findViewById(R.id.notification_message)).setText(msg);

        notification.findViewById(R.id.close_notification).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationView.removeView(notification);
            }
        });
        Runnable mRunnable;
        Handler nHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                notificationView.removeView(notification);
            }
        };
        nHandler.postDelayed(mRunnable, 3000);
        notificationView.addView(notification);
    }

    private void notifyMessage(CharSequence ntype, int msg) {
        final ViewGroup notification = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.notification_layout, notificationView, false);
        ((TextView) notification.findViewById(R.id.notification_type)).setText(ntype);
        ((TextView) notification.findViewById(R.id.notification_message)).setText(msg);

        notification.findViewById(R.id.close_notification).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationView.removeView(notification);
            }
        });
        Runnable mRunnable;
        Handler mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                notificationView.removeView(notification);
            }
        };

        mHandler.postDelayed(mRunnable, 3000);
        notificationView.addView(notification);
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
        private boolean isAttackerMove = false;
        private boolean isAttckerSelected = false;

        private boolean isDefenderMoved = false;
        private boolean isAttackerMoved = false;

        private boolean isAttackerVictory = false;
        private boolean isDefenderVictory = false;

        private boolean removePerformed = false;
        private boolean isYourTurn = false;

        private Runnable runTimer = new Runnable() {
            @Override
            public void run() {
                time.setText("" + (Integer.parseInt(time.getText().toString()) - 1));
                if (Integer.parseInt(time.getText().toString()) == 0) {
                    timeIsUp();
                } else {
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        public CellSelected() {
            /**
             * The Handler that gets information back from the BluetoothService
             */
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    server = getIntent().getExtras().getBoolean(PLAY_MODE);
                    switch (msg.what) {
                        case Constants.MESSAGE_STATE_CHANGE:
                            switch (msg.arg1) {
                                case BluetoothService.STATE_CONNECTED:
                                    if (server) {
                                        showLoadingOrSelectPiece(true);  // handle select piece
                                    } else {
                                        showLoadingOrSelectPiece(false);
                                        updateLoadingLabel("Server is Picking A Piece");
                                    }
                                    break;
                                case BluetoothService.STATE_CONNECTING:
                                    showLoadingOrSelectPiece(false);
                                    updateLoadingLabel(getString(R.string.title_connecting, mConnectingDeviceName));
                                    break;
                                case BluetoothService.STATE_LISTEN:
                                    updateLoadingLabel("Waiting For Someone to Connect");
                                    showLoadingOrSelectPiece(false);
                                    break;
                                case BluetoothService.STATE_NONE:
                                    notifyMessage("", R.string.title_not_connected);
                                    break;
                            }
                            break;
                        case Constants.MESSAGE_DEVICE_NAME_CONNECTED_TO:
                            // save the connected device's name
                            mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                            notifyMessage("Connection Notice", "Connected to " + mConnectedDeviceName);
                            ((TextView) chatDialog.findViewById(R.id.chat_connected_to)).setText(mConnectedDeviceName);
                            break;
                        case Constants.MESSAGE_DEVICE_NAME_CONNECTING_TO:
                            mConnectingDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                            updateLoadingLabel("Connecting to " + mConnectingDeviceName);
                            break;
                        case Constants.MESSAGE_TOAST:
                            Toast.makeText(ArdRiBluetooth.this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                            gameOver = true;
                            finish();
                            System.exit(0);
                            break;
                        case Constants.MESSAGE_WRITE:
                            resetTimer();
                            byte[] writeBuf = (byte[]) msg.obj;
                            // construct a string from the buffer
                            Object yourMessage = StreamUtils.toObject(writeBuf);

                            if (yourMessage instanceof Move) {
                                Move urMove = (Move) yourMessage;
                                isYourTurn = false;
                                if (yourPiece == State.ATTACKER) {
                                    isAttackerMoved = true;
                                    updateTurnIndicator("Defender on the Move");
                                } else if (yourPiece == State.DEFENDER) {
                                    isDefenderMoved = true;
                                    updateTurnIndicator("Attacker on the Move");
                                }
                                performMove(urMove, true); // animate move
                                Log.d("Moved", " Sent Move Performed! ");
                            } else if (yourMessage instanceof ServerSelectPiece) {
                                yourPiece = ((ServerSelectPiece) yourMessage).piece;
                                startGame();
                            } else if (yourMessage instanceof String) {
                                mConversationArrayAdapter.add("Me:  " + yourMessage);
                                txtMessage.setText("");
                            } else if (yourMessage instanceof IdleMode) {
                                pauseGame();
                            } else if (yourMessage instanceof ResumeGame) {
                                resumeGame();
                            } else if (yourMessage instanceof DrawGame) {
                                switch (((DrawGame) yourMessage).type) {
                                    case ASKDRAW:
                                        askDraw();
                                        break;
                                    case DODRAW:
                                        drawGame(true);
                                        break;
                                    case REFUSEDRAW:
                                        drawGame(false);
                                        break;
                                }
                            } else if (yourMessage instanceof RestartGame) {
                                restartGame();
                            }
                            break;
                        case Constants.MESSAGE_READ:
                            byte[] readBuf = (byte[]) msg.obj;
                            Object message = StreamUtils.toObject(readBuf);
                            if (message instanceof Move) {
                                Move remoteMove = (Move) message;
                                isYourTurn = true;

                                if (remoteMove.player == State.ATTACKER) {
                                    isAttackerMoved = true;
                                    updateTurnIndicator("Your Turn to Defend");
                                } else if (remoteMove.player == State.DEFENDER || remoteMove.player == State.KING) {
                                    isDefenderMoved = true;
                                    updateTurnIndicator("Your Turn to Attack");
                                }

                                performMove(remoteMove, true); // animate move
                                game.setMove(remoteMove);

                                startTimer();
                                Log.d("Moved", "Received Move Performed! ");
                            } else if (message instanceof ServerSelectPiece) {
                                yourPiece = game.getOpposingColour(((ServerSelectPiece) message).piece);
                                startGame();
                            } else if (message instanceof String) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + message);
                                notifyMessage("Message From " + mConnectedDeviceName, message.toString());
                            } else if (message instanceof IdleMode) {
                                pauseGame();
                            } else if (message instanceof ResumeGame) {
                                resumeGame();
                            } else if (message instanceof DrawGame) {
                                switch (((DrawGame) message).type) {
                                    case ASKDRAW:
                                        askForADraw();
                                        break;
                                    case DODRAW:
                                        drawGame(true);
                                        break;
                                    case REFUSEDRAW:
                                        drawGame(false);
                                        break;
                                }
                            }
                            break;
                    }
                }
            };
        }

        private void restartGame() {
            setupGame();
            showSelectPieceLayout();
            if (server) {
                showLoadingOrSelectPiece(true);
            } else {
                setupGame();
                showLoadingOrSelectPiece(false);
                updateLoadingLabel("Server is Picking A Piece");
            }
        }

        private void startGame() {

            boardView.setFocusable(true);
            boardView.setFocusableInTouchMode(true);

            // Initialize positions.
            GameRules.setEmptyValues(positions);

            //setup piece positions
            GameRules.initializeStartPositions(positions);

            game.setPositions(positions);

            boardView.setPositions(positions);

            boardView.invalidate(); //repaint board

            removeSelectPieceLayout();

            gameOver = false;
            isYourTurn = (yourPiece == State.ATTACKER); //Attacker moves First
            if (isYourTurn) {
                startTimer();
                updateTurnIndicator("Your Turn To Attack");
            }else{
                updateTurnIndicator("Attacker on the Move");
            }

            showDrawtBtn(true);

            findViewById(R.id.btnDraw).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessage(new DrawGame(DrawType.ASKDRAW));
                }
            });
        }

        private void drawGame(boolean doDraw) {
            if (doDraw) {
                gameOver = true;
                findViewById(R.id.btnDraw).setOnClickListener(null);
                removeSelectPieceLayout();
                updateTurnIndicator("The Game is DRAW");

                showDrawtBtn(false);

            } else {
                removeSelectPieceLayout();
                notifyMessage("", mConnectedDeviceName+" Refuse to Draw");
            }
        }

        private void resumeGame() {
            boardView.setFocusable(true);
            boardView.setFocusableInTouchMode(true);
            removeSelectPieceLayout();
            if (isYourTurn)
                resumeTimer();
        }

        private void pauseGame() {
            pauseTimer();
            serverSelectPiece();

            boardView.setFocusable(false);
            boardView.setFocusableInTouchMode(false);

            showLoadingOrSelectPiece(false);
            updateLoadingLabel(game.getOpposingColour(yourPiece).toString()
                    + " is in IDLE Mode");
        }

        private void askDraw() {
            serverSelectPiece();

            boardView.setFocusable(false);
            boardView.setFocusableInTouchMode(false);

            showLoadingOrSelectPiece(false);
            updateLoadingLabel("Asking Draw to the " + game.getOpposingColour(yourPiece).toString());

            pauseTimer();
        }

        private void askForADraw() {
            pauseTimer();
            final AlertDialog.Builder builder = new AlertDialog.Builder(ArdRiBluetooth.this);
            builder.setTitle("Ask For Draw");
            builder.setMessage(mConnectedDeviceName + " Wants to Ask For as DRAW. ");
            // Add the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendMessage(new DrawGame(DrawType.DODRAW));
                }
            });
            builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendMessage(new DrawGame(DrawType.REFUSEDRAW));
                }
            });
            builder.show();
        }


        private void startTimer() {
            resetTimer();
            timerHandler.postDelayed(runTimer, 1000);
        }

        private void pauseTimer() {
            timerHandler.removeCallbacks(runTimer);
        }

        private void resumeTimer() {
            timerHandler.postDelayed(runTimer, 1000);
        }

        private void timeIsUp() {
            if (!gameOver) {
                resetTimer();
                Move emptyMove = new Move();
                emptyMove.player = yourPiece;
                notifyMessage("", "Your time is Up!");
                sendMessage(emptyMove);
            }
        }

        private void resetTimer() {
            timerHandler.removeCallbacks(runTimer);
            if(gameOver)
                time.setText("00");
            else
                time.setText(timeLimit + "");
        }

        /**
         * React on user click on the board. If user clicks on her/his
         * ball then select that piece, of she/he select empty field then
         * move ball, else display error by displaying error animation
         * on that square.
         */
        public void userClick(int i, int j) {

            if(gameOver){
                return;
            }

            if (mService.getState() != BluetoothService.STATE_CONNECTED) {
                notifyMessage("Connection Notice", R.string.title_not_connected);
                return;
            }

            if (!isYourTurn) {
                notifyMessage("", getString(R.string.not_you_turn, mConnectedDeviceName));
                return;
            }

            Coordinate humanSource = game.getMove().sourceMove;

            //Your Piece is selected
            if (yourPiece == State.ATTACKER && positions[i][j] == yourPiece) {
                game.getMove().player = State.ATTACKER;
                boardView.highlightCells(game.getValidMovesForPiece(i, j, State.ATTACKER));
                humanSource.i = i;
                humanSource.j = j;
            } else if ( yourPiece == State.DEFENDER
                    && (positions[i][j] == State.DEFENDER || positions[i][j] == State.KING)) {

                if (positions[i][j] == State.DEFENDER)
                    game.getMove().player = State.DEFENDER;
                else
                    game.getMove().player = State.KING;

                boardView.highlightCells(game.getValidMovesForPiece(i, j, game.getMove().player));
                humanSource.i = i;
                humanSource.j = j;

                Log.d("Your Move", " Piece Selected : " + humanSource);
                Log.d("VALID_Moves", " Your Piece Selected : " + game.getMove().player
                        + ", VALID MOVES Coordinates : " + game.getValidMovesForPiece(i, j, game.getMove().player));

            } else if (humanSource.isNotEmpty() && positions[i][j] == State.EMPTY) {

                game.getMove().destinationMove.i = i;
                game.getMove().destinationMove.j = j;

                if (game.isValidMove(game.getMove())) {
                    sendMessage(game.getMove());
                } else {
                    boardView.highlightSquares(game.getMovablePieces(game.getMove().player));
                }
            } else {
                boardView.error(i, j);
                if (yourPiece == State.ATTACKER)
                    isAttackerMoved = false;
                else if (yourPiece == State.DEFENDER)
                    isDefenderMoved = false;
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

            if(gameOver){
                resetTimer();
            }

            if (removePerformed) {
                removePerformed = false;
                boardView.highlightSquares(game.getMovablePieces(game.getOpposingColour(game.getMove().player)));
                game.deleteMove();
                return;
            }

            if (isAttackerVictory) {
                isAttackerVictory = false;
                // stop all activity
                isAttckerSelected = false;
                return;
            }

            if (isDefenderVictory) {
                isDefenderVictory = false;
                // stop all activity
                isAttckerSelected = false;
                return;
            }

            if (isAttackerMoved) {
                isAttackerMoved = false;
                isAttackerMove = false;
                List<Coordinate> tobeRemove = game.performMove(game.getMove());
                performRemove(tobeRemove, game.getOpposingColour(game.getMove().player), true);
                //game.deleteMove();
                checkWin();
                removePerformed = true;
            }

            if (isDefenderMoved) {
                isDefenderMoved = false;
                isAttackerMove = true;
                List<Coordinate> tobeRemove = game.performMove(game.getMove());
                performRemove(tobeRemove, game.getOpposingColour(game.getMove().player), true);
                //game.deleteMove();
                checkWin();
                removePerformed = true;
            }

        }

        private boolean checkWin() {
            if (game.getGameStatus() != GameStatus.CONTINUE) {
                if (game.getGameStatus() == GameStatus.BLACK_WIN) {
                    isAttackerVictory = true;
                    isDefenderVictory = false;
                    gameOver = true;
                    notifyMessage("Winner", R.string.attacker_wins);
                    updateTurnIndicator(getString( R.string.attacker_wins ));
                    showDrawtBtn(false);
                    //Toast.makeText(ArdRiBluetooth.this, R.string.attacker_wins, Toast.LENGTH_LONG).show();
                } else if (game.getGameStatus() == GameStatus.WHITE_WIN) {
                    isDefenderVictory = true;
                    isAttackerVictory = false;
                    gameOver = true;
                    notifyMessage("Winner", R.string.defender_wins);
                    updateTurnIndicator(getString( R.string.defender_wins ));
                    showDrawtBtn(false);
                }
            }
            if (!game.hasMoreMoves(State.ATTACKER)) {
                isDefenderVictory = true;
                isAttackerVictory = false;
                gameOver = true;
                notifyMessage("Winner", R.string.defender_wins);
                updateTurnIndicator(getString( R.string.defender_wins ));
                showDrawtBtn(false);
            }
            if (!game.hasMoreMoves(State.DEFENDER)) {
                isAttackerVictory = true;
                isDefenderVictory = false;
                gameOver = true;
                notifyMessage("Winner", R.string.attacker_wins);
                updateTurnIndicator(getString( R.string.attacker_wins ));
                showDrawtBtn(false);
            }
            return gameOver;
        }
    }

    private void performRemove(List<Coordinate> pieces, State who, boolean withAnimation) {
        if (withAnimation)
            boardView.removeBall(pieces, who);
        for (Coordinate c : pieces) {
            positions[c.i][c.j] = State.EMPTY;
        }
    }

    private void performMove(Move move, boolean withAnimation) {
        if (move.isEmpty()) {
            boardView.noAction();
            return;
        }
        State who = move.player;
        if (withAnimation)
            boardView.moveBall(move, who);
        positions[move.sourceMove.i][move.sourceMove.j] = State.EMPTY;
        positions[move.destinationMove.i][move.destinationMove.j] = who;
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message A string of text to send.
     */
    private void sendMessage(Object message) {
        // Check that we're actually connected before trying anything
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            notifyMessage("Connection Notice", R.string.not_connected);
            return;
        }
        mService.write(StreamUtils.toByteArray(message));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setUpBluetoothService();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(ArdRiBluetooth.this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //get the device name
        String name = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_NAME);
        Log.d("Connecting", "name = " + name + " address = " + address);

        showLoadingOrSelectPiece(false);
        updateLoadingLabel(getString(R.string.title_connecting, name));

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mService.connect(device, secure);
    }
}

