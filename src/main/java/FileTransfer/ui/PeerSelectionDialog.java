package FileTransfer.ui;

import FileTransfer.core.peer.Peer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import java.util.List;

public class PeerSelectionDialog extends DialogWindow {
    private Peer selectedPeer = null;
    public PeerSelectionDialog(List<Peer> peers) {
        super("Select Peer");
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        mainPanel.addComponent(new Label("Select a peer to send file:"));
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Create radio box list
        RadioBoxList<Peer> radioList = new RadioBoxList<>();
        for (Peer peer : peers) {
            radioList.addItem(peer);
        }
        
        mainPanel.addComponent(radioList.withBorder(Borders.singleLine()));
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Buttons
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button okButton = new Button("OK", () -> {
            selectedPeer = radioList.getCheckedItem();
            close();
        });
        
        Button cancelButton = new Button("Cancel", this::close);
        
        buttonPanel.addComponent(okButton);
        buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1)));
        buttonPanel.addComponent(cancelButton);
        
        mainPanel.addComponent(buttonPanel);
        
        setComponent(mainPanel);
    }
    
    public Peer getSelectedPeer(WindowBasedTextGUI gui) {
        showDialog(gui);
        return selectedPeer;
    }
    
    @Override
    public String toString() {
        return "Select Peer";
    }
}
