package com.bm.wschat.feature.dm.mapper;

import com.bm.wschat.feature.dm.dto.response.DirectMessageResponse;
import com.bm.wschat.feature.dm.model.DirectMessage;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.dto.UserShortResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    @Mapping(target = "read", expression = "java(dm.isRead())")
    @Mapping(target = "edited", expression = "java(dm.getEditedAt() != null)")
    DirectMessageResponse toResponse(DirectMessage dm);

    List<DirectMessageResponse> toResponses(List<DirectMessage> messages);

    UserShortResponse toUserShortResponse(User user);
}
