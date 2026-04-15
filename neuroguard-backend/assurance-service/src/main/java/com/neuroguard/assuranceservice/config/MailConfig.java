package com.neuroguard.assuranceservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@Slf4j
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Try System properties first (from .env via EnvConfig), then System environment, then defaults
        String host = System.getProperty("MAIL_HOST") != null ? System.getProperty("MAIL_HOST") : System.getenv("MAIL_HOST");
        String port = System.getProperty("MAIL_PORT") != null ? System.getProperty("MAIL_PORT") : System.getenv("MAIL_PORT");
        String username = System.getProperty("MAIL_USERNAME") != null ? System.getProperty("MAIL_USERNAME") : System.getenv("MAIL_USERNAME");
        String password = System.getProperty("MAIL_PASSWORD") != null ? System.getProperty("MAIL_PASSWORD") : System.getenv("MAIL_PASSWORD");

        mailSender.setHost(host != null && !host.isEmpty() ? host : "smtp.gmail.com");
        mailSender.setPort(port != null && !port.isEmpty() ? Integer.parseInt(port) : 587);
        mailSender.setUsername(username != null && !username.isEmpty() ? username : "");
        mailSender.setPassword(password != null && !password.isEmpty() ? password : "");

        log.info("Mail Configuration:");
        log.info("   Host: {}", mailSender.getHost());
        log.info("   Port: {}", mailSender.getPort());
        log.info("   Username: {}", mailSender.getUsername() != null && !mailSender.getUsername().isEmpty() ? "***configured***" : "NOT SET");
        log.info("   Password: {}", mailSender.getPassword() != null && !mailSender.getPassword().isEmpty() ? "***configured***" : "NOT SET");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}
