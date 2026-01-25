package FileTransfer.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import java.io.File;
import java.util.Arrays;

public class FileBrowserDialog extends DialogWindow {
    private File selectedFile = null;
    private File currentDirectory;
    private ActionListBox fileListBox;
    private Label pathLabel;
    private Label statusLabel;
    private boolean showHiddenFiles = false;
    
    public FileBrowserDialog() {
        super("Select File to Send");
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
        currentDirectory = new File(System.getProperty("user.home"));
        
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        // Path display with color
        pathLabel = new Label(currentDirectory.getAbsolutePath());
        pathLabel.setForegroundColor(TextColor.ANSI.CYAN);
        mainPanel.addComponent(pathLabel.withBorder(Borders.singleLine("Current Path")));
        
        // File list
        fileListBox = new ActionListBox(new TerminalSize(65, 15));
        refreshFileList();
        
        mainPanel.addComponent(fileListBox.withBorder(Borders.singleLine("Files & Directories")));
        
        // Status label
        statusLabel = new Label("Select a file to send");
        statusLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        mainPanel.addComponent(statusLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Navigation buttons
        Panel navPanel = new Panel();
        navPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button homeButton = new Button("Home", this::goHome);
        Button upButton = new Button("Parent", this::goUpDirectory);
        Button toggleHiddenButton = new Button("Toggle Hidden", this::toggleHiddenFiles);
        
        navPanel.addComponent(homeButton);
        navPanel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        navPanel.addComponent(upButton);
        navPanel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        navPanel.addComponent(toggleHiddenButton);
        
        mainPanel.addComponent(navPanel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Cancel button
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button cancelButton = new Button("Cancel", this::close);
        buttonPanel.addComponent(cancelButton);
        
        mainPanel.addComponent(buttonPanel);
        
        setComponent(mainPanel);
    }
    
    private void goHome() {
        currentDirectory = new File(System.getProperty("user.home"));
        refreshFileList();
    }
    
    private void toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles;
        statusLabel.setText(showHiddenFiles ? "Showing hidden files" : "Hiding hidden files");
        refreshFileList();
    }
    
    private void refreshFileList() {
        fileListBox.clearItems();
        
        File[] files = currentDirectory.listFiles();
        if (files == null) {
            fileListBox.addItem("(Empty or inaccessible)", () -> {});
            pathLabel.setText(currentDirectory.getAbsolutePath());
            return;
        }
        
        // Filter hidden files if needed
        if (!showHiddenFiles) {
            files = Arrays.stream(files)
                .filter(f -> !f.isHidden() && !f.getName().startsWith("."))
                .toArray(File[]::new);
        }
        
        // Sort: directories first, then files
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        
        if (files.length == 0) {
            fileListBox.addItem("(No files found)", () -> {});
        }
        
        for (File file : files) {
            String displayName;
            if (file.isDirectory()) {
                displayName = "[DIR]  " + file.getName() + "/";
            } else {
                displayName = "[FILE] " + file.getName() + " (" + formatFileSize(file.length()) + ")";
            }
            
            fileListBox.addItem(displayName, () -> {
                if (file.isDirectory()) {
                    if (file.canRead()) {
                        currentDirectory = file;
                        pathLabel.setText(currentDirectory.getAbsolutePath());
                        refreshFileList();
                        statusLabel.setText("Select a file to send");
                    } else {
                        statusLabel.setText("Permission denied: " + file.getName());
                        statusLabel.setForegroundColor(TextColor.ANSI.RED);
                    }
                } else {
                    if (file.canRead()) {
                        selectedFile = file;
                        close();
                    } else {
                        statusLabel.setText("Cannot read file: " + file.getName());
                        statusLabel.setForegroundColor(TextColor.ANSI.RED);
                    }
                }
            });
        }
        
        pathLabel.setText(currentDirectory.getAbsolutePath());
    }
    
    private void goUpDirectory() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            currentDirectory = parent;
            refreshFileList();
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    public File getSelectedFile(WindowBasedTextGUI gui) {
        showDialog(gui);
        return selectedFile;
    }
}
