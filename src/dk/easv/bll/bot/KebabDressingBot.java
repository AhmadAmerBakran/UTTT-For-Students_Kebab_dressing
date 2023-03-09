package dk.easv.bll.bot;

import dk.easv.bll.field.Field;
import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import java.util.List;

public class KebabDressingBot implements IBot {

    private static final String BOTNAME = "Kebab Dressings Bot";

    private IField field;

    private void setField(IField field) {
        this.field = field;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    @Override
    public IMove doMove(IGameState state) {
        int depth = 41; // Maximum depth for the search
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int currentPlayer = state.getMoveNumber();
        IMove bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        for (IMove move : state.getField().getAvailableMoves()) {
            int score = minimax(state, depth, alpha, beta, currentPlayer, false);
            if (score > bestScore || (score == 1 && bestScore != 1)) { // Prioritize winning moves
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }


    private int minimax(IGameState state, int depth, int alpha, int beta, int currentPlayer, boolean maximizingPlayer) {
        if (depth == 0 || state.getField().isFull()) {
            return evaluate(state, currentPlayer);
        }
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        if (availableMoves.isEmpty()) {
            return 0;
        }
        if (maximizingPlayer) {
            int bestScore = Integer.MIN_VALUE;
            for (IMove move : availableMoves) {
                IGameState nextState = cloneState(state);
                nextState.getField().getPlayerId(move.getX(), move.getY());
                nextState.setMoveNumber(nextState.getMoveNumber() + 1);
                int score = minimax(nextState, depth - 1, alpha, beta, currentPlayer, false);
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (IMove move : availableMoves) {
                IGameState nextState = cloneState(state);
                nextState.getField().getPlayerId(move.getX(), move.getY());
                nextState.setMoveNumber(nextState.getMoveNumber() + 1);
                int score = minimax(nextState, depth - 1, alpha, beta, currentPlayer, true);
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestScore;
        }
    }

    private int evaluate(IGameState state, int currentPlayer) {
        // Check if the current player has won
        String[][] macroboard = state.getField().getMacroboard();
        for (String[] strings : macroboard) {
            if (strings[0].equals(String.valueOf(currentPlayer)) &&
                    strings[1].equals(String.valueOf(currentPlayer)) &&
                    strings[2].equals(String.valueOf(currentPlayer))) {
                return 1; // Current player has won
            }
        }
        return 0;
    }

    private IGameState cloneState(IGameState state) {
        IField fieldCopy = cloneField(state.getField());
        int moveNumber = state.getMoveNumber();
        int roundNumber = state.getRoundNumber();
        int timePerMove = state.getTimePerMove();
        IGameState newState = new GameState();
        setField(fieldCopy);
        newState.setMoveNumber(moveNumber);
        newState.setRoundNumber(roundNumber);
        newState.setTimePerMove(timePerMove);
        return newState;
    }

    private IField cloneField(IField field) {
        String[][] board = field.getBoard();
        String[][] macroboard = field.getMacroboard();
        String[][] boardCopy = new String[board.length][];
        String[][] macroboardCopy = new String[macroboard.length][];
        for (int i = 0; i < board.length; i++) {
            boardCopy[i] = board[i].clone();
        }
        for (int i = 0; i < macroboard.length; i++) {
            macroboardCopy[i] = macroboard[i].clone();
        }
        Field fieldCopy = new Field();
        fieldCopy.setBoard(boardCopy);
        fieldCopy.setMacroboard(macroboardCopy);
        return fieldCopy;
    }
}
