package com.aquilaoffice.adapter.out.github;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ProcessGitCommandRunner implements GitCommandRunner {

    @Override
    public String run(GitCommand command) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("git");
        commandLine.addAll(command.arguments());

        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        if (command.workingDirectory() != null) {
            processBuilder.directory(command.workingDirectory().toFile());
        }
        processBuilder.environment().putAll(command.environment());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("git command failed with exit code " + exitCode);
            }
            return output;
        } catch (IOException exception) {
            throw new IllegalStateException("git command failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("git command interrupted", exception);
        }
    }
}
