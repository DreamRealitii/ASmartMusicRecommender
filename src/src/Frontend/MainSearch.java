/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Frontend;

import Backend.Analysis.AnalysisCompare;
import Backend.Analysis.AnalysisCompare.CompareResult;
import Backend.Analysis.SpotifyAnalysis;
import Backend.Spotify.SpotifyAPI;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

/**
 *
 * @author Arnav
 */
public class MainSearch extends javax.swing.JFrame {

    private String id = "";
    private String[] resultURLs;
    private static final DecimalFormat percentFormat = new DecimalFormat("0.00%");

    /**
     * Creates new form MainSearch
     */
    public MainSearch() {
        initComponents();
        // Empty list.
        songList.setModel(new AbstractListModel<>() {
            @Override
            public int getSize() {
                return 0;
            }

            @Override
            public String getElementAt(int index) {
                return null;
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        idInput = new javax.swing.JTextField();
        titleLabel = new javax.swing.JLabel();
        backButton = new javax.swing.JButton();
        suggestButton = new javax.swing.JButton();
        idLabel = new javax.swing.JLabel();
        errorLabel = new javax.swing.JLabel();
        instructions = new javax.swing.JLabel();
        scrollPanel = new javax.swing.JScrollPane();
        songList = new javax.swing.JList<>();
        copyButton = new javax.swing.JButton();
        playlistButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        idInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                idInputKeyReleased(evt);
            }
        });

        titleLabel.setFont(new java.awt.Font("Helvetica Neue", 0, 36)); // NOI18N
        titleLabel.setText("ASMR");

        backButton.setText("Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        suggestButton.setText("Suggest Songs");
        suggestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suggestButtonActionPerformed(evt);
            }
        });

        idLabel.setText("Track URL:");

        errorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        errorLabel.setText("Invalid ID");
        errorLabel.setVisible(false);

        instructions.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        instructions.setText("<html> Type in the URL of a Spotify song given by (... -> Share -> Copy Song Link). <br> For Example: https://open.spotify.com/track/17lrs2l9qXEuFybi7hSsid?si=37b141e7c99649c7 </html>");

        songList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scrollPanel.setViewportView(songList);

        copyButton.setText("Copy Selected");
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });

        playlistButton.setText("Generate Playlist");
        playlistButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playlistButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("*Mac users should use ctrl-c and ctrl-v to copy and paste!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addComponent(instructions, javax.swing.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(idLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(errorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(idInput, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(copyButton)
                            .addComponent(suggestButton, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(playlistButton))
                        .addGap(12, 12, 12)))
                .addComponent(scrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 485, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(46, 46, 46))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(backButton)
                        .addGap(251, 251, 251)
                        .addComponent(titleLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(237, 237, 237)
                        .addComponent(jLabel1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(titleLabel)
                    .addComponent(backButton))
                .addGap(18, 18, 18)
                .addComponent(instructions, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(idLabel)
                            .addComponent(idInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(errorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(suggestButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(copyButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(playlistButton))
                    .addComponent(scrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(40, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Simply moves back a frame, to the Login frame. The current Search frame is disposed.
     * @param evt
     */
    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        this.toBack();
        this.dispose();
        Login newLogin = new Login();
        newLogin.setVisible(true);
        newLogin.toFront();
    }//GEN-LAST:event_backButtonActionPerformed

    /**
     * Uses the id variable to analyze however many songs we have set it to
     * After analyzing the songs and finding matches, update songList within ScrollPanel, and display matches to user ordered by highest match.
     * @param evt
     */
    private void suggestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suggestButtonActionPerformed
        suggestButton.setText("Working...");
        try {
            // Get user analysis.
            List<SpotifyAnalysis> userAnalysis = new ArrayList<>();
            String trackId = id.substring(31, 53);
            System.out.println("TrackID:" + trackId);
            userAnalysis.add(SpotifyAPI.getTrackFeatures(trackId));

            // Get N random songs to compare with.
            List<SpotifyAnalysis> comparisonAnalyses;
            String[] comparisonIds = SpotifyAPI.getRecommendations(userAnalysis.get(0));
            comparisonAnalyses = new ArrayList<>(List.of(SpotifyAPI.getTracksFeatures(comparisonIds)));

            // Compare songs and print results.
            List<CompareResult> results = AnalysisCompare.compareTheseToThoseAnalyses(userAnalysis, comparisonAnalyses);

            String[] resultIds = new String[results.size()];
            String[] trackNames = new String[results.size()];
            for (int i = 0; i < results.size(); i++){
                resultIds[i] = ((SpotifyAnalysis) results.get(i).b).getTrackId();
                trackNames[i] = ((SpotifyAnalysis) results.get(i).b).getTrackName();
            }
            resultURLs= SpotifyAPI.getTrackURLs(resultIds);

            for (int i = 0; i < results.size(); i++) {
                String matchPercent = percentFormat.format(results.get(i).result);
                System.out.println(resultURLs[i] + " = " + matchPercent);
            }

            // DefaultListModel object to update the list representing songs
            DefaultListModel<String> lm = new DefaultListModel<>();
            // iterate through each url result and append the match percentage
            for (int i = 0; i < resultURLs.length; i++) {
                String matchPercent = percentFormat.format(results.get(i).result);
                String combined = trackNames[i] +  ", " + matchPercent;
                // add appended string to our DefaultListModel
                lm.add(i, combined);
            }
            // set the current ListModel as our updated DefaultListModel
            songList.setModel(lm);
            errorLabel.setVisible(false);
            // update the frame
            this.revalidate();
            this.repaint();
            
        } catch (RuntimeException e) {
            errorLabel.setVisible(true);
            e.printStackTrace();
            System.out.println("MainSearch: Invalid ID - " + e.getMessage());
        }
        suggestButton.setText("Suggest Songs");
        
    }//GEN-LAST:event_suggestButtonActionPerformed

    /**
     * Updates the trackId to the text inputted in idInput field
     * @param evt
     */
    private void idInputKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_idInputKeyReleased
        id = idInput.getText();
    }//GEN-LAST:event_idInputKeyReleased

    /**
     * Copies Spotify URL of selected song to clipboard
     * @param evt
     */
    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
        System.out.println("MainSearch: Copy Selected button clicked.");

        int index = songList.getSelectedIndex();
        StringSelection stringSelection = new StringSelection(resultURLs[index]);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);

        System.out.println("MainSearch: Copied URL " + resultURLs[index]);
    }//GEN-LAST:event_copyButtonActionPerformed

    private void playlistButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playlistButtonActionPerformed
        System.out.println("MainSearch: Generate Playlist button clicked.");

        List<String> ids = new ArrayList<>();
        for (String resultURL : resultURLs)
            if (resultURL != null)
                ids.add("spotify:track:" + urlToId(resultURL));
        SpotifyAnalysis song = new SpotifyAnalysis(id.substring(31, 53));
        SpotifyAPI.createPlaylist(ids.toArray(new String[0]), song.getTrackName());
    }//GEN-LAST:event_playlistButtonActionPerformed

    private static String urlToId(String url) {
        return url.substring(31);
    }

    private static String matchToUrl(String match) {
        return match.substring(0, match.indexOf(','));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainSearch.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainSearch.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainSearch.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainSearch.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainSearch().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JButton copyButton;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JTextField idInput;
    private javax.swing.JLabel idLabel;
    private javax.swing.JLabel instructions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton playlistButton;
    private javax.swing.JScrollPane scrollPanel;
    private javax.swing.JList<String> songList;
    private javax.swing.JButton suggestButton;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}
