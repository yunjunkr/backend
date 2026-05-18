package com.zoopick.server.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private Resource resource;

    @BeforeEach
    void setUp() {
        // @Value 로 주입되는 resource 필드를 리플렉션으로 설정
        ReflectionTestUtils.setField(emailService, "resource", resource);
    }

    @Test
    @DisplayName("인증 메일 전송 - 성공적으로 MimeMessage를 생성하고 전송한다")
    void senderCertificationMail_Success() throws Exception {
        // given
        String email = "test@mju.ac.kr";
        String certificationNumber = "123456";
        String templateContent = "<html>인증번호: ${certificationNumber}</html>";
        InputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));

        MimeMessage mimeMessage = mock(MimeMessage.class);

        given(resource.getInputStream()).willReturn(inputStream);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

        // when
        emailService.senderCertificationMail(email, certificationNumber);

        // then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }
}