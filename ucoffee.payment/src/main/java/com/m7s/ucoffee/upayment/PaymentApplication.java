package com.m7s.ucoffee.upayment;

import io.micronaut.runtime.Micronaut;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("api")
public class PaymentApplication  extends Application {

    public static void main(String[] args) {
        Micronaut.run(PaymentApplication.class, args);
    }
}
