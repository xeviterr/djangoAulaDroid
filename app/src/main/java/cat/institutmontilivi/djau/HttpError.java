package cat.institutmontilivi.djau;

public class HttpError {
    private String msg;
    private String errorCode;

    public HttpError(String msg, String errorCode) {
        this.msg = msg;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return this.errorCode;
    }
    public String getMsg() {
        return this.msg;
    }
    public String toString() { return this.errorCode + ":" + this.msg; }
}