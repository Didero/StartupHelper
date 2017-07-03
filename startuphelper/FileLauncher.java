package startuphelper;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

//BIG thanks to: http://stackoverflow.com/questions/325299/cross-platform-way-to-open-a-file-using-java-1-5
//The 'Desktop' class was acting up, and this made things a lot more stable, if less elegant

public class FileLauncher {
    // Created the appropriate instance
    public static FileLauncher getInstance(){
        String os = System.getProperty("os.name").toLowerCase();

        //First check if the Desktop class, which handles opening files platform-independently is supported
        // If it is, use that
        if (Desktop.isDesktopSupported()) {
          if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return new GenericFileLauncher();
          }
        }
        //Otherwise, use a platform-dependent fallback
        FileLauncher fl;
        if ( os.equals("windows 95") || os.equals("windows 98") ){
            fl = new Windows9xFileLauncher();
        }
        else if ( os.indexOf("windows") != -1 || os.indexOf("nt") != -1){
            fl = new WindowsFileLauncher();
        }
        else if ( os.indexOf("mac") != -1 ) {
            fl = new OSXFileLauncher();
        }
        else if ( os.indexOf("linux") != -1) {
            fl = new LinuxFileLauncher();
        } else {
            throw new UnsupportedOperationException(String.format("The platform %s is not supported ",os) );
        }
        return fl;
    }

    // default implementation :( 
    public void open(String filename) throws IOException {
        Runtime.getRuntime().exec(getCommand(filename));
    }
    
    public String getCommand(String filename) {
        throw new UnsupportedOperationException();
    }
}


//If the Desktop class is supported, which is ostensibly meant for this sort of thing, use that
class GenericFileLauncher extends FileLauncher {
  @Override
  public void open(String filename) throws IOException {
    Desktop.getDesktop().open(new File(filename));
  }
}

// One subclass per platform below:
// Each one knows how to handle its own platform   

class LinuxFileLauncher extends FileLauncher {
    @Override
    public String getCommand(String filename) {
        return "xdg-open "+filename;
    }
}
class OSXFileLauncher extends FileLauncher {
    @Override
    public String getCommand(String filename) {
        return "open "+filename;
    }
}
class WindowsFileLauncher extends FileLauncher {
    @Override
    public String getCommand(String filename) {
        //First part between quotes is the title of the command window, otherwise 'start' thinks filename is title
        return "cmd /c start \"StartupHelper Helper\" \""+filename+"\"";
    }
}
class Windows9xFileLauncher extends FileLauncher {
    @Override
    public String getCommand(String filename) {
        //First part between quotes is the title of the command window, otherwise 'start' thinks filename is title
        return "command.com /C start \"StartupHelper Helper\" \""+filename+"\"";
    }
}