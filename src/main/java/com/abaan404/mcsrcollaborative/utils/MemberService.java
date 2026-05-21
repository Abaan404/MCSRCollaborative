package com.abaan404.mcsrcollaborative.utils;

import java.util.concurrent.CompletableFuture;

import static com.abaan404.mcsrcollaborative.McsrCollaborative.CONFIG;

public class MemberService {
    public static CompletableFuture<MemberInfo> getMemberByDiscordId(String discordId) {
        return Http.get(CONFIG.getApiUri() + "members/member?id=" + discordId, MemberInfo.class);
    }

    public static CompletableFuture<MemberInfo> getMemberByJavaId(String javaId) {
        return Http.get(CONFIG.getApiUri() + "members/member?javaId=" + javaId, MemberInfo.class);
    }

    public static CompletableFuture<MemberInfo> getMemberByBedrockId(String bedrockId) {
        return Http.get(CONFIG.getApiUri() + "members/member?bedrockId=" + bedrockId, MemberInfo.class);
    }

    public static class MemberInfo {
        public String id;
        public String javaId;
        public String javaUsername;
        public String bedrockId;
        public String bedrockUsername;
    }
}
