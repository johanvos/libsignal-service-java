package com.gluonhq.snl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.privacyresearch.servermodel.CredentialsMessage;
import io.privacyresearch.servermodel.CredentialsResponseMessage;
import io.privacyresearch.servermodel.UserRemoteConfigListMessage;
import io.privacyresearch.servermodel.UserRemoteConfigMessage;
import io.privacyresearch.servermodel.PreKeyResponseItemMessage;
import io.privacyresearch.servermodel.PreKeyResponseMessage;
import io.privacyresearch.servermodel.PreKeyEntityMessage;
import io.privacyresearch.servermodel.SignedPreKeyEntityMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.usernames.Username;
import org.whispersystems.signalservice.api.ArchiveCredentialPresentation;
import org.whispersystems.signalservice.api.groupsv2.CredentialResponse;
import org.whispersystems.signalservice.api.groupsv2.TemporalCredential;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity;
import org.whispersystems.signalservice.api.push.exceptions.AuthorizationFailedException;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.configuration.SignalUrl;
import org.whispersystems.signalservice.internal.push.PreKeyEntity;
import org.whispersystems.signalservice.internal.push.PreKeyResponse;
import org.whispersystems.signalservice.internal.push.PreKeyResponseItem;
import org.whispersystems.signalservice.internal.push.RemoteConfigResponse;
import org.whispersystems.signalservice.internal.push.SenderCertificate;
import org.whispersystems.util.Base64;

/**
 *
 * @author johan
 */
public class NetworkAPI {

    public static Optional<CredentialsProvider> cp;
    public static SignalUrl signalUrl = null;
    static String HOST = "chat.signal.org";
    private static boolean local = false;

    static private NetworkClient networkClient;
    private static final Logger LOG = Logger.getLogger(NetworkAPI.class.getName());
    static {
        if ("true".equals(System.getProperty("wave.staging"))) {
            HOST = "chat.staging.signal.org";
        }
        if ("true".equals(System.getProperty("wave.local"))) {
            local = true;
            HOST = "localhost:8079";
        }
    }
    private static NetworkClient getClient() {
        if (networkClient == null) {
            networkClient = NetworkClient.createNetworkClient(signalUrl, cp);
        }
        return networkClient;
    }

