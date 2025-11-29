import java.util.Scanner;

public class Gomoku {
    private Board board;

    public Gomoku(Board board) {
        this.board = board;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("欢迎来到五子棋游戏！");
        System.out.println("黑棋: ●, 白棋: ○");
        System.out.println("输入坐标格式: 行 列 (如: 7 7)");
        System.out.println("输入-1 -1来悔棋");

        while (true) {
            board.print();

            //显示当前玩家
            String playerName = (board.getCurrentPlayer() == -1) ? "黑棋(●)" : "白棋(○)";
            System.out.println("\n当前回合：" + playerName);

            //获取玩家输入
            int x = 0, y = 0;
            while (true) {
                System.out.print("请输入坐标：");
                x = scanner.nextInt();
                y = scanner.nextInt();
                if (x == -1 && y == -1) {
                    break;
                }
                if (!board.inBounds(x, y)) {
                    System.out.println("错误：坐标非法！");
                    continue;
                }
                if (board.hasPiece(x, y)) {
                    System.out.println("错误：此位置已经有子！");
                    continue;
                }
                break;
            }
            if (x == -1 && y == -1) {
                if (!board.hasHistory()) {
                    System.out.println("错误：无法悔棋，没有历史记录！");
                } else {
                    board.withdrawLast();
                }
                continue;
            }
            board.placePiece(x, y);
            if (board.hasWinner()) {
                board.print();
                System.out.println("恭喜 " + playerName + " 获胜！");
                break;
            }
            if (board.isADraw()) {
                board.print();
                System.out.println("本局平局！");
                break;
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        new Gomoku(new Board()).start();
    }
}
