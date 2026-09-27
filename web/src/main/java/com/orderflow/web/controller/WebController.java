package com.orderflow.web.controller;

import com.orderflow.web.service.GatewayClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private static final String JWT_SESSION_ATTRIBUTE = "JWT";

    private final GatewayClient gatewayClient;
    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "integration", required = false) String integration,
                            @RequestParam(value = "verified", required = false) String verified,
                            Model model) {

        if (error != null) {
            model.addAttribute("error", "E-mail ou senha inválidos.");
        } else if (integration != null) {
            model.addAttribute("error", "Erro ao conectar com o serviço de autenticação.");
        } else if (verified != null) {
            model.addAttribute("verified", true);
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          HttpSession session,
                          Model model) {
        String jwt;
        try {
            jwt = gatewayClient.login(email, password);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Integration failure during login for email={}", email, ex);
            return "redirect:/login?integration";
        }

        if (jwt == null || jwt.isBlank()) {
            model.addAttribute("error", "E-mail ou senha inválidos.");
            return "login";
        }

        session.setAttribute(JWT_SESSION_ATTRIBUTE, jwt);
        authenticateSession(email, jwt, request, response);




        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model) {
        String verificationToken;
        try {
            verificationToken = gatewayClient.register(name, email, password);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Integration failure during register for email={}", email, ex);
            model.addAttribute("error", "Erro ao conectar com o serviço de autenticação.");
            return "register";
        }

        if (verificationToken == null || verificationToken.isBlank()) {
            model.addAttribute("error", "Não foi possível criar a conta. Verifique os dados e tente novamente.");
            return "register";
        }

        return "redirect:/verifycode?token=" + verificationToken;
    }

    @GetMapping("/verifycode")
    public String verifyCodePage(@RequestParam String token,
                                 @RequestParam(value = "email", required = false) String email,
                                 @RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "resent", required = false) String resent,
                                 @RequestParam(value = "cooldownSeconds", required = false) Long cooldownSeconds,
                                 Model model) {
        model.addAttribute("token", token);
        model.addAttribute("email", email);
        if (error != null) {
            model.addAttribute("error", "Código inválido ou expirado.");
        }
        model.addAttribute("resent", resent != null);
        model.addAttribute("cooldownSeconds", cooldownSeconds);
        return "verify-code";
    }

    @PostMapping("/verifycode")
    public String doVerifyCode(@RequestParam String token,
                               @RequestParam String code,
                               HttpSession session,
                               @RequestParam(value = "email", required = false) String email,
                               Model model) {
        String jwt;
        try {
            jwt = gatewayClient.verifyCode(token, code);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Integration failure during verifycode", ex);
            return "redirect:/verifycode?token=" + token + "&error";
        }

        if (jwt == null || jwt.isBlank()) {
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            model.addAttribute("error", "Código inválido ou expirado.");
            return "verify-code";
        }

        session.setAttribute(JWT_SESSION_ATTRIBUTE, jwt);
        return "redirect:/login?verified";
    }

    @PostMapping("/resend-code")
    public String doResendCode(@RequestParam String token,
                               @RequestParam(value = "email", required = false) String email,
                               Model model) {
        GatewayClient.ResendOutcome outcome;
        try {
            outcome = gatewayClient.resendCode(token);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Integration failure during resend-code", ex);
            return "redirect:/verifycode?token=" + token + "&error";
        }

        if (outcome.resent()) {
            return "redirect:/verifycode?token=" + token + "&resent";
        }

        if (outcome.cooldownSeconds() != null && outcome.cooldownSeconds() > 0) {
            return "redirect:/verifycode?token=" + token + "&resent&cooldownSeconds=" + outcome.cooldownSeconds();
        }

        return "redirect:/verifycode?token=" + token + "&error";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("orders", gatewayClient.getOrders(jwt));
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Failed to load orders for dashboard", ex);
            model.addAttribute("orders", java.util.List.of());
            model.addAttribute("loadError", true);
        }
        return "dashboard";
    }

    @GetMapping("/catalog")
    public String catalog(HttpSession session, Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("products", gatewayClient.getProducts(jwt));
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Failed to load products for catalog", ex);
            model.addAttribute("products", java.util.List.of());
            model.addAttribute("loadError", true);
        }
        return "catalog";
    }

    @GetMapping("/buy")
    public String buyPage(HttpSession session, Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("products", gatewayClient.getProducts(jwt));
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Failed to load products for buy page", ex);
            model.addAttribute("products", java.util.List.of());
            model.addAttribute("error", "Erro ao conectar com o serviço de estoque.");
        }
        return "buy";
    }

    @PostMapping("/buy")
    public String doBuy(@RequestParam Long productId,
                        @RequestParam Integer quantity,
                        HttpSession session,
                        Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        GatewayClient.OrderCreated created;
        try {
            created = gatewayClient.createOrder(jwt, productId, quantity);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Integration failure during buy for productId={}", productId, ex);
            model.addAttribute("error", "Erro ao conectar com o serviço de pedidos.");
            model.addAttribute("products", reloadProducts(jwt));
            return "buy";
        }

        if (created == null) {
            model.addAttribute("error", "Não foi possível registrar o pedido. Verifique o estoque e tente novamente.");
            model.addAttribute("products", reloadProducts(jwt));
            return "buy";
        }

        model.addAttribute("confirmation", created);
        model.addAttribute("products", reloadProducts(jwt));
        return "buy";
    }

    private List<com.orderflow.web.dto.ProductViewDto> reloadProducts(String jwt) {
        try {
            return gatewayClient.getProducts(jwt);
        } catch (GatewayClient.GatewayIntegrationException ex) {
            log.error("Failed to reload products after buy", ex);
            return java.util.List.of();
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private void authenticateSession(String email, String jwt,
                                     HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, AuthorityUtils.createAuthorityList(resolveRoles(jwt)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
    }

    private String[] resolveRoles(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[1]);
            com.fasterxml.jackson.databind.JsonNode claims =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            com.fasterxml.jackson.databind.JsonNode roles = claims.get("roles");
            if (roles != null && roles.isArray() && !roles.isEmpty()) {
                String[] result = new String[roles.size()];
                for (int i = 0; i < roles.size(); i++) {
                    result[i] = roles.get(i).asText();
                }
                return result;
            }
        } catch (Exception ex) {
            log.warn("Failed to extract roles from JWT; falling back to ROLE_CUSTOMER", ex);
        }
        return new String[]{"ROLE_CUSTOMER"};
    }
}
