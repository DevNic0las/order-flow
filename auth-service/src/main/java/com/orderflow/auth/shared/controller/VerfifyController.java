package com.orderflow.auth.shared.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VerfifyController {

  @GetMapping("/verifycode")
  public String verifyPage(Model model) {
    model.addAttribute("email", "a@a.ggmail.com");
    return "verifycode";
  }
}
