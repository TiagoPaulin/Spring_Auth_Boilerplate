package com.boilerplate.springauth.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp () {

        ReflectionTestUtils.setField(mailService, "from", "no-reply@test.com");
        ReflectionTestUtils.setField(mailService, "resetPasswordUrl", "http://localhost:3000/reset-password");

    }

    @Test
    @DisplayName("Should send password reset email containing the reset link")
    void sendPasswordResetEmail_ShouldSendEmailWithResetLink () {

        mailService.sendPasswordResetEmail("tiago@test.com", "reset-token", 10L);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@test.com", message.getFrom());
        assertArrayEquals(new String[] { "tiago@test.com" }, message.getTo());
        assertTrue(message.getText().contains("http://localhost:3000/reset-password?token=reset-token"));
        assertTrue(message.getText().contains("10 minutes"));

    }

    @Test
    @DisplayName("Should not propagate exception when email delivery fails")
    void sendPasswordResetEmail_ShouldNotThrow_WhenDeliveryFails () {

        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendPasswordResetEmail("tiago@test.com", "reset-token", 10L));

    }

}
