package cat.institutmontilivi.djau;

import java.util.Calendar;
import java.util.Date;

public class Utils {

    public static Date sumaORestaDiesAData(Date data, int nDies)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(data); // Now use today date.
        c.add(Calendar.DATE, nDies);
        return c.getTime();
    }
}
