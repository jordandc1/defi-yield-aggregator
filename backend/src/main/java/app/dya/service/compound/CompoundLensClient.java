package app.dya.service.compound;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal client for interacting with the Compound Lens contract.
 * Only the fields required by the application are exposed.
 */
public class CompoundLensClient {

    private static final String LENS_ADDRESS = "0xd513d22422a3062Bd342Ae374b4b9c20E0a9a074";

    private final Web3j web3j;

    public CompoundLensClient(Web3j web3j) {
        this.web3j = web3j;
    }

    /**
     * Fetch supply and borrow balances for a given cToken and account.
     *
     * @param cTokenAddress address of the cToken contract
     * @param account wallet address
     * @return token balances
     * @throws IOException on RPC or decoding failure
     */
    public CTokenBalance getBalance(String cTokenAddress, String account) throws IOException {
        Function function = new Function(
                "cTokenBalances",
                Arrays.asList(new Address(cTokenAddress), new Address(account)),
                Arrays.asList(
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(account, LENS_ADDRESS, encodedFunction);
        EthCall response = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
        if (response == null || response.hasError()) {
            throw new IOException(response != null ? response.getError().getMessage() : "null response");
        }

        List<Type> values = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        BigInteger borrow = ((Uint256) values.get(2)).getValue();
        BigInteger supply = ((Uint256) values.get(3)).getValue();
        return new CTokenBalance(supply, borrow);
    }

    /**
     * Simple DTO for balances returned by the Compound Lens.
     * Supply corresponds to {@code balanceOfUnderlying} and borrow to
     * {@code borrowBalanceCurrent}.
     */
    public record CTokenBalance(BigInteger supply, BigInteger borrow) {}
}
