package com.example.pps.exception;

public class InvalidMerchantKeyException extends RuntimeException {

    public InvalidMerchantKeyException(String message) {
        super(message);
    }
}