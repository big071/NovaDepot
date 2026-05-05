package com.novadepot.backend.modules.partners;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerRequest {
    @NotBlank
    @Size(max = 64)
    private String partnerCode;
    @NotBlank
    @Size(max = 128)
    private String partnerName;
    @NotBlank
    @Size(max = 32)
    private String partnerType;
    @Size(max = 64)
    private String contactName;
    @Size(max = 32)
    private String phone;
    @Size(max = 255)
    private String address;
    @Size(max = 500)
    private String remark;
}
