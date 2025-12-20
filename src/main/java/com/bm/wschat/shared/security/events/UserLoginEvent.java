package com.bm.wschat.shared.security.events;

import com.bm.wschat.feature.user.model.User;

public record UserLoginEvent(User user) {

}
