package cn.zbx1425.nquestmod;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.Mac;
import java.nio.ByteBuffer;
import java.util.UUID;

public class CommandSigner {

    public UUID signingKey;

    public CommandSigner() {
        signingKey = UUID.randomUUID();
    }

    public String signTimestamp() {
        ByteBuffer bbKey = ByteBuffer.allocate(16);
        bbKey.putLong(signingKey.getMostSignificantBits());
        bbKey.putLong(signingKey.getLeastSignificantBits());
        Mac mac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_MD5, bbKey.array());

        long timestamp = System.currentTimeMillis();
        ByteBuffer bbPayload = ByteBuffer.allocate(8);
        bbPayload.putLong(timestamp);

        byte[] sign = mac.doFinal(bbPayload.array());
        ByteBuffer bbResult = ByteBuffer.allocate(24);
        bbResult.putLong(timestamp);
        bbResult.put(sign);

        return Base64.encodeBase64URLSafeString(bbResult.array());
    }

    public boolean verifySignedTimestamp(String signature, long expiry) {
        byte[] decoded;
        try {
            decoded = Base64.decodeBase64(signature);
        } catch (Exception e) {
            return false;
        }
        if (decoded.length != 24) return false;

        ByteBuffer bbKey = ByteBuffer.allocate(16);
        bbKey.putLong(signingKey.getMostSignificantBits());
        bbKey.putLong(signingKey.getLeastSignificantBits());
        Mac mac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_MD5, bbKey.array());

        ByteBuffer bbSrc = ByteBuffer.wrap(decoded, 0, 8);
        long timestamp = bbSrc.getLong();
        if (System.currentTimeMillis() - timestamp > expiry) return false;

        ByteBuffer bbPayload = ByteBuffer.allocate(8);
        bbPayload.putLong(timestamp);
        byte[] expectedSign = mac.doFinal(bbPayload.array());
        for (int i = 0; i < 16; i++) {
            if (decoded[8 + i] != expectedSign[i]) return false;
        }
        return true;
    }
}
