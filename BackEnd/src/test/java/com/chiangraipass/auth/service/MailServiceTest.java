package com.chiangraipass.auth.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MailServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    @Test
    void logModeNeedsNoMailServer() {
        MailService service = new MailService(provider, "log", "from@example.test");

        assertThatCode(() -> service.send("a@example.test", "Hi", "Body")).doesNotThrowAnyException();
        verifyNoInteractions(provider);
    }

    @Test
    void smtpModeSendsThroughTheConfiguredServer() {
        JavaMailSender sender = mock(JavaMailSender.class);
        given(provider.getIfAvailable()).willReturn(sender);

        new MailService(provider, "smtp", "from@example.test").send("a@example.test", "Hi", "Body");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("from@example.test");
        assertThat(message.getValue().getTo()).containsExactly("a@example.test");
        assertThat(message.getValue().getSubject()).isEqualTo("Hi");
        assertThat(message.getValue().getText()).isEqualTo("Body");
    }

    @Test
    void smtpModeWithoutAServerFailsLoudly() {
        given(provider.getIfAvailable()).willReturn(null);
        MailService service = new MailService(provider, "smtp", "from@example.test");

        assertThatThrownBy(() -> service.send("a@example.test", "Hi", "Body"))
                .isInstanceOf(IllegalStateException.class);
    }
}