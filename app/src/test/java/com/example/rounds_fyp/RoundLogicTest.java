package com.example.rounds_fyp;

import org.junit.Test;
import static org.junit.Assert.*;

public class RoundLogicTest {

    // --- Business Logic ---

    public boolean isValidContribution(double amount) {
        return amount > 0;
    }

    public boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    public boolean isValidRound(String name, int memberCount) {
        return name != null && !name.trim().isEmpty() && memberCount > 1;
    }

    public boolean isValidWithdrawal(double amount, double balance) {
        return amount > 0 && amount <= balance;
    }

    public boolean isValidUserDetails(String fullName, String phone, String nationalId) {
        return fullName != null && phone != null && nationalId != null &&
                !fullName.isEmpty() && phone.length() == 11 && nationalId.length() >= 6;
    }
    @Test
    public void testValidContributionAmount() {
        double input = 100.00;
        boolean result = isValidContribution(input);
        assertTrue(result);
    }

    @Test
    public void testInvalidNegativeContribution() {
        double input = -50;
        boolean result = isValidContribution(input);
        assertFalse(result);
    }

    @Test
    public void testZeroContribution() {
        double input = 0;
        boolean result = isValidContribution(input);
        assertFalse(result);
    }
    public boolean isLoginFieldsValid(String email, String password) {
        return email != null && !email.isEmpty() &&
                password != null && !password.isEmpty();
    }



    // --- Tests ---

    // Contributions
    @Test public void testValidContribution() { assertTrue(isValidContribution(50.0)); }
    @Test public void testInvalidContributionZero() { assertFalse(isValidContribution(0)); }
    @Test public void testInvalidContributionNegative() { assertFalse(isValidContribution(-10)); }

    // Email
    @Test public void testValidEmail() { assertTrue(isValidEmail("user@example.com")); }
    @Test public void testInvalidEmailNoAt() { assertFalse(isValidEmail("userexample.com")); }
    @Test public void testInvalidEmailNoDot() { assertFalse(isValidEmail("user@examplecom")); }

    // Round creation
    @Test public void testValidRound() { assertTrue(isValidRound("April Round", 3)); }
    @Test public void testInvalidRoundOneMember() { assertFalse(isValidRound("April Round", 1)); }
    @Test public void testInvalidRoundNoName() { assertFalse(isValidRound("", 3)); }

    // Withdrawals
    @Test public void testValidWithdrawal() { assertTrue(isValidWithdrawal(30.0, 100.0)); }
    @Test public void testInvalidWithdrawalOverBalance() { assertFalse(isValidWithdrawal(150.0, 100.0)); }

    // Personal details
    @Test public void testValidUserDetails() { assertTrue(isValidUserDetails("Temu", "07576971051", "FN763961")); }
    @Test public void testInvalidUserDetailsEmptyName() { assertFalse(isValidUserDetails("", "07576971051", "FN763961")); }
    @Test public void testInvalidUserDetailsShortPhone() { assertFalse(isValidUserDetails("Temu", "07576", "FN763961")); }
    @Test
    public void testLoginFieldsFilled() {
        String email = "user@example.com";
        String password = "password123";
        boolean result = isLoginFieldsValid(email, password);
        assertTrue(result);
    }

    @Test
    public void testLoginFieldsEmpty() {
        String email = "";
        String password = "";
        boolean result = isLoginFieldsValid(email, password);
        assertFalse(result);
    }

    @Test
    public void testOnlyEmailProvided() {
        String email = "user@example.com";
        String password = "";
        boolean result = isLoginFieldsValid(email, password);
        assertFalse(result);
    }

    @Test
    public void testOnlyPasswordProvided() {
        String email = "";
        String password = "password123";
        boolean result = isLoginFieldsValid(email, password);
        assertFalse(result);
    }

}
