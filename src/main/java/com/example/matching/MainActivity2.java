package com.example.matching;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private Button quitButton;
    private GridLayout gridLayout;
    private List<Integer> imageResources;
    private List<Card> cards;
    private Card firstCard, secondCard;
    private boolean isBusy = false;
    private static final int NUM_COLUMNS = 4;
    private static final int MARGIN_DP = 8;
    private static final int DELAY_MILLIS = 1000;

    private TextView timerTextView;
    private CountDownTimer timer;
    private long timeLeftInMillis;
    private boolean timerRunning;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        quitButton = findViewById(R.id.resetButton);
        quitButton.setOnClickListener(v -> showExitDialog());

        timerTextView = findViewById(R.id.timerTextView);

        gridLayout = findViewById(R.id.gridLayout);
        if (gridLayout != null) {
            setupGame();
        } else {
            Log.e("MainActivity", "GridLayout not found!");
        }
    }

    private void continueGame() {
        startTimer();
    }


    private void setupGame() {
        imageResources = new ArrayList<>(Arrays.asList(
                R.drawable.img, R.drawable.img_1, R.drawable.img_2, R.drawable.img_3,
                R.drawable.img_4, R.drawable.img_6
        ));

        imageResources.addAll(new ArrayList<>(imageResources));
        Collections.shuffle(imageResources);

        cards = new ArrayList<>();
        gridLayout.removeAllViews();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int marginPx = (int) (MARGIN_DP * metrics.density);
        int cardSize = (screenWidth - (NUM_COLUMNS * (marginPx * 2))) / NUM_COLUMNS;

        for (int i = 0; i < imageResources.size(); i++) {
            ImageButton button = new ImageButton(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.img_7);
            int imageId = imageResources.get(i);
            Card card = new Card(button, imageId);
            cards.add(card);
            button.setOnClickListener(v -> handleCardClick(card));
            gridLayout.addView(button);
        }

        startTimer();
    }

    private void handleCardClick(Card card) {
        if (card.isMatched() || card.isFlipped() || isBusy) return;

        card.flip();

        if (firstCard == null) {
            firstCard = card;
        } else {
            secondCard = card;
            isBusy = true;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (firstCard.getImageId() == secondCard.getImageId()) {
                    Log.d("MainActivity", "Cards matched!");
                    firstCard.setMatched(true);
                    secondCard.setMatched(true);
                    if (checkAllCardsMatched()) {
                        stopTimer();
                        showWinDialog();
                    }
                } else {
                    firstCard.flipBack();
                    secondCard.flipBack();
                }
                firstCard = null;
                secondCard = null;
                isBusy = false;
            }, DELAY_MILLIS);
        }
    }

    private boolean checkAllCardsMatched() {
        for (Card card : cards) {
            if (!card.isMatched()) {
                return false;
            }
        }
        return true;
    }

    private void showWinDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You won the game in " + formatTime(timeLeftInMillis) + "!")
                .setPositiveButton("Play Again", (dialog, which) -> setupGame())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    private void showExitDialog() {
        stopTimer();
        new AlertDialog.Builder(this)
                .setTitle("QUITER!")
                .setMessage("You sure you want to quit?")
                .setPositiveButton("Continue Playing", (dialog, which) -> continueGame())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    private void startTimer() {
        if (!timerRunning) {
            if (timeLeftInMillis == 0) {
                timeLeftInMillis = 60000;
            }
            updateTimerText();
            timerRunning = true;

            timer = new CountDownTimer(timeLeftInMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                    updateTimerText();
                }

                @Override
                public void onFinish() {
                    timerRunning = false;
                    timeLeftInMillis = 0;
                    updateTimerText();
                    showTimeoutDialog();
                }
            }.start();
        }
    }


    private void stopTimer() {
        if (timerRunning) {
            timer.cancel();
            timerRunning = false;
        }
    }

    private void updateTimerText() {
        String timeLeftFormatted = formatTime(timeLeftInMillis);
        timerTextView.setText(timeLeftFormatted);
    }

    private String formatTime(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void showTimeoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Time's Up!")
                .setMessage("You ran out of time.")
                .setPositiveButton("Play Again", (dialog, which) -> setupGame())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    public static class Card {
        private final ImageButton button;
        private final int imageId;
        private boolean isMatched = false;
        private boolean isFlipped = false;

        public Card(ImageButton button, int imageId) {
            this.button = button;
            this.imageId = imageId;
        }

        public void flip() {
            button.setBackgroundResource(imageId);
            isFlipped = true;
        }

        public void flipBack() {
            button.setBackgroundResource(R.drawable.img_7);
            isFlipped = false;
        }

        public int getImageId() {
            return imageId;
        }

        public boolean isMatched() {
            return isMatched;
        }

        public void setMatched(boolean matched) {
            isMatched = matched;
        }

        public boolean isFlipped() {
            return isFlipped;
        }
    }
}