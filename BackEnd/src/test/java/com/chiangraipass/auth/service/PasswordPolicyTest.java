package com.chiangraipass.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordsThatMeetEveryRule() {
        assertThat(PasswordPolicy.validate("Abcdef1!")).isEmpty();
        assertThat(PasswordPolicy.validate("Correct-horse-battery-1")).isEmpty();
        assertThat(PasswordPolicy.validate("pass word 1!")).isEmpty();
    }

    @Test
    void rejectsPasswordsShorterThanEightCharacters() {
        assertThat(PasswordPolicy.validate("Ab1!Ab1")).hasValueSatisfying(
                message -> assertThat(message).contains("at least 8 characters"));
    }

    @Test
    void requiresANumber() {
        assertThat(PasswordPolicy.validate("Abcdefg!")).hasValueSatisfying(
                message -> assertThat(message).contains("at least 1 number")
                        .doesNotContain("special character"));
    }

    @Test
    void requiresASpecialCharacterAndDoesNotCountSpacesOrLetters() {
        assertThat(PasswordPolicy.validate("Abcdefg1")).hasValueSatisfying(
                message -> assertThat(message).contains("special character"));
        assertThat(PasswordPolicy.validate("Abcdef12 ")).hasValueSatisfying(
                message -> assertThat(message).contains("special character"));
    }

    @Test
    void rejectsNonEnglishCharacters() {
        String thai = "\u0e23\u0e2b\u0e31\u0e2a\u0e1c\u0e48\u0e32\u0e19" + "1!ab";

        assertThat(PasswordPolicy.validate(thai)).hasValueSatisfying(
                message -> assertThat(message).contains("only English letters"));
    }

    @Test
    void listsEveryUnmetRuleInOneMessage() {
        assertThat(PasswordPolicy.validate("abc")).hasValueSatisfying(message ->
                assertThat(message)
                        .contains("at least 8 characters")
                        .contains("at least 1 number")
                        .contains("special character"));
    }

    @Test
    void rejectsOverlongPasswordsBecauseBcryptOnlyUsesTheFirst72Bytes() {
        assertThat(PasswordPolicy.validate("a".repeat(70) + "1!x")).hasValueSatisfying(
                message -> assertThat(message).contains("at most 72 characters"));
    }

    @Test
    void rejectsMissingPassword() {
        assertThat(PasswordPolicy.validate(null)).contains("Password is required.");
        assertThat(PasswordPolicy.validate("")).contains("Password is required.");
    }
}