package com.aquilaoffice.adapter.out.github;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

record GitCommand(Path workingDirectory, List<String> arguments, Map<String, String> environment) {

    GitCommand {
        if (arguments == null || arguments.isEmpty()) {
            throw new IllegalArgumentException("arguments must not be empty");
        }
        arguments = List.copyOf(arguments);
        environment = environment == null ? Map.of() : Map.copyOf(environment);
    }

    static GitCommand local(Path workingDirectory, List<String> arguments) {
        return new GitCommand(workingDirectory, arguments, Map.of());
    }

    static GitCommand remote(Path workingDirectory, List<String> arguments, Map<String, String> environment) {
        return new GitCommand(workingDirectory, arguments, environment);
    }
}
