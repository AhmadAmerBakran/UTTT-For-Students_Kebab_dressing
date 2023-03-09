package dk.easv.bll.bot;



import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class Khalaf implements IBot{
    private static final int LOOP_END_TIME_MS = 1000;
    private static final String BOT_NAME = "Khalaf";

    private Random rand;
    private int opponent;
    private UCT uct;

    public Khalaf()
    {
        rand = new Random();
        uct = new UCT();
    }

    @Override
    public IMove doMove(IGameState state) {


        long startTime = System.currentTimeMillis();
        long endTime = startTime + LOOP_END_TIME_MS;



        Node rootNode = new Node(state);


        opponent = (state.getMoveNumber()+1)%2;
        int simulations = 0;

        while(System.currentTimeMillis() < endTime)
        {


            Node promisingNode = selectPromisingNode(rootNode);


            if(!isGameOver(promisingNode.getState()))
            {
                expandNode(promisingNode);
            }


            Node nodeToExplore = promisingNode;
            if(!nodeToExplore.getChildren().isEmpty())
            {
                nodeToExplore = promisingNode.getRandomChild();
            }

            int rolloutResult = performRollout(nodeToExplore);
            backPropagation(nodeToExplore, rolloutResult);
            simulations++;
        }
        Node winnerNode = rootNode.getChildWithMaxScore();
        System.out.println("Simulations: " + simulations);
        return getMove(rootNode, winnerNode);
    }

    private Node selectPromisingNode(Node rootNode)
    {
        Node promisingNode = rootNode;
        while(!promisingNode.getChildren().isEmpty())
        {
            promisingNode = uct.findBestNodeWithUCT(promisingNode);
        }
        return promisingNode;
    }

    private void expandNode(Node promisingNode)
    {
        List<IMove> availableMoves = promisingNode.getState().getField().getAvailableMoves();
        for(IMove move : availableMoves)
        {


            Node childNode = new Node(promisingNode.getState());
            childNode.setParent(promisingNode);
            promisingNode.getChildren().add(childNode);


            performMove(childNode.getState(), move.getX(), move.getY());
        }
    }

    private int performRollout(Node nodeToExplore)
    {
        Node tempNode = new Node(nodeToExplore);
        IGameState tempState = tempNode.getState();
        if(isGameOver(tempState) && (tempState.getMoveNumber()+1)%2 == opponent)
        {
            tempNode.getParent().setScore(Integer.MIN_VALUE);
            return opponent;
        }
        while(!isGameOver(tempState))
        {
            randomPlay(tempState);
        }
        return (tempState.getMoveNumber()+1)%2;

    }

    private void backPropagation(Node node, int value)
    {
        Node tempNode = node;
        while(tempNode != null)
        {
            tempNode.incrementVisit();
            if((tempNode.getState().getMoveNumber()+1)%2 == value)
            {
                tempNode.addScore(10);
            }
            tempNode = tempNode.getParent();
        }

    }

    private void randomPlay(IGameState state)
    {
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        IMove randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
        performMove(state, randomMove.getX(), randomMove.getY());
    }

    private class UCT {

        public double uctValue(int totalVisit, double nodeWinScore, int nodeVisit)
        {
            if (nodeVisit == 0)
            {
                return Integer.MAX_VALUE;
            }
            return (nodeWinScore / (double) nodeVisit) + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
        }

        public Node findBestNodeWithUCT(Node node)
        {
            int parentVisit = node.getNumberOfVisits();
            return Collections.max(
                    node.getChildren(),
                    Comparator.comparing(child -> uctValue(parentVisit,
                            child.getScore(), child.getNumberOfVisits())));
        }

    }

    private class Node {

        private Node parent;

        private IGameState state;
        private int score;
        private int numberOfVisits;

        private List<Node> children;

        public Node(Node node)
        {
            this(node.getState());

            this.children = new ArrayList<>();
            if(node.getParent() != null)
            {
                this.parent = node.getParent();
            }

            List<Node> childArray = node.getChildren();
            for(Node child : childArray)
            {
                this.children.add(new Node(child));
            }

            score = node.getScore();
            numberOfVisits = node.getNumberOfVisits();
        }

        public Node(IGameState state)
        {
            this.state = new GameState();
            String[][] board = new String[9][9];
            String[][] macroBoard = new String[3][3];
            for(int i = 0; i < board.length; i++)
            {
                for(int j = 0; j < board[i].length; j++)
                {
                    board[i][j] = state.getField().getBoard()[i][j];
                }
            }
            for(int i = 0; i < macroBoard.length; i++)
            {
                for(int j = 0; j < macroBoard[i].length; j++)
                {
                    macroBoard[i][j] = state.getField().getMacroboard()[i][j];
                }
            }

            this.state.getField().setBoard(board);
            this.state.getField().setMacroboard(macroBoard);

            this.state.setMoveNumber(state.getMoveNumber());
            this.state.setRoundNumber(state.getRoundNumber());

            this.children = new ArrayList<>();

            this.score = 0;
            this.numberOfVisits = 0;
        }

        public List<Node> getChildren()
        {
            return children;
        }

        public Node getChildWithMaxScore()
        {
            return Collections.max(this.children, Comparator.comparing(Node::getNumberOfVisits));
        }

        public Node getRandomChild()
        {
            return children.get(rand.nextInt(children.size()));
        }

        public Node getParent()
        {
            return parent;
        }

        public void setParent(Node parent)
        {
            this.parent = parent;
        }

        public IGameState getState()
        {
            return state;
        }

        public void setScore(int score)
        {
            this.score = score;
        }

        public void addScore(int score)
        {
            this.score += score;
        }

        public int getScore()
        {
            return score;
        }

        public void incrementVisit()
        {
            numberOfVisits++;
        }

        public int getNumberOfVisits()
        {
            return numberOfVisits;
        }

    }

    private IMove getMove(Node parentNode, Node childNode)
    {
        String[][] parentBoard = parentNode.getState().getField().getBoard();
        String[][] childBoard = childNode.getState().getField().getBoard();
        for(int i = 0; i < parentBoard.length; i++)
        {
            for(int j = 0; j < parentBoard[i].length; j++)
            {
                if(!parentBoard[i][j].equals(childBoard[i][j]))
                {
                    return new Move(i,j);
                }
            }
        }
        return null;
    }

    private void performMove(IGameState state, int moveX, int moveY)
    {
        String[][] board = state.getField().getBoard();
        board[moveX][moveY] = state.getMoveNumber()%2 + "";
        state.getField().setBoard(board);
        updateMacroBoard(state, moveX, moveY);
        state.setMoveNumber(state.getMoveNumber()+1);
    }

    private void updateMacroBoard(IGameState state, int moveX, int moveY)
    {
        updateMicroBoardState(state, moveX, moveY);
        updateMicroBoardsAvailability(state, moveX, moveY);
    }

    private void updateMicroBoardState(IGameState state, int moveX, int moveY)
    {
        String[][] macroBoard = state.getField().getMacroboard();
        int startingXPosition = (moveX/3)*3;
        int startingYPosition = (moveY/3)*3;
        if(isWinOnMicroBoard(state, startingXPosition, startingYPosition))
        {
            macroBoard[moveX/3][moveY/3] = state.getMoveNumber()%2+"";
        }
        else if(isDrawOnMicroBoard(state, startingXPosition, startingYPosition))
        {
            macroBoard[moveX/3][moveY/3] = "-";
        }
        state.getField().setMacroboard(macroBoard);
    }

    private void updateMicroBoardsAvailability(IGameState state, int moveX, int moveY)
    {
        int activeMicroBoardX = moveX%3;
        int activeMicroBoardY = moveY%3;
        String[][] macroBoard = state.getField().getMacroboard();
        if(macroBoard[activeMicroBoardX][activeMicroBoardY].equals(IField.AVAILABLE_FIELD)
                || macroBoard[activeMicroBoardX][activeMicroBoardY].equals(IField.EMPTY_FIELD))
        {
            setAvailableMicroBoard(state, activeMicroBoardX, activeMicroBoardY);
        }
        else
        {
            setAllMicroBoardsAvailable(state);
        }
    }

    private void setAvailableMicroBoard(IGameState state, int activeMicroboardX, int activeMicroboardY)
    {
        String[][] macroBoard = state.getField().getMacroboard();
        for(int x = 0; x < macroBoard.length; x++)
        {
            for(int y = 0; y < macroBoard[x].length; y++)
            {
                if(x == activeMicroboardX && y == activeMicroboardY)
                {
                    macroBoard[x][y] = IField.AVAILABLE_FIELD;
                }
                else if(macroBoard[x][y].equals(IField.AVAILABLE_FIELD))
                {
                    macroBoard[x][y] = IField.EMPTY_FIELD;
                }
            }
        }
        state.getField().setMacroboard(macroBoard);
    }

    private void setAllMicroBoardsAvailable(IGameState state)
    {
        String[][] macroBoard = state.getField().getMacroboard();
        for(int x = 0; x < 3; x++)
        {
            for(int y = 0; y < 3; y++)
            {
                if(macroBoard[x][y].equals(IField.EMPTY_FIELD))
                {
                    macroBoard[x][y] = IField.AVAILABLE_FIELD;
                }
            }
        }
    }

    private boolean isWinOnMicroBoard(IGameState state, int startingX, int startingY)
    {
        String[][] board = state.getField().getBoard();
        return isWinOnBoard(board, startingX, startingY);
    }

    private boolean isDrawOnMicroBoard(IGameState state, int startingX, int startingY)
    {
        boolean isDraw = true;
        String[][] board = state.getField().getBoard();
        for(int x = startingX; x < startingX+3; x++)
        {
            for(int y = startingY; y < startingY+3; y++)
            {
                if(board[x][y].equals(IField.EMPTY_FIELD))
                {
                    isDraw = false;
                }
            }
        }
        return isDraw;
    }

    private boolean isGameOver(IGameState state)
    {
        String[][] macroBoard = state.getField().getMacroboard();
        return isWinOnBoard(macroBoard, 0, 0) || isDraw(state);
    }

    private boolean isWin(IGameState state)
    {
        String[][] macroBoard = state.getField().getMacroboard();
        return isWinOnBoard(macroBoard, 0, 0);
    }

    private boolean isDraw(IGameState state)
    {
        String[][] macroboard = state.getField().getMacroboard();
        for(int x = 0; x < macroboard.length; x++)
        {
            for(int y = 0; y < macroboard[x].length; y++)
            {
                if(macroboard[x][y].equals(IField.EMPTY_FIELD) || macroboard[x][y].equals(IField.AVAILABLE_FIELD))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isWinOnBoard(String[][] board, int startingX, int startingY)
    {
        for(int x = startingX; x < startingX+3; x++)
        {
            if(isHorizontalWin(board, x, startingY))
            {
                return true;
            }
            for(int y = startingY; y < startingY+3; y++)
            {

                if(isVerticalWin(board, startingX, y))
                {
                    return true;
                }
            }
        }
        return isDiagonalWin(board, startingX, startingY);
    }

    private boolean isHorizontalWin(String[][] board, int startingX, int startingY)
    {
        return ((board[startingX][startingY].equals("0") || board[startingX][startingY].equals("1"))
                && board[startingX][startingY].equals(board[startingX][startingY+1])
                && board[startingX][startingY+1].equals(board[startingX][startingY+2]));
    }

    private boolean isVerticalWin(String[][] board, int startingX, int startingY)
    {
        return ((board[startingX][startingY].equals("0") || board[startingX][startingY].equals("1"))
                && board[startingX][startingY].equals(board[startingX+1][startingY])
                && board[startingX+1][startingY].equals(board[startingX+2][startingY]));
    }

    private boolean isDiagonalWin(String[][] board, int startingX, int startingY)
    {
        if((board[startingX][startingY].equals("0") || board[startingX][startingY].equals("1"))
                && board[startingX][startingY].equals(board[startingX+1][startingY+1])
                && board[startingX+1][startingY+1].equals(board[startingX+2][startingY+2]))
        {
            return true;
        }
        else if((board[startingX][startingY+2].equals("0") || board[startingX][startingY+2].equals("1"))
                && board[startingX][startingY+2].equals(board[startingX+1][startingY+1])
                && board[startingX+1][startingY+1].equals(board[startingX+2][startingY]))
        {
            return true;
        }
        return false;
    }
    @Override
    public String getBotName() {
        return BOT_NAME;
    }

}