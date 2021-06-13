package com.github.zavier;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mojo( name = "count", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class CountMojo extends AbstractMojo {

    private static final String[] INCLUDES_DEFAULT = {"java", "xml", "properties"};

    @Parameter( defaultValue = "${project.basedir}", required = true, readonly = true )
    private File baseDir;

    @Parameter( defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true )
    private File sourceDirectory;

    @Parameter( defaultValue = "${project.build.testSourceDirectory}", required = true, readonly = true )
    private File testSourceDirectory;

    @Parameter( defaultValue = "${project.build.resources}", required = true, readonly = true )
    private List<Resource> resources;

    @Parameter( defaultValue = "${project.build.testResources}", required = true, readonly = true )
    private List<Resource> testResources;

    private String[] includes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (includes == null || includes.length == 0) {
            includes = INCLUDES_DEFAULT;
        }

        try {
            countDir(sourceDirectory);
            countDir(testSourceDirectory);

            for (Resource resource : resources) {
                countDir(new File(resource.getDirectory()));
            }

            for (Resource resource : testResources) {
                countDir(new File(resource.getDirectory()));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("count lines of code error", e);
        }

    }

    private void countDir(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        List<File> collected = new ArrayList<>();
        collectFiles(collected, dir);
        int lines = 0;
        for (File sourceFile : collected) {
            lines += countLine(sourceFile);
        }
        String path = dir.getAbsolutePath().substring(baseDir.getAbsolutePath().length());
        getLog().info(path + ": " + lines + " lines of code in " + collected.size() + " files");
    }

    private void collectFiles(List<File> collected, File file) {
        if (file.isFile()) {
            for (String include : includes) {
                if (file.getName().endsWith("." + include)) {
                    collected.add(file);
                    break;
                }
            }
        } else {
            for (File sub : file.listFiles()) {
                collectFiles(collected, sub);
            }
        }
    }

    private int countLine(File file) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            int line = 0;
            while (bufferedReader.ready()) {
                bufferedReader.readLine();
                line++;
            }
            return line;
        }
    }
}
