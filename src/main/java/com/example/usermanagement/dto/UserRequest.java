package com.example.usermanagement.dto;

import com.example.usermanagement.model.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating or updating a user")
public class UserRequest {

    @NotBlank(message = "firstName is required")
    @Size(max = 50, message = "firstName must be at most 50 characters")
    @Schema(example = "Alice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 50, message = "lastName must be at most 50 characters")
    @Schema(example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Min(value = 0, message = "age must be >= 0")
    @Max(value = 150, message = "age must be <= 150")
    @Schema(example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    private int age;

    @NotNull(message = "gender is required")
    @Schema(example = "FEMALE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Gender gender;
}
