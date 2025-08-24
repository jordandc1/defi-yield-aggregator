package app.dya.price;

public class CoinGeckoClientException extends RuntimeException {
    private final int statusCode;

    public CoinGeckoClientException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
