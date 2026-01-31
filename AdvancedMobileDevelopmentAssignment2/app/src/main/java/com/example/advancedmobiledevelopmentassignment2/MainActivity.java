
package com.example.advancedmobiledevelopmentassignment2;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public class MainActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 4;
    private int[][] gameBoard;
    private TextView scoreTextView;
    private TextView differenceTextView;
    private TextView bomb_msg;
    private GridLayout gameGrid;
    private GestureDetectorCompat gestureDetector;
    private int score = 0;
    private int prevScore = 0;
    private int difference = 0;
    private String bomb_MSG = "";
    private MediaPlayer bangPlayer;
    private MediaPlayer times2Player;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeGame();
        setupGestureDetector();
    }

    private void assignProperVolume(MediaPlayer mediaPlayer, float left, float right) {
        if (mediaPlayer != null) {
            // Set volume to maximum (1.0f for both left and right channels)
            mediaPlayer.setVolume(left, right);
        }
    }

    private void initializeViews() {
        assignProperVolume(bangPlayer,1,1);
        assignProperVolume(times2Player,1,1);
        scoreTextView = findViewById(R.id.scoreTextView);
        differenceTextView = findViewById(R.id.Difference);
        gameGrid = findViewById(R.id.gameGrid);
        Button resetButton = findViewById(R.id.resetButton);
        bomb_msg = findViewById(R.id.bomb_message);
        bomb_msg.setText(bomb_MSG);
        resetButton.setOnClickListener(v -> resetGame());
        bangPlayer = MediaPlayer.create(this, R.raw.bang);
        times2Player = MediaPlayer.create(this, R.raw.times2);
    }

    private void initializeGame() {
        gameBoard = new int[GRID_SIZE][GRID_SIZE];
        score = 0;
        prevScore = 0;
        difference = 0;
        updateScore();

        // Clear the grid
        gameGrid.removeAllViews();

        // Add two initial tiles
        addRandomTile();
        addRandomTile();

        updateGridDisplay();
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                result = moveTiles("RIGHT");
                            } else {
                                result = moveTiles("LEFT");
                            }
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                result = moveTiles("DOWN");
                            } else {
                                result = moveTiles("UP");
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private boolean moveTiles(String direction) {
        boolean moved = false;
        int[][] previousBoard = copyBoard(gameBoard);
        switch (direction) {
            case "UP":
                moved = moveUp();
                break;
            case "DOWN":
                moved = moveDown();
                break;
            case "LEFT":
                moved = moveLeft();
                break;
            case "RIGHT":
                moved = moveRight();
                break;
        }
        if (moved) {
            addRandomTile();
            updateGridDisplay();
            updateScore();
            if (checkWin()) {
                showGameResultDialog(true);
            } else if (checkGameOver()) {
                showGameResultDialog(false);
            }
        }
        return moved;
    }

    private boolean moveUp() {
        boolean moved = false;
        // First, move all tiles including bombs
        for (int col = 0; col < GRID_SIZE; col++) {
            for (int row = 1; row < GRID_SIZE; row++) {
                if (gameBoard[row][col] != 0) {
                    int currentRow = row;
                    while (currentRow > 0 && gameBoard[currentRow - 1][col] == 0) {
                        gameBoard[currentRow - 1][col] = gameBoard[currentRow][col];
                        gameBoard[currentRow][col] = 0;
                        currentRow--;
                        moved = true;
                    }
                    if (currentRow > 0) {
                        moved = handleTileCollision(currentRow - 1, col, currentRow, col) || moved;
                    }
                }
            }
        }
        // Process bombs after movement - UP direction
        processBombsAfterMove("UP");
        return moved;
    }

    private boolean moveDown() {
        boolean moved = false;
        for (int col = 0; col < GRID_SIZE; col++) {
            for (int row = GRID_SIZE - 2; row >= 0; row--) {
                if (gameBoard[row][col] != 0) {
                    int currentRow = row;
                    while (currentRow < GRID_SIZE - 1 && gameBoard[currentRow + 1][col] == 0) {
                        gameBoard[currentRow + 1][col] = gameBoard[currentRow][col];
                        gameBoard[currentRow][col] = 0;
                        currentRow++;
                        moved = true;
                    }
                    if (currentRow < GRID_SIZE - 1) {
                        moved = handleTileCollision(currentRow + 1, col, currentRow, col) || moved;
                    }
                }
            }
        }
        processBombsAfterMove("DOWN");
        return moved;
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 1; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] != 0) {
                    int currentCol = col;
                    while (currentCol > 0 && gameBoard[row][currentCol - 1] == 0) {
                        gameBoard[row][currentCol - 1] = gameBoard[row][currentCol];
                        gameBoard[row][currentCol] = 0;
                        currentCol--;
                        moved = true;
                    }
                    if (currentCol > 0) {
                        moved = handleTileCollision(row, currentCol - 1, row, currentCol) || moved;
                    }
                }
            }
        }
        processBombsAfterMove("LEFT");
        return moved;
    }

    private boolean moveRight() {
        boolean moved = false;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = GRID_SIZE - 2; col >= 0; col--) {
                if (gameBoard[row][col] != 0) {
                    int currentCol = col;
                    while (currentCol < GRID_SIZE - 1 && gameBoard[row][currentCol + 1] == 0) {
                        gameBoard[row][currentCol + 1] = gameBoard[row][currentCol];
                        gameBoard[row][currentCol] = 0;
                        currentCol++;
                        moved = true;
                    }
                    if (currentCol < GRID_SIZE - 1) {
                        moved = handleTileCollision(row, currentCol + 1, row, currentCol) || moved;
                    }
                }
            }
        }
        processBombsAfterMove("RIGHT");
        return moved;
    }

    private void showExplosionAnimation(int row, int col) {
        ImageView explodeImage = findViewById(R.id.explode_img);
        //sound
        if (bangPlayer != null) {
            bangPlayer.start();
        }
        // Make visible and start animation
        explodeImage.setVisibility(View.VISIBLE);
        // Hide after 1 move (which typically happens quickly, but we'll use a delay)
        new android.os.Handler().postDelayed(() -> {
            explodeImage.setVisibility(View.INVISIBLE);
        }, 500); // 500ms = 0.5 seconds, adjust as needed
    }

    private void showGameResultDialog(boolean isWin) {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_result, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Initialize views
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button newGameButton = dialogView.findViewById(R.id.dialogNewGameButton);
        Button continueButton = dialogView.findViewById(R.id.dialogContinueButton);
        // Set dialog content based on win/lose
        if (isWin) {
            dialogTitle.setText("Congratulations!");
            dialogMessage.setText("You reached 2048!\nFinal Score: " + score);
        } else {
            dialogTitle.setText("Game Over!");
            dialogMessage.setText("No more moves available!\nFinal Score: " + score);
            continueButton.setVisibility(View.GONE); // Hide continue button for game over
        }
        // Button listeners
        newGameButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetGame();
        });
        continueButton.setOnClickListener(v -> {
            dialog.dismiss();
            // Continue playing (do nothing)
        });
        dialog.show();
    }

    private boolean handleTileCollision(int targetRow, int targetCol, int sourceRow, int sourceCol) {
        int targetValue = gameBoard[targetRow][targetCol];
        int sourceValue = gameBoard[sourceRow][sourceCol];

        // Remove bombs on collision with any tile (they'll be processed later)
        if (sourceValue == -1 || targetValue == -1) {
            // Bombs will be handled in processBombsAfterMove
            return true;
        }

        // Multiplier tile behavior
        if (sourceValue == -2 && targetValue > 0) { // *2 hits regular tile
            gameBoard[targetRow][targetCol] = targetValue * 2;
            gameBoard[sourceRow][sourceCol] = 0;
            //sound
            if (times2Player != null) {
                times2Player.start();
            }
            return true;
        }
        if (targetValue == -2 && sourceValue > 0) { // Regular tile hits *2
            gameBoard[targetRow][targetCol] = sourceValue * 2;
            gameBoard[sourceRow][sourceCol] = 0;
            return true;
        }

        // Regular tile merging (original behavior)
        if (targetValue > 0 && sourceValue > 0 && targetValue == sourceValue) {
            gameBoard[targetRow][targetCol] = targetValue * 2;
            gameBoard[sourceRow][sourceCol] = 0;
            return true;
        }

        return false;
    }

    private void processBombsAfterMove(String direction) {
        boolean[][] bombsToExplode = new boolean[GRID_SIZE][GRID_SIZE];
        boolean[][] bombsToDefuse = new boolean[GRID_SIZE][GRID_SIZE];

        // First pass: identify bombs and check if they hit borders
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == -1) {
                    boolean hitBorder = false;

                    switch (direction) {
                        case "UP":
                            hitBorder = (row == 0); // Hit top border
                            break;
                        case "DOWN":
                            hitBorder = (row == GRID_SIZE - 1); // Hit bottom border
                            break;
                        case "LEFT":
                            hitBorder = (col == 0); // Hit left border
                            break;
                        case "RIGHT":
                            hitBorder = (col == GRID_SIZE - 1); // Hit right border
                            break;
                    }

                    if (hitBorder) {
                        bombsToDefuse[row][col] = true;
                        // display message "bomb"
                        bomb_MSG="DEFUSED!";
                        bomb_msg.setText(bomb_MSG);
                        bomb_msg.setTextColor(Color.BLUE);
                    } else {
                        bombsToExplode[row][col] = true;
                        // display message "bomb"
                        bomb_MSG="BOMB!";
                        bomb_msg.setText(bomb_MSG);
                        bomb_msg.setTextColor(Color.RED);

                    }
                }
            }
        }

        // Defuse bombs that hit borders (simply remove them)
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (bombsToDefuse[row][col]) {
                    gameBoard[row][col] = 0; // Remove bomb
                }
            }
        }

        // Explode bombs that didn't hit borders
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (bombsToExplode[row][col]) {
                    explodeBomb(row, col);
                }
            }
        }
    }

    private void explodeBomb(int row, int col) {
        // Show explosion animation
        showExplosionAnimation(row, col);

        // Remove the bomb itself
        gameBoard[row][col] = 0;

        // Define adjacent positions (up, down, left, right)
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // Check if adjacent position is within bounds
            if (newRow >= 0 && newRow < GRID_SIZE && newCol >= 0 && newCol < GRID_SIZE) {
                // Remove adjacent tile (except other bombs to avoid chain reaction)
                if (gameBoard[newRow][newCol] != -1) {
                    gameBoard[newRow][newCol] = 0;
                }
            }
        }
    }

    private void addRandomTile() {
        java.util.ArrayList<int[]> emptyCells = new java.util.ArrayList<>();

        // Find all empty cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == 0) {
                    emptyCells.add(new int[]{row, col});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            int[] randomCell = emptyCells.get((int) (Math.random() * emptyCells.size()));
            double randomValue = Math.random();

            if (randomValue < 0.6) {
                // 60% chance: spawn "2"
                gameBoard[randomCell[0]][randomCell[1]] = 2;
            } else if (randomValue < 0.8) {
                // 20% chance: spawn "4"
                gameBoard[randomCell[0]][randomCell[1]] = 4;
            } else if (randomValue < 0.9) {
                // 10% chance: spawn "*2" (multiplier tile)
                gameBoard[randomCell[0]][randomCell[1]] = -2; // Using negative for special tiles
            } else {
                // 10% chance: spawn "bomb"
                gameBoard[randomCell[0]][randomCell[1]] = -1; // Using -1 for bomb
            }
        }
    }

    private void updateGridDisplay() {
        gameGrid.removeAllViews();

        int cellSize = Math.min(gameGrid.getWidth(), gameGrid.getHeight()) / GRID_SIZE - 16;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(4, 4, 4, 4);
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                cell.setLayoutParams(params);

                int value = gameBoard[row][col];
                String displayText = "";


                if (value > 0) {
                    displayText = String.valueOf(value);
                } else if (value == -2) {
                    displayText = "*2";
                } else if (value == -1) {
                    displayText = "💣"; // Bomb emoji
                    // display message "bomb"
                    bomb_MSG="BOMB Has Spawned";
                    bomb_msg.setText(bomb_MSG);
                    bomb_msg.setTextColor(Color.parseColor("#FFA500"));
                }
                // value == 0 remains empty

                cell.setText(displayText);
                cell.setTextSize(20);
                cell.setGravity(android.view.Gravity.CENTER);
                cell.setBackgroundColor(getTileColor(value));
                cell.setTextColor(getTextColor(value));

                gameGrid.addView(cell);
            }
        }
    }

    private int getTileColor(int value) {
        switch (value) {
            case 0: return Color.parseColor("#cdc1b4");
            case 2: return Color.parseColor("#eee4da");
            case 4: return Color.parseColor("#ede0c8");
            case 8: return Color.parseColor("#f2b179");
            case 16: return Color.parseColor("#f59563");
            case 32: return Color.parseColor("#f67c5f");
            case 64: return Color.parseColor("#f65e3b");
            case 128: return Color.parseColor("#edcf72");
            case 256: return Color.parseColor("#edcc61");
            case 512: return Color.parseColor("#edc850");
            case 1024: return Color.parseColor("#edc53f");
            case 2048: return Color.parseColor("#edc22e");
            case -2: return Color.parseColor("#4CAF50"); // Green for multiplier
            case -1: return Color.parseColor("#F44336"); // Red for bomb
            default: return Color.parseColor("#3c3a32");
        }
    }

    private int getTextColor(int value) {
        if (value == -1 || value == -2) {
            return Color.parseColor("#FFFFFF"); // White text for special tiles
        }
        return value <= 4 ? Color.parseColor("#776e65") : Color.parseColor("#f9f6f2");
    }

    private void updateScore() {
        int totalScore = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int tileValue = gameBoard[row][col];

                // Only count positive number tiles (2, 4, 8, 16, etc.)
                // Exclude bombs (-1), multiplier tiles (-2), and empty tiles (0)
                if (tileValue > 0) {
                    totalScore += tileValue;
                }
            }
        }
        difference= totalScore - prevScore;
        prevScore = totalScore;
        score = totalScore;
        scoreTextView.setText(String.valueOf(score));
        if(difference>0){
            differenceTextView.setText("+" + String.valueOf((difference)));
            differenceTextView.setTextColor(Color.GREEN);
        }else{
            differenceTextView.setText("-" + String.valueOf((difference)));
            differenceTextView.setTextColor(Color.RED);
        }
    }

    private void resetGame() {
        initializeGame();
    }

    private boolean checkWin() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (score >= 2048) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkGameOver() {
        // Check for empty cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (gameBoard[row][col] == 0) {
                    return false;
                }
            }
        }

        // Check for possible merges
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int current = gameBoard[row][col];
                if ((row < GRID_SIZE - 1 && current == gameBoard[row + 1][col]) ||
                        (col < GRID_SIZE - 1 && current == gameBoard[row][col + 1])) {
                    return false;
                }
            }
        }

        return true;
    }

    private int[][] copyBoard(int[][] original) {
        int[][] copy = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, GRID_SIZE);
        }
        return copy;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force layout update when activity resumes
        gameGrid.post(this::updateGridDisplay);
    }

}