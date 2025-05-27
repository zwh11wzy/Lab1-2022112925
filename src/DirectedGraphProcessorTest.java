import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*; // Required for JTextArea
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DirectedGraphProcessorTest {

    private DirectedGraphProcessor graphProcessor;
    private static final String TEST_FILE_NAME = "Easy_Test_JUnit.txt";

    private static void createTestFile() throws IOException {
        File testFile = new File(TEST_FILE_NAME);
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("The scientist carefully analyzed the data, wrote a detailed report, and shared the report with the team, but the team requested more data, so the scientist analyzed it again.");
        }
    }

    private static void deleteTestFile() {
        try {
            Files.deleteIfExists(Paths.get(TEST_FILE_NAME));
        } catch (IOException e) {
            System.err.println("Failed to delete test file: " + e.getMessage());
        }
    }

    @BeforeAll
    static void setUpAll() throws IOException {
        createTestFile();
    }

    @AfterAll
    static void tearDownAll() {
        deleteTestFile();
    }

    @BeforeEach
    void setUp() {
        graphProcessor = new DirectedGraphProcessor();
        File testFile = new File(TEST_FILE_NAME);

        // **FIX: Initialize outputArea using reflection before calling loadFile**
        try {
            Field outputAreaField = DirectedGraphProcessor.class.getDeclaredField("outputArea");
            outputAreaField.setAccessible(true);
            outputAreaField.set(graphProcessor, new JTextArea()); // Assign a new JTextArea instance
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Critical Warning: Could not initialize 'outputArea' via reflection. Tests will likely fail.");
            e.printStackTrace();
            // Optionally, fail the setup explicitly if this is critical
            // fail("Failed to set up outputArea, cannot proceed with tests.");
        }

        // Load the graph using reflection for the private loadFile method
        if (testFile.exists()) {
            try {
                Method loadFileMethod = DirectedGraphProcessor.class.getDeclaredMethod("loadFile", File.class);
                loadFileMethod.setAccessible(true); // Make private method accessible
                loadFileMethod.invoke(graphProcessor, testFile);
            } catch (Exception e) {
                System.err.println("Warning: Could not call loadFile via reflection or an error occurred within loadFile.");
                // Check if the cause is the NPE we are trying to fix, or something else
                if (e.getCause() instanceof NullPointerException) {
                    System.err.println("NPE occurred during loadFile, check 'outputArea' initialization: " + e.getCause().getMessage());
                }
                e.printStackTrace();
            }
        } else {
            System.err.println("Test file " + TEST_FILE_NAME + " not found. Graph will be empty for tests.");
        }
    }

    // --- Tests for queryBridgeWords (as per your report sections 5 & 6) ---

    @Test
    void TC_BW_01_queryBridgeWords_oneBridge() {
        assertNotNull(graphProcessor, "GraphProcessor instance should not be null.");
        String result = graphProcessor.queryBridgeWords("analyzed", "data");
        assertEquals("The bridge word from \"analyzed\" to \"data\" is: \"the\".", result, "Test Case BW_01 Failed: One bridge word expected.");
    }

    @Test
    void TC_BW_02_queryBridgeWords_anotherOneBridge() {
        String result = graphProcessor.queryBridgeWords("scientist", "analyzed");
        assertEquals("The bridge word from \"scientist\" to \"analyzed\" is: \"carefully\".", result, "Test Case BW_02 Failed: One bridge word 'carefully' expected.");
    }

    @Test
    void TC_BW_03_queryBridgeWords_noBridge_wordsExist_caseInsensitive() {
        String result = graphProcessor.queryBridgeWords("THE", "TEAM");
        assertEquals("No bridge words from \"the\" to \"team\"!", result, "Test Case BW_03 Failed: No bridge words expected for THE and TEAM.");
    }

    @Test
    void TC_BW_04_queryBridgeWords_word1NotExist() {
        String result = graphProcessor.queryBridgeWords("nonexistent", "data");
        assertEquals("No \"nonexistent\" in the graph!", result, "Test Case BW_04 Failed: Word1 not in graph message expected.");
    }

    // --- White-box Tests (Section 6.6) ---

    @Test
    void TC_WB_01_queryBridgeWords_path_oneBridge() {
        String result = graphProcessor.queryBridgeWords("analyzed", "data");
        assertEquals("The bridge word from \"analyzed\" to \"data\" is: \"the\".", result, "Test Case WB_01 Failed.");
    }

    @Test
    void TC_WB_02_queryBridgeWords_path_noBridge() {
        String result = graphProcessor.queryBridgeWords("the", "team");
        assertEquals("No bridge words from \"the\" to \"team\"!", result, "Test Case WB_02 Failed.");
    }

    @Test
    void TC_WB_03_queryBridgeWords_path_bothWordsNotExist() {
        String result = graphProcessor.queryBridgeWords("nonexistent1", "nonexistent2");
        assertEquals("No \"nonexistent1\" and \"nonexistent2\" in the graph!", result, "Test Case WB_03 Failed.");
    }

    @Test
    void TC_WB_04_queryBridgeWords_path_oneWordExists_word2Not() {
        String result = graphProcessor.queryBridgeWords("data", "nonexistent");
        assertEquals("No \"nonexistent\" in the graph!", result, "Test Case WB_04 Failed.");
    }

    @Test
    void TC_WB_05_queryBridgeWords_path_oneWordExists_word1Not() {
        String result = graphProcessor.queryBridgeWords("nonexistent", "data");
        assertEquals("No \"nonexistent\" in the graph!", result, "Test Case WB_05 Failed.");
    }

    @Test
    void TC_WB_06_queryBridgeWords_graphEmpty() {
        DirectedGraphProcessor emptyGraphProcessor = new DirectedGraphProcessor();
        // For an empty graph processor, outputArea would also be null if not set.
        // However, queryBridgeWords checks graph.isEmpty() first.
        String result = emptyGraphProcessor.queryBridgeWords("any", "word");
        assertEquals("No graph exists. Please load a file first.", result, "Test Case WB_06 Failed: Graph empty message expected.");
    }

    @Test
    void TC_WB_07_queryBridgeWords_path_checkFormattingLogicPath_singleBridge() {
        String result = graphProcessor.queryBridgeWords("the", "report");
        assertEquals("The bridge word from \"the\" to \"report\" is: \"shared\".", result, "Test Case WB_07 Failed: Single bridge word 'shared' expected for the and report.");
    }

    // You can add your tests for generateNewText here as well,
    // they will benefit from the corrected setUp method.
    // Example placeholder for one of your generateNewText tests:
    /*
    @Test
    void generateNewText_BBT_GNT_01_withBridgeWords() {
        // Assuming your generateNewText test was something like:
        // String inputText = "the scientist analyzed data";
        // String expectedPattern = "the scientist carefully analyzed the data"; // Example expected, actual may vary due to randomness
        // String actualOutput = graphProcessor.generateNewText(inputText);
        // Assert based on the logic of generateNewText, which might involve checking for inserted bridge words.
        // Since it can be random, you might need to check for possible bridge words or a pattern.
        // For simplicity, if it returns the original if no bridge words, or a modified string:
        String inputText = "scientist analyzed"; // "scientist" -> "carefully" -> "analyzed"
        String generatedText = graphProcessor.generateNewText(inputText);
        // Expected: "scientist carefully analyzed"
        assertEquals("scientist carefully analyzed", generatedText, "generateNewText test with bridge words failed.");
    }
    */
}