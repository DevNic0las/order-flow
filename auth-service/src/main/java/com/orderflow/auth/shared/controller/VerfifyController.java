package com.orderflow.auth.shared.controller;


import com.orderflow.auth.shared.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class VerfifyController {


  private final AuthService authService;

  @GetMapping("/verifycode/email")
  public String verifyPage(
          @RequestParam String token,
          Model model
  ) {
    model.addAttribute("verificationToken", token);

    return "verifycode";
  }
  @GetMapping("/register/form")
  public String registerPage() {
    return "register";
  }
}
