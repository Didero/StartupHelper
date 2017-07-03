/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package startuphelper;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import javax.swing.*;


public class StartupHelper extends JFrame {
    //Debug levels:
    //  0: Nothing; 1: Only for test messages; 2: Only base information; 3: More detailed messages;
    //    4: All messages;
    /*private static final byte DEBUG_ALWAYS = 0, DEBUG_TEST = 1, DEBUG_BASIC = 2, DEBUG_DETAILED = 3,
            DEBUG_VERBOSE = 4, DEBUG_NEVER = 5;
    private static final byte DEBUG_LEVEL = DEBUG_TEST;*/
    private byte programDebugLevel = Util.DEBUG_TEST;
    
    private String programlistLocation = Util.DEFAULT_PROGRAMLIST_LOCATION;
   
    private JButton startButton, skipButton, pauseButton, quitButton;
    private JButton editProgramlistButton;
    private JProgressBar progressBar;
    private ProgramLoopWorker worker;
    
    private FileLauncher fileLauncher;
    
    private int startWait = 10000, betweenWait = 10000;
    private LinkedList<String> programQueue;
    private LinkedList<String> errorList;
    
    public static void main(String[] args) {
        StartupHelper sh = new StartupHelper(args);
    }
    
    public StartupHelper(String[] args) {
        boolean startEditor = false;
        //Read through all the arguments to see if any of them are valid
        for (String arg : args) {
            //Converting everything to lower case makes it easier to pick the right option
            arg = arg.toLowerCase();
            //If there are any parameters too (like 'debug:BASIC'), retrieve those
            String parameter = "";
            if (arg.contains(":")) parameter = arg.substring(arg.indexOf(":")+1);
            
            //Often arguments start with a dash. If users do that here out of habit, ignore that
            while (arg.startsWith("-")) arg = arg.substring(1);
            
            //Set the debug level
            if (arg.startsWith("debug:")) {
                if (parameter.equals("verbose") || parameter.equals("5")) programDebugLevel = 4;
                else if (parameter.equals("detailed") || parameter.equals("4")) programDebugLevel = 3;
                else if (parameter.equals("basic") || parameter.equals("3")) programDebugLevel = 2;
                else if (parameter.equals("test") || parameter.equals("1")) programDebugLevel = 1;
            }
            else if (arg.equals("editor")) {
                startEditor = true;
            }
            //Allow specification of a different location of the programlist file
            else if (arg.startsWith("file:")) {
                //If the file location is surrounded with quotes, as it should be if it contains spaces, remove them
                if (parameter.startsWith("\"") || parameter.startsWith("'")) parameter = parameter.substring(1);
                if (parameter.endsWith("\"") || parameter.endsWith("'")) parameter = parameter.substring(0, parameter.length());
                programlistLocation = parameter;
            }
        }
        
        //Check if the required program list exists, otherwise redirect to the editor
        if (new File(programlistLocation).exists() == false) {
            debugPrint(Util.DEBUG_BASIC, "Program list not found at '"+programlistLocation+"', starting editor");
            startEditor = true;
        }
        
        if (startEditor) {
            ProgramlistEditor pe = new ProgramlistEditor(programlistLocation);
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    programQueue = loadProgramList(Util.DEFAULT_PROGRAMLIST_LOCATION);
                    errorList = new LinkedList<String>();
                    createAndShowGUI();
                    startProgramLoop();
                }
            });
        }
    }

    private void debugPrint(byte debugLevel, String message) {
        Util.debugPrint(debugLevel, programDebugLevel, message);
    }
    
    protected void exit(int status) {
        //Pause the countdown, so user can think about their choice
        boolean workerIsPaused = false;
        if (worker != null) {
            workerIsPaused = worker.isPaused();
            worker.setPaused(true);
        }
        //Ask if the user's certain
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nNo further programs will be started.",
                "Really quit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            //Stop the worker thread, if it's been started already
            if (worker != null) {
                worker.cancel(false);
            }
            System.exit(status);
        }
        else {
            //If the user doesn't want to quit, resume the program countdown and starting
            if (worker != null) worker.setPaused(workerIsPaused);
        }
    }
    protected void exit() {
        exit(0);
    }
    
    protected LinkedList<String> getProgramQueue() {
        return programQueue;
    }
    protected int getStartWait() {
        return startWait;
    }
    protected int getBetweenWait() {
        return betweenWait;
    }
    
    private void createAndShowGUI() {
      setResizable(false);
      setTitle("Startup Helper "+Util.VERSION + " [Fallback method]");
      if (Desktop.isDesktopSupported()) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          setTitle("Startup Helper "+Util.VERSION + " [Generic method]");
        }
      }

      setLayout(new BorderLayout());
      addWindowListener(new WindowHandler());

      try {
          fileLauncher = FileLauncher.getInstance();
      } catch (UnsupportedOperationException uoe) {
          showError("This OS is not supported. Please report your OS name and how to launch programs to the project", "ERROR: Not supported");
          System.exit(1);
      }

      //Keep track of how long until the next program's started
      progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
      //Set a basic string to draw, so the GUI can be sized and outlined properly
      progressBar.setStringPainted(true);
      progressBar.setString("Loading...");

      add(progressBar, BorderLayout.NORTH);

      //Create a place for the control buttons, and the buttons themselves
      JPanel buttonPanel = new JPanel();
      ButtonHandler buttonHandler = new ButtonHandler();
      startButton = Util.createButton("Start", buttonPanel, buttonHandler, false);
      skipButton = Util.createButton("Skip", buttonPanel, buttonHandler, false);
      pauseButton = Util.createButton("Resume", buttonPanel, buttonHandler, false); //'Resume' instead of 'Pause' to fix alignment oddity
      quitButton = Util.createButton("Quit", buttonPanel, buttonHandler, false);
      add(buttonPanel, BorderLayout.CENTER);

      editProgramlistButton = new JButton("Edit Program List...");
      editProgramlistButton.addActionListener(buttonHandler);
      add(editProgramlistButton, BorderLayout.SOUTH);

      //Show all the hard work
      setVisible(true);
      pack();
      setLocationRelativeTo(null);
    } //End 'createAndShowGUI'
    
    private LinkedList<String> loadProgramList(String filename) {
        LinkedList<String> queue = new LinkedList<String>();
        boolean betweenWaitIsSet = false; //Retrieve the initial value for 'betweenWait'. Subsequent sets should be executed at their point in the queue.
        //Load in the combined program list/settings file
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                debugPrint(Util.DEBUG_VERBOSE, "LINE: " + line);

                //Ignore comments and lines that are either empty or too short
                if (!line.startsWith("#") && line.length() > 3) {
                    String lineLowerCase = line.toLowerCase();
                    //Retrieve the first wait period
                    if (lineLowerCase.startsWith("startwait")) {
                        //Get the number next to 'startWait'
                        startWait = Util.convertStringToInt(Util.getArgumentFromLine(line), 1) * 1000;
                    }
                    else if (lineLowerCase.startsWith("betweenwait") && betweenWaitIsSet == false) {
                        betweenWaitIsSet = true;
                        betweenWait = Util.convertStringToInt(Util.getArgumentFromLine(line), 1) * 1000;
                    }
                    //If the line contains any permutation of "Always on top", make sure the window is just that
                    else if (lineLowerCase.replaceAll(" ", "").equals("alwaysontop")) {
                        setAlwaysOnTop(true);
                    }
                    //For everything else, store it in the queue for later
                    else {
                        queue.add(line);
                    }
                }
                else Util.debugPrint(Util.DEBUG_DETAILED, "Discarding line '"+line+"'");
            }
            scanner.close();
        } catch (FileNotFoundException fnfe) {
            debugPrint(Util.DEBUG_ALWAYS, "Program list not found! (" + fnfe + ")");
        } catch (IOException ioe) {
            debugPrint(Util.DEBUG_ALWAYS, "ERROR while reading the program list: " + ioe);
        }
        debugPrint(Util.DEBUG_BASIC, "Items in ProgramQueue: " + queue.size());
        
        return queue;
    } //End of 'loadProgramList'
    
    
    private void startProgramLoop() {
        //Set up the initial progress bar data
        progressBar.setMaximum(100);
        progressBar.setValue(100);
        progressBar.setStringPainted(true);
        repaint();
        
        //Start up the background thread for reading through the program queue
        worker = new ProgramLoopWorker(this);
        worker.execute();
        
        //Now that everything is properly set up, enable the control buttons
        startButton.setEnabled(true);        
        skipButton.setEnabled(true);
        pauseButton.setEnabled(true);
        quitButton.setEnabled(true);
        updatePauseButton();
    }
    
    protected void startProgram(String programLocation) {
        //TODO: Move 'startProgram' function to the Worker?
        debugPrint(Util.DEBUG_BASIC, "Starting program '"+programLocation+"'.");
        try {
            File programFile = new File(programLocation);
            if (!programFile.exists()) throw new FileNotFoundException();
            //Desktop.getDesktop().open(programFile);
            ///Runtime.getRuntime().exec("cmd /c start \"StartupHelper\" \""+programLocation+"\"");
            fileLauncher.open(programLocation);
        } catch (FileNotFoundException fnfe) {
            debugPrint(Util.DEBUG_ALWAYS, "ERROR, file '"+programLocation+"' not found ("+fnfe+")");
            //showError("File '"+programLocation+"' was not found. Please check for spelling errors.");
            errorList.add("File '"+programLocation+"' was not found. Please check for spelling errors.");
        } catch (IOException ioe) {
            debugPrint(Util.DEBUG_ALWAYS, "ERROR while opening '"+programLocation+"': "+ioe);
            //showError("Error while trying to start '"+programLocation+"\n("+ioe+")");
            errorList.add("IO error while trying to start '"+programLocation+" ("+ioe+")");
        } catch (SecurityException se) {
            debugPrint(Util.DEBUG_ALWAYS, "Security ERROR while trying to open '"+programLocation+"': "+se);
            errorList.add("Read error while trying to start '"+programLocation+" ("+se+")");
        } catch (Exception e) {
            debugPrint(Util.DEBUG_ALWAYS, "UNKNOWN ERROR: "+e);
            //showError("Generic error while trying to start '"+programLocation+"': "+e, "ERROR");
            errorList.add("Generic error while trying to start '"+programLocation+"': "+e);
        }
    }
    protected void updateDisplay(long percentage) {
        progressBar.setValue((int) percentage);
        //pack();
        repaint();
    }
    protected void setProgressbarString(String text) {
        progressBar.setString(text);
    }
    
    protected void updatePauseButton() {
        if (worker.isPaused()) pauseButton.setText("Resume");
        else pauseButton.setText("Pause");
    }
    
    //This method is used by the Start and Skip buttons, to skip waiting for the countdown
    //Both buttons do basically the same thing, with the only difference being started the program yes or no
    protected void skipWaitingForProgramStart(boolean startProgram) {
        //First make sure the worker is actually running, to prevent confusion
        boolean workerIsPaused = worker.isPaused();
        worker.setPaused(false);
        //Then make sure the program gets started as appropriate, and move beyond it
        worker.setStartProgram(startProgram);
        worker.setProgramStartTime(System.currentTimeMillis() - 1);
        
        if (workerIsPaused) {
            //Give the worker some time to start the program
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                Util.debugPrint(Util.DEBUG_ALWAYS, "ERROR: Skip/Start button sleep interrupted! ("+ie+")");
            }
            worker.setPaused(true);
        }
        //Make sure the pause button still displays the correct text
        updatePauseButton();
    }
    
    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    protected LinkedList<String> getErrorList() {
        return errorList;
    }
    
    
    private class ButtonHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            debugPrint(Util.DEBUG_VERBOSE, "Button pressed: "+e.getSource().toString());
            
            if (e.getSource() == startButton) {
                skipWaitingForProgramStart(true);
            }
            else if (e.getSource() == pauseButton) {
                //Toggle whether the worker is paused or not
                worker.setPaused(!worker.isPaused());
                //Update the button text
                updatePauseButton();
                
                pack();
            }
            else if (e.getSource() == skipButton) {
                skipWaitingForProgramStart(false);
            }
            else if (e.getSource() == quitButton) exit();
            else if (e.getSource() == editProgramlistButton) {
                ProgramlistEditor pe = new ProgramlistEditor(programlistLocation);
                worker.cancel(false);
                dispose();
            }
        }        
    }
    
    private class WindowHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            //Cleanly exit, including stopping the background thread
            exit(0);
        }
    }
}
