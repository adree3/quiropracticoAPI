package com.example.quiropracticoapi.exception;

import lombok.Getter;

@Getter
public class HorarioOverlapException extends RuntimeException {
  private final String tipoConflicto;

  public HorarioOverlapException(String message, String tipoConflicto) {
    super(message);
    this.tipoConflicto = tipoConflicto;
  }
}