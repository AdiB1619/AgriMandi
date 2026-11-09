package com.agrimandi.api.controller;

import com.agrimandi.api.dto.auth.LoginRequestDTO;
import com.agrimandi.api.entity.Market;
import com.agrimandi.api.entity.User;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.agrimandi.api.security.CustomUserDetailsService;
import com.agrimandi.api.security.JwtAuthTokenFilter;
import com.agrimandi.api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrimandi.api.security.AuthEntryPointJwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    void login_withValidCredentials_shouldReturnJwt() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("agent.demo");
        user.setRole("AGENT");
        Market market = new Market();
        market.setId(UUID.randomUUID());
        user.setMarket(market);
        
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"agent.demo\", \"password\": \"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.user.username").value("agent.demo"))
                .andExpect(jsonPath("$.user.role").value("AGENT"));
    }

    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"agent.demo\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
