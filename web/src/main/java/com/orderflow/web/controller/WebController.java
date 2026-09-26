package com.orderflow.web.controller;

import com.orderflow.web.service.GatewayClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebController {

    private static final String JWT_SESSION_ATTRIBUTE = "JWT";

    private final GatewayClient gatewayClient;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "E-mail ou senha inválidos.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        String jwt = gatewayClient.login(email, password);

        if (jwt == null || jwt.isBlank()) {
            model.addAttribute("error", "E-mail ou senha inválidos.");
            return "login";
        }

        session.setAttribute(JWT_SESSION_ATTRIBUTE, jwt);
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        model.addAttribute("orders", gatewayClient.getOrders(jwt));
        return "dashboard";
    }

    @GetMapping("/catalog")
    public String catalog(HttpSession session, Model model) {
        String jwt = (String) session.getAttribute(JWT_SESSION_ATTRIBUTE);
        if (jwt == null || jwt.isBlank()) {
            return "redirect:/login";
        }

        model.addAttribute("products", gatewayClient.getProducts(jwt));
        return "catalog";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
