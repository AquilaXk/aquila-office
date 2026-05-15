package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {

    CreateWorkspaceCommand toCommand(String creatorGoogleSub) {
        return new CreateWorkspaceCommand(name, description, creatorGoogleSub);
    }
}
