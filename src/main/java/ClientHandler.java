import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private GameRoom currentRoom;
    private int playerNumber;
    private String playerName;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("创建客户端处理器失败: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到来自 " + getPlayerInfo() + " 的消息: " + inputLine);
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("客户端断开连接: " + getPlayerInfo());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split(":");
        String command = parts[0];

        switch (command) {
            case "CREATE_ROOM":
                handleCreateRoom();
                break;
            case "LIST_ROOMS":
                handleListRooms();
                break;
            case "JOIN_ROOM":
                if (parts.length >= 2) {
                    int joinRoomId = Integer.parseInt(parts[1]);
                    handleJoinRoom(joinRoomId);
                }
                break;
            case "MAKE_MOVE":
                if (currentRoom != null && parts.length >= 3) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    currentRoom.makeMove(playerNumber, x, y);
                }
                break;
            case "CHAT":
                if (currentRoom != null && parts.length >= 2) {
                    String chatMessage = parts[1];
                    currentRoom.broadcastChat(playerNumber, chatMessage);
                }
                break;
            case "SET_NAME":
                if (parts.length >= 2) {
                    this.playerName = parts[1];
                    System.out.println("玩家设置名称: " + playerName);
                }
                break;
        }
    }

    private void handleCreateRoom() {
        int roomId = server.createGameRoom(this);
        sendMessage("ROOM_CREATED:" + roomId);
    }

    private void handleListRooms() {
        var rooms = server.getAvailableRooms();
        StringBuilder roomList = new StringBuilder("ROOM_LIST:");
        if (rooms.isEmpty()) {
            roomList.append("EMPTY");
        } else {
            for (GameRoom room : rooms) {
                String status = room.isFull() ? "游戏中" : "等待中";
                roomList.append(room.getRoomId())
                        .append("|")
                        .append(room.getPlayer1().getPlayerName())
                        .append("|")
                        .append(status)
                        .append(",");
            }
            roomList.deleteCharAt(roomList.length() - 1);
        }
        sendMessage(roomList.toString());
    }

    private void handleJoinRoom(int roomId) {
        GameRoom room = server.getGameRoom(roomId);
        if (room != null) {
            if (room.isFull()) {
                sendMessage("JOIN_FAILED:房间已满，无法加入");
            } else if (room.joinRoom(this)) {
                if (room.isFull()) {
                    room.startGame();
                } else {
                    sendMessage("JOIN_SUCCESS:" + roomId + ":1");
                }
            } else {
                sendMessage("JOIN_FAILED:无法加入该房间");
            }
        } else {
            sendMessage("JOIN_FAILED:房间不存在");
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            server.removeClient(this);
            if (currentRoom != null) {
                currentRoom.playerDisconnected(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null;
    }

    public String getPlayerInfo() {
        return "玩家" + playerNumber + "(" + getPlayerName() + ")";
    }

    public String getClientAddress() {
        if (socket != null && !socket.isClosed()) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return "未知";
    }

    // Getters and Setters
    public void setCurrentRoom(GameRoom room) { this.currentRoom = room; }
    public void setPlayerNumber(int number) { this.playerNumber = number; }
    public int getPlayerNumber() { return playerNumber; }
    public String getPlayerName() {
        return playerName != null ? playerName : "玩家" + playerNumber;
    }
    public GameRoom getCurrentRoom() { return currentRoom; }
}