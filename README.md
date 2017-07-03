# StartupHelper

An old project I made in 2011/2012. Here is the readme I wrote then, slightly edited:

## Description
StartupHelper lets users choose which programs are launched when the OS boots up. It also allows for waiting periods between launching programs, to prevent programs slowing each other down. During this startup process, the user can control the waiting period through the StartupHelper GUI, pausing the program, skipping a single program, or foregoing the waiting period and launching the next program immediately.

## How To Use
When StartupHelper is first started, you will be prompted to create the list of programs you want it to start. Hopefully the buttons on the right side are self-explanatory. The numbers at the bottom of the window are the waiting times. 'StartWait' means the time StartupHelper will wait when it is started, before it starts the first program from the list. 'BetweenWait' is the time StartupHelper will wait between starting programs from the list. The 'Always On Top' checkbox sets whether or not the StartupHelper window will stay above all other programs during its run.
When StartupHelper has started all programs from the list, it will exit.
If StartupHelper can't find the program specified in the list, it will show an error, after starting the other programs. An error log will also be created in the folder you saved StartupHelper.

## Command Line Options (Advanced)
If you want StartupHelper to launch into the Programlist Editor, skipping the starting of programs entirely, add '-editor' as a command line parameter.
To set the debug level, add '-debug:[level]'. The possibilities for '[level]' are 'none' or '0', 'test' or '1', 'basic' or '2', 'detailed' or '3', and 'verbose' or '4'. A higher level produces more detailed and more frequent debug messages. If you want to pinpoint the cause of a problem, gradually increase the debug level until a debug message about the specific problem is outputted.

## Manually Add StartupHelper To The OS Startup List
In Windows, placing a shortcut to the .jar file in the 'Startup' folder of the Start Menu will make sure StartupHelper is launched when Windows is started.
Since I'm unfamiliar with Linux, and since there are a lot of different distributions, I don't know how to add StartupHelper to the bootup of the OS. But since you're brave enough to use Linux in the first place, I'm sure you can figure it out.
I am unfamiliar with Mac OSX too, but this page explains how to add it to the bootup there: http://www.zimbio.com/Mac+OS+X/articles/43/OS+X+startup+programs

## Bugs And Suggestions
This project is made in Java, so it should be multiplatform. However, I've only been able to test it on a Windows XP machine, so please let me know if there are oddities, bugs and/or malfunctions on other platforms.
Please report all bugs and suggestions.
