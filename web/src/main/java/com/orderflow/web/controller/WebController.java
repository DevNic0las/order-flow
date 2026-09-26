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
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "E-mail ou senha inválidos.");
        } else if (integration != null) {
            model.addAttribute("error", "Erro ao conectar com o serviço de autenticação.");
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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        return "redirect:/dashboard";
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

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
