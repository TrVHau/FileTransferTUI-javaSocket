package FileTransfer.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import java.io.File;
import java.util.Arrays;

public class FileBrowserDialog extends DialogWindow {
    private File selectedFile = null;
    private File currentDirectory;
    private ActionListBox fileListBox;
    private Label pathLabel;
    
    public FileBrowserDialog() {
        super("Select File");
        currentDirectory = new File(System.getProperty("user.home"));
        
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        // Path display
        pathLabel = new Label("Path: " + currentDirectory.getAbsolutePath());
        mainPanel.addComponent(pathLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // File list
        fileListBox = new ActionListBox(new TerminalSize(60, 15));
        refreshFileList();
        
        mainPanel.addComponent(fileListBox.withBorder(Borders.singleLine("Files")));
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Buttons
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button upButton = new Button("Parent Dir", this::goUpDirectory);
        Button cancelButton = new Button("Cancel", this::close);
        
        buttonPanel.addComponent(upButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(cancelButton);
        
        mainPanel.addComponent(buttonPanel);
        
        setComponent(mainPanel);
    }
    
    private void refreshFileList() {
        fileListBox.clearItems();
        
        File[] files = currentDirectory.listFiles();
        if (files == null) {
            return;
        }
        
        // Sort: directories first, then files
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        
        for (File file : files) {
            String displayName;
            if (file.isDirectory()) {
                displayName = "[DIR] " + file.getName();
            } else {
                displayName = file.getName() + " (" + formatFileSize(file.length()) + ")";
            }
            
            fileListBox.addItem(displayName, () -> {
                if (file.isDirectory()) {
                    currentDirectory = file;
                    pathLabel.setText("Path: " + currentDirectory.getAbsolutePath());
                    refreshFileList();
                } else {
                    selectedFile = file;
                    close();
                }
            });
        }
        
        pathLabel.setText("Path: " + currentDirectory.getAbsolutePath());
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
