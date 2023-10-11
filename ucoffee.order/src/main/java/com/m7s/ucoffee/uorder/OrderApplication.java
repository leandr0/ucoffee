package com.m7s.ucoffee.uorder;

import io.micronaut.runtime.Micronaut;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import static com.m7s.ucoffee.uorder.OrderApplicationConfig.*;

@ApplicationPath(CONTROLLER_PATH)
public class OrderApplication extends Application {

    public static void main(String[] args) {
        Micronaut.run(OrderApplication.class, args);
    }
}
