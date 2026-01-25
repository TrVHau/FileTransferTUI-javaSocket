package FileTransfer.ui;

import FileTransfer.core.peer.Peer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;

import java.util.List;

public class PeerSelectionDialog extends DialogWindow {
    private Peer selectedPeer = null;
    
    public PeerSelectionDialog(List<Peer> peers) {
        super("Select Peer");
        setHints(java.util.Arrays.asList(Window.Hint.CENTERED));
        
        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        
        // Title
        Label titleLabel = new Label("Select a peer to send file:");
        titleLabel.setForegroundColor(TextColor.ANSI.CYAN);
        mainPanel.addComponent(titleLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Info label
        Label infoLabel = new Label("Found " + peers.size() + " peer(s) online");
        infoLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        mainPanel.addComponent(infoLabel);
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Create action list for better UX
        ActionListBox peerList = new ActionListBox(new TerminalSize(50, Math.min(peers.size() + 1, 10)));
        for (Peer peer : peers) {
            String displayText = String.format("%-20s @ %s", peer.getPeerName(), peer.getIPAddress());
            peerList.addItem(displayText, () -> {
                selectedPeer = peer;
                close();
            });
        }
        
        mainPanel.addComponent(peerList.withBorder(Borders.singleLine("Online Peers")));
        mainPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        
        // Buttons
        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
        
        Button cancelButton = new Button("Cancel", this::close);
        cancelButton.setRenderer(new Button.FlatButtonRenderer());
        
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
