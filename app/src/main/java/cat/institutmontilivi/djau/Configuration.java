package cat.institutmontilivi.djau;

public class Configuration
{
    private static Configuration instance;

    public String APILevel="2";
    public boolean APIDebug=true;

    public static Configuration getInstance()
    {
        if (instance == null)
            instance = new Configuration();

        // Return the instance
        return instance;
    }
}