package com.mindmate.mindmate_server.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignUpRequest {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "확인 비밀번호를 입력해주세요.")
    private String confirmPassword;

    @AssertTrue(message = "개인 정보 동의를 체크해주세요.")
    private boolean agreeToTerm;

//    @NotBlank(message = "닉네임을 입력해주세요.")
//    @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
//    private String nickname;
//
//    @NotBlank(message = "학과를 입력해주세요.")
//    @Size(max = 20, message = "학과는 20자 이하여야 합니다.")
//    private String department;
//
//    private String imgUrl;
//
//    @NotNull(message = "입학 연도를 입력해주세요.")
//    private Integer entranceTime;
//
//    private boolean graduation;

}
