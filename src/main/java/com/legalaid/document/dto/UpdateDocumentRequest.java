package com.legalaid.document.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDocumentRequest {

    @Size(max = 255, message = "Name too long")
    private String name;

    @Size(max = 150, message = "Folder name too long")
    private String folderName;

    private Boolean isStarred;

    private Boolean isShared;
}