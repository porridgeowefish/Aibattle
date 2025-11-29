import java.util.ArrayList;
import java.util.Stack;

public class Board {
    public static class Index {
        public int x;
        public int y;

        public Index(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final int BOUND = 15;
    public static final int WINNING_LENGTH = 4;
    // 0为空位，-1为黑，1为白
    private int[] board;
    private int currentPlayer;
    private int winner;
    private ArrayList<Index> indexLeft = new ArrayList<Index>();
    private Stack<Index> history = new Stack<Index>();

    // 空棋盘
    public Board() {
        this.board = new int[BOUND * BOUND];
        // 白子先下
        this.currentPlayer = 1;
        this.winner = 0;
        calIndexLeft();
    }

    // 用已有棋盘来初始化
    public Board(int[] initBoard, int currentPlayer) {
        assert (initBoard.length == BOUND * BOUND);
        this.board = initBoard.clone();
        this.currentPlayer = currentPlayer;
        calIndexLeft();
    }

    private void calIndexLeft() {
        for (int y = 1; y <= BOUND; y++) {
            for (int x = 1; x <= BOUND; x++) {
                if (!hasPiece(x, y)) {
                    this.indexLeft.add(new Index(x, y));
                }
            }
        }
    }

    public int getPiece(int x, int y) {
        return this.board[getIndex(x, y)];
    }

    public boolean hasPiece(int x, int y) {
        return getPiece(x, y) != 0;
    }

    public void placePiece(int x, int y) {
        assert (!hasPiece(x, y));
        this.board[getIndex(x, y)] = this.currentPlayer;
        this.checkWin(x, y);
        this.currentPlayer = -this.currentPlayer;
        this.indexLeft.removeIf(index -> index.x == x && index.y == y);
        this.history.push(new Index(x, y));
    }

    public void placePiece(Index index) {
        placePiece(index.x, index.y);
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public void withdrawLast() {
        assert (hasHistory());
        Index idx = history.pop();
        this.board[getIndex(idx)] = 0;
        this.indexLeft.add(idx);
        this.currentPlayer = -this.currentPlayer;
    }

    private void checkWin(int x, int y) {
        int maxLength = 0;
        for (int i = -WINNING_LENGTH; i <= WINNING_LENGTH; i++) {
            if (!this.inBounds(x + i, y)) {
                continue;
            }
            if (this.getPiece(x + i, y) == this.currentPlayer) {
                ++maxLength;
                if (maxLength >= WINNING_LENGTH + 1) {
                    this.winner = this.currentPlayer;
                    return;
                }
            } else {
                maxLength = 0;
            }
        }
        maxLength = 0;
        for (int i = -WINNING_LENGTH; i <= WINNING_LENGTH; i++) {
            if (!this.inBounds(x, y + i)) {
                continue;
            }
            if (this.getPiece(x, y + i) == this.currentPlayer) {
                ++maxLength;
                if (maxLength >= WINNING_LENGTH + 1) {
                    this.winner = this.currentPlayer;
                    return;
                }
            } else {
                maxLength = 0;
            }
        }
        maxLength = 0;
        for (int i = -WINNING_LENGTH; i <= WINNING_LENGTH; i++) {
            if (!this.inBounds(x + i, y + i)) {
                continue;
            }
            if (this.getPiece(x + i, y + i) == this.currentPlayer) {
                ++maxLength;
                if (maxLength >= WINNING_LENGTH + 1) {
                    this.winner = this.currentPlayer;
                    return;
                }
            } else {
                maxLength = 0;
            }
        }
        maxLength = 0;
        for (int i = -WINNING_LENGTH; i <= WINNING_LENGTH; i++) {
            if (!this.inBounds(x + i, y - i)) {
                continue;
            }
            if (this.getPiece(x + i, y - i) == this.currentPlayer) {
                ++maxLength;
                if (maxLength >= WINNING_LENGTH + 1) {
                    this.winner = this.currentPlayer;
                    return;
                }
            } else {
                maxLength = 0;
            }
        }
    }

    public boolean inBounds(int x, int y) {
        return 0 < x && x <= BOUND && 0 < y && y <= BOUND;
    }

    public boolean inBounds(Index index) {
        return inBounds(index.x, index.y);
    }

    // 下标从1开始
    private int getIndex(int x, int y) {
        assert (inBounds(x, y));
        return (y - 1) * BOUND + x - 1;
    }

    private int getIndex(Index index) {
        return getIndex(index.x, index.y);
    }

    public void print() {
        System.out.println("当前棋盘：");
        //打印列号
        System.out.print("  ");
        for (int i = 1; i <= BOUND; i++) {
            System.out.printf("%3d", i);
        }
        System.out.println();

        //打印棋盘内容
        for (int i = 1; i <= BOUND; i++) {
            System.out.printf("%2d", i);
            for (int j = 1; j <= BOUND; j++) {
                switch (getPiece(i, j)) {
                    case 0: //空位
                        if ((i == 8 && j == 8) || (i == 4 && j == 4) || (i == 4 && j == 12) || (i == 12 && j == 4) || (i == 12 && j == 12)) {
                            System.out.print("  +"); // 星位
                        } else {
                            System.out.print("  .");
                        }
                        break;
                    case -1: //黑棋
                        System.out.print("  ●");
                        break;
                    case 1: //白棋
                        System.out.print("  ○");
                        break;
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public int getCurrentPlayer() {
        return this.currentPlayer;
    }

    public int getWinner() {
        return this.winner;
    }

    public boolean hasWinner() {
        return this.winner != 0;
    }

    public ArrayList<Index> getIndexLeft() {
        return (ArrayList<Index>) this.indexLeft.clone();
    }

    // 平局
    public boolean isADraw() {
        return (this.winner == 0) && this.indexLeft.isEmpty();
    }

}