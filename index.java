package System.com;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;

public class SystemStatusGUI extends JFrame {

    private static final String IP_PATTERN =
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private JTextField startIpField;
    private JTextField endIpField;
    private JButton searchButton;
    private JLabel loadingLabel;
    private JPanel outputPanel;
    private JScrollPane outputScrollPane;
    private JLabel logoLabel;
    private JLabel totalCountLabel;
    private JLabel onlineCountLabel;
    private JLabel offlineCountLabel;

    private int totalCount = 0;
    private int onlineCount = 0;
    private int offlineCount = 0;

    public SystemStatusGUI() {
        setTitle("System Status Checker");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create the input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5)); // Smaller gaps
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new RoundedBorder(10)); // Apply rounded border

        JLabel startIpLabel = new JLabel("Start IP:");
        JLabel endIpLabel = new JLabel("End IP:");

        startIpField = new JTextField();
        endIpField = new JTextField();
        searchButton = new JButton("Search");
        loadingLabel = new JLabel("Loading...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVisible(false);

        Dimension preferredSize = new Dimension(150, 25); // Reduced size
        startIpField.setPreferredSize(preferredSize);
        endIpField.setPreferredSize(preferredSize);
        searchButton.setPreferredSize(new Dimension(100, 25)); // Adjust button size

        Font smallerFont = new Font("Arial", Font.PLAIN, 12); // Smaller font size
        startIpField.setFont(smallerFont);
        endIpField.setFont(smallerFont);
        searchButton.setFont(smallerFont);
        loadingLabel.setFont(smallerFont);

        inputPanel.add(startIpLabel);
        inputPanel.add(startIpField);
        inputPanel.add(endIpLabel);
        inputPanel.add(endIpField);
        inputPanel.add(searchButton);

        // Add a logo
        logoLabel = new JLabel();
        logoLabel.setOpaque(true);
        logoLabel.setBackground(Color.BLACK);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            URL logoUrl = new URL("https://www.innogeecks.com/wp-content/uploads/2020/06/Logo_with_title_without_technologies_white.png");
            ImageIcon logoIcon = new ImageIcon(logoUrl);
            logoLabel.setIcon(logoIcon);
        } catch (IOException e) {
            logoLabel.setText("Logo not available");
        }

        // Initialize JPanel for output with GridLayout
        outputPanel = new JPanel();
        outputPanel.setLayout(new GridLayout(0, 8, 5, 5)); // 8 columns, variable rows

        outputScrollPane = new JScrollPane(outputPanel);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Results"));
        outputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

       
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(Color.WHITE); // Set background color to white
        statusPanel.setBorder(new RoundedBorder(10)); // Apply rounded border

        totalCountLabel = new JLabel("Total Systems: 0");
        onlineCountLabel = new JLabel("Online Systems: 0");
        offlineCountLabel = new JLabel("Offline Systems: 0");

        Font boldFont = new Font("Arial", Font.BOLD, 14); // Bold font size
        totalCountLabel.setFont(boldFont);
        onlineCountLabel.setFont(boldFont);
        offlineCountLabel.setFont(boldFont);

        // Set text color for each label
        totalCountLabel.setForeground(Color.BLUE); 
        onlineCountLabel.setForeground(Color.GREEN); 
        offlineCountLabel.setForeground(Color.RED);

        statusPanel.add(totalCountLabel);
        statusPanel.add(onlineCountLabel);
        statusPanel.add(offlineCountLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(logoLabel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(loadingLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);
        add(outputScrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> handleSearchAction());
    }

    private void handleSearchAction() {
        String startIp = startIpField.getText().trim();
        String endIp = endIpField.getText().trim();

        if (!isValidIp(startIp) || !isValidIp(endIp)) {
            JOptionPane.showMessageDialog(this,
                "Invalid IP address format. Please provide valid IP addresses.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        int start = ipToInt(startIp);
        int end = ipToInt(endIp);

        if (start > end) {
            JOptionPane.showMessageDialog(this,
                "Start IP must be less than or equal to End IP.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        totalCount = end - start + 1;
        onlineCount = 0;
        offlineCount = 0;

        loadingLabel.setVisible(true);
        outputPanel.removeAll(); // Clear previous results
        new SystemStatusWorker(start, end).execute();
    }

    private class SystemStatusWorker extends SwingWorker<Void, JButton> {
        private final int start;
        private final int end;

        public SystemStatusWorker(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Void doInBackground() {
            for (int ip = start; ip <= end; ip++) {
                String ipAddress = intToIp(ip);
                String systemName = "System" + (ip - start + 1);
                boolean reachable = pingHost(ipAddress);
                String macAddress = getMacAddress(ipAddress);

                boolean isOnline = reachable || macAddress != null;
                String statusText = isOnline ? "Online" : "Offline";
                String statusColor = isOnline ? "green" : "red";

                JButton button = new JButton("<html><div><b>" + systemName + "</b></div>"
                        + "<div>Status: <font color='" + statusColor + "'>" + statusText + "</font></div></html>");

                // Improve visibility with white background
                button.setFont(new Font("Arial", Font.BOLD, 14)); // Larger, bold font
                button.setForeground(Color.BLACK); // Black text color for contrast on white background
                button.setBackground(Color.WHITE); // White background color
                button.setMargin(new Insets(10, 10, 10, 10)); // Padding around text
                button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1)); // Black border for definition

                
                String tooltipText = "<html><body style='background-color: black; color: white; font-family: Arial, sans-serif; font-size: 12px;'>"
                                   + "<b>IP Address:</b> " + ipAddress + "<br>"
                                   + "<b>MAC Address:</b> " + (macAddress != null ? macAddress.toUpperCase() : "N/A")
                                   + "</body></html>";
                button.setToolTipText(tooltipText);

               
                button.setPreferredSize(new Dimension(150, 150)); 

                publish(button);

                if (isOnline) {
                    onlineCount++;
                } else {
                    offlineCount++;
                }
            }
            return null;
        }

        @Override
        protected void process(java.util.List<JButton> chunks) {
            for (JButton button : chunks) {
                outputPanel.add(button);
            }
            outputPanel.revalidate();
            outputPanel.repaint();
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                loadingLabel.setVisible(false);

                // Update status labels
                totalCountLabel.setText("Total Systems: " + totalCount);
                onlineCountLabel.setText("Online Systems: " + onlineCount);
                offlineCountLabel.setText("Offline Systems: " + offlineCount);
            }
        }
    }

    private boolean isValidIp(String ip) {
        if (ip == null) return false;
        Pattern pattern = Pattern.compile(IP_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    private boolean pingHost(String ipAddress) {
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            return inet.isReachable(2000);
        } catch (IOException e) {
            return false;
        }
    }

    private String getMacAddress(String ipAddress) {
        String macAddress = null;
        try {
            Process process = Runtime.getRuntime().exec("arp -a " + ipAddress);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(ipAddress)) {
                    int index = line.indexOf(ipAddress) + ipAddress.length();
                    macAddress = line.substring(index).trim().split(" ")[0];
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return macAddress;
    }

    private int ipToInt(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        int ipInt = 0;
        for (int i = 0; i < octets.length; i++) {
            ipInt |= (Integer.parseInt(octets[i]) << (24 - (8 * i)));
        }
        return ipInt;
    }

    private String intToIp(int ipInt) {
        return ((ipInt >> 24) & 0xFF) + "." +
               ((ipInt >> 16) & 0xFF) + "." +
               ((ipInt >> 8) & 0xFF) + "." +
               (ipInt & 0xFF);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SystemStatusGUI().setVisible(true));
    }

    // Custom border with rounded corners
    class RoundedBorder extends AbstractBorder {
        private int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));

            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 2, radius + 2, radius + 2, radius + 2);
        }
    }
}
