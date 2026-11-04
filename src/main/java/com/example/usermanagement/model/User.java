package com.example.usermanagement.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A user managed by the service")
public class User {

    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @Schema(description = "First name", example = "Alice")
    private String firstName;

    @Schema(description = "Last name", example = "Smith")
    private String lastName;

    @Schema(description = "Age in years", example = "30")
    private int age;

    @Schema(description = "Gender", example = "FEMALE")
    private Gender gender;
}
