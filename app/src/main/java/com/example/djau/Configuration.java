package com.example.djau;

public class Configuration
{
    private static Configuration instance;

    public String APILevel="1";

    public static Configuration getInstance()
    {
        if (instance == null)
            instance = new Configuration();

        // Return the instance
        return instance;
    }
}