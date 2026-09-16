package com.orderflow.order.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductViewController {

  @GetMapping("/home")
  public String products() {
    return "home";
  }
}
