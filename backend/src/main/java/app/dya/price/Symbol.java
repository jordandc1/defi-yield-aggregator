package app.dya.price;

public enum Symbol {
    ETH("ethereum"),
    DAI("dai"),
    USDC("usd-coin");

    public final String coingeckoId;
    Symbol(String id){ this.coingeckoId = id; }

    public static Symbol from(String s){
        return Symbol.valueOf(s.trim().toUpperCase());
    }

    public static boolean supported(String s){
        try { from(s); return true; } catch (Exception e){ return false; }
    }
}
