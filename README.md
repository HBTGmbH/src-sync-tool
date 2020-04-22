# src-sync-tool
Syncs source directories, useful for refactoring activities.

Note: Sources in dest-dir have to exist.

## Usage with Gradle
* Run the ``shell`` task from build.gradle

## Usage with prebuilt jar (preferred)
For interactive mode, run:

```
java -jar src-sync-tool-1.0-all.jar
```
For non-interactive mode, use
```
java -jar src-sync-tool-1.0-all.jar <srcdir> <destdir>
```
