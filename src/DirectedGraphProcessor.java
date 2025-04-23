import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DirectedGraphProcessor {
    // 图结构定义
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private JFrame mainFrame;
    private JTextArea outputArea;
    private JTextField word1Field;
    private JTextField word2Field;
    private JTextField newTextField;
    private String currentFileName = "";

    // 主程序入口
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            DirectedGraphProcessor app = new DirectedGraphProcessor();
            app.createAndShowGUI();

            // 如果有命令行参数，则尝试读取文件
            if (args.length > 0) {
                app.loadFile(new File(args[0]));
            }
        });
    }

    private void createAndShowGUI() {
        // 创建主窗口
        mainFrame = new JFrame("Directed Graph Processor");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建功能区域
        JPanel functionPanel = createFunctionPanel();
        JPanel outputPanel = createOutputPanel();

        mainPanel.add(functionPanel, BorderLayout.NORTH);
        mainPanel.add(outputPanel, BorderLayout.CENTER);

        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }

    private JPanel createFunctionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 文件选择区域
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openButton = new JButton("Open File");
        JLabel fileLabel = new JLabel("No file selected");

        openButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
            int result = fileChooser.showOpenDialog(mainFrame);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                loadFile(selectedFile);
                fileLabel.setText(selectedFile.getName());
            }
        });

        filePanel.add(openButton);
        filePanel.add(fileLabel);

        // 功能按钮区域
        JPanel buttonPanel = new JPanel(new GridLayout(3, 3, 5, 5));

        JButton showGraphButton = new JButton("Show Graph");
        JButton bridgeWordsButton = new JButton("Query Bridge Words");
        JButton generateTextButton = new JButton("Generate New Text");
        JButton shortestPathButton = new JButton("Calculate Shortest Path");
        JButton pageRankButton = new JButton("Calculate PageRank");
        JButton randomWalkButton = new JButton("Random Walk");
        JButton visualizeButton = new JButton("Visualize Graph");
        JButton allShortestPathsButton = new JButton("All Shortest Paths");
        JButton saveButton = new JButton("Save Results");

        // 添加按钮点击事件
        showGraphButton.addActionListener(e -> showDirectedGraph());

        bridgeWordsButton.addActionListener(e -> {
            String word1 = word1Field.getText().trim().toLowerCase();
            String word2 = word2Field.getText().trim().toLowerCase();
            String result = queryBridgeWords(word1, word2);
            outputArea.setText(result);
        });

        generateTextButton.addActionListener(e -> {
            String inputText = newTextField.getText();
            String result = generateNewText(inputText);
            outputArea.setText("Original text: " + inputText + "\n\nGenerated text: " + result);
        });

        shortestPathButton.addActionListener(e -> {
            String word1 = word1Field.getText().trim().toLowerCase();
            String word2 = word2Field.getText().trim().toLowerCase();
            String result = calcShortestPath(word1, word2);
            outputArea.setText(result);
        });

        pageRankButton.addActionListener(e -> {
            String word = word1Field.getText().trim().toLowerCase();
            if (word.isEmpty()) {
                outputArea.setText("Please enter a word to calculate PageRank");
                return;
            }
            Double rank = calcPageRank(word);
            if (rank == null) {
                outputArea.setText("Word '" + word + "' not found in the graph.");
            } else {
                outputArea.setText("PageRank of '" + word + "' is: " + String.format("%.4f", rank));

                // 显示所有单词的PageRank值（按值从大到小排序）
                outputArea.append("\n\nPageRank values of all words (sorted):\n");
                Map<String, Double> allRanks = new HashMap<>();
                for (String w : graph.keySet()) {
                    allRanks.put(w, calcPageRank(w));
                }

                allRanks.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .forEach(entry -> outputArea.append(entry.getKey() + ": " +
                                String.format("%.4f", entry.getValue()) + "\n"));
            }
        });

        randomWalkButton.addActionListener(e -> {
            String result = randomWalk();
            outputArea.setText("Random Walk Result:\n" + result);
        });

        visualizeButton.addActionListener(e -> {
            try {
                visualizeGraph();
                outputArea.setText("Graph visualization created and saved as 'graph.png'");
            } catch (Exception ex) {
                outputArea.setText("Failed to visualize graph: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        allShortestPathsButton.addActionListener(e -> {
            String word1 = word1Field.getText().trim().toLowerCase();
            String word2 = word2Field.getText().trim().toLowerCase();

            if (word1.isEmpty()) {
                outputArea.setText("Please enter the first word");
                return;
            }

            if (word2.isEmpty()) {
                // 如果只输入一个单词，计算到所有其他单词的最短路径
                outputArea.setText("Shortest paths from '" + word1 + "' to all other words:\n\n");

                if (!graph.containsKey(word1)) {
                    outputArea.setText("Word '" + word1 + "' not found in the graph.");
                    return;
                }

                for (String target : graph.keySet()) {
                    if (!target.equals(word1)) {
                        String path = calcShortestPath(word1, target);
                        if (!path.contains("No path")) {
                            outputArea.append(path + "\n\n");
                        }
                    }
                }
            } else {
                // 计算两个单词之间的所有最短路径
                List<List<String>> allPaths = findAllShortestPaths(word1, word2);

                if (allPaths.isEmpty()) {
                    outputArea.setText("No path found from '" + word1 + "' to '" + word2 + "'");
                } else {
                    outputArea.setText("All shortest paths from '" + word1 + "' to '" + word2 + "':\n\n");
                    for (int i = 0; i < allPaths.size(); i++) {
                        List<String> path = allPaths.get(i);
                        outputArea.append((i+1) + ". " + String.join(" -> ", path) + "\n");
                    }
                }
            }
        });

        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Results");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));

            int userSelection = fileChooser.showSaveDialog(mainFrame);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
                }

                try (PrintWriter writer = new PrintWriter(fileToSave)) {
                    writer.write(outputArea.getText());
                    JOptionPane.showMessageDialog(mainFrame,
                            "Results saved successfully to " + fileToSave.getName(),
                            "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Error saving file: " + ex.getMessage(),
                            "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        buttonPanel.add(showGraphButton);
        buttonPanel.add(bridgeWordsButton);
        buttonPanel.add(generateTextButton);
        buttonPanel.add(shortestPathButton);
        buttonPanel.add(pageRankButton);
        buttonPanel.add(randomWalkButton);
        buttonPanel.add(visualizeButton);
        buttonPanel.add(allShortestPathsButton);
        buttonPanel.add(saveButton);

        // 输入字段区域
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        JLabel word1Label = new JLabel("Word 1:");
        word1Field = new JTextField(20);

        JLabel word2Label = new JLabel("Word 2:");
        word2Field = new JTextField(20);

        JLabel newTextLabel = new JLabel("New Text:");
        newTextField = new JTextField(20);

        inputPanel.add(word1Label);
        inputPanel.add(word1Field);
        inputPanel.add(word2Label);
        inputPanel.add(word2Field);
        inputPanel.add(newTextLabel);
        inputPanel.add(newTextField);

        panel.add(filePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(inputPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Output"));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadFile(File file) {
        try {
            currentFileName = file.getName();
            outputArea.setText("Loading file: " + file.getName() + "\n");

            // 读取文件内容
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
            reader.close();

            // 处理文本，生成图
            String text = content.toString().toLowerCase();
            text = text.replaceAll("[^a-z ]", " ");  // 移除非字母字符
            text = text.replaceAll("\\s+", " ").trim();  // 规范化空格

            String[] words = text.split(" ");

            graph.clear();  // 清除现有图

            // 构建有向图
            for (int i = 0; i < words.length - 1; i++) {
                String currentWord = words[i];
                String nextWord = words[i + 1];

                // 跳过空字符串
                if (currentWord.isEmpty() || nextWord.isEmpty()) continue;

                // 如果当前单词不在图中，添加它
                if (!graph.containsKey(currentWord)) {
                    graph.put(currentWord, new HashMap<>());
                }

                // 更新边的权重
                Map<String, Integer> neighbors = graph.get(currentWord);
                neighbors.put(nextWord, neighbors.getOrDefault(nextWord, 0) + 1);
            }

            // 确保最后一个单词也在图中
            if (words.length > 0 && !words[words.length - 1].isEmpty()) {
                String lastWord = words[words.length - 1];
                if (!graph.containsKey(lastWord)) {
                    graph.put(lastWord, new HashMap<>());
                }
            }

            outputArea.append("File loaded successfully.\n");
            outputArea.append("Graph created with " + graph.size() + " nodes.\n");
            outputArea.append("Use the buttons above to analyze the graph.\n");

        } catch (Exception e) {
            outputArea.setText("Error loading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 展示有向图
    public void showDirectedGraph() {
        if (graph.isEmpty()) {
            outputArea.setText("No graph to display. Please load a file first.");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("Directed Graph (").append(graph.size()).append(" nodes):\n\n");

        // 显示所有节点
        result.append("Nodes: ");
        result.append(String.join(", ", graph.keySet()));
        result.append("\n\nEdges:\n");

        // 显示所有边
        List<String> edges = new ArrayList<>();
        for (String source : graph.keySet()) {
            for (Map.Entry<String, Integer> target : graph.get(source).entrySet()) {
                edges.add(source + " -> " + target.getKey() + " (weight: " + target.getValue() + ")");
            }
        }

        // 按字母顺序排序边
        Collections.sort(edges);
        for (String edge : edges) {
            result.append(edge).append("\n");
        }

        outputArea.setText(result.toString());
    }

    // 查询桥接词
    public String queryBridgeWords(String word1, String word2) {
        if (graph.isEmpty()) {
            return "No graph exists. Please load a file first.";
        }

        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        // 检查两个单词是否都在图中
        if (!graph.containsKey(word1) && !graph.containsKey(word2)) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        } else if (!graph.containsKey(word1)) {
            return "No \"" + word1 + "\" in the graph!";
        } else if (!graph.containsKey(word2)) {
            return "No \"" + word2 + "\" in the graph!";
        }

        // 查找所有桥接词
        List<String> bridgeWords = new ArrayList<>();

        // 遍历从word1出发的所有边
        for (String bridge : graph.get(word1).keySet()) {
            // 检查从bridge是否有到word2的边
            if (graph.containsKey(bridge) && graph.get(bridge).containsKey(word2)) {
                bridgeWords.add(bridge);
            }
        }

        // 根据桥接词数量返回相应结果
        if (bridgeWords.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        } else if (bridgeWords.size() == 1) {
            return "The bridge word from \"" + word1 + "\" to \"" + word2 + "\" is: \"" + bridgeWords.get(0) + "\".";
        } else {
            StringBuilder result = new StringBuilder("The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: ");
            for (int i = 0; i < bridgeWords.size() - 1; i++) {
                result.append("\"").append(bridgeWords.get(i)).append("\", ");
            }
            result.append("and \"").append(bridgeWords.get(bridgeWords.size() - 1)).append("\".");
            return result.toString();
        }
    }

    // 根据bridge word生成新文本
    public String generateNewText(String inputText) {
        if (graph.isEmpty()) {
            return "No graph exists. Please load a file first.";
        }

        // 处理输入文本
        String cleanText = inputText.toLowerCase();
        cleanText = cleanText.replaceAll("[^a-z ]", " ");
        cleanText = cleanText.replaceAll("\\s+", " ").trim();

        String[] words = cleanText.split(" ");
        if (words.length <= 1) {
            return inputText; // 如果只有0或1个单词，则直接返回原文本
        }

        StringBuilder result = new StringBuilder();
        result.append(words[0]);

        // 遍历词对，寻找并插入桥接词
        for (int i = 0; i < words.length - 1; i++) {
            String currentWord = words[i];
            String nextWord = words[i + 1];

            // 获取桥接词
            List<String> bridges = new ArrayList<>();
            if (graph.containsKey(currentWord)) {
                for (String bridge : graph.get(currentWord).keySet()) {
                    if (graph.containsKey(bridge) && graph.get(bridge).containsKey(nextWord)) {
                        bridges.add(bridge);
                    }
                }
            }

            // 插入桥接词（如果存在）
            if (!bridges.isEmpty()) {
                // 随机选择一个桥接词
                String selectedBridge = bridges.get(new Random().nextInt(bridges.size()));
                result.append(" ").append(selectedBridge);
            }

            // 添加下一个词
            result.append(" ").append(nextWord);
        }

        return result.toString();
    }

    // 计算两个单词之间的最短路径
    public String calcShortestPath(String word1, String word2) {
        if (graph.isEmpty()) {
            return "No graph exists. Please load a file first.";
        }

        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        // 检查两个单词是否都在图中
        if (!graph.containsKey(word1)) {
            return "Word \"" + word1 + "\" is not in the graph!";
        }
        if (!graph.containsKey(word2)) {
            return "Word \"" + word2 + "\" is not in the graph!";
        }

        // 使用Dijkstra算法计算最短路径
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));
        Set<String> visited = new HashSet<>();

        // 初始化距离
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(word1, 0);
        queue.add(word1);

        // Dijkstra算法主循环
        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(word2)) {
                break; // 找到目标单词，停止搜索
            }

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            // 遍历当前节点的所有邻居
            for (Map.Entry<String, Integer> edge : graph.get(current).entrySet()) {
                String neighbor = edge.getKey();
                int weight = edge.getValue();

                int newDistance = distances.get(current) + weight;
                if (newDistance < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // 重建路径
        if (!previous.containsKey(word2) && !word1.equals(word2)) {
            return "No path from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        // 特殊情况：源和目标相同
        if (word1.equals(word2)) {
            return "Path from \"" + word1 + "\" to \"" + word2 + "\": " + word1 + "\nPath length: 0";
        }

        List<String> path = new ArrayList<>();
        String current = word2;

        // 从目标向源回溯
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }

        // 反转路径（从源到目标）
        Collections.reverse(path);

        StringBuilder result = new StringBuilder();
        result.append("Path from \"").append(word1).append("\" to \"").append(word2).append("\": ");
        result.append(String.join(" -> ", path));
        result.append("\nPath length: ").append(distances.get(word2));

        return result.toString();
    }

    // 查找所有最短路径
    public List<List<String>> findAllShortestPaths(String word1, String word2) {
        List<List<String>> allPaths = new ArrayList<>();
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (graph.isEmpty() || !graph.containsKey(word1) || !graph.containsKey(word2)) {
            return allPaths; // 返回空列表
        }

        // BFS找最短距离
        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        // 初始化
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
            predecessors.put(node, new ArrayList<>());
        }

        distances.put(word1, 0);
        queue.add(word1);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            // 对于当前节点的所有邻居
            for (Map.Entry<String, Integer> edge : graph.get(current).entrySet()) {
                String neighbor = edge.getKey();

                // 找到一条新的最短路径
                if (distances.get(neighbor) > distances.get(current) + 1) {
                    distances.put(neighbor, distances.get(current) + 1);

                    // 清除之前的前驱节点
                    predecessors.get(neighbor).clear();
                    predecessors.get(neighbor).add(current);
                    queue.add(neighbor);
                }
                // 找到另一条相同长度的路径
                else if (distances.get(neighbor) == distances.get(current) + 1) {
                    predecessors.get(neighbor).add(current);
                }
            }
        }

        // 构建所有路径
        buildPaths(word1, word2, predecessors, new ArrayList<>(Collections.singletonList(word2)), allPaths);

        return allPaths;
    }

    private void buildPaths(String start, String current, Map<String, List<String>> predecessors,
                            List<String> path, List<List<String>> allPaths) {
        if (current.equals(start)) {
            // 反转路径并添加到结果中
            List<String> completePath = new ArrayList<>(path);
            Collections.reverse(completePath);
            allPaths.add(completePath);
            return;
        }

        // 对于当前节点的每个前驱
        for (String predecessor : predecessors.get(current)) {
            path.add(predecessor);
            buildPaths(start, predecessor, predecessors, new ArrayList<>(path), allPaths);
            path.remove(path.size() - 1);
        }
    }

    // 计算PageRank值
    public Double calcPageRank(String word) {
        if (graph.isEmpty()) {
            return null;
        }

        word = word.toLowerCase();

        if (!graph.containsKey(word)) {
            return null;
        }

        // 设置参数
        double d = 0.85; // 阻尼系数
        int iterations = 100; // 迭代次数
        double epsilon = 1e-8; // 收敛阈值

        // 初始化PageRank值
        Map<String, Double> pageRank = new HashMap<>();
        Map<String, Double> newPageRank = new HashMap<>();

        // 初始化为均匀分布
        double initialRank = 1.0 / graph.size();
        for (String node : graph.keySet()) {
            pageRank.put(node, initialRank);
        }

        // 计算所有节点的出度
        Map<String, Integer> outDegrees = new HashMap<>();
        for (String node : graph.keySet()) {
            int outDegree = 0;
            for (Integer weight : graph.get(node).values()) {
                outDegree += weight;
            }
            outDegrees.put(node, outDegree);
        }

        // PageRank迭代计算
        boolean converged = false;
        for (int i = 0; i < iterations && !converged; i++) {
            double sumDifference = 0.0;

            for (String node : graph.keySet()) {
                double sum = 0.0;

                // 计算指向当前节点的所有节点的贡献
                for (String source : graph.keySet()) {
                    if (graph.get(source).containsKey(node)) {
                        // 边的权重作为贡献因子
                        int weight = graph.get(source).get(node);
                        int outDegree = outDegrees.get(source);
                        if (outDegree > 0) {
                            sum += pageRank.get(source) * weight / outDegree;
                        }
                    }
                }

                // 计算新的PageRank值
                double newRank = (1 - d) / graph.size() + d * sum;
                newPageRank.put(node, newRank);

                // 计算与上一次迭代的差异
                sumDifference += Math.abs(newRank - pageRank.get(node));
            }

            // 更新PageRank值
            pageRank = new HashMap<>(newPageRank);

            // 检查是否收敛
            if (sumDifference < epsilon) {
                converged = true;
            }
        }

        // 归一化PageRank值
        double sum = pageRank.values().stream().mapToDouble(Double::doubleValue).sum();
        for (String node : pageRank.keySet()) {
            pageRank.put(node, pageRank.get(node) / sum);
        }

        return pageRank.get(word);
    }

    // 随机游走
    public String randomWalk() {
        if (graph.isEmpty()) {
            return "No graph exists. Please load a file first.";
        }

        // 随机选择起始节点
        List<String> nodes = new ArrayList<>(graph.keySet());
        String currentNode = nodes.get(new Random().nextInt(nodes.size()));

        List<String> path = new ArrayList<>();
        path.add(currentNode);

        Set<String> visitedEdges = new HashSet<>();

        while (true) {
            // 获取当前节点的所有出边
            Map<String, Integer> outEdges = graph.get(currentNode);

            if (outEdges.isEmpty()) {
                // 如果没有出边，结束游走
                break;
            }

            // 构建边的权重列表
            List<String> neighbors = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();

            for (Map.Entry<String, Integer> edge : outEdges.entrySet()) {
                String neighbor = edge.getKey();
                int weight = edge.getValue();

                for (int i = 0; i < weight; i++) {
                    neighbors.add(neighbor);
                }
                weights.add(weight);
            }

            if (neighbors.isEmpty()) {
                break;
            }

            // 随机选择下一个节点（根据权重）
            String nextNode = neighbors.get(new Random().nextInt(neighbors.size()));

            // 构建边标识
            String edge = currentNode + " -> " + nextNode;

            // 检查是否重复访问边
            if (visitedEdges.contains(edge)) {
                break;
            }

            // 添加边到已访问集合
            visitedEdges.add(edge);

            // 更新当前节点和路径
            currentNode = nextNode;
            path.add(currentNode);
        }

        // 保存随机游走结果到文件
        try {
            FileWriter writer = new FileWriter("random_walk.txt");
            String result = String.join(" ", path);
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            return "Error saving random walk to file: " + e.getMessage();
        }

        return String.join(" ", path);
    }

    // 可视化图结构（使用简单的文本形式表示）
    private void visualizeGraph() throws IOException {
        if (graph.isEmpty()) {
            throw new IllegalStateException("No graph to visualize");
        }

        // 这里使用简单的文本方式来可视化
        // 在实际应用中可以集成第三方库如GraphViz或JGraphX来生成图像

        StringBuilder dot = new StringBuilder();
        dot.append("digraph G {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=circle, style=filled, fillcolor=lightblue];\n");

        // 添加所有边
        for (String source : graph.keySet()) {
            for (Map.Entry<String, Integer> edge : graph.get(source).entrySet()) {
                String target = edge.getKey();
                int weight = edge.getValue();
                dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\", weight=%d];\n",
                        source, target, weight, weight));
            }
        }

        dot.append("}\n");

        // 将DOT格式保存到文件
        try (FileWriter writer = new FileWriter("graph.dot")) {
            writer.write(dot.toString());
        }

        // 尝试使用GraphViz渲染（如果系统中安装了）
        try {
            Process process = Runtime.getRuntime().exec("dot -Tpng -o graph.png graph.dot");
            process.waitFor();
        } catch (Exception e) {
            // 如果GraphViz不可用，只保存DOT文件
            System.err.println("GraphViz not available: " + e.getMessage());
            throw new IOException("Could not generate graph visualization. GraphViz might not be installed.");
        }
    }
}

