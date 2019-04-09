package cat.institutmontilivi.djau;

public class HttpErrorException extends Exception {
    private String msg;
    private String errorCode;

    public HttpErrorException(String msg, String errorCode, Exception parent) {
        super(errorCode + ": " + msg, parent);
        this.msg = msg;
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return this.errorCode;
    }
}
