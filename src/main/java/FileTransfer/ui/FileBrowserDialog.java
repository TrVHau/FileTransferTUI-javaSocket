package FileTransfer.ui;

import FileTransfer.core.util.FormatUtils;

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
    private Label fileCountLabel;
    private boolean showHiddenFiles = false;
    
    public FileBrowserDialog() {
        super("Select File to Send");
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
        currentDirectory = new File(System.getProperty("user.home"));
        
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        // Header
        Label headerLabel = new Label("  Navigate to select a file to send");
        headerLabel.setForegroundColor(TextColor.ANSI.WHITE);
        mainPanel.addComponent(headerLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Path display with breadcrumbs style
        Panel pathPanel = new Panel();
        pathPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        pathLabel = new Label(truncatePath(currentDirectory.getAbsolutePath(), 55));
        pathLabel.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        pathPanel.addComponent(pathLabel);
        mainPanel.addComponent(pathPanel.withBorder(Borders.singleLine("Current Path")));
        
        // File count
        fileCountLabel = new Label("  Files: 0  |  Folders: 0");
        fileCountLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        mainPanel.addComponent(fileCountLabel);
        
        // File list with icons
        fileListBox = new ActionListBox(new TerminalSize(65, 14));
        refreshFileList();
        
        mainPanel.addComponent(fileListBox.withBorder(Borders.doubleLine("Files & Directories")));
        
        // Status label
        Panel statusPanel = new Panel();
        statusPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        statusLabel = new Label("Use arrow keys to navigate, Enter to select");
        statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
        statusPanel.addComponent(statusLabel);
        mainPanel.addComponent(statusPanel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Navigation buttons with better styling
        Panel navPanel = new Panel();
        navPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button homeButton = new Button("Home", this::goHome);
        Button upButton = new Button("Parent", this::goUpDirectory);
        Button toggleHiddenButton = new Button("Hidden", this::toggleHiddenFiles);
        Button cancelButton = new Button("Cancel", this::close);
        
        navPanel.addComponent(homeButton);
        navPanel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        navPanel.addComponent(upButton);
        navPanel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        navPanel.addComponent(toggleHiddenButton);
        navPanel.addComponent(new EmptySpace(new TerminalSize(3, 1)));
        navPanel.addComponent(cancelButton);
        
        mainPanel.addComponent(navPanel);
        
        // Help text
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        Label helpLabel = new Label("  [↑↓] Navigate  [Enter] Select/Open  [Tab] Buttons");
        helpLabel.setForegroundColor(TextColor.ANSI.WHITE);
        mainPanel.addComponent(helpLabel);
        
        setComponent(mainPanel);
    }
    
    private String truncatePath(String path, int maxLen) {
        if (path.length() <= maxLen) return path;
        return "..." + path.substring(path.length() - maxLen + 3);
    }
    
    private void goHome() {
        currentDirectory = new File(System.getProperty("user.home"));
        refreshFileList();
        statusLabel.setText("Navigated to home directory");
        statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
    }
    
    private void toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles;
        statusLabel.setText(showHiddenFiles ? "Showing hidden files" : "Hidden files are hidden");
        statusLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        refreshFileList();
    }
    
    private void refreshFileList() {
        fileListBox.clearItems();
        
        File[] files = currentDirectory.listFiles();
        if (files == null) {
            fileListBox.addItem("(Empty or inaccessible)", () -> {});
            pathLabel.setText(truncatePath(currentDirectory.getAbsolutePath(), 55));
            fileCountLabel.setText("  Files: 0  |  Folders: 0");
            return;
        }
        
        // Filter hidden files if needed
        if (!showHiddenFiles) {
            files = Arrays.stream(files)
                .filter(f -> !f.isHidden() && !f.getName().startsWith("."))
                .toArray(File[]::new);
        }
        
        // Sort: directories first, then files, alphabetically
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        
        // Count files and folders
        int folderCount = 0;
        int fileCount = 0;
        for (File f : files) {
            if (f.isDirectory()) folderCount++;
            else fileCount++;
        }
        fileCountLabel.setText("  Files: " + fileCount + "  |  Folders: " + folderCount);
        
        if (files.length == 0) {
            fileListBox.addItem("(No files found)", () -> {});
        }
        
        // Add parent directory option
        if (currentDirectory.getParentFile() != null) {
            fileListBox.addItem("[DIR] ..", this::goUpDirectory);
        }
        
        for (File file : files) {
            String displayName;
            if (file.isDirectory()) {
                displayName = "[DIR] " + file.getName();
            } else {
                String icon = getFileIcon(file.getName());
                displayName = icon + " " + file.getName() + " (" + FormatUtils.formatFileSize(file.length()) + ")";
            }
            
            fileListBox.addItem(displayName, () -> {
                if (file.isDirectory()) {
                    if (file.canRead()) {
                        currentDirectory = file;
                        pathLabel.setText(truncatePath(currentDirectory.getAbsolutePath(), 55));
                        refreshFileList();
                        statusLabel.setText("Opened folder: " + file.getName());
                        statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
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
        
        pathLabel.setText(truncatePath(currentDirectory.getAbsolutePath(), 55));
    }
    
    private String getFileIcon(String filename) {
        return "    ";
    }
    
    private void goUpDirectory() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            currentDirectory = parent;
            refreshFileList();
            statusLabel.setText("Navigated to parent directory");
            statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
        }
    }
    
    public File getSelectedFile(WindowBasedTextGUI gui) {
        showDialog(gui);
        return selectedFile;
    }
}
