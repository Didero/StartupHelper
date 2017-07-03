package startuphelper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ProgramlistEditor extends JFrame {
    private String programlistFileLocation;
    private JList programlist;
    private DefaultListModel programlistModel;
    private JSpinner startwaitSpinner, betweenwaitSpinner;
    private JCheckBox alwaysOnTopCheckbox;
    private JButton addProgramButton, addWaitButton, addCommentButton, addEmptyLineButton;
    private JButton moveLineUpButton, moveLineDownButton, commentLineButton, editLineButton, removeLineButton;
    private JButton helpButton, saveButton, closeButton;
    private boolean unsavedChanges = false;
    
    private final int WINDOW_HEIGHT = 315;
    
    public ProgramlistEditor(String listfileLocation) {
        programlistFileLocation = listfileLocation;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
        
    private void createAndShowGUI() {
        setTitle("Startup Helper "+Util.VERSION+" - Editor");
        setLayout(new BorderLayout());
        //No defaultOnClose, since we need to be able to ask the user to confirm closing the window
        addWindowListener(new WindowHandler());
        
        //Add startup value selectors to the bottom
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
        SpinnerHandler spinnerHandler = new SpinnerHandler();
        startwaitSpinner = Util.createSpinner("StartWait: ", spinnerPanel, spinnerHandler, 1, 0, 99999, 1);
        betweenwaitSpinner = Util.createSpinner("BetweenWait: ", spinnerPanel, spinnerHandler, 1, 0, 99999, 1);
        
        alwaysOnTopCheckbox = new JCheckBox("Always On Top", false);
        alwaysOnTopCheckbox.addItemListener(new CheckboxHandler());
        spinnerPanel.add(alwaysOnTopCheckbox);
        
        add(spinnerPanel, BorderLayout.SOUTH);
        
        //Create the control panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        ButtonHandler buttonHandler = new ButtonHandler(this);
        addProgramButton = Util.createButton("Add Program", buttonPanel, buttonHandler);
        addWaitButton = Util.createButton("Add Wait", buttonPanel, buttonHandler);
        addCommentButton = Util.createButton("Add Comment", buttonPanel, buttonHandler);
        addEmptyLineButton = Util.createButton("Add Empty Line", buttonPanel, buttonHandler, false);        
        
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        buttonPanel.add(Box.createVerticalGlue());
        
        moveLineUpButton = Util.createButton("Move Up", buttonPanel, buttonHandler, false);
        moveLineDownButton = Util.createButton("Move Down", buttonPanel, buttonHandler, false);
        
        commentLineButton = Util.createButton("Comment Line", buttonPanel, buttonHandler, false);
        editLineButton = Util.createButton("Edit Line", buttonPanel, buttonHandler, false);
        removeLineButton = Util.createButton("Remove Line", buttonPanel, buttonHandler, false);
        
        helpButton = Util.createButton("Help", buttonPanel, buttonHandler);
        saveButton = Util.createButton("Save List", buttonPanel, buttonHandler, false);
        closeButton = Util.createButton("Close Editor", buttonPanel, buttonHandler);
        
        buttonPanel.setPreferredSize(new Dimension(140, WINDOW_HEIGHT));
        add(buttonPanel, BorderLayout.EAST);
        
        programlistModel = new DefaultListModel();
        File programlistFile = new File(Util.DEFAULT_PROGRAMLIST_LOCATION);
        if (programlistFile.exists()) {
            //Load in the combined program list/settings file
            try {
                boolean betweenwaitIsSet = false;
                Scanner scanner = new Scanner(programlistFile);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String lineLowerCase = line.toLowerCase();
                    
                    Util.debugPrint(Util.DEBUG_VERBOSE, "LINE: " + line);

                    //Get the initial values, to set the spinners correctly. Don't add them to the list
                    if (lineLowerCase.startsWith("startwait")) {
                        startwaitSpinner.setValue(Util.convertStringToInt(Util.getArgumentFromLine(line), 1));
                    }
                    else if (lineLowerCase.startsWith("betweenwait") && betweenwaitIsSet == false) {
                        betweenwaitSpinner.setValue(Util.convertStringToInt(Util.getArgumentFromLine(line), 1));
                        betweenwaitIsSet = true;
                    }
                    else if (lineLowerCase.replaceAll(" ", "").equals("alwaysontop")) {
                        alwaysOnTopCheckbox.setSelected(true);
                        unsavedChanges(false);
                    }
                    //If it's not one of the initial settings, add it to the programlist editor as normal
                    else {
                        if (line.length() == 0) line = " ";

                        programlistModel.addElement(line);
                    }
                }
                scanner.close();
                
                //Remove any empty lines at the top
                while (programlistModel.get(0).equals(" ") && programlistModel.size() >= 1) {
                    programlistModel.remove(0);
                }
            } catch (FileNotFoundException fnfe) {
                Util.debugPrint(Util.DEBUG_ALWAYS, "Program list not found! (" + fnfe + ")");
            } catch (IOException ioe) {
                Util.debugPrint(Util.DEBUG_ALWAYS, "ERROR while reading the program list: " + ioe);
            }
        }
        programlist = new JList(programlistModel);
        
        programlist.setLayoutOrientation(JList.VERTICAL);
        //Make sure only one item can be selected at a time
        programlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        programlist.addListSelectionListener(new ListHandler());

        //Make sure long lists can be shown too without going off-screen
        JScrollPane programlistScroller = new JScrollPane(programlist, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        //programlistScroller.setMaximumSize(new Dimension(500, 2000));
        programlistScroller.setPreferredSize(new Dimension(500, WINDOW_HEIGHT));
        
        add(programlistScroller, BorderLayout.CENTER);
        
        
        //Finally, display the entire GUI
        pack();
        setVisible(true);
        //Center window on screen
        setLocationRelativeTo(null);
        //If the program list didn't exist before, inform the user and fill the file with some starting information
        if (programlistModel.isEmpty()) {
            showHelp(true);
        }
        //else unsavedChanges(false);
    }
    
    //A method to determine at which index a new line should be inserted into the program list
    private int getInsertIndex() {
        int selectedIndex = programlist.getSelectedIndex();
        //If no entry has been selected, add the program at the end
        if (selectedIndex == -1) return programlistModel.getSize();
        //Otherwise, return the place after the currently selected one
        else return selectedIndex + 1;
    }
    private void addLineToProgramlist(String line, int insertIndex) {
        //Add the program to the list, and make sure it's visible and selected
        programlistModel.add(insertIndex, line);
        programlist.ensureIndexIsVisible(insertIndex);
        programlist.setSelectedIndex(insertIndex);
        unsavedChanges(true);
    }
    private void addLineToProgramlist(String line) {
        addLineToProgramlist(line, getInsertIndex());
    }
    
    private void unsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
        saveButton.setEnabled(unsavedChanges);
    }
    
    private void saveProgramlist() {
        try {
            //Open a file to write to
            BufferedWriter writer = new BufferedWriter(new FileWriter(Util.DEFAULT_PROGRAMLIST_LOCATION));
            
            //Write the initial settings first
            writer.write("startWait " + startwaitSpinner.getValue());
            writer.newLine();
            writer.write("betweenWait " + betweenwaitSpinner.getValue());
            writer.newLine();
            if (alwaysOnTopCheckbox.isSelected()) {
                writer.write("AlwaysOnTop");
                writer.newLine();
            }
            writer.newLine();
            
            //Iterate over all the lines, writing each one down
            for (int i = 0; i < programlistModel.getSize(); i++) {
                //Make sure no errant spaces are left in, that were put there so they're displayed in the list
                String line = programlistModel.get(i).toString();
                
                if (line.equals(" ")) line = "";

                writer.write(line);
                writer.newLine();
            }
            //Make sure all the data is written to disk
            writer.flush();
            //Writer isn't needed anymore
            writer.close();
            
            unsavedChanges(false);
            
            JOptionPane.showMessageDialog(this, "The program list was saved successfully!", "Save Successful!", JOptionPane.INFORMATION_MESSAGE);
        } catch (FileNotFoundException fnfe) {
            Util.debugPrint(Util.DEBUG_ALWAYS, "ERROR while writing program list: "+fnfe);
        } catch (IOException ioe) {
            Util.debugPrint(Util.DEBUG_ALWAYS, "IO ERROR while writing program list: "+ioe);
        } catch (Exception e) {
            Util.debugPrint(Util.DEBUG_ALWAYS, "Generic ERROR while writing program list: "+e);
        }
    }
    
    private void showHelp(boolean firstStartup) {
        String message = "You can add programs you want to start with the buttons on the right.\n"
                + "In the fields at the bottom you can set initial waiting values.\n"
                + "Press the 'Save' button when you're done, and the 'Close' button to dismiss the Editor.\n"
                + "The next time Startup Helper is launched, it will start the programs listed here.\n"
                + "If you want to come back to this screen again, press the 'Edit Program List' button.";
        if (firstStartup) {
            message = "Since the program list wasn't found,"
                    + "this is most likely the first time you start this program.\n" + message;
        }
        
        JOptionPane.showMessageDialog(this, message, "Startup Helper Help", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void toggleButtonState(boolean setEnabled, JButton... buttons) {
        for (JButton button : buttons) {
            button.setEnabled(setEnabled);
        }
    }
    
    private void closeWindow() {
        //If there are any changes to the program list that haven't been saved yet, ask if they should
        if (unsavedChanges) {
            int choice = JOptionPane.showConfirmDialog(this, "Some changes to the program list aren't saved.\nDo you want to save changes before exiting?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.OK_OPTION) saveProgramlist();
            if (choice != JOptionPane.CANCEL_OPTION) System.exit(0);
        }
        else {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to close the Editor?",
                    "Close Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                System.exit(0);
            }
        }        
    }
    
    private class ButtonHandler implements ActionListener {
        private final ProgramlistEditor PARENT;
        
        public ButtonHandler(ProgramlistEditor parent) {
            this.PARENT = parent;
        }        
        
        @Override
        public void actionPerformed(ActionEvent e) {
            Util.debugPrint(Util.DEBUG_DETAILED, "Button pressed: "+((JButton) e.getSource()).getText());
            
            if (e.getSource() == addProgramButton) {
                //Show a standard file chooser so the user can pick the program they want
                JFileChooser filechooser = new JFileChooser();
                int choice = filechooser.showDialog(PARENT, "Select");
                //If the user didn't press the 'cancel' button
                if (choice == JFileChooser.APPROVE_OPTION) {
                    String program = filechooser.getSelectedFile().getAbsolutePath();
                    Util.debugPrint(Util.DEBUG_TEST, "Program selected: "+program);
                    
                    if (JOptionPane.showConfirmDialog(PARENT, "Do you want StartupHelper to ask whether to start this program?",
                            "Ask On Startup?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                        program = "?"+program;
                    }
                    
                    //Finally add the line to the list on the selected spot
                    addLineToProgramlist(program);
                }
            }
            else if (e.getSource() == addWaitButton) {
                String[] options = new String[]{"Between Wait", "Wait", "Cancel"};
                int choice = JOptionPane.showOptionDialog(PARENT, "Which command do you want to add here?",
                        "Pick A Command", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                //Only continue if the user didn't press 'Cancel'
                if (choice != 2 && choice != JOptionPane.CLOSED_OPTION) {
                    //The command is the readable version without spaces
                    String line = options[choice].replaceAll(" ", "");

                    //Get a time value for the waiting period
                    String value = JOptionPane.showInputDialog(PARENT, "Please enter the number of seconds you want this waiting period to be.", "1");
                    Util.debugPrint(Util.DEBUG_DETAILED, "Entered Wait Value: '"+value+"'.");
                    //Only add the line if the user didn't press 'Cancel'
                    if (value != null) {
                        line += " " + Util.convertStringToInt(value, 1);
                        addLineToProgramlist(line);
                    }
                }
            }
            else if (e.getSource() == addCommentButton) {
                String comment = JOptionPane.showInputDialog(PARENT, "Enter any comment you want.", "Comment Entry", JOptionPane.PLAIN_MESSAGE);
                if (comment != null && comment.length() > 0) addLineToProgramlist("#"+comment);
            }
            else if (e.getSource() == addEmptyLineButton) {
                //Only add a line if there is a line selected. Empty lines at the end are rather useless
                if (programlist.getSelectedIndex() != -1) addLineToProgramlist(" ");
            }
            
            //'Move Up' and 'Move Down' are so similar in function, they can share an 'else if'
            else if (e.getSource() == moveLineUpButton || e.getSource() == moveLineDownButton) {
                int selectedIndex = programlist.getSelectedIndex();
                int otherIndex;
                if (e.getSource() == moveLineUpButton) otherIndex = selectedIndex - 1;
                else otherIndex = selectedIndex + 1;
                
                //Retrieve the top line
                String selectedLine = (String) programlistModel.get(selectedIndex);
                //Replace the second line with the first. This gives us the original second line
                String otherLine = (String) programlistModel.set(otherIndex, selectedLine);
                //Replace the first line with the second
                programlistModel.set(selectedIndex, otherLine);                
                
                //Select the originally selected line
                programlist.setSelectedIndex(otherIndex);                
                unsavedChanges(true);
            }
            
            else if (e.getSource() == commentLineButton) {
                String line = (String) programlist.getSelectedValue();
                //If there's already a comment sign, remove it, otherwise add it
                //Update the button's text to reflect the new situation in the process
                if (line.startsWith("#")) {
                    line = line.substring(1);
                    commentLineButton.setText("Comment Line");
                }
                else {
                    line = "#"+line;
                    commentLineButton.setText("Uncomment Line");
                }                
                
                programlistModel.set(programlist.getSelectedIndex(), line);                
                unsavedChanges(true);
            }
            else if (e.getSource() == editLineButton) {
                //Three possible cases: Comment line, command or program
                String line = programlist.getSelectedValue().toString();
                String lineLowerCase = line.toLowerCase();
                String lineNew = line;
                //If the line's a comment
                if (line.startsWith("#")) {
                    String input = "#"+JOptionPane.showInputDialog(PARENT, "Make the desired changes to this comment here.",
                            "Comment Change", JOptionPane.PLAIN_MESSAGE, null, null, line.substring(1));
                    if (input != null) lineNew = "#"+input;
                }
                //If the line is a command
                else if (lineLowerCase.startsWith("startwait") || lineLowerCase.startsWith("betweenwait") || lineLowerCase.startsWith("wait")) {
                    String time = (String) JOptionPane.showInputDialog(PARENT, "Enter a new wait value here.", "New Wait Time",
                            JOptionPane.PLAIN_MESSAGE, null, null, Util.getArgumentFromLine(line));
                    if (time != null) lineNew = line.split(" ")[0] + " " + time;
                }
                //If it's neither a comment nor a command, it's a program (empty lines are ignored)
                else {
                    File lineFile;
                    if (line.startsWith("?")) lineFile = new File(line.substring(1));
                    else lineFile = new File(line);                    
                    //Show a standard file chooser so the user can pick the program they want
                    JFileChooser filechooser = new JFileChooser(lineFile);
                    filechooser.setSelectedFile(lineFile);
                    filechooser.ensureFileIsVisible(lineFile);
                    int choice = filechooser.showDialog(PARENT, "Select");
                    //If the user didn't press the 'cancel' button
                    if (choice == JFileChooser.APPROVE_OPTION) {
                        lineNew = filechooser.getSelectedFile().getAbsolutePath();
                        Util.debugPrint(Util.DEBUG_DETAILED, "Program selected: "+lineNew);

                        if (JOptionPane.showConfirmDialog(PARENT, "Do you want StartupHelper to ask whether to start this program?",
                                "Ask On Startup?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                            lineNew = "?"+lineNew;
                        }
                    }
                }//End 'edit program'-if
                
                //Finally, edit the old line into the new one, if necessary
                if (!lineNew.equals(line)) {
                    Util.debugPrint(Util.DEBUG_BASIC, "Old line: "+line);
                    Util.debugPrint(Util.DEBUG_BASIC, "New line: "+lineNew);
                    programlistModel.set(programlist.getSelectedIndex(), lineNew);
                    unsavedChanges(true);
                }
            }
            else if (e.getSource() == removeLineButton) {
                //First check if a line is selected
                int selectedIndex = programlist.getSelectedIndex();
                if (selectedIndex != -1) {
                    boolean removeLine = false;
                    //If it's just an empty line, just remove the line
                    if (programlist.getSelectedValue().toString().length() < 3) removeLine = true;
                    //Otherwise, ask for confirmation
                    else if (JOptionPane.showConfirmDialog(PARENT, "Are you sure you want to delete the selected line?",
                            "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        removeLine = true;
                    }
                    
                    if (removeLine) {
                        programlistModel.remove(selectedIndex);
                        //Select the entry just before the one that was just deleted, if it wasn't the first one
                        programlist.setSelectedIndex(Math.max(selectedIndex - 1, 0));
                        unsavedChanges(true);
                    }
                }
            }
            
            else if (e.getSource() == helpButton) showHelp(false);
            else if (e.getSource() == saveButton) {
                saveProgramlist();
            }
            else if (e.getSource() == closeButton) {
                closeWindow();
            }//end of 'closeButton' if
        } //End of 'actionPerformed' function
    } //End of 'ButtonHandler' class
    
    private class ListHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            //Only fire if it's the final change
            if (!e.getValueIsAdjusting()) {
                Util.debugPrint(Util.DEBUG_DETAILED, "Selected item: "+programlist.getSelectedIndex() + ": '"+programlist.getSelectedValue()+"'");
                //If there's no item selected now, disable buttons related to a specific line
                if (programlist.getSelectedIndex() == -1) {
                    toggleButtonState(false, moveLineUpButton, moveLineDownButton, commentLineButton, editLineButton, removeLineButton, addEmptyLineButton);
                }
                //Otherwise, if there's nothing special going on, enable all buttons
                else {
                    String selectedValue = programlist.getSelectedValue().toString().toLowerCase();
                    int selectedIndex = programlist.getSelectedIndex();
                    //Since there's now a line to remove, enable the button
                    removeLineButton.setEnabled(true);

                    //Some lines can't be edited (empty lines), disable 'Edit' button on those
                    if (selectedValue.length() < 2) {
                        editLineButton.setEnabled(false);
                        commentLineButton.setEnabled(false);
                    }
                    else {
                        editLineButton.setEnabled(true);
                        commentLineButton.setEnabled(true);
                    }
                    //Editing the 'alwaysOnTop' line doesn't make sense
                    if (selectedValue.replaceAll(" ", "").equals("alwaysontop")) editLineButton.setEnabled(false);
                    
                    if (selectedValue.startsWith("#")) commentLineButton.setText("Uncomment Line");
                    else commentLineButton.setText("Comment Line");
                    
                    //Don't allow adding an empty line below the last entry
                    if (selectedIndex == programlistModel.size()-1) {
                        //addEmptyLineButton.setEnabled(false);
                        toggleButtonState(false, moveLineDownButton, addEmptyLineButton);
                    }
                    else {
                        //addEmptyLineButton.setEnabled(true);
                        toggleButtonState(true, moveLineDownButton, addEmptyLineButton);
                    }
                    
                    //Don't allow moving lines up from the top row
                    if (selectedIndex == 0) moveLineUpButton.setEnabled(false);
                    else moveLineUpButton.setEnabled(true);
                }//End of valid-index check
            }//End of 'valueIsAdjusting' check for list
        }//End of 'ValueChanged' method
    }//End of ListHandler class
    
    //When the user changes the wait times, enable the 'save' button
    private class SpinnerHandler implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            unsavedChanges(true);
        }        
    }
    
    //When the user toggles 'Always On Top', enable the 'save' button
    private class CheckboxHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            unsavedChanges(true);
        }        
    }
    
    private class WindowHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            closeWindow();
        }
    }
}