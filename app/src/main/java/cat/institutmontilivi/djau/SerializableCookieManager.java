package cat.institutmontilivi.djau;

import java.io.IOException;
import java.io.Serializable;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.List;

public class SerializableCookieManager extends CookieManager implements Serializable {
    /**
     * Hack per poder serialitzar les cookies i passar-les d'activitat a Activitat.
     * L'altre opció seria no modificar el password.
     * Aquí hi ha una possible implementació millor: https://gist.github.com/franmontiel/ed12a2295566b7076161
     * Però per el que necessito així va bé.
     * @param out
     * @throws IOException
     */

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        //Serialitza els cookies per defecte.
        List<HttpCookie> llistaCookies = this.getCookieStore().getCookies();

        int nCookies = llistaCookies.size();
        out.writeInt(nCookies);
        for (HttpCookie cookie: llistaCookies)
        {
            out.writeObject(cookie.getName());
            out.writeObject(cookie.getValue());
            out.writeObject(cookie.getComment());
            out.writeObject(cookie.getCommentURL());
            out.writeObject(cookie.getDomain());
            out.writeLong(cookie.getMaxAge());
            out.writeObject(cookie.getPath());
            out.writeObject(cookie.getPortlist());
            out.writeInt(cookie.getVersion());
            out.writeBoolean(cookie.getSecure());
            out.writeBoolean(cookie.getDiscard());
        }
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        int nCookies = in.readInt();
        for (int i=0;i<nCookies;i++)
        {
            String name = (String) in.readObject();
            String value = (String) in.readObject();

            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setComment((String) in.readObject());
            cookie.setCommentURL((String) in.readObject());
            cookie.setDomain((String) in.readObject());
            cookie.setMaxAge(in.readLong());
            cookie.setPath((String) in.readObject());
            cookie.setPortlist((String) in.readObject());
            cookie.setVersion(in.readInt());
            cookie.setSecure(in.readBoolean());
            cookie.setDiscard(in.readBoolean());
            
            this.getCookieStore().add(null, cookie);
        }
    }
}
