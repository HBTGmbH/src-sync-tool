package de.hbt.tools.sourcesync;

import org.apache.commons.io.FileUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SourceSyncTool {

    private static final Set<String> IGNORE_FILES = new HashSet<>();
    private static final Collection<String> BINARY_FILE_TYPES = Arrays.asList(".jpg", ".png", ".gif", ".jpeg", ".pdf", ".svg", ".gz", ".zip", ".ser", ".jar", ".obj");

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
        boolean doNewFileCheck = false;
        BufferedReader br;
        if (args.length != 2) {
            br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please specify source dir ");
            sourceDir = br.readLine();
            System.out.println("Please specify dest dir");
            destDir = br.readLine();
            System.out.println("Would you like to check for new files? Type (y/n)");
            String yesNo = br.readLine();
            doNewFileCheck = "y".equals(yesNo);
        } else {
            sourceDir = args[0];
            destDir = args[1];
        }


        if (!new File(sourceDir).isDirectory()) {
            throw new IllegalArgumentException("Source dir " + sourceDir + " is not a directory!");
        }
        if (!new File(destDir).isDirectory()) {
            throw new IllegalArgumentException("Dest dir " + destDir + " is not a directory!");
        }
        System.out.println("Syncing sources from " + sourceDir + " to " + destDir);
        Collection<File> allDestFiles = FileUtils.listFiles(new File(destDir), null, true);
        System.out.println("Found " + allDestFiles.size() + " files");
        allDestFiles.stream()
                .filter(f -> !IGNORE_FILES.contains(f.getName()))
                .forEach(destFile -> {
                    String sourceFilePath = sourceDir + destFile.getAbsolutePath().substring(destDir.length());
                    File sourceFile = new File(sourceFilePath);
                    if (!sourceFile.exists()) {
                        System.err.println(sourceFilePath + " no longer exists. Cannot update dest file! Check the reason manually.");
                    } else {
                        if (isBinaryFile(sourceFile)) {
                            syncBinaryContent(destFile, sourceFile);
                        } else {
                            syncTextContent(destFile, sourceFilePath, sourceFile);
                        }

                    }
                });
        if (doNewFileCheck) {
            System.out.println();
            System.out.println("Src-Sync done. Now checking for new potentially new and relevant files.");
            new PotentialNewFileCheck(sourceDir, destDir).checkForPotentiallyNewFiles(30);
        }
    }

    private static void syncBinaryContent(File destFile, File sourceFile) {
        try {
            FileUtils.copyFile(sourceFile, destFile);
            System.out.println("Updated " + destFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not check or copy source file " + sourceFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void syncTextContent(File destFile, String sourceFilePath, File sourceFile) {
        try {
            CharsetDetector detector = new CharsetDetector();
            detector.setText(FileUtils.readFileToByteArray(sourceFile));
            CharsetMatch[] matches = detector.detectAll();
            if (matches.length > 0) {
                for (CharsetMatch match : matches) {
                    try {
                        syncOne(destFile, sourceFile, match.getName());
                        break;
                    } catch (UnsupportedCharsetException e) {
                        System.err.println(match.getName() + " failed for " + sourceFilePath + ": " + e.getMessage());
                    }
                }
            } else {
                System.err.println("Could not detect encoding of source file " + sourceFilePath);
            }
        } catch (IOException e) {
            System.err.println("Could not check or copy source file " + sourceFilePath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to sync " + sourceFilePath);
            throw e;
        }
    }

    private static boolean isBinaryFile(File file) {
        return BINARY_FILE_TYPES.stream().anyMatch(s -> file.getName().endsWith(s));
    }

    private static void syncOne(File destFile, File sourceFile, String sourceCharset) throws IOException {
        File tempFile = File.createTempFile(sourceFile.getName(), ".tmp");
        String sourceContentWithLf = FileUtils.readFileToString(sourceFile, sourceCharset)
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n");
        FileUtils.write(tempFile, sourceContentWithLf, "UTF-8");
        if (FileUtils.checksumCRC32(tempFile) != FileUtils.checksumCRC32(destFile)) {
            FileUtils.copyFile(tempFile, destFile);
            System.out.println("Updated " + destFile.getAbsolutePath());
        }
        tempFile.delete();
    }
}
