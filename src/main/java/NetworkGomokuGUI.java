// NetworkGomokuGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class NetworkGomokuGUI extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BoardPanel boardPanel;
    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel;
    private JButton createRoomBtn, refreshRoomsBtn;
    private JList<String> roomList;
    private DefaultListModel<String> roomListModel;
    private int currentPlayer;
    private int myPlayerNumber;
    private String playerName;
    private boolean gameStarted = false;

    public NetworkGomokuGUI() {
        this.playerName = getPlayerName();
        initializeGUI();
        connectToServer();
    }

    private String getPlayerName() {
        String name = JOptionPane.showInputDialog(this,
                "请输入您的玩家名称:", "五子棋网络对战", JOptionPane.QUESTION_MESSAGE);

        if (name == null) {
            System.exit(0);
        }

        if (name == null || name.trim().isEmpty()) {
            name = "玩家" + System.currentTimeMillis() % 1000;
        }

        return name.trim();
    }

    private void initializeGUI() {
        setTitle("五子棋网络对战 - " + playerName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 顶部面板
        JPanel topPanel = new JPanel(new FlowLayout());
        statusLabel = new JLabel("未连接");
        createRoomBtn = new JButton("创建房间");
        refreshRoomsBtn = new JButton("刷新房间列表");

        createRoomBtn.addActionListener(e -> createRoom());
        refreshRoomsBtn.addActionListener(e -> refreshRooms());

        topPanel.add(statusLabel);
        topPanel.add(createRoomBtn);
        topPanel.add(refreshRoomsBtn);

        // 中央面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // 棋盘面板
        boardPanel = new BoardPanel();
        splitPane.setLeftComponent(boardPanel);

        // 右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(300, 700));

        // 房间列表
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.setPreferredSize(new Dimension(280, 200));
        roomPanel.setBorder(BorderFactory.createTitledBorder("房间列表 (双击加入)"));

        JScrollPane roomScrollPane = new JScrollPane(roomList);
        roomPanel.add(roomScrollPane, BorderLayout.CENTER);

        JPanel roomButtonPanel = new JPanel(new FlowLayout());
        JButton joinRoomBtn = new JButton("加入选中房间");
        joinRoomBtn.addActionListener(e -> joinSelectedRoom());
        roomButtonPanel.add(joinRoomBtn);
        roomPanel.add(roomButtonPanel, BorderLayout.SOUTH);

        // 房间列表双击监听
        roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        // 聊天区域
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("聊天室"));

        chatArea = new JTextArea(15, 20);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setPreferredSize(new Dimension(280, 300));

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatField = new JTextField();
        chatField.addActionListener(e -> sendChatMessage());
        JButton sendChatBtn = new JButton("发送");
        sendChatBtn.addActionListener(e -> sendChatMessage());

        chatInputPanel.add(chatField, BorderLayout.CENTER);
        chatInputPanel.add(sendChatBtn, BorderLayout.EAST);

        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        // 组装右侧面板
        rightPanel.add(roomPanel, BorderLayout.NORTH);
        rightPanel.add(chatPanel, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(700);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8080);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            statusLabel.setText("已连接到服务器");

            out.println("SET_NAME:" + playerName);

            new Thread(this::listenToServer).start();

            refreshRooms();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "无法连接到服务器: " + e.getMessage() + "\n请确保服务器已启动。",
                    "连接错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenToServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("收到服务器消息: " + message);
                handleServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("服务器连接断开");
                JOptionPane.showMessageDialog(this, "与服务器的连接已断开");
            });
        }
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(":");
            String command = parts[0];

            switch (command) {
                case "ROOM_CREATED":
                    int roomId = Integer.parseInt(parts[1]);
                    statusLabel.setText("房间创建成功: " + roomId + " - 等待其他玩家加入...");
                    break;

                case "ROOM_LIST":
                    roomListModel.clear();
                    if (!parts[1].equals("EMPTY")) {
                        String[] rooms = parts[1].split(",");
                        for (String room : rooms) {
                            String[] roomInfo = room.split("\\|");
                            if (roomInfo.length >= 2) {
                                roomListModel.addElement("房间 " + roomInfo[0] + " - " + roomInfo[1]);
                            } else {
                                roomListModel.addElement("房间 " + room);
                            }
                        }
                    }
                    break;

                case "JOIN_SUCCESS":
                    int joinedRoomId = Integer.parseInt(parts[1]);
                    myPlayerNumber = Integer.parseInt(parts[2]);
                    statusLabel.setText("成功加入房间: " + joinedRoomId + " - 您是玩家" + myPlayerNumber +
                            (myPlayerNumber == 1 ? "(黑棋)" : "(白棋)"));
                    break;

                case "JOIN_FAILED":
                    String errorMsg = parts[1];
                    JOptionPane.showMessageDialog(this,
                            "加入房间失败: " + errorMsg,
                            "加入失败",
                            JOptionPane.WARNING_MESSAGE);
                    break;

                case "GAME_START":
                    gameStarted = true;
                    if (myPlayerNumber == 1) {
                        statusLabel.setText("游戏开始! 您是黑棋，请先下子");
                    } else {
                        statusLabel.setText("游戏开始! 您是白棋，等待黑棋下子");
                    }
                    break;

                case "PLAYER_INFO":
                    String player1Name = parts[2];
                    String player2Name = parts[4];
                    chatArea.append("=== 对局开始 ===\n");
                    chatArea.append("玩家1(黑棋): " + player1Name + "\n");
                    chatArea.append("玩家2(白棋): " + player2Name + "\n");
                    chatArea.append("================\n");
                    break;

                case "MOVE":
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int player = Integer.parseInt(parts[3]);
                    boardPanel.placePiece(x, y, player);
                    break;

                case "TURN":
                    currentPlayer = Integer.parseInt(parts[1]);
                    if (currentPlayer == myPlayerNumber) {
                        statusLabel.setText("轮到您下棋 (" + (currentPlayer == 1 ? "黑棋" : "白棋") + ")");
                    } else {
                        statusLabel.setText("等待对手下棋...");
                    }
                    break;

                case "GAME_OVER":
                    int winner = Integer.parseInt(parts[1]);
                    gameStarted = false;

                    // 显示胜利弹窗
                    if (winner == 0) {
                        statusLabel.setText("游戏结束: 平局!");
                        chatArea.append("*** 游戏结束: 平局! ***\n");
                        JOptionPane.showMessageDialog(this,
                                "游戏结束！\n平局！",
                                "游戏结束",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (winner == myPlayerNumber) {
                        statusLabel.setText("游戏结束: 您赢了!");
                        chatArea.append("*** 恭喜您获胜! ***\n");
                        JOptionPane.showMessageDialog(this,
                                "恭喜！\n您获得了胜利！",
                                "胜利",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("游戏结束: 您输了!");
                        chatArea.append("*** 很遗憾，您输了 ***\n");
                        JOptionPane.showMessageDialog(this,
                                "很遗憾！\n您输了！",
                                "失败",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    break;

                case "CHAT":
                    int fromPlayer = Integer.parseInt(parts[1]);
                    String sender = parts[2];
                    String chatMsg = parts[3];
                    chatArea.append(sender + ": " + chatMsg + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    break;

                case "PLAYER_DISCONNECTED":
                    int disconnectedPlayer = Integer.parseInt(parts[1]);
                    chatArea.append("*** 玩家" + disconnectedPlayer + " 断开连接，游戏结束 ***\n");
                    gameStarted = false;
                    statusLabel.setText("对手断开连接");
                    JOptionPane.showMessageDialog(this,
                            "对手断开连接！\n游戏结束！",
                            "连接中断",
                            JOptionPane.WARNING_MESSAGE);
                    break;
            }
        });
    }

    private void createRoom() {
        if (out != null && !gameStarted) {
            out.println("CREATE_ROOM");
        }
    }

    private void refreshRooms() {
        if (out != null) {
            out.println("LIST_ROOMS");
        }
    }

    private void joinSelectedRoom() {
        int selectedIndex = roomList.getSelectedIndex();
        if (selectedIndex != -1 && out != null && !gameStarted) {
            String selected = roomListModel.get(selectedIndex);
            // 从 "房间 123 - 玩家名 - 状态" 中提取房间ID
            String[] parts = selected.split(" ");
            if (parts.length >= 2) {
                String roomIdStr = parts[1];
                try {
                    int roomId = Integer.parseInt(roomIdStr);
                    out.println("JOIN_ROOM:" + roomId);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "房间ID格式错误");
                }
            }
        } else if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一个房间");
        }
    }

    private void sendChatMessage() {
        String message = chatField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println("CHAT:" + message);
            chatField.setText("");
        }
    }

    private class BoardPanel extends JPanel {
        private static final int CELL_SIZE = 35;
        private static final int BOARD_MARGIN = 40;
        private int[][] boardState;

        public BoardPanel() {
            boardState = new int[Board.BOUND + 1][Board.BOUND + 1];

            int boardSize = 2 * BOARD_MARGIN + (Board.BOUND - 1) * CELL_SIZE;
            setPreferredSize(new Dimension(boardSize, boardSize));
            setBackground(new Color(220, 179, 92));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!gameStarted || currentPlayer != myPlayerNumber) {
                        JOptionPane.showMessageDialog(BoardPanel.this,
                                "现在不是您的回合！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    int x = Math.round((float)(e.getX() - BOARD_MARGIN) / CELL_SIZE) + 1;
                    int y = Math.round((float)(e.getY() - BOARD_MARGIN) / CELL_SIZE) + 1;

                    if (x >= 1 && x <= Board.BOUND && y >= 1 && y <= Board.BOUND) {
                        if (boardState[x][y] == 0) {
                            out.println("MAKE_MOVE:" + x + ":" + y);
                        } else {
                            JOptionPane.showMessageDialog(BoardPanel.this,
                                    "该位置已有棋子！", "错误", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            });
        }

        public void placePiece(int x, int y, int player) {
            if (x >= 1 && x <= Board.BOUND && y >= 1 && y <= Board.BOUND) {
                boardState[x][y] = player;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBoard(g);
            drawPieces(g);
        }

        private void drawBoard(Graphics g) {
            g.setColor(new Color(220, 179, 92));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.BLACK);
            for (int i = 0; i < Board.BOUND; i++) {
                int pos = BOARD_MARGIN + i * CELL_SIZE;
                g.drawLine(BOARD_MARGIN, pos,
                        BOARD_MARGIN + (Board.BOUND - 1) * CELL_SIZE, pos);
                g.drawLine(pos, BOARD_MARGIN,
                        pos, BOARD_MARGIN + (Board.BOUND - 1) * CELL_SIZE);
            }

            g.setColor(Color.BLACK);
            int[][] starPoints = {
                    {4, 4}, {4, 12}, {8, 8}, {12, 4}, {12, 12}
            };

            for (int[] point : starPoints) {
                int x = point[0];
                int y = point[1];
                int centerX = BOARD_MARGIN + (x - 1) * CELL_SIZE;
                int centerY = BOARD_MARGIN + (y - 1) * CELL_SIZE;
                g.fillOval(centerX - 3, centerY - 3, 6, 6);
            }
        }

        private void drawPieces(Graphics g) {
            for (int x = 1; x <= Board.BOUND; x++) {
                for (int y = 1; y <= Board.BOUND; y++) {
                    if (boardState[x][y] != 0) {
                        int centerX = BOARD_MARGIN + (x - 1) * CELL_SIZE;
                        int centerY = BOARD_MARGIN + (y - 1) * CELL_SIZE;

                        if (boardState[x][y] == 1) {
                            g.setColor(Color.BLACK);
                            g.fillOval(centerX - CELL_SIZE/2 + 1, centerY - CELL_SIZE/2 + 1,
                                    CELL_SIZE - 2, CELL_SIZE - 2);
                        } else if (boardState[x][y] == 2) {
                            g.setColor(Color.WHITE);
                            g.fillOval(centerX - CELL_SIZE/2 + 1, centerY - CELL_SIZE/2 + 1,
                                    CELL_SIZE - 2, CELL_SIZE - 2);
                            g.setColor(Color.BLACK);
                            g.drawOval(centerX - CELL_SIZE/2 + 1, centerY - CELL_SIZE/2 + 1,
                                    CELL_SIZE - 2, CELL_SIZE - 2);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NetworkGomokuGUI().setVisible(true);
        });
    }
}