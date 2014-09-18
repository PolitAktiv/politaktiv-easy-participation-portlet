package org.politaktiv.easyParticipation.application;

import org.politaktiv.community.domain.Community;
import org.politaktiv.community.domain.CommunityFactory;

import com.liferay.portal.model.GroupConstants;

public class CommunityFactoryImpl implements CommunityFactory {

    @Override
    public Community createCommunity(String name, long communityId,
            int memberCount, String friendlyURL, int type) {
        boolean isOpenCommunity = false;
        boolean isRescritedCommunity = false;

        if (type == GroupConstants.TYPE_SITE_OPEN) {
            isOpenCommunity = true;
        }

        if (type == GroupConstants.TYPE_SITE_RESTRICTED) {
            isRescritedCommunity = true;
        }

        return new Community(name, communityId, memberCount, friendlyURL,
                isOpenCommunity, isRescritedCommunity);

    }

    @Override
    public Community createCommunity(String name, long communityId,
            long logoFolderId, int memberCount, String friendlyURL, int type) {

        boolean isOpenCommunity = false;
        boolean isRescritedCommunity = false;

        if (type == GroupConstants.TYPE_SITE_OPEN) {
            isOpenCommunity = true;
        }

        if (type == GroupConstants.TYPE_SITE_RESTRICTED) {
            isRescritedCommunity = true;
        }

        return new Community(name, communityId, logoFolderId, memberCount,
                friendlyURL, isOpenCommunity, isRescritedCommunity);
    }

}
