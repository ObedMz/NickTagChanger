package obed.me.nametag.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MojangAPI {
    private static URL API_STATUS_URL = null;

    private static URL GET_UUID_URL = null;

    private static final JSONParser PARSER = new JSONParser();

    private static Plugin plugin;

    static {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getClass().getProtectionDomain().getCodeSource().equals(MojangAPI.class.getProtectionDomain().getCodeSource()))
                MojangAPI.plugin = plugin;
        }
        try {
            API_STATUS_URL = new URL("https://status.mojang.com/check");
            GET_UUID_URL = new URL("https://api.mojang.com/profiles/minecraft");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unused")
    public static void setPlugin(Plugin plugin) {
        MojangAPI.plugin = plugin;
    }
    @SuppressWarnings("unused")
    public static void getAPIStatusWithCallBack(final ResultCallBack<Map<String, APIStatus>> callBack) {
        getAPIStatusAsyncWithCallBack((successful, result, exception) -> (new BukkitRunnable() {
            public void run() {
                callBack.callBack(successful, result, exception);
            }
        }).runTask(plugin));
    }

    public static void getAPIStatusAsyncWithCallBack(ResultCallBack<Map<String, APIStatus>> callBack) {
        if (plugin == null)
            return;
        makeAsyncGetRequest(API_STATUS_URL, (successful, response, exception, responseCode) -> {
            if (callBack == null)
                return;
            if (successful && responseCode == 200) {
                try {
                    Map<String, APIStatus> map = Maps.newHashMap();
                    JSONArray jsonArray = (JSONArray)PARSER.parse(response);
                    for (Object jsonObject : jsonArray) {
                        JSONObject json = (JSONObject) jsonObject;
                        for (Map.Entry<String, String> entry : (Iterable<Map.Entry<String, String>>)json.entrySet())
                            map.put(entry.getKey(), APIStatus.fromString(entry.getValue()));
                    }
                    callBack.callBack(true, map, null);
                } catch (Exception e) {
                    callBack.callBack(false, null, e);
                }
            } else if (exception != null) {
                callBack.callBack(false, null, exception);
            } else {
                callBack.callBack(false, null, new IOException("Failed to obtain Mojang data! Response code: " + responseCode));
            }
        });
    }

    public enum APIStatus {
        RED, YELLOW, GREEN;

        public static APIStatus fromString(String string) {
            switch (string) {
                case "red":
                    return RED;
                case "yellow":
                    return YELLOW;
                case "green":
                    return GREEN;
            }
            throw new IllegalArgumentException("Unknown status: " + string);
        }
    }

    @SuppressWarnings("unused")
    public static void getUUIDAtTimeWithCallBack(String username, long timeStamp, final ResultCallBack<UUIDAtTime> callBack) {
        getUUIDAtTimeAsyncWithCallBack(username, timeStamp, (successful, result, exception) -> (new BukkitRunnable() {
            public void run() {
                callBack.callBack(successful, result, exception);
            }
        }).runTask(plugin));
    }

    public static void getUUIDAtTimeAsyncWithCallBack(String username, long timeStamp, ResultCallBack<UUIDAtTime> callBack) {
        if (plugin == null)
            return;
        Validate.notNull(username);
        Validate.isTrue(!username.isEmpty(), "username cannot be empty");
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username + ((timeStamp != -1L) ? ("?at=" + timeStamp) : ""));
            makeAsyncGetRequest(url, (successful, response, exception, responseCode) -> {
                if (callBack == null)
                    return;
                if (successful && (responseCode == 200 || responseCode == 204)) {
                    try {
                        UUIDAtTime[] uuidAtTime = new UUIDAtTime[1];
                        if (responseCode == 200) {
                            JSONObject object = (JSONObject)PARSER.parse(response);
                            String uuidString = (String)object.get("id");
                            uuidAtTime[0] = new UUIDAtTime((String)object.get("name"), getUUIDFromString(uuidString));
                        }
                        callBack.callBack(true, uuidAtTime[0], null);
                    } catch (Exception e) {
                        callBack.callBack(false, null, e);
                    }
                } else if (exception != null) {
                    callBack.callBack(false, null, exception);
                } else {
                    callBack.callBack(false, null, new IOException("Failed to obtain Mojang data! Response code: " + responseCode));
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unused")
    public static class UUIDAtTime {
        private final String name;

        private final UUID uuid;

        public UUIDAtTime(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return this.name;
        }

        public UUID getUUID() {
            return this.uuid;
        }

        public String toString() {
            return "UUIDAtTime{name=" + this.name + ",uuid=" + this.uuid + "}";
        }

        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof UUIDAtTime))
                return false;
            UUIDAtTime uuidAtTime = (UUIDAtTime)obj;
            return (this.name.equals(uuidAtTime.name) && this.uuid.equals(uuidAtTime.uuid));
        }

        public int hashCode() {
            return Objects.hash(this.name, this.uuid);
        }
    }
    @SuppressWarnings("unused")
    public static void getNameHistoryWithCallBack(UUID uuid, final ResultCallBack<Map<String, Long>> callBack) {
        getNameHistoryAsyncWithCallBack(uuid, (successful, result, exception) -> (new BukkitRunnable() {
            public void run() {
                callBack.callBack(successful, result, exception);
            }
        }).runTask(plugin));
    }

    public static void getNameHistoryAsyncWithCallBack(UUID uuid, ResultCallBack<Map<String, Long>> callBack) {
        if (plugin == null)
            return;
        Validate.notNull(uuid, "uuid cannot be null!");
        try {
            URL url = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names");
            makeAsyncGetRequest(url, (successful, response, exception, responseCode) -> {
                if (callBack == null)
                    return;
                if (successful && (responseCode == 200 || responseCode == 204)) {
                    try {
                        Map<String, Long> map = Maps.newHashMap();
                        if (responseCode == 200) {
                            JSONArray jsonArray = (JSONArray)PARSER.parse(response);
                            for (Object jsonObject : jsonArray) {
                                JSONObject json = (JSONObject) jsonObject;
                                String name = (String)json.get("name");
                                if (json.containsKey("changedToAt")) {
                                    map.put(name, (Long)json.get("changedToAt"));
                                    continue;
                                }
                                map.put(name, -1L);
                            }
                        }
                        callBack.callBack(true, map, null);
                    } catch (Exception e) {
                        callBack.callBack(false, null, e);
                    }
                } else if (exception != null) {
                    callBack.callBack(false, null, exception);
                } else {
                    callBack.callBack(false, null, new IOException("Failed to obtain Mojang data! Response code: " + responseCode));
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unused")
    public static void getUUIDWithCallBack(ResultCallBack<Map<String, Profile>> callBack, String... usernames) {
        getUUIDWithCallBack(Arrays.asList(usernames), callBack);
    }

    public static void getUUIDWithCallBack(List<String> usernames, final ResultCallBack<Map<String, Profile>> callBack) {
        getUUIDAsyncWithCallBack(usernames, (successful, result, exception) -> (new BukkitRunnable() {
            public void run() {
                callBack.callBack(successful, result, exception);
            }
        }).runTask(plugin));
    }
    @SuppressWarnings("unused")
    public static void getUUIDAsyncWithCallBack(ResultCallBack<Map<String, Profile>> callBack, String... usernames) {
        getUUIDAsyncWithCallBack(Arrays.asList(usernames), callBack);
    }
    @SuppressWarnings("unused")
    public static Result<Map<String, Profile>> getUUID(List<String> usernames) {
        if (plugin == null)
            return new Result<>(null, false, new RuntimeException("No plugin instance found!"));
        Validate.notNull(usernames, "usernames cannot be null");
        Validate.isTrue((usernames.size() <= 100), "cannot request more than 100 usernames at once");
        JSONArray usernameJson = new JSONArray();
        usernameJson.addAll(usernames.stream().filter(s -> !Strings.isNullOrEmpty(s)).collect(Collectors.toList()));
        RequestResult result = makeSyncPostRequest(GET_UUID_URL, usernameJson.toJSONString());
        if (result == null)
            return new Result<>(null, false, new RuntimeException("No plugin instance found!"));
        try {
            if (result.successful && result.responseCode == 200) {
                Map<String, Profile> map = Maps.newHashMap();
                JSONArray jsonArray = (JSONArray)PARSER.parse(result.response);
                for (Object jsonObject : jsonArray) {
                    JSONObject json = (JSONObject) jsonObject;
                    String uuidString = (String)json.get("id");
                    String name = (String)json.get("name");
                    boolean legacy = false;
                    if (json.containsKey("legacy"))
                        legacy = (Boolean) json.get("legacy");
                    boolean unpaid = false;
                    if (json.containsKey("demo"))
                        unpaid = (Boolean) json.get("demo");
                    map.put(name, new Profile(getUUIDFromString(uuidString), name, legacy, unpaid));
                }
                return new Result<>(map, true, null);
            }
            if (result.exception != null)
                return new Result<>(null, false, result.exception);
            return new Result<>(null, false, new IOException("Failed to obtain Mojang data! Response code: " + result.responseCode));
        } catch (Exception e) {
            return new Result<>(null, false, e);
        }
    }

    public static void getUUIDAsyncWithCallBack(List<String> usernames, ResultCallBack<Map<String, Profile>> callBack) {
        if (plugin == null)
            return;
        Validate.notNull(usernames, "usernames cannot be null");
        Validate.isTrue((usernames.size() <= 100), "cannot request more than 100 usernames at once");
        JSONArray usernameJson = new JSONArray();
        usernameJson.addAll(usernames.stream().filter(s -> !Strings.isNullOrEmpty(s)).collect(Collectors.toList()));
        makeAsyncPostRequest(GET_UUID_URL, usernameJson.toJSONString(), (successful, response, exception, responseCode) -> {
            if (callBack == null)
                return;
            try {
                if (successful && responseCode == 200) {
                    Map<String, Profile> map = Maps.newHashMap();
                    JSONArray jsonArray = (JSONArray)PARSER.parse(response);
                    for (Object jsonObject : jsonArray) {
                        JSONObject json = (JSONObject) jsonObject;
                        String uuidString = (String)json.get("id");
                        String name = (String)json.get("name");
                        boolean legacy = false;
                        if (json.containsKey("legacy"))
                            legacy = (Boolean) json.get("legacy");
                        boolean unpaid = false;
                        if (json.containsKey("demo"))
                            unpaid = (Boolean) json.get("demo");
                        map.put(name, new Profile(getUUIDFromString(uuidString), name, legacy, unpaid));
                    }
                    callBack.callBack(true, map, null);
                } else if (exception != null) {
                    callBack.callBack(false, null, exception);
                } else {
                    callBack.callBack(false, null, new IOException("Failed to obtain Mojang data! Response code: " + responseCode));
                }
            } catch (Exception e) {
                callBack.callBack(false, null, e);
            }
        });
    }
    @SuppressWarnings("unused")
    public static class Profile {
        private final UUID uuid;

        private final String name;

        private final boolean legacy;

        private final boolean unpaid;

        Profile(UUID uuid, String name, boolean legacy, boolean unpaid) {
            this.uuid = uuid;
            this.name = name;
            this.legacy = legacy;
            this.unpaid = unpaid;
        }

        public UUID getUUID() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public boolean isLegacy() {
            return this.legacy;
        }

        public boolean isUnpaid() {
            return this.unpaid;
        }

        public String toString() {
            return "Profile{uuid=" + this.uuid + ", name=" + this.name + ", legacy=" + this.legacy + ", unpaid=" + this.unpaid + "}";
        }

        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof Profile))
                return false;
            Profile otherProfile = (Profile)obj;
            return (this.uuid.equals(otherProfile.uuid) && this.name.equals(otherProfile.name) && this.legacy == otherProfile.legacy && this.unpaid == otherProfile.unpaid);
        }

        public int hashCode() {
            return Objects.hash(this.uuid, this.name, this.legacy, this.unpaid);
        }
    }
    @SuppressWarnings("unused")
    public static Result<SkinData> getSkinData(UUID uuid) {
        URL url;
        if (plugin == null)
            return new Result<>(null, false, new RuntimeException("No plugin instance found!"));
        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false");
        } catch (MalformedURLException e) {
            return new Result<>(null, false, e);
        }
        RequestResult result = makeSyncGetRequest(url);
        if (result == null)
            return new Result<>(null, false, new RuntimeException("No plugin instance found!"));
        try {
            if (result.successful && (result.responseCode == 200 || result.responseCode == 204)) {
                if (result.responseCode == 204)
                    return new Result<>(null, true, null);
                JSONObject object = (JSONObject)PARSER.parse(result.response);
                JSONArray propertiesArray = (JSONArray)object.get("properties");
                String base64 = null;
                String signedBase64 = null;
                for (Object property : propertiesArray) {
                    JSONObject proper = (JSONObject) property;
                    String name = (String)proper.get("name");
                    if (name.equals("textures")) {
                        base64 = (String)proper.get("value");
                        signedBase64 = (String)proper.get("signature");
                    }
                }
                if (base64 == null)
                    return new Result<>(null, true, null);
                String decodedBase64 = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                JSONObject base64json = (JSONObject)PARSER.parse(decodedBase64);
                long timeStamp = (Long) base64json.get("timestamp");
                String profileName = (String)base64json.get("profileName");
                UUID profileId = getUUIDFromString((String)base64json.get("profileId"));
                JSONObject textures = (JSONObject)base64json.get("textures");
                String skinURL = null;
                String capeURL = null;
                if (textures.containsKey("SKIN")) {
                    JSONObject skinObject = (JSONObject)textures.get("SKIN");
                    skinURL = (String)skinObject.get("url");
                }
                if (textures.containsKey("CAPE")) {
                    JSONObject capeObject = (JSONObject)textures.get("CAPE");
                    capeURL = (String)capeObject.get("url");
                }
                return new Result<>(new SkinData(profileId, profileName, skinURL, capeURL, timeStamp, base64, signedBase64), true, null);
            }
            if (result.exception != null)
                return new Result<>(null, false, result.exception);
            return new Result<>(null, false, new IOException("Failed to obtain Mojang data! Response code: " + result.responseCode));
        } catch (Exception e) {
            return new Result<>(null, false, e);
        }
    }
    @SuppressWarnings("unused")
    public static void getSkinData(UUID uuid, final ResultCallBack<SkinData> callBack) {
        getSkinDataAsync(uuid, (successful, result, exception) -> (new BukkitRunnable() {
            public void run() {
                callBack.callBack(successful, result, exception);
            }
        }).runTask(plugin));
    }

    public static void getSkinDataAsync(UUID uuid, ResultCallBack<SkinData> callBack) {
        URL url;
        if (plugin == null)
            return;
        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        makeAsyncGetRequest(url, (successful, response, exception, responseCode) -> {
            try {
                if (successful && (responseCode == 200 || responseCode == 204)) {
                    if (responseCode == 204) {
                        callBack.callBack(true, null, null);
                        return;
                    }
                    JSONObject object = (JSONObject)PARSER.parse(response);
                    JSONArray propertiesArray = (JSONArray)object.get("properties");
                    String base64 = null;
                    String signedBase64 = null;
                    for (Object property : propertiesArray) {
                        JSONObject proper = (JSONObject) property;
                        String name = (String)proper.get("name");
                        if (name.equals("textures")) {
                            base64 = (String)proper.get("value");
                            signedBase64 = (String)proper.get("signature");
                        }
                    }
                    if (base64 == null) {
                        callBack.callBack(true, null, null);
                        return;
                    }
                    String decodedBase64 = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                    JSONObject base64json = (JSONObject)PARSER.parse(decodedBase64);
                    long timeStamp = (Long) base64json.get("timestamp");
                    String profileName = (String)base64json.get("profileName");
                    UUID profileId = getUUIDFromString((String)base64json.get("profileId"));
                    JSONObject textures = (JSONObject)base64json.get("textures");
                    String skinURL = null;
                    String capeURL = null;
                    if (textures.containsKey("SKIN")) {
                        JSONObject skinObject = (JSONObject)textures.get("SKIN");
                        skinURL = (String)skinObject.get("url");
                    }
                    if (textures.containsKey("CAPE")) {
                        JSONObject capeObject = (JSONObject)textures.get("CAPE");
                        capeURL = (String)capeObject.get("url");
                    }
                    callBack.callBack(true, new SkinData(profileId, profileName, skinURL, capeURL, timeStamp, base64, signedBase64), null);
                } else if (exception != null) {
                    callBack.callBack(false, null, exception);
                } else {
                    callBack.callBack(false, null, new IOException("Failed to obtain Mojang data! Response code: " + responseCode));
                }
            } catch (Exception e) {
                callBack.callBack(false, null, e);
            }
        });
    }
    @SuppressWarnings("unused")
    public static class SkinData {
        private final UUID uuid;

        private final String name;

        private final String skinURL;

        private final String capeURL;

        private final long timeStamp;

        private final String base64;

        private final String signedBase64;

        public SkinData(UUID uuid, String name, String skinURL, String capeURL, long timeStamp, String base64, String signedBase64) {
            this.uuid = uuid;
            this.name = name;
            this.skinURL = skinURL;
            this.capeURL = capeURL;
            this.timeStamp = timeStamp;
            this.base64 = base64;
            this.signedBase64 = signedBase64;
        }

        public UUID getUUID() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public boolean hasSkinURL() {
            return (this.skinURL != null);
        }

        public String getSkinURL() {
            return this.skinURL;
        }

        public boolean hasCapeURL() {
            return (this.capeURL != null);
        }

        public String getCapeURL() {
            return this.capeURL;
        }

        public long getTimeStamp() {
            return this.timeStamp;
        }

        public String getBase64() {
            return this.base64;
        }

        public boolean hasSignedBase64() {
            return (this.signedBase64 != null);
        }

        public String getSignedBase64() {
            return this.signedBase64;
        }

        public String toString() {
            return "SkinData{uuid=" + this.uuid + ",name=" + this.name + ",skinURL=" + this.skinURL + ",capeURL=" + this.capeURL + ",timeStamp=" + this.timeStamp + ",base64=" + this.base64 + ",signedBase64=" + this.signedBase64 + "}";
        }

        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof SkinData))
                return false;
            SkinData skinData = (SkinData)obj;
            return (this.uuid.equals(skinData.uuid) && this.name.equals(skinData.name) && (Objects.equals(this.skinURL, skinData.skinURL)) && ((this.capeURL == null) ? (skinData.capeURL == null) : this.capeURL
                    .equals(skinData.skinURL)) && this.timeStamp == skinData.timeStamp && this.base64
                    .equals(skinData.base64) && (Objects.equals(this.signedBase64, skinData.signedBase64)));
        }

        public int hashCode() {
            return Objects.hash(this.uuid, this.name, this.skinURL, this.capeURL, this.timeStamp, this.base64, this.signedBase64);
        }
    }

    private static RequestResult makeSyncGetRequest(URL url) {
        if (plugin == null)
            return null;
        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.connect();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    response.append(line);
                    line = reader.readLine();
                }
                RequestResult result = new RequestResult();
                result.successful = true;
                result.responseCode = connection.getResponseCode();
                result.response = response.toString();
                return result;
            }
        } catch (IOException e) {
            RequestResult result = new RequestResult();
            result.exception = e;
            result.successful = false;
            return result;
        }
    }

    private static void makeAsyncGetRequest(final URL url, final RequestCallBack asyncCallBack) {
        if (plugin == null)
            return;
        (new BukkitRunnable() {
            public void run() {
                StringBuilder response = new StringBuilder();
                try {
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.connect();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line = reader.readLine();
                        while (line != null) {
                            response.append(line);
                            line = reader.readLine();
                        }
                        asyncCallBack.callBack(true, response.toString(), null, connection.getResponseCode());
                    }
                } catch (Exception e) {
                    asyncCallBack.callBack(false, response.toString(), e, -1);
                }
            }
        }).runTaskAsynchronously(plugin);
    }

    private static RequestResult makeSyncPostRequest(URL url, String payload) {
        if (plugin == null)
            return null;
        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            try (PrintWriter writer = new PrintWriter(connection.getOutputStream())) {
                writer.write(payload);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    response.append(line);
                    line = reader.readLine();
                }
                RequestResult result = new RequestResult();
                result.successful = true;
                result.responseCode = connection.getResponseCode();
                result.response = response.toString();
                return result;
            }
        } catch (IOException e) {
            RequestResult result = new RequestResult();
            result.successful = false;
            result.exception = e;
            return result;
        }
    }

    private static void makeAsyncPostRequest(final URL url, final String payload, final RequestCallBack asyncCallBack) {
        if (plugin == null)
            return;
        (new BukkitRunnable() {
            public void run() {
                StringBuilder response = new StringBuilder();
                try {
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.connect();
                    try (PrintWriter writer = new PrintWriter(connection.getOutputStream())) {
                        writer.write(payload);
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line = reader.readLine();
                        while (line != null) {
                            response.append(line);
                            line = reader.readLine();
                        }
                        asyncCallBack.callBack(true, response.toString(), null, connection.getResponseCode());
                    }
                } catch (Exception e) {
                    asyncCallBack.callBack(false, response.toString(), e, -1);
                }
            }
        }).runTaskAsynchronously(plugin);
    }

    public static UUID getUUIDFromString(String string) {
        String uuidString = string.substring(0, 8) + "-" + string.substring(8, 12) + "-" + string.substring(12, 16) + "-" + string.substring(16, 20) + "-" + string.substring(20);
        return UUID.fromString(uuidString);
    }

    private static class RequestResult {
        boolean successful;

        String response;

        Exception exception;

        int responseCode;

        private RequestResult() {}
    }

    @SuppressWarnings("unused")
    public static class Result<T> {
        private final T value;

        private final boolean successful;

        private final Exception exception;

        public Result(T value, boolean successful, Exception exception) {
            this.value = value;
            this.successful = successful;
            this.exception = exception;
        }

        public T getValue() {
            return this.value;
        }

        public boolean wasSuccessful() {
            return this.successful;
        }

        public Exception getException() {
            return this.exception;
        }
    }
    @FunctionalInterface
    public interface ResultCallBack<T> {
        void callBack(boolean param1Boolean, T param1T, Exception param1Exception);
    }
    @SuppressWarnings("unused")
    @FunctionalInterface
    private interface RequestCallBack {
        void callBack(boolean param1Boolean, String param1String, Exception param1Exception, int param1Int);
    }
}
