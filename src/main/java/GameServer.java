// GameServer.java - 游戏服务器主类
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GameServer {
    private static final int PORT = 8080;
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private int roomIdCounter = 1;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("五子棋服务器启动在端口: " + PORT);
            System.out.println("等待客户端连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新的客户端连接: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler client = new ClientHandler(clientSocket, this);
                clients.add(client);
                pool.execute(client);
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized int createGameRoom(ClientHandler creator) {
        int roomId = roomIdCounter++;
        GameRoom room = new GameRoom(roomId, creator);
        gameRooms.put(roomId, room);
        System.out.println("创建新房间: " + roomId + " 创建者: " + creator.getPlayerName());
        return roomId;
    }

    public List<GameRoom> getAvailableRooms() {
        return gameRooms.values().stream()
                .filter(room -> room.getPlayer1() != null) // 只过滤掉无效房间
                .collect(Collectors.toList());
    }

    public GameRoom getGameRoom(int roomId) {
        return gameRooms.get(roomId);
    }

    public void removeGameRoom(int roomId) {
        GameRoom room = gameRooms.remove(roomId);
        if (room != null) {
            System.out.println("移除房间: " + roomId);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("客户端断开连接: " + client.getPlayerName());
    }

    public void broadcastToAll(String message) {
        synchronized (clients) {
            Iterator<ClientHandler> iterator = clients.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                if (client.isConnected()) {
                    client.sendMessage(message);
                } else {
                    iterator.remove();
                    System.out.println("清理断开连接的客户端: " + client.getPlayerName());
                }
            }
        }
    }

    public int getConnectedClientCount() {
        return clients.size();
    }

    public int getActiveRoomCount() {
        return gameRooms.size();
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("正在关闭服务器...");
            server.pool.shutdown();
            try {
                if (!server.pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.pool.shutdownNow();
            }
            System.out.println("服务器已关闭");
        }));

        Thread monitorThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== 五子棋服务器管理控制台 ===");
                System.out.println("1. 查看服务器状态");
                System.out.println("2. 查看所有房间详情");
                System.out.println("3. 查看连接客户端");
                System.out.println("4. 广播服务器消息");
                System.out.println("5. 清理断开连接的客户端");
                System.out.println("6. 退出服务器");
                System.out.print("请输入选择 (1-6): ");

                try {
                    String input = scanner.nextLine().trim();
                    int choice = Integer.parseInt(input);

                    switch (choice) {
                        case 1:
                            System.out.println("\n=== 服务器状态 ===");
                            System.out.println("启动时间: " + new Date());
                            System.out.println("连接客户端数: " + server.getConnectedClientCount());
                            System.out.println("活跃房间数: " + server.getActiveRoomCount());
                            System.out.println("线程池活跃线程数: " + ((ThreadPoolExecutor) server.pool).getActiveCount());
                            break;
                        case 2:
                            System.out.println("\n=== 所有房间详情 ===");
                            if (server.gameRooms.isEmpty()) {
                                System.out.println("没有活跃房间");
                            } else {
                                for (GameRoom room : server.gameRooms.values()) {
                                    System.out.println("房间 ID: " + room.getRoomId());
                                    System.out.println("  玩家1: " + (room.getPlayer1() != null ?
                                            room.getPlayer1().getPlayerName() + " [" + room.getPlayer1().getClientAddress() + "]" : "无"));
                                    System.out.println("  玩家2: " + (room.getPlayer2() != null ?
                                            room.getPlayer2().getPlayerName() + " [" + room.getPlayer2().getClientAddress() + "]" : "等待中"));
                                    System.out.println("  游戏状态: " + (room.isFull() ? "进行中" : "等待玩家加入"));
                                    System.out.println("  ------------");
                                }
                            }
                            break;
                        case 3:
                            System.out.println("\n=== 连接客户端 ===");
                            synchronized (server.clients) {
                                if (server.clients.isEmpty()) {
                                    System.out.println("没有连接客户端");
                                } else {
                                    for (ClientHandler client : server.clients) {
                                        System.out.println("客户端: " + client.getPlayerName() +
                                                " [" + client.getClientAddress() + "]" +
                                                " - 状态: " + (client.isConnected() ? "已连接" : "已断开"));
                                    }
                                }
                            }
                            break;
                        case 4:
                            System.out.print("请输入要广播的消息: ");
                            String message = scanner.nextLine().trim();
                            if (!message.isEmpty()) {
                                server.broadcastToAll("BROADCAST:服务器公告:" + message);
                                System.out.println("消息已广播给所有客户端");
                            } else {
                                System.out.println("消息不能为空");
                            }
                            break;
                        case 5:
                            synchronized (server.clients) {
                                int initialSize = server.clients.size();
                                server.clients.removeIf(client -> !client.isConnected());
                                System.out.println("清理完成: 移除了 " + (initialSize - server.clients.size()) + " 个断开连接的客户端");
                            }
                            break;
                        case 6:
                            System.out.println("正在关闭服务器...");
                            System.exit(0);
                            break;
                        default:
                            System.out.println("无效选择，请输入 1-6");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("输入错误，请输入数字 1-6");
                } catch (Exception e) {
                    System.out.println("发生错误: " + e.getMessage());
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        server.start();
    }
}