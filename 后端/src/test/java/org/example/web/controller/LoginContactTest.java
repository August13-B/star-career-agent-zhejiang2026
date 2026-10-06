package org.example.web.controller;

import org.example.web.entity.User;
import org.example.web.service.UserService;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SHA_256;
import org.example.web.tool.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginContactTest {
    @BeforeAll
    static void configureTestJwt() {
        new JwtUtil("test-only-jwt-signing-key-32-bytes-long");
    }

    @ParameterizedTest
    @CsvSource({"auto,account", "auto,email", "auto,nickname", "userAccount,account",
            "email,email", "nickname,nickname", ",account"})
    void allLoginWaysReturnPlainContactsInsideTransportEncryption(String way, String kind) throws Exception {
        var controller = new UserController();
        var users = mock(UserService.class);
        var crypto = mock(RSA_256.class);
        ReflectionTestUtils.setField(controller, "userService", users);
        ReflectionTestUtils.setField(controller, "rsa256", crypto);
        String identifier = switch (kind) {
            case "email" -> "test@example.test";
            case "nickname" -> "测试昵称";
            default -> "STUTEST";
        };
        var user = new User();
        user.setId(1L);
        user.setUserAccount("STUTEST");
        user.setUserPassword(SHA_256.sha256("test-password"));
        user.setUserRole(1);
        user.setUserStatus(1);
        boolean emailLookup = "email".equals(kind);
        user.setEmail(emailLookup ? "test@example.test" : "stored-email");
        user.setPhone(emailLookup ? "13800000000" : "stored-phone");
        switch (kind) {
            case "email" -> when(users.findByEmail(identifier)).thenReturn(user);
            case "nickname" -> when(users.findByNickname(identifier)).thenReturn(user);
            default -> when(users.findByUserAccount(identifier)).thenReturn(user);
        }
        when(crypto.rsaDecrypt("rsa-key")).thenReturn("key");
        when(crypto.rsaDecrypt("rsa-iv")).thenReturn("iv");
        when(crypto.aesDecrypt("login", "key", "iv")).thenReturn(identifier);
        when(crypto.aesDecrypt("password", "key", "iv")).thenReturn("test-password");
        when(crypto.decryptFromDB("stored-email")).thenReturn("test@example.test");
        when(crypto.decryptFromDB("stored-phone")).thenReturn("13800000000");
        when(crypto.aesEncrypt(anyString(), eq("key"), eq("iv")))
                .thenAnswer(call -> "transport:" + call.getArgument(0));
        var request = new HashMap<String, String>();
        if (way != null) request.put("login_way", way);
        request.put("AES", "rsa-key");
        request.put("IV", "rsa-iv");
        request.put("encryptedLoginValue", "login");
        request.put("encryptedPassword", "password");
        var result = controller.login(request);
        assertEquals(10001, result.getCode());
        assertEquals("transport:test@example.test", result.getData().get("encryptedEmail"));
        assertEquals("transport:13800000000", result.getData().get("encryptedPhone"));
        if (emailLookup) verify(crypto, never()).decryptFromDB(anyString());
        else verify(crypto).decryptFromDB("stored-email");
    }
}
