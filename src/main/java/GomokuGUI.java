import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

public class GomokuGUI extends JFrame {
    private Board board;
    private GamePanel gamePanel;
    private JLabel statusLabel;
    private JButton undoButton;
    private JButton restartButton;
    private boolean gameEnded = false;

    public GomokuGUI(Board board) {
        this.board = board;
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("五子棋游戏 - 图形界面版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        statusLabel = new JLabel("游戏开始 - 白棋(○)先行");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel();
        undoButton = new JButton("悔棋");
        restartButton = new JButton("重新开始");

        undoButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        restartButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        undoButton.addActionListener(e -> undoMove());
        restartButton.addActionListener(e -> restartGame());

        controlPanel.add(undoButton);
        controlPanel.add(restartButton);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void undoMove() {
        if (gameEnded) return;
        if (!board.hasHistory()) {
            JOptionPane.showMessageDialog(this, "无法悔棋，没有历史记录！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        board.withdrawLast();
        gamePanel.repaint();
        updateStatus();
    }

    private void restartGame() {
        // 直接重新开始，不需要确认对话框
        this.board = new Board();
        this.gameEnded = false;
        gamePanel.repaint();
        updateStatus();
    }

    private void updateStatus() {
        if (board.hasWinner()) {
            String winner = board.getWinner() == 1 ? "白棋(○)" : "黑棋(●)";
            statusLabel.setText("游戏结束 - " + winner + "获胜！");
            statusLabel.setForeground(Color.RED);
            if (!gameEnded) {
                gameEnded = true;
                showGameResult();
            }
        } else if (board.isADraw()) {
            statusLabel.setText("游戏结束 - 平局！");
            statusLabel.setForeground(Color.BLUE);
            if (!gameEnded) {
                gameEnded = true;
                showGameResult();
            }
        } else {
            String currentPlayer = board.getCurrentPlayer() == 1 ? "白棋(○)" : "黑棋(●)";
            statusLabel.setText("当前回合：" + currentPlayer);
            statusLabel.setForeground(Color.BLACK);
        }
    }

    private class GamePanel extends JPanel {
        private static final int CELL_SIZE = 40;
        private static final int MARGIN = 30;
        private static final int BOARD_SIZE = Board.BOUND;

        public GamePanel() {
            setPreferredSize(new Dimension(
                    (BOARD_SIZE - 1) * CELL_SIZE + 2 * MARGIN,
                    (BOARD_SIZE - 1) * CELL_SIZE + 2 * MARGIN
            ));
            setBackground(new Color(220, 179, 92));

            addMouseListener(new GameMouseListener());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawBoard(g);
            drawPieces(g);
        }

        private void drawBoard(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(220, 179, 92));
            g.fillRect(MARGIN, MARGIN,
                    (BOARD_SIZE - 1) * CELL_SIZE,
                    (BOARD_SIZE - 1) * CELL_SIZE);

            g.setColor(Color.BLACK);
            for (int i = 0; i < BOARD_SIZE; i++) {
                g.drawLine(MARGIN, MARGIN + i * CELL_SIZE,
                        MARGIN + (BOARD_SIZE - 1) * CELL_SIZE,
                        MARGIN + i * CELL_SIZE);
                g.drawLine(MARGIN + i * CELL_SIZE, MARGIN,
                        MARGIN + i * CELL_SIZE,
                        MARGIN + (BOARD_SIZE - 1) * CELL_SIZE);
            }

            drawStar(g, 4, 4);
            drawStar(g, 4, 12);
            drawStar(g, 8, 8);
            drawStar(g, 12, 4);
            drawStar(g, 12, 12);
        }

        private void drawStar(Graphics g, int x, int y) {
            int centerX = MARGIN + (x - 1) * CELL_SIZE;
            int centerY = MARGIN + (y - 1) * CELL_SIZE;
            g.setColor(Color.BLACK);
            g.fillOval(centerX - 4, centerY - 4, 8, 8);
        }

        private void drawPieces(Graphics g) {
            for (int x = 1; x <= BOARD_SIZE; x++) {
                for (int y = 1; y <= BOARD_SIZE; y++) {
                    int piece = board.getPiece(x, y);
                    if (piece != 0) {
                        drawPiece(g, x, y, piece == -1);
                    }
                }
            }
        }

        private void drawPiece(Graphics g, int x, int y, boolean isBlack) {
            int centerX = MARGIN + (x - 1) * CELL_SIZE;
            int centerY = MARGIN + (y - 1) * CELL_SIZE;
            int radius = CELL_SIZE / 2 - 2;

            Graphics2D g2d = (Graphics2D) g;
            if (isBlack) {
                GradientPaint gradient = new GradientPaint(
                        centerX - radius, centerY - radius, Color.DARK_GRAY,
                        centerX + radius, centerY + radius, Color.BLACK
                );
                g2d.setPaint(gradient);
            } else {
                GradientPaint gradient = new GradientPaint(
                        centerX - radius, centerY - radius, Color.WHITE,
                        centerX + radius, centerY + radius, Color.LIGHT_GRAY
                );
                g2d.setPaint(gradient);
            }

            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            if (!isBlack) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
        }

        private class GameMouseListener extends MouseAdapter {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (board.hasWinner() || board.isADraw() || gameEnded) {
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                if (x < MARGIN || y < MARGIN ||
                        x >= MARGIN + (BOARD_SIZE - 1) * CELL_SIZE ||
                        y >= MARGIN + (BOARD_SIZE - 1) * CELL_SIZE) {
                    return;
                }

                int boardX = Math.round((float)(x - MARGIN) / CELL_SIZE) + 1;
                int boardY = Math.round((float)(y - MARGIN) / CELL_SIZE) + 1;

                boardX = Math.max(1, Math.min(BOARD_SIZE, boardX));
                boardY = Math.max(1, Math.min(BOARD_SIZE, boardY));

                if (!board.hasPiece(boardX, boardY)) {
                    board.placePiece(boardX, boardY);
                    repaint();
                    updateStatus();
                }
            }
        }
    }

    private void showGameResult() {
        String message;
        if (board.hasWinner()) {
            String winner = board.getWinner() == 1 ? "白棋(○)" : "黑棋(●)";
            message = "恭喜 " + winner + " 获胜！\n是否开始新游戏？";
        } else {
            message = "本局平局！\n是否开始新游戏？";
        }

        int result = JOptionPane.showConfirmDialog(this,
                message, "游戏结束",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            restartGame(); // 这里直接重新开始，不会有第二次确认
        }
    }

    public static void main(String[] args) {

        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        Font globalFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14); 

        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(globalFont));
            }
        }
        SwingUtilities.invokeLater(() -> {
            Board board = new Board();
            GomokuGUI game = new GomokuGUI(board);
            game.setVisible(true);
        });
    }
}