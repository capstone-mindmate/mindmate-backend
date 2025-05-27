package com.mindmate.mindmate_server.auth.dto;

import java.util.HashMap;
import java.util.Map;

public class GoogleOAuth2UserInfo {
    private Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }
    public String getId() {
        Object id = attributes.get("sub");
        return id != null ? id.toString() : null;
    }
    public String getName() {
        Object name = attributes.get("name");
        return name != null ? name.toString() : null;
    }
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }
    public String getImageUrl() {
        Object picture = attributes.get("picture");
        return picture != null ? picture.toString() : null;
    }}
