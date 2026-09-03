package com.orderflow.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {
  public InventoryNotFoundException(String message) {
    super(message);
  }
}
