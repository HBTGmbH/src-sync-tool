package de.hbt.tools.sourcesync;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PotentialNewFileCheck {
    String sourceDir;
    String destDir;

    public PotentialNewFileCheck(String sourceDir, String destDir) {
        this.sourceDir = sourceDir;
        this.destDir = destDir;
    }

    public void checkForPotentiallyNewFiles(int days) {
        Collection<File> allDestFiles = FileUtils.listFiles(new File(destDir), null, true);
        Collection<String> allDestFilePaths = allDestFiles.stream()
                .map(File::getName)
                .collect(Collectors.toSet());
        Collection<File> allSourceFiles = FileUtils.listFiles(new File(sourceDir), null, true);

        List<File> potentiallyChangedFiles = allSourceFiles.stream()
                .filter(sourceFile -> !allDestFilePaths.contains(sourceFile.getName()))  // Exclude all dest files from source files
                .filter(isYoungerThan(days)) // exclude all that are older than days
                .filter(isRelevantByDirectory(allDestFiles))
                .collect(Collectors.toList());
        System.out.println("Found " + potentiallyChangedFiles.size() + " potentially changed files.");
        potentiallyChangedFiles.stream()
                .map(File::getAbsolutePath)
                .forEach(System.out::println);
    }



    // The potentially changed file must be relevant by directory, meaning
    // That it must be in a directory with at least one synced file
    private Predicate<File> isRelevantByDirectory(Collection<File> allDestFiles) {
        Set<String> allDestDirectoryNames = allDestFiles.stream()
                .map(File::getParentFile)
                .filter(this::hasAtLeastOneFile)
                .map(this::toDestFilePath)
                .collect(Collectors.toSet());
        return file -> {
            File parentFile = file.getParentFile();
            return allDestDirectoryNames.contains(toSourceFilePath(parentFile));
        };
    }

    private boolean hasAtLeastOneFile(File file) {
        return !FileUtils.listFiles(file, null, false).isEmpty();
    }

    private String toDestFilePath(File file) {
        return file.getAbsolutePath().substring(destDir.length());
    }

    private String toSourceFilePath(File file) {
        return file.getAbsolutePath().substring(sourceDir.length());
    }

    private Predicate<File> isYoungerThan(int days)  {
        return file -> {
            Date date = new Date(file.lastModified());
            Date refDate = DateUtils.addDays(date, days);
            return refDate.after(date);
        };
    }
}
