package com.elitemastereric.helloworld;

public class Main {
    public Main() {}

    public void execute() {
        System.out.println(getGreeting());
    }

    /*
     * Returns a greeting.
     * This is the function which will act as the target of our Mixin.
     */
    public String getGreeting() {
        return "Hello World!";
    }
}
