import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(final String[] args) throws CommitException, GatewayException, InterruptedException, CertificateException, IOException, InvalidKeyException {
        Reader certReader = Files.newBufferedReader(certificatePath);
        X509Certificate certificate = Identities.readX509Certificate(certReader);
        Identity identity = new X509Identity("mspId", certificate);

        Reader keyReader = Files.newBufferedReader(privateKeyPath);
        PrivateKey privateKey = Identities.readPrivateKey(keyReader);
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        ManagedChannel grpcChannel = Grpc.newChannelBuilder("gateway.example.org:1337", TlsChannelCredentials.create())
                .build();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel);

        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("evote");

            byte[] putResult = contract.submitTransaction("put", "time", LocalDateTime.now().toString());
            System.out.println(new String(putResult, StandardCharsets.UTF_8));

            byte[] getResult = contract.evaluateTransaction("get", "time");
            System.out.println(new String(getResult, StandardCharsets.UTF_8));
        } finally {
            grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}