package startuphelper;

//A Class with methods both other classes can use

import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

public abstract class Util {
    protected static final String VERSION = "3.1.0-b5";
    //Debug levels:
    //  0: Nothing; 1: Only for test messages; 2: Only base information; 3: More detailed messages; 4: All messages;
    protected static final byte DEBUG_ALWAYS = 0, DEBUG_TEST = 1, DEBUG_BASIC = 2, DEBUG_DETAILED = 3,
            DEBUG_VERBOSE = 4, DEBUG_NEVER = 5;
    protected static final byte DEFAULT_DEBUG_LEVEL = DEBUG_TEST;
    
    protected static final String DEFAULT_PROGRAMLIST_LOCATION = System.getProperty("user.dir") + "/StartupHelper.txt";
    
    protected static void debugPrint(byte debugLevel, String message) {
        debugPrint(debugLevel, DEFAULT_DEBUG_LEVEL, message);
    }
    protected static void debugPrint(byte messageDebugLevel, byte programDebugLevel, String message) {
        if (messageDebugLevel <= programDebugLevel) System.out.println(message);
        
    }
    
    protected static JButton createButton(String label, JPanel parent, ActionListener actionListener, boolean setEnabled) {
        JButton button = new JButton(label);
        //button.setFocusPainted(false);
        parent.add(button);
        button.addActionListener(actionListener);
        button.setEnabled(setEnabled);
        
        return button;
    }
    protected static JButton createButton(String label, JPanel parent, ActionListener actionListener) {
        return createButton(label, parent, actionListener, true);
    }
    
    protected static JSpinner createSpinner(String labelString, JPanel parent, int value, int min, int max, int stepsize) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, stepsize));
        spinner.setMaximumSize(new Dimension(100, spinner.getMaximumSize().height));
        
        //Create a label to go along with the spinner, explaining what it does
        JLabel label = new JLabel(labelString);
        label.setLabelFor(spinner);
        
        parent.add(label);
        parent.add(spinner);
        
        return spinner;
    }
    protected static JSpinner createSpinner(String labelString, JPanel parent,ChangeListener changeListener,
                                                                              int value, int min, int max, int stepsize) {
        JSpinner spinner = createSpinner(labelString, parent, value, min, max, stepsize);
        spinner.addChangeListener(changeListener);
        return spinner;
    }
        
    protected static int convertStringToInt(String string, int defaultNumber) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            debugPrint(DEBUG_ALWAYS, "ERROR converting '" + string + "' to a number, returning '" + defaultNumber + "' instead. (" + nfe + ")");
            return defaultNumber;
        }
    }
    
    protected static String getArgumentFromLine(String line) {
        String[] splitLine = line.split(" ");
        if (splitLine.length > 1) return splitLine[1];
        else return "";
    }
}
