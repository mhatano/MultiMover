package jp.hatano.multimover;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiMover {
    public static void main(String[] args) throws URISyntaxException {
        new MultiMover(args);
    }
        
    private boolean dryRun = false;
    private boolean verbose = false;
    private boolean helpMessage = false;
    private String srcPattern = null;
    private String dstPattern = null;
    private String sourceDir = ".";
    private String targetDir = ".";
    private final Class<?> theClass = this.getClass();
    private final URL location = theClass.getProtectionDomain().getCodeSource().getLocation();
    private String simpleClassName = null;
    private String fullClassName = null;
    
    public MultiMover(String[] args) throws URISyntaxException {
        if ( location == null ) {
            String message = "Cannot detect running directory and executable class name or filename.";
            if ( verbose ) {
                System.out.println(message);
            } else {
                System.err.println(message);
            }
        } else {
            String jarPath = location.getPath();
            if ( jarPath.endsWith(".jar") ) {
                if ( verbose ) {
                    System.out.printf("Running from JAR: %s\n", jarPath);
                }
                simpleClassName = new File(location.toURI().getPath()).getName();
                fullClassName = simpleClassName;
            } else {
                if ( verbose ) {
                    System.out.printf("Running from directory: %s\n", jarPath);
                }
                simpleClassName = theClass.getSimpleName();
                fullClassName   = theClass.getName();
            }
        }
        
        if (args.length >= 1) {
            analyzeArgs(args);
        }
        
        
        if ( helpMessage ) {
            showHelp(verbose, simpleClassName, fullClassName);
        } else if ( args.length < 2 || srcPattern == null || dstPattern == null ) {
            System.out.printf("Usage: java %s%s [options] <source-pattern> <destination-pattern>\n",(simpleClassName.equals(fullClassName))?"-jar ":"",fullClassName);
            System.exit(1);
        }
        
        
        // Extracting %n{pattern}
        Pattern tokenPattern = Pattern.compile("%(\\d+)\\{([^}]+)\\}");
        Matcher m = tokenPattern.matcher(srcPattern);
        
        
        StringBuilder regexBuilder = new StringBuilder();
        int lastEnd = 0;
        while (m.find()) {
            regexBuilder.append(Pattern.quote(srcPattern.substring(lastEnd, m.start())));
            regexBuilder.append("(").append(m.group(2)).append(")");
            lastEnd = m.end();
        }
        regexBuilder.append(Pattern.quote(srcPattern.substring(lastEnd)));
        Pattern filePattern = Pattern.compile(regexBuilder.toString());
        
        File dir = new File(sourceDir);
        
        boolean matched = false;
        if ( dir.listFiles() != null ) {
            for (File f : dir.listFiles()) {
                if (!f.isFile()) continue;
                Matcher fm = filePattern.matcher(f.getName());
                if (fm.matches()) {
                    String newName = dstPattern;
                    // Replacing %1, %2, ...
                    for (int i = 1; i <= fm.groupCount(); i++) {
                        newName = newName.replace("%" + i, fm.group(i));
                    }
                    File newFile = new File(targetDir, newName);
                    if ( dryRun ) {
                        System.out.printf("%s : Would move: %s -> %s\n",simpleClassName,f.getPath(),newFile.getPath());
                    } else {
                        if ( f.renameTo(newFile) ) {
                            if ( verbose ) {
                                System.out.printf("%s: Moved %s -> %s\n",simpleClassName,f.getPath(),newFile.getPath());
                            }
                        } else {
                            failedToRename(f, newName, newFile);
                            String errReason = "";
                            if ( newFile.exists() ) {
                                errReason = "Destination file already exists.";
                            } else if ( !newFile.canWrite() ) {
                                errReason = "Directory is read-only.";
                            } else {
                                errReason = "Some reason, which this time is not tracked down.";
                            }
                            String message = "%s : Failed to move: %s -> %s (%s)\n".formatted(simpleClassName,f.getPath(),newFile.getPath(),errReason);
                            if ( verbose ) {
                                System.out.print(message);
                            } else {
                                System.err.print(message);
                            }
                        }
                    }
                    matched = true;
                }
            }
        }
        if ( !matched ) {
            String message = "%s : Found no match to pattern: %s\n".formatted(simpleClassName,srcPattern);
            if ( verbose ) {
                System.out.print(message);
            } else {
                System.err.print(message);
            }
        }
    }
    
    
    private void failedToRename(File f, String newName, File newFile) {
        String errReason = "";
        if ( newFile.exists() ) {
            errReason = "Destination file already exists.";
        } else if ( !newFile.canWrite() ) {
            errReason = "Directory is read-only.";
        } else {
            errReason = "Some reason, which this time is not tracked down.";
        }
        String message = "%s : Failed to rename: %s -> %s (%s)\n".formatted(simpleClassName,f.getName(),newName,errReason);
        if ( verbose ) {
            System.out.print(message);
        } else {
            System.err.print(message);
        }
    }
    
    
    private void analyzeArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-d") || arg.equals("--dryrun")) {
                dryRun = true;
            } else if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                helpMessage = true;
            } else if (arg.equals("-s") || arg.equals("--sourcedir")) {
                if (i + 1 < args.length) {
                    sourceDir = args[++i];
                    continue;
                } else {
                    System.out.printf("%s : Error. Option '%s' requires an argument.\n", simpleClassName, arg);
                    System.exit(1);
                }
            } else if (arg.equals("-t") || arg.equals("--targetdir")) {
                if (i + 1 < args.length) {
                    targetDir = args[++i];
                    continue;
                } else {
                    System.out.printf("%s : Error. Option '%s' requires an argument.\n", simpleClassName, arg);
                    System.exit(1);
                }
            } else if (arg.startsWith("-")) {
                System.out.printf("%s : Error. Unknown Option '%s'\n", simpleClassName, arg);
                System.exit(1);
            } else if (srcPattern == null) {
                srcPattern = arg;
            } else if (dstPattern == null) {
                dstPattern = arg;
            }
        }
    }
    
    
    private void showHelp(boolean verbose, String simpleClassName, String fullClassName) {
        System.out.printf( "Usage: java %s%s [options] <source-pattern> <destination-pattern>\n",(simpleClassName.equals(fullClassName))?"-jar ":"",fullClassName);
        System.out.println("Options:");
        System.out.println("  -d, --dryrun     \t: Do not actually move files, just show what would be done.");
        System.out.println("  -v, --verbose    \t: Show verbose output.");
        System.out.println("  -h, --help       \t: Show this help message.");
        System.out.println("  -s, --sourcedir  \t: Source directory to search for files (default: current directory).");
        System.out.println("  -t, --targetdir  \t: Target directory to move files to (default: current directory).");
        System.out.println("Source patterns can use %<n>{pattern} to match and destination pattern can use %<n> which are parts of filenames specified in the source pattern.");
        if ( verbose ) {
            System.out.printf("\nExample: java %s%s -s sourcedir -t targetdir 'file-%%1{\\d+}.txt' 'renamed-file-%%1.txt'\n",(simpleClassName.equals(fullClassName))?"-jar ":"",fullClassName);
            System.out.println("\t* This will move files like 'file-123.txt' in sourcedir to 'renamed-file-123.txt' in targetdir.");
            System.out.println("\t* The %<n>{pattern} syntax allows you to specify a regex pattern to match parts of the filename.");
            System.out.println("\t* The destination pattern can use %1, %2, ... to reference the captured groups from the source pattern.");
            System.out.println("\t* If the source pattern is not valid, it will print an error message and exit.");
            System.out.println("\t* If the destination pattern is not valid, it will print an error message and exit.");
            System.out.println("\t* If the option '--dryrun' (or '-d') is given, it will show the list of candidate move actions,");
            System.out.println("\t  so you can check the behavior and correctness of patterns before it actually moves files.");
        }
        System.exit(1);
    }
}
