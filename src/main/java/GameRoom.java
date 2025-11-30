// GameRoom.java - 游戏房间类
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private int roomId;
    private ClientHandler player1;
    private ClientHandler player2;
    private Board board;
    private boolean gameStarted = false;
    private int currentPlayer = 1;

    public GameRoom(int roomId, ClientHandler creator) {
        this.roomId = roomId;
        this.player1 = creator;
        this.board = new Board();
        creator.setCurrentRoom(this);
        creator.setPlayerNumber(1);
    }

    public boolean joinRoom(ClientHandler player) {
        if (player2 == null && player != player1) {
            player2 = player;
            player.setCurrentRoom(this);
            player.setPlayerNumber(2);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return player1 != null && player2 != null;
    }

    public void startGame() {
        this.gameStarted = true;

        // 分别通知每个玩家他们的编号
        if (player1 != null && player1.isConnected()) {
            player1.sendMessage("JOIN_SUCCESS:" + roomId + ":1");
        }
        if (player2 != null && player2.isConnected()) {
            player2.sendMessage("JOIN_SUCCESS:" + roomId + ":2");
        }

        broadcastMessage("GAME_START:" + roomId);
        broadcastPlayerInfo();
        broadcastTurnInfo();
    }

    public boolean makeMove(int playerNum, int x, int y) {
        if (!gameStarted || currentPlayer != playerNum) {
            return false;
        }

        // 检查位置是否有效
        if (!board.inBounds(x, y) || board.hasPiece(x, y)) {
            return false;
        }

        // 使用Board类的落子逻辑
        board.placePiece(x, y);

        // 先广播落子信息
        broadcastMove(x, y, playerNum);

        // 检查胜负
        if (board.hasWinner()) {
            int winner = board.getWinner();
            // 正确转换玩家编号
            // Board类：-1=黑棋(玩家1)，1=白棋(玩家2)
            // 网络对战：1=玩家1(黑棋)，2=玩家2(白棋)
            int networkWinner = (winner == -1) ? 2 : 1;
            System.out.println("游戏结束！Board获胜者: " + winner + " -> 网络获胜者: " + networkWinner);
            broadcastMessage("GAME_OVER:" + networkWinner);
            return true;
        }

        // 检查平局
        if (board.isADraw()) {
            broadcastMessage("GAME_OVER:0");
            return true;
        }

        // 切换玩家并广播回合信息
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        broadcastTurnInfo();
        return true;
    }

    public void broadcastChat(int fromPlayer, String message) {
        String chatMsg = "CHAT:" + fromPlayer + ":" +
                (fromPlayer == 1 ? player1.getPlayerName() : player2.getPlayerName()) +
                ":" + message;
        broadcastMessage(chatMsg);
    }

    private void broadcastMove(int x, int y, int playerNum) {
        String message = "MOVE:" + x + ":" + y + ":" + playerNum;
        broadcastMessage(message);
    }

    private void broadcastTurnInfo() {
        String message = "TURN:" + currentPlayer;
        broadcastMessage(message);
    }

    private void broadcastPlayerInfo() {
        String playerInfo = "PLAYER_INFO:1:" + player1.getPlayerName() + ":2:" +
                (player2 != null ? player2.getPlayerName() : "等待玩家");
        broadcastMessage(playerInfo);
    }

    private void broadcastMessage(String message) {
        if (player1 != null && player1.isConnected()) {
            player1.sendMessage(message);
        }
        if (player2 != null && player2.isConnected()) {
            player2.sendMessage(message);
        }
    }

    public void playerDisconnected(ClientHandler player) {
        if (gameStarted) {
            int disconnectedPlayer = player.getPlayerNumber();
            int winner = disconnectedPlayer == 1 ? 2 : 1;
            broadcastMessage("PLAYER_DISCONNECTED:" + disconnectedPlayer);
            broadcastMessage("GAME_OVER:" + winner);
        }
    }

    // Getters
    public int getRoomId() { return roomId; }
    public ClientHandler getPlayer1() { return player1; }
    public ClientHandler getPlayer2() { return player2; }
    public Board getBoard() { return board; }

}