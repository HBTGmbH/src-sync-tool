package de.hbt.tools.sourcesync;

import org.apache.commons.io.FileUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SourceSyncTool {

    private static final Set<String> IGNORE_FILES = new HashSet<>();
    static {
        IGNORE_FILES.add(".DS_Store");
    }

    public static void main(String[] args) {
        try {
            runSync(args);
        } catch (Exception e) {
            System.out.println("Failed to sync: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runSync(String[] args) throws Exception {
        String sourceDir;
        String destDir;
        if(args.length != 2) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please specify source dir ");
            sourceDir = br.readLine();
            System.out.println("Please specify dest dir");
            destDir = br.readLine();
        } else {
            sourceDir = args[0];
            destDir = args[1];
        }


        if(!new File(sourceDir).isDirectory()) {
            throw new IllegalArgumentException("Source dir " + sourceDir + " is not a directory!");
        }
        if(!new File(destDir).isDirectory()) {
            throw new IllegalArgumentException("Dest dir " + destDir + " is not a directory!");
        }
        System.out.println("Syncing sources from " + sourceDir + " to " + destDir);
        Collection<File> allDestFiles = FileUtils.listFiles(new File(destDir), null, true);
        System.out.println("Found " + allDestFiles.size() + " files");
        allDestFiles.stream().filter(f -> !IGNORE_FILES.contains(f.getName())).forEach(destFile -> {
            String sourceFilePath =  sourceDir + destFile.getAbsolutePath().substring(destDir.length());
            File sourceFile = new File(sourceFilePath);
            if(!sourceFile.exists()) {
                System.err.println(sourceFilePath + " no longer exists. Cannot update dest file! Check the reason manually.");
            } else {
                try {
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText(FileUtils.readFileToByteArray(sourceFile));
                    CharsetMatch[] matches = detector.detectAll();
                    if(matches.length > 0) {
                        // always use best match of charset
                        String sourceCharset = matches[0].getName();
                        File tempFile = File.createTempFile(sourceFile.getName(), ".tmp");
                        String sourceContentWithLf = FileUtils.readFileToString(sourceFile, sourceCharset)
                                .replaceAll("\\r\\n", "\n")
                                .replaceAll("\\r", "\n");
                        FileUtils.write(tempFile, sourceContentWithLf, "UTF-8");
                        if(FileUtils.checksumCRC32(tempFile) != FileUtils.checksumCRC32(destFile)) {
                            FileUtils.copyFile(tempFile, destFile);
                            System.out.println("Updated " + destFile.getAbsolutePath());
                        }
                        tempFile.delete();
                    } else {
                        System.err.println("Could not detect encoding of source file " + sourceFilePath);
                    }
                } catch(IOException e) {
                    System.err.println("Could not check or copy source file " + sourceFilePath);
                    e.printStackTrace();
                }
            }
        });
    }
}
