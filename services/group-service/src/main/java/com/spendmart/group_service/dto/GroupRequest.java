package com.spendmart.group_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupRequest {

    @NotBlank
    private String name;
}
