package jp.hatano.multimover;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.invoke.MethodHandles;

public class MultiMover {
    public static void main(String[] args) throws URISyntaxException {
        boolean dryRun = false;
        boolean verbose = false;
        boolean helpMessage = false;
        String srcPattern = null;
        String dstPattern = null;
        Class theClass = MethodHandles.lookup().lookupClass();
        URL location = theClass.getProtectionDomain().getCodeSource().getLocation();
        String simpleClassName = null;
        String fullClassName = null;

        if (args.length >= 1) {
            for ( String arg : args ) {
                if ( arg.equals("-d") || arg.equals("--dryrun") ) {
                    dryRun = true;
                } else if ( arg.equals("-v") || arg.equals("--verbose") ) {
                    verbose = true;
                } else if ( arg.equals("-h") || arg.equals("--help") ) {
                    helpMessage = true;
                } else if ( arg.startsWith("-") ) {
                    System.out.printf("%s : Error. Unknown Option '%s'\n",simpleClassName,arg);
                    System.exit(1);
                } else if ( srcPattern == null ) {
                    srcPattern = arg;
                } else if ( dstPattern == null ) {
                    dstPattern = arg;
                }
            }
        }

        if ( location != null ) {
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
        } else {
            System.out.println("Cannot detect running directory and executable class name or filename.");
        }

        if ( helpMessage ) {
            System.out.printf( "Usage: java %s%s [options] <source-pattern> <destination-pattern>\n",(simpleClassName.equals(fullClassName))?"-jar ":"",fullClassName);
            System.out.println("Options:");
            System.out.println("  -d, --dryrun \t: Do not actually rename files, just show what would be done.");
            System.out.println("  -v, --verbose\t: Show verbose output.");
            System.out.println("  -h, --help   \t: Show this help message.");
            System.out.println("Source patterns can use %<n>{pattern} to match and destination pattern can use %<n> which are parts of filenames specified in the source pattern.");
            if ( verbose ) {
                System.out.printf("\nExample: java %s%s 'file-%%1{\\d+}.txt' 'renamed-file-%%1.txt'\n",(simpleClassName.equals(fullClassName))?"-jar ":"",fullClassName);
                System.out.println("\t* This will rename files like 'file-123.txt' to 'renamed-file-123.txt'.");
                System.out.println("\t* The %<n>{pattern} syntax allows you to specify a regex pattern to match parts of the filename.");
                System.out.println("\t* The destination pattern can use %1, %2, ... to reference the captured groups from the source pattern.");
                System.out.println("\t* If the source pattern does not match any files, it will print a message and exit.");
                System.out.println("\t* If the source pattern is not valid, it will print an error message and exit.");
                System.out.println("\t* If the destination pattern is not valid, it will print an error message and exit.");
                System.out.println("\t* If the option '--dryrun' (or '-d') is given, it will show the list of candidate of renaming actions,");
                System.out.println("\t  so you can check the behavior and correctness of patterns before it actually renames.");
            }
            System.exit(1);
        } else if ( args.length < 2 || srcPattern == null || dstPattern == null ) {
            System.out.printf("Usage: java %s  [-d|--dryrun] [-v|--verbose] [-h|--help] <srcPattern> <dstPattern>\n",fullClassName);
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

        File dir = new File(".");
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
                    File newFile = new File(f.getParent(), newName);
                    if ( dryRun ) {
                        System.out.printf("%s : Would rename: %s -> %s\n",simpleClassName,f.getName(),newName);
                    } else {
                        if ( f.renameTo(newFile) ) {
                            if ( verbose ) {
                                System.out.printf("%s: Renamed %s -> %s\n",simpleClassName,f.getName(),newName);
                            }
                        } else {
                            if ( verbose ) {
                                System.out.printf("%s : Failed to rename: %s\n",simpleClassName,f.getName());
                            }
                        }
                    }
                    matched = true;
                }
            }
        }
        if ( !matched ) {
            System.out.printf("%s : Found no match to pattern: %s\n",simpleClassName,srcPattern);
        }
    }
}
