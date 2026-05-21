package com.abaan404.mcsrcollaborative.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.server.players.NameAndId;

import static com.abaan404.mcsrcollaborative.McsrCollaborative.CONFIG;

public class MemberService {
    public static CompletableFuture<MemberInfo> getMemberByDiscordId(String discordId) {
        return Http.get(CONFIG.getApiUri() + "members/member?id=" + discordId, MemberInfo.class);
    }

    public static CompletableFuture<MemberInfo> getMemberByJavaId(UUID javaId) {
        String uuid = javaId.toString().replaceAll("-", "");
        return Http.get(CONFIG.getApiUri() + "members/member?javaId=" + uuid, MemberInfo.class);
    }

    public static CompletableFuture<MemberInfo> getMemberByBedrockId(UUID bedrockId) {
        String uuid = String.valueOf(bedrockId.getLeastSignificantBits());
        return Http.get(CONFIG.getApiUri() + "members/member?bedrockId=" + uuid, MemberInfo.class);
    }

    public static class MemberInfo {
        public String id;
        public String javaId;
        public String javaUsername;
        public String bedrockId;
        public String bedrockUsername;

        public Optional<NameAndId> asNameAndId() {
            if (this.javaId != null) {
                UUID uuid = UUID.fromString(this.javaId.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"));

                return Optional.of(new NameAndId(uuid, this.javaUsername));

            } else if (this.bedrockId != null) {
                UUID uuid = new UUID(0L, Long.parseLong(this.bedrockId));
                return Optional.of(new NameAndId(uuid, "." + this.bedrockUsername));

            } else {
                return Optional.empty();
            }
        }
    }
}
