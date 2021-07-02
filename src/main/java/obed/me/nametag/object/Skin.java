package obed.me.nametag.object;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class Skin implements ConfigurationSerializable {
    public static final Skin EMPTY_SKIN = new Skin();

    private UUID uuid;

    private String base64;

    private String signedBase64;

    public Skin(UUID uuid, String base64, String signedBase64) {
        Validate.notNull(uuid, "uuid cannot be null");
        Validate.notNull(base64, "base64 cannot be null");
        this.uuid = uuid;
        this.base64 = base64;
        this.signedBase64 = signedBase64;
    }

    private Skin() {}

    public boolean hasSignedBase64() {
        return (this.signedBase64 != null);
    }

    public String getSignedBase64() {
        return this.signedBase64;
    }

    public String getBase64() {
        return this.base64;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Skin))
            return false;
        Skin skin = (Skin)obj;
        if (skin == EMPTY_SKIN)
            return (this == EMPTY_SKIN);
        return (skin.base64.equals(this.base64) && skin.uuid.equals(this.uuid) && skin.signedBase64.equals(this.signedBase64));
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.base64, this.uuid, this.signedBase64 });
    }

    public String toString() {
        return "Skin{uuid=" + this.uuid + ",base64=" + this.base64 + ",signedBase64=" + this.signedBase64 + "}";
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = Maps.newHashMap();
        if (this == EMPTY_SKIN) {
            map.put("empty", "true");
        } else {
            map.put("uuid", this.uuid.toString());
            map.put("base64", this.base64);
            if (hasSignedBase64())
                map.put("signedBase64", this.signedBase64);
        }
        return map;
    }

    public static Skin deserialize(Map<String, Object> map) {
        if (map.containsKey("empty"))
            return EMPTY_SKIN;
        return new Skin(UUID.fromString((String)map.get("uuid")), (String)map.get("base64"), map.containsKey("signedBase64") ? (String)map.get("signedBase64") : null);
    }
}
