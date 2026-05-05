package com.zoopick.server.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@NullMarked
public class EmailService {
    private static final String SUBJECT = "[명지대 분실물 찾기 서비스] 인증메일 입니다.";

    @Value("classpath:/templates/email-certification.html")
    private Resource resource;
    private final JavaMailSender javaMailSender;

    public void senderCertificationMail(String email, String certificationNumber) throws MessagingException, IOException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");

        String htmlContent = getCertificationMessage(certificationNumber);

        messageHelper.setTo(email);
        messageHelper.setSubject(SUBJECT);
        messageHelper.setText(htmlContent, true);

        javaMailSender.send(message);
    }

    private String getCertificationMessage(String certificationNumber) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            String template = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            return template.replace("${certificationNumber}", certificationNumber);
        }
    }
}