    /**
     * Retrieve a sender certificate, required for unauthenticated messages
     * (sealed sender)
     *
     * @param cred
     * @return a byte array containing the certificate
     * @throws IOException
     */
    public static byte[] getSenderCertificate(CredentialsProvider cred) throws IOException {
        try {
            URI uri = new URI("xhttps://"+HOST+"/v1/certificate/delivery");
            if (local) {
                uri = new URI("xhttp://"+HOST+"/v1/certificate/delivery");
            }
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cred)));
            NetworkClient client = getClient();
            Response response = client.sendRequest(uri, "GET", new byte[0], headers);
            if (response.getStatusCode() == 401) {
                throw new AuthorizationFailedException(response.getStatusCode(), "Got a 401 code from server when asking sendercertifcate");
            }
            if (client.supportsJson()) {
                ObjectMapper objectMapper = new ObjectMapper();
                SenderCertificate cert = objectMapper.readValue(response.body().string(), SenderCertificate.class);
                return cert.getCertificate();
            }
            byte[] raw = response.body().bytes();
            return raw;
        } catch (URISyntaxException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }

    public static Map<String, Object> getRemoteConfig(CredentialsProvider cred) throws IOException {
        try {
            Map<String, Object> answer = new HashMap<>();
            URI uri = new URI("xhttps://"+HOST+"/v1/config");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cred)));
            NetworkClient client = getClient();
            Response response = client.sendRequest(uri, "GET", new byte[0], headers);
            checkResponseStatus(response.getStatusCode());
            if (client.supportsJson()) {
                ObjectMapper objectMapper = new ObjectMapper();
                RemoteConfigResponse remoteConf = objectMapper.readValue(response.body().string(), RemoteConfigResponse.class);
                List<RemoteConfigResponse.Config> configs = remoteConf.getConfig();
                for (RemoteConfigResponse.Config config : configs) {
                    answer.put(config.getName(), config.getValue());
                }
                return answer;
            }
            byte[] raw = response.body().bytes();
            UserRemoteConfigListMessage urlm = UserRemoteConfigListMessage.parseFrom(raw);
            for (UserRemoteConfigMessage urcm : urlm.getUserRemoteConfigList()) {
                answer.put(urcm.getName(), urcm.hasValue() ? urcm.getValue() : urcm.getEnabled());
            }
            return answer;
        } catch (URISyntaxException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }

    public static String reserveUsername(List<Username> usernames) {
        try {
            URI uri = new URI("https://" + HOST + "/v1/accounts/username_hash/reserve");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            headers.put("content-type", List.of("application/json"));
            String body = "{\"usernameHashes\":[";
            for (int i = 0; i < usernames.size(); i++) {
                Username candidate = usernames.get(i);
                String hash = java.util.Base64.getUrlEncoder().encodeToString(candidate.getHash());
                body = body + "\"" + hash + "\"";
                if (i < usernames.size() - 1) {
                    body = body + ",";
                }
            }
            body = body + "]}";
            Response response = getClient().sendRequest(uri, "PUT", body.getBytes(), headers);
            LOG.info("response code = "+response.getStatusCode());
            return response.body().string();
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Confirm the supplied username
     * @param hash
     * @param proof
     * @param link
     * @return the response body of the server message 
     */
    public static String confirmUsername(String hash, String proof, String link) {
        try {
            URI uri = new URI("https://" + HOST + "/v1/accounts/username_hash/confirm");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            headers.put("content-type", List.of("application/json"));
            String body = "{\"usernameHash\":\"" + hash + "\",\n"
                    + "\"zkProof\":\"" + proof + "\",\n"
                    + "\"encryptedUsername\":\"" + link + "\"\n}";
            Response response = getClient().sendRequest(uri, "PUT", body.getBytes(), headers);
            LOG.info("response code = " + response.getStatusCode());
            return response.body().string();
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Return the ACI (as a String) that belongs to the user with the specified usernamehash
     * @param hash
     * @return an ACI, or null if no such user is found.
     */
    public static String getAciByUsernameHash(String hash) {
        try {
            URI uri = new URI("https://" + HOST + "/v1/accounts/username_hash/" + hash);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("content-type", List.of("application/json"));

            Response response = getClient().sendRequest(uri, "GET", new byte[0], headers);
            LOG.info("response code = " + response.getStatusCode());
            if (response.getStatusCode()!= 200) {
                LOG.info("No ACI linked to this username, statuscode "+response.getStatusCode());
                return null;
            }
            LOG.info("and body = " + response.body().string());
            String answer = response.body().string();
            int sep = answer.indexOf(":");
            String aci = answer.substring(sep+2, answer.length() -2);
            return aci;
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static PreKeyResponse getPreKey(String uuid, int deviceId) throws IOException {
        try {
            URI uri = new URI("xhttps://"+HOST+"/v2/keys/" + uuid + "/" + deviceId);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            Response response = getClient().sendRequest(uri, "GET", new byte[0], headers);
            checkResponseStatus(response.getStatusCode());
            byte[] raw = response.body().bytes();
            PreKeyResponseMessage pkrm = PreKeyResponseMessage.parseFrom(raw);
            byte[] keyBytes = pkrm.getIdentityKey().toByteArray();
            IdentityKey ik = new IdentityKey(keyBytes);
            List<PreKeyResponseItem> pkris = new ArrayList<>();
            for (PreKeyResponseItemMessage item : pkrm.getDevicesList()) {
                int devid = item.getDeviceId();
                int regid = item.getRegistrationId();
                PreKeyEntityMessage pke = item.getPreKeyEntity();
                long keyId = pke.getKeyId();
                if (keyId > Integer.MAX_VALUE) {
                    throw new RuntimeException("Major issue, can't cast a long to an int");
                }
                ECPublicKey pk = new ECPublicKey(pke.getPublicKeyBytes().toByteArray());
                PreKeyEntity preKey = new PreKeyEntity((int) keyId, pk);
                SignedPreKeyEntityMessage spke = item.getSignedPreKeyEntity();
                long skeyId = spke.getKeyId();
                if (skeyId > Integer.MAX_VALUE) {
                    throw new RuntimeException("Major issue, can't cast a long to an int");
                }
                ECPublicKey spk = new ECPublicKey(spke.getPublicKeyBytes().toByteArray());
                byte[] sig = spke.getSignature().toByteArray();
                SignedPreKeyEntity signedPreKey = new SignedPreKeyEntity((int) skeyId, spk, sig);
                PreKeyResponseItem pkItem = new PreKeyResponseItem(devid, regid, preKey, signedPreKey);
                pkris.add(pkItem);
            }
            PreKeyResponse answer = new PreKeyResponse(ik, pkris);
            return answer;
        } catch (URISyntaxException | InvalidKeyException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }

    public static CredentialResponse retrieveGroupsV2Credentials(long todaySeconds)
            throws IOException {
        try {
            long todayPlus7 = todaySeconds + TimeUnit.DAYS.toSeconds(7);
            URI uri = new URI("xhttps://"+HOST+"/v1/certificate/auth/group?redemptionStartSeconds=" + todaySeconds + "&redemptionEndSeconds=" + todayPlus7);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            Response response = getClient().sendRequest(uri, "GET", new byte[0], headers);
            checkResponseStatus(response.getStatusCode());
            byte[] raw = response.body().bytes();
            CredentialsResponseMessage crm = CredentialsResponseMessage.parseFrom(raw);
            TemporalCredential[] creds = new TemporalCredential[crm.getCredentialsList().size()];
            int idx = 0;
            for (CredentialsMessage msg : crm.getCredentialsList()) {
                TemporalCredential tc = new TemporalCredential(msg.getCredentials().toByteArray(), msg.getRedemptionTime());
                creds[idx++] = tc;
            }
            CredentialResponse answer = new CredentialResponse(creds);
            LOG.info("Retrieved groupsv2Credentials: "+answer);
            return answer;
        } catch (URISyntaxException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new IOException(ex);

        }
    }

    /**
     * For testing only!
     * The device that wants to be linked generates a URL that should be scanned
     * by the main device. In testing, we want the main device to get access to
     * this URL without scanning. This call allows that.
     * @param uuid
     * @param deviceId
     * @throws IOException
     */
    public static void sendProvisionUrl(String url) throws IOException {
        try {
            URI uri = new URI("http://"+HOST+"/v1/main/provisionurl/" + url);
            Map<String, List<String>> headers = new HashMap<>();
            Response response = getClient().sendRequest(uri, "GET", new byte[0], headers);
        } catch (URISyntaxException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // === BACKUP ===
    public static String enableBackup(String context) {
        LOG.info("Enabling backup...");
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives/backupid");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            headers.put("content-type", List.of("application/json"));
            String body = "{\"backupAuthCredentialRequest\":\"" + context + "\"\n}";
            Response response = getClient().sendRequest(uri, "PUT", body.getBytes(), headers);
            LOG.info("response code = " + response.getStatusCode());
            return response.body().string();
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getBackupAuthCredentials(long startSeconds, long endSeconds) throws NonSuccessfulResponseCodeException {
        int statusCode = 0;
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives/auth?redemptionStartSeconds="
                    +startSeconds+"&redemptionEndSeconds="+endSeconds);
            LOG.info("get backup authcredenials from/to with uri = "+uri);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", List.of(getAuthorizationHeader(cp.get())));
            headers.put("content-type", List.of("application/json"));
            Response response = getClient().sendRequest(uri, "GET", new byte[0], headers);
            LOG.info("response code = " + response.getStatusCode());
            statusCode = response.getStatusCode();
            if (statusCode < 300) {
                return response.body().string();
            }
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        } 
        throw new NonSuccessfulResponseCodeException(statusCode);
    }

    public static String getArchive(ArchiveCredentialPresentation credentialPresentation) throws NonSuccessfulResponseCodeException {
        Response response = null;
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives");

            Map<String, List<String>> headers = new HashMap<>();
            headers.put("X-Signal-ZK-Auth", List.of(Base64.encodeBytes(credentialPresentation.presentation())));
            headers.put("X-Signal-ZK-Auth-Signature", List.of(Base64.encodeBytes(credentialPresentation.signedPresentation())));
            headers.put("content-type", List.of("application/json"));
            response = getClient().sendRequest(uri, "GET", new byte[0], headers);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOG.info("response code = " + response.getStatusCode());
        if (response.getStatusCode() != 200) {
            throw new NonSuccessfulResponseCodeException(response.getStatusCode());
        }
        return response.body().string();
    }

    public static String setArchivePublicKey(ECPublicKey publicKey, ArchiveCredentialPresentation credentialPresentation) throws NonSuccessfulResponseCodeException {
        Response response = null;
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives/keys");
            String key = Base64.encodeBytes(publicKey.serialize());
            String body = "{\"backupIdPublicKey\": \"" + key + "\"}";
            LOG.info("SEND BODY: " + body);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("X-Signal-ZK-Auth", List.of(Base64.encodeBytes(credentialPresentation.presentation())));
            headers.put("X-Signal-ZK-Auth-Signature", List.of(Base64.encodeBytes(credentialPresentation.signedPresentation())));
            headers.put("content-type", List.of("application/json"));
            response = getClient().sendRequest(uri, "PUT", body.getBytes(), headers);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(NetworkAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOG.info("response code = " + response.getStatusCode() + " and body = " + response.body().string());
        if (response.getStatusCode() != 200) {
            throw new NonSuccessfulResponseCodeException(response.getStatusCode());
        }
        return response.body().string();
    }

    public static String getArchiveMessageBackupUploadForm(ArchiveCredentialPresentation credentialPresentation) throws NonSuccessfulResponseCodeException {
          Response response = null;
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives/upload/form");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("X-Signal-ZK-Auth", List.of(Base64.encodeBytes(credentialPresentation.presentation())));
            headers.put("X-Signal-ZK-Auth-Signature", List.of(Base64.encodeBytes(credentialPresentation.signedPresentation())));
            headers.put("content-type", List.of("application/json"));
            response = getClient().sendRequest(uri, "GET", new byte[0], headers);
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        if (response.getStatusCode() != 200) {
            throw new NonSuccessfulResponseCodeException(response.getStatusCode());
        }
        return response.body().string();
    }
    
    public static String getReadCredentials(ArchiveCredentialPresentation credentialPresentation) throws NonSuccessfulResponseCodeException {
                  Response response = null;
        try {
            URI uri = new URI("https://" + HOST + "/v1/archives/auth/read");
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("X-Signal-ZK-Auth", List.of(Base64.encodeBytes(credentialPresentation.presentation())));
            headers.put("X-Signal-ZK-Auth-Signature", List.of(Base64.encodeBytes(credentialPresentation.signedPresentation())));
            headers.put("content-type", List.of("application/json"));
            response = getClient().sendRequest(uri, "GET", new byte[0], headers);
        } catch (URISyntaxException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        LOG.info("response code = " + response.getStatusCode() + " and body = " + response.body().string());
        if (response.getStatusCode() != 200) {
            throw new NonSuccessfulResponseCodeException(response.getStatusCode());
        }
        return response.body().string();
    }
    // === END BACKUP ===

    private static String getAuthorizationHeader(CredentialsProvider credentialsProvider) {
        try {
            String identifier = credentialsProvider.getAci() != null ? credentialsProvider.getAci().toString() : credentialsProvider.getE164();
            if (credentialsProvider.getDeviceId() != SignalServiceAddress.DEFAULT_DEVICE_ID) {
                identifier += "." + credentialsProvider.getDeviceId();
            }
            return "Basic " + Base64.encodeBytes((identifier + ":" + credentialsProvider.getPassword()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private static void checkResponseStatus(int responseCode) throws AuthorizationFailedException {
        if (responseCode == 401) throw new AuthorizationFailedException(responseCode, "Authorization failed");
    }
}
