package startuphelper;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class ProgramLoopWorker extends SwingWorker<Void, Long> {
    private final StartupHelper PARENT;
    private boolean isPaused = false, startProgram = true;
    //Don't draw too often
    private final long timeBetweenTriggers = 1000/60; //60 Frames per Second, 1/60 seconds per frame, *1000 for ms
    private long programStartTime, pausedAt;
    int betweenWait, currentWaitTime;
    

    public ProgramLoopWorker(StartupHelper parent) {
        this.PARENT = parent;
    }
    
    protected long getProgramStartTime() {
        return programStartTime;
    }
    protected void setProgramStartTime(long time) {
        programStartTime = time;
    }
    protected boolean isPaused() {
        return isPaused;
    }
    protected void setPaused(boolean isPaused) {
        //Only act if the pause state is a change, to prevent weird behavior when pressing Pause then Quit, or vice versa
        if (isPaused != this.isPaused) {
            if (isPaused) {
                //Take note of the time at which the program was paused, so the pause time can be determined
                pausedAt = System.currentTimeMillis();
            }
            else {
                //Add the time the program was paused to the time the program should be started
                programStartTime += (System.currentTimeMillis() - pausedAt);
            }
            //Finally, resume the countdown thread
            this.isPaused = isPaused;
        }
    }
    protected void setStartProgram(boolean startProgram) {
        this.startProgram = startProgram;
    }
    
    private void addWaitTime(long timeToAdd) {
        programStartTime += timeToAdd;
        currentWaitTime += timeToAdd;
        Util.debugPrint(Util.DEBUG_BASIC, "Adding '"+timeToAdd+"' ms to the wait time. Current wait time is now "+currentWaitTime+" ms. Starting program in "+(programStartTime - System.currentTimeMillis())+" ms.");
    }
     
    
    @Override
    protected Void doInBackground() {
        LinkedList<String> programQueue = PARENT.getProgramQueue();
        
        //Fire immediately
        long triggerTime = System.currentTimeMillis();
        //Set the first wait until a program should be started
        programStartTime = System.currentTimeMillis() + PARENT.getStartWait();
        betweenWait = PARENT.getBetweenWait();
        
        currentWaitTime = PARENT.getStartWait();
        
        PARENT.setProgressbarString(programQueue.peek());
        
        Util.debugPrint(Util.DEBUG_DETAILED, "Current time: "+System.currentTimeMillis()+", launch program at "+programStartTime);        
        
        //Fix brief moment of weird misdrawing on program startup
        PARENT.updateDisplay(100);
        
        //Loop through the program list until there's no programs left
        while (programQueue.size() > 0 && !isCancelled()) {
            //Don't redraw too often, wastes CPU power
            if (System.currentTimeMillis() >= triggerTime) {
                //Update the next fire time
                triggerTime += timeBetweenTriggers;
                
                //Only do something if the worker isn't paused
                if (!isPaused) {
                    //Check if a program needs to be started
                    if (programStartTime <= System.currentTimeMillis()) {
                        String program = programQueue.poll();
                        //A question mark before the program location means ask whether the program should be started
                        if (startProgram == true && program.startsWith("?")) {
                            //Remove the question mark
                            program = program.substring(1);
                            //Ask if it should be started
                            if (JOptionPane.showConfirmDialog(PARENT, "Do you want to start '"+program+"'?", "Start program?", 
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != 0) startProgram = false;
                        }
                        //LAUNCH PROGRAM (if we should)
                        if (startProgram) PARENT.startProgram(program);
                        //Reset the start trigger. If the user wants to skip the next one, they'll have to press 'Skip' again
                        startProgram = true;
                        
                        //Set up the next program to load, if there is any
                        program = programQueue.peek();
                        if (program != null) {                            
                            //Catch any special commands
                            boolean commandFound = true;
                            while (commandFound) {
                                commandFound = false;
                                String programLowerCase = program.toLowerCase();
                                if (programLowerCase.startsWith("wait")) {
                                    //Add to the waiting time
                                    addWaitTime(Util.convertStringToInt(Util.getArgumentFromLine(program), 0)*1000);
                                    commandFound = true;
                                }
                                else if (programLowerCase.startsWith("betweenwait")) {
                                    betweenWait = Util.convertStringToInt(Util.getArgumentFromLine(program), betweenWait)*1000;
                                    commandFound = true;
                                }

                                //If a special command was given, discard it, and retrieve the next real program in line
                                if (commandFound) {
                                    programQueue.poll();
                                    program = programQueue.peek();
                                }
                            }
                            
                            //Calculate the new wait time (after command check since BetweenWait could've changed)
                            programStartTime = System.currentTimeMillis() + betweenWait;
                            currentWaitTime = betweenWait;
                            
                            //If the program name is too long, shorten it
                            if (program.length() > 40) {
                                program = program.substring(0, 18) + " ... "+program.substring(program.length()-18);
                            }
                            //Display the new program to be started next                        
                            PARENT.setProgressbarString(program);
                        } //End 'program=null' check for end of program queue
                    } //End of check for program start time

                    //Time left: executeTime - currentTime
                    //Time left as percentage: timeLeft / totalTimeLeft * 100
                    Util.debugPrint(Util.DEBUG_VERBOSE, "Current time: "+System.currentTimeMillis()+", program start time: "+programStartTime+", current wait time: "+currentWaitTime);
                    long percentageWaitLeft = 100* (programStartTime - System.currentTimeMillis()) / currentWaitTime;
                    publish(percentageWaitLeft);
                } //End of pause check
            } //End of trigger time check, to prevent firing too often
            //If there's no need to fire yet, rest a while
            //*
            else {
                try {
                    Thread.sleep(triggerTime - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Util.debugPrint(Util.DEBUG_ALWAYS, "ERROR: Worker sleep method interrupted ("+ie+")");
                }
            }
            /**/
        } //End queue-reader while-loop
        Util.debugPrint(Util.DEBUG_BASIC, "Reached end of program list, exiting.");
        return null;
    }//End of 'doInBackground'
    
    @Override
    protected void process(List<Long> percentageList) {
        //Get the current page
        long percentage = percentageList.get(percentageList.size()-1);
        PARENT.updateDisplay(percentage);
    }
    
    @Override
    protected void done() {
        //Show the list of errors, if there are any
        if (PARENT.getErrorList().size() > 0) {
            String message = "";
            for (String error : PARENT.getErrorList()) {
                message += error+"\n";
            }
            message += "\nA log file has been created in the StartupHelper directory with the contents of this error message.";
            
            try {
                //Open a file to write to
                BufferedWriter writer = new BufferedWriter(new FileWriter("ErrorLog.txt"));
                writer.write(new Date().toString());
                writer.newLine();
                //Iterate over all the lines, writing each one down
                for (String error : PARENT.getErrorList()) {
                    writer.write(error);
                    writer.newLine();
                }
                //Make sure all the data is written to disk
                writer.flush();
                //Writer isn't needed anymore
                writer.close();
            } catch (FileNotFoundException fnfe) {
                Util.debugPrint(Util.DEBUG_ALWAYS, "ERROR while writing error log: "+fnfe);
            } catch (IOException ioe) {
                Util.debugPrint(Util.DEBUG_ALWAYS, "IO ERROR while writing error log: "+ioe);
            }
            
            //Finally, show the errors
            JOptionPane.showMessageDialog(PARENT, message, "Errors Occured!", JOptionPane.ERROR_MESSAGE);
        }
        if (!isCancelled()) System.exit(0);
    }
}
